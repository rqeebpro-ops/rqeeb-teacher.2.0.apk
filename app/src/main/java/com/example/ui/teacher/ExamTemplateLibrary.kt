package com.example.ui.teacher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import java.util.UUID

/**
 * Paper Visual Frame & Header Layout Mode
 */
enum class PaperLayoutStyle(val arabicLabel: String) {
    COMPACT_QUIZ("ورقة اختبار قصير مدمجة"),
    OFFICIAL_MONTHLY("النموذج الرسمي القياسي A4"),
    MINISTRY_FINAL("الإطار الوزاري المزدوج النهائي"),
    WORKSHEET_MODERN("ورقة عمل ونشاط صفي")
}

/**
 * Full Format & Structure Template Definition for the Editor's Template Library
 */
data class ExamPaperFormatTemplate(
    val id: String,
    val title: String,
    val badgeText: String,
    val category: String,
    val description: String,
    val examTypeTitle: String,
    val durationText: String,
    val durationMinutes: Int,
    val defaultTotalScore: Int,
    val primaryColorHex: String,
    val showEmblem: Boolean,
    val logoSizeDp: Float,
    val showSummaryTable: Boolean,
    val paperLayoutStyle: PaperLayoutStyle,
    val questionFontSizeSp: Int,
    val boldQuestionHeader: Boolean,
    val answerLinesCount: Int,
    val instructionsText: String,
    val sampleQuestions: List<ExamQuestion>
)

object ExamTemplateLibrary {
    const val CAT_ALL = "الكل"
    const val CAT_QUIZ = "اختبار قصير"
    const val CAT_MONTHLY = "اختبار شهري"
    const val CAT_MIDTERM_FINAL = "نصفي ونهائي"
    const val CAT_WORKSHEET = "ورقة عمل وتطبيق"
    const val CAT_MY_SAVED = "قوالبي المحفوظة"

    val CATEGORIES = listOf(
        CAT_ALL,
        CAT_QUIZ,
        CAT_MONTHLY,
        CAT_MIDTERM_FINAL,
        CAT_WORKSHEET,
        CAT_MY_SAVED
    )

