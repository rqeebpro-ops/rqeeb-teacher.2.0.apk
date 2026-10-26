package com.example.data.ai

import android.content.Context
import com.example.BuildConfig
import com.example.data.model.ExamQuestion
import com.example.data.model.QuestionType
import com.example.data.model.YemeniCurriculum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {

    companion object {
        @Volatile
        private var INSTANCE: GeminiService? = null

        fun getInstance(@Suppress("UNUSED_PARAMETER") context: Context): GeminiService {
            return INSTANCE ?: synchronized(this) {
                val instance = GeminiService()
                INSTANCE = instance
                instance
            }
        }
    }

    fun isConfigured(): Boolean {
        return try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = keyField.get(null) as? String
            val hasValidKey: Boolean = if (key != null) key.isNotBlank() && key != "MY_GEMINI_API_KEY" else false
            hasValidKey
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun generateLessonObjectives(
        subject: String,
        grade: String,
        unitName: String,
        lessonTitle: String
    ): Triple<String, String, String> = withContext(Dispatchers.IO) {
        val displayGrade = YemeniCurriculum.toDisplayGrade(grade)
        val cognitive = "1. أن يتعرف الطالب على المفاهيم والمصطلحات الأساسية لدرس ($lessonTitle) في مادة $subject لصف $displayGrade.\n" +
                "2. أن يستنتج الطالب القواعد والنظريات المرتبطة بموضوع الدرس بدقة.\n" +
                "3. أن يفسر الطالب العلاقات الرياضية/العلمية/اللغوية المطروحة في الدرس."

        val skill = "1. أن يطبق الطالب خطوات الحل والتحليل على مسائل وأمثلة متنوعة.\n" +
                "2. أن يميز الطالب بين الحالات المتشابهة بدقة عالية.\n" +
                "3. أن يشارك في حل التدريبات الصفيّة في كراسة النشاط."

        val affective = "1. أن يقدّر الطالب أهمية موضوع ($lessonTitle) وتطبيقاته في الحياة اليومية.\n" +
                "2. أن يتحلى الطالب بروح التعاون والمشاركة الإيجابية مع زملائه والمعلم.\n" +
                "3. الاعتزاز بالهوية اليمنية والتفوق العلمي لخدمة الوطن."

        Triple(cognitive, skill, affective)
    }

    suspend fun generateExamQuestions(
        subject: String,
        grade: String,
        topic: String,
        examType: String,
        count: Int,
        difficulty: String
    ): List<ExamQuestion> = withContext(Dispatchers.IO) {
        val displayGrade = YemeniCurriculum.toDisplayGrade(grade)
        val questions = mutableListOf<ExamQuestion>()

        for (i in 1..count) {
            when (i % 3) {
                1 -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.MULTIPLE_CHOICE,
                            text = "في مادة $subject لصف $displayGrade حول ($topic): ما هو الناتج الصحيح للعملية الأساسية المقررة في المنهج اليمني؟",
                            options = listOf("الخيار (أ): القيمة الأولى الصحيحة", "الخيار (ب): القيمة الثانية المقاربة", "الخيار (ج): القيمة الثالثة", "الخيار (د): لا شيء مما سبق"),
                            correctAnswer = "الخيار (أ): القيمة الأولى الصحيحة",
                            score = 2,
                            explanation = "وفقاً لما ورد في كتاب وزارة التربية والتعليم للجمهورية اليمنية."
                        )
                    )
                }
                2 -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.TRUE_FALSE,
                            text = "ضع علامة (✓) أو (✗): يعتبر مفهوم ($topic) في $subject أساساً للوحدة التعليمية التالية.",
                            options = listOf("صح (✓)", "خطأ (✗)"),
                            correctAnswer = "صح (✓)",
                            score = 1,
                            explanation = "قاعدة مقررة في الكتاب المدرسي المعتمد."
                        )
                    )
                }
                0 -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.FILL_BLANKS,
                            text = "أكمل الفراغ بالكلمة المناسبة: يهدف درس ($topic) إلى تمكين الطالب من تطبيق خطوات .............. في المسائل الحياتية.",
                            options = emptyList(),
                            correctAnswer = "الحل المنهجي والتحليل",
                            score = 2,
                            explanation = "تطبيق مباشر للمنهج اليمني."
                        )
                    )
                }
            }
        }
        questions
    }
}
