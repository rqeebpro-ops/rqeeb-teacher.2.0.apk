package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers ORDER BY createdAt DESC")
    fun getAllTeachers(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE status = :status ORDER BY createdAt DESC")
    fun getTeachersByStatus(status: String): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    suspend fun getTeacherById(id: Long): TeacherEntity?

    @Query("SELECT * FROM teachers WHERE email = :email OR phone = :phone LIMIT 1")
    suspend fun getTeacherByEmailOrPhone(email: String, phone: String = email): TeacherEntity?

    @Query("SELECT COUNT(*) FROM teachers WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM teachers")
    fun getTotalCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity): Long

    @Update
    suspend fun updateTeacher(teacher: TeacherEntity)

    @Query("UPDATE teachers SET status = :status, isActivated = (:status = 'APPROVED') WHERE id = :id")
    suspend fun updateTeacherStatus(id: Long, status: String)

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteTeacherById(id: Long)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getLessonsByTeacher(teacherId: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun getLessonById(id: Long): LessonEntity?

    @Query("SELECT COUNT(*) FROM lessons WHERE teacherId = :teacherId")
    fun getLessonsCount(teacherId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM lessons")
    fun getTotalLessonsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Update
    suspend fun updateLesson(lesson: LessonEntity)

    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getExamsByTeacher(teacherId: Long): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: Long): ExamEntity?

    @Query("SELECT COUNT(*) FROM exams WHERE teacherId = :teacherId")
    fun getExamsCount(teacherId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM exams")
    fun getTotalExamsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)
}

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY uploadedAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE grade = :grade ORDER BY uploadedAt DESC")
    fun getBooksByGrade(grade: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE (subject = :subject OR :subject = '' OR :subject = 'ALL') AND (grade = :grade OR :grade = '' OR :grade = 'ALL') ORDER BY uploadedAt DESC")
    fun getBooksBySubjectAndGrade(subject: String, grade: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE teacherId = :teacherId ORDER BY fullName ASC")
    fun getStudentsByTeacher(teacherId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE teacherId = :teacherId AND id = :studentId LIMIT 1")
    suspend fun getStudentById(teacherId: Long, studentId: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE teacherId = :teacherId AND id = :studentId LIMIT 1")
    fun getStudentByIdFlow(teacherId: Long, studentId: Long): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE teacherId = :teacherId AND grade = :grade AND section = :section ORDER BY fullName ASC")
    fun getStudentsByGradeAndSection(teacherId: Long, grade: String, section: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE teacherId = :teacherId AND isFollowedUp = 1 ORDER BY fullName ASC")
    fun getFollowedStudents(teacherId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE teacherId = :teacherId AND (fullName LIKE '%' || :query || '%' OR schoolNumber LIKE '%' || :query || '%' OR parentPhone LIKE '%' || :query || '%') ORDER BY fullName ASC")
    fun searchStudents(teacherId: Long, query: String): Flow<List<StudentEntity>>

    @Query("SELECT COUNT(*) FROM students WHERE teacherId = :teacherId")
    fun getStudentsCount(teacherId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM students WHERE teacherId = :teacherId AND parentPhone != ''")
    fun getLinkedStudentsCount(teacherId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)
}

@Dao
interface StudentAttendanceDao {
    @Query("SELECT * FROM student_attendance WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getAttendanceByStudent(studentId: Long): Flow<List<StudentAttendanceEntity>>

    @Query("SELECT * FROM student_attendance WHERE teacherId = :teacherId AND date = :date")
    fun getAttendanceByDate(teacherId: Long, date: String): Flow<List<StudentAttendanceEntity>>

    @Query("SELECT COUNT(*) FROM student_attendance WHERE studentId = :studentId AND status = :status")
    fun getCountByStatus(studentId: Long, status: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: StudentAttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: StudentAttendanceEntity)
}

@Dao
interface StudentBehaviorDao {
    @Query("SELECT * FROM student_behavior WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getBehaviorNotesByStudent(studentId: Long): Flow<List<StudentBehaviorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviorNote(note: StudentBehaviorEntity): Long

    @Delete
    suspend fun deleteBehaviorNote(note: StudentBehaviorEntity)
}

@Dao
interface CommunicationLogDao {
    @Query("SELECT * FROM communication_logs WHERE teacherId = :teacherId ORDER BY timestamp DESC")
    fun getLogsByTeacher(teacherId: Long): Flow<List<CommunicationLogEntity>>

    @Query("SELECT * FROM communication_logs WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getLogsByStudent(studentId: Long): Flow<List<CommunicationLogEntity>>

    @Query("SELECT COUNT(*) FROM communication_logs WHERE teacherId = :teacherId")
    fun getTotalLogsCount(teacherId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CommunicationLogEntity): Long

    @Update
    suspend fun updateLog(log: CommunicationLogEntity)
}

@Dao
interface ParentNotificationConfigDao {
    @Query("SELECT * FROM parent_notification_configs WHERE teacherId = :teacherId LIMIT 1")
    suspend fun getConfig(teacherId: Long): ParentNotificationConfigEntity?

    @Query("SELECT * FROM parent_notification_configs WHERE teacherId = :teacherId LIMIT 1")
    fun getConfigFlow(teacherId: Long): Flow<ParentNotificationConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: ParentNotificationConfigEntity)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable WHERE teacherId = :teacherId ORDER BY periodIndex ASC")
    fun getTimetable(teacherId: Long): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlotEntity): Long

    @Query("DELETE FROM timetable WHERE teacherId = :teacherId")
    suspend fun clearTimetable(teacherId: Long)
}

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getHomeworkByTeacher(teacherId: Long): Flow<List<HomeworkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(homework: HomeworkEntity): Long

    @Delete
    suspend fun deleteHomework(homework: HomeworkEntity)
}

@Dao
interface TermPlanDao {
    @Query("SELECT * FROM term_plans WHERE teacherId = :teacherId ORDER BY weekNumber ASC")
    fun getTermPlans(teacherId: Long): Flow<List<TermPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: TermPlanEntity): Long
}

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(setting: AppSettingEntity)
}

@Dao
interface EducationalResourceDao {
    @Query("SELECT * FROM educational_resources ORDER BY createdAt DESC")
    fun getAllResources(): Flow<List<EducationalResourceEntity>>

    @Query("SELECT * FROM educational_resources WHERE (grade = :grade OR grade = 'عام') AND (subject = :subject OR subject = 'عام') AND isPublic = 1 ORDER BY createdAt DESC")
    fun getResourcesForTeacher(grade: String, subject: String): Flow<List<EducationalResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: EducationalResourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResources(resources: List<EducationalResourceEntity>)

    @Query("DELETE FROM educational_resources WHERE id = :id")
    suspend fun deleteResourceById(id: Long)

    @Query("DELETE FROM educational_resources WHERE cloudId = :cloudId")
    suspend fun deleteResourceByCloudId(cloudId: String)
}

@Dao
interface AppAnnouncementDao {
    @Query("SELECT * FROM app_announcements WHERE isActive = 1 ORDER BY priority DESC, createdAt DESC")
    fun getActiveAnnouncements(): Flow<List<AppAnnouncementEntity>>

    @Query("SELECT * FROM app_announcements ORDER BY priority DESC, createdAt DESC")
    fun getAllAnnouncements(): Flow<List<AppAnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AppAnnouncementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<AppAnnouncementEntity>)

    @Query("DELETE FROM app_announcements WHERE id = :id")
    suspend fun deleteAnnouncementById(id: Long)

    @Query("DELETE FROM app_announcements WHERE cloudId = :cloudId")
    suspend fun deleteAnnouncementByCloudId(cloudId: String)
}

@Dao
interface AppDynamicContentDao {
    @Query("SELECT * FROM app_dynamic_content ORDER BY contentKey ASC")
    fun getAllContent(): Flow<List<AppDynamicContentEntity>>

    @Query("SELECT * FROM app_dynamic_content WHERE contentKey = :key LIMIT 1")
    suspend fun getContentByKey(key: String): AppDynamicContentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setContent(content: AppDynamicContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contents: List<AppDynamicContentEntity>)
}

@Dao
interface ActivationCodeDao {
    @Query("SELECT * FROM activation_codes ORDER BY createdAt DESC")
    fun getAllCodes(): Flow<List<ActivationCodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCode(code: ActivationCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodes(codes: List<ActivationCodeEntity>)

    @Query("SELECT COUNT(*) FROM activation_codes")
    suspend fun getCodesCount(): Int

    @Query("SELECT * FROM activation_codes WHERE UPPER(code) = UPPER(:code) LIMIT 1")
    suspend fun getCodeByValue(code: String): ActivationCodeEntity?

    @Update
    suspend fun updateCode(code: ActivationCodeEntity)
}

@Dao
interface SmartExplanationDao {
    @Query("SELECT * FROM smart_explanations WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getExplanationsByTeacher(teacherId: Long): Flow<List<SmartExplanationEntity>>

    @Query("SELECT * FROM smart_explanations WHERE id = :id LIMIT 1")
    suspend fun getExplanationById(id: Long): SmartExplanationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExplanation(explanation: SmartExplanationEntity): Long

    @Delete
    suspend fun deleteExplanation(explanation: SmartExplanationEntity)

    @Query("DELETE FROM smart_explanations WHERE id = :id")
    suspend fun deleteExplanationById(id: Long)
}

@Dao
interface CustomExamTemplateDao {
    @Query("SELECT * FROM custom_exam_templates WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getTemplatesByTeacher(teacherId: Long): Flow<List<CustomExamTemplateEntity>>

    @Query("SELECT * FROM custom_exam_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): CustomExamTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: CustomExamTemplateEntity): Long

    @Delete
    suspend fun deleteTemplate(template: CustomExamTemplateEntity)
}