    val TEMPLATES: List<ExamPaperFormatTemplate> = listOf(
        ExamPaperFormatTemplate(
            id = "quiz_fast",
            title = "قالب الاختبار القصير (Quiz)",
            badgeText = "15 دقيقة • 10 درجات",
            category = CAT_QUIZ,
            description = "تنسيق مدمج وسريع برأس ثنائي مضغوط بدون جدول درجات مطول، مثالي للتقويم التكويني والحصص اليومية.",
            examTypeTitle = "اختبار قصير (Quiz)",
            durationText = "15 دقيقة",
            durationMinutes = 15,
            defaultTotalScore = 10,
            primaryColorHex = "#0369A1",
            showEmblem = true,
            logoSizeDp = 44f,
            showSummaryTable = false,
            paperLayoutStyle = PaperLayoutStyle.COMPACT_QUIZ,
            questionFontSizeSp = 13,
            boldQuestionHeader = true,
            answerLinesCount = 1,
            instructionsText = "أجب عن الفقرات السريعة التالية في نفس الورقة خلال 15 دقيقة:",
            sampleQuestions = listOf(
                ExamQuestion(
                    id = 1,
                    type = QuestionType.MULTIPLE_CHOICE,
                    text = "السؤال الأول: اختر الإجابة الصحيحة مما بين القوسين:",
                    options = listOf("أ) الخيار الأول", "ب) الخيار الثاني", "ج) الخيار الثالث", "د) الخيار الرابع"),
                    correctAnswer = "أ) الخيار الأول",
                    score = 5,
                    explanation = "درجة واحدة لكل فقرة"
                ),
                ExamQuestion(
                    id = 2,
                    type = QuestionType.TRUE_FALSE,
                    text = "السؤال الثاني: ضع علامة (✓) أمام العبارة الصحيحة وعلامة (✗) أمام العبارة الخطأ:",
                    options = emptyList(),
                    correctAnswer = "✓",
                    score = 5,
                    explanation = "تقييم استيعاب أهداف الدرس"
                )
            )
        ),
        ExamPaperFormatTemplate(
            id = "monthly_official",
            title = "قالب الاختبار الشهري الرسمي",
            badgeText = "45 دقيقة • 20 درجة",
            category = CAT_MONTHLY,
            description = "التنسيق المعتمد للاختبارات الشهرية: ترويسة ثلاثية كاملة مع الطير الجمهوري، حدود سوداء رسمية وجدول درجات ختامي.",
            examTypeTitle = "اختبار شهري",
            durationText = "45 دقيقة",
            durationMinutes = 45,
            defaultTotalScore = 20,
            primaryColorHex = "#006874",
            showEmblem = true,
            logoSizeDp = 64f,
            showSummaryTable = true,
            paperLayoutStyle = PaperLayoutStyle.OFFICIAL_MONTHLY,
            questionFontSizeSp = 13,
            boldQuestionHeader = true,
            answerLinesCount = 2,
            instructionsText = "أجب مستعيناً بالله عن جميع الأسئلة التالية بخط جميل وواضح في نفس الورقة:",
            sampleQuestions = listOf(
                ExamQuestion(
                    id = 1,
                    type = QuestionType.TRUE_FALSE,
                    text = "السؤال الأول: ضع علامة (✓) أو (✗) أمام العبارات الآتية:",
                    options = emptyList(),
                    correctAnswer = "✓",
                    score = 5,
                    explanation = "درجة لكل فقرة"
                ),
                ExamQuestion(
                    id = 2,
                    type = QuestionType.MULTIPLE_CHOICE,
                    text = "السؤال الثاني: اختر الإجابة الصحيحة مما يلي:",
                    options = listOf("أ) الإجابة الأولى", "ب) الإجابة الثانية", "ج) الإجابة الثالثة"),
                    correctAnswer = "أ) الإجابة الأولى",
                    score = 5,
                    explanation = "درجة لكل فقرة"
                ),
                ExamQuestion(
                    id = 3,
                    type = QuestionType.FILL_BLANKS,
                    text = "السؤال الثالث: أكمل الفراغات التالية بما يناسبها من المنهج:",
                    options = emptyList(),
                    correctAnswer = "المصطلح العلمي المعتمد",
                    score = 5,
                    explanation = "توزع الدرجة بالتساوي على الفراغات"
                ),
                ExamQuestion(
                    id = 4,
                    type = QuestionType.EXPLAIN_WHY,
                    text = "السؤال الرابع: علل لما يأتي / وضح خطوات الحل:",
                    options = emptyList(),
                    correctAnswer = "التعليل العلمي الصحيح",
                    score = 5,
                    explanation = "وضوح الفكرة وسلامة الخطوات"
                )
            )
        ),
        ExamPaperFormatTemplate(
            id = "midterm_exam",
            title = "قالب اختبار منتصف الفصل (النصفي)",
            badgeText = "60 دقيقة • 30 درجة",
            category = CAT_MIDTERM_FINAL,
            description = "تنسيق منتصف الفصل مع جدول مقارنة مدمج وتوزيع درجات متدرج يناسب تقييم نصف المقرر.",
            examTypeTitle = "اختبار منتصف الفصل الدراسي",
            durationText = "60 دقيقة",
            durationMinutes = 60,
            defaultTotalScore = 30,
            primaryColorHex = "#1E3A8A",
            showEmblem = true,
            logoSizeDp = 68f,
            showSummaryTable = true,
            paperLayoutStyle = PaperLayoutStyle.OFFICIAL_MONTHLY,
            questionFontSizeSp = 13,
            boldQuestionHeader = true,
            answerLinesCount = 2,
            instructionsText = "أجب مستعيناً بالله عن جميع الأسئلة التالية مع توضيح خطوات الحل:",
            sampleQuestions = listOf(
                ExamQuestion(
                    id = 1,
                    type = QuestionType.MULTIPLE_CHOICE,
                    text = "السؤال الأول: اختر الإجابة الصحيحة من بين الأقواس:",
                    options = listOf("أ) خيار 1", "ب) خيار 2", "ج) خيار 3", "د) خيار 4"),
                    correctAnswer = "أ) خيار 1",
                    score = 8,
                    explanation = "درجتان لكل فقرة"
                ),
                ExamQuestion(
                    id = 2,
                    type = QuestionType.DEFINITIONS,
                    text = "السؤال الثاني: عرف المصطلحات والمفاهيم التالية بدقة:",
                    options = emptyList(),
                    correctAnswer = "التعريف المعتمد في الكتاب المدرسي",
                    score = 6,
                    explanation = "3 درجات لكل تعريف"
                ),
                ExamQuestion(
                    id = 3,
                    type = QuestionType.COMPARE,
                    text = "السؤال الثالث: قارن في الجدول التالي بين العنصر الأول والعنصر الثاني:",
                    options = emptyList(),
                    correctAnswer = "المقارنة وفق أوجه الشبه والاختلاف",
                    score = 8,
                    explanation = "درجتان لكل صف مقارنة",
                    insertedTables = listOf(
                        ExamTableBlock(
                            id = UUID.randomUUID().toString(),
                            title = "جدول المقارنة",
                            rows = listOf(
                                listOf("وجه المقارنة", "العنصر الأول", "العنصر الثاني"),
                                listOf("التعريف / الخاصية", "", ""),
                                listOf("المثال / الأهمية", "", "")
                            )
                        )
                    )
                ),
                ExamQuestion(
                    id = 4,
                    type = QuestionType.MATH_PHYSICS_PROBLEM,
                    text = "السؤال الرابع: أجب عن المسألة التطبيقية التالية موضحاً الخطوات:",
                    options = emptyList(),
                    correctAnswer = "القانون + التعويض + الناتج النهائي",
                    score = 8,
                    explanation = "توزع على القانون والناتج",
                    answerLinesCount = 3
                )
            )
        ),
        ExamPaperFormatTemplate(
            id = "final_ministry",
            title = "قالب الاختبار النهائي الوزاري",
            badgeText = "90 دقيقة • 50 درجة",
            category = CAT_MIDTERM_FINAL,
            description = "النموذج الرسمي الكامل لاختبار نهاية الفصل الدراسي: إطار مزدوج فاخر، شعار الطير الجمهوري بحجم بارز، 5 أسئلة شاملة وجدول درجات ختامي.",
            examTypeTitle = "اختبار نهاية الفصل الدراسي",
            durationText = "90 دقيقة",
            durationMinutes = 90,
            defaultTotalScore = 50,
            primaryColorHex = "#0F172A",
            showEmblem = true,
            logoSizeDp = 76f,
            showSummaryTable = true,
            paperLayoutStyle = PaperLayoutStyle.MINISTRY_FINAL,
            questionFontSizeSp = 13,
            boldQuestionHeader = true,
            answerLinesCount = 3,
            instructionsText = "أجب مستعيناً بالله عن جميع الأسئلة الآتية، وتأكد من مراجعة إجاباتك وكتابة اسمك بوضوح:",
            sampleQuestions = listOf(
                ExamQuestion(
                    id = 1,
                    type = QuestionType.TRUE_FALSE,
                    text = "السؤال الأول: ضع علامة (✓) أمام العبارة الصحيحة وعلامة (✗) أمام العبارة الخطأ:",
                    options = emptyList(),
                    correctAnswer = "✓",
                    score = 10,
                    explanation = "درجتان لكل فقرة"
                ),
                ExamQuestion(
                    id = 2,
                    type = QuestionType.MULTIPLE_CHOICE,
                    text = "السؤال الثاني: اختر الإجابة الصحيحة مما بين القوسين:",
                    options = listOf("أ) الخيار الأول", "ب) الخيار الثاني", "ج) الخيار الثالث", "د) الخيار الرابع"),
                    correctAnswer = "أ) الخيار الأول",
                    score = 10,
                    explanation = "درجتان لكل فقرة"
                ),
                ExamQuestion(
                    id = 3,
                    type = QuestionType.DEFINITIONS,
                    text = "السؤال الثالث: اذكر المصطلح / عرف المفاهيم العلمية الآتية:",
                    options = emptyList(),
                    correctAnswer = "التعريف الدقيق",
                    score = 10,
                    explanation = "توزيع الدرجات على المفاهيم"
                ),
                ExamQuestion(
                    id = 4,
                    type = QuestionType.COMPARE,
                    text = "السؤال الرابع: أكمل جدول المقارنة التالي:",
                    options = emptyList(),
                    correctAnswer = "استكمال جميع خلايا المقارنة بدقة",
                    score = 10,
                    explanation = "تقييم شمولية المقارنة",
                    insertedTables = listOf(
                        ExamTableBlock(
                            id = UUID.randomUUID().toString(),
                            title = "جدول المقارنة الختامي",
                            rows = listOf(
                                listOf("المعيار", "المفهوم (أ)", "المفهوم (ب)"),
                                listOf("المفهوم", "", ""),
                                listOf("الوظيفة / الدور", "", ""),
                                listOf("مثال", "", "")
                            )
                        )
                    )
                ),
                ExamQuestion(
                    id = 5,
                    type = QuestionType.ESSAY,
                    text = "السؤال الخامس: حل المسألة / اشرح الفكرة الرئيسة مع التمثيل:",
                    options = emptyList(),
                    correctAnswer = "الحل الكامل مع التبرير",
                    score = 10,
                    explanation = "سلم تصحيح متدرج",
                    answerLinesCount = 3
                )
            )
        ),
        ExamPaperFormatTemplate(
            id = "worksheet_class",
            title = "قالب ورقة عمل ونشاط صفي",
            badgeText = "25 دقيقة • 10 درجات",
            category = CAT_WORKSHEET,
            description = "تصميم عصري مخصص لأوراق العمل الصفية والمنزلية بمساحة كتابة موسعة (3 أسطر) ورأس خفيف بدون شعار رسمي ثقيل.",
            examTypeTitle = "ورقة عمل تفاعلية",
            durationText = "25 دقيقة",
            durationMinutes = 25,
            defaultTotalScore = 10,
            primaryColorHex = "#047857",
            showEmblem = false,
            logoSizeDp = 42f,
            showSummaryTable = false,
            paperLayoutStyle = PaperLayoutStyle.WORKSHEET_MODERN,
            questionFontSizeSp = 14,
            boldQuestionHeader = true,
            answerLinesCount = 3,
            instructionsText = "نشاط صفي تفاعلي: اقرأ التوجيهات وأكمل الجدول أو الشكل المرفق بعناية:",
            sampleQuestions = listOf(
                ExamQuestion(
                    id = 1,
                    type = QuestionType.LABEL_DIAGRAM,
                    text = "النشاط الأول: تأمل الشكل المرفق ثم اكتب الملاحظات والاستنتاجات:",
                    options = emptyList(),
                    correctAnswer = "تحديد الأجزاء والوظيفة",
                    score = 4,
                    explanation = "تقييم الملاحظة والتحليل",
                    insertedGraphics = listOf(
                        ExamGraphicsLibrary.toGraphicBlock(
                            ExamGraphicsLibrary.ITEMS.first { it.id == "plant_sprout" }
                        )
                    ),
                    answerLinesCount = 3
                ),
                ExamQuestion(
                    id = 2,
                    type = QuestionType.TABLE_GRAPH_READING,
                    text = "النشاط الثاني: أكمل جدول الملاحظة التالي:",
                    options = emptyList(),
                    correctAnswer = "ملء البيانات بصورة صحيحة",
                    score = 6,
                    explanation = "3 درجات لكل صف",
                    insertedTables = listOf(
                        ExamTableBlock(
                            id = UUID.randomUUID().toString(),
                            title = "بطاقة الملاحظة والتطبيق",
                            rows = listOf(
                                listOf("الملاحظة", "التفسير", "النتيجة"),
                                listOf("", "", ""),
                                listOf("", "", "")
                            )
                        )
                    )
                )
            )
        ),
        ExamPaperFormatTemplate(
            id = "lab_visual",
            title = "قالب الاختبار العملي والرسوم العلمية",
            badgeText = "45 دقيقة • 20 درجة",
            category = CAT_WORKSHEET,
            description = "قالب مجهز بأسئلة تطبيقية تتضمن أشكالاً من مكتبة الرسومات وجدول بيانات علمي للعلوم والرياضيات.",
            examTypeTitle = "اختبار عملي وتطبيقي",
            durationText = "45 دقيقة",
            durationMinutes = 45,
            defaultTotalScore = 20,
            primaryColorHex = "#7C2D12",
            showEmblem = true,
            logoSizeDp = 56f,
            showSummaryTable = true,
            paperLayoutStyle = PaperLayoutStyle.OFFICIAL_MONTHLY,
            questionFontSizeSp = 13,
            boldQuestionHeader = true,
            answerLinesCount = 2,
            instructionsText = "تأمل الرسومات والأدوات والجداول المرفقة ثم أجب بدقة علمية:",
            sampleQuestions = listOf(
                ExamQuestion(
                    id = 1,
                    type = QuestionType.LABEL_DIAGRAM,
                    text = "السؤال الأول: سمّ الأداة أو الشكل الموضح واذكر استخدامه الأساسي:",
                    options = emptyList(),
                    correctAnswer = "المجهر الضوئي / فحص العينات الدقيقة",
                    score = 6,
                    explanation = "3 درجات للتسمية و3 درجات للاستخدام",
                    insertedGraphics = listOf(
                        ExamGraphicsLibrary.toGraphicBlock(
                            ExamGraphicsLibrary.ITEMS.first { it.id == "sci_microscope" }
                        )
                    )
                ),
                ExamQuestion(
                    id = 2,
                    type = QuestionType.MATH_PHYSICS_PROBLEM,
                    text = "السؤال الثاني: احسب القياس المطلوب للشكل الهندسي المرفق:",
                    options = emptyList(),
                    correctAnswer = "التطبيق الصحيح للقانون",
                    score = 7,
                    explanation = "3 درجات للقانون و4 للناتج",
                    insertedGraphics = listOf(
                        ExamGraphicsLibrary.toGraphicBlock(
                            ExamGraphicsLibrary.ITEMS.first { it.id == "geo_triangle" }
                        )
                    )
                ),
                ExamQuestion(
                    id = 3,
                    type = QuestionType.TABLE_GRAPH_READING,
                    text = "السؤال الثالث: سجّل نتائج التجربة في الجدول التالي:",
                    options = emptyList(),
                    correctAnswer = "القيم التجريبية الدقيقة",
                    score = 7,
                    explanation = "دقة القراءة والوحدة",
                    insertedTables = listOf(
                        ExamTableBlock(
                            id = UUID.randomUUID().toString(),
                            title = "جدول النتائج العملية",
                            rows = listOf(
                                listOf("المتغير", "القراءة الأولى", "القراءة الثانية"),
                                listOf("النتيجة", "", ""),
                                listOf("الاستنتاج", "", "")
                            )
                        )
                    )
                )
            )
        )
    )

