package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.ExamEntity
import com.example.data.model.ExamQuestion
import com.example.data.model.LessonEntity
import com.example.data.model.YemeniCurriculum

object PrintUtils {

    fun printLesson(context: Context, lesson: LessonEntity) {
        val displayGrade = YemeniCurriculum.toDisplayGrade(lesson.grade)
        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: sans-serif; margin: 20px; color: #222; }
                    .header { text-align: center; border-bottom: 2px solid #006874; padding-bottom: 10px; margin-bottom: 15px; }
                    .title { font-size: 20px; font-weight: bold; color: #006874; margin: 5px 0; }
                    .subtitle { font-size: 14px; color: #555; }
                    .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 15px; }
                    .meta-table td { border: 1px solid #ccc; padding: 6px 10px; font-size: 13px; }
                    .section-title { background: #e0f2f1; padding: 6px 10px; font-weight: bold; font-size: 14px; margin-top: 10px; border-radius: 4px; }
                    .content-box { border: 1px solid #e0e0e0; padding: 8px 12px; margin-top: 4px; font-size: 13px; line-height: 1.6; white-space: pre-line; }
                    .footer { text-align: center; margin-top: 25px; font-size: 11px; color: #777; border-top: 1px solid #ddd; padding-top: 8px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div>الجمهورية اليمنية - وزارة التربية والتعليم</div>
                    <div class="title">خطة تحضير درس: ${lesson.title}</div>
                    <div class="subtitle">${lesson.schoolName} • ${lesson.governorate} (${lesson.directorate})</div>
                </div>

                <table class="meta-table">
                    <tr>
                        <td><strong>المادة:</strong> ${lesson.subject}</td>
                        <td><strong>الصف الدراسي:</strong> $displayGrade</td>
                        <td><strong>التاريخ:</strong> ${lesson.lessonDate}</td>
                    </tr>
                    <tr>
                        <td><strong>الوحدة:</strong> ${lesson.unitName}</td>
                        <td><strong>الحصة:</strong> ${lesson.periodNumber}</td>
                        <td><strong>الزمن:</strong> ${lesson.durationMinutes} دقيقة</td>
                    </tr>
                    <tr>
                        <td colspan="3"><strong>المرجع:</strong> ${lesson.textbookReference} (${lesson.curriculumEdition})</td>
                    </tr>
                </table>

                <div class="section-title">الأهداف السلوكية (المعرفية والمهارية والوجدانية)</div>
                <div class="content-box">
                    <strong>الأهداف المعرفية:</strong><br>${lesson.cognitiveObjectives}
                    <br><br>
                    <strong>الأهداف المهارية:</strong><br>${lesson.skillObjectives}
                    <br><br>
                    <strong>الأهداف الوجدانية:</strong><br>${lesson.affectiveObjectives}
                </div>

                <div class="section-title">التهيئة والمقدمة والربط بالدرس السابق</div>
                <div class="content-box">${lesson.introduction}</div>

                <div class="section-title">خطوات وإجراءات سير الدرس والأنشطة</div>
                <div class="content-box">${lesson.teachingSteps}</div>

                <div class="section-title">التقويم التكويني والواجب المنزلي</div>
                <div class="content-box">
                    <strong>التقويم التكويني:</strong> ${lesson.formativeAssessment}<br>
                    <strong>الواجب المنزلي:</strong> ${lesson.homework}
                </div>

                <div class="footer">
                    تم إنشاء هذا التحضير عبر تطبيق «رقيب – مساعد المعلم الذكي» وفق معايير وزارة التربية والتعليم في اليمن
                </div>
            </body>
            </html>
        """.trimIndent()

        doPrint(context, "Lesson_${lesson.id}", html)
    }

    fun printExam(context: Context, exam: ExamEntity, questions: List<ExamQuestion>) {
        val displayGrade = YemeniCurriculum.toDisplayGrade(exam.grade)
        val qHtml = questions.mapIndexed { idx, q ->
            val optHtml = if (q.options.isNotEmpty()) {
                "<div style='margin-right: 20px; margin-top: 4px;'>" +
                        q.options.mapIndexed { oIdx, opt -> "<span>(${('أ' + oIdx)}) $opt</span> &nbsp;&nbsp;&nbsp;&nbsp;" }.joinToString("") +
                        "</div>"
            } else ""

            """
                <div style="margin-bottom: 12px;">
                    <strong>س${idx + 1}:</strong> ${q.text} <span style="float: left; color: #666;">[${q.score} درجات]</span>
                    $optHtml
                </div>
            """.trimIndent()
        }.joinToString("<hr style='border: 0; border-top: 1px dashed #ddd; margin: 10px 0;'>")

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: sans-serif; margin: 25px; color: #222; }
                    .header { border: 2px solid #333; padding: 12px; margin-bottom: 18px; border-radius: 6px; }
                    .header-top { display: flex; justify-content: space-between; font-size: 13px; font-weight: bold; }
                    .title { text-align: center; font-size: 18px; font-weight: bold; margin: 8px 0; color: #006874; }
                    .student-box { border: 1px solid #999; padding: 6px 10px; margin-top: 8px; font-size: 13px; }
                    .questions-container { margin-top: 15px; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="header-top">
                        <div>الجمهورية اليمنية<br>وزارة التربية والتعليم<br>${exam.schoolName}</div>
                        <div style="text-align: left;">المادة: ${exam.subject}<br>الصف: $displayGrade<br>الزمن: ${exam.durationMinutes} دقيقة</div>
                    </div>
                    <div class="title">${exam.title} (${exam.examType})</div>
                    <div class="student-box">
                        اسم الطالب: .................................................... الشعبة: ........ رقم الجلوس: ......... الدرجة: [ &nbsp;&nbsp;&nbsp;&nbsp; / ${exam.totalScore} ]
                    </div>
                </div>

                <div class="questions-container">
                    $qHtml
                </div>

                <div class="footer">
                    «مع تمنياتنا لجميع أبنائنا الطلبة بالتوفيق والنجاح»
                </div>
            </body>
            </html>
        """.trimIndent()

        doPrint(context, "Exam_${exam.id}", html)
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
