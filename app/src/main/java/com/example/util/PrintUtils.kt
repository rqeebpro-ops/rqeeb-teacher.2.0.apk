package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.model.ExamEntity
import com.example.data.model.ExamQuestion
import com.example.data.model.LessonEntity
import com.example.data.model.YemeniCurriculum

object PrintUtils {

    /**
     * Print or export a prepared lesson as official Yemeni Ministry PDF format
     */
    fun printLesson(context: Context, lesson: LessonEntity) {
        val gov = if (lesson.governorate.isNotBlank()) lesson.governorate else "أمانة العاصمة"
        val dir = if (lesson.directorate.isNotBlank()) lesson.directorate else "المديرية التعليمية"
        val sch = if (lesson.schoolName.isNotBlank()) lesson.schoolName else "المدرسة النموذجية"

        // Parse steps if stored as bullet points or format rows
        val stepsRowsHtml = StringBuilder()
        val lines = lesson.detailedExplanation.lines()
        var currentStage = ""
        var currentTeacher = ""
        var currentStudent = ""
        var currentMaterial = lesson.teachingMaterials.split("،").firstOrNull()?.trim() ?: "الكتاب المدرسي"
        var currentMethod = lesson.strategies.split("،").firstOrNull()?.trim() ?: "الحوار والمناقشة"

        // Check if detailedExplanation contains lines starting with • or format rows
        val stepBlocks = lesson.detailedExplanation.split("\n\n").filter { it.isNotBlank() }
        if (stepBlocks.isNotEmpty()) {
            stepBlocks.forEachIndexed { idx, block ->
                val linesInBlock = block.lines().map { it.trim() }
                val stageTitle = linesInBlock.firstOrNull()?.removePrefix("•")?.substringBefore(":")?.trim() 
                    ?: when(idx) {
                        0 -> "التهيئة والتمهيد (5 د)"
                        1 -> "عرض المفهوم والشرح (18 د)"
                        2 -> "النشاط التطبيقي (14 د)"
                        else -> "التقويم الختامي والغلق (8 د)"
                    }
                val teacherAction = linesInBlock.find { it.contains("إجراء") || it.contains("المعلم") } 
                    ?: linesInBlock.getOrElse(0) { "شرح وتوضيح محاور الدرس" }
                val studentAct = lesson.studentActivities.lines().getOrNull(idx)?.removePrefix("•")?.trim()
                    ?: "المشاركة الفعالة والتفاعل مع الأنشطة وتدوين الملاحظات"
                val aid = lesson.teachingMaterials.split("،").getOrNull(idx)?.trim()
                    ?: lesson.teachingMaterials.split("،").firstOrNull()?.trim() ?: "الكتاب المدرسي"
                val method = lesson.strategies.split("،").getOrNull(idx)?.trim()
                    ?: lesson.strategies.split("،").firstOrNull()?.trim() ?: "التعلم التعاوني"

                stepsRowsHtml.append("""
                    <tr>
                        <td style="font-weight:bold; width:16%; text-align:center; background:#f8fafc;">$stageTitle</td>
                        <td style="width:30%;">$teacherAction</td>
                        <td style="width:26%;">$studentAct</td>
                        <td style="width:14%; text-align:center;">$aid</td>
                        <td style="width:14%; text-align:center;">$method</td>
                    </tr>
                """.trimIndent())
            }
        }

        if (stepsRowsHtml.isEmpty()) {
            stepsRowsHtml.append("""
                <tr>
                    <td style="font-weight:bold; text-align:center; background:#f8fafc;">التهيئة والتمهيد (5 د)</td>
                    <td>${lesson.introduction}</td>
                    <td>الاستماع والتفاعل ومناقشة السؤال الاستهلالي</td>
                    <td style="text-align:center;">${lesson.teachingMaterials.split("،").firstOrNull() ?: "الكتاب المدرسي"}</td>
                    <td style="text-align:center;">${lesson.strategies.split("،").firstOrNull() ?: "الحوار والمناقشة"}</td>
                </tr>
                <tr>
                    <td style="font-weight:bold; text-align:center; background:#f8fafc;">عرض الدرس والشرح (20 د)</td>
                    <td>${lesson.teacherActivities}</td>
                    <td>${lesson.studentActivities}</td>
                    <td style="text-align:center;">${lesson.teachingMaterials}</td>
                    <td style="text-align:center;">${lesson.strategies}</td>
                </tr>
                <tr>
                    <td style="font-weight:bold; text-align:center; background:#f8fafc;">التقويم والغلق (10 د)</td>
                    <td>${lesson.formativeAssessment}</td>
                    <td>تقديم الإجابات وتقييم الأقران وتلخيص الدرس</td>
                    <td style="text-align:center;">الكتاب المدرسي</td>
                    <td style="text-align:center;">التعزيز والمناقشة</td>
                </tr>
            """.trimIndent())
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 12px; color: #1e293b; line-height: 1.45; font-size: 12px; }
                    .header-box { display: flex; justify-content: space-between; border-bottom: 2px solid #0D47A1; padding-bottom: 8px; margin-bottom: 10px; }
                    .header-side { text-align: right; width: 33%; font-size: 11px; line-height: 1.4; }
                    .header-center { text-align: center; width: 34%; }
                    .header-center h1 { color: #0D47A1; margin: 0; font-size: 17px; }
                    .header-center h2 { color: #0284C7; margin: 3px 0; font-size: 13px; }
                    .header-left { text-align: left; width: 33%; font-size: 11px; line-height: 1.4; }
                    
                    .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 10px; font-size: 11px; }
                    .meta-table td { padding: 5px 8px; border: 1px solid #94a3b8; }
                    .meta-table th { background: #f1f5f9; padding: 5px 8px; border: 1px solid #94a3b8; color: #0f172a; font-weight: bold; }
                    
                    .section-card { border: 1px solid #94a3b8; margin-bottom: 10px; page-break-inside: avoid; border-radius: 6px; overflow: hidden; word-break: break-word; overflow-wrap: break-word; }
                    .section-header { background: #0D47A1; color: #ffffff; padding: 6px 12px; font-weight: bold; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
                    .section-body { padding: 8px 12px; font-size: 11px; white-space: pre-line; word-break: break-word; overflow-wrap: break-word; }
                    
                    .choices-grid, .dashboard-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 10px; margin-bottom: 10px; }
                    .choice-box, .dashboard-card { border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px 10px; background: #f8fafc; font-size: 11px; overflow: hidden; word-break: break-word; overflow-wrap: break-word; }
                    .choice-box strong, .dashboard-card strong { color: #0369a1; display: block; margin-bottom: 3px; border-bottom: 1px solid #e2e8f0; padding-bottom: 2px; overflow: hidden; text-overflow: ellipsis; }
                    
                    .steps-table { width: 100%; border-collapse: collapse; margin-bottom: 10px; font-size: 11px; word-break: break-word; }
                    .steps-table th { background: #0369a1; color: white; padding: 6px 8px; border: 1px solid #0284c7; font-size: 11px; text-align: center; }
                    .steps-table td { padding: 6px 8px; border: 1px solid #cbd5e1; vertical-align: top; word-break: break-word; overflow-wrap: break-word; }
                    
                    .notes-area { height: 45px; border: 1px dashed #94a3b8; border-radius: 4px; margin-top: 4px; padding: 6px; color: #64748b; font-size: 10px; word-break: break-word; }
                    
                    .footer { text-align: center; margin-top: 20px; font-size: 10px; color: #64748b; border-top: 1px solid #cbd5e1; padding-top: 6px; }
                </style>
            </head>
            <body>
                <div class="header-box">
                    <div class="header-side">
                        <strong>${YemeniCurriculum.COUNTRY_NAME_AR}</strong><br>
                        ${YemeniCurriculum.MINISTRY_NAME_AR}<br>
                        مكتب التربية بمحافظة: $gov<br>
                        المديرية: $dir<br>
                        المدرسة: $sch
                    </div>
                    <div class="header-center">
                        <h1>دفتر تحضير الدرس اليومي</h1>
                        <h2>(وفق المعايير التربوية المعتمدة في اليمن)</h2>
                        <p style="margin: 2px 0; color: #64748b; font-size: 10px;">منظومة رقيب - مساعد المعلم الذكي</p>
                    </div>
                    <div class="header-left">
                        <strong>التاريخ:</strong> ${java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date(lesson.createdAt))}<br>
                        <strong>رقم الحصة:</strong> ${lesson.period}<br>
                        <strong>عدد الحصص:</strong> ${if (lesson.lessonNumber.isNotBlank()) lesson.lessonNumber else "1"}<br>
                        <strong>الفصل الدراسي:</strong> ${lesson.term}
                    </div>
                </div>

                <!-- أ. البيانات الأساسية -->
                <table class="meta-table">
                    <tr>
                        <th style="width: 15%;">المدرسة:</th>
                        <td style="width: 35%;">${sch}</td>
                        <th style="width: 15%;">الصف الدراسي:</th>
                        <td style="width: 35%;">${lesson.grade}</td>
                    </tr>
                    <tr>
                        <th>المادة الدراسية:</th>
                        <td>${lesson.subject}</td>
                        <th>موضوع الدرس:</th>
                        <td style="font-weight: bold; color: #0D47A1;">${lesson.title}</td>
                    </tr>
                    <tr>
                        <th>زمن الحصة:</th>
                        <td>${lesson.durationMinutes} دقيقة</td>
                        <th>المعلم المنفذ:</th>
                        <td>${if (lesson.unitNumber.isNotBlank()) lesson.unitNumber else "معلم المادة"}</td>
                    </tr>
                </table>

                <!-- ب. الأهداف التعليمية -->
                <div class="section-card">
                    <div class="section-header">ب. الأهداف التعليمية (معرفية، مهارية، وجدانية وقيمية)</div>
                    <div class="section-body">${lesson.objectives}</div>
                </div>

                <!-- ج. الأساليب والطرق والوسائل المختارة فقط -->
                <div class="choices-grid">
                    <div class="choice-box">
                        <strong>أساليب المعلم:</strong>
                        ${if (lesson.teacherActivities.isNotBlank()) lesson.teacherActivities.take(60) else "الإلقاء والتوضيح، الحوار والمناقشة"}
                    </div>
                    <div class="choice-box">
                        <strong>طرق التدريس:</strong>
                        ${lesson.strategies}
                    </div>
                    <div class="choice-box">
                        <strong>المهارات والقيم:</strong>
                        ${if (lesson.previousKnowledge.isNotBlank()) lesson.previousKnowledge else "الملاحظة، التفكير النقدي، التعاون"}
                    </div>
                    <div class="choice-box">
                        <strong>الوسائل التعليمية:</strong>
                        ${lesson.teachingMaterials}
                    </div>
                </div>

                <!-- د. التمهيد (5 دقائق) -->
                <div class="section-card">
                    <div class="section-header">د. التهيئة والتمهيد (5 دقائق)</div>
                    <div class="section-body">${lesson.introduction}</div>
                </div>

                <!-- هـ. خطوات عرض الدرس في جدول تربوي رسمي -->
                <div style="margin-bottom: 6px; font-weight: bold; color: #0D47A1; font-size: 12px;">
                    هـ. خطوات عرض الدرس (إجراءات المعلم ونشاط المتعلم واستخدام الوسائل والطرائق):
                </div>
                <table class="steps-table">
                    <thead>
                        <tr>
                            <th>المرحلة والزمن</th>
                            <th>إجراءات المعلم</th>
                            <th>نشاط التلميذ</th>
                            <th>الوسيلة المستخدمة</th>
                            <th>الأسلوب / طريقة التدريس</th>
                        </tr>
                    </thead>
                    <tbody>
                        $stepsRowsHtml
                    </tbody>
                </table>

                <!-- و. التقويم -->
                <div class="section-card">
                    <div class="section-header">و. التقويم (قياس تحقق الأهداف المعرفية والمهارية والقيمية)</div>
                    <div class="section-body">${lesson.formativeAssessment}</div>
                </div>

                <!-- ز. الواجب المنزلي أو النشاط اللاصفي -->
                <div class="section-card">
                    <div class="section-header">ز. الواجب المنزلي أو النشاط اللاصفي</div>
                    <div class="section-body">${lesson.homework}</div>
                </div>

                <!-- ح. الملاحظات -->
                <div class="section-card">
                    <div class="section-header">ح. ملاحظات المعلم بعد تنفيذ الدرس</div>
                    <div class="section-body">
                        ${if (lesson.teacherNotes.isNotBlank()) lesson.teacherNotes else "مساحة مخصصة للمعلم لتدوين الانطباعات والصعوبات ومستوى تجاوب الطلاب بعد انتهاء الحصة..."}
                    </div>
                </div>

                <div class="footer">
                    تم إعداد هذا التحضير عبر تطبيق «رقيب مساعد المعلم الذكي» - المنظومة التربوية المتوافقة مع المنهج اليمني المعتمد | واتساب: +967785502919
                </div>
            </body>
            </html>
        """.trimIndent()

        doPrintHtml(context, "تحضير_${lesson.title}", htmlContent)
    }

    /**
     * Print or export an exam as printable Yemeni Ministry format PDF
     */
    fun printExam(context: Context, exam: ExamEntity) {
        val questions = ExamQuestion.parseListFromJson(exam.questionsJson)
        val questionsHtml = StringBuilder()

        questions.forEachIndexed { index, q ->
            questionsHtml.append("""
                <div class="q-card">
                    <div class="q-header">
                        <span class="q-num">س ${index + 1}:</span>
                        <span>${q.questionText}</span>
                        <span class="q-mark">(${q.marks} درجات)</span>
                    </div>
            """.trimIndent())

            // Render Educational Diagram if attached
            if (q.diagram != null && q.diagram.svgContent.isNotBlank()) {
                questionsHtml.append("""
                    <div style="text-align:center; margin: 10px 0;">
                        ${q.diagram.svgContent}
                        ${if (q.diagram.customNotes.isNotBlank()) "<div style='font-size:11px; color:#475569; margin-top:4px;'>📊 المعطيات: ${q.diagram.customNotes}</div>" else ""}
                    </div>
                """.trimIndent())
            }

            if (q.options.isNotEmpty()) {
                questionsHtml.append("<div class=\"options-grid\">")
                q.options.forEachIndexed { oIdx, opt ->
                    val letter = listOf("أ", "ب", "ج", "د").getOrElse(oIdx) { "$oIdx" }
                    questionsHtml.append("<div class=\"option-item\"><span class=\"opt-letter\">($letter)</span> $opt</div>")
                }
                questionsHtml.append("</div>")
            } else {
                questionsHtml.append("<div class=\"answer-space\">الإجابة: __________________________________________________________________</div>")
            }

            questionsHtml.append("</div>")
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 15px; color: #0f172a; line-height: 1.5; font-size: 13px; }
                    .header { text-align: center; border-bottom: 2px solid #0D47A1; padding-bottom: 8px; margin-bottom: 12px; }
                    .header h1 { color: #0D47A1; margin: 0; font-size: 18px; }
                    .header p { margin: 2px 0; color: #475569; font-size: 12px; }
                    .info-box { border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px 12px; margin-bottom: 16px; font-size: 12px; }
                    .info-row { display: flex; justify-content: space-between; margin-bottom: 4px; }
                    .student-info { margin-top: 6px; padding-top: 6px; border-top: 1px dashed #cbd5e1; }
                    .q-card { margin-bottom: 12px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; page-break-inside: avoid; overflow: hidden; word-break: break-word; overflow-wrap: break-word; }
                    .q-header { font-weight: bold; font-size: 13px; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; word-break: break-word; }
                    .q-num { color: #0D47A1; margin-left: 5px; }
                    .q-mark { float: left; font-size: 11px; color: #64748b; }
                    .options-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 8px; margin-top: 8px; font-size: 12px; }
                    .option-item { background: #f8fafc; padding: 6px 10px; border-radius: 4px; border: 1px solid #e2e8f0; overflow: hidden; text-overflow: ellipsis; word-break: break-word; overflow-wrap: break-word; }
                    .opt-letter { font-weight: bold; color: #0284c7; margin-left: 4px; }
                    .answer-space { margin-top: 8px; color: #94a3b8; font-size: 12px; word-break: break-word; }
                    .footer { text-align: center; margin-top: 25px; font-size: 11px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 6px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <p style="font-size: 12px; font-weight: bold;">${YemeniCurriculum.COUNTRY_NAME_AR} - ${YemeniCurriculum.MINISTRY_NAME_AR}</p>
                    <h1>اختبار مادة: ${exam.subject}</h1>
                    <p>عنوان الاختبار: ${exam.title} | الصف: ${exam.grade}</p>
                </div>
                <div class="info-box">
                    <div class="info-row">
                        <span><strong>زمن الإجابة:</strong> ${exam.durationMinutes} دقيقة</span>
                        <span><strong>الدرجة الكلية:</strong> ${exam.totalMarks} درجة</span>
                        <span><strong>رمز الاختبار:</strong> ${if (exam.examCode.isNotBlank()) exam.examCode else "YE-EXAM"}</span>
                    </div>
                    <div class="student-info">
                        اسم الطالب الرباعي: _____________________________ | الشعبة: _________ | رقم الجلوس: _________
                    </div>
                </div>

                $questionsHtml

                <div class="footer">
                    مع تمنياتنا لجميع أبنائنا وبناتنا الطلاب بالتوفيق والنجاح | تطبيق رقيب مساعد المعلم الذكي
                </div>
            </body>
            </html>
        """.trimIndent()

        doPrintHtml(context, "اختبار_${exam.title}", htmlContent)
    }

    /**
     * Print student test sheets for each variant (without answer key)
     */
    fun printStudentVariantExams(context: Context, variantSet: com.example.data.model.QuestionVariantSet) {
        val pagesHtml = StringBuilder()

        variantSet.variants.forEachIndexed { vIdx, q ->
            val isLast = vIdx == variantSet.variants.size - 1
            pagesHtml.append("""
                <div class="variant-page" style="${if (!isLast) "page-break-after: always; margin-bottom: 30px;" else ""}">
                    <div class="header">
                        <p style="font-size: 11px; font-weight: bold; margin: 0;">${YemeniCurriculum.COUNTRY_NAME_AR} - ${YemeniCurriculum.MINISTRY_NAME_AR}</p>
                        <h1 style="color: #0D47A1; margin: 4px 0; font-size: 17px;">اختبار مادة: ${variantSet.subject}</h1>
                        <p style="margin: 2px 0; color: #475569; font-size: 11px;">الصف: ${variantSet.grade} | الموضوع: ${variantSet.topic} | الصعوبة: ${variantSet.difficulty}</p>
                    </div>

                    <div class="info-box" style="border: 1.5px solid #0284c7; background: #f0f9ff; margin-bottom: 14px;">
                        <div class="info-row">
                            <span style="font-size: 13px; font-weight: bold; color: #0369a1;">🏷️ نموذج الاختبار: ${q.variantLabel.ifBlank { "النسخة ${vIdx + 1}" }}</span>
                            <span><strong>الدرجة:</strong> ${q.marks} درجات</span>
                            <span><strong>زمن الإجابة:</strong> 15 دقيقة</span>
                        </div>
                        <div class="student-info" style="margin-top: 6px; padding-top: 6px; border-top: 1px dashed #7dd3fc; font-size: 11px;">
                            اسم الطالب الرباعي: __________________________________ | الشعبة: _________ | رقم الجلوس: _________
                        </div>
                    </div>

                    <div class="q-card" style="border: 1px solid #cbd5e1; padding: 14px;">
                        <div class="q-header" style="font-size: 14px; line-height: 1.6;">
                            <span class="q-num" style="color: #0D47A1; font-weight: bold;">السؤال:</span>
                            <span>${q.questionText}</span>
                        </div>
            """.trimIndent())

            if (q.diagram != null && q.diagram.svgContent.isNotBlank()) {
                pagesHtml.append("""
                    <div style="text-align:center; margin: 12px 0;">
                        ${q.diagram.svgContent}
                        ${if (q.diagram.customNotes.isNotBlank()) "<div style='font-size:11px; color:#475569; margin-top:4px;'>📊 المعطيات: ${q.diagram.customNotes}</div>" else ""}
                    </div>
                """.trimIndent())
            }

            if (q.options.isNotEmpty()) {
                pagesHtml.append("<div class=\"options-grid\" style=\"margin-top: 12px;\">")
                q.options.forEachIndexed { oIdx, opt ->
                    val letter = listOf("أ", "ب", "ج", "د").getOrElse(oIdx) { "$oIdx" }
                    pagesHtml.append("""
                        <div class="option-item" style="padding: 8px 12px; font-size: 12px;">
                            <span class="opt-letter" style="color: #0284c7; font-weight: bold;">($letter)</span> $opt
                        </div>
                    """.trimIndent())
                }
                pagesHtml.append("</div>")
            } else {
                pagesHtml.append("""
                    <div class="answer-space" style="margin-top: 16px; border: 1px dashed #cbd5e1; border-radius: 6px; padding: 24px; color: #94a3b8; font-size: 11px;">
                        مساحة مخصصة لإجابة الطالب وخطوات الحل:
                    </div>
                """.trimIndent())
            }

            pagesHtml.append("""
                    </div>
                    <div class="footer" style="margin-top: 20px; font-size: 10px; color: #94a3b8; text-align: center;">
                        ${q.variantLabel} | منظومة رقيب مساعد المعلم الذكي - الجمهورية اليمنية
                    </div>
                </div>
            """.trimIndent())
        }

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 15px; color: #0f172a; line-height: 1.5; }
                    .header { text-align: center; border-bottom: 2px solid #0D47A1; padding-bottom: 8px; margin-bottom: 12px; }
                    .info-box { border: 1px solid #cbd5e1; border-radius: 6px; padding: 8px 12px; margin-bottom: 14px; }
                    .info-row { display: flex; justify-content: space-between; }
                    .q-card { border-radius: 6px; page-break-inside: avoid; }
                    .options-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 8px; }
                    .option-item { background: #f8fafc; border-radius: 4px; border: 1px solid #e2e8f0; }
                </style>
            </head>
            <body>
                $pagesHtml
            </body>
            </html>
        """.trimIndent()

        doPrintHtml(context, "نسخ_الطلاب_${variantSet.topic}", html)
    }

    /**
     * Print Teacher Answer Key and verification guide for all variants
     */
    fun printTeacherVariantKey(context: Context, variantSet: com.example.data.model.QuestionVariantSet) {
        val rowsHtml = StringBuilder()

        variantSet.variants.forEachIndexed { idx, v ->
            rowsHtml.append("""
                <div style="border: 1px solid #cbd5e1; border-radius: 6px; padding: 12px; margin-bottom: 12px; background: #ffffff; page-break-inside: avoid;">
                    <div style="display: flex; justify-content: space-between; border-bottom: 1px solid #e2e8f0; padding-bottom: 6px; margin-bottom: 6px;">
                        <span style="font-weight: bold; color: #0369a1; font-size: 13px;">${v.variantLabel.ifBlank { "النسخة ${idx + 1}" }}</span>
                        <span style="font-size: 11px; background: #dcfce7; color: #166534; padding: 2px 8px; border-radius: 12px; font-weight: bold;">
                            ${if (v.verificationStatus == "VERIFIED") "✓ إجابة محققة" else "⚠️ تحتاج مراجعة المعلم"}
                        </span>
                    </div>

                    <p style="margin: 4px 0; font-size: 12px; font-weight: 600;">نص السؤال: ${v.questionText}</p>
                    ${if (v.diagram != null && v.diagram.customNotes.isNotBlank()) "<p style='margin: 2px 0; font-size: 11px; color: #64748b;'>📊 معطيات الرسم: ${v.diagram.customNotes}</p>" else ""}

                    <div style="background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 4px; padding: 8px 10px; margin-top: 6px; font-size: 12px;">
                        <strong style="color: #166534;">الإجابة النموذجية:</strong> <span style="font-weight: bold; color: #14532d;">${v.correctAnswer}</span>
                        ${if (v.explanation.isNotBlank()) "<div style='color: #166534; font-size: 11px; margin-top: 3px;'>💡 الشرح وطريقة الحل: ${v.explanation}</div>" else ""}
                        ${if (v.verificationNote.isNotBlank()) "<div style='color: #0369a1; font-size: 10px; margin-top: 2px;'>🔍 التحقق: ${v.verificationNote}</div>" else ""}
                    </div>
                </div>
            """.trimIndent())
        }

        val html = """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 15px; color: #0f172a; line-height: 1.5; font-size: 12px; }
                    .header { text-align: center; border-bottom: 2px solid #0D47A1; padding-bottom: 8px; margin-bottom: 12px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <p style="font-size: 11px; font-weight: bold; margin: 0;">${YemeniCurriculum.COUNTRY_NAME_AR} - ${YemeniCurriculum.MINISTRY_NAME_AR}</p>
                    <h1 style="color: #0D47A1; margin: 4px 0; font-size: 17px;">مفتاح الإجابة ونموذج التصحيح (كافة النسخ)</h1>
                    <p style="margin: 2px 0; color: #475569; font-size: 11px;">المادة: ${variantSet.subject} | الصف: ${variantSet.grade} | الموضوع: ${variantSet.topic} | عدد النسخ: ${variantSet.variants.size}</p>
                </div>

                $rowsHtml

                <div style="text-align: center; margin-top: 20px; font-size: 10px; color: #94a3b8;">
                    سري ومخصص للمعلم فقط | تطبيق رقيب مساعد المعلم الذكي
                </div>
            </body>
            </html>
        """.trimIndent()

        doPrintHtml(context, "مفتاح_الإجابة_${variantSet.topic}", html)
    }

    private fun doPrintHtml(context: Context, jobName: String, htmlContent: String) {
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("id", "print", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        printManager.print(jobName, printAdapter, printAttributes)
                    } else {
                        Toast.makeText(context, "تعذر تهيئة مدير الطباعة", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ في تشغيل الطباعة: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareText(context: Context, title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة عبر"))
    }
}
