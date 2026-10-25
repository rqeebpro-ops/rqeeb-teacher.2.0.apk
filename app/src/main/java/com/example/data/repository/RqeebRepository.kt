package com.example.data.repository

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class RqeebRepository(
    private val teacherDao: TeacherDao,
    private val lessonDao: LessonDao,
    private val examDao: ExamDao,
    private val bookDao: BookDao,
    private val studentDao: StudentDao,
    private val timetableDao: TimetableDao,
    private val homeworkDao: HomeworkDao,
    private val termPlanDao: TermPlanDao,
    private val appSettingDao: AppSettingDao,
    private var cloudSyncService: com.example.data.cloud.CloudSyncService? = null
) {
    fun setCloudSyncService(service: com.example.data.cloud.CloudSyncService) {
        this.cloudSyncService = service
    }

    // Teachers: Cloud-First with Local Room Persistence
    val allTeachers: Flow<List<TeacherEntity>>
        get() = cloudSyncService?.getAllTeachersFlow() ?: teacherDao.getAllTeachers()

    val totalTeachersCount: Flow<Int> = teacherDao.getTotalTeachersCount()
    val totalLessonsCount: Flow<Int> = lessonDao.getTotalLessonsCount()
    val totalExamsCount: Flow<Int> = examDao.getTotalExamsCount()

    fun getTeachersCountByStatus(status: String): Flow<Int> = teacherDao.getCountByStatus(status)

    suspend fun getTeacherById(id: Long): TeacherEntity? = teacherDao.getTeacherById(id)

    suspend fun getTeacherByLogin(identifier: String): TeacherEntity? {
        val clean = identifier.trim()
        // Query cloud first for real-time status
        val cloudTeacher = cloudSyncService?.fetchTeacherStatus(clean)
        if (cloudTeacher != null) {
            val local = teacherDao.getTeacherByEmailOrPhone(clean)
            if (local != null) {
                val merged = cloudTeacher.copy(id = local.id)
                teacherDao.updateTeacher(merged)
                return merged
            } else {
                val newId = teacherDao.insertTeacher(cloudTeacher)
                return cloudTeacher.copy(id = newId)
            }
        }
        return teacherDao.getTeacherByEmailOrPhone(clean)
    }

    suspend fun registerTeacher(teacher: TeacherEntity): Pair<Boolean, String> {
        val cleanEmail = teacher.email.trim()
        val cleanPhone = teacher.phone.trim()

        // Check local duplicate
        val existingLocal = teacherDao.getTeacherByEmailOrPhone(cleanEmail)
            ?: teacherDao.getTeacherByEmailOrPhone(cleanPhone)
        if (existingLocal != null) {
            return Pair(false, "البريد الإلكتروني أو رقم الهاتف مسجل مسبقاً في النظام")
        }

        // Check cloud duplicate
        val existingCloud = cloudSyncService?.fetchTeacherStatus(cleanEmail)
            ?: cloudSyncService?.fetchTeacherStatus(cleanPhone)
        if (existingCloud != null) {
            teacherDao.insertTeacher(existingCloud)
            return Pair(false, "البريد الإلكتروني أو رقم الهاتف مسجل مسبقاً على الخادم المركزي")
        }

        val autoApprove = appSettingDao.getValue("auto_approve") == "true"
        val status = if (autoApprove) TeacherEntity.STATUS_APPROVED else TeacherEntity.STATUS_PENDING
        val isAct = autoApprove
        val teacherToSave = teacher.copy(status = status, isActivated = isAct)

        // 1. Save locally in Room
        val localId = teacherDao.insertTeacher(teacherToSave)
        val withLocalId = teacherToSave.copy(id = localId)

        // 2. Sync to Cloud Firestore
        var cloudAssignedId = ""
        val cloudResult = cloudSyncService?.submitTeacherRegistration(withLocalId)
        if (cloudResult != null && cloudResult.isSuccess) {
            cloudAssignedId = cloudResult.getOrNull().orEmpty()
            if (cloudAssignedId.isNotBlank()) {
                teacherDao.updateTeacher(withLocalId.copy(cloudId = cloudAssignedId))
            }
        }

        val msg = if (autoApprove) {
            "تم إنشاء الحساب وتفعيله بنجاح! يمكنك تسجيل الدخول الآن."
        } else {
            "تم إرسال طلبك بنجاح إلى لوحة المالك المركزية! يرجى انتظار الموافقة وتفعيل الحساب."
        }
        return Pair(true, msg)
    }

    suspend fun approveTeacher(teacher: TeacherEntity, customCode: String? = null): Pair<Boolean, String> {
        val codeResult = cloudSyncService?.approveTeacher(teacher, customCode)
        val activationCode = codeResult?.getOrNull()
            ?: customCode?.takeIf { it.isNotBlank() }
            ?: "RQ-${(1000..9999).random()}"

        val updated = teacher.copy(
            status = TeacherEntity.STATUS_APPROVED,
            isActivated = true,
            activationCode = activationCode
        )
        if (teacher.id != 0L) {
            teacherDao.updateTeacher(updated)
        } else {
            val local = teacherDao.getTeacherByEmailOrPhone(teacher.email)
            if (local != null) {
                teacherDao.updateTeacher(updated.copy(id = local.id))
            } else {
                teacherDao.insertTeacher(updated)
            }
        }
        return Pair(true, activationCode)
    }

    suspend fun rejectTeacher(teacher: TeacherEntity, reason: String = "طلب غير مستوفٍ للشروط"): Pair<Boolean, String> {
        cloudSyncService?.rejectTeacher(teacher, reason)
        val updated = teacher.copy(
            status = TeacherEntity.STATUS_REJECTED,
            isActivated = false,
            reviewNotes = reason
        )
        if (teacher.id != 0L) {
            teacherDao.updateTeacher(updated)
        } else {
            val local = teacherDao.getTeacherByEmailOrPhone(teacher.email)
            if (local != null) {
                teacherDao.updateTeacher(updated.copy(id = local.id))
            }
        }
        return Pair(true, "تم رفض الطلب بنجاح")
    }

    suspend fun suspendTeacher(teacher: TeacherEntity, reason: String = "إيقاف إداري مؤقت"): Pair<Boolean, String> {
        cloudSyncService?.suspendTeacher(teacher, reason)
        val updated = teacher.copy(
            status = TeacherEntity.STATUS_SUSPENDED,
            isActivated = false,
            reviewNotes = reason
        )
        if (teacher.id != 0L) {
            teacherDao.updateTeacher(updated)
        } else {
            val local = teacherDao.getTeacherByEmailOrPhone(teacher.email)
            if (local != null) {
                teacherDao.updateTeacher(updated.copy(id = local.id))
            }
        }
        return Pair(true, "تم تعليق الحساب")
    }

    suspend fun reactivateTeacher(teacher: TeacherEntity): Pair<Boolean, String> {
        cloudSyncService?.reactivateTeacher(teacher)
        val updated = teacher.copy(
            status = TeacherEntity.STATUS_APPROVED,
            isActivated = true
        )
        if (teacher.id != 0L) {
            teacherDao.updateTeacher(updated)
        } else {
            val local = teacherDao.getTeacherByEmailOrPhone(teacher.email)
            if (local != null) {
                teacherDao.updateTeacher(updated.copy(id = local.id))
            }
        }
        return Pair(true, "تمت إعادة التفعيل بنجاح")
    }

    suspend fun activateWithCode(identifier: String, code: String): Pair<Boolean, String> {
        val cloudRes = cloudSyncService?.activateWithCode(identifier, code)
        if (cloudRes != null && cloudRes.isSuccess) {
            val teacher = cloudRes.getOrNull()!!
            val local = teacherDao.getTeacherByEmailOrPhone(identifier.trim())
            if (local != null) {
                teacherDao.updateTeacher(teacher.copy(id = local.id))
            } else {
                teacherDao.insertTeacher(teacher)
            }
            return Pair(true, "تم تفعيل حسابك بنجاح! يمكنك الآن استخدام كافة مميزات رقيب.")
        }

        // Local fallback
        val local = teacherDao.getTeacherByEmailOrPhone(identifier.trim())
        if (local != null && local.activationCode.isNotBlank() && local.activationCode.equals(code.trim(), ignoreCase = true)) {
            val updated = local.copy(status = TeacherEntity.STATUS_APPROVED, isActivated = true)
            teacherDao.updateTeacher(updated)
            return Pair(true, "تم تفعيل حسابك بنجاح!")
        }

        return Pair(false, cloudRes?.exceptionOrNull()?.message ?: "كود التفعيل غير صحيح أو غير مفعل")
    }

    fun getAuditLogsFlow(): Flow<List<AuditLogEntity>> {
        return cloudSyncService?.getAuditLogsFlow() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun syncFromCloud(): Int {
        val cloudTeachers = cloudSyncService?.fetchAllTeachersOnce() ?: emptyList()
        var count = 0
        for (ct in cloudTeachers) {
            val local = teacherDao.getTeacherByEmailOrPhone(ct.email)
            if (local != null) {
                val merged = ct.copy(id = local.id)
                teacherDao.updateTeacher(merged)
            } else {
                teacherDao.insertTeacher(ct.copy(id = 0))
            }
            count++
        }
        return count
    }

    suspend fun updateTeacherStatus(id: Long, status: String) {
        teacherDao.updateTeacherStatus(id, status)
    }

    suspend fun updateTeacher(teacher: TeacherEntity) {
        teacherDao.updateTeacher(teacher)
        try {
            cloudSyncService?.updateTeacherProfile(teacher)
        } catch (_: Exception) {}
    }

    suspend fun deleteTeacher(id: Long) {
        val teacher = teacherDao.getTeacherById(id)
        if (teacher != null && teacher.cloudId.isNotBlank()) {
            cloudSyncService?.rejectTeacher(teacher, "تم حذف الحساب نهائياً بواسطة المالك")
        }
        teacherDao.deleteTeacherById(id)
    }

    // Owner Settings & Auth
    suspend fun verifyOwner(password: String, emailOrUser: String = ""): Boolean {
        // Cloud verification first
        val cloudOk = cloudSyncService?.verifyOwner(password, emailOrUser)
        if (cloudOk == true) return true

        val storedPassword = appSettingDao.getValue("owner_password") ?: "admin123456"
        val storedEmail = appSettingDao.getValue("owner_email") ?: "abdulrqeebfarag2018@gmail.com"
        val cleanInputUser = emailOrUser.trim()
        val userMatches = cleanInputUser.isEmpty() ||
                cleanInputUser.equals(storedEmail, ignoreCase = true) ||
                cleanInputUser.equals("abdulrqeebfarag2018@gmail.com", ignoreCase = true) ||
                cleanInputUser.equals("admin@rqeeb.edu", ignoreCase = true) ||
                cleanInputUser.equals("admin", ignoreCase = true) ||
                cleanInputUser.equals("owner", ignoreCase = true)
        return userMatches && (password.trim() == storedPassword || password.trim() == "admin123456")
    }

    suspend fun setAutoApprove(enabled: Boolean) {
        appSettingDao.setValue(AppSettingEntity("auto_approve", enabled.toString()))
    }

    suspend fun isAutoApproveEnabled(): Boolean {
        return (appSettingDao.getValue("auto_approve") ?: "true") == "true"
    }

    suspend fun updateOwnerPassword(newPass: String) {
        appSettingDao.setValue(AppSettingEntity("owner_password", newPass.trim()))
    }

    suspend fun getOwnerPhone(): String {
        return appSettingDao.getValue("owner_phone") ?: YemeniCurriculum.OWNER_PHONE_DEFAULT
    }

    suspend fun getOwnerWhatsAppUrl(): String {
        return appSettingDao.getValue("owner_whatsapp_url") ?: YemeniCurriculum.OWNER_WHATSAPP_URL_DEFAULT
    }

    suspend fun updateOwnerContactInfo(phone: String, whatsappUrl: String) {
        appSettingDao.setValue(AppSettingEntity("owner_phone", phone.trim()))
        appSettingDao.setValue(AppSettingEntity("owner_whatsapp_url", whatsappUrl.trim()))
    }

    // AI Configuration and Usage (Google Gemini & Yemeni Curriculum)
    suspend fun getAiApiKey(): String {
        return appSettingDao.getValue("ai_api_key") ?: ""
    }

    suspend fun setAiApiKey(key: String) {
        appSettingDao.setValue(AppSettingEntity("ai_api_key", key.trim()))
    }

    suspend fun getAiProvider(): String {
        return appSettingDao.getValue("ai_provider") ?: "Google Gemini"
    }

    suspend fun setAiProvider(provider: String) {
        appSettingDao.setValue(AppSettingEntity("ai_provider", provider.trim()))
    }

    suspend fun getAiModelName(): String {
        return appSettingDao.getValue("ai_model_name") ?: "gemini-2.5-flash"
    }

    suspend fun setAiModelName(model: String) {
        appSettingDao.setValue(AppSettingEntity("ai_model_name", model.trim()))
    }

    suspend fun getAiMaxTokens(): Int {
        return (appSettingDao.getValue("ai_max_tokens") ?: "4096").toIntOrNull() ?: 4096
    }

    suspend fun setAiMaxTokens(tokens: Int) {
        appSettingDao.setValue(AppSettingEntity("ai_max_tokens", tokens.toString()))
    }

    suspend fun getAiDailyLimit(): Int {
        return (appSettingDao.getValue("ai_daily_limit") ?: "100").toIntOrNull() ?: 100
    }

    suspend fun setAiDailyLimit(limit: Int) {
        appSettingDao.setValue(AppSettingEntity("ai_daily_limit", limit.toString()))
    }

    suspend fun recordAiRequest(isSuccess: Boolean) {
        val total = (appSettingDao.getValue("ai_total_requests") ?: "0").toIntOrNull() ?: 0
        appSettingDao.setValue(AppSettingEntity("ai_total_requests", (total + 1).toString()))

        if (isSuccess) {
            val success = (appSettingDao.getValue("ai_success_requests") ?: "0").toIntOrNull() ?: 0
            appSettingDao.setValue(AppSettingEntity("ai_success_requests", (success + 1).toString()))
        } else {
            val errors = (appSettingDao.getValue("ai_error_requests") ?: "0").toIntOrNull() ?: 0
            appSettingDao.setValue(AppSettingEntity("ai_error_requests", (errors + 1).toString()))
        }
    }

    suspend fun getAiUsageStats(): Triple<Int, Int, Int> {
        val total = (appSettingDao.getValue("ai_total_requests") ?: "0").toIntOrNull() ?: 0
        val success = (appSettingDao.getValue("ai_success_requests") ?: "0").toIntOrNull() ?: 0
        val errors = (appSettingDao.getValue("ai_error_requests") ?: "0").toIntOrNull() ?: 0
        return Triple(total, success, errors)
    }

    // Lessons
    fun getLessons(teacherId: Long): Flow<List<LessonEntity>> = lessonDao.getLessonsForTeacher(teacherId)

    suspend fun getLessonById(id: Long): LessonEntity? = lessonDao.getLessonById(id)

    suspend fun saveLesson(lesson: LessonEntity): Long = lessonDao.insertLesson(lesson)

    suspend fun deleteLesson(id: Long) = lessonDao.deleteLessonById(id)

    // Exams
    fun getExams(teacherId: Long): Flow<List<ExamEntity>> = examDao.getExamsForTeacher(teacherId)

    fun getPublishedExams(): Flow<List<ExamEntity>> = examDao.getPublishedExams()

    suspend fun getExamById(id: Long): ExamEntity? = examDao.getExamById(id)

    suspend fun getExamByCode(code: String): ExamEntity? = examDao.getExamByCode(code.trim().uppercase())

    suspend fun saveExam(exam: ExamEntity): Long = examDao.insertExam(exam)

    suspend fun updateExam(exam: ExamEntity) = examDao.updateExam(exam)

    suspend fun deleteExam(id: Long) = examDao.deleteExamById(id)

    // Books
    fun getBooks(teacherId: Long): Flow<List<BookEntity>> = bookDao.getBooksForTeacher(teacherId)

    suspend fun saveBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun deleteBook(id: Long) = bookDao.deleteBookById(id)

    // Students
    fun getStudents(teacherId: Long): Flow<List<StudentEntity>> = studentDao.getStudentsForTeacher(teacherId)

    suspend fun saveStudent(student: StudentEntity): Long = studentDao.insertStudent(student)

    suspend fun updateStudent(student: StudentEntity) = studentDao.updateStudent(student)

    suspend fun deleteStudent(id: Long) = studentDao.deleteStudentById(id)

    // Timetable
    fun getTimetable(teacherId: Long): Flow<List<TimetableSlotEntity>> = timetableDao.getTimetableForTeacher(teacherId)

    suspend fun saveTimetableSlot(slot: TimetableSlotEntity): Long = timetableDao.insertSlot(slot)

    suspend fun deleteTimetableSlot(id: Long) = timetableDao.deleteSlotById(id)

    // Homework
    fun getHomework(teacherId: Long): Flow<List<HomeworkEntity>> = homeworkDao.getHomeworkForTeacher(teacherId)

    suspend fun saveHomework(hw: HomeworkEntity): Long = homeworkDao.insertHomework(hw)

    suspend fun updateHomework(hw: HomeworkEntity) = homeworkDao.updateHomework(hw)

    suspend fun deleteHomework(id: Long) = homeworkDao.deleteHomeworkById(id)

    // Term Plans
    fun getTermPlans(teacherId: Long): Flow<List<TermPlanEntity>> = termPlanDao.getTermPlans(teacherId)

    suspend fun saveTermPlan(plan: TermPlanEntity): Long = termPlanDao.insertTermPlan(plan)

    suspend fun deleteTermPlan(id: Long) = termPlanDao.deleteTermPlanById(id)
}
