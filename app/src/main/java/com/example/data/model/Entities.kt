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
    val expectedOutcomes: String = "",
    val targetKnowledgeAndSkills: String = "",
    val prerequisites: String = "",
    val introduction: String = "",
    val teachingSteps: String = "",
    val teacherStudentActivities: String = "",
    val discussionQuestions: String = "",
    val diagnosticAssessment: String = "",
    val formativeAssessment: String = "",
    val summativeAssessment: String = "",
    val homework: String = "",
    val remedialActivities: String = "",
    val enrichmentActivities: String = "",
    val timeDistribution: String = "",
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
    val difficulty: String = "متوسط",
    val unitName: String = "",
    val hasAnswerKey: Boolean = true,
    val hasDiagrams: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    FILL_BLANKS,
    MATCHING,
    DEFINITIONS,
    EXPLAIN_WHY,
    COMPARE,
    ENUMERATE,
    ESSAY,
    MATH_PHYSICS_PROBLEM,
    TABLE_GRAPH_READING,
    LABEL_DIAGRAM,
    COMPLETE_DIAGRAM,
    PRACTICAL_APPLIED;

    fun toArabicName(): String {
        return when (this) {
            MULTIPLE_CHOICE -> "اختيار من متعدد"
            TRUE_FALSE -> "صح أو خطأ"
            FILL_BLANKS -> "أكمل الفراغ"
            MATCHING -> "صل / مطابقة"
            DEFINITIONS -> "عرف المصطلحات"
            EXPLAIN_WHY -> "علل / بيّن السبب"
            COMPARE -> "قارن بين"
            ENUMERATE -> "اذكر / عدّد"
            ESSAY -> "سؤال مقالي"
            MATH_PHYSICS_PROBLEM -> "مسائل رياضية وفيزيائية"
            TABLE_GRAPH_READING -> "قراءة جداول ورسوم بيانية"
            LABEL_DIAGRAM -> "اكتب البيانات على الرسم"
            COMPLETE_DIAGRAM -> "أكمل الرسم الناقص"
            PRACTICAL_APPLIED -> "أسئلة تطبيقية وعملية"
        }
    }
}

data class ExamQuestion(
    val id: Int,
    val type: QuestionType,
    val text: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val score: Int = 1,
    val explanation: String = "",
    val diagramId: String? = null,
    val diagramType: String? = null,
    val diagramInstruction: String? = null
)

/**
 * Textbook reference in Yemeni Educational Library (with Supabase Storage & AI Text Indexing)
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val stage: String = "التعليم الأساسي", // المرحلة الدراسية (أساسي / ثانوي)
    val grade: String, // الصف الدراسي (الأول إلى التاسع الأساسي، العاشر إلى 12)
    val subject: String, // المادة الدراسية
    val term: String = "الفصل الدراسي الأول", // الفصل الدراسي
    val unitName: String = "", // الوحدة (اختياري)
    val editionYear: String = "2024-2025م", // سنة الطبعة
    val description: String = "", // وصف وتفاصيل الكتاب
    val fileName: String = "", // اسم الملف الحقيقي
    val fileSize: Long = 0L, // حجم الملف بالبايت
    val storagePath: String = "", // المسار داخل Supabase Storage
    val fileUrl: String = "", // رابط الوصول المباشر للملف
    val uploadedAt: Long = System.currentTimeMillis(),
    val processingStatus: String = "COMPLETED", // PENDING, PROCESSING, COMPLETED, FAILED
    val processingProgress: Int = 100, // 0 to 100%
    val extractedTextJson: String = "", // Indexed JSON content for AI Lesson Prep (RAG)
    val isPublished: Boolean = true, // حالة النشر والمعاينة
    val edition: String = "طبعة المنهج المعتمد",
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
    val schoolNumber: String = "", // الرقم المدرسي
    val grade: String,
    val section: String = "أ",
    val schoolName: String = "",
    val parentName: String = "", // اسم ولي الأمر الرئيسي
    val parentPhone: String = "", // رقم واتساب ولي الأمر الرئيسي
    val parentRelationship: String = "أب", // صلة القرابة (أب، أم، ولي أمر، أخ)
    val secondaryParentName: String = "", // ولي أمر إضافي
    val secondaryParentPhone: String = "",
    val secondaryParentRelationship: String = "أم",
    val isFollowedUp: Boolean = true, // تتبع ولي الأمر
    val academicYear: String = "2024-2025م",

    // Academic Scores & Evaluations
    val monthlyTestScore: Double = 0.0,
    val termScore: Double = 0.0,
    val finalExamScore: Double = 0.0,
    val homeworkScore: Double = 0.0,
    val activityScore: Double = 0.0,
    val examScore: Double = 0.0,
    val generalRating: String = "جيد جداً", // ممتاز، جيد جداً، جيد، مقبول، يحتاج تحسين
    val strengths: String = "", // نقاط القوة
    val weaknesses: String = "", // نقاط الضعف والمواد المستهدفة للتحسين
    val notes: String = ""
)

/**
 * Daily Attendance & Discipline Log for Students
 */