    fun findById(id: String?): ExamPaperFormatTemplate? = TEMPLATES.firstOrNull { it.id == id }
}

/**
 * Interactive Template Library Modal allowing the teacher to browse exam formats,
 * apply formatting immediately onto the live editor, or load format + sample questions.
 */
@Composable
fun TemplateLibraryModalDialog(
    activeTemplateId: String,
    customTemplates: List<CustomExamTemplateEntity>,
    onDismiss: () -> Unit,
    onApplyFormatTemplate: (template: ExamPaperFormatTemplate, replaceQuestions: Boolean) -> Unit,
    onApplyCustomTemplate: (custom: CustomExamTemplateEntity) -> Unit,
    onDeleteCustomTemplate: (custom: CustomExamTemplateEntity) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ExamTemplateLibrary.CAT_ALL) }

    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == ExamTemplateLibrary.CAT_ALL) {
            ExamTemplateLibrary.TEMPLATES
        } else if (selectedCategory == ExamTemplateLibrary.CAT_MY_SAVED) {
            emptyList()
        } else {
            ExamTemplateLibrary.TEMPLATES.filter { it.category == selectedCategory }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DashboardCustomize,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    "مكتبة القوالب الجاهزة (Template Library)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "اختر تنسيق الاختبار ليُطبَّق تلقائياً على المحرر",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp).testTag("close_template_library_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                }

                // Category Chips Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExamTemplateLibrary.CATEGORIES.forEach { cat ->
                        val labelWithCount = if (cat == ExamTemplateLibrary.CAT_MY_SAVED && customTemplates.isNotEmpty()) {
                            "$cat (${customTemplates.size})"
                        } else {
                            cat
                        }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(labelWithCount, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                HorizontalDivider()

                // Templates List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedCategory != ExamTemplateLibrary.CAT_MY_SAVED) {
                        items(filteredTemplates, key = { it.id }) { tpl ->
                            val isActive = activeTemplateId == tpl.id
                            val accentColor = remember(tpl.primaryColorHex) {
                                try {
                                    Color(android.graphics.Color.parseColor(tpl.primaryColorHex))
                                } catch (_: Throwable) {
                                    Color(0xFF006874)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) accentColor.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isActive) 2.dp else 1.dp,
                                    color = if (isActive) accentColor else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("template_card_${tpl.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Miniature Wireframe Layout Preview Box
                                        TemplateMiniPaperWireframe(
                                            layoutStyle = tpl.paperLayoutStyle,
                                            accentColor = accentColor,
                                            showEmblem = tpl.showEmblem,
                                            showSummaryTable = tpl.showSummaryTable
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = accentColor.copy(alpha = 0.14f)
                                                ) {
                                                    Text(
                                                        tpl.badgeText,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = accentColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (isActive) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF16A34A)
                                                    ) {
                                                        Text(
                                                            "مُفعّل ✓",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                tpl.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Text(
                                                tpl.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 15.sp,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Action Buttons: Apply Formatting Only (keeps current questions) vs Apply Format + Template Questions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onApplyFormatTemplate(tpl, false) },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .height(42.dp)
                                                .testTag("apply_format_only_${tpl.id}")
                                        ) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("تطبيق التنسيق فقط", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { onApplyFormatTemplate(tpl, true) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, accentColor),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .testTag("apply_format_and_questions_${tpl.id}")
                                        ) {
                                            Icon(Icons.Default.LibraryAdd, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("التنسيق + الأسئلة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Teacher's Custom Saved Templates
                    if (selectedCategory == ExamTemplateLibrary.CAT_ALL || selectedCategory == ExamTemplateLibrary.CAT_MY_SAVED) {
                        if (customTemplates.isNotEmpty()) {
                            item {
                                Text(
                                    "قوالبي المحفوظة (${customTemplates.size}):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(customTemplates, key = { "custom_${it.id}" }) { custom ->
                                val cColor = remember(custom.primaryColorHex) {
                                    try {
                                        Color(android.graphics.Color.parseColor(custom.primaryColorHex))
                                    } catch (_: Throwable) {
                                        Color(0xFF006874)
                                    }
                                }
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, cColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(custom.templateName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                            Text(
                                                "${custom.schoolName.ifBlank { "المدرسة" }} • ${custom.directorateName}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Button(
                                                onClick = { onApplyCustomTemplate(custom) },
                                                colors = ButtonDefaults.buttonColors(containerColor = cColor),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("تطبيق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            IconButton(
                                                onClick = { onDeleteCustomTemplate(custom) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف القالب", tint = Color.Red, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (selectedCategory == ExamTemplateLibrary.CAT_MY_SAVED) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text("لا توجد قوالب مخصصة محفوظة بعد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "يمكنك تعديل الترويسة والألوان في المحرر ثم الضغط على «حفظ كقالب خاص بي» لإضافته هنا.",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Crisp visual wireframe thumbnail showing how the template formats the A4 paper
 */
@Composable
private fun TemplateMiniPaperWireframe(
    layoutStyle: PaperLayoutStyle,
    accentColor: Color,
    showEmblem: Boolean,
    showSummaryTable: Boolean
) {
    val outerBorderWidth = if (layoutStyle == PaperLayoutStyle.MINISTRY_FINAL) 2.dp else 1.dp
    Box(
        modifier = Modifier
            .width(58.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(if (layoutStyle == PaperLayoutStyle.WORKSHEET_MODERN) 8.dp else 4.dp))
            .background(Color.White)
            .border(outerBorderWidth, Color.Black, RoundedCornerShape(if (layoutStyle == PaperLayoutStyle.WORKSHEET_MODERN) 8.dp else 4.dp))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Header Wireframe
            when (layoutStyle) {
                PaperLayoutStyle.COMPACT_QUIZ -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                            .border(0.6.dp, accentColor, RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(accentColor, CircleShape))
                        Box(modifier = Modifier.width(24.dp).height(3.dp).background(Color.Black))
                    }
                }
                PaperLayoutStyle.WORKSHEET_MODERN -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(accentColor, RoundedCornerShape(4.dp))
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.6.dp, Color.Black))
                        if (showEmblem) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFEAB308), CircleShape))
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.6.dp, Color.Black))
                    }
                }
            }

            // Question Blocks Wireframe
            repeat(if (layoutStyle == PaperLayoutStyle.COMPACT_QUIZ) 2 else 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(0.6.dp, Color.Black, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .background(accentColor.copy(alpha = 0.75f))
                    )
                }
            }

            // Bottom Summary Table Wireframe
            if (showSummaryTable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFFE2E8F0))
                        .border(0.6.dp, Color.Black)
                )
            }
        }
    }
}
