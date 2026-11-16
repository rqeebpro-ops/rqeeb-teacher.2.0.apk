package com.example.ui.teacher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiService
import com.example.data.model.*
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartExplanationScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1) Selection State: Grade, Subject, Unit, Lesson
    var grade by remember {
        mutableStateOf(
            if (teacher.gradeTaught.isNotBlank()) YemeniCurriculum.toDisplayGrade(teacher.gradeTaught) else "9/ إعدادي"
        )
    }
    val availableSubjects = remember(grade) { YemeniCurriculum.getSubjectsForGrade(grade) }
    var subject by remember(grade) {
        mutableStateOf(
            if (availableSubjects.contains(teacher.subject)) teacher.subject else availableSubjects.firstOrNull() ?: "العلوم"
        )
    }
    val curriculumUnits = remember(grade, subject) {
        YemeniCurriculum.getCurriculumUnits(grade, subject, YemeniCurriculum.TERMS.first())
    }
    var selectedUnitIndex by remember(curriculumUnits) { mutableIntStateOf(0) }
    val selectedUnit = curriculumUnits.getOrNull(selectedUnitIndex)
    val selectedUnitTitle = selectedUnit?.title ?: "الوحدة الأولى"
    val availableLessons = selectedUnit?.lessons ?: emptyList()

    var lessonTitle by remember(availableLessons) {
        mutableStateOf(availableLessons.firstOrNull() ?: "")
    }
    var customLessonText by remember { mutableStateOf("") }
    var isCustomTextExpanded by remember { mutableStateOf(false) }

    // Dropdown expansion states
    var gradeMenuExpanded by remember { mutableStateOf(false) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var lessonMenuExpanded by remember { mutableStateOf(false) }

    // 2) Pre-generation Controls
    var expansionLevel by remember { mutableStateOf("أعمق (مفصل وشامل)") }
    var dialectStyle by remember { mutableStateOf("لهجة بيضاء يمنية") }
    var studentLevel by remember { mutableStateOf("شامل لجميع المستويات") }
    var examplesCount by remember { mutableIntStateOf(5) }
    val featureOptions = listOf("تشبيهات يمنية", "تجارب وأنشطة", "أسئلة تفاعلية", "سيناريو إلقاء", "تخطيط السبورة")
    val selectedFeatures = remember { mutableStateListOf("تشبيهات يمنية", "تجارب وأنشطة", "أسئلة تفاعلية", "سيناريو إلقاء", "تخطيط السبورة") }

    // 3) Result & Generation State
    var isGenerating by remember { mutableStateOf(false) }
    var explanationResult by remember { mutableStateOf<SmartExplanationResult?>(null) }
    var showSavedDrawer by remember { mutableStateOf(false) }

    // Interactive Action State (Bottom Sheet / Dialog)
    var activeRefiningConcept by remember { mutableStateOf<ConceptExplanation?>(null) }
    var activeActionTitle by remember { mutableStateOf("") }
    var isRefining by remember { mutableStateOf(false) }
    var refinedTextResult by remember { mutableStateOf<String?>(null) }

    // Accordion Expand / Collapse states (12 sections)
    val expandedSections = remember {
        mutableStateMapOf(
            1 to true,   // Concepts (Deep)
            2 to true,   // Analogies
            3 to true,   // Yemeni Examples
            4 to false,  // Delivery 10 steps
            5 to false,  // Teaching Methods
            6 to true,   // Remediation
            7 to false,  // Questions
            8 to false,  // Graded Examples
            9 to false,  // Blackboard
            10 to false, // Script
            11 to false, // Teacher Enrichment
            12 to true   // Save & Export
        )
    }

    val savedExplanations by repository.getSmartExplanationsFlow(teacher.id).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "الشرح الذكي (المساعد التربوي)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "شرح عميق وأمثلة يمنية وتشبيهات وطرق تدريس",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("smart_explanation_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Open Saved History
                    IconButton(
                        onClick = { showSavedDrawer = true },
                        modifier = Modifier.testTag("smart_explanation_saved_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (savedExplanations.isNotEmpty()) {
                                    Badge { Text("${savedExplanations.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "الشروحات المحفوظة", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (explanationResult != null) {
                        IconButton(
                            onClick = {
                                PrintUtils.exportSmartExplanationToWord(context, explanationResult!!)
                            },
                            modifier = Modifier.testTag("export_smart_explanation_word")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "تصدير Word", tint = Color(0xFF2563EB))
                        }

                        IconButton(
                            onClick = {
                                PrintUtils.printSmartExplanation(context, explanationResult!!)
                            },
                            modifier = Modifier.testTag("print_smart_explanation")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "طباعة / PDF", tint = Color(0xFF006874))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Teacher Context Info Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF7C3AED).copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "المساعد التربوي التفاعلي للمعلم اليمني",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "تحليل عميق لكل مفهوم، تشبيهات قريبة للطالب، خطوات تدريسية عملية وسيناريو إلقاء",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Lesson Setup Form Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "1. تحديد بيانات الدرس من المنهج اليمني",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Grade and Subject Pickers
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Grade
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = grade,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الصف الدراسي", fontSize = 11.sp) },
                                    trailingIcon = {
                                        IconButton(onClick = { gradeMenuExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { gradeMenuExpanded = true }
                                )
                                DropdownMenu(expanded = gradeMenuExpanded, onDismissRequest = { gradeMenuExpanded = false }) {
                                    for (g in YemeniCurriculum.DISPLAY_GRADES) {
                                        DropdownMenuItem(
                                            text = { Text(g) },
                                            onClick = {
                                                grade = g
                                                gradeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Subject
                            Box(modifier = Modifier.weight(1.2f)) {
                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("المادة", fontSize = 11.sp) },
                                    trailingIcon = {
                                        IconButton(onClick = { subjectMenuExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { subjectMenuExpanded = true }
                                )
                                DropdownMenu(expanded = subjectMenuExpanded, onDismissRequest = { subjectMenuExpanded = false }) {
                                    for (s in availableSubjects) {
                                        DropdownMenuItem(
                                            text = { Text(s) },
                                            onClick = {
                                                subject = s
                                                subjectMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Unit and Lesson Pickers
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Unit
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = selectedUnitTitle,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الوحدة التعليمية", fontSize = 11.sp) },
                                    trailingIcon = {
                                        IconButton(onClick = { unitMenuExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { unitMenuExpanded = true }
                                )
                                DropdownMenu(expanded = unitMenuExpanded, onDismissRequest = { unitMenuExpanded = false }) {
                                    curriculumUnits.forEachIndexed { idx, u ->
                                        DropdownMenuItem(
                                            text = { Text(u.title) },
                                            onClick = {
                                                selectedUnitIndex = idx
                                                unitMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Lesson Title (editable + dropdown)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = lessonTitle,
                                onValueChange = { lessonTitle = it },
                                label = { Text("عنوان الدرس *", fontSize = 12.sp) },
                                placeholder = { Text("اختر من القائمة أو اكتب عنوان الدرس يدوياً") },
                                trailingIcon = {
                                    if (availableLessons.isNotEmpty()) {
                                        IconButton(onClick = { lessonMenuExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("smart_explanation_title_input")
                            )
                            if (availableLessons.isNotEmpty()) {
                                DropdownMenu(expanded = lessonMenuExpanded, onDismissRequest = { lessonMenuExpanded = false }) {
                                    for (l in availableLessons) {
                                        DropdownMenuItem(
                                            text = { Text(l) },
                                            onClick = {
                                                lessonTitle = l
                                                lessonMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Expandable Lesson Text/Points Input
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCustomTextExpanded = !isCustomTextExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isCustomTextExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "إدخال نص الدرس أو محاور خاصة للمعلم (اختياري)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = isCustomTextExpanded) {
                            OutlinedTextField(
                                value = customLessonText,
                                onValueChange = { customLessonText = it },
                                label = { Text("نص الدرس أو النقاط المطلوب التوسع فيها") },
                                placeholder = { Text("الصق هنا فقرات من الكتاب أو نقاط معينة ترغب في شرحها بعمق مع أمثلة...") },
                                minLines = 3,
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Pedagogical Configuration Controls Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "2. تخصيص مستوى الشرح واللهجة والبيئة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Expansion Level
                        Text("مستوى التوسع والعمق:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("عميق (موجز وواضح)", "أعمق (مفصل وشامل)", "موسع جداً").forEach { lvl ->
                                FilterChip(
                                    selected = expansionLevel == lvl,
                                    onClick = { expansionLevel = lvl },
                                    label = { Text(lvl, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Dialect Style for Analogies
                        Text("أسلوب التشبيهات («قرّب المعلومة للطالب»):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("لهجة بيضاء يمنية", "لهجة صنعانية", "فصحى مبسطة").forEach { d ->
                                FilterChip(
                                    selected = dialectStyle == d,
                                    onClick = { dialectStyle = d },
                                    label = { Text(d, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Student Level & Examples Count
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text("مستوى الطلاب:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                var stMenuExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = studentLevel,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { stMenuExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().clickable { stMenuExpanded = true }
                                    )
                                    DropdownMenu(expanded = stMenuExpanded, onDismissRequest = { stMenuExpanded = false }) {
                                        listOf("شامل لجميع المستويات", "مبتدئ ومتعثر", "متوسط", "متقدم ومتفوق").forEach { sl ->
                                            DropdownMenuItem(
                                                text = { Text(sl) },
                                                onClick = {
                                                    studentLevel = sl
                                                    stMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(0.9f)) {
                                Text("الأمثلة:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(3, 5, 7).forEach { num ->
                                        FilterChip(
                                            selected = examplesCount == num,
                                            onClick = { examplesCount = num },
                                            label = { Text("$num", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Checkbox Toggles for Features
                        Text("عناصر الشرح الإضافية:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            featureOptions.forEach { opt ->
                                val isSelected = selectedFeatures.contains(opt)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedFeatures.remove(opt) else selectedFeatures.add(opt)
                                    },
                                    label = { Text(opt, fontSize = 11.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Generate Button
                        Button(
                            onClick = {
                                if (lessonTitle.isBlank()) {
                                    Toast.makeText(context, "يرجى تحديد أو كتابة عنوان الدرس أولاً!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isGenerating = true
                                coroutineScope.launch {
                                    val res = geminiService.generateSmartExplanation(
                                        subject = subject,
                                        grade = grade,
                                        unitName = selectedUnitTitle,
                                        lessonTitle = lessonTitle,
                                        lessonContentText = customLessonText,
                                        expansionLevel = expansionLevel,
                                        dialectStyle = dialectStyle,
                                        studentLevel = studentLevel,
                                        examplesCount = examplesCount,
                                        selectedFeatures = selectedFeatures.toList(),
                                        context = context
                                    )
                                    explanationResult = res
                                    isGenerating = false
                                    Toast.makeText(context, "تم إعداد الشرح الذكي والدليل التربوي بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_smart_explanation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري تحليل المفاهيم وتوليد الشرح العميق والأمثلة...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("توليد الشرح الذكي والدليل التربوي المتكامل", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Results Accordions (12 Sections)
            if (explanationResult != null) {
                val res = explanationResult!!

                // Status & Controls Bar
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (res.isFromGeminiApi) Color(0xFFEDE9FE) else Color(0xFFE0F2F1)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (res.isFromGeminiApi) Icons.Default.CloudDone else Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = if (res.isFromGeminiApi) Color(0xFF7C3AED) else Color(0xFF006874),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (res.isFromGeminiApi) "تم التوليد بنجاح عبر Gemini AI المتقدم" else "دليل تربوي معتمد وفق معايير المنهج اليمني",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (res.isFromGeminiApi) Color(0xFF5B21B6) else Color(0xFF004D40)
                                    )
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val entity = SmartExplanationEntity(
                                                teacherId = teacher.id,
                                                subject = res.subject,
                                                grade = res.grade,
                                                unitName = selectedUnitTitle,
                                                lessonTitle = res.lessonTitle,
                                                expansionLevel = res.expansionLevel,
                                                dialectStyle = res.dialectStyle,
                                                studentLevel = studentLevel,
                                                rawJson = serializeResultToJson(res),
                                                summary = "${res.concepts.size} مفاهيم • ${res.yemeniAnalogies.size} تشبيهات • ${res.yemeniExamples.size} أمثلة"
                                            )
                                            repository.insertSmartExplanation(entity)
                                            Toast.makeText(context, "تم حفظ الشرح في السجل المحلي بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("save_smart_explanation_db")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حفظ بالسجل", fontSize = 11.sp)
                                }
                            }

                            // Expand All / Collapse All controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        (1..12).forEach { expandedSections[it] = true }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("توسيع الكل", fontSize = 11.sp)
                                }
                                Text(" • ", color = MaterialTheme.colorScheme.outline)
                                TextButton(
                                    onClick = {
                                        (1..12).forEach { expandedSections[it] = false }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("طي الكل", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // SECTION 1: Deep Concept Explanations + Interactive Command Chips
                item {
                    AccordionCard(
                        sectionNumber = 1,
                        title = "🔬 الشرح العميق وتفكيك المفاهيم (${res.concepts.size} مفاهيم)",
                        subtitle = "توضيح كيف ولماذا تحدث الظاهرة، وتصويب المفاهيم الخاطئة، مع أزرار التفاعل المباشر",
                        isExpanded = expandedSections[1] ?: true,
                        onToggle = { expandedSections[1] = !(expandedSections[1] ?: true) },
                        accentColor = Color(0xFF006874)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            res.concepts.forEachIndexed { idx, c ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "المفهوم ${idx + 1}: ${c.title}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF006874)
                                            )
                                            IconButton(
                                                onClick = {
                                                    copyText(context, "${c.title}\n${c.deepExplanation}\n${c.whyAndHow}")
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(15.dp))
                                            }
                                        }

                                        Text(c.deepExplanation, fontSize = 12.sp, lineHeight = 17.sp)

                                        if (c.whyAndHow.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF1F5F9),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text("لماذا وكيف تحدث؟", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF334155))
                                                    Text(c.whyAndHow, fontSize = 11.sp, color = Color(0xFF475569))
                                                }
                                            }
                                        }

                                        if (c.causeAndEffect.isNotBlank()) {
                                            Text("الأسباب والنتائج: ${c.causeAndEffect}", fontSize = 11.sp, color = Color(0xFF64748B))
                                        }

                                        if (c.commonMisconception.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEE2E2),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Text("المفهوم الخاطئ الشائع لدى الطلاب:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF991B1B))
                                                        Text(c.commonMisconception, fontSize = 11.sp, color = Color(0xFFB91C1C))
                                                    }
                                                }
                                            }
                                        }

                                        if (c.studentQuestion.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDCFCE7),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text("❓ سؤال الطالب المتوقع: ${c.studentQuestion}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF166534))
                                                    Text("💡 إجابة المعلم المقنعة: ${c.studentQuestionAnswer}", fontSize = 11.sp, color = Color(0xFF14532D))
                                                }
                                            }
                                        }

                                        // Interactive Action Chips for this concept
                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                        Text("أوامر تفاعلية فورية لهذا المفهوم:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ActionChip("⚡ وسّع أكثر") {
                                                activeRefiningConcept = c
                                                activeActionTitle = "توسيع علمي عميق: ${c.title}"
                                                isRefining = true
                                                coroutineScope.launch {
                                                    val expanded = geminiService.refineExplanationConcept(
                                                        subject = res.subject,
                                                        grade = res.grade,
                                                        lessonTitle = res.lessonTitle,
                                                        conceptTitle = c.title,
                                                        currentExplanation = c.deepExplanation,
                                                        actionType = "EXPAND_MORE",
                                                        dialectStyle = res.dialectStyle,
                                                        context = context
                                                    )
                                                    refinedTextResult = expanded
                                                    isRefining = false
                                                }
                                            }
                                            ActionChip("🐣 اشرحها بأبسط") {
                                                activeRefiningConcept = c
                                                activeActionTitle = "شرح مبسط للأطفال: ${c.title}"
                                                isRefining = true
                                                coroutineScope.launch {
                                                    val simplified = geminiService.refineExplanationConcept(
                                                        subject = res.subject,
                                                        grade = res.grade,
                                                        lessonTitle = res.lessonTitle,
                                                        conceptTitle = c.title,
                                                        currentExplanation = c.deepExplanation,
                                                        actionType = "SIMPLIFY",
                                                        dialectStyle = res.dialectStyle,
                                                        context = context
                                                    )
                                                    refinedTextResult = simplified
                                                    isRefining = false
                                                }
                                            }
                                            ActionChip("🇾🇪 تشبيه يمني آخر") {
                                                activeRefiningConcept = c
                                                activeActionTitle = "تشبيهات يمنية جديدة: ${c.title}"
                                                isRefining = true
                                                coroutineScope.launch {
                                                    val anlg = geminiService.refineExplanationConcept(
                                                        subject = res.subject,
                                                        grade = res.grade,
                                                        lessonTitle = res.lessonTitle,
                                                        conceptTitle = c.title,
                                                        currentExplanation = c.deepExplanation,
                                                        actionType = "YEMENI_ANALOGY",
                                                        dialectStyle = res.dialectStyle,
                                                        context = context
                                                    )
                                                    refinedTextResult = anlg
                                                    isRefining = false
                                                }
                                            }
                                            ActionChip("📝 أمثلة إضافية") {
                                                activeRefiningConcept = c
                                                activeActionTitle = "مسائل وأمثلة إضافية محلولة: ${c.title}"
                                                isRefining = true
                                                coroutineScope.launch {
                                                    val exs = geminiService.refineExplanationConcept(
                                                        subject = res.subject,
                                                        grade = res.grade,
                                                        lessonTitle = res.lessonTitle,
                                                        conceptTitle = c.title,
                                                        currentExplanation = c.deepExplanation,
                                                        actionType = "MORE_EXAMPLES",
                                                        dialectStyle = res.dialectStyle,
                                                        context = context
                                                    )
                                                    refinedTextResult = exs
                                                    isRefining = false
                                                }
                                            }
                                            ActionChip("🤝 لطالب ضعيف") {
                                                activeRefiningConcept = c
                                                activeActionTitle = "معالجة للطالب المتعثر: ${c.title}"
                                                isRefining = true
                                                coroutineScope.launch {
                                                    val rem = geminiService.refineExplanationConcept(
                                                        subject = res.subject,
                                                        grade = res.grade,
                                                        lessonTitle = res.lessonTitle,
                                                        conceptTitle = c.title,
                                                        currentExplanation = c.deepExplanation,
                                                        actionType = "WEAK_STUDENT",
                                                        dialectStyle = res.dialectStyle,
                                                        context = context
                                                    )
                                                    refinedTextResult = rem
                                                    isRefining = false
                                                }
                                            }
                                            ActionChip("🧪 تجربة صفية") {
                                                activeRefiningConcept = c
                                                activeActionTitle = "تجربة عملية بأدوات متوفرة: ${c.title}"
                                                isRefining = true
                                                coroutineScope.launch {
                                                    val exp = geminiService.refineExplanationConcept(
                                                        subject = res.subject,
                                                        grade = res.grade,
                                                        lessonTitle = res.lessonTitle,
                                                        conceptTitle = c.title,
                                                        currentExplanation = c.deepExplanation,
                                                        actionType = "PRACTICAL_EXPERIMENT",
                                                        dialectStyle = res.dialectStyle,
                                                        context = context
                                                    )
                                                    refinedTextResult = exp
                                                    isRefining = false
                                                }
                                            }
                                            ActionChip("🗣️ ما أقوله حرفياً") {
                                                activeRefiningConcept = c
                                                activeActionTitle = "العبارة التي يقولها المعلم حرفياً: ${c.title}"
                                                isRefining = true
                                                coroutineScope.launch {
                                                    val script = geminiService.refineExplanationConcept(
                                                        subject = res.subject,
                                                        grade = res.grade,
                                                        lessonTitle = res.lessonTitle,
                                                        conceptTitle = c.title,
                                                        currentExplanation = c.deepExplanation,
                                                        actionType = "VERBATIM_SCRIPT",
                                                        dialectStyle = res.dialectStyle,
                                                        context = context
                                                    )
                                                    refinedTextResult = script
                                                    isRefining = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: Yemeni Dialect Analogies («قرّب المعلومة للطالب»)
                item {
                    AccordionCard(
                        sectionNumber = 2,
                        title = "🇾🇪 التشبيهات باللهجة اليمنية («قرّب المعلومة للطالب»)",
                        subtitle = "تشبيهات حسية من بيئة الطالب لنقل المفهوم من التجريد للواقع",
                        isExpanded = expandedSections[2] ?: true,
                        onToggle = { expandedSections[2] = !(expandedSections[2] ?: true) },
                        accentColor = Color(0xFFD97706)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            res.yemeniAnalogies.forEach { a ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(a.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                                        Text("« ${a.analogyText} »", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF78350F), lineHeight = 18.sp)
                                        if (a.spokenDialect.isNotBlank()) {
                                            Text("العبارة المنطوقة: ${a.spokenDialect}", fontSize = 11.sp, color = Color(0xFFB45309))
                                        }
                                        Text("كيف تربطها علمياً؟ ${a.explanationLink}", fontSize = 11.sp, color = Color(0xFF475569))
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 3: Real Yemeni Environmental Examples
                item {
                    AccordionCard(
                        sectionNumber = 3,
                        title = "🌾 أمثلة مستمدة من واقع البيئة اليمنية المعاشة",
                        subtitle = "الأسواق، مزارع البن، الطاقة الشمسية، خزانات المياه، ومدرجات الجبال",
                        isExpanded = expandedSections[3] ?: true,
                        onToggle = { expandedSections[3] = !(expandedSections[3] ?: true) },
                        accentColor = Color(0xFF059669)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            res.yemeniExamples.forEach { e ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFECFDF5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(e.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF065F46))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFD1FAE5)
                                            ) {
                                                Text(e.contextType, fontSize = 10.sp, color = Color(0xFF047857), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text(e.description, fontSize = 12.sp, color = Color(0xFF1E293B), lineHeight = 17.sp)
                                        Text("طريقة التوظيف الصفي: ${e.howToApply}", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 4: 10-Step Pedagogical Delivery Guide
                item {
                    AccordionCard(
                        sectionNumber = 4,
                        title = "🔟 دليل إلقاء الفكرة (10 خطوات عملية للمعلم)",
                        subtitle = "من المدخل والتهيئة حتى معالجة التعثر والتحقق من الفهم",
                        isExpanded = expandedSections[4] ?: false,
                        onToggle = { expandedSections[4] = !(expandedSections[4] ?: false) },
                        accentColor = Color(0xFF2563EB)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            res.deliveryGuide.forEach { step ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF2563EB),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${step.stepNumber}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(step.stepTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                                            Text(step.stepDetail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            if (step.teacherScript.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("🗣️ ما يقوله المعلم: « ${step.teacherScript} »", fontSize = 11.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 5: Classroom Teaching Strategies & Low-Cost Experiments
                item {
                    AccordionCard(
                        sectionNumber = 5,
                        title = "🎯 استراتيجيات التدريس الصفية وتجارب بدون تكلفة",
                        subtitle = "الحوار، الاكتشاف، لعب الأدوار، وتجارب صفية بأدوات متاحة",
                        isExpanded = expandedSections[5] ?: false,
                        onToggle = { expandedSections[5] = !(expandedSections[5] ?: false) },
                        accentColor = Color(0xFF7C3AED)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            res.teachingMethods.forEach { m ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF5F3FF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("✦ ${m.strategyName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF5B21B6))
                                        Text("إجراءات التنفيذ الصفي: ${m.executionSteps}", fontSize = 11.sp, color = Color(0xFF374151))
                                        Text("الأثر التعليمي: ${m.expectedOutcome}", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 6: Remediation for Learning Difficulties («إذا لم يفهم الطالب»)
                item {
                    AccordionCard(
                        sectionNumber = 6,
                        title = "🤝 علاج تعثر الفهم («إذا لم يفهم الطالب»)",
                        subtitle = "صياغات بديلة، وسائل توضيح حسية، وأسئلة تدرج ذكية",
                        isExpanded = expandedSections[6] ?: true,
                        onToggle = { expandedSections[6] = !(expandedSections[6] ?: true) },
                        accentColor = Color(0xFFEA580C)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            res.remediation.forEach { r ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF7ED),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("نقطة الصعوبة: ${r.difficulty}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC2410C))
                                        Text("الصياغة البديلة الأبسط: ${r.simplerExplanation}", fontSize = 11.sp, color = Color(0xFF9A3412))
                                        Text("توضيح محسوس بأدوات متوفرة: ${r.visualOrPhysicalDemonstration}", fontSize = 11.sp, color = Color(0xFF475569))
                                        if (r.scaffoldQuestions.isNotEmpty()) {
                                            Text("أسئلة التدرج لاستدراج الطالب:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                                            r.scaffoldQuestions.forEachIndexed { qIdx, q ->
                                                Text("  ${qIdx + 1}. $q", fontSize = 11.sp, color = Color(0xFF7C2D12))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 7: Interactive Questions with Teacher Feedback
                item {
                    AccordionCard(
                        sectionNumber = 7,
                        title = "❓ أسئلة صفية مثيرة للتفكير مع إدارتها تربوياً",
                        subtitle = "السؤال، الإجابة المتوقعة، كيف تتصرف إذا أخطأ الطالب، والسؤال التالي",
                        isExpanded = expandedSections[7] ?: false,
                        onToggle = { expandedSections[7] = !(expandedSections[7] ?: false) },
                        accentColor = Color(0xFF0284C7)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            res.interactiveQuestions.forEachIndexed { qIdx, q ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("س ${qIdx + 1}: ${q.question}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0369A1))
                                        Text("الإجابة النموذجية المتوقعة: ${q.expectedAnswer}", fontSize = 11.sp, color = Color(0xFF047857))
                                        Text("توجيه المعلم في حال الخطأ: ${q.teacherFeedbackIfWrong}", fontSize = 11.sp, color = Color(0xFFB91C1C))
                                        if (q.followUpQuestion.isNotBlank()) {
                                            Text("السؤال لتعميق الفهم: ${q.followUpQuestion}", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 8: Graded Solved Examples
                item {
                    AccordionCard(
                        sectionNumber = 8,
                        title = "📊 مسائل وأمثلة متدرجة محلولة بالكامل (${res.gradedExamples.size} أمثلة)",
                        subtitle = "مباشر، بيئي يمني، نشاط صفي، متوسط، ومتقدم وتحدي للمتميزين",
                        isExpanded = expandedSections[8] ?: false,
                        onToggle = { expandedSections[8] = !(expandedSections[8] ?: false) },
                        accentColor = Color(0xFF0D9488)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            res.gradedExamples.forEach { g ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFCCFBF1)
                                        ) {
                                            Text(g.level, fontSize = 10.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                        Text(g.problem, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF8FAFC),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(g.stepByStepSolution, fontSize = 11.sp, modifier = Modifier.padding(8.dp), lineHeight = 16.sp)
                                        }
                                        if (g.pedagogicalTip.isNotBlank()) {
                                            Text("💡 توجيه تربوي للمعلم: ${g.pedagogicalTip}", fontSize = 10.sp, color = Color(0xFF0284C7))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 9: Blackboard Planning Scheme
                item {
                    AccordionCard(
                        sectionNumber = 9,
                        title = "📋 تخطيط وتنظيم السبورة الصفية",
                        subtitle = "تقسيم السبورة: يمين (المفاهيم)، منتصف (الشرح والرسم)، يسار (الأمثلة والواجب)",
                        isExpanded = expandedSections[9] ?: false,
                        onToggle = { expandedSections[9] = !(expandedSections[9] ?: false) },
                        accentColor = Color(0xFF475569)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                res.blackboardPlan,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // SECTION 10: Full 45-Minute Lesson Script
                item {
                    AccordionCard(
                        sectionNumber = 10,
                        title = "⏱️ سيناريو إلقاء الحصة كاملاً (45 دقيقة)",
                        subtitle = "التوزيع الدقيق لدقائق الحصة وما يقوله ويفعله المعلم خطوة بخطوة",
                        isExpanded = expandedSections[10] ?: false,
                        onToggle = { expandedSections[10] = !(expandedSections[10] ?: false) },
                        accentColor = Color(0xFF16A34A)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                res.classroomScript,
                                fontSize = 11.sp,
                                color = Color(0xFF14532D),
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // SECTION 11: Teacher Enrichment & Historical Context
                item {
                    AccordionCard(
                        sectionNumber = 11,
                        title = "💡 معلومات إثرائية لتعميق تمكن المعلم وثقافته",
                        subtitle = "جذور تاريخية، تطبيقات معاصرة، وتوجيهات الموجه التربوي",
                        isExpanded = expandedSections[11] ?: false,
                        onToggle = { expandedSections[11] = !(expandedSections[11] ?: false) },
                        accentColor = Color(0xFF8B5CF6)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAF5FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                res.teacherEnrichment,
                                fontSize = 11.sp,
                                color = Color(0xFF4C1D95),
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // SECTION 12: Export, Print, and Sharing Tools
                item {
                    AccordionCard(
                        sectionNumber = 12,
                        title = "📤 أدوات الحفظ والتصدير والطباعة",
                        subtitle = "نسخ، تصدير مستند Word مائل للطباعة، أو طباعة مباشرة عبر نظام أندرويد",
                        isExpanded = expandedSections[12] ?: true,
                        onToggle = { expandedSections[12] = !(expandedSections[12] ?: true) },
                        accentColor = Color(0xFF006874)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val fullText = buildFullShareText(res)
                                    copyText(context, fullText)
                                },
                                modifier = Modifier.weight(1f).testTag("copy_full_explanation_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ النص كاملاً", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    PrintUtils.exportSmartExplanationToWord(context, res)
                                },
                                modifier = Modifier.weight(1f).testTag("export_word_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير Word", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    PrintUtils.printSmartExplanation(context, res)
                                },
                                modifier = Modifier.weight(1f).testTag("print_pdf_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006874))
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("طباعة / PDF", fontSize = 11.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Interactive Action Result Modal (for concept commands)
    if (activeRefiningConcept != null) {
        val c = activeRefiningConcept!!
        AlertDialog(
            onDismissRequest = {
                activeRefiningConcept = null
                refinedTextResult = null
                isRefining = false
            },
            icon = {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(28.dp))
            },
            title = {
                Text(activeActionTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRefining) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7C3AED))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("جاري استحضار المعالجة التربوية المخصصة...", fontSize = 11.sp)
                        }
                    } else if (refinedTextResult != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                refinedTextResult!!,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 18.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (refinedTextResult != null) {
                    Button(
                        onClick = {
                            copyText(context, refinedTextResult!!)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ المعالجة")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        activeRefiningConcept = null
                        refinedTextResult = null
                        isRefining = false
                    }
                ) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Saved Explanations Drawer/Dialog
    if (showSavedDrawer) {
        AlertDialog(
            onDismissRequest = { showSavedDrawer = false },
            icon = {
                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("سجل الشروحات المحفوظة (${savedExplanations.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                if (savedExplanations.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد شروحات محفوظة بعد.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("عند توليد أي شرح يمكنك الضغط على 'حفظ بالسجل' للرجوع إليه أوفلاين في أي وقت.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedExplanations) { item ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val loaded = deserializeJsonToResult(item.rawJson)
                                        if (loaded != null) {
                                            explanationResult = loaded
                                            lessonTitle = item.lessonTitle
                                            subject = item.subject
                                            grade = item.grade
                                            showSavedDrawer = false
                                            Toast.makeText(context, "تم استرجاع شرح: ${item.lessonTitle}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.lessonTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("${item.subject} • ${item.grade}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(item.summary, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteSmartExplanation(item)
                                                Toast.makeText(context, "تم حذف الشرح من السجل", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedDrawer = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun AccordionCard(
    sectionNumber: Int,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("accordion_section_$sectionNumber")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$sectionNumber", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accentColor)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = accentColor
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Divider(modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    content()
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("المعلم التربوي الذكي - الشرح الذكي", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "تم النسخ إلى الحافظة!", Toast.LENGTH_SHORT).show()
}

private fun serializeResultToJson(res: SmartExplanationResult): String {
    val obj = JSONObject()
    obj.put("subject", res.subject)
    obj.put("grade", res.grade)
    obj.put("lessonTitle", res.lessonTitle)
    obj.put("expansionLevel", res.expansionLevel)
    obj.put("dialectStyle", res.dialectStyle)
    obj.put("blackboardPlan", res.blackboardPlan)
    obj.put("classroomScript", res.classroomScript)
    obj.put("teacherEnrichment", res.teacherEnrichment)

    val conceptsArr = org.json.JSONArray()
    res.concepts.forEach { c ->
        val cObj = JSONObject()
        cObj.put("title", c.title)
        cObj.put("deepExplanation", c.deepExplanation)
        cObj.put("whyAndHow", c.whyAndHow)
        cObj.put("causeAndEffect", c.causeAndEffect)
        cObj.put("commonMisconception", c.commonMisconception)
        cObj.put("studentQuestion", c.studentQuestion)
        cObj.put("studentQuestionAnswer", c.studentQuestionAnswer)
        conceptsArr.put(cObj)
    }
    obj.put("concepts", conceptsArr)

    val anlgArr = org.json.JSONArray()
    res.yemeniAnalogies.forEach { a ->
        val aObj = JSONObject()
        aObj.put("title", a.title)
        aObj.put("analogyText", a.analogyText)
        aObj.put("spokenDialect", a.spokenDialect)
        aObj.put("explanationLink", a.explanationLink)
        anlgArr.put(aObj)
    }
    obj.put("yemeniAnalogies", anlgArr)

    val exArr = org.json.JSONArray()
    res.yemeniExamples.forEach { e ->
        val eObj = JSONObject()
        eObj.put("title", e.title)
        eObj.put("contextType", e.contextType)
        eObj.put("description", e.description)
        eObj.put("howToApply", e.howToApply)
        exArr.put(eObj)
    }
    obj.put("yemeniExamples", exArr)

    val delArr = org.json.JSONArray()
    res.deliveryGuide.forEach { d ->
        val dObj = JSONObject()
        dObj.put("stepNumber", d.stepNumber)
        dObj.put("stepTitle", d.stepTitle)
        dObj.put("stepDetail", d.stepDetail)
        dObj.put("teacherScript", d.teacherScript)
        delArr.put(dObj)
    }
    obj.put("deliveryGuide", delArr)

    val metArr = org.json.JSONArray()
    res.teachingMethods.forEach { m ->
        val mObj = JSONObject()
        mObj.put("strategyName", m.strategyName)
        mObj.put("executionSteps", m.executionSteps)
        mObj.put("expectedOutcome", m.expectedOutcome)
        metArr.put(mObj)
    }
    obj.put("teachingMethods", metArr)

    val remArr = org.json.JSONArray()
    res.remediation.forEach { r ->
        val rObj = JSONObject()
        rObj.put("difficulty", r.difficulty)
        rObj.put("simplerExplanation", r.simplerExplanation)
        rObj.put("visualOrPhysicalDemonstration", r.visualOrPhysicalDemonstration)
        val qArr = org.json.JSONArray()
        r.scaffoldQuestions.forEach { qArr.put(it) }
        rObj.put("scaffoldQuestions", qArr)
        remArr.put(rObj)
    }
    obj.put("remediation", remArr)

    val qArr = org.json.JSONArray()
    res.interactiveQuestions.forEach { q ->
        val qObj = JSONObject()
        qObj.put("question", q.question)
        qObj.put("expectedAnswer", q.expectedAnswer)
        qObj.put("teacherFeedbackIfWrong", q.teacherFeedbackIfWrong)
        qObj.put("followUpQuestion", q.followUpQuestion)
        qArr.put(qObj)
    }
    obj.put("interactiveQuestions", qArr)

    val grArr = org.json.JSONArray()
    res.gradedExamples.forEach { g ->
        val gObj = JSONObject()
        gObj.put("level", g.level)
        gObj.put("problem", g.problem)
        gObj.put("stepByStepSolution", g.stepByStepSolution)
        gObj.put("pedagogicalTip", g.pedagogicalTip)
        grArr.put(gObj)
    }
    obj.put("gradedExamples", grArr)

    return obj.toString()
}

private fun deserializeJsonToResult(json: String): SmartExplanationResult? {
    return try {
        val obj = JSONObject(json)
        val concepts = mutableListOf<ConceptExplanation>()
        val cArr = obj.optJSONArray("concepts")
        if (cArr != null) {
            for (i in 0 until cArr.length()) {
                val c = cArr.getJSONObject(i)
                concepts.add(
                    ConceptExplanation(
                        title = c.optString("title"),
                        deepExplanation = c.optString("deepExplanation"),
                        whyAndHow = c.optString("whyAndHow"),
                        causeAndEffect = c.optString("causeAndEffect"),
                        commonMisconception = c.optString("commonMisconception"),
                        studentQuestion = c.optString("studentQuestion"),
                        studentQuestionAnswer = c.optString("studentQuestionAnswer")
                    )
                )
            }
        }

        val analogies = mutableListOf<YemeniAnalogy>()
        val aArr = obj.optJSONArray("yemeniAnalogies")
        if (aArr != null) {
            for (i in 0 until aArr.length()) {
                val a = aArr.getJSONObject(i)
                analogies.add(
                    YemeniAnalogy(
                        title = a.optString("title"),
                        analogyText = a.optString("analogyText"),
                        spokenDialect = a.optString("spokenDialect"),
                        explanationLink = a.optString("explanationLink")
                    )
                )
            }
        }

        val examples = mutableListOf<YemeniExample>()
        val eArr = obj.optJSONArray("yemeniExamples")
        if (eArr != null) {
            for (i in 0 until eArr.length()) {
                val e = eArr.getJSONObject(i)
                examples.add(
                    YemeniExample(
                        title = e.optString("title"),
                        contextType = e.optString("contextType"),
                        description = e.optString("description"),
                        howToApply = e.optString("howToApply")
                    )
                )
            }
        }

        val delivery = mutableListOf<DeliveryStep>()
        val dArr = obj.optJSONArray("deliveryGuide")
        if (dArr != null) {
            for (i in 0 until dArr.length()) {
                val d = dArr.getJSONObject(i)
                delivery.add(
                    DeliveryStep(
                        stepNumber = d.optInt("stepNumber", i + 1),
                        stepTitle = d.optString("stepTitle"),
                        stepDetail = d.optString("stepDetail"),
                        teacherScript = d.optString("teacherScript")
                    )
                )
            }
        }

        val methods = mutableListOf<TeachingMethodGuide>()
        val mArr = obj.optJSONArray("teachingMethods")
        if (mArr != null) {
            for (i in 0 until mArr.length()) {
                val m = mArr.getJSONObject(i)
                methods.add(
                    TeachingMethodGuide(
                        strategyName = m.optString("strategyName"),
                        executionSteps = m.optString("executionSteps"),
                        expectedOutcome = m.optString("expectedOutcome")
                    )
                )
            }
        }

        val remediation = mutableListOf<RemediationItem>()
        val rArr = obj.optJSONArray("remediation")
        if (rArr != null) {
            for (i in 0 until rArr.length()) {
                val r = rArr.getJSONObject(i)
                val qList = mutableListOf<String>()
                val sqArr = r.optJSONArray("scaffoldQuestions")
                if (sqArr != null) {
                    for (q in 0 until sqArr.length()) qList.add(sqArr.getString(q))
                }
                remediation.add(
                    RemediationItem(
                        difficulty = r.optString("difficulty"),
                        simplerExplanation = r.optString("simplerExplanation"),
                        visualOrPhysicalDemonstration = r.optString("visualOrPhysicalDemonstration"),
                        scaffoldQuestions = qList
                    )
                )
            }
        }

        val questions = mutableListOf<InteractiveQuestion>()
        val qArr = obj.optJSONArray("interactiveQuestions")
        if (qArr != null) {
            for (i in 0 until qArr.length()) {
                val q = qArr.getJSONObject(i)
                questions.add(
                    InteractiveQuestion(
                        question = q.optString("question"),
                        expectedAnswer = q.optString("expectedAnswer"),
                        teacherFeedbackIfWrong = q.optString("teacherFeedbackIfWrong"),
                        followUpQuestion = q.optString("followUpQuestion")
                    )
                )
            }
        }

        val graded = mutableListOf<GradedExample>()
        val grArr = obj.optJSONArray("gradedExamples")
        if (grArr != null) {
            for (i in 0 until grArr.length()) {
                val g = grArr.getJSONObject(i)
                graded.add(
                    GradedExample(
                        level = g.optString("level"),
                        problem = g.optString("problem"),
                        stepByStepSolution = g.optString("stepByStepSolution"),
                        pedagogicalTip = g.optString("pedagogicalTip")
                    )
                )
            }
        }

        SmartExplanationResult(
            subject = obj.optString("subject"),
            grade = obj.optString("grade"),
            lessonTitle = obj.optString("lessonTitle"),
            expansionLevel = obj.optString("expansionLevel"),
            dialectStyle = obj.optString("dialectStyle"),
            concepts = concepts,
            yemeniAnalogies = analogies,
            yemeniExamples = examples,
            deliveryGuide = delivery,
            teachingMethods = methods,
            remediation = remediation,
            interactiveQuestions = questions,
            gradedExamples = graded,
            blackboardPlan = obj.optString("blackboardPlan"),
            classroomScript = obj.optString("classroomScript"),
            teacherEnrichment = obj.optString("teacherEnrichment"),
            isFromGeminiApi = false
        )
    } catch (_: Exception) {
        null
    }
}

private fun buildFullShareText(res: SmartExplanationResult): String {
    val sb = StringBuilder()
    sb.appendLine("📘 دليل الشرح الذكي والمساعد التربوي: ${res.lessonTitle}")
    sb.appendLine("المادة: ${res.subject} | الصف: ${res.grade}")
    sb.appendLine("---------------------------------------")

    sb.appendLine("🔬 أولاً: الشرح العميق والمفاهيم الأساسية:")
    res.concepts.forEachIndexed { i, c ->
        sb.appendLine("${i + 1}. ${c.title}:")
        sb.appendLine(c.deepExplanation)
        if (c.whyAndHow.isNotBlank()) sb.appendLine("• لماذا وكيف تحدث؟ ${c.whyAndHow}")
        if (c.commonMisconception.isNotBlank()) sb.appendLine("• المفهوم الخاطئ الشائع: ${c.commonMisconception}")
        if (c.studentQuestion.isNotBlank()) sb.appendLine("• سؤال الطالب: ${c.studentQuestion} | الإجابة: ${c.studentQuestionAnswer}")
    }

    sb.appendLine("\n🇾🇪 ثانياً: التشبيهات باللهجة اليمنية:")
    res.yemeniAnalogies.forEach { a ->
        sb.appendLine("• ${a.title}: ${a.analogyText}")
    }

    sb.appendLine("\n🌾 ثالثاً: أمثلة من البيئة اليمنية:")
    res.yemeniExamples.forEach { e ->
        sb.appendLine("• ${e.title} (${e.contextType}): ${e.description}")
    }

    sb.appendLine("\n🔟 رابعاً: دليل الإلقاء (10 خطوات):")
    res.deliveryGuide.forEach { d ->
        sb.appendLine("${d.stepNumber}. ${d.stepTitle}: ${d.stepDetail}")
        if (d.teacherScript.isNotBlank()) sb.appendLine("   ما يقوله المعلم: ${d.teacherScript}")
    }

    sb.appendLine("\n🤝 خامساً: علاج تعثر الفهم:")
    res.remediation.forEach { r ->
        sb.appendLine("• الصعوبة: ${r.difficulty} -> الشرح الأبسط: ${r.simplerExplanation}")
    }

    sb.appendLine("\n⏱️ سادساً: سيناريو الحصة (45 دقيقة):")
    sb.appendLine(res.classroomScript)

    sb.appendLine("\n📋 سابعاً: تخطيط السبورة:")
    sb.appendLine(res.blackboardPlan)

    sb.appendLine("---------------------------------------")
    sb.appendLine("تم إعداده عبر تطبيق «المعلم التربوي الذكي»")
    return sb.toString()
}