@Entity(tableName = "student_attendance")
data class StudentAttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val teacherId: Long,
    val date: String, // YYYY-MM-DD or Hijri/Gregorian string
    val status: String = "حاضر", // حاضر، غائب، متأخر، مستأذن
    val minutesLate: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Behavioral & Pedagogical Notes for Students
 */
@Entity(tableName = "student_behavior")
data class StudentBehaviorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val teacherId: Long,
    val date: String,
    val category: String = "سلوكي", // سلوكي، تحصيل دراسي، واجبات، تميز وتشجيع
    val title: String,
    val details: String = "",
    val severity: String = "ملاحظة عامة", // إيجابي / تفوق، تنبيه خفيف، ملاحظة هامة
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Parent WhatsApp Communication History Log
 */
@Entity(tableName = "communication_logs")
data class CommunicationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val teacherId: Long,
    val studentName: String,
    val parentName: String,
    val parentPhone: String,
    val messageType: String, // تقرير شامل، غياب، درجات، ملاحظة سلوكية، واجب، تميز، إلخ
    val messageSummary: String,
    val fullText: String,
    val hasPdfAttachment: Boolean = false,
    val pdfPath: String = "",
    val status: String = "DRAFTED", // DRAFTED, OPENED_WHATSAPP, SENT_CONFIRMED
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Parent Instant Notification Config Settings for Teacher
 */
