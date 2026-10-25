package com.example.ui.teacher

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.data.ai.GeminiService
import com.example.data.model.*
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusPending
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

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

    // Top Navigation Tabs
    // 0: صانع الاختبار العام (General Exam Maker)
    // 1: توليد صيغ متعددة من السؤال (Multi-Version Question Variant Generator)
    // 2: مكتبة ومحرر الرسومات التعليمية (Educational Diagrams Library & Editor)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Tab 0 State: General Exam Maker
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(teacher.subject) }
    var grade by remember {
        mutableStateOf(
            if (teacher.gradeTaught.isNotBlank()) {
                YemeniCurriculum.toDisplayGrade(teacher.gradeTaught)
            } else if (teacher.stage.isNotBlank()) {
                YemeniCurriculum.toDisplayGrade(teacher.stage)
            } else {
                "9/ إعدادي"
            }
        )
    }
    var examType by remember { mutableStateOf(YemeniCurriculum.EXAM_TYPES[1]) }
    var difficulty by remember { mutableStateOf("متوسط") }
    var questionCount by remember { mutableIntStateOf(5) }
    var durationMinutes by remember { mutableIntStateOf(45) }
    val selectedTypes = remember {
        mutableStateListOf(QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE, QuestionType.FILL_BLANKS)
    }
    val questionsList = remember { mutableStateListOf<ExamQuestion>() }
    var isGeneratingExam by remember { mutableStateOf(false) }
    var isSavedExam by remember { mutableStateOf(false) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var questionBeingEdited by remember { mutableStateOf<Pair<Int, ExamQuestion>?>(null) }
    var activeDiagramToAttach by remember { mutableStateOf<DiagramInfo?>(null) }

    // Tab 1 State: Multi-Version Question Variant Generator
    var seedQuestionText by remember {
        mutableStateOf("إذا كان 3س + 5 = 20، فأوجد قيمة س موضحاً خطوات الحل.")
    }
    var seedSubject by remember { mutableStateOf(teacher.subject) }
    var seedGrade by remember {
        mutableStateOf(
            if (teacher.gradeTaught.isNotBlank()) {
                YemeniCurriculum.toDisplayGrade(teacher.gradeTaught)
            } else if (teacher.stage.isNotBlank()) {
                YemeniCurriculum.toDisplayGrade(teacher.stage)
            } else {
                "9/ إعدادي"
            }
        )
    }
    var seedTopic by remember { mutableStateOf("حل المعادلات الخطية من الدرجة الأولى") }
    var seedDifficulty by remember { mutableStateOf("متوسط") }
    var seedStrategy by remember { mutableStateOf(VariantStrategy.CHANGE_NUMBERS) }
    var requestedVariantCount by remember { mutableIntStateOf(10) }
    val generatedVariants = remember { mutableStateListOf<ExamQuestion>() }
    var isGeneratingVariants by remember { mutableStateOf(false) }
    var variantProgress by remember { mutableStateOf(Pair(0, 0)) }
    var isVariantSetSaved by remember { mutableStateOf(false) }
    var variantBeingEdited by remember { mutableStateOf<Pair<Int, ExamQuestion>?>(null) }

    // Tab 2 State: Educational Diagram Library
    var selectedDiagramCategory by remember { mutableStateOf("PHYSICS") }
    var activePreviewTemplate by remember { mutableStateOf(EducationalDiagramLibrary.ALL_TEMPLATES.first()) }
    var hideDiagramLabelsForQuiz by remember { mutableStateOf(false) }
    val customDiagramLabels = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("نظام الاختبارات الذكية", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            when (selectedTab) {
                                0 -> "صانع ومحرر الاختبارات المدرسية"
                                1 -> "توليد صيغ متعددة من السؤال الواحد (للطلاب)"
                                else -> "مكتبة ومحرر الرسومات والمخططات التعليمية"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("exam_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (selectedTab == 0 && questionsList.isNotEmpty()) {
                        IconButton(onClick = {
                            val entity = ExamEntity(
                                teacherId = teacher.id,
                                title = title.ifBlank { "اختبار فصلي" },
                                subject = subject,
                                grade = grade,
                                difficulty = difficulty,
                                durationMinutes = durationMinutes,
                                totalMarks = questionsList.sumOf { it.marks },
                                examCode = "RQ-${(100..999).random()}",
                                questionsJson = ExamQuestion.listToJsonString(questionsList)
                            )
                            PrintUtils.printExam(context, entity)
                        }) {
                            Icon(Icons.Default.Print, contentDescription = "طباعة الاختبار / PDF")
                        }
                    } else if (selectedTab == 1 && generatedVariants.isNotEmpty()) {
                        IconButton(onClick = {
                            val vSet = QuestionVariantSet(
                                originalQuestion = ExamQuestion(
                                    questionText = seedQuestionText,
                                    type = QuestionType.SHORT_ANSWER,
                                    correctAnswer = "حسب المعطيات",
                                    diagram = activeDiagramToAttach
                                ),
                                subject = seedSubject,
                                grade = seedGrade,
                                topic = seedTopic,
                                difficulty = seedDifficulty,
                                variationType = seedStrategy,
                                variants = generatedVariants.toList()
                            )
                            PrintUtils.printStudentVariantExams(context, vSet)
                        }) {
                            Icon(Icons.Default.Print, contentDescription = "طباعة نسخ الطلاب PDF")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null) },
                    label = { Text("صانع الاختبار", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.DynamicFeed, contentDescription = null) },
                    label = { Text("صيغ متعددة للسؤال", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Draw, contentDescription = null) },
                    label = { Text("الرسومات والمخططات", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddQuestionDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_add_question")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة سؤال يدوياً")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                // ==================== TAB 0: صانع الاختبار العام ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "مواصفات الاختبار والأسئلة (المنهج اليمني)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("موضوع أو عنوان الاختبار *") },
                                    placeholder = { Text("مثال: اختبار الوحدة الأولى / اختبار تجريبي في الكيمياء") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("exam_title_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = subject,
                                        onValueChange = { subject = it },
                                        label = { Text("المادة") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = grade,
                                        onValueChange = { grade = it },
                                        label = { Text("الصف") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Exam Type Filter
                                Text("نوع الاختبار (المنهج اليمني):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    YemeniCurriculum.EXAM_TYPES.forEach { t ->
                                        FilterChip(
                                            selected = examType == t,
                                            onClick = { examType = t },
                                            label = { Text(t, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                // Difficulty & Count
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الصعوبة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    listOf("سهل", "متوسط", "متقدم").forEach { diff ->
                                        FilterChip(
                                            selected = difficulty == diff,
                                            onClick = { difficulty = diff },
                                            label = { Text(diff, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("عدد الأسئلة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    listOf(3, 5, 10, 15).forEach { cnt ->
                                        FilterChip(
                                            selected = questionCount == cnt,
                                            onClick = { questionCount = cnt },
                                            label = { Text("$cnt أسئلة", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                // Question Types with horizontal scroll
                                Text("أنواع الأسئلة المطلوبة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    QuestionType.values().forEach { qType ->
                                        val isSelected = selectedTypes.contains(qType)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) selectedTypes.remove(qType) else selectedTypes.add(qType)
                                            },
                                            label = { Text(qType.titleAr, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (title.isBlank()) {
                                            Toast.makeText(context, "يرجى تحديد عنوان أو موضوع الاختبار", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isGeneratingExam = true
                                        isSavedExam = false
                                        coroutineScope.launch {
                                            val generated = geminiService.generateExam(
                                                subject = subject,
                                                grade = grade,
                                                topic = title,
                                                difficulty = difficulty,
                                                questionCount = questionCount,
                                                selectedTypes = selectedTypes.toList(),
                                                examType = examType
                                            )
                                            questionsList.clear()
                                            questionsList.addAll(generated)
                                            isGeneratingExam = false
                                        }
                                    },
                                    enabled = !isGeneratingExam,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("generate_exam_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isGeneratingExam) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جارٍ صياغة الأسئلة بالذكاء الاصطناعي...", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("توليد الأسئلة بالذكاء الاصطناعي", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (questionsList.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "بنك أسئلة الاختبار (${questionsList.size} سؤال - ${questionsList.sumOf { it.marks }} درجة)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val entity = ExamEntity(
                                                teacherId = teacher.id,
                                                title = title.ifBlank { "اختبار $subject" },
                                                subject = subject,
                                                grade = grade,
                                                difficulty = difficulty,
                                                durationMinutes = durationMinutes,
                                                totalMarks = questionsList.sumOf { it.marks },
                                                examCode = "RQ-${(100..999).random()}",
                                                questionsJson = ExamQuestion.listToJsonString(questionsList),
                                                isPublishedToStudents = true
                                            )
                                            repository.saveExam(entity)
                                            isSavedExam = true
                                            Toast.makeText(context, "تم حفظ الاختبار ونشره في بنك الاختبارات بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isSavedExam,
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isSavedExam) Color(0xFF10B981) else MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("save_exam_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSavedExam) "تم الحفظ والنشر" else "حفظ ونشر", fontSize = 12.sp)
                                }
                            }
                        }

                        itemsIndexed(questionsList) { index, q ->
                            ExamQuestionItemCard(
                                index = index,
                                question = q,
                                onDelete = { questionsList.removeAt(index) },
                                onEdit = { questionBeingEdited = Pair(index, q) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // ==================== TAB 1: توليد صيغ متعددة من السؤال لعدد كبير من الطلاب ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "الميزة الأولى: توليد صيغ متعددة من السؤال الواحد",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "توزيع نماذج متكافئة للطلاب لمنع الغش وضمان العدالة التعليمية",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            "المنهج اليمني",
                                            color = Color(0xFF166534),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                OutlinedTextField(
                                    value = seedQuestionText,
                                    onValueChange = { seedQuestionText = it },
                                    label = { Text("السؤال الأصلي المراد توليد صيغ متكافئة منه *") },
                                    placeholder = { Text("مثال: إذا كان 3س + 5 = 20، فأوجد قيمة س.") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth().testTag("seed_question_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = seedSubject,
                                        onValueChange = { seedSubject = it },
                                        label = { Text("المادة") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = seedGrade,
                                        onValueChange = { seedGrade = it },
                                        label = { Text("الصف") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = seedTopic,
                                    onValueChange = { seedTopic = it },
                                    label = { Text("الموضوع / الدرس التعليمي") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Number of Variants Selector (5, 10, 20, 30, 50, 100)
                                Text("عدد النسخ المطلوب توليدها للطلاب:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(5, 10, 20, 30, 50, 100).forEach { count ->
                                        FilterChip(
                                            selected = requestedVariantCount == count,
                                            onClick = { requestedVariantCount = count },
                                            label = { Text("$count نسخة", fontSize = 11.sp, fontWeight = if (requestedVariantCount == count) FontWeight.Bold else FontWeight.Normal) }
                                        )
                                    }
                                }

                                // Variation Strategy
                                Text("نوع التغيير واستراتيجية التنويع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    VariantStrategy.values().forEach { st ->
                                        val isSelected = seedStrategy == st
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { seedStrategy = st }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(selected = isSelected, onClick = { seedStrategy = st })
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(st.titleAr, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text(st.descriptionAr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Attached diagram indicator (if attached)
                                if (activeDiagramToAttach != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("مخطط مرفق بالسؤال: ${activeDiagramToAttach!!.title}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                                    Text("سيتم تحديث معطيات الرسم تلقائياً لكل نسخة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            IconButton(onClick = { activeDiagramToAttach = null }, modifier = Modifier.size(26.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "إزالة الرسم", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { selectedTab = 2 },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ربط رسم أو مخطط تعليمي بالسؤال (من التبويب 3)", fontSize = 12.sp)
                                    }
                                }

                                // Generate Variants Button
                                Button(
                                    onClick = {
                                        if (seedQuestionText.isBlank()) {
                                            Toast.makeText(context, "يرجى كتابة السؤال الأصلي أولاً", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isGeneratingVariants = true
                                        isVariantSetSaved = false
                                        variantProgress = Pair(0, requestedVariantCount)

                                        val seed = ExamQuestion(
                                            questionText = seedQuestionText.trim(),
                                            type = if (seedQuestionText.contains("=") || seedQuestionText.contains("احسب")) QuestionType.SHORT_ANSWER else QuestionType.MULTIPLE_CHOICE,
                                            correctAnswer = "حسب المعطيات",
                                            diagram = activeDiagramToAttach
                                        )

                                        coroutineScope.launch {
                                            val variants = geminiService.generateQuestionVariants(
                                                seedQuestion = seed,
                                                subject = seedSubject,
                                                grade = seedGrade,
                                                topic = seedTopic,
                                                difficulty = seedDifficulty,
                                                strategy = seedStrategy,
                                                count = requestedVariantCount,
                                                onProgress = { current: Int, total: Int ->
                                                    variantProgress = Pair(current, total)
                                                }
                                            )
                                            generatedVariants.clear()
                                            generatedVariants.addAll(variants)
                                            isGeneratingVariants = false
                                            Toast.makeText(context, "تم توليد ${variants.size} نسخة متكافئة ومحققة بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isGeneratingVariants,
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("generate_variants_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isGeneratingVariants) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("جارٍ توليد الصيغ (${variantProgress.first} من ${variantProgress.second})...", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("توليد النسخ ($requestedVariantCount نسخة للطلاب)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Variants Table and Action Buttons
                    if (generatedVariants.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "إدارة نسخ الطلاب (${generatedVariants.size} نسخة جاهزة):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFDCFCE7)) {
                                            Text(
                                                "✓ ${generatedVariants.count { it.verificationStatus == "VERIFIED" }} محققة آلياً",
                                                color = Color(0xFF166534),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Action Buttons Row (تصدير اختبارات الطلاب PDF / مفتاح الإجابة / حفظ المجموعة)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val vSet = QuestionVariantSet(
                                                    originalQuestion = ExamQuestion(
                                                        questionText = seedQuestionText,
                                                        type = QuestionType.SHORT_ANSWER,
                                                        correctAnswer = "",
                                                        diagram = activeDiagramToAttach
                                                    ),
                                                    subject = seedSubject,
                                                    grade = seedGrade,
                                                    topic = seedTopic,
                                                    difficulty = seedDifficulty,
                                                    variationType = seedStrategy,
                                                    variants = generatedVariants.toList()
                                                )
                                                PrintUtils.printStudentVariantExams(context, vSet)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("اختبارات الطلاب PDF", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val vSet = QuestionVariantSet(
                                                    originalQuestion = ExamQuestion(
                                                        questionText = seedQuestionText,
                                                        type = QuestionType.SHORT_ANSWER,
                                                        correctAnswer = "",
                                                        diagram = activeDiagramToAttach
                                                    ),
                                                    subject = seedSubject,
                                                    grade = seedGrade,
                                                    topic = seedTopic,
                                                    difficulty = seedDifficulty,
                                                    variationType = seedStrategy,
                                                    variants = generatedVariants.toList()
                                                )
                                                PrintUtils.printTeacherVariantKey(context, vSet)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("مفتاح الإجابة للمعلم", fontSize = 11.sp)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val entity = ExamEntity(
                                                        teacherId = teacher.id,
                                                        title = "مجموعة صيغ: $seedTopic (${generatedVariants.size} نسخة)",
                                                        subject = seedSubject,
                                                        grade = seedGrade,
                                                        difficulty = seedDifficulty,
                                                        questionsJson = ExamQuestion.listToJsonString(generatedVariants.toList())
                                                    )
                                                    repository.saveExam(entity)
                                                    isVariantSetSaved = true
                                                    Toast.makeText(context, "تم حفظ مجموعة النسخ في بنك الاختبارات بنجاح!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            enabled = !isVariantSetSaved
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isVariantSetSaved) "تم الحفظ" else "حفظ المجموعة", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Variants List
                        itemsIndexed(generatedVariants) { vIdx, variant ->
                            VariantCardItem(
                                index = vIdx,
                                variant = variant,
                                onEdit = { variantBeingEdited = Pair(vIdx, variant) },
                                onRegenerateSingle = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, "جارٍ إعادة توليد ${variant.variantLabel} فقط...", Toast.LENGTH_SHORT).show()
                                        val singleNew = geminiService.generateQuestionVariants(
                                            seedQuestion = variant,
                                            subject = seedSubject,
                                            grade = seedGrade,
                                            topic = seedTopic,
                                            difficulty = seedDifficulty,
                                            strategy = seedStrategy,
                                            count = 1
                                        )
                                        if (singleNew.isNotEmpty()) {
                                            generatedVariants[vIdx] = singleNew.first().copy(variantLabel = variant.variantLabel)
                                            Toast.makeText(context, "تم تحديث ${variant.variantLabel} بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDelete = { generatedVariants.removeAt(vIdx) }
                            )
                        }
                    }
                }
            }

            2 -> {
                // ==================== TAB 2: مكتبة ومحرر الرسومات التعليمية ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "الميزة الثانية: مكتبة الرسومات والمخططات التعليمية المصغرة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "رسومات علمية متقنة تدمج كجزء أساسي من السؤال وتطبع بدقة في PDF",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Category Chips
                                Text("اختر المجال أو المادة الدراسية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        Pair("PHYSICS", "الفيزياء والكهرباء"),
                                        Pair("MATHEMATICS", "الرياضيات والهندسة"),
                                        Pair("BIOLOGY", "العلوم والأحياء"),
                                        Pair("CHEMISTRY", "الكيمياء"),
                                        Pair("GEOGRAPHY", "الجغرافيا والاجتماعيات")
                                    ).forEach { (catKey, catTitle) ->
                                        FilterChip(
                                            selected = selectedDiagramCategory == catKey,
                                            onClick = {
                                                selectedDiagramCategory = catKey
                                                val firstInCat = EducationalDiagramLibrary.ALL_TEMPLATES.find { it.category == catKey }
                                                if (firstInCat != null) {
                                                    activePreviewTemplate = firstInCat
                                                    customDiagramLabels.clear()
                                                    customDiagramLabels.putAll(firstInCat.defaultLabels)
                                                }
                                            },
                                            label = { Text(catTitle, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                // Diagrams in category
                                Text("المخططات المتاحة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                val templatesInCat = remember(selectedDiagramCategory) {
                                    EducationalDiagramLibrary.ALL_TEMPLATES.filter { it.category == selectedDiagramCategory }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    templatesInCat.forEach { tmpl ->
                                        val isSel = tmpl.key == activePreviewTemplate.key
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                            border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    activePreviewTemplate = tmpl
                                                    customDiagramLabels.clear()
                                                    customDiagramLabels.putAll(tmpl.defaultLabels)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(selected = isSel, onClick = {
                                                    activePreviewTemplate = tmpl
                                                    customDiagramLabels.clear()
                                                    customDiagramLabels.putAll(tmpl.defaultLabels)
                                                })
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(tmpl.title, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Preview & Label Customizer Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("معاينة المخطط التفاعلي:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = hideDiagramLabelsForQuiz,
                                            onCheckedChange = { hideDiagramLabelsForQuiz = it }
                                        )
                                        Text("إخفاء التسميات للاختبار (أرقام 1، 2..)", fontSize = 11.sp)
                                    }
                                }

                                // Interactive Render Canvas / SVG
                                val currentSvg = remember(activePreviewTemplate, customDiagramLabels, hideDiagramLabelsForQuiz) {
                                    activePreviewTemplate.generator(customDiagramLabels, hideDiagramLabelsForQuiz)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SvgHtmlViewer(svgString = currentSvg, modifier = Modifier.fillMaxSize())
                                }

                                // Label values editor
                                Text("محرر قيم وتسميات المخطط:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                activePreviewTemplate.defaultLabels.keys.forEach { key ->
                                    val currentVal = customDiagramLabels[key] ?: ""
                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = { customDiagramLabels[key] = it },
                                        label = { Text("المعطى / التسمية ($key)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Button to attach to question
                                Button(
                                    onClick = {
                                        val diag = EducationalDiagramLibrary.buildDiagramInfo(
                                            templateKey = activePreviewTemplate.key,
                                            customLabels = customDiagramLabels,
                                            hideLabelsForQuiz = hideDiagramLabelsForQuiz
                                        )
                                        activeDiagramToAttach = diag
                                        selectedTab = 1 // Switch to multi-version tab
                                        Toast.makeText(context, "تم إرفاق المخطط بالسؤال بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("اعتماد وربط هذا المخطط بالسؤال", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for adding a question manually in Tab 0
    if (showAddQuestionDialog) {
        ManualQuestionDialog(
            onDismiss = { showAddQuestionDialog = false },
            onAdd = { newQ ->
                questionsList.add(newQ)
                showAddQuestionDialog = false
            }
        )
    }

    // Dialog for editing an existing question in Tab 0
    if (questionBeingEdited != null) {
        val (qIdx, qToEdit) = questionBeingEdited!!
        EditQuestionDialog(
            initialQuestion = qToEdit,
            onDismiss = { questionBeingEdited = null },
            onSave = { updated ->
                questionsList[qIdx] = updated
                questionBeingEdited = null
            }
        )
    }

    // Dialog for editing a single variant in Tab 1
    if (variantBeingEdited != null) {
        val (vIdx, vToEdit) = variantBeingEdited!!
        EditQuestionDialog(
            initialQuestion = vToEdit,
            dialogTitle = "تعديل ${vToEdit.variantLabel}",
            onDismiss = { variantBeingEdited = null },
            onSave = { updated ->
                generatedVariants[vIdx] = updated
                variantBeingEdited = null
            }
        )
    }
}

/**
 * Question Item Card with SVG Diagram display and overflow constraints
 */
@Composable
fun ExamQuestionItemCard(
    index: Int,
    question: ExamQuestion,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "س ${index + 1}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        question.type.titleAr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${question.marks} درجات", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                question.questionText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = true
            )

            // Diagram display if present
            if (question.diagram != null && question.diagram.svgContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    SvgHtmlViewer(svgString = question.diagram.svgContent, modifier = Modifier.fillMaxSize())
                }
            }

            if (question.options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    question.options.forEachIndexed { oIdx, opt ->
                        val isCorrect = opt == question.correctAnswer
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCorrect) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${listOf("أ", "ب", "ج", "د").getOrElse(oIdx) { "$oIdx" }}) $opt",
                                    fontSize = 12.sp,
                                    color = if (isCorrect) Color(0xFF166534) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    softWrap = true
                                )
                                if (isCorrect) {
                                    Text("✓ الإجابة المعتمدة", color = Color(0xFF166534), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "الإجابة النموذجية: ${question.correctAnswer}",
                    fontSize = 12.sp,
                    color = Color(0xFF166534),
                    fontWeight = FontWeight.SemiBold,
                    softWrap = true
                )
            }

            if (question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "💡 الشرح: ${question.explanation}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
            }
        }
    }
}

/**
 * Variant Card Item for Multi-Student Exam generation
 */
@Composable
fun VariantCardItem(
    index: Int,
    variant: ExamQuestion,
    onEdit: () -> Unit,
    onRegenerateSingle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            variant.variantLabel.ifBlank { "النسخة ${index + 1}" },
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (variant.verificationStatus == "VERIFIED") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                    ) {
                        Text(
                            if (variant.verificationStatus == "VERIFIED") "✓ محققة" else "⚠️ تحتاج مراجعة",
                            color = if (variant.verificationStatus == "VERIFIED") Color(0xFF166534) else Color(0xFF92400E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRegenerateSingle, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "إعادة توليد هذه النسخة فقط", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل يدوي", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف النسخة", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Text(
                variant.questionText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = true
            )

            // Render Diagram if present in variant
            if (variant.diagram != null && variant.diagram.svgContent.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    SvgHtmlViewer(svgString = variant.diagram.svgContent, modifier = Modifier.fillMaxSize())
                }
            }

            // Correct Answer & Verification Details
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "الإجابة المعتمدة: ${variant.correctAnswer}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        fontSize = 12.sp,
                        softWrap = true
                    )
                    if (variant.verificationNote.isNotBlank()) {
                        Text(
                            "🔍 التحقق: ${variant.verificationNote}",
                            color = Color(0xFF0369A1),
                            fontSize = 10.5.sp,
                            softWrap = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lightweight native HTML/SVG Viewer via AndroidView
 */
@Composable
fun SvgHtmlViewer(svgString: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
            }
        },
        update = { webView ->
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { margin: 0; padding: 0; display: flex; justify-content: center; align-items: center; background: transparent; overflow: hidden; }
                    svg { width: 100%; height: auto; max-height: 190px; }
                </style>
                </head>
                <body>
                    $svgString
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        },
        modifier = modifier
    )
}

/**
 * Manual Question Dialog with Diagram Selector
 */
@Composable
fun ManualQuestionDialog(
    onDismiss: () -> Unit,
    onAdd: (ExamQuestion) -> Unit
) {
    var qText by remember { mutableStateOf("") }
    var qType by remember { mutableStateOf(QuestionType.MULTIPLE_CHOICE) }
    var correctAns by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }
    var opt4 by remember { mutableStateOf("") }
    var marks by remember { mutableIntStateOf(2) }
    var attachedDiagram by remember { mutableStateOf<DiagramInfo?>(null) }
    var showDiagramPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("إضافة سؤال يدوياً", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = qText,
                    onValueChange = { qText = it },
                    label = { Text("نص السؤال *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE, QuestionType.SHORT_ANSWER).forEach { type ->
                        FilterChip(
                            selected = qType == type,
                            onClick = { qType = type },
                            label = { Text(type.titleAr, fontSize = 10.sp) }
                        )
                    }
                }

                if (qType == QuestionType.MULTIPLE_CHOICE) {
                    OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("الخيار أ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("الخيار ب") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text("الخيار ج") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt4, onValueChange = { opt4 = it }, label = { Text("الخيار د") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                OutlinedTextField(
                    value = correctAns,
                    onValueChange = { correctAns = it },
                    label = { Text("الإجابة الصحيحة أو النموذجية *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Diagram Button
                if (attachedDiagram != null) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("رسم مرفق: ${attachedDiagram!!.title}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { attachedDiagram = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showDiagramPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة رسم أو مخطط تعليمي", fontSize = 12.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (qText.isNotBlank() && correctAns.isNotBlank()) {
                                val optionsList = if (qType == QuestionType.MULTIPLE_CHOICE) {
                                    listOf(opt1, opt2, opt3, opt4).filter { it.isNotBlank() }
                                } else if (qType == QuestionType.TRUE_FALSE) {
                                    listOf("صح", "خطأ")
                                } else emptyList()

                                onAdd(
                                    ExamQuestion(
                                        questionText = qText.trim(),
                                        type = qType,
                                        options = optionsList,
                                        correctAnswer = correctAns.trim(),
                                        marks = marks,
                                        diagram = attachedDiagram
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إضافة السؤال")
                    }
                }
            }
        }
    }

    if (showDiagramPicker) {
        QuickDiagramPickerDialog(
            onDismiss = { showDiagramPicker = false },
            onSelect = { selected ->
                attachedDiagram = selected
                showDiagramPicker = false
            }
        )
    }
}

/**
 * Edit Question Dialog for editing question or variant
 */
@Composable
fun EditQuestionDialog(
    initialQuestion: ExamQuestion,
    dialogTitle: String = "تعديل السؤال",
    onDismiss: () -> Unit,
    onSave: (ExamQuestion) -> Unit
) {
    var qText by remember { mutableStateOf(initialQuestion.questionText) }
    var correctAns by remember { mutableStateOf(initialQuestion.correctAnswer) }
    var explanation by remember { mutableStateOf(initialQuestion.explanation) }
    var vNote by remember { mutableStateOf(initialQuestion.verificationNote) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(dialogTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = qText,
                    onValueChange = { qText = it },
                    label = { Text("نص السؤال") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = correctAns,
                    onValueChange = { correctAns = it },
                    label = { Text("الإجابة الصحيحة أو النموذجية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("الشرح وطريقة الحل") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vNote,
                    onValueChange = { vNote = it },
                    label = { Text("ملاحظة التحقق العلمي والرياضي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            onSave(
                                initialQuestion.copy(
                                    questionText = qText.trim(),
                                    correctAnswer = correctAns.trim(),
                                    explanation = explanation.trim(),
                                    verificationNote = vNote.trim()
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ التعديل")
                    }
                }
            }
        }
    }
}

/**
 * Quick Diagram Picker Dialog to attach to manual question
 */
@Composable
fun QuickDiagramPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (DiagramInfo) -> Unit
) {
    val templates = EducationalDiagramLibrary.ALL_TEMPLATES

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("اختر رسماً أو مخططاً تعليمياً", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                templates.forEach { tmpl ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val diag = EducationalDiagramLibrary.buildDiagramInfo(tmpl.key)
                                onSelect(diag)
                            }
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tmpl.title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(tmpl.categoryTitleAr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("إلغاء")
                }
            }
        }
    }
}
