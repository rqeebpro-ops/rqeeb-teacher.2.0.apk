package com.example

import com.example.data.model.YemeniCurriculum
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testOfficialGradesNaming() {
        val expected = listOf(
            "1/ أساسي",
            "2/ أساسي",
            "3/ أساسي",
            "4/ أساسي",
            "5/ أساسي",
            "6/ أساسي",
            "7/ إعدادي",
            "8/ إعدادي",
            "9/ إعدادي",
            "10/ ثانوي",
            "11/ ثانوي",
            "12/ ثانوي"
        )
        assertEquals(expected, YemeniCurriculum.DISPLAY_GRADES)
    }

    @Test
    fun testStandardSections() {
        val expected = listOf("أ", "ب", "ج", "د", "هـ", "و", "أخرى")
        assertEquals(expected, YemeniCurriculum.STANDARD_SECTIONS)
    }

    @Test
    fun testGradeConversion() {
        assertEquals("9/ إعدادي", YemeniCurriculum.toDisplayGrade("الصف التاسع"))
        assertEquals("1/ أساسي", YemeniCurriculum.toDisplayGrade("1"))
        assertEquals("12/ ثانوي", YemeniCurriculum.toDisplayGrade("الصف الثاني عشر"))
        assertEquals("7/ إعدادي", YemeniCurriculum.toDisplayGrade("7/ إعدادي"))
    }
}
