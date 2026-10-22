package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiService
import com.example.data.model.YemeniCurriculum
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("رقيب", appName)
    }

    @Test
    fun `verify YemeniCurriculum four checkbox boxes contain official options`() {
        // Box 1: أساليب المعلم
        assertTrue(YemeniCurriculum.TEACHER_STYLES.contains("الإلقاء والتوضيح"))
        assertTrue(YemeniCurriculum.TEACHER_STYLES.contains("الحوار والمناقشة"))
        assertTrue(YemeniCurriculum.TEACHER_STYLES.contains("القصة"))
        assertTrue(YemeniCurriculum.TEACHER_STYLES.contains("التعزيز"))

        // Box 2: طرق التدريس
        assertTrue(YemeniCurriculum.TEACHING_METHODS.contains("التعلم التعاوني"))
        assertTrue(YemeniCurriculum.TEACHING_METHODS.contains("العصف الذهني"))
        assertTrue(YemeniCurriculum.TEACHING_METHODS.contains("حل المشكلات"))
        assertTrue(YemeniCurriculum.TEACHING_METHODS.contains("الاستقصاء"))

        // Box 3: المهارات والقيم
        assertTrue(YemeniCurriculum.SKILLS_AND_VALUES.contains("الملاحظة"))
        assertTrue(YemeniCurriculum.SKILLS_AND_VALUES.contains("التفكير النقدي"))
        assertTrue(YemeniCurriculum.SKILLS_AND_VALUES.contains("القيم الدينية والوطنية"))

        // Box 4: الوسائل التعليمية
        assertTrue(YemeniCurriculum.TEACHING_AIDS.contains("السبورة والطباشير"))
        assertTrue(YemeniCurriculum.TEACHING_AIDS.contains("الكتاب المدرسي"))
        assertTrue(YemeniCurriculum.TEACHING_AIDS.contains("أوراق عمل"))
    }

    @Test
    fun `verify Yemeni grades and subjects mapping`() {
        val basicSubjects = YemeniCurriculum.getSubjectsForGrade("الصف الرابع الأساسي")
        assertTrue(basicSubjects.contains("القرآن الكريم"))
        assertTrue(basicSubjects.contains("العلوم"))

        val secondarySubjects = YemeniCurriculum.getSubjectsForGrade("الصف الثاني عشر (الثالث الثانوي - علمي)")
        assertTrue(secondarySubjects.contains("الفيزياء"))
        assertTrue(secondarySubjects.contains("الكيمياء"))
    }

    @Test
    fun `verify lesson plan generation strictly respects selected options`() = runBlocking {
        val service = GeminiService()
        val styles = listOf("الحوار والمناقشة", "القصة")
        val methods = listOf("التعلم التعاوني", "حل المشكلات")
        val skills = listOf("التفكير النقدي", "العمل الجماعي والتعاون")
        val aids = listOf("الكتاب المدرسي", "أوراق عمل")

        val plan = service.generateLessonPlan(
            schoolName = "مدرسة الشهيد الدرة",
            teacherName = "أ. عبد الرقيب",
            subject = "العلوم",
            grade = "الصف التاسع الأساسي",
            title = "التيار الكهربائي وقانون أوم",
            date = "2026/10/21",
            duration = 45,
            selectedTeacherStyles = styles,
            selectedTeachingMethods = methods,
            selectedSkillsAndValues = skills,
            selectedTeachingAids = aids
        )

        // Verify title & subjects
        assertEquals("التيار الكهربائي وقانون أوم", plan.title)
        assertEquals("العلوم", plan.subject)

        // Verify steps table has stages and 5 elements per step
        assertTrue(plan.steps.isNotEmpty())
        val firstStep = plan.steps.first()
        assertTrue(firstStep.stageAndTime.isNotBlank())
        assertTrue(firstStep.teacherActions.isNotBlank())
        assertTrue(firstStep.studentActivities.isNotBlank())
        assertTrue(firstStep.materialsUsed.isNotBlank())
        assertTrue(firstStep.teachingMethod.isNotBlank())

        // Verify selected aids and styles are reflected
        assertTrue(plan.selectedTeachingAids.contains("الكتاب المدرسي"))
        assertTrue(plan.selectedTeachingMethods.contains("التعلم التعاوني"))
    }
}

