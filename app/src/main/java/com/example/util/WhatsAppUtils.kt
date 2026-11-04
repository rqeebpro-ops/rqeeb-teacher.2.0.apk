package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.StudentEntity
import java.io.File

object WhatsAppUtils {

    /**
     * Cleans phone number to international format (Defaults to Yemen +967 if local 9-digit)
     */
    fun cleanPhoneNumber(phone: String): String {
        var digits = phone.replace(Regex("[^0-9+]"), "")
        if (digits.startsWith("+")) {
            return digits.replace("+", "")
        }
        if (digits.startsWith("00")) {
            return digits.substring(2)
        }
        // Yemeni 9-digit local phone number starting with 77, 73, 71, 70
        if (digits.length == 9 && (digits.startsWith("77") || digits.startsWith("73") || digits.startsWith("71") || digits.startsWith("70"))) {
            return "967$digits"
        }
        if (digits.length == 12 && digits.startsWith("967")) {
            return digits
        }
        return digits
    }

    /**
     * Opens direct WhatsApp conversation with pre-filled message text
     */
    fun openWhatsAppChat(context: Context, rawPhone: String, messageText: String) {
        try {
            val cleanPhone = cleanPhoneNumber(rawPhone)
            val encodedText = Uri.encode(messageText)

            val uri = if (cleanPhone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback to general intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة الرسالة عبر واتساب"))
            } catch (ex: Exception) {
                Toast.makeText(context, "تعذر فتح تطبيق واتساب: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Shares a PDF file report directly via WhatsApp or general share sheet
     */
    fun sharePdfReportToWhatsApp(context: Context, pdfFile: File, rawPhone: String, captionText: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, captionText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback share sheet
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, captionText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير الطالب PDF عبر واتساب"))
            } catch (ex: Exception) {
                Toast.makeText(context, "خطأ في مشاركة الملف: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Builds professional pre-formatted Arabic WhatsApp message templates for 12 report types
     */
    fun buildReportMessage(
        reportType: String,
        student: StudentEntity,
        schoolName: String,
        teacherName: String,
        subjectName: String,
        customNotes: String = ""
    ): String {
        val sName = student.fullName
        val parent = if (student.parentName.isNotBlank()) student.parentName else "ولي الأمر الكرم"
        val gradeSection = "${student.grade} - شعبة (${student.section})"
        val school = schoolName.ifBlank { student.schoolName.ifBlank { "المدرسة المعتمدة" } }
        val teacher = teacherName.ifBlank { "أستاذ المادة" }

        return when (reportType) {
            "FULL_REPORT" -> """
السلام عليكم ورحمة الله وبركاته

المكرم ولي أمر الطالب / $sName

نرفع إليكم تقرير المتابعة الشامل لأداء ابنكم الدراسي والسلوكي:

🏫 المدرسة: $school
📚 الصف والشعبة: $gradeSection
📖 المادة: $subjectName

📊 ملخص الدرجات والأكاديميات:
• الاختبارات الشهرية: ${student.monthlyTestScore} / 20
• الدرجة الفصلية: ${student.termScore} / 20
• الواجبات والأنشطة: ${student.homeworkScore} / 10
• المشاركة والتفاعل: ${student.activityScore} / 10
• التقدير العام: ${student.generalRating}

🌟 نقاط القوة: ${student.strengths.ifBlank { "مواظبة وانضباط وتفاعل جيد" }}
🎯 جوانب المتابعة المقترحة: ${student.weaknesses.ifBlank { "الاستمرار في مراجعة الدروس أولاً بأول" }}

📝 ملاحظات المعلم:
${customNotes.ifBlank { student.notes.ifBlank { "نتمنى للطالب دوام التوفيق والنجاح." } }}

شاكرين تعاونكم مع إدارة المدرسة والمعلم.
مع خالص التقدير،
أستاذ المادة / $teacher
                """.trimIndent()

            "GRADES_REPORT" -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب المحترم / $sName

نحيطكم علماً بكشف درجات الطالب في مادة $subjectName:

🏫 المدرسة: $school
📚 الصف والشعبة: $gradeSection

📈 تفاصيل النتائج:
• اختبار الشهر: ${student.monthlyTestScore} / 20
• اختبار منتصف الفصل: ${student.termScore} / 20
• التقييم النهائي: ${student.finalExamScore} / 20
• الواجبات والأنشطة: ${student.homeworkScore} / 10
• التقدير العام المحسوب: ${student.generalRating}

${if (customNotes.isNotBlank()) "📝 ملاحظة المعلم: $customNotes\n" else ""}
نرجو متابعة الطالب وتشجيعه على مواصلة الاجتهاد والتفوق.

مع خالص التحية والتقدير،
المعلم / $teacher
                """.trimIndent()

            "ABSENCE_NOTICE" -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب / $sName

نحيطكم علماً بأن الطالب لم يحضر اليوم إلى المدرسة.

📚 الصف: $gradeSection
🏫 المدرسة: $school
📖 المادة: $subjectName

نرجو منكم متابعة سبب الغياب والحرص على انتظام الطالب في الحضور لضمان عدم تفويت الدروس الشارحة.

${if (customNotes.isNotBlank()) "📝 ملاحظة: $customNotes\n" else ""}
مع تحيات إدارة المدرسة والمعلم / $teacher
                """.trimIndent()

            "LATE_NOTICE" -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب / $sName

نحيطكم علماً بتسجيل تأخر صباحي للطالب عن طابور الصباح والحصة الأولى.

📚 الصف: $gradeSection
🏫 المدرسة: $school

نرجو منكم حث الطالب على النوم المبكر والحضور في الوقت المحدد لانتظام العملية التعليمية.

${if (customNotes.isNotBlank()) "📝 ملاحظة المعلم: $customNotes\n" else ""}
شاكرين حسن تعاونكم،
المعلم / $teacher
                """.trimIndent()

            "BEHAVIOR_NOTE" -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب / $sName

نود إبلاغكم بالملاحظة السلوكية والتربوية التالية داخل الفصل:

📚 الصف: $gradeSection
📖 المادة: $subjectName

📝 الملاحظة المسجلة:
${customNotes.ifBlank { student.notes.ifBlank { "نأمل التوجيه والمتابعة المنزلية المستمرة." } }}

نأمل التعاون في توجيه الطالب بما يساعده على الالتزام بالانضباط والتعليمات المدرسية.

شاكرين تعاونكم الدائم،
المعلم / $teacher
                """.trimIndent()

            "HOMEWORK_NOTICE" -> """
السلام عليكم ورحمة.الله وبركاته

ولي أمر الطالب / $sName

نود تنبيهكم بأن الطالب لم يقم بتسليم الواجب المنزلي المطلوب في مادة $subjectName.

📚 الصف والشعبة: $gradeSection

📝 تفاصيل الواجب والملاحظة:
${customNotes.ifBlank { "حل تمارين الدرس وإعداد النشاط المطلوب للكتاب المدرسي." }}

نرجو متابعة الطالب في المنزل لإنهاء الواجب وإحضاره في الحصة القادمة.

مع الشكر والتقدير،
المعلم / $teacher
                """.trimIndent()

            "EXAM_RESULT" -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب العزيز / $sName

نبارك لكم الحصول على نتيجة الاختبار في مادة $subjectName:

📚 الصف والشعبة: $gradeSection
📊 الدرجة المحصلة: ${student.monthlyTestScore} / 20
🌟 المستوى والتقدير: ${student.generalRating}

${if (customNotes.isNotBlank()) "📝 توجيه المعلم: $customNotes\n" else ""}
نتمنى للطالب دوام التميز والارتقاء.

مع خالص التحية،
المعلم / $teacher
                """.trimIndent()

            "LOW_SCORE_ALERT" -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب / $sName

حرصاً منا على مستقبل الطالب التعليمي، نود إبلاغكم ولوحظ انخفاض في مستواه المحصل في مادة $subjectName.

📚 الصف: $gradeSection
📊 الدرجة الحالية: ${student.monthlyTestScore} / 20

🎯 المواد والمواضيع التي تحتاج تركيزاً ومراجعة:
${student.weaknesses.ifBlank { "مراجعة تمارين الكتاب والتركيز أثناء شرح الحصة." }}

${if (customNotes.isNotBlank()) "📝 توصية المعلم: $customNotes\n" else ""}
نرجو منكم التواصل وتكثيف المتابعة المنزلية لرفع مستواه في الفترة القادمة.

شاكرين حرصكم،
المعلم / $teacher
                """.trimIndent()

            "EXCELLENCE_CONGRATS" -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب المتميز / $sName

يسرنا تهنئتكم بالتفوق الباهر والتميز الملحوظ للطالب في مادة $subjectName.

📚 الصف والشعبة: $gradeSection
🌟 التقدير العام: ${student.generalRating} (الأول والمتميز صفياً)

💪 نقاط القوة المتميزة:
${student.strengths.ifBlank { "سرعة الفهم، الالتزام التام بالواجبات والمشاركة الفعالة." }}

${if (customNotes.isNotBlank()) "📝 كلمة المعلم: $customNotes\n" else ""}
نثمن جهودكم المباركة في المتابعة المنزلية ونتمنى له دوام الصدارة.

مع خالص الفخر والاعتزاز،
المعلم / $teacher
                """.trimIndent()

            "UPCOMING_EXAM_REMINDER" -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب / $sName

نود تذكيركم بموعد الاختبار القادم في مادة $subjectName.

📚 الصف: $gradeSection
📅 موعد الاختبار والموضوع:
${customNotes.ifBlank { "اختبار شهر القادم في الوحدات المقررة بالكتاب المدرسي." }}

نرجو مساعدة الطالب في الاستعداد الجيد والمراجعة الشاملة.

مع تمنياتنا بالتوفيق والنجاح،
المعلم / $teacher
                """.trimIndent()

            "THANK_YOU_PARENT" -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب الفاضل / $sName

تتقدم إدارة المدرسة والمعلم ببالغ الشكر والتقدير لجهودكم المباركة ومتابعتكم المستمرة لابنكم.

📚 الصف: $gradeSection
🏫 المدرسة: $school

إن هذا الاهتمام والتواصل المستمر كان له الأثر البالغ في تفوق الطالب وانضباطه الملحوظ.

${if (customNotes.isNotBlank()) "📝 ملاحظة خاصة: $customNotes\n" else ""}
دمتم ودام ابنكم مصدراً للفخر والاعتزاز.

مع أطيب التحيات،
المعلم / $teacher
                """.trimIndent()

            else -> """
السلام عليكم ورحمة الله وبركاته

ولي أمر الطالب / $sName

📚 الصف والشعبة: $gradeSection
🏫 المدرسة: $school
📖 المادة: $subjectName

📝 ملاحظة من المعلم:
${customNotes.ifBlank { student.notes.ifBlank { "نرجو المتابعة المستمرة وتفقد واجبات الطالب." } }}

مع خالص التقدير،
المعلم / $teacher
                """.trimIndent()
        }
    }
}
