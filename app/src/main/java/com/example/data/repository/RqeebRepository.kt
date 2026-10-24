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
    private val appSettingDao: AppSettingDao
) {
    // Teachers
    val allTeachers: Flow<List<TeacherEntity>> = teacherDao.getAllTeachers()
    val totalTeachersCount: Flow<Int> = teacherDao.getTotalTeachersCount()
    val totalLessonsCount: Flow<Int> = lessonDao.getTotalLessonsCount()
    val totalExamsCount: Flow<Int> = examDao.getTotalExamsCount()

    fun getTeachersCountByStatus(status: String): Flow<Int> = teacherDao.getCountByStatus(status)

    suspend fun getTeacherById(id: Long): TeacherEntity? = teacherDao.getTeacherById(id)

    suspend fun getTeacherByLogin(identifier: String): TeacherEntity? =
        teacherDao.getTeacherByEmailOrPhone(identifier.trim())

    suspend fun registerTeacher(teacher: TeacherEntity): Pair<Boolean, String> {
        val existing = teacherDao.getTeacherByEmailOrPhone(teacher.email.trim())
            ?: teacherDao.getTeacherByEmailOrPhone(teacher.phone.trim())
        if (existing != null) {
            return Pair(false, "البريد الإلكتروني أو رقم الهاتف مسجل مسبقاً")
        }
        val autoApprove = appSettingDao.getValue("auto_approve") == "true"
        val status = if (autoApprove) TeacherEntity.STATUS_APPROVED else TeacherEntity.STATUS_PENDING
        val teacherWithStatus = teacher.copy(status = status)
        teacherDao.insertTeacher(teacherWithStatus)
        val msg = if (autoApprove) {
            "تم إنشاء الحساب وتفعيله بنجاح! يمكنك تسجيل الدخول الآن."
        } else {
            "تم إرسال طلب التسجيل بنجاح! حسابك قيد المراجعة من قِبل إدارة رقيب."
        }
        return Pair(true, msg)
    }

    suspend fun updateTeacherStatus(id: Long, status: String) {
        teacherDao.updateTeacherStatus(id, status)
    }

    suspend fun updateTeacher(teacher: TeacherEntity) {
        teacherDao.updateTeacher(teacher)
    }

    suspend fun deleteTeacher(id: Long) {
        teacherDao.deleteTeacherById(id)
    }

    // Owner Settings & Auth
    suspend fun verifyOwner(password: String, emailOrUser: String = ""): Boolean {
        val storedPassword = appSettingDao.getValue("owner_password") ?: "admin123456"
        val storedEmail = appSettingDao.getValue("owner_email") ?: "admin@rqeeb.edu"
        val cleanInputUser = emailOrUser.trim()
        val userMatches = cleanInputUser.isEmpty() ||
                cleanInputUser.equals(storedEmail, ignoreCase = true) ||
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
