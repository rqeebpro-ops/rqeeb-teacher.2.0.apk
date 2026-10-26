package com.example.data.model

/**
 * Data structures and constants specifically designed for the Yemeni Educational System
 */
object YemeniCurriculum {

    const val COUNTRY_NAME_AR = "الجمهورية اليمنية"
    const val MINISTRY_NAME_AR = "وزارة التربية والتعليم"

    // Default Owner credentials and contact info
    const val OWNER_EMAIL_DEFAULT = "abdulrqeebfarag2018@gmail.com"
    const val OWNER_PHONE_DEFAULT = "785502919"
    const val OWNER_WHATSAPP_URL_DEFAULT = "https://wa.me/967785502919"

    // Supported Curriculum Editions in Yemen
    val EDITIONS = listOf(
        "طبعة صنعاء (وزارة التربية والتعليم - صنعاء)",
        "طبعة عدن (وزارة التربية والتعليم - عدن)",
        "المنهج العام الموحد"
    )

    // Yemeni Governorates (22 المحافظات اليمنية)
    val GOVERNORATES = listOf(
        "أمانة العاصمة",
        "محافظة صنعاء",
        "عدن",
        "تعز",
        "إب",
        "الحديدة",
        "حضرموت (الساحل)",
        "حضرموت (الوادي والصحراء)",
        "ذمار",
        "مأرب",
        "لحج",
        "أبين",
        "شبوة",
        "حجة",
        "صعدة",
        "المحويت",
        "عمران",
        "الضالع",
        "البيضاء",
        "الجوف",
        "المهرة",
        "سقطرى",
        "ريمة"
    )

