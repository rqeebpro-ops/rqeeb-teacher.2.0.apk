package com.example.ui.teacher

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.ai.GeminiService
import com.example.data.model.*
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun YemeniPaperExamView(
    exam: ExamEntity,
    questions: List<ExamQuestion>,
    teacher: TeacherEntity,
    repository: RqeebRepository,
    geminiService: GeminiService,
    onBack: () -> Unit,
    onExamUpdated: (ExamEntity, List<ExamQuestion>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1) Current Exam Header State (Fully Editable by Teacher)
    var headerCountry by remember { mutableStateOf("الجمهورية اليمنية") }
    var headerMinistry by remember { mutableStateOf("وزارة التربية والتعليم والبحث العلمي") }
    var headerDirectorate by remember { mutableStateOf("مكتب التربية والتعليم") }
    var directorateName by remember { mutableStateOf(teacher.directorate.ifBlank { "مديرية معين التعليمية" }) }
    var schoolName by remember { mutableStateOf(exam.schoolName.ifBlank { teacher.schoolName.ifBlank { "مدرسة المختار النموذجية" } }) }
    var teacherName by remember { mutableStateOf(teacher.fullName.ifBlank { "أ. المعلم القدير" }) }
    var gradeText by remember { mutableStateOf(YemeniCurriculum.toDisplayGrade(exam.grade)) }
    var sectionText by remember { mutableStateOf("....................") }
    var periodText by remember { mutableStateOf("....................") }
    var academicYear by remember { mutableStateOf(exam.academicYear.ifBlank { "2025/2026م" }) }
    var subjectName by remember { mutableStateOf(exam.subject) }
    var examTypeTitle by remember { mutableStateOf(exam.examType.ifBlank { "اختبار شهري" }) }
    var examDate by remember { mutableStateOf("1446 / 05 / 15 هـ") }
    var durationText by remember { mutableStateOf("${exam.durationMinutes} دقيقة") }
    var studentNamePlaceholder by remember { mutableStateOf("...................................................") }
    var logoType by remember { mutableStateOf("EAGLE") } // EAGLE, MINISTRY, CUSTOM
    var showEmblem by remember { mutableStateOf(true) }
    var logoSizeDp by remember { mutableFloatStateOf(64f) }
    var primaryColorHex by remember { mutableStateOf("#006874") }
    val primaryColor = remember(primaryColorHex) {
        try { Color(android.graphics.Color.parseColor(primaryColorHex)) } catch (_: Throwable) { Color(0xFF006874) }
    }

    // 2) Questions List State
    val questionsList = remember { mutableStateListOf<ExamQuestion>().apply { addAll(questions) } }
    var activeFormCode by remember { mutableStateOf("A") } // Form A, Form B, Form C
    var isAnswerKeyView by remember { mutableStateOf(false) }

    // 3) Dialogs and BottomSheets
    var showHeaderEditor by remember { mutableStateOf(false) }
    var showInteractiveCanvas by remember { mutableStateOf(false) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var showFormGeneratorDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var editingQuestionIndex by remember { mutableIntStateOf(-1) }
    var isGeneratingSingleQuestion by remember { mutableIntStateOf(-1) }

    val totalCalculatedScore = remember(questionsList) { questionsList.sumOf { it.score } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "محرر ورقة الاختبار الرسمية (قالب أوراق Yemen)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "المادة: $subjectName • الدرجة الكلية: $totalCalculatedScore / ${exam.totalScore}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("yemeni_paper_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Save Exam State
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val updatedExam = exam.copy(
                                    title = exam.title.ifBlank { "اختبار $subjectName" },
                                    subject = subjectName,
                                    schoolName = schoolName,
                                    totalScore = totalCalculatedScore
                                )
                                onExamUpdated(updatedExam, questionsList.toList())
                                Toast.makeText(context, "تم حفظ جميع تعديلات الاختبار والورقة بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("save_paper_exam_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "حفظ", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Export Word
                    IconButton(
                        onClick = {
                            PrintUtils.exportExamToWord(
                                context = context,
                                exam = exam.copy(subject = subjectName, schoolName = schoolName, totalScore = totalCalculatedScore),
                                questions = questionsList.toList(),
                                isAnswerKey = isAnswerKeyView,
                                useOfficialMathTemplate = true,
                                customSchoolName = schoolName,
                                customDirectorate = directorateName,
                                customTeacherName = teacherName,
                                customGradeText = gradeText,
                                customAcademicYear = academicYear,
                                customExamDate = examDate,
                                customCountry = headerCountry,
                                customMinistry = headerMinistry
                            )
                        },
                        modifier = Modifier.testTag("export_paper_word_btn")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "تصدير Word", tint = Color(0xFF2563EB))
                    }

                    // Print PDF
                    IconButton(
                        onClick = {
                            PrintUtils.printExam(
                                context = context,
                                exam = exam.copy(subject = subjectName, schoolName = schoolName, totalScore = totalCalculatedScore),
                                questions = questionsList.toList(),
                                isAnswerKey = isAnswerKeyView,
                                useOfficialMathTemplate = true,
                                customSchoolName = schoolName,
                                customDirectorate = directorateName,
                                customTeacherName = teacherName,
                                customGradeText = gradeText,
                                customAcademicYear = academicYear,
                                customExamDate = examDate,
                                customCountry = headerCountry,
                                customMinistry = headerMinistry
                            )
                        },
                        modifier = Modifier.testTag("print_paper_pdf_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "طباعة / PDF", tint = Color(0xFF006874))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE2E8F0))
        ) {
            // Action Control Strip (View Modes, Header Editor, Forms, Add Question)
            Surface(
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Row 1: Mode Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = !isAnswerKeyView,
                            onClick = { isAnswerKeyView = false },
                            label = { Text("📄 ورقة الطالب", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = isAnswerKeyView,
                            onClick = { isAnswerKeyView = true },
                            label = { Text("🔑 نموذج الإجابة وسلّم الدرجات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1.3f)
                        )

                        // Form Selector Badges (Form A, B, C)
                        listOf("A", "B", "C").forEach { code ->
                            FilterChip(
                                selected = activeFormCode == code,
                                onClick = { activeFormCode = code },
                                label = { Text("نموذج $code", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Row 2: Editor Actions
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistChip(
                            onClick = { showInteractiveCanvas = true },
                            label = { Text("🎨 السحب والمحاذاة التفاعلية (Canvas)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.testTag("open_interactive_canvas_chip")
                        )

                        AssistChip(
                            onClick = { showHeaderEditor = true },
                            label = { Text("✏️ تعديل الترويسة والشعار", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )

                        AssistChip(
                            onClick = { showAddQuestionDialog = true },
                            label = { Text("➕ إضافة سؤال جديد", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )

                        AssistChip(
                            onClick = { showFormGeneratorDialog = true },
                            label = { Text("🔀 توليد نماذج متكافئة (أ/ب/ج)", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )

                        AssistChip(
                            onClick = { showSaveTemplateDialog = true },
                            label = { Text("💾 حفظ كقالب خاص بي", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            // Interactive Canvas Full-screen Dialog
            if (showInteractiveCanvas) {
                Dialog(
                    onDismissRequest = { showInteractiveCanvas = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        InteractiveTemplateCanvas(
                            teacher = teacher,
                            exam = exam.copy(subject = subjectName, schoolName = schoolName, totalScore = totalCalculatedScore),
                            questions = questionsList.toList(),
                            repository = repository,
                            onClose = { showInteractiveCanvas = false }
                        )
                    }
                }
            }

            // Scrollable Live A4 Paper Sheet Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                // A4 Paper Container matching attached visual
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("yemeni_a4_paper_container")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick Edit Header Toolbar Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = primaryColor.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor),
                            modifier = Modifier.fillMaxWidth().clickable { showHeaderEditor = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                    Text("الترويسة الرسمية قابلة للتعديل بالكامل (اسم المدرسة، المادة، المعلم...)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                }
                                Text(
                                    "تعديل ✏️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }

                        // Decorative Corner Accent Header Frame
                        YemeniOfficialPaperHeader(
                            headerCountry = headerCountry,
                            headerMinistry = headerMinistry,
                            headerDirectorate = headerDirectorate,
                            directorateName = directorateName,
                            schoolName = schoolName,
                            teacherName = teacherName,
                            gradeText = gradeText,
                            sectionText = sectionText,
                            periodText = periodText,
                            academicYear = academicYear,
                            subjectName = subjectName,
                            examTypeTitle = examTypeTitle,
                            examDate = examDate,
                            durationText = durationText,
                            studentNamePlaceholder = studentNamePlaceholder,
                            logoType = logoType,
                            showEmblem = showEmblem,
                            logoSizeDp = logoSizeDp,
                            primaryColor = primaryColor,
                            isAnswerKeyView = isAnswerKeyView,
                            activeFormCode = activeFormCode,
                            onEditHeader = { showHeaderEditor = true }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Instructions / Banner Row
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "أجب مستعيناً بالله عن جميع الأسئلة التالية بخط جميل وواضح في نفس الورقة:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        // Question Containers (Matching reference image blocks)
                        questionsList.forEachIndexed { idx, question ->
                            YemeniQuestionBox(
                                index = idx,
                                question = question,
                                isAnswerKeyView = isAnswerKeyView,
                                primaryColor = primaryColor,
                                isGeneratingSingle = isGeneratingSingleQuestion == idx,
                                onEditQuestion = { editingQuestionIndex = idx },
                                onDeleteQuestion = {
                                    questionsList.removeAt(idx)
                                    Toast.makeText(context, "تم حذف السؤال ${idx + 1}", Toast.LENGTH_SHORT).show()
                                },
                                onMoveUp = {
                                    if (idx > 0) {
                                        val item = questionsList.removeAt(idx)
                                        questionsList.add(idx - 1, item)
                                    }
                                },
                                onMoveDown = {
                                    if (idx < questionsList.size - 1) {
                                        val item = questionsList.removeAt(idx)
                                        questionsList.add(idx + 1, item)
                                    }
                                },
                                onRegenerateSingle = {
                                    isGeneratingSingleQuestion = idx
                                    coroutineScope.launch {
                                        try {
                                            val newQs = geminiService.generateRichExamQuestions(
                                                subject = subjectName,
                                                grade = exam.grade,
                                                topic = exam.unitName.ifBlank { "المقرر المعتمد" },
                                                examType = exam.examType,
                                                count = 1,
                                                totalScore = question.score,
                                                difficulty = exam.difficulty,
                                                selectedTypes = listOf(question.type.toArabicName()),
                                                includeDiagrams = true
                                            )
                                            val newQ = newQs.firstOrNull()
                                            if (newQ != null) {
                                                questionsList[idx] = newQ.copy(id = question.id)
                                                Toast.makeText(context, "تم إعادة توليد السؤال ${idx + 1} بنجاح!", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطأ في التوليد: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingSingleQuestion = -1
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom Summary Score Table (Matching Bottom of Attached Reference Image)
                        YemeniScoreSummaryTable(
                            questions = questionsList.toList(),
                            primaryColor = primaryColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Page Footer Matching Reference Image
                        Text(
                            "الصفحة 1 من 1   •   « انتهت الأسئلة • مع تمنياتنا لكم بالتوفيق والنجاح »",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // 1) Header & Logo Editor Dialog (Full Teacher Customization)
    if (showHeaderEditor) {
        AlertDialog(
            onDismissRequest = { showHeaderEditor = false },
            icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = primaryColor) },
            title = {
                Text(
                    "تعديل الترويسة والبيانات الرسمية والمدرسة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🏫 1. بيانات المدرسة والجهة التعليمية:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("اسم المدرسة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_school_name_field")
                    )
                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = { Text("اسم المعلم / أستاذ المادة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_teacher_name_field")
                    )
                    OutlinedTextField(
                        value = directorateName,
                        onValueChange = { directorateName = it },
                        label = { Text("اسم المديرية / المنطقة التعليمية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = headerMinistry,
                        onValueChange = { headerMinistry = it },
                        label = { Text("الوزارة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = headerCountry,
                        onValueChange = { headerCountry = it },
                        label = { Text("اسم الدولة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("📚 2. تفاصيل الاختبار والصف والدراسة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = subjectName,
                            onValueChange = { subjectName = it },
                            label = { Text("اسم المادة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gradeText,
                            onValueChange = { gradeText = it },
                            label = { Text("الصف") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sectionText,
                            onValueChange = { sectionText = it },
                            label = { Text("الشعبة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = periodText,
                            onValueChange = { periodText = it },
                            label = { Text("الفترة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { academicYear = it },
                        label = { Text("العام الدراسي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = examTypeTitle,
                        onValueChange = { examTypeTitle = it },
                        label = { Text("نوع / عنوان الاختبار (شهري/فصلي/نهائي...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = examDate,
                            onValueChange = { examDate = it },
                            label = { Text("التاريخ") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it },
                            label = { Text("الزمن") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = studentNamePlaceholder,
                        onValueChange = { studentNamePlaceholder = it },
                        label = { Text("نص خانة اسم الطالب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("🎨 3. المظهر والشعار واللون الرسمى:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إظهار شعار الطير الجمهوري", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = showEmblem,
                            onCheckedChange = { showEmblem = it }
                        )
                    }

                    if (showEmblem) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("حجم شعار الطير الجمهوري:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${logoSizeDp.toInt()} dp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            }
                            Slider(
                                value = logoSizeDp,
                                onValueChange = { logoSizeDp = it },
                                valueRange = 30f..120f,
                                steps = 17,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Text("اختر لون الترويسة والحدود:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val colorPresets = listOf(
                            "#006874" to "أزرق كحلي",
                            "#0F172A" to "كحلي داكن",
                            "#0052CC" to "أزرق ملكي",
                            "#004D40" to "أخضر تربوي",
                            "#800020" to "عنابي ملكي"
                        )
                        colorPresets.forEach { (hex, name) ->
                            FilterChip(
                                selected = primaryColorHex == hex,
                                onClick = { primaryColorHex = hex },
                                label = { Text(name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHeaderEditor = false
                        Toast.makeText(context, "تم حفظ وتحديث الترويسة بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("save_header_changes_btn")
                ) {
                    Text("تم وحفظ التعديلات")
                }
            }
        )
    }

    // 2) Question Edit / Add Dialog
    if (editingQuestionIndex != -1 || showAddQuestionDialog) {
        val isEditing = editingQuestionIndex != -1
        val q = if (isEditing) questionsList[editingQuestionIndex] else ExamQuestion(
            id = questionsList.size + 1,
            type = QuestionType.MULTIPLE_CHOICE,
            text = "",
            score = 2
        )

        var qText by remember(q) { mutableStateOf(q.text) }
        var qScore by remember(q) { mutableIntStateOf(q.score) }
        var qType by remember(q) { mutableStateOf(q.type) }
        var qCorrect by remember(q) { mutableStateOf(q.correctAnswer) }
        var qExplanation by remember(q) { mutableStateOf(q.explanation) }
        var optionsText by remember(q) { mutableStateOf(q.options.joinToString("\n")) }
        var qDiagramType by remember(q) { mutableStateOf(q.diagramType ?: "NONE") }

        AlertDialog(
            onDismissRequest = {
                editingQuestionIndex = -1
                showAddQuestionDialog = false
            },
            title = {
                Text(if (isEditing) "تعديل السؤال ${editingQuestionIndex + 1}" else "إضافة سؤال جديد للورقة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qText,
                        onValueChange = { qText = it },
                        label = { Text("نص السؤال *") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qScore.toString(),
                            onValueChange = { qScore = it.toIntOrNull() ?: 1 },
                            label = { Text("الدرجة") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Question Type Selector
                    Text("نوع السؤال:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        QuestionType.entries.take(8).forEach { type ->
                            FilterChip(
                                selected = qType == type,
                                onClick = { qType = type },
                                label = { Text(type.toArabicName(), fontSize = 10.sp) }
                            )
                        }
                    }

                    if (qType == QuestionType.MULTIPLE_CHOICE) {
                        OutlinedTextField(
                            value = optionsText,
                            onValueChange = { optionsText = it },
                            label = { Text("الخيارات (كل خيار في سطر)") },
                            placeholder = { Text("أ) خيار 1\nب) خيار 2\nج) خيار 3\nد) خيار 4") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = qCorrect,
                        onValueChange = { qCorrect = it },
                        label = { Text("الإجابة الصحيحة (لنموذج المعلم)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = qExplanation,
                        onValueChange = { qExplanation = it },
                        label = { Text("سلم الدرجات وطريقة الحل") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Diagram Selector
                    Text("إرفاق شكل / رسم توضيحي:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    var diagExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = ExamDiagramTypes.ALL_DIAGRAM_TYPES.find { it.first == qDiagramType }?.second ?: "بدون رسم",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { IconButton(onClick = { diagExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = null) } },
                            modifier = Modifier.fillMaxWidth().clickable { diagExpanded = true }
                        )
                        DropdownMenu(expanded = diagExpanded, onDismissRequest = { diagExpanded = false }) {
                            ExamDiagramTypes.ALL_DIAGRAM_TYPES.forEach { dt ->
                                DropdownMenuItem(
                                    text = { Text(dt.second, fontSize = 11.sp) },
                                    onClick = {
                                        qDiagramType = dt.first
                                        diagExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val opts = optionsText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                        val updated = ExamQuestion(
                            id = q.id,
                            type = qType,
                            text = qText,
                            options = opts,
                            correctAnswer = qCorrect,
                            score = qScore,
                            explanation = qExplanation,
                            diagramType = if (qDiagramType != "NONE") qDiagramType else null,
                            diagramInstruction = if (qDiagramType != "NONE") "لاحظ الرسم وأجب" else null
                        )

                        if (isEditing) {
                            questionsList[editingQuestionIndex] = updated
                        } else {
                            questionsList.add(updated)
                        }

                        editingQuestionIndex = -1
                        showAddQuestionDialog = false
                    }
                ) {
                    Text("حفظ السؤال")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingQuestionIndex = -1
                        showAddQuestionDialog = false
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 3) Multiple Forms Generator Modal
    if (showFormGeneratorDialog) {
        AlertDialog(
            onDismissRequest = { showFormGeneratorDialog = false },
            icon = { Icon(Icons.Default.Shuffle, contentDescription = null, tint = primaryColor) },
            title = { Text("توليد نماذج متكافئة (نموذج أ / ب / ج)", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "يقوم النظام بإعادة ترتيب الأسئلة وتغيير أرقام المسائل والخيارات ذكياً لإنشاء نماذج اختبارية مختلفة ومساوية تماماً في درجة الصعوبة وسلّم الدرجات.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                activeFormCode = "B"
                                val shuffled = questionsList.shuffled().mapIndexed { i, item ->
                                    item.copy(id = i + 1, options = item.options.shuffled())
                                }
                                questionsList.clear()
                                questionsList.addAll(shuffled)
                                showFormGeneratorDialog = false
                                Toast.makeText(context, "تم توليد النموذج (ب) بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("توليد نموذج (ب)")
                        }

                        Button(
                            onClick = {
                                activeFormCode = "C"
                                val shuffled = questionsList.shuffled().mapIndexed { i, item ->
                                    item.copy(id = i + 1, options = item.options.shuffled())
                                }
                                questionsList.clear()
                                questionsList.addAll(shuffled)
                                showFormGeneratorDialog = false
                                Toast.makeText(context, "تم توليد النموذج (ج) بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("توليد نموذج (ج)")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormGeneratorDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // 4) Save as Custom Template Dialog
    if (showSaveTemplateDialog) {
        var tName by remember { mutableStateOf("قالب مدرسة ${schoolName.ifBlank { "النموذجية" }}") }
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            icon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = primaryColor) },
            title = { Text("حفظ كقالب خاص بي", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حفظ تصميم الترويسة والجداول والألوان لاستخدامها لاحقاً في جميع المواد والصفوف.", fontSize = 11.sp)
                    OutlinedTextField(
                        value = tName,
                        onValueChange = { tName = it },
                        label = { Text("اسم القالب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val template = CustomExamTemplateEntity(
                                teacherId = teacher.id,
                                templateName = tName,
                                countryName = headerCountry,
                                ministryName = headerMinistry,
                                directorateName = directorateName,
                                schoolName = schoolName,
                                logoType = logoType,
                                primaryColorHex = primaryColorHex
                            )
                            repository.insertCustomExamTemplate(template)
                            showSaveTemplateDialog = false
                            Toast.makeText(context, "تم حفظ القالب الخاص بك بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("حفظ القالب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Official Yemeni 3-Section Header Box matching the attached reference image
 */
@Composable
private fun YemeniOfficialPaperHeader(
    headerCountry: String,
    headerMinistry: String,
    headerDirectorate: String,
    directorateName: String,
    schoolName: String,
    teacherName: String,
    gradeText: String,
    sectionText: String,
    periodText: String,
    academicYear: String,
    subjectName: String,
    examTypeTitle: String,
    examDate: String,
    durationText: String,
    studentNamePlaceholder: String,
    logoType: String,
    showEmblem: Boolean,
    logoSizeDp: Float,
    primaryColor: Color,
    isAnswerKeyView: Boolean,
    activeFormCode: String,
    onEditHeader: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditHeader() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Cell: Republic & Ministry Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.weight(1.1f)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(headerCountry, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    Text(headerMinistry, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
                    Text(headerDirectorate, fontSize = 9.5.sp, color = Color(0xFF334155))
                    Text("مديرية : $directorateName", fontSize = 9.5.sp, color = Color.Black)
                    Text("المدرسة : $schoolName", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = primaryColor, textAlign = TextAlign.Center)
                    if (teacherName.isNotBlank()) {
                        Text("أستاذ المادة: $teacherName", fontSize = 9.5.sp, color = Color(0xFF1E293B))
                    }
                }
            }

            // Center Cell: Eagle Logo & Subject Title
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (showEmblem) {
                    GoldenEagleLogo(
                        modifier = Modifier
                            .size(logoSizeDp.dp)
                            .padding(bottom = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primaryColor.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "اختبار مادة : $subjectName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            textAlign = TextAlign.Center
                        )
                        if (academicYear.isNotBlank()) {
                            Text(
                                "للعام الدراسي: $academicYear",
                                fontSize = 9.5.sp,
                                color = Color(0xFF334155),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = primaryColor
                        ) {
                            Text(
                                "$examTypeTitle (نموذج $activeFormCode)",
                                fontSize = 9.5.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Left Cell: Date, Student, Subject, Duration Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.weight(1.1f)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("اسم الطالب: ${studentNamePlaceholder.ifBlank { "..................................................." }}", fontSize = 9.5.sp, maxLines = 1)
                    Text("التاريخ : ${examDate.ifBlank { "..... / ..... / 2026م" }}", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    Text("الفترة : ${periodText.ifBlank { "...................." }}", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    Text("الصف : $gradeText (${sectionText.ifBlank { "...................." }})", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    Text("المادة : ${subjectName.ifBlank { "...................." }}", fontSize = 9.5.sp, color = Color.Black)
                    Text("الزمن : ${durationText.ifBlank { "...................." }}", fontSize = 9.5.sp, color = Color.Black)
                }
            }
        }

        if (isAnswerKeyView) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFDCFCE7),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "🔑 [ نموذج الإجابة الرسمي وسلم الدرجات للمعلم ]",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Question Box Block matching the reference image layout
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YemeniQuestionBox(
    index: Int,
    question: ExamQuestion,
    isAnswerKeyView: Boolean,
    primaryColor: Color,
    isGeneratingSingle: Boolean,
    onEditQuestion: () -> Unit,
    onDeleteQuestion: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRegenerateSingle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("yemeni_question_box_$index")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar: [ م ] | [ السؤال ] | [ الدرجة ]
            Surface(
                color = primaryColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "السؤال ${index + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "الدرجة : ${question.score}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Actions: Edit, Regenerate, Up, Down, Delete
                        IconButton(onClick = onEditQuestion, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White, modifier = Modifier.size(14.dp))
                        }

                        IconButton(onClick = onRegenerateSingle, modifier = Modifier.size(24.dp)) {
                            if (isGeneratingSingle) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "إعادة توليد", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }

                        IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "أعلى", tint = Color.White, modifier = Modifier.size(14.dp))
                        }

                        IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "أسفل", tint = Color.White, modifier = Modifier.size(14.dp))
                        }

                        IconButton(onClick = onDeleteQuestion, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Question Content Area
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    question.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 18.sp
                )

                // Render Options if MCQ
                if (question.options.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        question.options.forEach { opt ->
                            val isCorrect = isAnswerKeyView && opt.trim() == question.correctAnswer.trim()
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCorrect) Color(0xFF16A34A) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    opt,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCorrect) Color(0xFF15803D) else Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Render True / False or Fill Blank lines
                if (question.type == QuestionType.TRUE_FALSE && !isAnswerKeyView) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("(   ) صح", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("(   ) خطأ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Render Diagram if present
                if (!question.diagramType.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ExamDiagramView(
                            diagramType = question.diagramType,
                            isAnswerKey = isAnswerKeyView
                        )
                    }
                }

                // Student Work Space / Dotted lines
                if (!isAnswerKeyView && question.options.isEmpty() && question.type != QuestionType.TRUE_FALSE) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("الإجابة / خطوات الحل:", fontSize = 10.sp, color = Color.Gray)
                        Text("...........................................................................................................................................", fontSize = 10.sp, color = Color.LightGray)
                        Text("...........................................................................................................................................", fontSize = 10.sp, color = Color.LightGray)
                    }
                }

                // Teacher Answer Key details
                if (isAnswerKeyView) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("✓ الإجابة النموذجية:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF166534))
                            Text(question.correctAnswer.ifBlank { "الحل مطابق للشروط" }, fontSize = 11.sp, color = Color(0xFF14532D))
                            if (question.explanation.isNotBlank()) {
                                Text("سلم الدرجات: ${question.explanation}", fontSize = 10.sp, color = Color(0xFF15803D))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Score Summary Table matching the Bottom of the attached reference image
 */
@Composable
private fun YemeniScoreSummaryTable(
    questions: List<ExamQuestion>,
    primaryColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor),
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val count = questions.size.coerceAtLeast(4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor.copy(alpha = 0.12f)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0 until count).forEach { i ->
                    val qNumStr = when (i) {
                        0 -> "الأول"
                        1 -> "الثاني"
                        2 -> "الثالث"
                        3 -> "الرابع"
                        4 -> "الخامس"
                        else -> "س ${i + 1}"
                    }
                    Text(
                        "السؤال $qNumStr",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    )
                }
                Text(
                    "المجموع",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                )
            }

            Divider(color = primaryColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0 until count).forEach { i ->
                    val score = questions.getOrNull(i)?.score ?: 0
                    Text(
                        if (score > 0) "/ $score" else "",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp)
                    )
                }
                Text(
                    "/ ${questions.sumOf { it.score }}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Golden Republican Eagle Logo (Yemeni Republic Emblem)
 */
@Composable
fun GoldenEagleLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.img_yemen_eagle),
        contentDescription = "شعار الجمهورية اليمنية",
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = modifier
    )
}
