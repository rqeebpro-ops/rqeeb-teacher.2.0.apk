package com.example

import com.example.data.model.ExamQuestion
import com.example.data.model.QuestionType
import com.example.data.model.StudentEntity
import com.example.data.model.TeacherEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RqeebLogicTest {

    @Test
    fun testExamQuestionSerialization() {
        val originalQuestions = listOf(
            ExamQuestion(
                questionText = "ما هي عاصمة المملكة العربية السعودية؟",
                type = QuestionType.MULTIPLE_CHOICE,
                options = listOf("الرياض", "جدة", "الدمام", "مكة المكرمة"),
                correctAnswer = "الرياض",
                explanation = "الرياض هي العاصمة الإدارية والسياسية.",
                marks = 2
            ),
            ExamQuestion(
                questionText = "الذكاء الاصطناعي الضيق متخصص في أداء مهام محددة بدقة.",
                type = QuestionType.TRUE_FALSE,
                options = listOf("صح", "خطأ"),
                correctAnswer = "صح",
                explanation = "صحيح، مثل أنظمة التوصية ومحركات البحث.",
                marks = 2
            )
        )

        val jsonString = ExamQuestion.listToJsonString(originalQuestions)
        val deserialized = ExamQuestion.parseListFromJson(jsonString)

        assertEquals(2, deserialized.size)
        assertEquals("ما هي عاصمة المملكة العربية السعودية؟", deserialized[0].questionText)
        assertEquals(QuestionType.MULTIPLE_CHOICE, deserialized[0].type)
        assertEquals("الرياض", deserialized[0].correctAnswer)
        assertEquals(4, deserialized[0].options.size)
        assertEquals(QuestionType.TRUE_FALSE, deserialized[1].type)
    }

    @Test
    fun testStudentScoringAndPercentage() {
        val student = StudentEntity(
            teacherId = 1L,
            className = "1/أ",
            studentName = "فهد عبد العزيز",
            quizScore = 9.0,
            homeworkScore = 9.0,
            examScore = 18.0,
            maxQuizScore = 10.0,
            maxHomeworkScore = 10.0,
            maxExamScore = 20.0
        )

        assertEquals(36.0, student.totalScore, 0.01)
        assertEquals(40.0, student.totalMaxScore, 0.01)
        assertEquals(90, student.percentage)
    }

    @Test
    fun testTeacherStatusLifecycle() {
        val approvedTeacher = TeacherEntity(
            fullName = "أحمد خالد",
            phone = "0501112233",
            email = "ahmed@rqeeb.edu",
            passwordHash = "123456",
            schoolName = "مدرسة الأمل",
            governorate = "الرياض",
            subject = "لغة عربية",
            stage = "ثانوي",
            status = TeacherEntity.STATUS_APPROVED
        )

        assertEquals(TeacherEntity.STATUS_APPROVED, approvedTeacher.status)

        val suspended = approvedTeacher.copy(status = TeacherEntity.STATUS_SUSPENDED)
        assertEquals(TeacherEntity.STATUS_SUSPENDED, suspended.status)
    }

    @Test
    fun testEducationalDiagramLibraryGeneratesValidSvg() {
        val templates = com.example.data.model.EducationalDiagramLibrary.ALL_TEMPLATES
        assertTrue("Library should contain educational templates", templates.isNotEmpty())

        for (tmpl in templates) {
            val normalDiag = com.example.data.model.EducationalDiagramLibrary.buildDiagramInfo(tmpl.key, hideLabelsForQuiz = false)
            assertNotNull(normalDiag)
            assertTrue("SVG should contain <svg tag", normalDiag.svgContent.contains("<svg"))
            assertTrue("SVG should contain </svg> closing tag", normalDiag.svgContent.contains("</svg>"))
            assertEquals(tmpl.category, normalDiag.category)

            val quizDiag = com.example.data.model.EducationalDiagramLibrary.buildDiagramInfo(tmpl.key, hideLabelsForQuiz = true)
            assertNotNull(quizDiag)
            assertTrue(quizDiag.hideLabelsForQuiz)
            assertTrue("Quiz SVG should be valid", quizDiag.svgContent.contains("<svg"))
        }
    }

    @Test
    fun testExamQuestionWithDiagramAndVariantFieldsSerialization() {
        val diag = com.example.data.model.DiagramInfo(
            title = "دائرة كهربائية بسيطة",
            category = "PHYSICS",
            svgContent = "<svg width='100' height='100'><rect/></svg>",
            customNotes = "مخطط دائرة كهربائية توضح التوالي",
            hideLabelsForQuiz = true
        )

        val question = ExamQuestion(
            questionText = "إذا كان 3س + 5 = 20، فأوجد قيمة س.",
            type = QuestionType.MULTIPLE_CHOICE,
            options = listOf("س = 5", "س = 6", "س = 7", "س = 4"),
            correctAnswer = "س = 5",
            explanation = "3س = 15 => س = 5",
            marks = 3,
            diagram = diag,
            variantLabel = "النسخة A",
            verificationStatus = "VERIFIED",
            verificationNote = "تم التحقق رياضياً: 3(5) + 5 = 20"
        )

        val jsonString = ExamQuestion.listToJsonString(listOf(question))
        val parsedList = ExamQuestion.parseListFromJson(jsonString)

        assertEquals(1, parsedList.size)
        val parsed = parsedList[0]
        assertEquals(question.questionText, parsed.questionText)
        assertEquals("النسخة A", parsed.variantLabel)
        assertEquals("VERIFIED", parsed.verificationStatus)
        assertEquals("تم التحقق رياضياً: 3(5) + 5 = 20", parsed.verificationNote)
        assertNotNull(parsed.diagram)
        assertEquals("دائرة كهربائية بسيطة", parsed.diagram?.title)
        assertTrue(parsed.diagram?.hideLabelsForQuiz == true)
    }

    @Test
    fun testVariantStrategyEnumCompleteness() {
        val strategies = com.example.data.model.VariantStrategy.values()
        assertEquals(6, strategies.size)
        for (strategy in strategies) {
            assertTrue(strategy.titleAr.isNotBlank())
            assertTrue(strategy.descriptionAr.isNotBlank())
        }
    }
}
