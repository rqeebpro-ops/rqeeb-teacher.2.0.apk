package com.example.ui.teacher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiService
import com.example.data.model.*
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

/**
 * Data model representing a ready-made exam template
 */
data class PrebuiltExamTemplate(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val defaultQuestionCount: Int,
    val defaultTotalScore: Int,
    val defaultDuration: Int,
    val questionTypes: List<String>,
    val sampleQuestions: List<ExamQuestion>
)

object PrebuiltExamTemplates {
    val templates = listOf(
        PrebuiltExamTemplate(
            id = "monthly_standard",
            title = "قالب الاختبار الشهري القياسي",
            category = "اختبارات شهرية",
            description = "4 أسئلة متنوعة (صح/خطأ، اختيار من متعدد، أكمل، مقالي قصير)",
            defaultQuestionCount = 4,
            defaultTotalScore = 20,
            defaultDuration = 45,
            questionTypes = listOf("صح وخطأ", "اختيار من متعدد", "أكمل الفراغ", "علل / بيّن السبب"),
            sampleQuestions = listOf(
                ExamQuestion(id = 1, type = QuestionType.TRUE_FALSE, text = "ضع علامة (✓) أمام العبارة الصحيحة وعلامة (✗) أمام العبارة الخطأ فيما يلي:", options = listOf("أ) العبارة الأولى صحيحة", "ب) العبارة الثانية خطأ"), correctAnswer = "✓", score = 5, explanation = "درجة واحدة لكل نقطة"),
                ExamQuestion(id = 2, type = QuestionType.MULTIPLE_CHOICE, text = "اختر الإجابة الصحيحة من بين الأقواس فيما يلي:", options = listOf("أ) الخيار الأول", "ب) الخيار الثاني", "ج) الخيار الثالث", "د) الخيار الرابع"), correctAnswer = "أ) الخيار الأول", score = 5, explanation = "درجة واحدة لكل فقرة"),
                ExamQuestion(id = 3, type = QuestionType.FILL_BLANKS, text = "أكمل الفراغات التالية بما يناسبها من كلمات مفتاحية:", options = emptyList(), correctAnswer = "الكلمات المناسبة بحسب المنهج", score = 5, explanation = "توزع الدرجات بالتساوي على الفراغات"),
                ExamQuestion(id = 4, type = QuestionType.EXPLAIN_WHY, text = "علل لما يأتي وبين السبب العلمي / الرياضي بوضوح:", options = emptyList(), correctAnswer = "التفسير العلمي المعتمد", score = 5, explanation = "درجتان للخطوة وثلاث درجات للنتيجة")
            )
        ),
        PrebuiltExamTemplate(
            id = "final_term",
            title = "قالب اختبار نهاية الفصل الدراسي",
            category = "اختبارات فصلية",
            description = "5 أسئلة شاملة بجدول درجات ختامي وسلم تصحيح معتمد",
            defaultQuestionCount = 5,
            defaultTotalScore = 50,
            defaultDuration = 90,
            questionTypes = listOf("صح وخطأ", "اختيار من متعدد", "عرف المصطلحات", "مسائل رياضية وفيزيائية", "علل / بيّن السبب"),
            sampleQuestions = listOf(
                ExamQuestion(id = 1, type = QuestionType.TRUE_FALSE, text = "السؤال الأول: ضع علامة (✓) أو (✗) أمام العبارات الآتية (10 درجات):", options = listOf("1) الفقرة الأولى", "2) الفقرة الثانية"), correctAnswer = "✓", score = 10, explanation = "درجتان لكل نقطة"),
                ExamQuestion(id = 2, type = QuestionType.MULTIPLE_CHOICE, text = "السؤال الثاني: اختر الإجابة الصحيحة مما بين القوسين (10 درجات):", options = listOf("أ) خيار 1", "ب) خيار 2", "ج) خيار 3"), correctAnswer = "أ) خيار 1", score = 10, explanation = "درجتان لكل نقطة"),
                ExamQuestion(id = 3, type = QuestionType.MATCHING, text = "السؤال الثالث: عرف المصطلحات والمفاهيم العلمية التالية (10 درجات):", options = emptyList(), correctAnswer = "التعريف الدقيق بحسب المقرر", score = 10, explanation = "شامل لمفاهيم المنهج"),
                ExamQuestion(id = 4, type = QuestionType.MATH_PHYSICS_PROBLEM, text = "السؤال الرابع: حل المسألة التالية موضحاً خطوات الحل (10 درجات):", options = emptyList(), correctAnswer = "القانون والخطوات والناتج النهائي", score = 10, explanation = "سلم الدرجات موزع على الخطوات"),
                ExamQuestion(id = 5, type = QuestionType.ESSAY, text = "السؤال الخامس: قارن وبين الفروق والأسباب في جدول منظم (10 درجات):", options = emptyList(), correctAnswer = "المقارنة التفصيلية", score = 10, explanation = "5 درجات لكل نقطة مقارنة")
            )
        ),
        PrebuiltExamTemplate(
            id = "math_science_visual",
            title = "قالب الرياضيات والعلوم (أشكال وجداول)",
            category = "تخصصي",
            description = "أسئلة مقارنة أعداد بأسهم، جداول آحاد وعشرات، ومربعات إجابة ورسم",
            defaultQuestionCount = 4,
            defaultTotalScore = 20,
            defaultDuration = 45,
            questionTypes = listOf("مقارنة أعداد بأسهم ودائرة", "جدول آحاد وعشرات راسي", "مسألة لفظية مع مربع ناتج", "توصيل نقاط وشكل هندسي"),
            sampleQuestions = listOf(
                ExamQuestion(id = 1, type = QuestionType.MULTIPLE_CHOICE, text = "ضع إشارة > أو < أو = في O المناسبة:", options = listOf("80 (  ) 50", "31 (  ) 36", "30 (  ) 45"), correctAnswer = ">", score = 5, explanation = "اكتب علامة المقارنة المناسبة داخل كل دائرة"),
                ExamQuestion(id = 2, type = QuestionType.MATH_PHYSICS_PROBLEM, text = "أوجد ناتج الجمع باستخدام جدول الآحاد والعشرات:", options = emptyList(), correctAnswer = "الجمع الرأسي وتجميع الآحاد والعشرات", score = 5, diagramType = "DATA_TABLE", explanation = "اجمع الأحاد مع الأحاد والعشرات مع العشرات واكتب الناتج في المربع المخصص"),
                ExamQuestion(id = 3, type = QuestionType.MATH_PHYSICS_PROBLEM, text = "حل المسألة اللفظية التالية واكتب الناتج في المربع المخصص:", options = emptyList(), correctAnswer = "30 - 20 = 10 سمكات", score = 5, explanation = "اكتب ناتج العملية الحسابية داخل المربع المخصص"),
                ExamQuestion(id = 4, type = QuestionType.MATCHING, text = "صل بين النقاط لتحصل على قطعة مستقيمة وشكل هندسي واضح:", options = emptyList(), correctAnswer = "التوصيل المستقيم بالمسطرة", score = 5, diagramType = "GEOMETRY_TRIANGLE", explanation = "استخدم المسطرة للتوصيل بين النقاط بخط مستقيم واضح")
            )
        ),
        PrebuiltExamTemplate(
            id = "arabic_islamic",
            title = "قالب اللغة العربية والتربية الإسلامية",
            category = "تخصصي",
            description = "نصوص قراءة، آيات وأحاديث، مفردات ومعاني، قواعد ونحو، تعبير وإملاء",
            defaultQuestionCount = 4,
            defaultTotalScore = 20,
            defaultDuration = 45,
            questionTypes = listOf("نص قرآني وقراءة", "معاني ومفردات", "قواعد ونحو وإعراب", "تعبير وإملاء"),
            sampleQuestions = listOf(
                ExamQuestion(id = 1, type = QuestionType.ESSAY, text = "أولاً: النص القرآني / الحديث الشريف والقراءة (5 درجات):", options = emptyList(), correctAnswer = "الشرح واستخلاص المعنى الإجمالي", score = 5, explanation = "درجتان للفكرة وثلاث درجات للشرح"),
                ExamQuestion(id = 2, type = QuestionType.MATCHING, text = "ثانياً: المعاني والمفردات واللغويات (5 درجات):", options = listOf("أ) الكلمة ومعناها", "ب) الكلمة ومضادها"), correctAnswer = "التوصيل والربط الصحيح", score = 5, explanation = "درجة لكل نقطة"),
                ExamQuestion(id = 3, type = QuestionType.ESSAY, text = "ثالثاً: القواعد والنحو والإعراب (5 درجات):", options = emptyList(), correctAnswer = "الإعراب التام المعتمد", score = 5, explanation = "درجة للوظيفة ودرجة للعلامة"),
                ExamQuestion(id = 4, type = QuestionType.ESSAY, text = "رابعاً: التعبير الكتابي والإملاء (5 درجات):", options = emptyList(), correctAnswer = "فقرة متناسقة بخط جميل وبدون أخطاء", score = 5, explanation = "تقييم الأفكار والإملاء والخط")
            )
        ),
        PrebuiltExamTemplate(
            id = "quick_quiz",
            title = "قالب الكويز والاختبار السريع",
            category = "اختبارات سريعة",
            description = "سؤالان سريعان لقياس مدى فهم الدرس الحالي في 15 دقيقة",
            defaultQuestionCount = 2,
            defaultTotalScore = 10,
            defaultDuration = 15,
            questionTypes = listOf("اختيار من متعدد", "صح وخطأ"),
            sampleQuestions = listOf(
                ExamQuestion(id = 1, type = QuestionType.MULTIPLE_CHOICE, text = "اختر الإجابة الصحيحة (5 درجات):", options = listOf("أ) خيار 1", "ب) خيار 2", "ج) خيار 3"), correctAnswer = "أ) خيار 1", score = 5, explanation = "تقييم تكويني سريع"),
                ExamQuestion(id = 2, type = QuestionType.TRUE_FALSE, text = "ضع علامة صح أو خطأ (5 درجات):", options = listOf("1) عبارة 1", "2) عبارة 2"), correctAnswer = "✓", score = 5, explanation = "تقييم استيعاب الحصة")
            )
        ),
        PrebuiltExamTemplate(
            id = "english_subject",
            title = "قالب اللغة الإنجليزية (English Exam)",
            category = "تخصصي",
            description = "Reading Passage, Vocabulary, Grammar, and Guided Writing",
            defaultQuestionCount = 4,
            defaultTotalScore = 20,
            defaultDuration = 45,
            questionTypes = listOf("Reading Passage", "Vocabulary", "Grammar", "Writing"),
            sampleQuestions = listOf(
                ExamQuestion(id = 1, type = QuestionType.ESSAY, text = "Question 1: Read the passage and answer the questions below (5 Marks):", options = emptyList(), correctAnswer = "Answers according to the passage", score = 5, explanation = "Read carefully and answer in complete sentences"),
                ExamQuestion(id = 2, type = QuestionType.MATCHING, text = "Question 2: Match the words in Column A with their meanings in Column B (5 Marks):", options = listOf("1) Word A", "2) Word B", "3) Word C"), correctAnswer = "Correct matching", score = 5, explanation = "1 mark for each pair"),
                ExamQuestion(id = 3, type = QuestionType.MULTIPLE_CHOICE, text = "Question 3: Choose the correct grammatical form between brackets (5 Marks):", options = listOf("a) option A", "b) option B", "c) option C"), correctAnswer = "a) option A", score = 5, explanation = "Focus on target grammar"),
                ExamQuestion(id = 4, type = QuestionType.ESSAY, text = "Question 4: Write a short paragraph of 5 lines about the topic (5 Marks):", options = emptyList(), correctAnswer = "Coherent paragraph with correct punctuation", score = 5, explanation = "Spelling, punctuation, and ideas")
            )
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamGeneratorScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Cascading Grade and Subject
    var grade by remember {
        mutableStateOf(
            if (teacher.gradeTaught.isNotBlank()) YemeniCurriculum.toDisplayGrade(teacher.gradeTaught) else "9/ إعدادي"
        )
    }

    val availableSubjects = remember(grade) { YemeniCurriculum.getSubjectsForGrade(grade) }
    var subject by remember(grade) {
        mutableStateOf(
            if (teacher.subject.isNotBlank() && availableSubjects.contains(teacher.subject)) {
                teacher.subject
            } else {
                availableSubjects.firstOrNull() ?: "الرياضيات"
            }
        )
    }

    var term by remember { mutableStateOf(YemeniCurriculum.TERMS.first()) }

    // Units for chosen Grade, Subject, Term
    val curriculumUnits = remember(grade, subject, term) {
        YemeniCurriculum.getCurriculumUnits(grade, subject, term)
    }

    var selectedUnitIndex by remember(grade, subject, term) { mutableIntStateOf(0) }
    var unitTopic by remember(selectedUnitIndex, curriculumUnits) {
        mutableStateOf(curriculumUnits.getOrNull(selectedUnitIndex)?.title ?: "الوحدة الأولى")
    }

    var examTitle by remember { mutableStateOf("") }
    var examType by remember { mutableStateOf(YemeniCurriculum.EXAM_TYPES.first()) }
    var difficulty by remember { mutableStateOf(YemeniCurriculum.EXAM_DIFFICULTY_LEVELS.first()) }
    var questionCount by remember { mutableIntStateOf(6) }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var totalScore by remember { mutableIntStateOf(20) }

    // Toggle for including diagrams
    var includeDiagrams by remember { mutableStateOf(true) }

    // Selected Question Types (from 14 types)
    val selectedQuestionTypes = remember {
        mutableStateListOf(
            "اختيار من متعدد",
            "صح وخطأ",
            "أكمل الفراغ",
            "عرف المصطلحات",
            "علل / بيّن السبب",
            "مسائل رياضية وفيزيائية",
            "اكتب البيانات على الرسم"
        )
    }

    // Generated Questions List
    val questionsList = remember { mutableStateListOf<ExamQuestion>() }

    // View Mode: Student Paper vs Teacher Model Answer Key
    var isAnswerKeyView by remember { mutableStateOf(false) }

    // View Style: Official Paper Layout (A4 format matching reference image) vs Interactive Cards
    var previewStyle by remember { mutableStateOf("OFFICIAL_PAPER") }
    var useOfficialMathPaperTemplate by remember { mutableStateOf(true) }

    // Editing Dialog State
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var isAddingManualQuestion by remember { mutableStateOf(false) }

    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showPaperExamView by remember { mutableStateOf(false) }
    var selectedTemplateForSetup by remember { mutableStateOf<PrebuiltExamTemplate?>(null) }
    var isGeneratingTemplateAI by remember { mutableStateOf(false) }
    var activePaperTemplateId by remember { mutableStateOf<String?>(null) }
    var showTemplateLibraryDialog by remember { mutableStateOf(false) }
    val customTemplates by repository.getCustomExamTemplatesFlow(teacher.id).collectAsState(initial = emptyList())

    if (showPaperExamView && questionsList.isNotEmpty()) {
        YemeniPaperExamView(
            exam = ExamEntity(
                id = 0,
                teacherId = teacher.id,
                title = examTitle.ifBlank { "اختبار في مقرر $subject" },
                subject = subject,
                grade = grade,
                examType = examType,
                durationMinutes = durationMinutes,
                totalScore = totalScore,
                schoolName = teacher.schoolName.ifBlank { "مدرسة المختار للتعليم الأساسي" },
                term = term,
                academicYear = "1446-1447هـ",
                difficulty = difficulty,
                unitName = unitTopic,
                hasAnswerKey = true,
                hasDiagrams = includeDiagrams
            ),
            questions = questionsList.toList(),
            teacher = teacher,
            repository = repository,
            geminiService = geminiService,
            initialTemplateId = activePaperTemplateId,
            onBack = { showPaperExamView = false },
            onExamUpdated = { updatedExam, updatedQuestions ->
                questionsList.clear()
                questionsList.addAll(updatedQuestions)
                coroutineScope.launch {
                    repository.insertExam(updatedExam)
                }
            }
        )
        return
    }

    // Dropdown toggles
    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var termDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var examTypeDropdownExpanded by remember { mutableStateOf(false) }
    var diffDropdownExpanded by remember { mutableStateOf(false) }

    // Calculate current questions total score
    val currentQuestionsScore = remember(questionsList.toList()) {
        questionsList.sumOf { it.score }
    }
    val isScoreMatching = (currentQuestionsScore == totalScore)

    fun autoBalanceScores() {
        if (questionsList.isEmpty()) return
        val count = questionsList.size
        val baseScore = (totalScore / count).coerceAtLeast(1)
        val remainder = totalScore % count

        val updated = questionsList.mapIndexed { index, q ->
            val balanced = baseScore + (if (index < remainder) 1 else 0)
            q.copy(score = balanced)
        }
        questionsList.clear()
        questionsList.addAll(updated)
        Toast.makeText(context, "تم توزيع الدرجات تلقائياً لتطابق الدرجة الكلية ($totalScore)!", Toast.LENGTH_SHORT).show()
    }

    fun buildCurrentExamEntity(): ExamEntity {
        return ExamEntity(
            id = 0,
            teacherId = teacher.id,
            title = examTitle.ifBlank { "اختبار في مقرر $subject" },
            subject = subject,
            grade = grade,
            examType = examType,
            durationMinutes = durationMinutes,
            totalScore = totalScore,
            schoolName = teacher.schoolName.ifBlank { "مدرسة المختار للتعليم الأساسي" },
            term = term,
            academicYear = "2022-2023م",
            difficulty = difficulty,
            unitName = unitTopic,
            hasAnswerKey = true,
            hasDiagrams = includeDiagrams
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("صانع الاختبارات النموذجية", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("exam_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (questionsList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val entity = buildCurrentExamEntity()
                                PrintUtils.printExam(
                                    context,
                                    entity,
                                    questionsList,
                                    isAnswerKey = isAnswerKeyView,
                                    useOfficialMathTemplate = (useOfficialMathPaperTemplate || subject.contains("الرياضيات"))
                                )
                            },
                            modifier = Modifier.testTag("print_exam_pdf_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "طباعة PDF")
                        }
                        IconButton(
                            onClick = {
                                val entity = buildCurrentExamEntity()
                                PrintUtils.exportExamToWord(
                                    context,
                                    entity,
                                    questionsList,
                                    isAnswerKey = isAnswerKeyView,
                                    useOfficialMathTemplate = (useOfficialMathPaperTemplate || subject.contains("الرياضيات"))
                                )
                            },
                            modifier = Modifier.testTag("export_exam_word_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "تصدير Word")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Ready-Made Templates Library Section (القوالب الجاهزة والنماذج السريعة)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("ready_made_templates_section")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("مكتبة القوالب الجاهزة والنماذج السريعة", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("اختر هيكل وتنسيق الاختبار ليُطبَّق تلقائياً على المحرر", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            OutlinedButton(
                                onClick = { showTemplateLibraryDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("open_template_library_from_generator_btn")
                            ) {
                                Text("عرض الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(PrebuiltExamTemplates.templates) { template ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable { selectedTemplateForSetup = template }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                template.category,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            template.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )

                                        Text(
                                            template.description,
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            lineHeight = 14.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${template.defaultTotalScore} درجة • ${template.defaultQuestionCount} أسئلة",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )

                                            Button(
                                                onClick = { selectedTemplateForSetup = template },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("استخدام القالب", fontSize = 10.5.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Exam Specifications Form
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("بيانات ومواصفات الاختبار (المنهج اليمني)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = examTitle,
                            onValueChange = { examTitle = it },
                            label = { Text("عنوان أو موضوع الاختبار *") },
                            placeholder = { Text("مثال: اختبار الشهر الأول في مادة $subject") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("exam_title_input")
                        )

                        // Cascading Grade and Subject
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Grade
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = grade,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الصف الدراسي") },
                                    trailingIcon = {
                                        IconButton(onClick = { gradeDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { gradeDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = gradeDropdownExpanded,
                                    onDismissRequest = { gradeDropdownExpanded = false }
                                ) {
                                    YemeniCurriculum.DISPLAY_GRADES.forEach { g ->
                                        DropdownMenuItem(
                                            text = { Text(g) },
                                            onClick = {
                                                grade = g
                                                gradeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Subject
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("المادة") },
                                    trailingIcon = {
                                        IconButton(onClick = { subjectDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { subjectDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = subjectDropdownExpanded,
                                    onDismissRequest = { subjectDropdownExpanded = false }
                                ) {
                                    availableSubjects.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s) },
                                            onClick = {
                                                subject = s
                                                subjectDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Term and Unit Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Term
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = term,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الفصل الدراسي") },
                                    trailingIcon = {
                                        IconButton(onClick = { termDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { termDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = termDropdownExpanded,
                                    onDismissRequest = { termDropdownExpanded = false }
                                ) {
                                    YemeniCurriculum.TERMS.forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t) },
                                            onClick = {
                                                term = t
                                                termDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Unit
                            Box(modifier = Modifier.weight(1.2f)) {
                                OutlinedTextField(
                                    value = unitTopic,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الوحدة / المقرر") },
                                    trailingIcon = {
                                        IconButton(onClick = { unitDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { unitDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = unitDropdownExpanded,
                                    onDismissRequest = { unitDropdownExpanded = false }
                                ) {
                                    curriculumUnits.forEachIndexed { idx, u ->
                                        DropdownMenuItem(
                                            text = { Text(u.title) },
                                            onClick = {
                                                selectedUnitIndex = idx
                                                unitTopic = u.title
                                                unitDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Exam Type and Difficulty Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Exam Type
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = examType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("نوع الاختبار") },
                                    trailingIcon = {
                                        IconButton(onClick = { examTypeDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { examTypeDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = examTypeDropdownExpanded,
                                    onDismissRequest = { examTypeDropdownExpanded = false }
                                ) {
                                    YemeniCurriculum.EXAM_TYPES.forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t) },
                                            onClick = {
                                                examType = t
                                                examTypeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Difficulty
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = difficulty,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("مستوى الصعوبة") },
                                    trailingIcon = {
                                        IconButton(onClick = { diffDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { diffDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = diffDropdownExpanded,
                                    onDismissRequest = { diffDropdownExpanded = false }
                                ) {
                                    YemeniCurriculum.EXAM_DIFFICULTY_LEVELS.forEach { d ->
                                        DropdownMenuItem(
                                            text = { Text(d) },
                                            onClick = {
                                                difficulty = d
                                                diffDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Question Count, Total Score, Duration Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = questionCount.toString(),
                                onValueChange = { questionCount = it.toIntOrNull() ?: 5 },
                                label = { Text("عدد الأسئلة") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = totalScore.toString(),
                                onValueChange = { totalScore = it.toIntOrNull() ?: 20 },
                                label = { Text("الدرجة الكلية *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("total_score_input")
                            )
                            OutlinedTextField(
                                value = durationMinutes.toString(),
                                onValueChange = { durationMinutes = it.toIntOrNull() ?: 45 },
                                label = { Text("الزمن (دقيقة)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Diagram Toggle Switch
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("إدراج الرسوم والأشكال التوضيحية عند الحاجة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("أشكال هندسية، دوائر كهربائية، تركيب الخلية، متجهات القوة، ذرات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = includeDiagrams,
                                    onCheckedChange = { includeDiagrams = it },
                                    modifier = Modifier.testTag("include_diagrams_switch")
                                )
                            }
                        }

                        // Question Types Selection Section
                        Text("أنواع الأسئلة المطلوبة في الاختبار:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(YemeniCurriculum.EXAM_QUESTION_TYPES) { qType ->
                                val isSelected = selectedQuestionTypes.contains(qType)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            if (selectedQuestionTypes.size > 1) selectedQuestionTypes.remove(qType)
                                        } else {
                                            selectedQuestionTypes.add(qType)
                                        }
                                    },
                                    label = { Text(qType, fontSize = 12.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Official Yemeni Math Exam Template Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF16A34A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFF16A34A))
                                    Text(
                                        "قالب ورقة الرياضيات الرسمية (نموذج وزارة التربية والتعليم)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                                Text(
                                    "توليد فوري لورقة اختبار رياضيات A4 بإطار زخرفي، ورأس ثلاثي، وجدول أسئلة من 3 أعمدة (السؤال، المحتوى، الدرجة)، ورسوم هندسية حقيقية.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            grade = "2/ أساسي"
                                            subject = "الرياضيات"
                                            term = "الفصل الدراسي الأول"
                                            examTitle = "اختبار نهاية الفصل الدراسي الأول لمادة الرياضيات للعام 2022-2023م"
                                            totalScore = 21
                                            questionCount = 5
                                            useOfficialMathPaperTemplate = true
                                            previewStyle = "OFFICIAL_PAPER"
                                            val mathQs = geminiService.generateOfficialYemeniMathExam("2/ أساسي", randomize = false)
                                            questionsList.clear()
                                            questionsList.addAll(mathQs)
                                            Toast.makeText(context, "تم تحميل نموذج ورقة الرياضيات المرفقة (الصف الثاني) بنجاح!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        modifier = Modifier.weight(1f).testTag("load_reference_math_exam_button")
                                    ) {
                                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نموذج الورقة المرفقة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            subject = "الرياضيات"
                                            useOfficialMathPaperTemplate = true
                                            previewStyle = "OFFICIAL_PAPER"
                                            val mathQs = geminiService.generateOfficialYemeniMathExam(grade, randomize = true)
                                            questionsList.clear()
                                            questionsList.addAll(mathQs)
                                            totalScore = mathQs.sumOf { it.score }
                                            questionCount = mathQs.size
                                            Toast.makeText(context, "تم توليد نموذج رياضيات وزاري متجدد لصف $grade!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).testTag("generate_random_math_exam_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("توليد عشوائي لصف $grade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Generate Button
                        Button(
                            onClick = {
                                val topic = examTitle.ifBlank { unitTopic }
                                isGenerating = true
                                coroutineScope.launch {
                                    val qs = geminiService.generateRichExamQuestions(
                                        subject = subject,
                                        grade = grade,
                                        topic = topic,
                                        examType = examType,
                                        count = questionCount,
                                        totalScore = totalScore,
                                        difficulty = difficulty,
                                        selectedTypes = selectedQuestionTypes.toList(),
                                        includeDiagrams = includeDiagrams,
                                        context = context
                                    )
                                    questionsList.clear()
                                    questionsList.addAll(qs)
                                    isGenerating = false
                                    Toast.makeText(context, "تم توليد أسئلة الاختبار وتحويل الجداول والرسومات بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_exam_questions_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري صياغة الأسئلة والرسوم...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("توليد الاختبار والرسوم ذكياً (AI)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Results Section (if questions exist)
            if (questionsList.isNotEmpty()) {
                // Tab Bar: Student View vs Teacher Answer Key
                item {
                    TabRow(
                        selectedTabIndex = if (isAnswerKeyView) 1 else 0,
                        modifier = Modifier.fillMaxWidth().testTag("exam_view_mode_tabs")
                    ) {
                        Tab(
                            selected = !isAnswerKeyView,
                            onClick = { isAnswerKeyView = false },
                            text = { Text("ورقة أسئلة الطالب", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Assignment, contentDescription = null) }
                        )
                        Tab(
                            selected = isAnswerKeyView,
                            onClick = { isAnswerKeyView = true },
                            text = { Text("نموذج إجابة المعلم وسلم الدرجات", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                        )
                    }
                }

                // Prominent 1-Tap Launch Button for the Full Interactive A4 Editor (Toolbar, Tables, Photos, Graphics Library, Drag & Drop, Print)
                item {
                    Button(
                        onClick = { showPaperExamView = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006874)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("open_full_a4_editor_button")
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "فتح محرر الورقة المتكامل A4 (جداول • صور • رسومات • سحب وإفلات • طباعة)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // View Style Toggle: Official Paper Layout (like attached image) vs Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = (previewStyle == "OFFICIAL_PAPER"),
                            onClick = { previewStyle = "OFFICIAL_PAPER" },
                            label = { Text("ورقة الاختبار الرسمية A4 (مثل النموذج المرفق)", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f).testTag("preview_official_paper_chip")
                        )
                        FilterChip(
                            selected = (previewStyle == "CARDS"),
                            onClick = { previewStyle = "CARDS" },
                            label = { Text("عرض البطاقات للتعديل", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.ViewList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f).testTag("preview_cards_chip")
                        )
                    }
                }

                // Score Balance Alert Card
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isScoreMatching) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isScoreMatching) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isScoreMatching) "مجموع الدرجات مطابق تماماً: $currentQuestionsScore / $totalScore درجة ✓"
                                    else "تنبيه: مجموع درجات الأسئلة ($currentQuestionsScore) لا يطابق الدرجة الكلية ($totalScore)!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isScoreMatching) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                                if (!isScoreMatching) {
                                    Text("انقر على زر الموازنة لتوزيع الدرجات بالعدل", fontSize = 11.sp, color = Color(0xFF795548))
                                }
                            }

                            if (!isScoreMatching) {
                                Button(
                                    onClick = { autoBalanceScores() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("auto_balance_scores_button")
                                ) {
                                    Icon(Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("موازنة الدرجات", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                if (previewStyle == "OFFICIAL_PAPER") {
                    item {
                        val currentEntity = buildCurrentExamEntity()
                        OfficialMathPaperPreview(
                            exam = currentEntity,
                            questions = questionsList,
                            isAnswerKey = isAnswerKeyView,
                            onEditQuestion = { editingQuestionIndex = it }
                        )
                    }
                } else {
                    // Add Manual Question Button Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "أسئلة الاختبار (${questionsList.size} أسئلة):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedButton(
                                onClick = { isAddingManualQuestion = true },
                                modifier = Modifier.testTag("add_manual_question_button"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة سؤال يدوي", fontSize = 12.sp)
                            }
                        }
                    }

                    // Question Cards
                    itemsIndexed(questionsList) { index, question ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            // Header Row: Question number, type, score, actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            "س${index + 1}",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        question.type.toArabicName(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "${question.score} ${if (question.score == 1) "درجة" else "درجات"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Edit Question Button
                                    IconButton(
                                        onClick = { editingQuestionIndex = index },
                                        modifier = Modifier.size(32.dp).testTag("edit_question_${index}_button")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                                    }

                                    // Delete Question Button
                                    IconButton(
                                        onClick = {
                                            questionsList.removeAt(index)
                                            Toast.makeText(context, "تم حذف السؤال س${index + 1}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp).testTag("delete_question_${index}_button")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Question Text
                            Text(
                                question.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )

                            // Options (if multiple choice)
                            if (question.options.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 8.dp)) {
                                    question.options.forEach { opt ->
                                        val isCorrect = isAnswerKeyView && opt.trim() == question.correctAnswer.trim()
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCorrect) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = if (isCorrect) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF388E3C)) else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                opt,
                                                fontSize = 13.sp,
                                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCorrect) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Embedded Diagram (if question has diagram)
                            if (!question.diagramType.isNullOrBlank() && question.diagramType != "NONE") {
                                ExamDiagramView(
                                    diagramType = question.diagramType,
                                    isAnswerKey = isAnswerKeyView,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                if (!question.diagramInstruction.isNullOrBlank()) {
                                    Text(
                                        question.diagramInstruction,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Rich Attachments: Inserted Tables, Graphics Library Icons & Photos
                            ExamQuestionRichAttachmentsView(
                                question = question,
                                isEditMode = true,
                                isAnswerKeyView = isAnswerKeyView,
                                isPrintPreviewMode = false,
                                onUpdateQuestion = { updatedQ ->
                                    questionsList[index] = updatedQ
                                }
                            )

                            // Teacher Model Answer Key & Rubric (only visible in Teacher Tab)
                            if (isAnswerKeyView) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE8F5E9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "✓ الإجابة النموذجية:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Text(
                                            question.correctAnswer,
                                            fontSize = 13.sp,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (question.explanation.isNotBlank()) {
                                            Text(
                                                "سلم التصحيح والخطوات: ${question.explanation}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF388E3C)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // Export and Print Buttons Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Print Current View PDF
                            Button(
                                onClick = {
                                    val entity = buildCurrentExamEntity()
                                    PrintUtils.printExam(
                                        context,
                                        entity,
                                        questionsList,
                                        isAnswerKey = isAnswerKeyView,
                                        useOfficialMathTemplate = (useOfficialMathPaperTemplate || subject.contains("الرياضيات"))
                                    )
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("print_exam_btn_bottom")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isAnswerKeyView) "طباعة نموذج الإجابة PDF" else "طباعة ورقة الطالب PDF", fontSize = 12.sp)
                            }

                            // Export Current View Word (.doc)
                            OutlinedButton(
                                onClick = {
                                    val entity = buildCurrentExamEntity()
                                    PrintUtils.exportExamToWord(
                                        context,
                                        entity,
                                        questionsList,
                                        isAnswerKey = isAnswerKeyView,
                                        useOfficialMathTemplate = (useOfficialMathPaperTemplate || subject.contains("الرياضيات"))
                                    )
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("export_exam_word_btn_bottom")
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isAnswerKeyView) "تصدير الإجابة Word" else "تصدير الاختبار Word", fontSize = 12.sp)
                            }
                        }

                        // Save Exam to Room DB
                        Button(
                            onClick = {
                                isSaving = true
                                coroutineScope.launch {
                                    val entity = buildCurrentExamEntity()
                                    repository.insertExam(entity)
                                    isSaving = false
                                    Toast.makeText(context, "تم حفظ الاختبار في سجلات المعلم بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_exam_to_db_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ الاختبار في قاعدة بيانات التطبيق", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Question Edit Dialog
    editingQuestionIndex?.let { idx ->
        val q = questionsList.getOrNull(idx)
        if (q != null) {
            var editText by remember { mutableStateOf(q.text) }
            var editAnswer by remember { mutableStateOf(q.correctAnswer) }
            var editScore by remember { mutableIntStateOf(q.score) }
            var editExplanation by remember { mutableStateOf(q.explanation) }
            var editDiagramType by remember { mutableStateOf(q.diagramType ?: "NONE") }

            AlertDialog(
                onDismissRequest = { editingQuestionIndex = null },
                title = { Text("تعديل السؤال س${idx + 1}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            label = { Text("نص السؤال") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editAnswer,
                            onValueChange = { editAnswer = it },
                            label = { Text("الإجابة النموذجية") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editScore.toString(),
                            onValueChange = { editScore = it.toIntOrNull() ?: 1 },
                            label = { Text("درجة السؤال") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editExplanation,
                            onValueChange = { editExplanation = it },
                            label = { Text("سلم التصحيح والخطوات") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("الرسم التوضيحي المرفق بالسؤال:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        ExamDiagramTypes.ALL_DIAGRAM_TYPES.forEach { (dtype, dname) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editDiagramType = dtype }
                            ) {
                                RadioButton(selected = (editDiagramType == dtype), onClick = { editDiagramType = dtype })
                                Text(dname, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            questionsList[idx] = q.copy(
                                text = editText,
                                correctAnswer = editAnswer,
                                score = editScore,
                                explanation = editExplanation,
                                diagramType = editDiagramType
                            )
                            editingQuestionIndex = null
                            Toast.makeText(context, "تم تعديل السؤال بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("حفظ التعديل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingQuestionIndex = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    // Add Manual Question Dialog
    if (isAddingManualQuestion) {
        var newText by remember { mutableStateOf("") }
        var newAnswer by remember { mutableStateOf("") }
        var newScore by remember { mutableIntStateOf(2) }
        var newType by remember { mutableStateOf(QuestionType.ESSAY) }
        var newDiagramType by remember { mutableStateOf("NONE") }

        AlertDialog(
            onDismissRequest = { isAddingManualQuestion = false },
            title = { Text("إضافة سؤال جديد يدوياً", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        label = { Text("نص السؤال الجديد *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAnswer,
                        onValueChange = { newAnswer = it },
                        label = { Text("الإجابة النموذجية *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newScore.toString(),
                        onValueChange = { newScore = it.toIntOrNull() ?: 1 },
                        label = { Text("درجة السؤال") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("الرسم التوضيحي المرفق (اختياري):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    ExamDiagramTypes.ALL_DIAGRAM_TYPES.forEach { (dtype, dname) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { newDiagramType = dtype }
                        ) {
                            RadioButton(selected = (newDiagramType == dtype), onClick = { newDiagramType = dtype })
                            Text(dname, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newText.isNotBlank()) {
                            val newQ = ExamQuestion(
                                id = questionsList.size + 1,
                                type = newType,
                                text = newText.trim(),
                                correctAnswer = newAnswer.trim(),
                                score = newScore,
                                diagramType = newDiagramType
                            )
                            questionsList.add(ExamPlaceholderParser.parseQuestionPlaceholders(newQ))
                            isAddingManualQuestion = false
                            Toast.makeText(context, "تمت إضافة السؤال بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingManualQuestion = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Ready-Made Template Setup Modal Dialog (إعداد القالب الجاهز)
    if (selectedTemplateForSetup != null) {
        val template = selectedTemplateForSetup!!
        var setupSubject by remember(template) { mutableStateOf(subject) }
        var setupGrade by remember(template) { mutableStateOf(grade) }
        var setupTerm by remember(template) { mutableStateOf(term) }
        var setupTitle by remember(template) { mutableStateOf("اختبار ${template.category} - مادة $setupSubject") }
        var setupSchoolName by remember(template) { mutableStateOf(teacher.schoolName.ifBlank { "مدرسة المختار النموذجية" }) }
        var generationOption by remember(template) { mutableStateOf("AI") } // "AI" or "MANUAL"

        AlertDialog(
            onDismissRequest = { if (!isGeneratingTemplateAI) selectedTemplateForSetup = null },
            icon = { Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text("إعداد ${template.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(template.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "هيكل القالب: ${template.defaultTotalScore} درجة • ${template.defaultQuestionCount} أسئلة • ${template.defaultDuration} دقيقة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    OutlinedTextField(
                        value = setupSubject,
                        onValueChange = { setupSubject = it },
                        label = { Text("اسم المادة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = setupGrade,
                            onValueChange = { setupGrade = it },
                            label = { Text("الصف الدراسي") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = setupTerm,
                            onValueChange = { setupTerm = it },
                            label = { Text("الفصل الدراسي") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = setupTitle,
                        onValueChange = { setupTitle = it },
                        label = { Text("عنوان / موضوع الاختبار") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = setupSchoolName,
                        onValueChange = { setupSchoolName = it },
                        label = { Text("اسم المدرسة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("خيار تعبئة الأسئلة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { generationOption = "AI" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = generationOption == "AI",
                            onClick = { generationOption = "AI" }
                        )
                        Column {
                            Text("توليد محتوى الأسئلة فوراً بالذكاء الاصطناعي", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("توليد أسئلة منهجية دقيقة تطابق هيكل القالب والمنهج اليمني", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { generationOption = "MANUAL" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = generationOption == "MANUAL",
                            onClick = { generationOption = "MANUAL" }
                        )
                        Column {
                            Text("استخدام القالب المفرغ للتعديل اليدوي المباشر", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("تعبئة هيكل القالب بأسئلة نموذجية مفرغة لتعديلها يدوياً", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isGeneratingTemplateAI = true
                                subject = setupSubject
                                grade = setupGrade
                                term = setupTerm
                                examTitle = setupTitle
                                questionCount = template.defaultQuestionCount
                                totalScore = template.defaultTotalScore
                                durationMinutes = template.defaultDuration
                                examType = template.category

                                questionsList.clear()

                                if (generationOption == "AI") {
                                    val generated = geminiService.generateRichExamQuestions(
                                        subject = setupSubject,
                                        grade = setupGrade,
                                        topic = setupTitle.ifBlank { "مقرر المادة" },
                                        examType = template.category,
                                        count = template.defaultQuestionCount,
                                        totalScore = template.defaultTotalScore,
                                        difficulty = "متدرج",
                                        selectedTypes = template.questionTypes,
                                        includeDiagrams = true,
                                        context = context
                                    )
                                    questionsList.addAll(generated)
                                } else {
                                    questionsList.addAll(template.sampleQuestions)
                                }

                                activePaperTemplateId = when (template.id) {
                                    "quick_quiz" -> "quiz_fast"
                                    "final_term" -> "final_ministry"
                                    "math_science_visual" -> "lab_visual"
                                    else -> "monthly_official"
                                }
                                selectedTemplateForSetup = null
                                showPaperExamView = true
                                Toast.makeText(context, "تم تطبيق القالب وإنشاء الاختبار بنجاح!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                questionsList.addAll(template.sampleQuestions)
                                activePaperTemplateId = when (template.id) {
                                    "quick_quiz" -> "quiz_fast"
                                    "final_term" -> "final_ministry"
                                    "math_science_visual" -> "lab_visual"
                                    else -> "monthly_official"
                                }
                                selectedTemplateForSetup = null
                                showPaperExamView = true
                                Toast.makeText(context, "تم تطبيق القالب بهيكل العينة المعتمد!", Toast.LENGTH_SHORT).show()
                            } finally {
                                isGeneratingTemplateAI = false
                            }
                        }
                    },
                    enabled = !isGeneratingTemplateAI
                ) {
                    if (isGeneratingTemplateAI) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            Text("جاري تطبيق القالب...", fontSize = 12.sp)
                        }
                    } else {
                        Text("تطبيق القالب ورؤية الورقة الرسمية")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedTemplateForSetup = null },
                    enabled = !isGeneratingTemplateAI
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Full Template Library Modal accessible directly from ExamGeneratorScreen
    if (showTemplateLibraryDialog) {
        TemplateLibraryModalDialog(
            activeTemplateId = activePaperTemplateId ?: "monthly_official",
            customTemplates = customTemplates,
            onDismiss = { showTemplateLibraryDialog = false },
            onApplyFormatTemplate = { tpl, replaceQuestions ->
                activePaperTemplateId = tpl.id
                examType = tpl.examTypeTitle
                durationMinutes = tpl.durationMinutes
                totalScore = tpl.defaultTotalScore
                if (replaceQuestions || questionsList.isEmpty()) {
                    questionsList.clear()
                    questionsList.addAll(tpl.sampleQuestions)
                }
                showTemplateLibraryDialog = false
                showPaperExamView = true
                Toast.makeText(context, "تم فتح المحرر وتطبيق «${tpl.title}» تلقائياً!", Toast.LENGTH_SHORT).show()
            },
            onApplyCustomTemplate = { custom ->
                activePaperTemplateId = "custom_${custom.id}"
                if (questionsList.isEmpty()) {
                    questionsList.addAll(ExamTemplateLibrary.TEMPLATES.first { it.id == "monthly_official" }.sampleQuestions)
                }
                showTemplateLibraryDialog = false
                showPaperExamView = true
                Toast.makeText(context, "تم فتح المحرر وتطبيق القالب المخصص «${custom.templateName}»!", Toast.LENGTH_SHORT).show()
            },
            onDeleteCustomTemplate = { custom ->
                coroutineScope.launch {
                    repository.deleteCustomExamTemplate(custom)
                    Toast.makeText(context, "تم حذف القالب «${custom.templateName}»", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

/**
 * Preview of the official Yemeni Ministry Mathematics Examination Paper (A4)
 * Matching the exact visual layout of the reference paper:
 * - Ornamental double border
 * - 3-column header (School right, Title center, Student left)
 * - Banner: «أجب مستعيناً بالله عن جميع الأسئلة التالية بخط جميل وواضح في نفس الورقة»
 * - 3-column table: [السؤال] (right), [المحتوى والرسومات] (center), [الدرجة] (left)
 * - High-contrast black-and-white print appearance
 * - Page footer: «الصفحة 1 من 2»
 */
@Composable
fun OfficialMathPaperPreview(
    exam: ExamEntity,
    questions: List<ExamQuestion>,
    isAnswerKey: Boolean,
    onEditQuestion: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayGrade = YemeniCurriculum.toDisplayGrade(exam.grade)
    val schoolName = exam.schoolName.ifBlank { "مدرسة المختار للتعليم الأساسي" }
    val directorate = "منطقة معين التعليمية"
    val academicYear = exam.academicYear.ifBlank { "2022-2023م" }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1E293B)),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                // Outer ornamental-style double frame
                .border(2.5.dp, Color.Black)
                .padding(4.dp)
                .border(1.dp, Color(0xFF475569))
                .padding(8.dp)
        ) {
            // 3-Part Header Matching Reference Image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color.Black)
            ) {
                // Right Cell: Republic, Ministry, Directorate, School
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("الجمهورية اليمنية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("وزارة التربية والتعليم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(directorate, fontSize = 10.sp, color = Color(0xFF334155))
                    Text(schoolName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                // Vertical Divider
                Box(modifier = Modifier.width(1.5.dp).height(80.dp).background(Color.Black))

                // Center Cell: Exam Title & Year
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        exam.title.ifBlank { "اختبار نهاية الفصل الدراسي الأول" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Text("لمادة الرياضيات للعام $academicYear", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    if (isAnswerKey) {
                        Surface(color = Color(0xFFE0F2F1), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                "« نموذج الإجابة وسلم الدرجات »",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF006874),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Vertical Divider
                Box(modifier = Modifier.width(1.5.dp).height(80.dp).background(Color.Black))

                // Left Cell: Student Name, Grade, Subject, Date
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("الاسم: ....................", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("الصف: $displayGrade", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("المادة: رياضيات", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("التاريخ: 30 / ربيع ثاني / 1446هـ", fontSize = 9.sp, color = Color(0xFF475569))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-header Banner Matching Image
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color.Black),
                color = Color(0xFFFAFAFA)
            ) {
                Text(
                    "أجب مستعيناً بالله عن جميع الأسئلة التالية بخط جميل وواضح في نفس الورقة",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                )
            }

            // Questions Table with 3 columns: [السؤال | المحتوى | الدرجة]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color.Black)
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color.Black),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "السؤال",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(44.dp).padding(vertical = 4.dp)
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Black))
                    Text(
                        "نص السؤال والمسألة والرسومات الهندسية",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Black))
                    Text(
                        "الدرجة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(44.dp).padding(vertical = 4.dp)
                    )
                }

                // Table Rows
                questions.forEachIndexed { idx, q ->
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Right Column: Question Number
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .padding(top = 10.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                qNumArabic,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        // Divider
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))

                        // Center Column: Content & Diagrams
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    q.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (onEditQuestion != null) {
                                    IconButton(
                                        onClick = { onEditQuestion(idx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(14.dp), tint = Color(0xFF006874))
                                    }
                                }
                            }

                            // If options exist
                            if (q.options.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    q.options.forEach { opt ->
                                        val isCorrect = isAnswerKey && opt.trim() == q.correctAnswer.trim()
                                        Surface(
                                            color = if (isCorrect) Color(0xFFC8E6C9) else Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCorrect) Color(0xFF2E7D32) else Color(0xFFCBD5E1)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                opt,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCorrect) Color(0xFF1B5E20) else Color.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Embedded Diagram
                            if (!q.diagramType.isNullOrBlank() && q.diagramType != "NONE") {
                                ExamDiagramView(
                                    diagramType = q.diagramType,
                                    isAnswerKey = isAnswerKey,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            // Rich Attachments (Tables, Graphics, Photos) inside A4 Preview
                            ExamQuestionRichAttachmentsView(
                                question = q,
                                isEditMode = false,
                                isAnswerKeyView = isAnswerKey,
                                isPrintPreviewMode = true,
                                onUpdateQuestion = {}
                            )

                            // Answer Key Box
                            if (isAnswerKey) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text(
                                            "✓ الإجابة النموذجية: ${q.correctAnswer}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                        if (q.explanation.isNotBlank()) {
                                            Text(
                                                "سلم التصحيح: ${q.explanation}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Divider
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))

                        // Left Column: Score
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .padding(top = 10.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                scoreArabic,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Matching Reference Image
            Text(
                "الصفحة 1 من 2   •   « انتهت الأسئلة • مع تمنياتنا لكم بالتوفيق والنجاح »",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
