package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Teacher account entity specifically configured for the Yemeni school system
 */
@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val phone: String,
    val email: String,
    val passwordHash: String,
    val schoolName: String,
    val governorate: String,
    val directorate: String = "", // المديرية التعليمية في اليمن
    val subject: String,
    val stage: String, // مرحلة التعليم الأساسي / مرحلة التعليم الثانوي
    val gradeTaught: String = "", // الصف الذي يدرسه
    val section: String = "", // الشعبة الدراسية (أ، ب، ج، ...) أو مخصصة
    val curriculumEdition: String = "طبعة صنعاء (وزارة التربية والتعليم - صنعاء)", // طبعة صنعاء / طبعة عدن
    val academicYear: String = "1446-1447هـ / 2024-2025م",
    val status: String = STATUS_APPROVED, // APPROVED, PENDING, SUSPENDED, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val cloudId: String = "",
    val activationCode: String = "",
    val isActivated: Boolean = (status == STATUS_APPROVED),
    val deviceModel: String = "",
    val reviewNotes: String = ""
) {
    companion object {
        const val STATUS_APPROVED = "APPROVED"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SUSPENDED = "SUSPENDED"
        const val STATUS_REJECTED = "REJECTED"
    }
}

/**
 * Audit log entry for administrative actions performed by the Owner
 */
data class AuditLogEntity(
    val id: String = "",
    val action: String = "", // APPROVE, REJECT, SUSPEND, REACTIVATE, CODE_GENERATED
    val targetTeacherId: String = "",
    val targetTeacherName: String = "",
    val performedBy: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)

/**
 * Prepared Lesson Entity according to the official Yemeni Lesson Plan template
 */
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val title: String,
    val subject: String,
    val grade: String,
    val durationMinutes: Int = 45,
    val schoolName: String = "",
    val directorate: String = "",
    val governorate: String = "",
    val term: String = "الفصل الدراسي الأول",
    val unitNumber: String = "",
    val lessonNumber: String = "",
    val bookPages: String = "",
    val period: String = "الحصة الأولى",
    val objectives: String,
    val introduction: String,
    val previousKnowledge: String = "",
    val detailedExplanation: String,
    val teacherActivities: String,
    val studentActivities: String,
    val strategies: String,
    val teachingMaterials: String = "",
    val formativeAssessment: String,
    val finalAssessment: String = "",
    val questionsAndAnswers: String = "",
    val lessonSummary: String,
    val homework: String,
    val timeAllocation: String = "",
    val teacherNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Generated or Manually Edited Exam Entity
 */
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val title: String,
    val subject: String,
    val grade: String,
    val difficulty: String = "متوسط", // سهل, متوسط, متقدم
    val durationMinutes: Int = 45,
    val totalMarks: Int = 20,
    val examCode: String = "",
    val questionsJson: String, // JSON representation of questions
    val instructions: String = "",
    val isPublishedToStudents: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Educational Library Book/Curriculum Entity (Yemeni textbooks)
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val title: String,
    val subject: String,
    val grade: String,
    val edition: String = "طبعة صنعاء", // طبعة صنعاء / طبعة عدن
    val unit: String = "",
    val lesson: String = "",
    val pages: String = "",
    val description: String,
    val extractedText: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Term Plan Entity (الخطة الفصلية وتوزيع المنهج على أسابيع الفصل الدراسي)
 */
@Entity(tableName = "term_plans")
data class TermPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val subject: String,
    val grade: String,
    val term: String,
    val weekNumber: Int,
    val unitTitle: String,
    val lessonTitle: String,
    val objectives: String,
    val assessmentPlan: String
)

/**
 * Student Entity for Attendance and Grades
 */
@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val className: String,
    val studentName: String,
    val attendanceStatus: String = ATTENDANCE_PRESENT, // PRESENT, ABSENT, LATE
    val quizScore: Double = 0.0,
    val homeworkScore: Double = 0.0,
    val examScore: Double = 0.0,
    val maxQuizScore: Double = 10.0,
    val maxHomeworkScore: Double = 10.0,
    val maxExamScore: Double = 20.0,
    val notes: String = ""
) {
    companion object {
        const val ATTENDANCE_PRESENT = "PRESENT"
        const val ATTENDANCE_ABSENT = "ABSENT"
        const val ATTENDANCE_LATE = "LATE"
    }

    val totalScore: Double get() = quizScore + homeworkScore + examScore
    val totalMaxScore: Double get() = maxQuizScore + maxHomeworkScore + maxExamScore
    val percentage: Int get() = if (totalMaxScore > 0) ((totalScore / totalMaxScore) * 100).toInt() else 0
}

/**
 * Weekly Timetable Slot
 */
@Entity(tableName = "timetable")
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val dayOfWeek: Int, // 0: الأحد, 1: الاثنين, 2: الثلاثاء, 3: الأربعاء, 4: الخميس
    val periodNumber: Int, // 1 to 7
    val subject: String,
    val className: String,
    val room: String = ""
)

/**
 * Homework Assignment
 */
@Entity(tableName = "homework")
data class HomeworkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val className: String,
    val title: String,
    val description: String,
    val dueDate: String,
    val totalMarks: Int = 10,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Application Settings Key-Value Store
 */
@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
