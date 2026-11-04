package com.example.data.repository

import com.example.data.cloud.CloudSyncService
import com.example.data.dao.*
import com.example.data.db.RqeebDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class RqeebRepository(
    private val database: RqeebDatabase,
    private val cloudSyncService: CloudSyncService? = null
) {
    private val teacherDao: TeacherDao = database.teacherDao()
    private val lessonDao: LessonDao = database.lessonDao()
    private val examDao: ExamDao = database.examDao()
    private val bookDao: BookDao = database.bookDao()
    private val studentDao: StudentDao = database.studentDao()
    private val studentAttendanceDao: StudentAttendanceDao = database.studentAttendanceDao()
    private val studentBehaviorDao: StudentBehaviorDao = database.studentBehaviorDao()
    private val communicationLogDao: CommunicationLogDao = database.communicationLogDao()
    private val parentNotificationConfigDao: ParentNotificationConfigDao = database.parentNotificationConfigDao()
    private val timetableDao: TimetableDao = database.timetableDao()
    private val homeworkDao: HomeworkDao = database.homeworkDao()
    private val termPlanDao: TermPlanDao = database.termPlanDao()
    private val appSettingDao: AppSettingDao = database.appSettingDao()
    private val educationalResourceDao: EducationalResourceDao = database.educationalResourceDao()
    private val appAnnouncementDao: AppAnnouncementDao = database.appAnnouncementDao()
    private val appDynamicContentDao: AppDynamicContentDao = database.appDynamicContentDao()
    private val activationCodeDao: ActivationCodeDao = database.activationCodeDao()
    private val smartExplanationDao: SmartExplanationDao = database.smartExplanationDao()
    private val customExamTemplateDao: CustomExamTemplateDao = database.customExamTemplateDao()

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
        val digitsOnly = clean.replace(Regex("[^0-9]"), "")
        val altPhone = if (digitsOnly.startsWith("967")) digitsOnly.substring(3)
                       else if (digitsOnly.startsWith("0")) digitsOnly.substring(1)
                       else digitsOnly

        val local = teacherDao.getTeacherByEmailOrPhone(clean)
            ?: if (altPhone.isNotBlank()) teacherDao.getTeacherByEmailOrPhone(altPhone) else null
            ?: if (digitsOnly.isNotBlank()) teacherDao.getTeacherByEmailOrPhone(digitsOnly) else null

        if (local != null) return local

        val cloudTeacher = cloudSyncService?.fetchTeacherStatus(clean)
            ?: if (altPhone.isNotBlank()) cloudSyncService?.fetchTeacherStatus(altPhone) else null
        if (cloudTeacher != null) {
            val localExisting = teacherDao.getTeacherByEmailOrPhone(cloudTeacher.email)
                ?: teacherDao.getTeacherByEmailOrPhone(cloudTeacher.phone)
            if (localExisting != null) {
                val merged = cloudTeacher.copy(id = localExisting.id)
                teacherDao.updateTeacher(merged)
                return merged
            } else {
                val newId = teacherDao.insertTeacher(cloudTeacher)
                return cloudTeacher.copy(id = newId)
            }
        }
        return null
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
        val cleanId = identifier.trim()
        val cleanCode = code.trim().uppercase()

        // 1. First attempt Cloud Validation on Supabase
        val cloudRes = cloudSyncService?.activateWithCode(cleanId, cleanCode)
        if (cloudRes != null && cloudRes.isSuccess) {
            val teacher = cloudRes.getOrNull()!!
            val local = teacherDao.getTeacherByEmailOrPhone(cleanId)
            if (local != null) {
                teacherDao.updateTeacher(teacher.copy(id = local.id))
            } else {
                teacherDao.insertTeacher(teacher)
            }
            // Mark code as used in local database as well if present
            val localCode = activationCodeDao.getCodeByValue(cleanCode)
            if (localCode != null) {
                activationCodeDao.updateCode(localCode.copy(
                    isUsed = true,
                    usedAt = System.currentTimeMillis(),
                    status = "USED",
                    teacherPhone = cleanId,
                    teacherName = teacher.fullName
                ))
            }
            return Pair(true, "تم تفعيل حسابك بنجاح لمدة عام كامل! يمكنك الآن استخدام كافة مميزات رقيب.")
        }

        // 2. Local Fallback Validation against Activation Codes Table
        val codeEntity = activationCodeDao.getCodeByValue(cleanCode)
        if (codeEntity != null) {
            if (codeEntity.isRevoked || codeEntity.status == "REVOKED") {
                return Pair(false, "كود التفعيل هذا تم إلغاؤه من قِبل إدارة النظام")
            }
            if (codeEntity.isUsed || codeEntity.status == "USED") {
                return Pair(false, "كود التفعيل هذا تم استخدامه مسبقاً ولا يمكن استخدامه مرة أخرى")
            }
            if (codeEntity.expiresAt <= System.currentTimeMillis()) {
                return Pair(false, "كود التفعيل منتهي الصلاحية")
            }
            if (codeEntity.teacherPhone.isNotBlank() && codeEntity.teacherPhone != cleanId) {
                return Pair(false, "هذا الكود مخصص لرقم هاتف آخر (${codeEntity.teacherPhone})")
            }

            // Consume code
            activationCodeDao.updateCode(codeEntity.copy(
                isUsed = true,
                usedAt = System.currentTimeMillis(),
                status = "USED",
                teacherPhone = cleanId,
                teacherName = if (codeEntity.teacherName.isNotBlank() && codeEntity.teacherName != "كود عام متاح") codeEntity.teacherName else "معلم رقيب"
            ))

            // Update or create teacher
            val localTeacher = teacherDao.getTeacherByEmailOrPhone(cleanId)
            if (localTeacher != null) {
                teacherDao.updateTeacher(localTeacher.copy(
                    status = TeacherEntity.STATUS_APPROVED,
                    isActivated = true,
                    activationCode = cleanCode
                ))
            } else {
                teacherDao.insertTeacher(
                    TeacherEntity(
                        fullName = if (codeEntity.teacherName.isNotBlank() && codeEntity.teacherName != "كود عام متاح") codeEntity.teacherName else "أ. معلم رقيب",
                        phone = cleanId,
                        email = if (cleanId.contains("@")) cleanId else "$cleanId@rqeeb.edu",
                        passwordHash = "123456",
                        schoolName = "مدرسة يمنية",
                        governorate = "أمانة العاصمة",
                        directorate = "مديرية التحرير",
                        subject = "الرياضيات",
                        stage = "مرحلة التعليم الأساسي",
                        gradeTaught = "9/ إعدادي",
                        section = "أ",
                        status = TeacherEntity.STATUS_APPROVED,
                        isActivated = true,
                        activationCode = cleanCode
                    )
                )
            }

            return Pair(true, "تم تفعيل حسابك بنجاح بكود الأمان المعتمد لمدة عام كامل!")
        }

        val errMsg = cloudRes?.exceptionOrNull()?.message
        return Pair(false, if (!errMsg.isNullOrBlank()) errMsg else "كود التفعيل غير صحيح أو غير متطابق مع رقم الهاتف")
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

        val storedPassword = appSettingDao.getValue("owner_password") ?: ""
        val storedEmail = appSettingDao.getValue("owner_email") ?: YemeniCurriculum.OWNER_EMAIL_DEFAULT
        val cleanInputUser = emailOrUser.trim()
        val userMatches = cleanInputUser.isEmpty() ||
                cleanInputUser.equals(storedEmail, ignoreCase = true) ||
                cleanInputUser.equals(YemeniCurriculum.OWNER_EMAIL_DEFAULT, ignoreCase = true) ||
                cleanInputUser.equals("abdulrqeebfarag2018@gmail.com", ignoreCase = true) ||
                cleanInputUser.equals("admin", ignoreCase = true) ||
                cleanInputUser.equals("owner", ignoreCase = true)

        return userMatches && storedPassword.isNotBlank() && password.trim() == storedPassword
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

    // Students & Parent Follow-up
    fun getStudents(teacherId: Long): Flow<List<StudentEntity>> = studentDao.getStudentsByTeacher(teacherId)

    suspend fun getStudentById(teacherId: Long, studentId: Long): StudentEntity? = studentDao.getStudentById(teacherId, studentId)

    fun getStudentByIdFlow(teacherId: Long, studentId: Long): Flow<StudentEntity?> = studentDao.getStudentByIdFlow(teacherId, studentId)

    fun getStudentsByGradeAndSection(teacherId: Long, grade: String, section: String): Flow<List<StudentEntity>> =
        studentDao.getStudentsByGradeAndSection(teacherId, grade, section)

    fun getFollowedStudents(teacherId: Long): Flow<List<StudentEntity>> = studentDao.getFollowedStudents(teacherId)

    fun searchStudents(teacherId: Long, query: String): Flow<List<StudentEntity>> = studentDao.searchStudents(teacherId, query)

    fun getStudentsCount(teacherId: Long): Flow<Int> = studentDao.getStudentsCount(teacherId)

    fun getLinkedStudentsCount(teacherId: Long): Flow<Int> = studentDao.getLinkedStudentsCount(teacherId)

    suspend fun insertStudent(student: StudentEntity): Long = studentDao.insertStudent(student)

    suspend fun updateStudent(student: StudentEntity) = studentDao.updateStudent(student)

    suspend fun deleteStudent(student: StudentEntity) = studentDao.deleteStudent(student)

    // Student Attendance
    fun getStudentAttendance(studentId: Long): Flow<List<StudentAttendanceEntity>> = studentAttendanceDao.getAttendanceByStudent(studentId)

    fun getAttendanceByDate(teacherId: Long, date: String): Flow<List<StudentAttendanceEntity>> = studentAttendanceDao.getAttendanceByDate(teacherId, date)

    fun getAttendanceCountByStatus(studentId: Long, status: String): Flow<Int> = studentAttendanceDao.getCountByStatus(studentId, status)

    suspend fun insertAttendance(attendance: StudentAttendanceEntity): Long = studentAttendanceDao.insertAttendance(attendance)

    suspend fun updateAttendance(attendance: StudentAttendanceEntity) = studentAttendanceDao.updateAttendance(attendance)

    // Student Behavior
    fun getStudentBehaviorNotes(studentId: Long): Flow<List<StudentBehaviorEntity>> = studentBehaviorDao.getBehaviorNotesByStudent(studentId)

    suspend fun insertBehaviorNote(note: StudentBehaviorEntity): Long = studentBehaviorDao.insertBehaviorNote(note)

    suspend fun deleteBehaviorNote(note: StudentBehaviorEntity) = studentBehaviorDao.deleteBehaviorNote(note)

    // Communication Logs
    fun getCommunicationLogsByTeacher(teacherId: Long): Flow<List<CommunicationLogEntity>> = communicationLogDao.getLogsByTeacher(teacherId)

    fun getCommunicationLogsByStudent(studentId: Long): Flow<List<CommunicationLogEntity>> = communicationLogDao.getLogsByStudent(studentId)

    fun getTotalLogsCount(teacherId: Long): Flow<Int> = communicationLogDao.getTotalLogsCount(teacherId)

    suspend fun insertCommunicationLog(log: CommunicationLogEntity): Long = communicationLogDao.insertLog(log)

    suspend fun updateCommunicationLog(log: CommunicationLogEntity) = communicationLogDao.updateLog(log)

    // Parent Notification Config
    suspend fun getParentNotificationConfig(teacherId: Long): ParentNotificationConfigEntity {
        return parentNotificationConfigDao.getConfig(teacherId) ?: ParentNotificationConfigEntity(teacherId = teacherId)
    }

    fun getParentNotificationConfigFlow(teacherId: Long): Flow<ParentNotificationConfigEntity?> = parentNotificationConfigDao.getConfigFlow(teacherId)

    suspend fun saveParentNotificationConfig(config: ParentNotificationConfigEntity) {
        parentNotificationConfigDao.insertOrUpdateConfig(config)
    }

    // Timetable
    fun getTimetable(teacherId: Long): Flow<List<TimetableSlotEntity>> = timetableDao.getTimetable(teacherId)

    suspend fun insertTimetableSlot(slot: TimetableSlotEntity) = timetableDao.insertSlot(slot)

    suspend fun clearTimetable(teacherId: Long) = timetableDao.clearTimetable(teacherId)

    // Homework
    fun getHomework(teacherId: Long): Flow<List<HomeworkEntity>> = homeworkDao.getHomeworkByTeacher(teacherId)

    suspend fun insertHomework(homework: HomeworkEntity) = homeworkDao.insertHomework(homework)

    suspend fun deleteHomework(homework: HomeworkEntity) = homeworkDao.deleteHomework(homework)

    // ==============================================================================
    // Manual Teacher Management (Owner Dashboard)
    // ==============================================================================
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
    ): Result<Pair<String, String>> {
        val res = cloudSyncService?.addTeacherManually(
            fullName, phone, schoolName, governorate, district, subject, stage, grade, section, curriculumEdition
        )
        if (res != null && res.isSuccess) {
            val (cloudId, code) = res.getOrThrow()
            val teacher = TeacherEntity(
                fullName = fullName,
                phone = phone,
                email = "$phone@rqeeb.edu",
                passwordHash = "",
                schoolName = schoolName,
                governorate = governorate,
                directorate = district,
                subject = subject,
                stage = stage,
                gradeTaught = grade,
                section = section,
                curriculumEdition = curriculumEdition,
                status = TeacherEntity.STATUS_APPROVED,
                isActivated = false,
                cloudId = cloudId,
                activationCode = code,
                reviewNotes = "تمت الإضافة يدويًا بواسطة المالك"
            )
            teacherDao.insertTeacher(teacher)
            activationCodeDao.insertCode(
                ActivationCodeEntity(
                    id = UUID.randomUUID().toString(),
                    code = code,
                    teacherId = cloudId,
                    teacherName = fullName,
                    teacherPhone = phone,
                    isUsed = false,
                    status = "AVAILABLE"
                )
            )
            return Result.success(Pair(cloudId, code))
        }
        return res ?: Result.failure(Exception("الخدمة السحابية غير متوفرة"))
    }

    suspend fun updateTeacherManually(teacher: TeacherEntity): Result<Unit> {
        teacherDao.updateTeacher(teacher)
        val res = cloudSyncService?.updateTeacherManually(teacher)
        return res ?: Result.success(Unit)
    }

    suspend fun deleteTeacherManually(teacher: TeacherEntity): Result<Unit> {
        teacherDao.deleteTeacherById(teacher.id)
        if (teacher.cloudId.isNotBlank()) {
            return cloudSyncService?.deleteTeacherManually(teacher.cloudId) ?: Result.success(Unit)
        }
        return Result.success(Unit)
    }

    // ==============================================================================
    // Activation Codes Management (Owner Dashboard)
    // ==============================================================================
    fun getAllActivationCodes(): Flow<List<ActivationCodeEntity>> = activationCodeDao.getAllCodes()

    suspend fun ensureOfficialCodesLoaded(): Int {
        val currentCount = activationCodeDao.getCodesCount()
        if (currentCount == 0) {
            val official = com.example.data.model.OfficialActivationCodes.createInitialEntities()
            activationCodeDao.insertCodes(official)
            return official.size
        }
        return currentCount
    }

    suspend fun syncActivationCodes(): Result<List<ActivationCodeEntity>> {
        ensureOfficialCodesLoaded()
        val res = cloudSyncService?.fetchAllActivationCodesResult()
        if (res != null && res.isSuccess) {
            val list = res.getOrThrow()
            if (list.isNotEmpty()) {
                activationCodeDao.insertCodes(list)
            }
            return Result.success(list)
        }
        return res ?: Result.failure(Exception("الخدمة السحابية غير متوفرة"))
    }

    suspend fun createSingleActivationCode(
        teacherName: String,
        teacherPhone: String,
        durationDays: Int = 365,
        customCode: String? = null
    ): Result<ActivationCodeEntity> {
        val res = cloudSyncService?.createSingleActivationCode(teacherName, teacherPhone, durationDays, customCode)
        if (res != null && res.isSuccess) {
            val codeEntity = res.getOrThrow()
            activationCodeDao.insertCode(codeEntity)
            return Result.success(codeEntity)
        }
        // Local generation fallback
        val days = if (durationDays <= 0) 365 else durationDays
        val codeStr = customCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
            ?: "RQ-${(1000..9999).random()}-${('A'..'Z').random()}"
        val localEntity = ActivationCodeEntity(
            id = "local-${java.util.UUID.randomUUID()}",
            code = codeStr,
            teacherName = teacherName.trim(),
            teacherPhone = teacherPhone.trim(),
            durationDays = days,
            isUsed = false,
            isRevoked = false,
            status = "AVAILABLE",
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (days * 24L * 3600 * 1000)
        )
        activationCodeDao.insertCode(localEntity)
        return Result.success(localEntity)
    }

    suspend fun seedOfficial100Codes(): Result<Int> {
        val official = com.example.data.model.OfficialActivationCodes.createInitialEntities()
        activationCodeDao.insertCodes(official)
        val cloudRes = cloudSyncService?.seedOfficial100CodesToCloud()
        if (cloudRes != null && cloudRes.isSuccess) {
            syncActivationCodes()
            return cloudRes
        }
        return Result.success(official.size)
    }

    suspend fun generateBatchCodes(count: Int = 100, prefix: String = "RQ"): Result<Int> {
        val res = cloudSyncService?.generateBatchCodes(count, prefix)
        syncActivationCodes()
        return res ?: Result.failure(Exception("الخدمة السحابية غير متوفرة"))
    }

    suspend fun revokeActivationCode(codeId: String, reason: String): Result<Unit> {
        val res = cloudSyncService?.revokeActivationCode(codeId, reason)
        // Mark revoked locally as well
        activationCodeDao.getAllCodes()
        syncActivationCodes()
        return res ?: Result.failure(Exception("الخدمة السحابية غير متوفرة"))
    }

    suspend fun reissueActivationCode(teacher: TeacherEntity): Result<String> {
        if (teacher.cloudId.isBlank()) return Result.failure(Exception("معرف السحابة مفقود"))
        val res = cloudSyncService?.reissueActivationCode(teacher.cloudId)
        if (res != null && res.isSuccess) {
            val newCode = res.getOrThrow()
            teacherDao.updateTeacher(teacher.copy(activationCode = newCode, isActivated = false, status = TeacherEntity.STATUS_APPROVED))
            syncActivationCodes()
            return Result.success(newCode)
        }
        return res ?: Result.failure(Exception("الخدمة السحابية غير متوفرة"))
    }

    // ==============================================================================
    // Educational Content & Resources Library
    // ==============================================================================
    fun getAllEducationalResources(): Flow<List<EducationalResourceEntity>> = educationalResourceDao.getAllResources()

    fun getTeacherEducationalResources(grade: String, subject: String): Flow<List<EducationalResourceEntity>> =
        educationalResourceDao.getResourcesForTeacher(grade, subject)

    suspend fun syncEducationalResources(grade: String? = null, subject: String? = null): List<EducationalResourceEntity> {
        val list = cloudSyncService?.fetchEducationalResources(grade, subject) ?: emptyList()
        if (list.isNotEmpty()) {
            educationalResourceDao.insertResources(list)
        }
        return list
    }

    suspend fun saveEducationalResource(res: EducationalResourceEntity): Result<Unit> {
        educationalResourceDao.insertResource(res)
        val cloudRes = cloudSyncService?.saveEducationalResource(res)
        syncEducationalResources()
        return cloudRes ?: Result.success(Unit)
    }

    suspend fun deleteEducationalResource(res: EducationalResourceEntity): Result<Unit> {
        educationalResourceDao.deleteResourceById(res.id)
        if (res.cloudId.isNotBlank()) {
            educationalResourceDao.deleteResourceByCloudId(res.cloudId)
            return cloudSyncService?.deleteEducationalResource(res.cloudId) ?: Result.success(Unit)
        }
        return Result.success(Unit)
    }

    // ==============================================================================
    // App Announcements & Alerts
    // ==============================================================================
    fun getActiveAnnouncements(): Flow<List<AppAnnouncementEntity>> = appAnnouncementDao.getActiveAnnouncements()

    fun getAllAnnouncements(): Flow<List<AppAnnouncementEntity>> = appAnnouncementDao.getAllAnnouncements()

    suspend fun syncAnnouncements(grade: String = "ALL", subject: String = "ALL"): List<AppAnnouncementEntity> {
        val list = cloudSyncService?.fetchAnnouncements(grade, subject) ?: emptyList()
        if (list.isNotEmpty()) {
            appAnnouncementDao.insertAnnouncements(list)
        }
        return list
    }

    suspend fun saveAnnouncement(ann: AppAnnouncementEntity): Result<Unit> {
        appAnnouncementDao.insertAnnouncement(ann)
        val res = cloudSyncService?.saveAnnouncement(ann)
        syncAnnouncements()
        return res ?: Result.success(Unit)
    }

    suspend fun deleteAnnouncement(ann: AppAnnouncementEntity): Result<Unit> {
        appAnnouncementDao.deleteAnnouncementById(ann.id)
        if (ann.cloudId.isNotBlank()) {
            appAnnouncementDao.deleteAnnouncementByCloudId(ann.cloudId)
            return cloudSyncService?.deleteAnnouncement(ann.cloudId) ?: Result.success(Unit)
        }
        return Result.success(Unit)
    }

    // ==============================================================================
    // App Dynamic Content & Support Contacts
    // ==============================================================================
    fun getAllDynamicContent(): Flow<List<AppDynamicContentEntity>> = appDynamicContentDao.getAllContent()

    suspend fun syncDynamicContent(category: String? = null): List<AppDynamicContentEntity> {
        val list = cloudSyncService?.fetchDynamicContent(category) ?: emptyList()
        if (list.isNotEmpty()) {
            appDynamicContentDao.insertAll(list)
        }
        return list
    }

    suspend fun saveDynamicContent(content: AppDynamicContentEntity): Result<Unit> {
        appDynamicContentDao.setContent(content)
        val res = cloudSyncService?.saveDynamicContent(content)
        syncDynamicContent()
        return res ?: Result.success(Unit)
    }

    suspend fun getSupportWhatsApp(): String {
        val fromDb = appDynamicContentDao.getContentByKey("owner_whatsapp")?.contentValue
        if (!fromDb.isNullOrBlank()) return fromDb
        return appSettingDao.getValue("owner_whatsapp_url") ?: YemeniCurriculum.OWNER_WHATSAPP_URL_DEFAULT
    }

    suspend fun getSupportPhone(): String {
        val fromDb = appDynamicContentDao.getContentByKey("owner_phone")?.contentValue
        if (!fromDb.isNullOrBlank()) return fromDb
        return appSettingDao.getValue("owner_phone") ?: YemeniCurriculum.OWNER_PHONE_DEFAULT
    }

    fun getActivationCodesFlow(): Flow<List<ActivationCodeEntity>> = getAllActivationCodes()
    fun getEducationalResourcesFlow(): Flow<List<EducationalResourceEntity>> = getAllEducationalResources()
    fun getAnnouncementsFlow(): Flow<List<AppAnnouncementEntity>> = getAllAnnouncements()
    fun getActiveAnnouncementsFlow(): Flow<List<AppAnnouncementEntity>> = getActiveAnnouncements()

    suspend fun getDynamicContentValue(key: String, defaultValue: String = ""): String {
        val fromDb = appDynamicContentDao.getContentByKey(key)?.contentValue
        if (!fromDb.isNullOrBlank()) return fromDb
        return appSettingDao.getValue(key) ?: defaultValue
    }

    suspend fun changeOwnerPin(currentPin: String, newPin: String): Boolean {
        if (!verifyOwner(currentPin)) return false
        appSettingDao.setValue(AppSettingEntity("owner_password", newPin))
        return true
    }

    // Smart Explanation Methods
    fun getSmartExplanationsFlow(teacherId: Long): Flow<List<SmartExplanationEntity>> =
        smartExplanationDao.getExplanationsByTeacher(teacherId)

    suspend fun getSmartExplanationById(id: Long): SmartExplanationEntity? =
        smartExplanationDao.getExplanationById(id)

    suspend fun insertSmartExplanation(explanation: SmartExplanationEntity): Long =
        smartExplanationDao.insertExplanation(explanation)

    suspend fun deleteSmartExplanation(explanation: SmartExplanationEntity) =
        smartExplanationDao.deleteExplanation(explanation)

    suspend fun deleteSmartExplanationById(id: Long) =
        smartExplanationDao.deleteExplanationById(id)

    // Custom Exam Template Methods
    fun getCustomExamTemplatesFlow(teacherId: Long): Flow<List<CustomExamTemplateEntity>> =
        customExamTemplateDao.getTemplatesByTeacher(teacherId)

    suspend fun getCustomExamTemplateById(id: Long): CustomExamTemplateEntity? =
        customExamTemplateDao.getTemplateById(id)

    suspend fun insertCustomExamTemplate(template: CustomExamTemplateEntity): Long =
        customExamTemplateDao.insertTemplate(template)

    suspend fun deleteCustomExamTemplate(template: CustomExamTemplateEntity) =
        customExamTemplateDao.deleteTemplate(template)

    // Yemeni Educational Books Methods (Supabase Storage & AI Text Indexing)
    fun getBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBooksByGrade(grade: String): Flow<List<BookEntity>> = bookDao.getBooksByGrade(grade)

    fun getBooksBySubjectAndGrade(subject: String, grade: String): Flow<List<BookEntity>> =
        bookDao.getBooksBySubjectAndGrade(subject, grade)

    suspend fun getBookById(id: Long): BookEntity? = bookDao.getBookById(id)

    suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun deleteBookById(id: Long) = bookDao.deleteBookById(id)

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
