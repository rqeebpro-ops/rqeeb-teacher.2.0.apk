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
    val gradeTaught: String = "", // الصف الذي يدرسه (مثل 9/ إعدادي)
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
    val unitName: String = "",
    val lessonDate: String = "",
    val periodNumber: String = "الحصة الأولى",
    val periodsCount: Int = 1,
    val studentLevel: String = "متوسط / اعتيادي",
    val preparationType: String = "تحضير يومي قياسي",
    val cognitiveObjectives: String = "",
    val skillObjectives: String = "",
    val affectiveObjectives: String = "",
    val introduction: String = "",
    val teachingSteps: String = "",
    val formativeAssessment: String = "",
    val homework: String = "",
    val teacherNotes: String = "",
    val teacherStyles: String = "",
    val teachingMethods: String = "",
    val skillsAndValues: String = "",
    val teachingAids: String = "",
    val textbookReference: String = "",
    val curriculumEdition: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Exam Entity generated according to Yemeni Ministry formats
 */
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val title: String,
    val subject: String,
    val grade: String,
    val examType: String = "اختبار شهري",
    val durationMinutes: Int = 45,
    val totalScore: Int = 20,
    val questionsJson: String = "",
    val schoolName: String = "",
    val term: String = "الفصل الدراسي الأول",
    val academicYear: String = "1446-1447هـ",
    val createdAt: Long = System.currentTimeMillis()
)

enum class QuestionType {
    MULTIPLE_CHOICE, TRUE_FALSE, ESSAY, FILL_BLANKS, MATCHING
}

data class ExamQuestion(
    val id: Int,
    val type: QuestionType,
    val text: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val score: Int = 1,
    val explanation: String = "",
    val diagramId: String? = null
)

/**
 * Textbook reference in Yemeni Educational Library
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val grade: String,
    val edition: String,
    val summary: String = "",
    val unitsCount: Int = 6,
    val pdfAssetPath: String = ""
)

/**
 * Student in Yemeni teacher's record
 */
@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val fullName: String,
    val seatNumber: String = "",
    val grade: String,
    val section: String = "أ",
    val monthlyTestScore: Double = 0.0,
    val homeworkScore: Double = 0.0,
    val activityScore: Double = 0.0,
    val examScore: Double = 0.0,
    val notes: String = ""
)

/**
 * Timetable Slot
 */
@Entity(tableName = "timetable")
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val dayOfWeek: String, // الأحد، الإثنين، ...
    val periodIndex: Int, // 1 to 7
    val subject: String,
    val grade: String,
    val section: String = "أ"
)

/**
 * Daily Homework assigned
 */
@Entity(tableName = "homework")
data class HomeworkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val title: String,
    val subject: String,
    val grade: String,
    val section: String = "",
    val description: String,
    val dueDate: String,
    val textbookPage: String = "",
    val exerciseNumbers: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Annual / Term Distribution Plan
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
    val topicTitle: String,
    val lessonsCount: Int = 4,
    val executionNotes: String = ""
)

/**
 * App Settings & Owner configuration
 */
@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