    // Official Display Names for Grades in Rqeeb Teacher App (الأسماء الرسمية المعتمدة للعرض)
    val DISPLAY_GRADES = listOf(
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

    // Standard school divisions/sections in Yemeni schools (شعب الفصول المعتمدة)
    val STANDARD_SECTIONS = listOf(
        "أ",
        "ب",
        "ج",
        "د",
        "هـ",
        "و",
        "أخرى"
    )

    // Legacy lists preserved for internal backwards compatibility
    val BASIC_GRADES = DISPLAY_GRADES.take(9)
    val SECONDARY_GRADES = DISPLAY_GRADES.takeLast(3)
    val ALL_GRADES = DISPLAY_GRADES

    /**
     * Converts any stored grade string (legacy full name or number) to the official display name
     */
    fun toDisplayGrade(grade: String): String {
        val trimmed = grade.trim()
        if (DISPLAY_GRADES.contains(trimmed)) return trimmed

        return when {
            trimmed.contains("الأول الأساسي") || trimmed == "1" || trimmed.startsWith("1/") || trimmed.contains("الاول") -> "1/ أساسي"
            trimmed.contains("الثاني الأساسي") || trimmed == "2" || trimmed.startsWith("2/") -> "2/ أساسي"
            trimmed.contains("الثالث الأساسي") || trimmed == "3" || trimmed.startsWith("3/") -> "3/ أساسي"
            trimmed.contains("الرابع الأساسي") || trimmed == "4" || trimmed.startsWith("4/") -> "4/ أساسي"
            trimmed.contains("الخامس الأساسي") || trimmed == "5" || trimmed.startsWith("5/") -> "5/ أساسي"
            trimmed.contains("السادس الأساسي") || trimmed == "6" || trimmed.startsWith("6/") -> "6/ أساسي"
            trimmed.contains("السابع الأساسي") || trimmed.contains("السابع") || trimmed == "7" || trimmed.startsWith("7/") -> "7/ إعدادي"
            trimmed.contains("الثامن الأساسي") || trimmed.contains("الثامن") || trimmed == "8" || trimmed.startsWith("8/") -> "8/ إعدادي"
            trimmed.contains("التاسع الأساسي") || trimmed.contains("التاسع") || trimmed == "9" || trimmed.startsWith("9/") -> "9/ إعدادي"
            trimmed.contains("الأول الثانوي") || trimmed.contains("العاشر") || trimmed == "10" || trimmed.startsWith("10/") -> "10/ ثانوي"
            trimmed.contains("الثاني الثانوي") || trimmed.contains("الحادي عشر") || trimmed == "11" || trimmed.startsWith("11/") -> "11/ ثانوي"
            trimmed.contains("الثالث الثانوي") || trimmed.contains("الثاني عشر") || trimmed == "12" || trimmed.startsWith("12/") -> "12/ ثانوي"
            else -> if (trimmed.isNotBlank()) trimmed else "9/ إعدادي"
        }
    }

    /**
     * Maps display grade to official stage in Yemen
     */
    fun getStageForDisplayGrade(displayGrade: String): String {
        val normalized = toDisplayGrade(displayGrade)
        return when {
            normalized.contains("أساسي") -> "مرحلة التعليم الأساسي"
            normalized.contains("إعدادي") -> "المرحلة الإعدادية"
            normalized.contains("ثانوي") -> "مرحلة التعليم الثانوي"
            else -> "مرحلة التعليم الأساسي"
        }
    }

    // Subjects in Yemeni Schools
    val SUBJECTS = listOf(
        "القرآن الكريم",
        "التربية الإسلامية",
        "اللغة العربية",
        "الرياضيات",
        "العلوم",
        "الاجتماعيات",
        "الفيزياء",
        "الكيمياء",
        "الأحياء",
        "اللغة الإنجليزية",
        "التاريخ",
        "الجغرافيا",
        "التربية الوطنية",
        "الحاسوب وتقنية المعلومات"
    )

    fun getSubjectsForGrade(grade: String): List<String> {
        val dg = toDisplayGrade(grade)
        return when {
            dg.startsWith("1/") || dg.startsWith("2/") || dg.startsWith("3/") -> {
                listOf("القرآن الكريم", "التربية الإسلامية", "اللغة العربية", "الرياضيات", "العلوم والبيئة")
            }
            dg.startsWith("4/") || dg.startsWith("5/") || dg.startsWith("6/") -> {
                listOf("القرآن الكريم", "التربية الإسلامية", "اللغة العربية", "الرياضيات", "العلوم", "الاجتماعيات", "اللغة الإنجليزية")
            }
            dg.startsWith("7/") || dg.startsWith("8/") || dg.startsWith("9/") -> {
                listOf("القرآن الكريم", "التربية الإسلامية", "اللغة العربية", "الرياضيات", "العلوم", "الاجتماعيات", "اللغة الإنجليزية", "الحاسوب")
            }
            dg.startsWith("10/") || dg.startsWith("11/") || dg.startsWith("12/") -> {
                listOf("القرآن الكريم", "التربية الإسلامية", "اللغة العربية", "اللغة الإنجليزية", "الرياضيات", "الفيزياء", "الكيمياء", "الأحياء", "التاريخ", "الجغرافيا", "التربية الوطنية", "الحاسوب")
            }
            else -> SUBJECTS
        }
    }

    // Official Yemeni Lesson Prep Terms
    val TERMS = listOf("الفصل الدراسي الأول", "الفصل الدراسي الثاني")

    // Official Preparation Types
    val PREPARATION_TYPES = listOf("تحضير يومي قياسي", "تحضير شامل معزز بالأنشطة", "خطة درس للمراجعة والتقويم")

    // Student Ability Levels
    val STUDENT_LEVELS = listOf("مبتدئ / تأسيسي", "متوسط / اعتيادي", "متقدم / متفوق")

    // Lesson Boxes & Ministry Teaching Methods
    val TEACHER_STYLES = listOf(
        "الإلقاء والتوضيح",
        "الحوار والمناقشة",
        "القصة والاستقراء",
        "القدوة والتمثيل",
        "التعزيز الإيجابي",
        "التكرار والتثبيت"
    )

    val TEACHING_METHODS = listOf(
        "العصف الذهني",
        "التعلم التعاوني (مجموعات)",
        "حل المشكلات",
        "الاستقصاء والاكتشاف",
        "لعب الأدوار",
        "المحاكاة والتطبيق العملي"
    )

    val SKILLS_AND_VALUES = listOf(
        "حب الوطن والاعتزاز بالهوية اليمنية",
        "التفكير الناقد والتحليل",
        "التعاون والعمل الجماعي",
        "الأمانة والصدق العلمي",
        "احترام المعلم والزملاء",
        "المحافظة على الممتلكات والبيئة"
    )

    val TEACHING_AIDS = listOf(
        "السبورة والأقلام الملونة",
        "الكتاب المدرسي المعتمد",
        "خرائط ومجسمات توضيحية",
        "بطاقات تعليمية وأوراق عمل",
        "أدوات ومواد معملية بسيطة",
        "عروض مرئية أو مسجل صوتي"
    )

    val EXAM_TYPES = listOf("اختبار تشخيصي", "اختبار شهري", "اختبار فصلي", "اختبار نهائي")
}
