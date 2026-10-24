package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Educational Diagram / Schematic Model
 */
data class DiagramInfo(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "PHYSICS_CIRCUIT", // BIOLOGY, PHYSICS_CIRCUIT, GEOMETRY, CHEMISTRY, GEOGRAPHY, CUSTOM
    val svgContent: String = "", // High-fidelity SVG graphics string
    val labelsJson: String = "{}", // Key-value map of part labels e.g. {"1": "بطارية V=12V", "2": "مقاومة R=4Ω"}
    val hideLabelsForQuiz: Boolean = false, // When true, labels are replaced with question marks or numbers (1, 2, 3)
    val widthDp: Int = 220,
    val heightDp: Int = 160,
    val customNotes: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("category", category)
            put("svgContent", svgContent)
            put("labelsJson", labelsJson)
            put("hideLabelsForQuiz", hideLabelsForQuiz)
            put("widthDp", widthDp)
            put("heightDp", heightDp)
            put("customNotes", customNotes)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DiagramInfo {
            return DiagramInfo(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                category = json.optString("category", "PHYSICS_CIRCUIT"),
                svgContent = json.optString("svgContent", ""),
                labelsJson = json.optString("labelsJson", "{}"),
                hideLabelsForQuiz = json.optBoolean("hideLabelsForQuiz", false),
                widthDp = json.optInt("widthDp", 220),
                heightDp = json.optInt("heightDp", 160),
                customNotes = json.optString("customNotes", "")
            )
        }
    }
}

/**
 * Strategy for generating question variants for students
 */
enum class VariantStrategy(val titleAr: String, val descriptionAr: String) {
    CHANGE_NUMBERS("تغيير الأرقام والمعطيات", "تغيير القيم والعمليات الحسابية مع ثبات المهارة والحل النموذجي"),
    REPHRASE("تغيير صياغة السؤال", "إعادة الصياغة اللغوية مع بقاء المعنى والهدف المعرفي"),
    SCENARIOS("تغيير الأمثلة والسيناريوهات", "تطبيق نفس المفهوم على سياقات واقعية وبيئية يمنية متنوعة"),
    SHUFFLE_OPTIONS("تغيير ترتيب الخيارات", "إعادة ترتيب الخيارات والمشتتات وتحديث مفتاح الحل بدقة"),
    PARALLEL_SKILL("إنشاء أسئلة متكافئة المهارة", "صياغة أسئلة موازية تقيس نفس الهدف التعليمي بالمستوى ذاته"),
    HYBRID_ALL("مزيج متكامل من الخيارات", "تنويع المعطيات والصياغات لإنتاج نسخ متباينة ومنيعة ضد الغش")
}

/**
 * Question Model for Exams (Multiple Choice, True/False, Short Answer, Fill Blanks, Essay)
 */
data class ExamQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val questionText: String,
    val type: QuestionType,
    val options: List<String> = emptyList(), // For MULTIPLE_CHOICE
    val correctAnswer: String,
    val explanation: String = "",
    val marks: Int = 2,
    val diagram: DiagramInfo? = null,
    val variantLabel: String = "", // e.g. "النسخة A", "النسخة B", "النسخة 1"...
    val verificationStatus: String = "VERIFIED", // "VERIFIED", "NEEDS_REVIEW", "PENDING"
    val verificationNote: String = "" // Notes on verification (e.g. "تم التحقق رياضياً: 2س + 6 = 18 => س = 6")
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("questionText", questionText)
            put("type", type.name)
            put("options", JSONArray(options))
            put("correctAnswer", correctAnswer)
            put("explanation", explanation)
            put("marks", marks)
            if (diagram != null) {
                put("diagram", diagram.toJson())
            }
            put("variantLabel", variantLabel)
            put("verificationStatus", verificationStatus)
            put("verificationNote", verificationNote)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ExamQuestion {
            val opts = mutableListOf<String>()
            val jsonArray = json.optJSONArray("options")
            if (jsonArray != null) {
                for (i in 0 until jsonArray.length()) {
                    opts.add(jsonArray.getString(i))
                }
            }
            val diagramObj = json.optJSONObject("diagram")
            val diag = if (diagramObj != null) DiagramInfo.fromJson(diagramObj) else null

            return ExamQuestion(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                questionText = json.optString("questionText", ""),
                type = runCatching { QuestionType.valueOf(json.optString("type", QuestionType.MULTIPLE_CHOICE.name)) }.getOrDefault(QuestionType.MULTIPLE_CHOICE),
                options = opts,
                correctAnswer = json.optString("correctAnswer", ""),
                explanation = json.optString("explanation", ""),
                marks = json.optInt("marks", 2),
                diagram = diag,
                variantLabel = json.optString("variantLabel", ""),
                verificationStatus = json.optString("verificationStatus", "VERIFIED"),
                verificationNote = json.optString("verificationNote", "")
            )
        }

        fun parseListFromJson(jsonString: String): List<ExamQuestion> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<ExamQuestion>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun listToJsonString(questions: List<ExamQuestion>): String {
            val array = JSONArray()
            questions.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}

