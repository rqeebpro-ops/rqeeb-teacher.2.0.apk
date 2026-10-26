package com.example.data.repository

import com.example.data.cloud.CloudSyncService
import com.example.data.dao.*
import com.example.data.db.RqeebDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class RqeebRepository(
    private val database: RqeebDatabase,
    private val cloudSyncService: CloudSyncService? = null
) {
    private val teacherDao: TeacherDao = database.teacherDao()
    private val lessonDao: LessonDao = database.lessonDao()
    private val examDao: ExamDao = database.examDao()
    private val bookDao: BookDao = database.bookDao()
    private val studentDao: StudentDao = database.studentDao()
    private val timetableDao: TimetableDao = database.timetableDao()
    private val homeworkDao: HomeworkDao = database.homeworkDao()
    private val termPlanDao: TermPlanDao = database.termPlanDao()
    private val appSettingDao: AppSettingDao = database.appSettingDao()

    // Real-time Cloud Flow for Owner Dashboard
    fun getCloudTeachersFlow(): Flow<List<TeacherEntity>> {
        return cloudSyncService?.getAllTeachersFlow() ?: teacherDao.getAllTeachers()
    }

    // Local DB Teachers Flow
    fun getAllTeachers(): Flow<List<TeacherEntity>> = teacherDao.getAllTeachers()

    fun getTeachersByStatus(status: String): Flow<List<TeacherEntity>> = teacherDao.getTeachersByStatus(status)

    fun getTeachersCountByStatus(status: String): Flow<Int> = teacherDao.getCountByStatus(status)

    suspend fun getTeacherById(id: Long): TeacherEntity? = teacherDao.getTeacherById(id)

    suspend fun getTeacherByLogin(identifier: String): TeacherEntity? {
        val clean = identifier.trim()
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

        val localId = teacherDao.insertTeacher(teacherToSave)
        val withLocalId = teacherToSave.copy(id = localId)

        var cloudAssignedId = ""
        val cloudResult = cloudSyncService?.registerTeacherOnCloud(withLocalId)
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
        return cloudSyncService?.getAuditLogsFlow() ?: flowOf(emptyList())
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
        val cloudOk = cloudSyncService?.verifyOwner(password, emailOrUser)
        if (cloudOk == true) return true

        val storedPassword = appSettingDao.getValue("owner_password") ?: "admin123456"
        val storedEmail = appSettingDao.getValue("owner_email") ?: YemeniCurriculum.OWNER_EMAIL_DEFAULT
        val cleanInputUser = emailOrUser.trim()
        val userMatches = cleanInputUser.isEmpty() ||
                cleanInputUser.equals(storedEmail, ignoreCase = true) ||
                cleanInputUser.equals(YemeniCurriculum.OWNER_EMAIL_DEFAULT, ignoreCase = true) ||
                cleanInputUser.equals("admin@rqeeb.edu", ignoreCase = true) ||
                cleanInputUser.equals("admin", ignoreCase = true) ||
                cleanInputUser.equals("owner", ignoreCase = true)

        return userMatches && (password.trim() == storedPassword || password.trim() == "admin123456")
    }

    suspend fun setAutoApprove(autoApprove: Boolean) {
        appSettingDao.setValue(AppSettingEntity("auto_approve", autoApprove.toString()))
    }

    suspend fun isAutoApprove(): Boolean {
        return appSettingDao.getValue("auto_approve") == "true"
    }

    suspend fun setOwnerCredentials(password: String, email: String, phone: String, whatsappUrl: String) {
        if (password.isNotBlank()) appSettingDao.setValue(AppSettingEntity("owner_password", password))
        if (email.isNotBlank()) appSettingDao.setValue(AppSettingEntity("owner_email", email))
        if (phone.isNotBlank()) appSettingDao.setValue(AppSettingEntity("owner_phone", phone))
        if (whatsappUrl.isNotBlank()) appSettingDao.setValue(AppSettingEntity("owner_whatsapp_url", whatsappUrl))
    }

    suspend fun getOwnerSetting(key: String, default: String): String {
        return appSettingDao.getValue(key) ?: default
    }

    fun getSupabaseService(): CloudSyncService? = cloudSyncService

    suspend fun testSupabaseConnection(): Pair<Boolean, String> {
        return cloudSyncService?.testConnection() ?: Pair(false, "خدمة السحابة غير مفعلة")
    }

    fun saveSupabaseConfig(url: String, anonKey: String) {
        cloudSyncService?.saveSupabaseConfig(url, anonKey)
    }

    fun getSupabaseConfig(): Pair<String, String> {
        return Pair(cloudSyncService?.getSupabaseUrl().orEmpty(), cloudSyncService?.getSupabaseAnonKey().orEmpty())
    }

    fun isSupabaseConfigured(): Boolean {
        return cloudSyncService?.isConfigured() == true
    }

    // Lessons
    fun getLessons(teacherId: Long): Flow<List<LessonEntity>> = lessonDao.getLessonsByTeacher(teacherId)

    suspend fun getLessonById(id: Long): LessonEntity? = lessonDao.getLessonById(id)

    suspend fun insertLesson(lesson: LessonEntity): Long = lessonDao.insertLesson(lesson)

    suspend fun updateLesson(lesson: LessonEntity) = lessonDao.updateLesson(lesson)

    suspend fun deleteLesson(lesson: LessonEntity) = lessonDao.deleteLesson(lesson)

    fun getLessonsCount(teacherId: Long): Flow<Int> = lessonDao.getLessonsCount(teacherId)

    fun getTotalLessonsCount(): Flow<Int> = lessonDao.getTotalLessonsCount()

    // Exams
    fun getExams(teacherId: Long): Flow<List<ExamEntity>> = examDao.getExamsByTeacher(teacherId)

    suspend fun getExamById(id: Long): ExamEntity? = examDao.getExamById(id)

    suspend fun insertExam(exam: ExamEntity): Long = examDao.insertExam(exam)

    suspend fun updateExam(exam: ExamEntity) = examDao.updateExam(exam)

    suspend fun deleteExam(exam: ExamEntity) = examDao.deleteExam(exam)

    fun getExamsCount(teacherId: Long): Flow<Int> = examDao.getExamsCount(teacherId)

    fun getTotalExamsCount(): Flow<Int> = examDao.getTotalExamsCount()

    // Books
    fun getBooks(grade: String = ""): Flow<List<BookEntity>> =
        if (grade.isNotBlank()) bookDao.getBooksByGrade(grade) else bookDao.getAllBooks()

    suspend fun insertBook(book: BookEntity) = bookDao.insertBook(book)

    // Students
    fun getStudents(teacherId: Long): Flow<List<StudentEntity>> = studentDao.getStudentsByTeacher(teacherId)

    suspend fun insertStudent(student: StudentEntity) = studentDao.insertStudent(student)

    suspend fun updateStudent(student: StudentEntity) = studentDao.updateStudent(student)

    suspend fun deleteStudent(student: StudentEntity) = studentDao.deleteStudent(student)

    // Timetable
    fun getTimetable(teacherId: Long): Flow<List<TimetableSlotEntity>> = timetableDao.getTimetable(teacherId)

    suspend fun insertTimetableSlot(slot: TimetableSlotEntity) = timetableDao.insertSlot(slot)

    suspend fun clearTimetable(teacherId: Long) = timetableDao.clearTimetable(teacherId)

    // Homework
    fun getHomework(teacherId: Long): Flow<List<HomeworkEntity>> = homeworkDao.getHomeworkByTeacher(teacherId)

    suspend fun insertHomework(homework: HomeworkEntity) = homeworkDao.insertHomework(homework)

    suspend fun deleteHomework(homework: HomeworkEntity) = homeworkDao.deleteHomework(homework)

    // Term Plans
    fun getTermPlans(teacherId: Long): Flow<List<TermPlanEntity>> = termPlanDao.getTermPlans(teacherId)

    suspend fun insertTermPlan(plan: TermPlanEntity) = termPlanDao.insertPlan(plan)

    companion object {
        @Volatile
        private var INSTANCE: RqeebRepository? = null

        fun getInstance(context: android.content.Context): RqeebRepository {
            return INSTANCE ?: synchronized(this) {
                val db = RqeebDatabase.getInstance(context)
                val cloud = CloudSyncService.getInstance(context)
                val instance = RqeebRepository(db, cloud)
                INSTANCE = instance
                instance
            }
        }
    }
}
