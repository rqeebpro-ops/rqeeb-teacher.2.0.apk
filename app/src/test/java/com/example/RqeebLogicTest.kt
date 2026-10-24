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

    @Test
    fun testPrimaryMathEngineAdditionWithRegrouping() {
        val steps = com.example.data.math.PrimaryMathEngine.solveAdditionWithCarries(468, 375)
        assertEquals(843, 468 + 375)
        assertTrue("Addition steps should not be empty", steps.isNotEmpty())

        // Ones column: 8 + 5 = 13 -> carry 1
        assertEquals(0, steps[0].columnIndex)
        assertEquals(1, steps[0].carryOut)
        assertEquals(3, steps[0].digitResult)

        // Tens column: 6 + 7 + 1 = 14 -> carry 1
        assertEquals(1, steps[1].columnIndex)
        assertEquals(1, steps[1].carryOut)
        assertEquals(4, steps[1].digitResult)

        // Hundreds column: 4 + 3 + 1 = 8 -> carry 0
        assertEquals(2, steps[2].columnIndex)
        assertEquals(0, steps[2].carryOut)
        assertEquals(8, steps[2].digitResult)
    }

    @Test
    fun testPrimaryMathEngineSubtractionBorrowingAcrossZeros() {
        val result504 = com.example.data.math.PrimaryMathEngine.solveSubtractionWithBorrowing(504, 238)
        assertEquals(266, result504.difference)
        assertEquals(504, result504.minuend)
        assertEquals(238, result504.subtrahend)

        // The ones column (4 - 8) needs a borrow
        val onesStep = result504.steps.first { it.columnIndex == 0 }
        assertTrue(onesStep.borrowNeeded)
        assertEquals(6, onesStep.digitResult) // 14 - 8 = 6

        // Tracing should show intermediate zero converting to 9
        val traceString = onesStep.borrowStepsTrace.joinToString(" ")
        assertTrue("Borrow trace must mention converting 0 to 9", traceString.contains("9") || traceString.contains("الصفر"))

        // Subtraction from 600 - 247
        val result600 = com.example.data.math.PrimaryMathEngine.solveSubtractionWithBorrowing(600, 247)
        assertEquals(353, result600.difference)
        val ones600 = result600.steps.first { it.columnIndex == 0 }
        assertTrue(ones600.borrowNeeded)
        assertEquals(3, ones600.digitResult) // 10 - 7 = 3
    }

    @Test
    fun testPrimaryMathEngineGeneratesExamQuestions() {
        val exam = com.example.data.math.PrimaryMathEngine.generatePrimaryMathExam(
            grade = 4,
            count = 5,
            skills = listOf("SUBTRACTION_ZEROS", "ADDITION_REGROUPING", "MULTIPLICATION"),
            difficulty = "متوسط"
        )
        assertEquals(5, exam.size)
        for (q in exam) {
            assertTrue(q.questionText.isNotBlank())
            assertTrue(q.correctAnswer.isNotBlank())
            assertTrue("Options should contain correct answer", q.options.contains(q.correctAnswer))
            assertTrue(q.explanation.isNotBlank())
            assertEquals(com.example.data.model.QuestionType.MULTIPLE_CHOICE, q.type)
        }
    }

    @Test
    fun testYemeniCurriculumUnitsMapping() {
        val mathUnitsG4 = com.example.data.model.YemeniCurriculum.getCurriculumUnits("الصف الرابع الابتدائي", "الرياضيات")
        assertTrue(mathUnitsG4.isNotEmpty())
        assertTrue(mathUnitsG4.any { it.key.contains("الجمع") || it.key.contains("الأعداد") })

        val scienceUnitsG8 = com.example.data.model.YemeniCurriculum.getCurriculumUnits("الصف الثاني الإعدادي (الثامن)", "العلوم")
        assertTrue(scienceUnitsG8.isNotEmpty())
    }
}