/**
 * Question Variant Set container (A single seed question and all student versions)
 */
data class QuestionVariantSet(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalQuestion: ExamQuestion,
    val subject: String,
    val grade: String,
    val topic: String,
    val difficulty: String = "متوسط",
    val variationType: VariantStrategy = VariantStrategy.CHANGE_NUMBERS,
    val variants: List<ExamQuestion> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class QuestionType(val titleAr: String) {
    MULTIPLE_CHOICE("اختيار من متعدد"),
    TRUE_FALSE("صح أو خطأ"),
    FILL_BLANKS("أكمل الفراغ"),
    SHORT_ANSWER("أسئلة مباشرة وإجابة قصيرة"),
    ESSAY("سؤال مقالي"),
    GIVE_REASON("علّل / اذكر السبب"),
    MENTION("اذكر / عدّد"),
    COMPARE("قارن بين"),
    REORDER("رتّب الخطوات أو المفاهيم"),
    MATH_PROBLEM("مسألة حسابية وتطبيقية"),
    APPLICATION_INFERENCE("تطبيق واستنتاج"),
    DIAGRAM_TABLE("سؤال مرتبط برسم أو جدول")
}

/**
 * Mind Map Node representation
 */
data class MindMapNode(
    val title: String,
    val details: String = "",
    val children: List<MindMapNode> = emptyList()
)

/**
 * AI Tool Option
 */
enum class AiToolType(val titleAr: String, val descriptionAr: String, val iconName: String) {
    SIMPLIFY_CONCEPT("تبسيط مفهوم صعب", "شرح بأسلوب سلس ومجازي ملائم لفهم الطلاب", "Lightbulb"),
    STEP_BY_STEP("شرح تفصيلي خطوة بخطوة", "تفكيك الدرس أو المسألة إلى خطوات منطقية متسلسلة", "FormatListNumbered"),
    WHY_IT_WORKS("لماذا تعمل هذه القاعدة؟", "توضيح الأساس المنطقي والرياضي وراء القانون أو القاعدة", "Psychology"),
    GENERATE_EXERCISES("توليد تمارين وأمثلة تطبيقية", "أمثلة متدرجة من السهل إلى المتقدم مع حلول نموذجية", "Assignment"),
    PREDICT_EXAM_QUESTIONS("توقع أسئلة الامتحانات", "أهم الأسئلة الوزارية والقياسية المتكررة حول الموضوع", "Quiz"),
    REAL_WORLD_CONNECTION("ربط الدرس بالواقع العملي", "أمثلة حية من الحياة اليومية والتقنية لتشويق الطلاب", "Public"),
    AUDIO_SCRIPT("إعداد بودكاست تعليمي", "سيناريو حواري شيق لشرح الدرس صوتياً", "Mic"),
    TEACHER_ASSISTANT("المساعد التعليمي الذكي", "مستشارك التربوي للإجابة عن أي استفسار وتخطيط الحصص", "SmartToy")
}
