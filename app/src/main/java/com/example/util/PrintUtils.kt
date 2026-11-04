package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ExamEntity
import com.example.data.model.ExamQuestion
import com.example.data.model.LessonEntity
import com.example.data.model.SmartExplanationResult
import com.example.data.model.StudentEntity
import com.example.data.model.YemeniCurriculum
import com.example.ui.teacher.ExamDiagramSvg
import java.io.File
import java.io.FileOutputStream

object PrintUtils {

    /**
     * Builds complete HTML for Yemeni Curriculum Lesson Preparation
     */
    fun buildLessonHtml(lesson: LessonEntity): String {
        val displayGrade = YemeniCurriculum.toDisplayGrade(lesson.grade)
        val strategies = if (lesson.teachingMethods.isNotBlank()) lesson.teachingMethods else "الحوار والمناقشة، التعلم التعاوني، حل المشكلات"

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 25px; color: #1e293b; background: #fff; }
                    .header-box { border: 2px solid #006874; border-radius: 8px; padding: 12px 18px; margin-bottom: 18px; text-align: center; }
                    .country { font-size: 13px; font-weight: bold; color: #475569; }
                    .main-title { font-size: 21px; font-weight: bold; color: #006874; margin: 6px 0; }
                    .school-sub { font-size: 13px; color: #334155; }
                    .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
                    .meta-table td { border: 1px solid #cbd5e1; padding: 7px 10px; font-size: 13px; }
                    .meta-table td strong { color: #006874; }
                    .section-head { background: #e0f2f1; color: #004d40; padding: 7px 12px; font-weight: bold; font-size: 14px; margin-top: 14px; border-radius: 6px; border-right: 5px solid #006874; }
                    .section-body { border: 1px solid #e2e8f0; padding: 10px 14px; margin-top: 4px; font-size: 13px; line-height: 1.7; white-space: pre-line; border-radius: 0 0 6px 6px; background: #f8fafc; }
                    .strategies-tag { display: inline-block; background: #006874; color: #fff; padding: 3px 8px; border-radius: 4px; font-size: 12px; margin: 2px 4px; }
                    .footer { text-align: center; margin-top: 30px; font-size: 11px; color: #64748b; border-top: 1px solid #cbd5e1; padding-top: 10px; }
                </style>
            </head>
            <body>
                <div class="header-box">
                    <div class="country">الجمهورية اليمنية • وزارة التربية والتعليم</div>
                    <div class="main-title">خطة تحضير درس: ${lesson.title}</div>
                    <div class="school-sub">مدرسة: ${lesson.schoolName.ifBlank { "المدرسة المعتمدة" }} • ${lesson.governorate} (${lesson.directorate}) • طبعة المنهج: ${lesson.curriculumEdition.ifBlank { "طبعة صنعاء الرسمية" }}</div>
                </div>

                <table class="meta-table">
                    <tr>
                        <td><strong>المادة:</strong> ${lesson.subject}</td>
                        <td><strong>الصف الدراسي:</strong> $displayGrade</td>
                        <td><strong>الفصل:</strong> ${lesson.term}</td>
                    </tr>
                    <tr>
                        <td><strong>الوحدة:</strong> ${lesson.unitName.ifBlank { "عام" }}</td>
                        <td><strong>تاريخ التحضير:</strong> ${lesson.lessonDate}</td>
                        <td><strong>زمن الحصة:</strong> ${lesson.durationMinutes} دقيقة (${lesson.periodNumber})</td>
                    </tr>
                    <tr>
                        <td colspan="3"><strong>المرجع المعتمد:</strong> ${lesson.textbookReference.ifBlank { "الكتاب المدرسي المقرر - وزارة التربية والتعليم" }}</td>
                    </tr>
                </table>

                <div class="section-head">1. استراتيجيات التدريس المعتمدة في الدرس</div>
                <div class="section-body">
                    <strong>الاستراتيجيات المختارة:</strong> ${strategies}
                </div>

                <div class="section-head">2. الأهداف السلوكية الإجرائية (قابلة للقياس)</div>
                <div class="section-body">
                    <strong>• الأهداف المعرفية:</strong><br>${lesson.cognitiveObjectives}
                    <br><br>
                    <strong>• الأهداف المهارية:</strong><br>${lesson.skillObjectives}
                    <br><br>
                    <strong>• الأهداف الوجدانية والقيم:</strong><br>${lesson.affectiveObjectives}
                </div>

                ${if (lesson.expectedOutcomes.isNotBlank()) """
                <div class="section-head">3. نواتج التعلم المتوقعة والمعارف والمهارات المستهدفة</div>
                <div class="section-body">
                    <strong>نواتج التعلم:</strong><br>${lesson.expectedOutcomes}
                    <br><br>
                    <strong>المعارف والمهارات المستهدفة:</strong><br>${lesson.targetKnowledgeAndSkills}
                </div>
                """ else ""}

                <div class="section-head">4. المتطلبات السابقة والتهيئة الحافزة والوسائل</div>
                <div class="section-body">
                    ${if (lesson.prerequisites.isNotBlank()) "<strong>المتطلبات السابقة والربط:</strong><br>${lesson.prerequisites}<br><br>" else ""}
                    <strong>التهيئة والمقدمة الحافزة:</strong><br>${lesson.introduction}
                    <br><br>
                    <strong>الوسائل التعليمية ومصادر التعلم:</strong><br>${lesson.teachingAids.ifBlank { "السبورة والأقلام الملونة، الكتاب المدرسي، بطاقات وأوراق عمل" }}
                </div>

                <div class="section-head">5. خطوات الشرح المتسلسلة وإجراءات التنفيذ والأنشطة</div>
                <div class="section-body">
                    <strong>سير خطوات الدرس:</strong><br>${lesson.teachingSteps}
                    ${if (lesson.teacherStudentActivities.isNotBlank()) "<br><br><strong>أنشطة المعلم والمتعلم:</strong><br>${lesson.teacherStudentActivities}" else ""}
                    ${if (lesson.discussionQuestions.isNotBlank()) "<br><br><strong>الأسئلة الصفية والمناقشة:</strong><br>${lesson.discussionQuestions}" else ""}
                </div>

                <div class="section-head">6. التقويم (تشخيصي، تكويني، ختامي) والواجب المنزلي</div>
                <div class="section-body">
                    ${if (lesson.diagnosticAssessment.isNotBlank()) "<strong>التقويم التشخيصي (القبلي):</strong> ${lesson.diagnosticAssessment}<br><br>" else ""}
                    <strong>التقويم التكويني (البنائي):</strong> ${lesson.formativeAssessment}<br><br>
                    ${if (lesson.summativeAssessment.isNotBlank()) "<strong>التقويم الختامي:</strong> ${lesson.summativeAssessment}<br><br>" else ""}
                    <strong>الواجب المنزلي والتكليفات:</strong> ${lesson.homework}
                </div>

                ${if (lesson.remedialActivities.isNotBlank() || lesson.enrichmentActivities.isNotBlank()) """
                <div class="section-head">7. مراعاة الفروق الفردية (الأنشطة العلاجية والإثرائية)</div>
                <div class="section-body">
                    <strong>• الأنشطة العلاجية (للطلاب الضعاف):</strong><br>${lesson.remedialActivities}<br><br>
                    <strong>• الأنشطة الإثرائية (للطلاب المتفوقين):</strong><br>${lesson.enrichmentActivities}
                </div>
                """ else ""}

                ${if (lesson.timeDistribution.isNotBlank()) """
                <div class="section-head">8. توزيع الزمن على فقرات الحصة</div>
                <div class="section-body">${lesson.timeDistribution}</div>
                """ else ""}

                ${if (lesson.teacherNotes.isNotBlank()) """
                <div class="section-head">9. ملاحظات المعلم والتأمل الذاتي</div>
                <div class="section-body">${lesson.teacherNotes}</div>
                """ else ""}

                <div class="footer">
                    تم إعداد وتحضير هذا الدرس وفق النموذج المعتمد لوزارة التربية والتعليم بالجمهورية اليمنية عبر تطبيق «رقيب – مساعد المعلم الذكي»
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Builds complete HTML for Exam (Student paper or Teacher Model Answer Key)
     */
    fun buildExamHtml(exam: ExamEntity, questions: List<ExamQuestion>, isAnswerKey: Boolean): String {
        val displayGrade = YemeniCurriculum.toDisplayGrade(exam.grade)
        val pageTypeTitle = if (isAnswerKey) "نموذج الإجابة الرسمي وسلم الدرجات والتصحيح" else "ورقة أسئلة الطالب"

        val qHtml = questions.mapIndexed { idx, q ->
            val optHtml = if (q.options.isNotEmpty()) {
                "<div style='margin-right: 25px; margin-top: 6px; font-size: 13px;'>" +
                        q.options.mapIndexed { oIdx, opt ->
                            val isCorrect = isAnswerKey && opt.trim() == q.correctAnswer.trim()
                            val style = if (isCorrect) "background: #c8e6c9; font-weight: bold; padding: 2px 6px; border-radius: 4px;" else ""
                            "<span style='$style'>(${('أ' + oIdx)}) $opt</span> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                        }.joinToString("") +
                        "</div>"
            } else ""

            val diagramHtml = if (!q.diagramType.isNullOrBlank() && q.diagramType != "NONE") {
                "<div style='text-align: center; margin: 12px auto;'>" +
                        ExamDiagramSvg.getSvg(q.diagramType, isAnswerKey) +
                        (if (!q.diagramInstruction.isNullOrBlank()) "<div style='font-size: 12px; color: #475569; margin-top: 4px;'><em>${q.diagramInstruction}</em></div>" else "") +
                        "</div>"
            } else ""

            val answerKeyDetails = if (isAnswerKey) {
                """
                <div style="background: #e8f5e9; border: 1px solid #81c784; padding: 8px 12px; margin-top: 8px; border-radius: 6px; font-size: 13px;">
                    <strong style="color: #1b5e20;">✓ الإجابة النموذجية وسلم الدرجات:</strong><br>
                    ${q.correctAnswer.ifBlank { "الإجابة الصحيحة كاملة كما هو مقرر بالمنهج." }}
                    ${if (q.explanation.isNotBlank()) "<br><span style='color: #2e7d32; font-size: 12px;'><strong>خطوات التوزيع والتوضيح:</strong> ${q.explanation}</span>" else ""}
                </div>
                """.trimIndent()
            } else {
                // Space for student to write answer if essay/problem/blank
                if (q.options.isEmpty() && q.type.name != "TRUE_FALSE") {
                    "<div style='border-bottom: 1px dotted #94a3b8; margin: 16px 0 10px 0; height: 35px;'></div>"
                } else ""
            }

            """
            <div style="margin-bottom: 16px; page-break-inside: avoid;">
                <div style="font-size: 14px; font-weight: bold; line-height: 1.6;">
                    <span>س${idx + 1}: ${q.text}</span>
                    <span style="float: left; background: #e2e8f0; padding: 2px 8px; border-radius: 4px; font-size: 12px; color: #334155;">[${q.score} ${if (q.score == 1) "درجة" else "درجات"}]</span>
                </div>
                $optHtml
                $diagramHtml
                $answerKeyDetails
            </div>
            """.trimIndent()
        }.joinToString("<hr style='border: 0; border-top: 1px dashed #cbd5e1; margin: 12px 0;'>")

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 25px; color: #1e293b; background: #fff; }
                    .header { border: 2px solid #0f172a; padding: 12px 16px; margin-bottom: 18px; border-radius: 8px; }
                    .header-top { display: flex; justify-content: space-between; font-size: 13px; font-weight: bold; }
                    .title { text-align: center; font-size: 19px; font-weight: bold; margin: 8px 0; color: #006874; }
                    .badge { text-align: center; font-size: 12px; font-weight: bold; color: ${if (isAnswerKey) "#006874" else "#475569"}; margin-bottom: 6px; }
                    .student-box { border: 1.5px solid #64748b; padding: 8px 12px; margin-top: 8px; font-size: 13px; background: #f8fafc; border-radius: 4px; }
                    .questions-container { margin-top: 16px; }
                    .footer { text-align: center; margin-top: 35px; font-size: 13px; font-weight: bold; border-top: 2px solid #cbd5e1; padding-top: 10px; color: #006874; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="header-top">
                        <div style="line-height: 1.5;">الجمهورية اليمنية<br>وزارة التربية والتعليم<br>${exam.schoolName.ifBlank { "مدرسة التعليم الأساسي والثانوي" }}</div>
                        <div style="text-align: left; line-height: 1.5;">المادة: ${exam.subject}<br>الصف: $displayGrade<br>الزمن: ${exam.durationMinutes} دقيقة</div>
                    </div>
                    <div class="title">${exam.title} (${exam.examType})</div>
                    <div class="badge">« $pageTypeTitle »</div>
                    ${if (!isAnswerKey) """
                    <div class="student-box">
                        اسم الطالب: .................................................... الشعبة: ........ رقم الجلوس: ......... الدرجة الكلية: [ &nbsp;&nbsp;&nbsp;&nbsp; / ${exam.totalScore} ]
                    </div>
                    """ else """
                    <div class="student-box" style="background: #e0f2f1; border-color: #006874; color: #004d40;">
                        <strong>الدرجة الكلية للاختبار:</strong> ${exam.totalScore} درجة • <strong>عدد الأسئلة:</strong> ${questions.size} • <strong>توزيع الدرجات:</strong> مطابق تماماً لسلم التصحيح
                    </div>
                    """}
                </div>

                <div class="questions-container">
                    $qHtml
                </div>

                <div class="footer">
                    ${if (!isAnswerKey) "« انتهت الأسئلة • مع تمنياتنا لجميع الطلاب بالتوفيق والنجاح الدائم »" else "« اعتمد نموذج الإجابة وسلم الدرجات وفق معايير التوجيه التربوي بوزارة التربية والتعليم »"}
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Builds official Yemeni Ministry Mathematics Exam paper matching the exact reference format:
     * - A4 portrait with ornamental border
     * - 3-column header (School right, Title center, Student info left)
     * - Banner: «أجب مستعيناً بالله عن جميع الأسئلة التالية بخط جميل وواضح في نفس الورقة»
     * - 3-column table: [السؤال] (right), [المحتوى والمسألة والرسومات] (center), [الدرجة] (left)
     * - High-contrast black and white print design
     */
    fun buildMathOfficialExamHtml(
        exam: ExamEntity,
        questions: List<ExamQuestion>,
        isAnswerKey: Boolean,
        customSchoolName: String = "",
        customDirectorate: String = "",
        customTeacherName: String = "",
        customGradeText: String = "",
        customAcademicYear: String = "",
        customExamDate: String = "",
        customCountry: String = "",
        customMinistry: String = ""
    ): String {
        val displayGrade = customGradeText.ifBlank { YemeniCurriculum.toDisplayGrade(exam.grade) }
        val schoolName = customSchoolName.ifBlank { exam.schoolName.ifBlank { "مدرسة المختار للتعليم الأساسي" } }
        val directorate = customDirectorate.ifBlank { "منطقة معين التعليمية" }
        val examTypeTitle = exam.title.ifBlank { "اختبار نهاية الفصل الدراسي الأول" }
        val academicYear = customAcademicYear.ifBlank { exam.academicYear.ifBlank { "2025-2026م" } }
        val country = customCountry.ifBlank { "الجمهورية اليمنية" }
        val ministry = customMinistry.ifBlank { "وزارة التربية والتعليم والبحث العلمي" }
        val dateText = customExamDate.ifBlank { "1446 / 05 / 15 هـ" }
        val teacherLabel = if (customTeacherName.isNotBlank()) "<br>أستاذ المادة: $customTeacherName" else ""

        val rowsHtml = questions.mapIndexed { idx, q ->
            val qNumArabic = when (idx + 1) {
                1 -> "١"
                2 -> "٢"
                3 -> "٣"
                4 -> "٤"
                5 -> "٥"
                6 -> "٦"
                7 -> "٧"
                8 -> "٨"
                9 -> "٩"
                10 -> "١٠"
                else -> "${idx + 1}"
            }
            val scoreArabic = when (q.score) {
                1 -> "١"
                2 -> "٢"
                3 -> "٣"
                4 -> "٤"
                5 -> "٥"
                6 -> "٦"
                7 -> "٧"
                8 -> "٨"
                9 -> "٩"
                10 -> "١٠"
                else -> "${q.score}"
            }

            val optHtml = if (q.options.isNotEmpty()) {
                "<div style='margin-right: 15px; margin-top: 8px; font-size: 14px;'>" +
                        q.options.mapIndexed { oIdx, opt ->
                            val isCorrect = isAnswerKey && opt.trim() == q.correctAnswer.trim()
                            val style = if (isCorrect) "background: #c8e6c9; font-weight: bold; padding: 2px 6px; border: 1px solid #388e3c; border-radius: 4px;" else ""
                            "<span style='$style'>(${('أ' + oIdx)}) $opt</span> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                        }.joinToString("") +
                        "</div>"
            } else ""

            val diagramHtml = if (!q.diagramType.isNullOrBlank() && q.diagramType != "NONE") {
                "<div style='text-align: center; margin: 10px auto;'>" +
                        ExamDiagramSvg.getSvg(q.diagramType, isAnswerKey) +
                        (if (!q.diagramInstruction.isNullOrBlank()) "<div style='font-size: 12px; color: #334155; margin-top: 4px;'><em>${q.diagramInstruction}</em></div>" else "") +
                        "</div>"
            } else ""

            val answerKeyDetails = if (isAnswerKey) {
                """
                <div style="background: #e8f5e9; border: 1.5px solid #4caf50; padding: 6px 12px; margin-top: 8px; font-size: 13px;">
                    <strong style="color: #1b5e20;">✓ الإجابة والحل النموذجي وسلم الدرجات:</strong><br>
                    ${q.correctAnswer.ifBlank { "الحل مطابق للخطوات المعتمدة." }}
                    ${if (q.explanation.isNotBlank()) "<br><span style='color: #2e7d32; font-size: 12px;'><strong>توزيع الدرجات:</strong> ${q.explanation}</span>" else ""}
                </div>
                """.trimIndent()
            } else {
                if (q.options.isEmpty() && q.diagramType.isNullOrBlank() && q.type.name != "TRUE_FALSE") {
                    "<div style='border: 1.5px solid #000; height: 38px; width: 140px; margin: 10px 0;'></div>"
                } else ""
            }

            """
            <tr>
                <td style="width: 8%; text-align: center; font-size: 18px; font-weight: bold; border: 1.5px solid #000; vertical-align: middle; padding: 8px 4px;">
                    $qNumArabic
                </td>
                <td style="width: 84%; border: 1.5px solid #000; padding: 8px 12px; vertical-align: top; text-align: right;">
                    <div style="font-size: 15px; font-weight: bold; line-height: 1.6;">
                        ${q.text}
                    </div>
                    $optHtml
                    $diagramHtml
                    $answerKeyDetails
                </td>
                <td style="width: 8%; text-align: center; font-size: 18px; font-weight: bold; border: 1.5px solid #000; vertical-align: middle; padding: 8px 4px;">
                    $scoreArabic
                </td>
            </tr>
            """.trimIndent()
        }.joinToString("")

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    @page {
                        size: A4 portrait;
                        margin: 10mm 10mm 12mm 10mm;
                    }
                    body {
                        font-family: 'Traditional Arabic', 'Arial', Tahoma, sans-serif;
                        direction: rtl;
                        text-align: right;
                        color: #000000;
                        background: #ffffff;
                        margin: 0;
                        padding: 0;
                    }
                    .outer-frame {
                        border: 3px double #000000;
                        outline: 1.5px dashed #000000;
                        outline-offset: 4px;
                        padding: 8px;
                        min-height: 96vh;
                        box-sizing: border-box;
                        position: relative;
                    }
                    .header-table {
                        width: 100%;
                        border-collapse: collapse;
                        border: 2px solid #000000;
                        margin-bottom: 6px;
                    }
                    .header-table td {
                        border-left: 2px solid #000000;
                        padding: 6px 8px;
                        vertical-align: top;
                    }
                    .header-right {
                        width: 32%;
                        text-align: center;
                        font-size: 13px;
                        font-weight: bold;
                        line-height: 1.6;
                    }
                    .header-center {
                        width: 38%;
                        text-align: center;
                        font-size: 15px;
                        font-weight: bold;
                        line-height: 1.6;
                        vertical-align: middle !important;
                    }
                    .header-left {
                        width: 30%;
                        text-align: right;
                        font-size: 12px;
                        font-weight: bold;
                        line-height: 1.8;
                        border-left: none !important;
                    }
                    .questions-table {
                        width: 100%;
                        border-collapse: collapse;
                        border: 2px solid #000000;
                    }
                    .banner-row th {
                        border: 1.5px solid #000000;
                        padding: 8px 10px;
                        font-size: 14px;
                        font-weight: bold;
                        text-align: center;
                        background: #fdfdfd;
                    }
                    .footer-note {
                        text-align: center;
                        margin-top: 20px;
                        font-size: 13px;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="outer-frame">
                    <!-- 3-Part Header Matching Reference Image -->
                    <table class="header-table">
                        <tr>
                            <td class="header-right">
                                $country<br>
                                $ministry<br>
                                $directorate<br>
                                <strong>$schoolName</strong>
                                $teacherLabel
                            </td>
                            <td class="header-center">
                                $examTypeTitle لمادة<br>
                                <strong>${exam.subject}</strong> للعام $academicYear
                                ${if (isAnswerKey) "<br><span style='color: #006874; font-size: 12px;'>[ نموذج الإجابة الرسمي وسلم الدرجات ]</span>" else ""}
                            </td>
                            <td class="header-left">
                                اسم الطالب: .......................................<br>
                                التاريخ: $dateText<br>
                                الفترة: ....................<br>
                                الشعبة: ....................<br>
                                الصف: $displayGrade<br>
                                الزمن: ${exam.durationMinutes} دقيقة
                            </td>
                        </tr>
                    </table>

                    <!-- 3-Column Questions Table Matching Reference Image -->
                    <table class="questions-table">
                        <tr class="banner-row">
                            <th style="width: 8%;">السؤال</th>
                            <th style="width: 84%;">أجب مستعيناً بالله عن جميع الأسئلة التالية بخط جميل وواضح في نفس الورقة</th>
                            <th style="width: 8%;">الدرجة</th>
                        </tr>
                        $rowsHtml
                    </table>

                    <!-- Score Summary Table Matching Reference Image -->
                    <table style="width: 100%; border-collapse: collapse; border: 2px solid #0052cc; margin-top: 14px; text-align: center; font-size: 13px;">
                        <tr style="background: #e6f0ff; font-weight: bold; color: #0033aa;">
                            <td style="border: 1.5px solid #0052cc; padding: 6px;">السؤال الأول</td>
                            <td style="border: 1.5px solid #0052cc; padding: 6px;">الثاني</td>
                            <td style="border: 1.5px solid #0052cc; padding: 6px;">الثالث</td>
                            <td style="border: 1.5px solid #0052cc; padding: 6px;">الرابع</td>
                            <td style="border: 1.5px solid #0052cc; padding: 6px; font-weight: bold;">المجموع</td>
                        </tr>
                        <tr style="height: 32px;">
                            <td style="border: 1.5px solid #0052cc;"></td>
                            <td style="border: 1.5px solid #0052cc;"></td>
                            <td style="border: 1.5px solid #0052cc;"></td>
                            <td style="border: 1.5px solid #0052cc;"></td>
                            <td style="border: 1.5px solid #0052cc; font-weight: bold;"></td>
                        </tr>
                    </table>

                    <!-- Page Footer Matching Reference Image -->
                    <div class="footer-note">
                        الصفحة 1 من 2 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; « انتهت الأسئلة • تمنياتنا لكم بالتوفيق والنجاح »
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Print Lesson via Android PrintManager (PDF / Printer)
     */
    fun printLesson(context: Context, lesson: LessonEntity) {
        val html = buildLessonHtml(lesson)
        doPrint(context, "تحضير_درس_${lesson.id}_${lesson.title.take(15)}", html)
    }

    /**
     * Print Exam via Android PrintManager (PDF / Printer)
     * Automatically applies the official Math template for math exams or when requested
     */
    fun printExam(
        context: Context,
        exam: ExamEntity,
        questions: List<ExamQuestion>,
        isAnswerKey: Boolean = false,
        useOfficialMathTemplate: Boolean = false,
        customSchoolName: String = "",
        customDirectorate: String = "",
        customTeacherName: String = "",
        customGradeText: String = "",
        customAcademicYear: String = "",
        customExamDate: String = "",
        customCountry: String = "",
        customMinistry: String = ""
    ) {
        val isMath = useOfficialMathTemplate || exam.subject.contains("الرياضيات") || exam.subject.contains("رياضيات")
        val html = if (isMath) {
            buildMathOfficialExamHtml(
                exam = exam,
                questions = questions,
                isAnswerKey = isAnswerKey,
                customSchoolName = customSchoolName,
                customDirectorate = customDirectorate,
                customTeacherName = customTeacherName,
                customGradeText = customGradeText,
                customAcademicYear = customAcademicYear,
                customExamDate = customExamDate,
                customCountry = customCountry,
                customMinistry = customMinistry
            )
        } else {
            buildExamHtml(exam, questions, isAnswerKey)
        }
        val prefix = if (isAnswerKey) "نموذج_إجابة_اختبار" else "ورقة_اختبار_طالب"
        doPrint(context, "${prefix}_${exam.id}_${exam.subject}", html)
    }

    /**
     * Export Lesson to Word (.doc) format with Arabic RTL and open share sheet
     */
    fun exportLessonToWord(context: Context, lesson: LessonEntity) {
        val html = buildLessonHtml(lesson)
        val wordHtml = wrapHtmlForWord("تحضير درس - " + lesson.title, html)
        val fileName = "تحضير_${lesson.subject}_${lesson.title.take(12).replace(" ", "_")}.doc"
        saveAndShareFile(context, fileName, wordHtml, "application/msword")
    }

    /**
     * Export Exam to Word (.doc) format with Arabic RTL and open share sheet
     * Supports both the official Math template and standard template
     */
    fun exportExamToWord(
        context: Context,
        exam: ExamEntity,
        questions: List<ExamQuestion>,
        isAnswerKey: Boolean = false,
        useOfficialMathTemplate: Boolean = false,
        customSchoolName: String = "",
        customDirectorate: String = "",
        customTeacherName: String = "",
        customGradeText: String = "",
        customAcademicYear: String = "",
        customExamDate: String = "",
        customCountry: String = "",
        customMinistry: String = ""
    ) {
        val isMath = useOfficialMathTemplate || exam.subject.contains("الرياضيات") || exam.subject.contains("رياضيات")
        val html = if (isMath) {
            buildMathOfficialExamHtml(
                exam = exam,
                questions = questions,
                isAnswerKey = isAnswerKey,
                customSchoolName = customSchoolName,
                customDirectorate = customDirectorate,
                customTeacherName = customTeacherName,
                customGradeText = customGradeText,
                customAcademicYear = customAcademicYear,
                customExamDate = customExamDate,
                customCountry = customCountry,
                customMinistry = customMinistry
            )
        } else {
            buildExamHtml(exam, questions, isAnswerKey)
        }
        val docTitle = if (isAnswerKey) "نموذج إجابة اختبار - ${exam.title}" else "اختبار - ${exam.title}"
        val wordHtml = wrapHtmlForWord(docTitle, html)
        val prefix = if (isAnswerKey) "نموذج_إجابة" else "اختبار"
        val fileName = "${prefix}_${exam.subject}_${exam.grade.take(5)}.doc"
        saveAndShareFile(context, fileName, wordHtml, "application/msword")
    }

    /**
     * Builds comprehensive, professional HTML for Smart Explanation (Pedagogical Guide)
     */
    fun buildSmartExplanationHtml(explanation: SmartExplanationResult): String {
        val conceptsHtml = explanation.concepts.joinToString("") { c ->
            """
            <div style="border: 1px solid #cbd5e1; border-radius: 8px; padding: 12px; margin-bottom: 12px; background: #ffffff;">
                <h3 style="color: #006874; margin-top: 0; margin-bottom: 6px;">🔹 ${c.title}</h3>
                <p style="margin: 4px 0; line-height: 1.6;"><strong>الشرح العميق:</strong> ${c.deepExplanation}</p>
                ${if (c.whyAndHow.isNotBlank()) "<p style='margin: 4px 0; color: #334155;'><strong>لماذا وكيف تحدث؟</strong> ${c.whyAndHow}</p>" else ""}
                ${if (c.causeAndEffect.isNotBlank()) "<p style='margin: 4px 0; color: #475569;'><strong>الأسباب والنتائج:</strong> ${c.causeAndEffect}</p>" else ""}
                ${if (c.commonMisconception.isNotBlank()) "<div style='background: #fee2e2; border-right: 4px solid #ef4444; padding: 6px 10px; margin-top: 6px; font-size: 13px; color: #991b1b;'><strong>⚠️ المفهوم الخاطئ الشائع:</strong> ${c.commonMisconception}</div>" else ""}
                ${if (c.studentQuestion.isNotBlank()) "<div style='background: #f0fdf4; border-right: 4px solid #22c55e; padding: 6px 10px; margin-top: 6px; font-size: 13px; color: #166534;'><strong>❓ سؤال الطالب المتوقع:</strong> ${c.studentQuestion}<br><strong>💡 إجابة المعلم:</strong> ${c.studentQuestionAnswer}</div>" else ""}
            </div>
            """.trimIndent()
        }

        val analogiesHtml = explanation.yemeniAnalogies.joinToString("") { a ->
            """
            <div style="background: #fefce8; border: 1.5px solid #facc15; border-radius: 8px; padding: 10px 14px; margin-bottom: 10px;">
                <h4 style="color: #854d0e; margin: 0 0 4px 0;">🇾🇪 ${a.title}</h4>
                <p style="margin: 4px 0; font-size: 14px; font-weight: bold; color: #713f12;">« ${a.analogyText} »</p>
                ${if (a.spokenDialect.isNotBlank()) "<p style='margin: 3px 0; font-size: 13px; color: #a16207;'><strong>العبارة المنطوقة:</strong> ${a.spokenDialect}</p>" else ""}
                <p style="margin: 3px 0; font-size: 12px; color: #475569;"><strong>الربط العلمي:</strong> ${a.explanationLink}</p>
            </div>
            """.trimIndent()
        }

        val examplesHtml = explanation.yemeniExamples.joinToString("") { e ->
            """
            <div style="background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 10px 14px; margin-bottom: 10px;">
                <h4 style="color: #166534; margin: 0 0 4px 0;">🌾 ${e.title} <span style="font-size: 11px; background: #dcfce7; padding: 2px 6px; border-radius: 4px;">${e.contextType}</span></h4>
                <p style="margin: 4px 0; line-height: 1.5;">${e.description}</p>
                <p style="margin: 4px 0; font-size: 13px; color: #15803d;"><strong>كيفية توظيفه في الحصة:</strong> ${e.howToApply}</p>
            </div>
            """.trimIndent()
        }

        val deliveryHtml = explanation.deliveryGuide.joinToString("") { d ->
            """
            <tr style="border-bottom: 1px solid #e2e8f0;">
                <td style="padding: 8px; font-weight: bold; color: #006874; width: 25%;">${d.stepTitle}</td>
                <td style="padding: 8px; width: 40%; font-size: 13px;">${d.stepDetail}</td>
                <td style="padding: 8px; width: 35%; font-size: 13px; background: #f8fafc; color: #0f172a;">${d.teacherScript}</td>
            </tr>
            """.trimIndent()
        }

        val methodsHtml = explanation.teachingMethods.joinToString("") { m ->
            """
            <div style="border-right: 4px solid #6366f1; background: #f8fafc; padding: 8px 12px; margin-bottom: 8px; border-radius: 0 6px 6px 0;">
                <h4 style="color: #4338ca; margin: 0 0 4px 0;">🎯 ${m.strategyName}</h4>
                <p style="margin: 3px 0; font-size: 13px;"><strong>الخطوات الصفية:</strong> ${m.executionSteps}</p>
                <p style="margin: 3px 0; font-size: 12px; color: #475569;"><strong>الأثر المتوقع:</strong> ${m.expectedOutcome}</p>
            </div>
            """.trimIndent()
        }

        val remediationHtml = explanation.remediation.joinToString("") { r ->
            """
            <div style="background: #fff7ed; border: 1px solid #ffedd5; border-radius: 6px; padding: 8px 12px; margin-bottom: 8px;">
                <strong style="color: #c2410c;">نقطة الصعوبة:</strong> ${r.difficulty}<br>
                <span style="font-size: 13px; color: #9a3412;"><strong>الصياغة البديلة:</strong> ${r.simplerExplanation}</span><br>
                <span style="font-size: 13px; color: #475569;"><strong>التوضيح المحسوس:</strong> ${r.visualOrPhysicalDemonstration}</span>
            </div>
            """.trimIndent()
        }

        val questionsHtml = explanation.interactiveQuestions.joinToString("") { q ->
            """
            <div style="border: 1px solid #e2e8f0; border-radius: 6px; padding: 8px 12px; margin-bottom: 8px; background: #ffffff;">
                <p style="margin: 2px 0; font-weight: bold; color: #1e293b;">❓ ${q.question}</p>
                <p style="margin: 2px 0; font-size: 13px; color: #166534;"><strong>الإجابة المتوقعة:</strong> ${q.expectedAnswer}</p>
                <p style="margin: 2px 0; font-size: 12px; color: #dc2626;"><strong>توجيه المعلم عند الخطأ:</strong> ${q.teacherFeedbackIfWrong}</p>
                ${if (q.followUpQuestion.isNotBlank()) "<p style='margin: 2px 0; font-size: 12px; color: #0284c7;'><strong>السؤال التالي:</strong> ${q.followUpQuestion}</p>" else ""}
            </div>
            """.trimIndent()
        }

        val gradedHtml = explanation.gradedExamples.joinToString("") { g ->
            """
            <div style="border: 1px solid #cbd5e1; border-radius: 6px; padding: 10px; margin-bottom: 10px; background: #ffffff;">
                <span style="background: #e0f2f1; color: #004d40; font-size: 11px; font-weight: bold; padding: 2px 8px; border-radius: 4px;">${g.level}</span>
                <p style="margin: 6px 0; font-weight: bold;">${g.problem}</p>
                <div style="background: #f8fafc; padding: 8px; border-radius: 4px; font-size: 13px; line-height: 1.6; white-space: pre-line;">${g.stepByStepSolution}</div>
                ${if (g.pedagogicalTip.isNotBlank()) "<p style='margin: 4px 0; font-size: 12px; color: #0284c7;'><strong>توجيه تربوي:</strong> ${g.pedagogicalTip}</p>" else ""}
            </div>
            """.trimIndent()
        }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 25px; color: #1e293b; background: #fff; direction: rtl; }
                    .header-box { border: 2px solid #006874; border-radius: 8px; padding: 12px 18px; margin-bottom: 18px; text-align: center; }
                    .country { font-size: 13px; font-weight: bold; color: #475569; }
                    .main-title { font-size: 22px; font-weight: bold; color: #006874; margin: 6px 0; }
                    .sub-meta { font-size: 13px; color: #334155; }
                    .section-head { background: #e0f2f1; color: #004d40; padding: 8px 14px; font-weight: bold; font-size: 15px; margin-top: 18px; margin-bottom: 10px; border-radius: 6px; border-right: 5px solid #006874; }
                    .box-content { border: 1px solid #e2e8f0; padding: 12px 16px; font-size: 13px; line-height: 1.7; white-space: pre-line; border-radius: 6px; background: #f8fafc; margin-bottom: 12px; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
                    th { background: #e2e8f0; padding: 8px; text-align: right; font-size: 13px; border: 1px solid #cbd5e1; }
                    td { border: 1px solid #cbd5e1; }
                    .footer { text-align: center; margin-top: 30px; font-size: 11px; color: #64748b; border-top: 1px solid #cbd5e1; padding-top: 10px; }
                </style>
            </head>
            <body>
                <div class="header-box">
                    <div class="country">الجمهورية اليمنية • وزارة التربية والتعليم</div>
                    <div class="main-title">دليل الشرح الذكي والمساعد التربوي: ${explanation.lessonTitle}</div>
                    <div class="sub-meta">المادة: ${explanation.subject} • الصف: ${explanation.grade} • مستوى التوسع: ${explanation.expansionLevel} • اللهجة: ${explanation.dialectStyle}</div>
                </div>

                <div class="section-head">🔬 أولاً: الشرح العميق والمفاهيم الأساسية وتفكيك الصعوبات</div>
                $conceptsHtml

                ${if (analogiesHtml.isNotBlank()) "<div class='section-head'>🇾🇪 ثانياً: التشبيهات باللهجة اليمنية («قرّب المعلومة للطالب»)</div>$analogiesHtml" else ""}

                ${if (examplesHtml.isNotBlank()) "<div class='section-head'>🌾 ثالثاً: أمثلة مستمدة من واقع البيئة اليمنية</div>$examplesHtml" else ""}

                ${if (explanation.deliveryGuide.isNotEmpty()) """
                <div class="section-head">🔟 رابعاً: دليل إلقاء الفكرة (10 خطوات عملية للمعلم داخل الفصل)</div>
                <table>
                    <thead>
                        <tr>
                            <th>الخطوة والهدف</th>
                            <th>الإجراء والنشاط</th>
                            <th>ما يقوله المعلم شفوياً</th>
                        </tr>
                    </thead>
                    <tbody>
                        $deliveryHtml
                    </tbody>
                </table>
                """ else ""}

                ${if (methodsHtml.isNotBlank()) "<div class='section-head'>🎯 خامساً: استراتيجيات التدريس الصفية وتجارب بدون تكاليف</div>$methodsHtml" else ""}

                ${if (remediationHtml.isNotBlank()) "<div class='section-head'>🤝 سادساً: علاج تعثر الفهم («إذا لم يفهم الطالب»)</div>$remediationHtml" else ""}

                ${if (questionsHtml.isNotBlank()) "<div class='section-head'>❓ سابعاً: أسئلة صفية مثيرة للتفكير مع إدارتها تربوياً</div>$questionsHtml" else ""}

                ${if (gradedHtml.isNotBlank()) "<div class='section-head'>📊 ثامناً: مسائل وتطبيقات متدرجة محلولة بالكامل</div>$gradedHtml" else ""}

                ${if (explanation.blackboardPlan.isNotBlank()) """
                <div class="section-head">📋 تاسعاً: تخطيط وتنظيم السبورة الصفية</div>
                <div class="box-content">${explanation.blackboardPlan}</div>
                """ else ""}

                ${if (explanation.classroomScript.isNotBlank()) """
                <div class="section-head">⏱️ عاشراً: سيناريو إلقاء الحصة كاملاً (45 دقيقة)</div>
                <div class="box-content">${explanation.classroomScript}</div>
                """ else ""}

                ${if (explanation.teacherEnrichment.isNotBlank()) """
                <div class="section-head">💡 حادي عشر: معلومات إثرائية لتعميق تمكن المعلم وثقافته التخصصية</div>
                <div class="box-content">${explanation.teacherEnrichment}</div>
                """ else ""}

                <div class="footer">
                    تم إعداد هذا الدليل عبر تطبيق «رقيب – مساعد المعلم الذكي» • وفق المعايير التربوية لوزارة التربية والتعليم بالجمهورية اليمنية
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun printSmartExplanation(context: Context, explanation: SmartExplanationResult) {
        val html = buildSmartExplanationHtml(explanation)
        val docName = "شرح_${explanation.subject}_${explanation.lessonTitle.take(10).replace(" ", "_")}"
        doPrint(context, docName, html)
    }

    fun exportSmartExplanationToWord(context: Context, explanation: SmartExplanationResult) {
        val html = buildSmartExplanationHtml(explanation)
        val docTitle = "دليل الشرح الذكي - ${explanation.lessonTitle}"
        val wordHtml = wrapHtmlForWord(docTitle, html)
        val fileName = "شرح_${explanation.subject}_${explanation.lessonTitle.take(10).replace(" ", "_")}.doc"
        saveAndShareFile(context, fileName, wordHtml, "application/msword")
    }

    private fun wrapHtmlForWord(title: String, contentHtml: String): String {
        return """
            <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
            <head>
                <meta charset="utf-8">
                <title>$title</title>
                <!--[if gte mso 9]>
                <xml>
                <w:WordDocument>
                    <w:View>Print</w:View>
                    <w:Zoom>100</w:Zoom>
                    <w:DoNotOptimizeForBrowser/>
                </w:WordDocument>
                </xml>
                <![endif]-->
                <style>
                    @page Section1 { size: 595.45pt 841.7pt; margin: 1.0in 1.0in 1.0in 1.0in; mso-header-margin: .5in; mso-footer-margin: .5in; mso-paper-source: 0; }
                    div.Section1 { page: Section1; }
                    body { font-family: 'Traditional Arabic', 'Arial', sans-serif; direction: rtl; text-align: right; }
                </style>
            </head>
            <body dir="rtl">
                <div class="Section1">
                    $contentHtml
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun saveAndShareFile(context: Context, fileName: String, content: String, mimeType: String) {
        try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "تصدير ومشاركة مستند Word: $fileName"))
            Toast.makeText(context, "تم تجهيز المستند للتصدير بنجاح!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ في التصدير: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Builds clean formatted HTML for Student Comprehensive Performance & Attendance Report
     */
    fun buildStudentReportHtml(
        student: StudentEntity,
        schoolName: String,
        teacherName: String,
        subjectName: String,
        customNotes: String = ""
    ): String {
        val displayGrade = YemeniCurriculum.toDisplayGrade(student.grade)
        val sName = student.fullName
        val parent = student.parentName.ifBlank { "ولي الأمر المحترم" }
        val school = schoolName.ifBlank { student.schoolName.ifBlank { "مدرسة التعليم الأساسي والثانوي" } }
        val teacher = teacherName.ifBlank { "أستاذ المادة" }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; color: #1e293b; background: #fff; direction: rtl; }
                    .header-box { border: 2px solid #25D366; border-radius: 10px; padding: 12px 18px; margin-bottom: 16px; text-align: center; background: #f0fdf4; }
                    .country { font-size: 13px; font-weight: bold; color: #166534; }
                    .main-title { font-size: 20px; font-weight: bold; color: #15803d; margin: 4px 0; }
                    .school-sub { font-size: 13px; color: #166534; }
                    .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 14px; }
                    .meta-table td { border: 1px solid #bbf7d0; padding: 6px 10px; font-size: 13px; }
                    .meta-table td strong { color: #15803d; }
                    .section-head { background: #dcfce7; color: #14532d; padding: 6px 12px; font-weight: bold; font-size: 14px; margin-top: 12px; border-radius: 6px; border-right: 5px solid #16a34a; }
                    .scores-table { width: 100%; border-collapse: collapse; margin-top: 8px; text-align: center; font-size: 13px; }
                    .scores-table th { background: #16a34a; color: #fff; padding: 8px; border: 1px solid #15803d; }
                    .scores-table td { padding: 7px; border: 1px solid #cbd5e1; }
                    .box-body { border: 1px solid #e2e8f0; padding: 10px 14px; margin-top: 4px; font-size: 13px; line-height: 1.6; border-radius: 6px; background: #f8fafc; }
                    .footer { text-align: center; margin-top: 25px; font-size: 11px; color: #64748b; border-top: 1px solid #cbd5e1; padding-top: 8px; }
                </style>
            </head>
            <body>
                <div class="header-box">
                    <div class="country">الجمهورية اليمنية • وزارة التربية والتعليم</div>
                    <div class="main-title">تقرير أداء الطالب ومتابعة ولي الأمر</div>
                    <div class="school-sub">مدرسة: $school • العام الدراسي: ${student.academicYear}</div>
                </div>

                <table class="meta-table">
                    <tr>
                        <td><strong>اسم الطالب:</strong> $sName</td>
                        <td><strong>الرقم المدرسي:</strong> ${student.schoolNumber.ifBlank { student.seatNumber.ifBlank { "غير مسجل" } }}</td>
                        <td><strong>الصف والشعبة:</strong> $displayGrade (${student.section})</td>
                    </tr>
                    <tr>
                        <td><strong>اسم ولي الأمر:</strong> $parent</td>
                        <td><strong>رقم التواصل:</strong> ${student.parentPhone.ifBlank { "غير مسجل" }} (${student.parentRelationship})</td>
                        <td><strong>أستاذ المادة:</strong> $teacher</td>
                    </tr>
                </table>

                <div class="section-head">📊 أولاً: السجل الأكاديمي والتقييمات بالدرجات</div>
                <table class="scores-table">
                    <thead>
                        <tr>
                            <th>اختبار الشهر (20)</th>
                            <th>منتصف الفصل (20)</th>
                            <th>التقييم النهائي (20)</th>
                            <th>الواجبات (10)</th>
                            <th>المشاركة (10)</th>
                            <th>التقدير العام</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>${student.monthlyTestScore}</td>
                            <td>${student.termScore}</td>
                            <td>${student.finalExamScore}</td>
                            <td>${student.homeworkScore}</td>
                            <td>${student.activityScore}</td>
                            <td><strong>${student.generalRating}</strong></td>
                        </tr>
                    </tbody>
                </table>

                <div class="section-head">🌟 ثانياً: التقييم التربوي ونقاط القوة والتحسين</div>
                <div class="box-body">
                    <strong>• نقاط القوة والتميز:</strong><br>
                    ${student.strengths.ifBlank { "مواظبة وانضباط وتفاعل جيد أثناء الحصة." }}
                    <br><br>
                    <strong>• المهارات والتوصيات المستهدفة للتحسين:</strong><br>
                    ${student.weaknesses.ifBlank { "المراجعة المستمرة وإنجاز التكليفات أولاً بأول." }}
                </div>

                ${if (customNotes.isNotBlank() || student.notes.isNotBlank()) """
                <div class="section-head">📝 ثالثاً: ملاحظات معلم المادة لولي الأمر</div>
                <div class="box-body">
                    ${customNotes.ifBlank { student.notes }}
                </div>
                """ else ""}

                <div class="footer">
                    تم إصدار هذا التقرير رسمياً عبر تطبيق «رقيب – مساعد المعلم الذكي» للتواصل المستمر مع أولياء الأمور
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun printStudentReport(
        context: Context,
        student: StudentEntity,
        schoolName: String,
        teacherName: String,
        subjectName: String,
        customNotes: String = ""
    ) {
        val html = buildStudentReportHtml(student, schoolName, teacherName, subjectName, customNotes)
        doPrint(context, "تقرير_الطالب_${student.fullName.take(10)}", html)
    }

    fun exportStudentReportToWord(
        context: Context,
        student: StudentEntity,
        schoolName: String,
        teacherName: String,
        subjectName: String,
        customNotes: String = ""
    ) {
        val html = buildStudentReportHtml(student, schoolName, teacherName, subjectName, customNotes)
        val wordHtml = wrapHtmlForWord("تقرير الطالب - ${student.fullName}", html)
        val fileName = "تقرير_${student.fullName.take(10).replace(" ", "_")}.doc"
        saveAndShareFile(context, fileName, wordHtml, "application/msword")
    }

    private fun doPrint(context: Context, docName: String, htmlContent: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = view.createPrintDocumentAdapter(docName)
                printManager?.print(docName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }
}
