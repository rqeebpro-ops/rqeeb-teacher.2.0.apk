package com.example.ui.teacher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ai.GeminiService
import com.example.data.model.LessonEntity
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPrepScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 1) Selection State: Grade, Subject, Term, Unit, Lesson
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

    // Cascading Curriculum Units
    val curriculumUnits = remember(grade, subject, term) {
        YemeniCurriculum.getCurriculumUnits(grade, subject, term)
    }

    var selectedUnitIndex by remember(grade, subject, term) { mutableIntStateOf(0) }
    var isCustomUnit by remember { mutableStateOf(false) }
    var customUnitName by remember { mutableStateOf("") }

    val currentUnitTitle = if (isCustomUnit) {
        customUnitName
    } else {
        curriculumUnits.getOrNull(selectedUnitIndex)?.title ?: "الوحدة الأولى"
    }

    val currentUnitLessons = remember(selectedUnitIndex, curriculumUnits, isCustomUnit) {
        if (!isCustomUnit) curriculumUnits.getOrNull(selectedUnitIndex)?.lessons ?: emptyList() else emptyList()
    }

    var selectedLessonIndex by remember(selectedUnitIndex, currentUnitLessons) { mutableIntStateOf(0) }
    var isCustomLesson by remember { mutableStateOf(false) }
    var customLessonTitle by remember { mutableStateOf("") }

    val currentLessonTitle = if (isCustomLesson) {
        customLessonTitle
    } else {
        currentUnitLessons.getOrNull(selectedLessonIndex) ?: ""
    }

    // Basic metadata auto-filled from teacher profile
    var schoolName by remember { mutableStateOf(teacher.schoolName.ifBlank { "مدرسة التعليم الأساسي والثانوي" }) }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var periodNumber by remember { mutableStateOf("الحصة الأولى") }
    var lessonDate by remember { mutableStateOf("1446/05/15هـ") }
    var prepType by remember { mutableStateOf(YemeniCurriculum.PREPARATION_TYPES.first()) }
    var textbookRef by remember {
        mutableStateOf("كتاب $subject - $grade - طبعة ${teacher.curriculumEdition.ifBlank { "صنعاء" }}")
    }

    // 11 Teaching Strategies (Checkboxes)
    val selectedStrategies = remember {
        mutableStateListOf(
            "الحوار والمناقشة",
            "التعلم التعاوني",
            "العصف الذهني"
        )
    }

    // 16 Detailed Lesson Content Sections
    var cognitiveObjectives by remember { mutableStateOf("") }
    var skillObjectives by remember { mutableStateOf("") }
    var affectiveObjectives by remember { mutableStateOf("") }
    var expectedOutcomes by remember { mutableStateOf("") }
    var targetKnowledgeAndSkills by remember { mutableStateOf("") }
    var prerequisites by remember { mutableStateOf("") }
    var introduction by remember { mutableStateOf("") }
    var teachingSteps by remember { mutableStateOf("") }
    var teacherStudentActivities by remember { mutableStateOf("") }
    var discussionQuestions by remember { mutableStateOf("") }
    var diagnosticAssessment by remember { mutableStateOf("") }
    var formativeAssessment by remember { mutableStateOf("") }
    var summativeAssessment by remember { mutableStateOf("") }
    var homework by remember { mutableStateOf("") }
    var remedialActivities by remember { mutableStateOf("") }
    var enrichmentActivities by remember { mutableStateOf("") }
    var timeDistribution by remember { mutableStateOf("") }
    var teachingAids by remember { mutableStateOf("") }
    var teacherNotes by remember { mutableStateOf("") }

    // Generation State & Gemini Status
    var isAiGenerating by remember { mutableStateOf(false) }
    var isGeneratedFromGeminiApi by remember { mutableStateOf(false) }
    var hasGeneratedContent by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(true) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    // Interactive View Tabs
    var selectedPlanTab by remember { mutableIntStateOf(0) }

    // Dropdown toggles
    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var termDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var lessonDropdownExpanded by remember { mutableStateOf(false) }

    fun populateAiContent(res: GeminiService.FullLessonPlanAiResult) {
        cognitiveObjectives = res.cognitiveObjectives
        skillObjectives = res.skillObjectives
        affectiveObjectives = res.affectiveObjectives
        expectedOutcomes = res.expectedOutcomes
        targetKnowledgeAndSkills = res.targetKnowledgeAndSkills
        prerequisites = res.prerequisites
        introduction = res.introduction
        teachingSteps = res.teachingSteps
        teacherStudentActivities = res.teacherStudentActivities
        discussionQuestions = res.discussionQuestions
        diagnosticAssessment = res.diagnosticAssessment
        formativeAssessment = res.formativeAssessment
        summativeAssessment = res.summativeAssessment
        homework = res.homework
        remedialActivities = res.remedialActivities
        enrichmentActivities = res.enrichmentActivities
        timeDistribution = res.timeDistribution
        teachingAids = res.teachingAids
        teacherNotes = res.teacherNotes
        isGeneratedFromGeminiApi = res.isFromGeminiApi
        hasGeneratedContent = true
    }

    fun buildCurrentEntity(): LessonEntity {
        return LessonEntity(
            id = 0,
            teacherId = teacher.id,
            title = currentLessonTitle.ifBlank { "درس غير معنون" },
            subject = subject,
            grade = grade,
            durationMinutes = durationMinutes,
            schoolName = schoolName,
            directorate = teacher.directorate,
            governorate = teacher.governorate,
            term = term,
            unitName = currentUnitTitle,
            lessonDate = lessonDate,
            periodNumber = periodNumber,
            studentLevel = "متوسط / اعتيادي",
            preparationType = prepType,
            cognitiveObjectives = cognitiveObjectives,
            skillObjectives = skillObjectives,
            affectiveObjectives = affectiveObjectives,
            expectedOutcomes = expectedOutcomes,
            targetKnowledgeAndSkills = targetKnowledgeAndSkills,
            prerequisites = prerequisites,
            introduction = introduction,
            teachingSteps = teachingSteps,
            teacherStudentActivities = teacherStudentActivities,
            discussionQuestions = discussionQuestions,
            diagnosticAssessment = diagnosticAssessment,
            formativeAssessment = formativeAssessment,
            summativeAssessment = summativeAssessment,
            homework = homework,
            remedialActivities = remedialActivities,
            enrichmentActivities = enrichmentActivities,
            timeDistribution = timeDistribution,
            teacherNotes = teacherNotes,
            teachingMethods = selectedStrategies.joinToString("، "),
            teachingAids = teachingAids,
            textbookReference = textbookRef,
            curriculumEdition = teacher.curriculumEdition
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(34.dp),
                            shadowElevation = 2.dp
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_logo),
                                contentDescription = "شعار المعلم التربوي الذكي",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("تحضير الدرس – المعلم الذكي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("المنهج اليمني • Gemini AI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("lesson_prep_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // API Key Settings Icon
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("gemini_api_key_settings_button")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = "إعدادات Gemini API", tint = Color(0xFF006874))
                    }
                    IconButton(
                        onClick = {
                            val entity = buildCurrentEntity()
                            PrintUtils.printLesson(context, entity)
                        },
                        modifier = Modifier.testTag("print_lesson_pdf_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "طباعة PDF")
                    }
                    IconButton(
                        onClick = {
                            val entity = buildCurrentEntity()
                            PrintUtils.exportLessonToWord(context, entity)
                        },
                        modifier = Modifier.testTag("export_lesson_word_button")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "تصدير Word")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card with Logo and Teacher Badge
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF004D40),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_logo),
                                contentDescription = "شعار المعلم الذكي",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "الجمهورية اليمنية • وزارة التربية والتعليم",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "نموذج خطة الدرس التفاعلية (المعلم الذكي)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "المدرسة: $schoolName",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        val sec = if (teacher.section.isNotBlank()) " | شعبة ${teacher.section}" else ""
                        Text(
                            "المعلم: ${teacher.fullName.ifBlank { "معلم المادة" }}$sec",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 1. Selector Component (الصف، المادة، الفصل، الوحدة، الدرس)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
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
                        Text(
                            "اختيار المنهج والدرس التفاعلي",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                        // Gemini AI Status Badge
                        val isConfigured = geminiService.isConfigured(context)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isConfigured) Color(0xFFE8F5E9) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isConfigured) Color(0xFF81C784) else Color(0xFFCBD5E1))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isConfigured) Color(0xFF2E7D32) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isConfigured) "Gemini API مفعّل" else "المحرك المنهجي جاهز",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConfigured) Color(0xFF1B5E20) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Grade & Subject Cascading Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Grade Selector (1 to 12)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = grade,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الصف الدراسي *") },
                                trailingIcon = {
                                    IconButton(onClick = { gradeDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { gradeDropdownExpanded = true }.testTag("grade_selector_input")
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

                        // Subject Selector
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = subject,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("المادة الدراسية *") },
                                trailingIcon = {
                                    IconButton(onClick = { subjectDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { subjectDropdownExpanded = true }.testTag("subject_selector_input")
                            )
                            DropdownMenu(
                                expanded = subjectDropdownExpanded,
                                onDismissRequest = { subjectDropdownExpanded = false }
                            ) {
                                availableSubjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub) },
                                        onClick = {
                                            subject = sub
                                            subjectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Term & Unit Cascading Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Term Selector
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = term,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الفصل الدراسي *") },
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

                        // Unit Selector (Cascading from Grade + Subject + Term)
                        Box(modifier = Modifier.weight(1.2f)) {
                            OutlinedTextField(
                                value = if (isCustomUnit) "يدوي: $customUnitName" else currentUnitTitle,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الوحدة المنهجية *") },
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
                                            isCustomUnit = false
                                            unitDropdownExpanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("+ إدخال اسم الوحدة يدوياً", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        isCustomUnit = true
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (isCustomUnit) {
                        OutlinedTextField(
                            value = customUnitName,
                            onValueChange = { customUnitName = it },
                            label = { Text("اكتب اسم الوحدة التعليمية يدوياً") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Lesson Selector (Cascading from Unit)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (isCustomLesson) "يدوي: $customLessonTitle" else currentLessonTitle.ifBlank { "اختر الدرس أو أدخله يدوياً" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("عنوان أو موضوع الدرس *") },
                            trailingIcon = {
                                IconButton(onClick = { lessonDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { lessonDropdownExpanded = true }.testTag("lesson_title_dropdown")
                        )
                        DropdownMenu(
                            expanded = lessonDropdownExpanded,
                            onDismissRequest = { lessonDropdownExpanded = false }
                        ) {
                            currentUnitLessons.forEachIndexed { lIdx, lTitle ->
                                DropdownMenuItem(
                                    text = { Text(lTitle) },
                                    onClick = {
                                        selectedLessonIndex = lIdx
                                        isCustomLesson = false
                                        lessonDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ إدخال عنوان درس مخصص يدوياً", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    isCustomLesson = true
                                    lessonDropdownExpanded = false
                                }
                            )
                        }
                    }

                    if (isCustomLesson || currentLessonTitle.isBlank()) {
                        OutlinedTextField(
                            value = customLessonTitle,
                            onValueChange = { customLessonTitle = it },
                            label = { Text("اكتب عنوان الدرس يدوياً *") },
                            placeholder = { Text("مثال: حل معادلات الدرجة الأولى بمجهول واحد") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_lesson_title_input")
                        )
                    }

                    // Duration, Period, Date Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lessonDate,
                            onValueChange = { lessonDate = it },
                            label = { Text("التاريخ") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = periodNumber,
                            onValueChange = { periodNumber = it },
                            label = { Text("الحصة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = durationMinutes.toString(),
                            onValueChange = { durationMinutes = it.toIntOrNull() ?: 45 },
                            label = { Text("الزمن (د)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // Generate with Gemini API Button
                    Button(
                        onClick = {
                            val actualTitle = currentLessonTitle.ifBlank { customLessonTitle }
                            if (actualTitle.isBlank()) {
                                Toast.makeText(context, "يرجى تحديد أو كتابة عنوان الدرس أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isAiGenerating = true
                            coroutineScope.launch {
                                val plan = geminiService.generateFullLessonPlan(
                                    subject = subject,
                                    grade = grade,
                                    unitName = currentUnitTitle,
                                    lessonTitle = actualTitle,
                                    selectedStrategies = selectedStrategies.toList(),
                                    context = context
                                )
                                populateAiContent(plan)
                                isAiGenerating = false
                                val msg = if (plan.isFromGeminiApi) {
                                    "تم توليد التحضير التفاعلي بدقة عبر Gemini API!"
                                } else {
                                    "تم توليد التحضير المنهجي النموذجي بنجاح!"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("ai_generate_lesson_plan_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isAiGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جاري توليد خطة الدرس عبر Gemini AI...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد محتوى الخطة تفاعلياً (Gemini API)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // 2. Teaching Strategies (11 Checkboxes)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "استراتيجيات التدريس التفاعلية (اختر الاستراتيجيات المطبقة):",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    YemeniCurriculum.TEACHING_STRATEGIES_11.chunked(2).forEach { rowPair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowPair.forEach { strategy ->
                                val isChecked = selectedStrategies.contains(strategy)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (isChecked) selectedStrategies.remove(strategy) else selectedStrategies.add(strategy)
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = if (isChecked) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedStrategies.add(strategy) else selectedStrategies.remove(strategy)
                                            }
                                        )
                                        Text(strategy, fontSize = 12.sp, fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            if (rowPair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 3. Interactive Lesson Plan Model Display (نموذج خطة التحضير التفاعلي المنظم)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth().testTag("interactive_lesson_plan_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header of the Plan Model
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "نموذج خطة تحضير الدرس التفاعلي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (hasGeneratedContent) {
                                Text(
                                    if (isGeneratedFromGeminiApi) "✓ تم التوليد بنجاح عبر Gemini API" else "✓ تم التوليد وفق المنهج اليمني المعتمد",
                                    fontSize = 11.sp,
                                    color = if (isGeneratedFromGeminiApi) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Toggle View/Edit Mode Button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { isEditMode = !isEditMode },
                                modifier = Modifier.testTag("toggle_edit_mode_button")
                            ) {
                                Icon(
                                    if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEditMode) "معاينة النموذج" else "تعديل المحتوى", fontSize = 12.sp)
                            }
                        }
                    }

                    // Interactive Tab Navigation for the 4 Main Pillars
                    val tabs = listOf("الأهداف والمخرجات", "التهيئة والشرح", "التقويم والواجبات", "الفروق والتأمل")
                    TabRow(
                        selectedTabIndex = selectedPlanTab,
                        modifier = Modifier.fillMaxWidth().testTag("lesson_plan_tabs")
                    ) {
                        tabs.forEachIndexed { idx, tabTitle ->
                            Tab(
                                selected = (selectedPlanTab == idx),
                                onClick = { selectedPlanTab = idx },
                                text = { Text(tabTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // Content based on Selected Tab
                    when (selectedPlanTab) {
                        0 -> {
                            // Pillar 1: Objectives & Outcomes
                            InteractiveSectionBox(
                                title = "الأهداف المعرفية (السلوكية)",
                                value = cognitiveObjectives,
                                onValueChange = { cognitiveObjectives = it },
                                isEditMode = isEditMode,
                                placeholder = "1. أن يتعرف الطالب على المفاهيم الأساسية...\n2. أن يفسر القواعد بدقة..."
                            )

                            InteractiveSectionBox(
                                title = "الأهداف المهارية",
                                value = skillObjectives,
                                onValueChange = { skillObjectives = it },
                                isEditMode = isEditMode,
                                placeholder = "1. أن يطبق خطوات الحل...\n2. أن يتقن رسم النماذج والجداول..."
                            )

                            InteractiveSectionBox(
                                title = "الأهداف الوجدانية والقيم الوطنية",
                                value = affectiveObjectives,
                                onValueChange = { affectiveObjectives = it },
                                isEditMode = isEditMode,
                                placeholder = "1. تقدير أهمية الدرس وتطبيقاته في خدمة المجتمع اليمني...\n2. تعزيز التعاون..."
                            )

                            InteractiveSectionBox(
                                title = "نواتج التعلم المتوقعة",
                                value = expectedOutcomes,
                                onValueChange = { expectedOutcomes = it },
                                isEditMode = isEditMode,
                                placeholder = "إتقان 85% من المهارات، القدرة على حل التمارين المستقلة..."
                            )

                            InteractiveSectionBox(
                                title = "المعارف والمهارات والقيم المستهدفة",
                                value = targetKnowledgeAndSkills,
                                onValueChange = { targetKnowledgeAndSkills = it },
                                isEditMode = isEditMode,
                                placeholder = "المعارف: المفاهيم والقوانين. المهارات: التحليل والاستنتاج..."
                            )
                        }

                        1 -> {
                            // Pillar 2: Prerequisites, Intro & Sequential Teaching
                            InteractiveSectionBox(
                                title = "المتطلبات السابقة والربط بالدرس السابق",
                                value = prerequisites,
                                onValueChange = { prerequisites = it },
                                isEditMode = isEditMode,
                                placeholder = "مراجعة مكتسبات الوحدة السابقة، أسئلة استكشافية تشخيصية..."
                            )

                            InteractiveSectionBox(
                                title = "التهيئة والمقدمة الحافزة (5 دقائق)",
                                value = introduction,
                                onValueChange = { introduction = it },
                                isEditMode = isEditMode,
                                placeholder = "طرح مسألة واقعية مستمدة من البيئة اليمنية تثير الفضول العلمي..."
                            )

                            InteractiveSectionBox(
                                title = "خطوات الشرح المتسلسلة وإجراءات التنفيذ (25 دقيقة)",
                                value = teachingSteps,
                                onValueChange = { teachingSteps = it },
                                isEditMode = isEditMode,
                                minLines = 4,
                                placeholder = "1. العرض والنمذجة\n2. الممارسة الموجهة بمجموعات\n3. التطبيق الفردي والغلق..."
                            )

                            InteractiveSectionBox(
                                title = "أنشطة المعلم والمتعلم",
                                value = teacherStudentActivities,
                                onValueChange = { teacherStudentActivities = it },
                                isEditMode = isEditMode,
                                placeholder = "دور المعلم: التيسير والتوجيه، دور الطالب: المشاركة وحل التدريبات..."
                            )

                            InteractiveSectionBox(
                                title = "الأسئلة الصفية والمناقشة",
                                value = discussionQuestions,
                                onValueChange = { discussionQuestions = it },
                                isEditMode = isEditMode,
                                placeholder = "1. كيف نفسر أهمية الموضوع؟\n2. ما الفرق بين الحالتين؟..."
                            )

                            InteractiveSectionBox(
                                title = "الوسائل ومصادر التعلم",
                                value = teachingAids,
                                onValueChange = { teachingAids = it },
                                isEditMode = isEditMode,
                                placeholder = "السبورة، الكتاب المدرسي، بطاقات وأوراق عمل، مجسمات..."
                            )
                        }

                        2 -> {
                            // Pillar 3: Assessments & Homework
                            InteractiveSectionBox(
                                title = "التقويم التشخيصي (القبلي)",
                                value = diagnosticAssessment,
                                onValueChange = { diagnosticAssessment = it },
                                isEditMode = isEditMode,
                                placeholder = "سؤال قبلي للتحقق من المفاهيم الأساسية قبل الشرح..."
                            )

                            InteractiveSectionBox(
                                title = "التقويم التكويني (البنائي المستمر)",
                                value = formativeAssessment,
                                onValueChange = { formativeAssessment = it },
                                isEditMode = isEditMode,
                                placeholder = "تطبيق صفي سريع وبطاقات إشارة وملاحظة مباشرة لحل التدريبات..."
                            )

                            InteractiveSectionBox(
                                title = "التقويم الختامي (5 دقائق)",
                                value = summativeAssessment,
                                onValueChange = { summativeAssessment = it },
                                isEditMode = isEditMode,
                                placeholder = "سؤال شامل لقياس تحقق أهداف الحصة لدى جميع الطلاب..."
                            )

                            InteractiveSectionBox(
                                title = "الواجب المنزلي والتكليفات",
                                value = homework,
                                onValueChange = { homework = it },
                                isEditMode = isEditMode,
                                placeholder = "حل التمارين ص (...) من الكتاب المدرسي وتكليف استكشافي..."
                            )
                        }

                        3 -> {
                            // Pillar 4: Remedial, Enrichment, Time & Reflection
                            InteractiveSectionBox(
                                title = "الأنشطة العلاجية (للطلاب الضعاف)",
                                value = remedialActivities,
                                onValueChange = { remedialActivities = it },
                                isEditMode = isEditMode,
                                placeholder = "تبسيط المفاهيم عبر نماذج حسية، أوراق عمل مدعمة، تعليم الأقران..."
                            )

                            InteractiveSectionBox(
                                title = "الأنشطة الإثرائية (للطلاب المتفوقين)",
                                value = enrichmentActivities,
                                onValueChange = { enrichmentActivities = it },
                                isEditMode = isEditMode,
                                placeholder = "مسائل تحدٍ ذات مستويات تفكير عليا وتصميم خرائط مفاهيم..."
                            )

                            InteractiveSectionBox(
                                title = "توزيع الزمن على فقرات الحصة",
                                value = timeDistribution,
                                onValueChange = { timeDistribution = it },
                                isEditMode = isEditMode,
                                placeholder = "تهيئة: 5د، شرح وأنشطة: 30د، تقويم وغلق: 10د (إجمالي: 45د)..."
                            )

                            InteractiveSectionBox(
                                title = "ملاحظات المعلم والتأمل الذاتي بعد الحصة",
                                value = teacherNotes,
                                onValueChange = { teacherNotes = it },
                                isEditMode = isEditMode,
                                placeholder = "مدى تحقق الأهداف، النقاط التي تحتاج تعزيزاً في الحصة القادمة..."
                            )
                        }
                    }
                }
            }

            // Quick Actions: Copy, Print, Word Export
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Copy to Clipboard
                OutlinedButton(
                    onClick = {
                        val entity = buildCurrentEntity()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("تحضير درس", PrintUtils.buildLessonHtml(entity))
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ نص الخطة إلى الحافظة بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("copy_lesson_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ الخطة", fontSize = 12.sp)
                }

                // Export to Word (.doc)
                OutlinedButton(
                    onClick = {
                        val entity = buildCurrentEntity()
                        PrintUtils.exportLessonToWord(context, entity)
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("export_word_btn_bottom")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير Word", fontSize = 12.sp)
                }

                // Print PDF
                OutlinedButton(
                    onClick = {
                        val entity = buildCurrentEntity()
                        PrintUtils.printLesson(context, entity)
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("print_pdf_btn_bottom")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طباعة PDF", fontSize = 12.sp)
                }
            }

            // Save Lesson Plan to Room Database
            Button(
                onClick = {
                    val finalTitle = currentLessonTitle.ifBlank { customLessonTitle }
                    if (finalTitle.isBlank()) {
                        Toast.makeText(context, "يرجى كتابة أو اختيار عنوان الدرس", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    coroutineScope.launch {
                        val entity = buildCurrentEntity()
                        repository.insertLesson(entity)
                        isSaving = false
                        Toast.makeText(context, "تم حفظ خطة الدرس بنجاح في قاعدة بيانات التطبيق!", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_lesson_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ خطة الدرس في السجلات الرسمية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // Gemini API Key Settings Dialog
    if (showApiKeyDialog) {
        var keyInput by remember { mutableStateOf(geminiService.getCustomApiKey(context)) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ربط Gemini API للمحتوى الذكي", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "أدخل مفتاح Gemini API Key الخاص بك لتوليد خطط التحضير بدقة واستجابة فورية، أو اترك الحقل فارغاً لاستخدام المولد المنهجي المعتمد تلقائياً.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("gemini_api_key_dialog_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        geminiService.setCustomApiKey(context, keyInput.trim())
                        showApiKeyDialog = false
                        Toast.makeText(context, "تم حفظ إعدادات Gemini API بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حفظ المفتاح")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Reusable Interactive Component for Plan Sections
 * Switches smoothly between Editable TextField and Styled Clean View Mode
 */
@Composable
private fun InteractiveSectionBox(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditMode: Boolean,
    minLines: Int = 2,
    placeholder: String = ""
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )

            if (isEditMode) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    minLines = minLines,
                    placeholder = { Text(placeholder, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = value.ifBlank { "لم يتم إدخال محتوى بعد (انقر تعديل لإضافة محتوى أو استخدم التوليد الذكي)." },
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = if (value.isNotBlank()) Color(0xFF1E293B) else Color(0xFF94A3B8),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
