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
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE grade = :grade")
    fun getBooksByGrade(grade: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE teacherId = :teacherId ORDER BY seatNumber ASC")
    fun getStudentsByTeacher(teacherId: Long): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)
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