@Entity(tableName = "parent_notification_configs")
data class ParentNotificationConfigEntity(
    @PrimaryKey
    val teacherId: Long,
    val notifyAbsence: Boolean = true,
    val notifyLowGrade: Boolean = true,
    val notifyExcellence: Boolean = true,
    val notifyMissingHomework: Boolean = true,
    val notifyTardiness: Boolean = true,
    val notifyBehaviorIssue: Boolean = true,
    val notifyImprovement: Boolean = true,
    val notifyUpcomingExam: Boolean = true
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

/**
 * Educational Resource (PDF, Images, Worksheets, Exams) uploaded by Owner
 */
@Entity(tableName = "educational_resources")
data class EducationalResourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cloudId: String = "",
    val title: String,
    val description: String = "",
    val resourceType: String = "PDF", // PDF, IMAGE, WORKSHEET, EXAM, OTHER
    val subject: String = "الرياضيات",
    val grade: String = "عام",
    val unitName: String = "",
    val lessonName: String = "",
    val fileUrl: String = "",
    val fileSizeBytes: Long = 0L,
    val isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * App Announcement & Alert
 */
@Entity(tableName = "app_announcements")
data class AppAnnouncementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cloudId: String = "",
    val title: String,
    val content: String,
    val type: String = "BANNER", // BANNER, POPUP, NOTICE, UPDATE
    val targetGrade: String = "ALL",
    val targetSubject: String = "ALL",
    val isActive: Boolean = true,
    val priority: Int = 0,
    val linkUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * App Dynamic Content (Instructions, Support Contacts, Curriculum Notes)
 */
@Entity(tableName = "app_dynamic_content")
data class AppDynamicContentEntity(
    @PrimaryKey
    val contentKey: String,
    val category: String = "GENERAL", // GENERAL, SUPPORT, CURRICULUM, INSTRUCTIONS
    val title: String,
    val contentValue: String,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Activation Code representation for Owner management
 */
@Entity(tableName = "activation_codes")
data class ActivationCodeEntity(
    @PrimaryKey
    val id: String, // UUID
    val code: String,
    val teacherId: String? = null,
    val teacherName: String = "",
    val teacherPhone: String = "",
    val durationDays: Int = 365,
    val isUsed: Boolean = false,
    val isRevoked: Boolean = false,
    val revokedReason: String = "",
    val status: String = "AVAILABLE", // AVAILABLE, USED, REVOKED, EXPIRED
    val createdAt: Long = System.currentTimeMillis(),
    val usedAt: Long? = null,
    val expiresAt: Long = System.currentTimeMillis() + (365L * 24 * 3600 * 1000)
)

/**
 * Smart Explanation Entity for saving deep pedagogical lesson explanations
 */
@Entity(tableName = "smart_explanations")
data class SmartExplanationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val subject: String,
    val grade: String,
    val unitName: String,
    val lessonTitle: String,
    val expansionLevel: String, // عميق، أعمق، موسع جداً
    val dialectStyle: String, // بيضاء، صنعانية، فصحى مبسطة
    val studentLevel: String, // مبتدئ، متوسط، متقدم، شامل
    val rawJson: String,
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ConceptExplanation(
    val title: String,
    val deepExplanation: String,
    val whyAndHow: String,
    val causeAndEffect: String,
    val commonMisconception: String,
    val studentQuestion: String,
    val studentQuestionAnswer: String
)

data class YemeniAnalogy(
    val title: String,
    val analogyText: String,
    val spokenDialect: String,
    val explanationLink: String
)

data class YemeniExample(
    val title: String,
    val contextType: String,
    val description: String,
    val howToApply: String
)

data class DeliveryStep(
    val stepNumber: Int,
    val stepTitle: String,
    val stepDetail: String,
    val teacherScript: String
)

data class TeachingMethodGuide(
    val strategyName: String,
    val executionSteps: String,
    val expectedOutcome: String
)

data class RemediationItem(
    val difficulty: String,
    val simplerExplanation: String,
    val visualOrPhysicalDemonstration: String,
    val scaffoldQuestions: List<String>
)

data class InteractiveQuestion(
    val question: String,
    val expectedAnswer: String,
    val teacherFeedbackIfWrong: String,
    val followUpQuestion: String
)

data class GradedExample(
    val level: String,
    val problem: String,
    val stepByStepSolution: String,
    val pedagogicalTip: String
)

data class SmartExplanationResult(
    val subject: String,
    val grade: String,
    val lessonTitle: String,
    val expansionLevel: String,
    val dialectStyle: String,
    val concepts: List<ConceptExplanation>,
    val yemeniAnalogies: List<YemeniAnalogy>,
    val yemeniExamples: List<YemeniExample>,
    val deliveryGuide: List<DeliveryStep>,
    val teachingMethods: List<TeachingMethodGuide>,
    val remediation: List<RemediationItem>,
    val interactiveQuestions: List<InteractiveQuestion>,
    val gradedExamples: List<GradedExample>,
    val blackboardPlan: String,
    val classroomScript: String,
    val teacherEnrichment: String,
    val isFromGeminiApi: Boolean = false
)

/**
 * Custom Exam Template Entity for saving teacher-defined paper layouts
 */
@Entity(tableName = "custom_exam_templates")
data class CustomExamTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherId: Long,
    val templateName: String,
    val countryName: String = "الجمهورية اليمنية",
    val ministryName: String = "وزارة التربية والتعليم والبحث العلمي",
    val directorateName: String = "مكتب التربية والتعليم",
    val schoolName: String = "",
    val logoType: String = "EAGLE", // EAGLE, CUSTOM_URI, MINISTRY
    val logoUri: String = "",
    val primaryColorHex: String = "#006874",
    val fontScale: Float = 1.0f,
    val showSummaryTable: Boolean = true,
    val headerStyleJson: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

