package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers ORDER BY createdAt DESC")
    fun getAllTeachers(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE id = :id")
    suspend fun getTeacherById(id: Long): TeacherEntity?

    @Query("SELECT * FROM teachers WHERE email = :identifier OR phone = :identifier LIMIT 1")
    suspend fun getTeacherByEmailOrPhone(identifier: String): TeacherEntity?

    @Query("SELECT * FROM teachers WHERE email = :email LIMIT 1")
    suspend fun getTeacherByEmail(email: String): TeacherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity): Long

    @Update
    suspend fun updateTeacher(teacher: TeacherEntity)

    @Query("UPDATE teachers SET status = :status WHERE id = :id")
    suspend fun updateTeacherStatus(id: Long, status: String)

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteTeacherById(id: Long)

    @Query("SELECT COUNT(*) FROM teachers")
    fun getTotalTeachersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM teachers WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getLessonsForTeacher(teacherId: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLessonById(id: Long): LessonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Query("DELETE FROM lessons WHERE id = :id")
    suspend fun deleteLessonById(id: Long)

    @Query("SELECT COUNT(*) FROM lessons")
    fun getTotalLessonsCount(): Flow<Int>
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getExamsForTeacher(teacherId: Long): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getExamById(id: Long): ExamEntity?

    @Query("SELECT * FROM exams WHERE examCode = :code LIMIT 1")
    suspend fun getExamByCode(code: String): ExamEntity?

    @Query("SELECT * FROM exams WHERE isPublishedToStudents = 1 ORDER BY createdAt DESC")
    fun getPublishedExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: Long)

    @Query("SELECT COUNT(*) FROM exams")
    fun getTotalExamsCount(): Flow<Int>
}

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getBooksForTeacher(teacherId: Long): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE teacherId = :teacherId ORDER BY className ASC, studentName ASC")
    fun getStudentsForTeacher(teacherId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE teacherId = :teacherId AND className = :className ORDER BY studentName ASC")
    fun getStudentsByClass(teacherId: Long, className: String): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: Long)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable WHERE teacherId = :teacherId ORDER BY dayOfWeek ASC, periodNumber ASC")
    fun getTimetableForTeacher(teacherId: Long): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlotEntity): Long

    @Query("DELETE FROM timetable WHERE id = :id")
    suspend fun deleteSlotById(id: Long)
}

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun getHomeworkForTeacher(teacherId: Long): Flow<List<HomeworkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(homework: HomeworkEntity): Long

    @Update
    suspend fun updateHomework(homework: HomeworkEntity)

    @Query("DELETE FROM homework WHERE id = :id")
    suspend fun deleteHomeworkById(id: Long)
}

@Dao
interface TermPlanDao {
    @Query("SELECT * FROM term_plans WHERE teacherId = :teacherId ORDER BY weekNumber ASC")
    fun getTermPlans(teacherId: Long): Flow<List<TermPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTermPlan(plan: TermPlanEntity): Long

    @Query("DELETE FROM term_plans WHERE id = :id")
    suspend fun deleteTermPlanById(id: Long)
}

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(setting: AppSettingEntity)
}
