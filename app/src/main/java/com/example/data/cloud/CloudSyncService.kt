package com.example.data.cloud

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import com.example.data.model.AuditLogEntity
import com.example.data.model.TeacherEntity
import com.example.data.model.EducationalResourceEntity
import com.example.data.model.AppAnnouncementEntity
import com.example.data.model.AppDynamicContentEntity
import com.example.data.model.ActivationCodeEntity
import com.example.util.RqeebNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * CloudSyncService coordinates live multi-device synchronization with PostgreSQL via Supabase REST API & RPC
 * Completely independent of Firebase.
 */
class CloudSyncService(private val context: Context) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val prefs = context.getSharedPreferences("rqeeb_supabase_prefs", Context.MODE_PRIVATE)
    private val seenPendingTeacherIds = mutableSetOf<String>()

    fun getSupabaseUrl(): String {
        val stored = prefs.getString("supabase_url", null)
        if (!stored.isNullOrBlank()) return stored.trim().removeSuffix("/")

        return try {
            val field = BuildConfig::class.java.getField("SUPABASE_URL")
            val v = field.get(null) as? String
            v?.trim()?.removeSuffix("/").orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    fun getSupabaseAnonKey(): String {
        val stored = prefs.getString("supabase_anon_key", null)
        if (!stored.isNullOrBlank()) return stored.trim()

        return try {
            val field = BuildConfig::class.java.getField("SUPABASE_ANON_KEY")
            val v = field.get(null) as? String
            v?.trim().orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    fun saveSupabaseConfig(url: String, anonKey: String) {
        prefs.edit()
            .putString("supabase_url", url.trim().removeSuffix("/"))
            .putString("supabase_anon_key", anonKey.trim())
            .apply()
    }

    fun getOwnerPin(): String = prefs.getString("owner_pin", "").orEmpty()

    fun saveOwnerPin(pin: String) {
        prefs.edit().putString("owner_pin", pin.trim()).apply()
    }

    fun isConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseAnonKey()
        return url.startsWith("https://") && !url.contains("your-project-id") && key.isNotBlank() && !key.contains("your-anon-key")
    }

    private fun buildRequest(endpoint: String, method: String = "GET", jsonBody: String? = null): Request.Builder {
        val baseUrl = getSupabaseUrl()
        val apiKey = getSupabaseAnonKey()
        val cleanEndpoint = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        val fullUrl = "$baseUrl$cleanEndpoint"

        val builder = Request.Builder()
            .url(fullUrl)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        when (method) {
            "POST" -> builder.post((jsonBody ?: "{}").toRequestBody(mediaType))
            "PATCH" -> builder.patch((jsonBody ?: "{}").toRequestBody(mediaType))
            "DELETE" -> builder.delete()
            else -> builder.get()
        }

        return builder
    }

    /**
     * Test live Supabase connection
     */
    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Pair(false, "إعدادات Supabase غير مكتملة. يرجى إدخال عنوان المشروع ومفتاح anon.")
        }
        try {
            val request = buildRequest("/rest/v1/owner_config?limit=1", "GET").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "تم الاتصال بنجاح بقاعدة بيانات Supabase (PostgreSQL)")
                } else {
                    val code = response.code
                    val msg = response.body?.string() ?: response.message
                    Pair(false, "فحص الخادم (رمز $code): $msg")
                }
            }
        } catch (e: Exception) {
            Pair(false, "تعذر الوصول إلى الخادم: ${e.localizedMessage ?: "تأكد من اتصال الإنترنت"}")
        }
    }

    /**
     * Register a new teacher on Supabase PostgreSQL
     */
    suspend fun registerTeacherOnCloud(teacher: TeacherEntity): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.success("local-temp-${System.currentTimeMillis()}")
        }
        try {
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            val payload = JSONObject().apply {
                put("full_name", teacher.fullName)
                put("phone", teacher.phone)
                put("email", teacher.email)
                put("password_hash", teacher.passwordHash)
                put("school_name", teacher.schoolName)
                put("governorate", teacher.governorate)
                put("district", teacher.directorate)
                put("subject", teacher.subject)
                put("education_stage", teacher.stage)
                put("grade", teacher.gradeTaught)
                put("section", teacher.section)
                put("curriculum_edition", teacher.curriculumEdition)
                put("academic_year", teacher.academicYear)
                put("request_status", TeacherEntity.STATUS_PENDING)
                put("is_activated", false)
                put("device_model", deviceName)
                put("review_notes", "")
            }

            val request = buildRequest("/rest/v1/teachers", "POST", payload.toString()).build()
            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val jsonArray = try { JSONArray(responseStr) } catch (_: Exception) { null }
                    val inserted = jsonArray?.optJSONObject(0)
                    val cloudId = inserted?.optString("id").orEmpty()
                    Result.success(if (cloudId.isNotBlank()) cloudId else "sb-${System.currentTimeMillis()}")
                } else {
                    Result.failure(Exception("خطأ من خادم Supabase (${response.code}): $responseStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch status for a teacher by email or phone securely via RPC
     */
    suspend fun fetchTeacherStatus(identifier: String): TeacherEntity? = withContext(Dispatchers.IO) {
        val clean = identifier.trim()
        if (clean.isBlank() || !isConfigured()) return@withContext null

        try {
            val rpcPayload = JSONObject().apply {
                put("p_identifier", clean)
            }
            val request = buildRequest("/rest/v1/rpc/rpc_get_teacher_by_login", "POST", rpcPayload.toString()).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val str = response.body?.string().orEmpty()
                if (str.isNotBlank() && str != "null") {
                    val obj = JSONObject(str)
                    return@withContext mapJsonToTeacher(obj)
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Real-time Flow of all teachers from Supabase PostgreSQL
     */
    fun getAllTeachersFlow(): Flow<List<TeacherEntity>> = flow {
        while (currentCoroutineContext().isActive) {
            val list = fetchAllTeachersOnce()
            emit(list)

            // Check for new pending registrations to trigger local notification for Realme owner phone
            for (teacher in list) {
                if (teacher.status == TeacherEntity.STATUS_PENDING && !seenPendingTeacherIds.contains(teacher.cloudId)) {
                    seenPendingTeacherIds.add(teacher.cloudId)
                    RqeebNotificationHelper.notifyNewTeacherRequest(context, teacher)
                }
            }

            delay(7000) // Poll interval for real-time synchronization
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Fetch all teachers for owner via secure RPC
     */
    suspend fun fetchAllTeachersOnce(): List<TeacherEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val rpcPayload = JSONObject().apply {
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_get_all_teachers_for_owner", "POST", rpcPayload.toString()).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val str = response.body?.string().orEmpty()
                val array = JSONArray(str)
                val list = mutableListOf<TeacherEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(mapJsonToTeacher(obj))
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Approve Teacher via Supabase RPC
     */
    suspend fun approveTeacher(teacher: TeacherEntity, customActivationCode: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val code = customActivationCode?.takeIf { it.isNotBlank() }
            ?: "RQ-${(1000..9999).random()}-${('A'..'Z').random()}"

        if (!isConfigured() || teacher.cloudId.isBlank()) {
            return@withContext Result.success(code)
        }

        try {
            val rpcPayload = JSONObject().apply {
                put("p_teacher_id", teacher.cloudId)
                put("p_custom_code", code)
                put("p_owner_pin", getOwnerPin())
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_approve_teacher", "POST", rpcPayload.toString()).build()
            client.newCall(rpcRequest).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val resObj = JSONObject(bodyStr)
                    if (resObj.optBoolean("success")) {
                        val returnedCode = resObj.optString("code", code)
                        logAuditAction("APPROVE", teacher.cloudId, teacher.fullName, "كود التفعيل: $returnedCode")
                        Result.success(returnedCode)
                    } else {
                        val error = resObj.optString("error", "فشلت عملية الموافقة")
                        Result.failure(Exception(error))
                    }
                } else {
                    Result.failure(Exception("خطأ من خادم Supabase (${response.code}): $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reject teacher via Supabase RPC
     */
    suspend fun rejectTeacher(teacher: TeacherEntity, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || teacher.cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val rpcPayload = JSONObject().apply {
                put("p_teacher_id", teacher.cloudId)
                put("p_reason", reason)
                put("p_owner_pin", getOwnerPin())
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_reject_teacher", "POST", rpcPayload.toString()).build()
            client.newCall(rpcRequest).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val resObj = JSONObject(bodyStr)
                    if (resObj.optBoolean("success")) {
                        logAuditAction("REJECT", teacher.cloudId, teacher.fullName, "السبب: $reason")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(resObj.optString("error", "فشلت العملية")))
                    }
                } else {
                    Result.failure(Exception("خطأ ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Suspend teacher via Supabase RPC
     */
    suspend fun suspendTeacher(teacher: TeacherEntity, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || teacher.cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val rpcPayload = JSONObject().apply {
                put("p_teacher_id", teacher.cloudId)
                put("p_reason", reason)
                put("p_owner_pin", getOwnerPin())
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_suspend_teacher", "POST", rpcPayload.toString()).build()
            client.newCall(rpcRequest).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val resObj = JSONObject(bodyStr)
                    if (resObj.optBoolean("success")) {
                        logAuditAction("SUSPEND", teacher.cloudId, teacher.fullName, "السبب: $reason")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(resObj.optString("error", "فشلت العملية")))
                    }
                } else {
                    Result.failure(Exception("خطأ ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reactivate teacher via Supabase RPC
     */
    suspend fun reactivateTeacher(teacher: TeacherEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || teacher.cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val rpcPayload = JSONObject().apply {
                put("p_teacher_id", teacher.cloudId)
                put("p_owner_pin", getOwnerPin())
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_reactivate_teacher", "POST", rpcPayload.toString()).build()
            client.newCall(rpcRequest).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val resObj = JSONObject(bodyStr)
                    if (resObj.optBoolean("success")) {
                        logAuditAction("REACTIVATE", teacher.cloudId, teacher.fullName, "إعادة التفعيل")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception(resObj.optString("error", "فشلت العملية")))
                    }
                } else {
                    Result.failure(Exception("خطأ ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Activate with code exclusively via atomic RPC
     */
    suspend fun activateWithCode(identifier: String, code: String): Result<TeacherEntity> = withContext(Dispatchers.IO) {
        val cleanId = identifier.trim()
        val cleanCode = code.trim().uppercase()

        if (!isConfigured()) {
            return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة بعد"))
        }

        try {
            val rpcPayload = JSONObject().apply {
                put("p_identifier", cleanId)
                put("p_code", cleanCode)
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_activate_with_code", "POST", rpcPayload.toString()).build()
            client.newCall(rpcRequest).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val resObj = JSONObject(bodyStr)
                    if (resObj.optBoolean("success")) {
                        val tJson = resObj.optJSONObject("teacher")
                        if (tJson != null) {
                            return@withContext Result.success(mapJsonToTeacher(tJson).copy(
                                status = TeacherEntity.STATUS_APPROVED,
                                isActivated = true
                            ))
                        }
                    } else {
                        val errMsg = resObj.optString("error", "كود التفعيل غير صحيح أو منتهي الصلاحية")
                        return@withContext Result.failure(Exception(errMsg))
                    }
                } else {
                    return@withContext Result.failure(Exception("خطأ من الخادم (${response.code}): $bodyStr"))
                }
            }
            Result.failure(Exception("تعذر تفعيل الحساب"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTeacherProfile(teacher: TeacherEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || teacher.cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val payload = JSONObject().apply {
                put("full_name", teacher.fullName)
                put("school_name", teacher.schoolName)
                put("governorate", teacher.governorate)
                put("district", teacher.directorate)
                put("subject", teacher.subject)
                put("education_stage", teacher.stage)
                put("grade", teacher.gradeTaught)
                put("section", teacher.section)
            }
            val request = buildRequest("/rest/v1/teachers?id=eq.${teacher.cloudId}", "PATCH", payload.toString()).build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit) else Result.failure(Exception("فشل التحديث (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun logAuditAction(action: String, targetId: String, targetName: String, details: String) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        try {
            val payload = JSONObject().apply {
                put("action", action)
                put("teacher_id", if (targetId.isNotBlank()) targetId else JSONObject.NULL)
                put("teacher_name", targetName)
                put("details", JSONObject().apply {
                    put("notes", details)
                    put("device", "Realme Phone (المالك)")
                })
            }
            val req = buildRequest("/rest/v1/audit_logs", "POST", payload.toString()).build()
            client.newCall(req).execute().close()
        } catch (_: Exception) {}
    }

    fun getAuditLogsFlow(): Flow<List<AuditLogEntity>> = flow {
        while (currentCoroutineContext().isActive) {
            val logs = fetchAuditLogsOnce()
            emit(logs)
            delay(10000)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchAuditLogsOnce(): List<AuditLogEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val req = buildRequest("/rest/v1/audit_logs?order=created_at.desc&limit=50", "GET").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty())
                val list = mutableListOf<AuditLogEntity>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        AuditLogEntity(
                            id = obj.optString("id"),
                            action = obj.optString("action"),
                            targetTeacherId = obj.optString("teacher_id"),
                            targetTeacherName = obj.optString("teacher_name", "معلم"),
                            performedBy = "المالك الرئيسي (Realme Phone)",
                            timestamp = System.currentTimeMillis(),
                            details = obj.optJSONObject("details")?.optString("notes") ?: obj.optString("details")
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Verify owner credentials securely against server RPC
     */
    suspend fun verifyOwner(password: String, emailOrUser: String = ""): Boolean = withContext(Dispatchers.IO) {
        val cleanUser = emailOrUser.trim()
        val cleanPass = password.trim()

        if (cleanPass.length < 6) return@withContext false

        val isOwnerUser = cleanUser.isEmpty() ||
                cleanUser.equals("abdulrqeebfarag2018@gmail.com", ignoreCase = true) ||
                cleanUser.equals("admin", ignoreCase = true) ||
                cleanUser.equals("owner", ignoreCase = true)

        if (!isOwnerUser) return@withContext false

        // Check against Supabase server RPC
        if (isConfigured()) {
            try {
                val rpcPayload = JSONObject().apply {
                    put("p_password", cleanPass)
                    put("p_email", "abdulrqeebfarag2018@gmail.com")
                }
                val req = buildRequest("/rest/v1/rpc/rpc_verify_owner", "POST", rpcPayload.toString()).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val obj = JSONObject(resp.body?.string().orEmpty())
                        val verified = obj.optBoolean("verified", false)
                        if (verified) {
                            saveOwnerPin(cleanPass)
                            return@withContext true
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Local verification fallback when offline if PIN was previously saved
        val storedPin = getOwnerPin()
        if (storedPin.isNotBlank()) {
            cleanPass == storedPin
        } else {
            // First time local save
            saveOwnerPin(cleanPass)
            true
        }
    }

    private fun mapJsonToTeacher(obj: JSONObject): TeacherEntity {
        val status = obj.optString("request_status", TeacherEntity.STATUS_PENDING)
        val isAct = obj.optBoolean("is_activated", false)

        return TeacherEntity(
            id = 0,
            cloudId = obj.optString("id"),
            fullName = obj.optString("full_name"),
            phone = obj.optString("phone"),
            email = obj.optString("email"),
            passwordHash = obj.optString("password_hash"),
            schoolName = obj.optString("school_name"),
            governorate = obj.optString("governorate", "أمانة العاصمة"),
            directorate = obj.optString("district", ""),
            subject = obj.optString("subject", "الرياضيات"),
            stage = obj.optString("education_stage", "مرحلة التعليم الأساسي"),
            gradeTaught = obj.optString("grade"),
            section = obj.optString("section", ""),
            curriculumEdition = obj.optString("curriculum_edition", "طبعة صنعاء (وزارة التربية والتعليم - صنعاء)"),
            academicYear = obj.optString("academic_year", "1446-1447هـ / 2024-2025م"),
            status = status,
            createdAt = System.currentTimeMillis(),
            activationCode = obj.optString("activation_code", ""),
            isActivated = isAct,
            deviceModel = obj.optString("device_model", ""),
            reviewNotes = obj.optString("review_notes", "")
        )
    }

    /**
     * Add Teacher Manually from Owner Dashboard
     */
    suspend fun addTeacherManually(
        fullName: String,
        phone: String,
        schoolName: String,
        governorate: String,
        district: String,
        subject: String,
        stage: String,
        grade: String,
        section: String,
        curriculumEdition: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة"))
        try {
            val payload = JSONObject().apply {
                put("p_full_name", fullName.trim())
                put("p_phone", phone.trim())
                put("p_school_name", schoolName.trim())
                put("p_governorate", governorate.trim())
                put("p_district", district.trim())
                put("p_subject", subject.trim())
                put("p_stage", stage.trim())
                put("p_grade", grade.trim())
                put("p_section", section.trim())
                put("p_curriculum_edition", curriculumEdition.trim())
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_add_teacher_manual", "POST", payload.toString()).build()
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val obj = JSONObject(bodyStr)
                    if (obj.optBoolean("success")) {
                        val teacherId = obj.optString("teacher_id")
                        val code = obj.optString("code")
                        Result.success(Pair(teacherId, code))
                    } else {
                        Result.failure(Exception(obj.optString("error", "فشلت إضافة المعلم")))
                    }
                } else {
                    Result.failure(Exception("خطأ من الخادم (${response.code}): $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update Teacher Manually
     */
    suspend fun updateTeacherManually(teacher: TeacherEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || teacher.cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val payload = JSONObject().apply {
                put("p_teacher_id", teacher.cloudId)
                put("p_full_name", teacher.fullName)
                put("p_phone", teacher.phone)
                put("p_school_name", teacher.schoolName)
                put("p_governorate", teacher.governorate)
                put("p_district", teacher.directorate)
                put("p_subject", teacher.subject)
                put("p_stage", teacher.stage)
                put("p_grade", teacher.gradeTaught)
                put("p_section", teacher.section)
                put("p_curriculum_edition", teacher.curriculumEdition)
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_update_teacher_manual", "POST", payload.toString()).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) Result.success(Unit)
                    else Result.failure(Exception(obj.optString("error", "فشل التحديث")))
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete Teacher Manually
     */
    suspend fun deleteTeacherManually(cloudId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val payload = JSONObject().apply {
                put("p_teacher_id", cloudId)
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_delete_teacher_manual", "POST", payload.toString()).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) Result.success(Unit)
                    else Result.failure(Exception(obj.optString("error", "فشل الحذف")))
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate Batch of Activation Codes (e.g. 100 codes)
     */
    suspend fun generateBatchCodes(count: Int = 100, prefix: String = "RQ"): Result<Int> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة"))
        try {
            val payload = JSONObject().apply {
                put("p_count", count)
                put("p_prefix", prefix)
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_generate_batch_codes", "POST", payload.toString()).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) {
                        Result.success(obj.optInt("generated_count", count))
                    } else {
                        Result.failure(Exception(obj.optString("error", "فشل توليد الأكواد")))
                    }
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a single custom activation code for a specific teacher with custom duration
     */
    suspend fun createSingleActivationCode(
        teacherName: String,
        teacherPhone: String,
        durationDays: Int = 365,
        customCode: String? = null
    ): Result<ActivationCodeEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة بعد"))
        try {
            val payload = JSONObject().apply {
                put("p_teacher_name", teacherName.trim())
                put("p_teacher_phone", teacherPhone.trim())
                put("p_duration_days", durationDays)
                if (!customCode.isNullOrBlank()) {
                    put("p_custom_code", customCode.trim().uppercase())
                }
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_create_activation_code", "POST", payload.toString()).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) {
                        val entity = ActivationCodeEntity(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            code = obj.optString("code"),
                            teacherId = null,
                            teacherName = obj.optString("teacher_name", teacherName.trim()),
                            teacherPhone = obj.optString("teacher_phone", teacherPhone.trim()),
                            durationDays = obj.optInt("duration_days", durationDays),
                            isUsed = false,
                            isRevoked = false,
                            revokedReason = "",
                            status = "AVAILABLE",
                            createdAt = System.currentTimeMillis(),
                            usedAt = null,
                            expiresAt = System.currentTimeMillis() + (durationDays * 24L * 3600 * 1000)
                        )
                        Result.success(entity)
                    } else {
                        Result.failure(Exception(obj.optString("error", "فشل إنشاء كود التفعيل")))
                    }
                } else {
                    Result.failure(Exception("خطأ الخادم (${resp.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Seed the 100 official activation codes (T001 - T100) directly to Supabase cloud
     */
    suspend fun seedOfficial100CodesToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة بعد"))
        try {
            val payload = JSONObject().apply {
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_seed_official_100_codes", "POST", payload.toString()).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) {
                        Result.success(obj.optInt("count", 100))
                    } else {
                        Result.failure(Exception(obj.optString("error", "فشل تثبيت الأكواد السحابية")))
                    }
                } else {
                    Result.failure(Exception("خطأ الخادم (${resp.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch All Activation Codes for Owner with full Result
     */
    suspend fun fetchAllActivationCodesResult(): Result<List<ActivationCodeEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة بعد"))
        try {
            val payload = JSONObject().apply {
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_get_all_activation_codes", "POST", payload.toString()).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("خطأ الخادم (${resp.code}): $body"))
                }
                val arr = JSONArray(body)
                val list = mutableListOf<ActivationCodeEntity>()
                val now = System.currentTimeMillis()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val days = o.optInt("duration_days", 365)
                    list.add(
                        ActivationCodeEntity(
                            id = o.optString("id"),
                            code = o.optString("code"),
                            teacherId = o.optString("teacher_id").takeIf { it.isNotBlank() && it != "null" },
                            teacherName = o.optString("teacher_name", "كود عام متاح"),
                            teacherPhone = o.optString("teacher_phone", ""),
                            durationDays = days,
                            isUsed = o.optBoolean("is_used", false),
                            isRevoked = o.optBoolean("is_revoked", false),
                            revokedReason = o.optString("revoked_reason", ""),
                            status = o.optString("status", "AVAILABLE"),
                            createdAt = now - (i * 1000L),
                            expiresAt = now + (days * 24L * 3600 * 1000)
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch All Activation Codes for Owner (backward-compatible list)
     */
    suspend fun fetchAllActivationCodes(): List<ActivationCodeEntity> = withContext(Dispatchers.IO) {
        fetchAllActivationCodesResult().getOrDefault(emptyList())
    }

    /**
     * Revoke Activation Code
     */
    suspend fun revokeActivationCode(codeId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة"))
        try {
            val payload = JSONObject().apply {
                put("p_code_id", codeId)
                put("p_reason", reason)
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_revoke_activation_code", "POST", payload.toString()).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) Result.success(Unit)
                    else Result.failure(Exception(obj.optString("error", "فشل إلغاء الكود")))
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reissue Activation Code for Teacher
     */
    suspend fun reissueActivationCode(teacherCloudId: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة"))
        try {
            val payload = JSONObject().apply {
                put("p_teacher_id", teacherCloudId)
                put("p_owner_pin", getOwnerPin())
            }
            val request = buildRequest("/rest/v1/rpc/rpc_reissue_activation_code", "POST", payload.toString()).build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) Result.success(obj.optString("code"))
                    else Result.failure(Exception(obj.optString("error", "فشل إصدار كود بديل")))
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch Educational Resources
     */
    suspend fun fetchEducationalResources(grade: String? = null, subject: String? = null): List<EducationalResourceEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val payload = JSONObject().apply {
                if (!grade.isNullOrBlank() && grade != "الكل") put("p_grade", grade)
                if (!subject.isNullOrBlank() && subject != "الكل") put("p_subject", subject)
            }
            val req = buildRequest("/rest/v1/rpc/rpc_get_educational_resources", "POST", payload.toString()).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty())
                val list = mutableListOf<EducationalResourceEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        EducationalResourceEntity(
                            id = 0,
                            cloudId = o.optString("id"),
                            title = o.optString("title"),
                            description = o.optString("description", ""),
                            resourceType = o.optString("resource_type", "PDF"),
                            subject = o.optString("subject", "الرياضيات"),
                            grade = o.optString("grade", "عام"),
                            unitName = o.optString("unit_name", ""),
                            lessonName = o.optString("lesson_name", ""),
                            fileUrl = o.optString("file_url", ""),
                            fileSizeBytes = o.optLong("file_size_bytes", 0L),
                            isPublic = o.optBoolean("is_public", true),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Save Educational Resource
     */
    suspend fun saveEducationalResource(res: EducationalResourceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة"))
        try {
            val payload = JSONObject().apply {
                if (res.cloudId.isNotBlank()) put("p_id", res.cloudId)
                put("p_title", res.title)
                put("p_description", res.description)
                put("p_resource_type", res.resourceType)
                put("p_subject", res.subject)
                put("p_grade", res.grade)
                put("p_unit_name", res.unitName)
                put("p_lesson_name", res.lessonName)
                put("p_file_url", res.fileUrl)
                put("p_file_size_bytes", res.fileSizeBytes)
                put("p_is_public", res.isPublic)
                put("p_owner_pin", getOwnerPin())
            }
            val req = buildRequest("/rest/v1/rpc/rpc_add_or_update_resource", "POST", payload.toString()).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) Result.success(Unit)
                    else Result.failure(Exception(obj.optString("error", "فشل الحفظ")))
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete Educational Resource
     */
    suspend fun deleteEducationalResource(cloudId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val payload = JSONObject().apply {
                put("p_resource_id", cloudId)
                put("p_owner_pin", getOwnerPin())
            }
            val req = buildRequest("/rest/v1/rpc/rpc_delete_resource", "POST", payload.toString()).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) Result.success(Unit)
                    else Result.failure(Exception(obj.optString("error", "فشل الحذف")))
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch Announcements
     */
    suspend fun fetchAnnouncements(grade: String = "ALL", subject: String = "ALL"): List<AppAnnouncementEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val payload = JSONObject().apply {
                put("p_grade", grade)
                put("p_subject", subject)
            }
            val req = buildRequest("/rest/v1/rpc/rpc_get_app_announcements", "POST", payload.toString()).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty())
                val list = mutableListOf<AppAnnouncementEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        AppAnnouncementEntity(
                            id = 0,
                            cloudId = o.optString("id"),
                            title = o.optString("title"),
                            content = o.optString("content"),
                            type = o.optString("type", "BANNER"),
                            targetGrade = o.optString("target_grade", "ALL"),
                            targetSubject = o.optString("target_subject", "ALL"),
                            isActive = o.optBoolean("is_active", true),
                            priority = o.optInt("priority", 0),
                            linkUrl = o.optString("link_url", ""),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Save Announcement
     */
    suspend fun saveAnnouncement(ann: AppAnnouncementEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة"))
        try {
            val payload = JSONObject().apply {
                if (ann.cloudId.isNotBlank()) put("p_id", ann.cloudId)
                put("p_title", ann.title)
                put("p_content", ann.content)
                put("p_type", ann.type)
                put("p_target_grade", ann.targetGrade)
                put("p_target_subject", ann.targetSubject)
                put("p_is_active", ann.isActive)
                put("p_priority", ann.priority)
                put("p_link_url", ann.linkUrl)
                put("p_owner_pin", getOwnerPin())
            }
            val req = buildRequest("/rest/v1/rpc/rpc_add_or_update_announcement", "POST", payload.toString()).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) Result.success(Unit)
                    else Result.failure(Exception(obj.optString("error", "فشل الحفظ")))
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete Announcement
     */
    suspend fun deleteAnnouncement(cloudId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val payload = JSONObject().apply {
                put("p_announcement_id", cloudId)
                put("p_owner_pin", getOwnerPin())
            }
            val req = buildRequest("/rest/v1/rpc/rpc_delete_announcement", "POST", payload.toString()).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) Result.success(Unit)
                    else Result.failure(Exception(obj.optString("error", "فشل الحذف")))
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch Dynamic Content
     */
    suspend fun fetchDynamicContent(category: String? = null): List<AppDynamicContentEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val payload = JSONObject().apply {
                if (!category.isNullOrBlank()) put("p_category", category)
            }
            val req = buildRequest("/rest/v1/rpc/rpc_get_dynamic_content", "POST", payload.toString()).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty())
                val list = mutableListOf<AppDynamicContentEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        AppDynamicContentEntity(
                            contentKey = o.optString("content_key"),
                            category = o.optString("category", "GENERAL"),
                            title = o.optString("title", ""),
                            contentValue = o.optString("content_value", ""),
                            isActive = o.optBoolean("is_active", true),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Save Dynamic Content
     */
    suspend fun saveDynamicContent(content: AppDynamicContentEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة"))
        try {
            val payload = JSONObject().apply {
                put("p_key", content.contentKey)
                put("p_category", content.category)
                put("p_title", content.title)
                put("p_value", content.contentValue)
                put("p_is_active", content.isActive)
                put("p_owner_pin", getOwnerPin())
            }
            val req = buildRequest("/rest/v1/rpc/rpc_add_or_update_dynamic_content", "POST", payload.toString()).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val obj = JSONObject(body)
                    if (obj.optBoolean("success")) Result.success(Unit)
                    else Result.failure(Exception(obj.optString("error", "فشل الحفظ")))
                } else {
                    Result.failure(Exception("خطأ ${resp.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CloudSyncService? = null

        fun getInstance(context: Context): CloudSyncService {
            return INSTANCE ?: synchronized(this) {
                val instance = CloudSyncService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
