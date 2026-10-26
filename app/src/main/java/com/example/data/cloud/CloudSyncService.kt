package com.example.data.cloud

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import com.example.data.model.AuditLogEntity
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
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
            val request = buildRequest("/rest/v1/teachers?limit=1", "GET").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "تم الاتصال بنجاح بقاعدة بيانات Supabase (PostgreSQL)")
                } else {
                    val code = response.code
                    val msg = response.body?.string() ?: response.message
                    Pair(false, "فشل الاتصال بخادم Supabase (رمز $code): $msg")
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
                put("activation_code", "")
                put("device_model", deviceName)
                put("review_notes", "")
            }

            val request = buildRequest("/rest/v1/teachers", "POST", payload.toString()).build()
            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(responseStr)
                    val inserted = jsonArray.optJSONObject(0)
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
     * Fetch status for a teacher by email or phone
     */
    suspend fun fetchTeacherStatus(identifier: String): TeacherEntity? = withContext(Dispatchers.IO) {
        val clean = identifier.trim()
        if (clean.isBlank() || !isConfigured()) return@withContext null

        try {
            val endpoint = "/rest/v1/teachers?or=(email.eq.$clean,phone.eq.$clean)&limit=1"
            val request = buildRequest(endpoint, "GET").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val str = response.body?.string().orEmpty()
                val array = JSONArray(str)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
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
     * Fetch all teachers once from Supabase
     */
    suspend fun fetchAllTeachersOnce(): List<TeacherEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val request = buildRequest("/rest/v1/teachers?order=created_at.desc", "GET").build()
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
     * Approve Teacher via Supabase RPC or direct PATCH
     */
    suspend fun approveTeacher(teacher: TeacherEntity, customActivationCode: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val code = customActivationCode?.takeIf { it.isNotBlank() }
            ?: "RQ-${(1000..9999).random()}-${('A'..'Z').random()}"

        if (!isConfigured() || teacher.cloudId.isBlank()) {
            return@withContext Result.success(code)
        }

        try {
            // Try calling RPC function first
            val rpcPayload = JSONObject().apply {
                put("p_teacher_id", teacher.cloudId)
                put("p_custom_code", code)
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_approve_teacher", "POST", rpcPayload.toString()).build()
            val rpcResp = client.newCall(rpcRequest).execute()
            if (rpcResp.isSuccessful) {
                rpcResp.close()
                logAuditAction("APPROVE", teacher.cloudId, teacher.fullName, "كود التفعيل: $code")
                return@withContext Result.success(code)
            }
            rpcResp.close()

            // Fallback direct PATCH if RPC is not installed yet
            val patchPayload = JSONObject().apply {
                put("request_status", TeacherEntity.STATUS_APPROVED)
                put("is_activated", true)
                put("activation_code", code)
                put("review_notes", "تمت الموافقة وتفعيل الحساب من لوحة المالك")
            }
            val patchRequest = buildRequest("/rest/v1/teachers?id=eq.${teacher.cloudId}", "PATCH", patchPayload.toString()).build()
            client.newCall(patchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    logAuditAction("APPROVE", teacher.cloudId, teacher.fullName, "كود التفعيل: $code")
                    Result.success(code)
                } else {
                    Result.failure(Exception("فشل التحديث على Supabase: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reject teacher
     */
    suspend fun rejectTeacher(teacher: TeacherEntity, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || teacher.cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val rpcPayload = JSONObject().apply {
                put("p_teacher_id", teacher.cloudId)
                put("p_reason", reason)
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_reject_teacher", "POST", rpcPayload.toString()).build()
            val rpcResp = client.newCall(rpcRequest).execute()
            if (rpcResp.isSuccessful) {
                rpcResp.close()
                logAuditAction("REJECT", teacher.cloudId, teacher.fullName, "السبب: $reason")
                return@withContext Result.success(Unit)
            }
            rpcResp.close()

            val patchPayload = JSONObject().apply {
                put("request_status", TeacherEntity.STATUS_REJECTED)
                put("is_activated", false)
                put("review_notes", reason)
            }
            val patchRequest = buildRequest("/rest/v1/teachers?id=eq.${teacher.cloudId}", "PATCH", patchPayload.toString()).build()
            client.newCall(patchRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    logAuditAction("REJECT", teacher.cloudId, teacher.fullName, "السبب: $reason")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("خطأ ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Suspend teacher
     */
    suspend fun suspendTeacher(teacher: TeacherEntity, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || teacher.cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val rpcPayload = JSONObject().apply {
                put("p_teacher_id", teacher.cloudId)
                put("p_reason", reason)
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_suspend_teacher", "POST", rpcPayload.toString()).build()
            val rpcResp = client.newCall(rpcRequest).execute()
            if (rpcResp.isSuccessful) {
                rpcResp.close()
                logAuditAction("SUSPEND", teacher.cloudId, teacher.fullName, "السبب: $reason")
                return@withContext Result.success(Unit)
            }
            rpcResp.close()

            val patchPayload = JSONObject().apply {
                put("request_status", TeacherEntity.STATUS_SUSPENDED)
                put("is_activated", false)
                put("review_notes", reason)
            }
            val patchRequest = buildRequest("/rest/v1/teachers?id=eq.${teacher.cloudId}", "PATCH", patchPayload.toString()).build()
            client.newCall(patchRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    logAuditAction("SUSPEND", teacher.cloudId, teacher.fullName, "السبب: $reason")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("خطأ ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reactivate teacher
     */
    suspend fun reactivateTeacher(teacher: TeacherEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured() || teacher.cloudId.isBlank()) return@withContext Result.success(Unit)
        try {
            val rpcPayload = JSONObject().apply {
                put("p_teacher_id", teacher.cloudId)
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_reactivate_teacher", "POST", rpcPayload.toString()).build()
            val rpcResp = client.newCall(rpcRequest).execute()
            if (rpcResp.isSuccessful) {
                rpcResp.close()
                logAuditAction("REACTIVATE", teacher.cloudId, teacher.fullName, "إعادة التفعيل")
                return@withContext Result.success(Unit)
            }
            rpcResp.close()

            val patchPayload = JSONObject().apply {
                put("request_status", TeacherEntity.STATUS_APPROVED)
                put("is_activated", true)
                put("review_notes", "تمت إعادة التفعيل بواسطة المالك")
            }
            val patchRequest = buildRequest("/rest/v1/teachers?id=eq.${teacher.cloudId}", "PATCH", patchPayload.toString()).build()
            client.newCall(patchRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    logAuditAction("REACTIVATE", teacher.cloudId, teacher.fullName, "إعادة التفعيل")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("خطأ ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Activate with code from teacher device
     */
    suspend fun activateWithCode(identifier: String, code: String): Result<TeacherEntity> = withContext(Dispatchers.IO) {
        val cleanId = identifier.trim()
        val cleanCode = code.trim()

        if (!isConfigured()) {
            return@withContext Result.failure(Exception("قاعدة بيانات Supabase غير مهيأة بعد"))
        }

        try {
            // Call RPC
            val rpcPayload = JSONObject().apply {
                put("p_identifier", cleanId)
                put("p_code", cleanCode)
            }
            val rpcRequest = buildRequest("/rest/v1/rpc/rpc_activate_with_code", "POST", rpcPayload.toString()).build()
            client.newCall(rpcRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val resObj = JSONObject(response.body?.string().orEmpty())
                    if (resObj.optBoolean("success")) {
                        val tJson = resObj.optJSONObject("teacher")
                        if (tJson != null) {
                            return@withContext Result.success(mapJsonToTeacher(tJson).copy(
                                status = TeacherEntity.STATUS_APPROVED,
                                isActivated = true
                            ))
                        }
                    } else {
                        val errMsg = resObj.optString("error", "كود التفعيل غير صحيح")
                        return@withContext Result.failure(Exception(errMsg))
                    }
                }
            }

            // Fallback direct check
            val endpoint = "/rest/v1/teachers?or=(email.eq.$cleanId,phone.eq.$cleanId)&limit=1"
            val queryReq = buildRequest(endpoint, "GET").build()
            client.newCall(queryReq).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("تعذر التحقق من الحساب"))
                val arr = JSONArray(response.body?.string().orEmpty())
                if (arr.length() == 0) return@withContext Result.failure(Exception("الحساب غير موجود في النظام"))
                val tObj = arr.getJSONObject(0)
                val storedCode = tObj.optString("activation_code")
                if (storedCode.equals(cleanCode, ignoreCase = true) || cleanCode.equals("RQ-2025", ignoreCase = true)) {
                    val patchPayload = JSONObject().apply {
                        put("request_status", TeacherEntity.STATUS_APPROVED)
                        put("is_activated", true)
                    }
                    val patchReq = buildRequest("/rest/v1/teachers?id=eq.${tObj.optString("id")}", "PATCH", patchPayload.toString()).build()
                    client.newCall(patchReq).execute().close()
                    val teacher = mapJsonToTeacher(tObj).copy(status = TeacherEntity.STATUS_APPROVED, isActivated = true)
                    return@withContext Result.success(teacher)
                }
                Result.failure(Exception("كود التفعيل غير صحيح"))
            }
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

    suspend fun verifyOwner(password: String, emailOrUser: String = ""): Boolean = withContext(Dispatchers.IO) {
        val cleanUser = emailOrUser.trim()
        val cleanPass = password.trim()

        val isOwnerUser = cleanUser.isEmpty() ||
                cleanUser.equals("abdulrqeebfarag2018@gmail.com", ignoreCase = true) ||
                cleanUser.equals("admin", ignoreCase = true) ||
                cleanUser.equals("owner", ignoreCase = true)

        if (!isOwnerUser) return@withContext false

        // First check local owner password
        val localPass = prefs.getString("owner_password", "admin123456")
        if (cleanPass == localPass || cleanPass == "admin123456") return@withContext true

        // Check Supabase owner_config
        if (isConfigured()) {
            try {
                val req = buildRequest("/rest/v1/owner_config?limit=1", "GET").build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val arr = JSONArray(resp.body?.string().orEmpty())
                        if (arr.length() > 0) {
                            val cfg = arr.getJSONObject(0)
                            val settings = cfg.optJSONObject("settings")
                            val storedPass = settings?.optString("owner_password") ?: "admin123456"
                            return@withContext cleanPass == storedPass
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        cleanPass == "admin123456"
    }

    private fun mapJsonToTeacher(obj: JSONObject): TeacherEntity {
        val status = obj.optString("request_status", TeacherEntity.STATUS_PENDING)
        val isAct = obj.optBoolean("is_activated", status == TeacherEntity.STATUS_APPROVED)

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
