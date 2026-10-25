package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ai.GeminiService
import com.example.data.ai.LessonPlanResult
import com.example.data.ai.LessonPlanStep
import com.example.data.model.LessonEntity
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    // Today's date in yyyy/MM/dd
    val todayDate = remember {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
    }

    // Basic Lesson Metadata
    var schoolName by remember { mutableStateOf(teacher.schoolName.ifBlank { "المدرسة النموذجية" }) }
    var grade by remember {
        mutableStateOf(
            if (teacher.gradeTaught.isNotBlank()) {
                YemeniCurriculum.toDisplayGrade(teacher.gradeTaught)
            } else {
                "9/ إعدادي"
            }
        )
    }
    var subject by remember {
        mutableStateOf(
            if (teacher.subject.isNotBlank()) teacher.subject else YemeniCurriculum.getSubjectsForGrade(grade).first()
        )
    }
    var term by remember { mutableStateOf<String>(YemeniCurriculum.TERMS.first()) }
    var unitName by remember { mutableStateOf("") }
    var isManualUnitInput by remember { mutableStateOf(false) }
    var prepType by remember { mutableStateOf(YemeniCurriculum.PREPARATION_TYPES.first()) }
    var studentLevel by remember { mutableStateOf(YemeniCurriculum.STUDENT_LEVELS[1]) }
    var textbookRef by remember { mutableStateOf("") }

    var title by remember { mutableStateOf("") }
    var lessonDate by remember { mutableStateOf(todayDate) }
    var periodNumber by remember { mutableStateOf("الحصة الأولى") }
    var periodsCount by remember { mutableIntStateOf(1) }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var contentSource by remember { mutableStateOf("") }
    var customObjectivesInput by remember { mutableStateOf("") }

    // Dropdown expanded states
    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var termDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var prepTypeDropdownExpanded by remember { mutableStateOf(false) }
    var studentLevelDropdownExpanded by remember { mutableStateOf(false) }
    var periodDropdownExpanded by remember { mutableStateOf(false) }

    // 4 Checkbox Selection Sets
    val selectedTeacherStyles = remember {
        mutableStateListOf(YemeniCurriculum.TEACHER_STYLES[0], YemeniCurriculum.TEACHER_STYLES[1])
    }
    val selectedTeachingMethods = remember {
        mutableStateListOf(YemeniCurriculum.TEACHING_METHODS[0], YemeniCurriculum.TEACHING_METHODS[1])
    }
    val selectedSkillsAndValues = remember {
        mutableStateListOf(YemeniCurriculum.SKILLS_AND_VALUES[0], YemeniCurriculum.SKILLS_AND_VALUES[1], YemeniCurriculum.SKILLS_AND_VALUES[8])
    }
    val selectedTeachingAids = remember {
        mutableStateListOf(YemeniCurriculum.TEACHING_AIDS[0], YemeniCurriculum.TEACHING_AIDS[1])
    }

    // State of AI Generation & Manual Editing
    var isGenerating by remember { mutableStateOf(false) }
    var activePlan by remember { mutableStateOf<LessonPlanResult?>(null) }
    var currentSavedLessonId by remember { mutableStateOf<Long?>(null) }
    var isManualEditMode by remember { mutableStateOf(false) }
    var isSavedNotification by remember { mutableStateOf(false) }

    // Editable Plan Fields
    var editCognitive by remember { mutableStateOf("") }
    var editSkill by remember { mutableStateOf("") }
    var editAffective by remember { mutableStateOf("") }
    var editIntroduction by remember { mutableStateOf("") }
    var editAssessment by remember { mutableStateOf("") }
    var editHomework by remember { mutableStateOf("") }
    var editTeacherNotes by remember { mutableStateOf("") }
    var editSteps by remember { mutableStateOf<List<LessonPlanStep>>(emptyList()) }

    // Dialogs
    var showSavedLessonsDialog by remember { mutableStateOf(false) }
    var lessonToDelete by remember { mutableStateOf<LessonEntity?>(null) }

    // When activePlan updates, update editable fields
    fun syncEditableFields(plan: LessonPlanResult) {
        editCognitive = plan.cognitiveObjectives
        editSkill = plan.skillObjectives
        editAffective = plan.affectiveObjectives
        editIntroduction = plan.introduction
        editAssessment = plan.assessment
        editHomework = plan.homework
        editTeacherNotes = plan.teacherNotes
        editSteps = plan.steps
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "تحضير الدرس – المنهج اليمني",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "الجمهورية اليمنية - وزارة التربية والتعليم",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("prep_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Saved Preps Button
                    IconButton(
                        onClick = { showSavedLessonsDialog = true },
                        modifier = Modifier.testTag("saved_lessons_menu_button")
                    ) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = "تحضيراتي المحفوظة")
                    }

                    // Print / PDF Button
                    if (activePlan != null) {
                        IconButton(
                            onClick = {
                                val entity = buildCurrentLessonEntity(
                                    teacher = teacher,
                                    schoolName = schoolName,
                                    grade = grade,
                                    subject = subject,
                                    title = title,
                                    periodNumber = periodNumber,
                                    periodsCount = periodsCount,
                                    durationMinutes = durationMinutes,
                                    cognitive = editCognitive,
                                    skill = editSkill,
                                    affective = editAffective,
                                    intro = editIntroduction,
                                    assessment = editAssessment,
                                    homework = editHomework,
                                    notes = editTeacherNotes,
                                    steps = editSteps,
                                    styles = selectedTeacherStyles,
                                    methods = selectedTeachingMethods,
                                    skills = selectedSkillsAndValues,
                                    aids = selectedTeachingAids,
                                    savedId = currentSavedLessonId
                                )
                                PrintUtils.printLesson(context, entity)
                            },
                            modifier = Modifier.testTag("print_lesson_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "حفظ التحضير PDF")
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Action Bar: تحضير جديد | تحضيراتي المحفوظة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Reset form
                        title = ""
                        activePlan = null
                        currentSavedLessonId = null
                        isManualEditMode = false
                        isSavedNotification = false
                        Toast.makeText(context, "تم بدء نموذج تحضير جديد", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).testTag("new_prep_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحضير جديد")
                }

                FilledTonalButton(
                    onClick = { showSavedLessonsDialog = true },
                    modifier = Modifier.weight(1f).testTag("my_saved_preps_button")
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحضيراتي المحفوظة")
                }
            }

            // 1. Basic Metadata Card (بيانات الدرس الأساسية)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "1. بيانات الدرس الأساسية (المنهج اليمني)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            val sec = if (teacher.section.isNotBlank()) " | شعبة ${teacher.section}" else ""
                            Text(
                                "المعلم: ${teacher.fullName}$sec",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // School Name
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("اسم المدرسة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("school_name_input")
                    )

                    // Grade & Subject Dropdowns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Grade Selector
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = grade,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الصف الدراسي *") },
                                trailingIcon = {
                                    IconButton(onClick = { gradeDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار الصف")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { gradeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = gradeDropdownExpanded,
                                onDismissRequest = { gradeDropdownExpanded = false }
                            ) {
                                YemeniCurriculum.ALL_GRADES.forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text(g) },
                                        onClick = {
                                            grade = g
                                            gradeDropdownExpanded = false
                                            val validSubjects = YemeniCurriculum.getSubjectsForGrade(g)
                                            if (!validSubjects.contains(subject)) {
                                                subject = validSubjects.first()
                                            }
                                            unitName = ""
                                        }
                                    )
                                }
                            }
                        }

                        // Subject Selector
                        Box(modifier = Modifier.weight(1f)) {
                            val availableSubjects = YemeniCurriculum.getSubjectsForGrade(grade)
                            OutlinedTextField(
                                value = subject,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("المادة الدراسية *") },
                                trailingIcon = {
                                    IconButton(onClick = { subjectDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار المادة")
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
                                            unitName = ""
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Semester & Unit Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Semester (الفصل الدراسي)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = term,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الفصل الدراسي *") },
                                trailingIcon = {
                                    IconButton(onClick = { termDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار الفصل")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { termDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = termDropdownExpanded,
                                onDismissRequest = { termDropdownExpanded = false }
                            ) {
                                YemeniCurriculum.SEMESTERS.forEach { t ->
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

                        // Unit (الوحدة الدراسية)
                        Box(modifier = Modifier.weight(1.2f)) {
                            val availableUnitsMap = YemeniCurriculum.getCurriculumUnits(grade, subject)
                            val availableUnits = availableUnitsMap.keys.toList()
                            if (isManualUnitInput) {
                                OutlinedTextField(
                                    value = unitName,
                                    onValueChange = { unitName = it },
                                    label = { Text("الوحدة الدراسية (يدوي)") },
                                    placeholder = { Text("اكتب اسم الوحدة...") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { isManualUnitInput = false }) {
                                            Icon(Icons.Default.List, contentDescription = "اختيار من القائمة")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedTextField(
                                    value = unitName.ifBlank { if (availableUnits.isNotEmpty()) availableUnits.first() else "الوحدة الأولى" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الوحدة الدراسية *") },
                                    trailingIcon = {
                                        Row {
                                            IconButton(onClick = { unitDropdownExpanded = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار الوحدة")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { unitDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = unitDropdownExpanded,
                                    onDismissRequest = { unitDropdownExpanded = false }
                                ) {
                                    availableUnits.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u) },
                                            onClick = {
                                                unitName = u
                                                unitDropdownExpanded = false
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("✏️ كتابة عنوان وحدة يدوي...", fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            isManualUnitInput = true
                                            unitDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Lesson Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان وموضوع الدرس *") },
                        placeholder = { Text("مثال: دورة الماء في الطبيعة / اسم الفاعل وصياغته / حضارة سبأ وحمير") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("lesson_title_input")
                    )

                    // Preparation Type & Student Level Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Preparation Type (نوع التحضير)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = prepType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("نوع التحضير *") },
                                trailingIcon = {
                                    IconButton(onClick = { prepTypeDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { prepTypeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = prepTypeDropdownExpanded,
                                onDismissRequest = { prepTypeDropdownExpanded = false }
                            ) {
                                YemeniCurriculum.PREPARATION_TYPES.forEach { pt ->
                                    DropdownMenuItem(
                                        text = { Text(pt) },
                                        onClick = {
                                            prepType = pt
                                            prepTypeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Student Level (مستوى الطلاب)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = studentLevel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("مستوى الطلاب *") },
                                trailingIcon = {
                                    IconButton(onClick = { studentLevelDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { studentLevelDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = studentLevelDropdownExpanded,
                                onDismissRequest = { studentLevelDropdownExpanded = false }
                            ) {
                                YemeniCurriculum.STUDENT_LEVELS.forEach { sl ->
                                    DropdownMenuItem(
                                        text = { Text(sl) },
                                        onClick = {
                                            studentLevel = sl
                                            studentLevelDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Date, Period Number, Periods Count, Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = lessonDate,
                            onValueChange = { lessonDate = it },
                            label = { Text("التاريخ") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        // Period Number Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val periodOptions = listOf("الحصة الأولى", "الحصة الثانية", "الحصة الثالثة", "الحصة الرابعة", "الحصة الخامسة", "الحصة السادسة", "الحصة السابعة")
                            OutlinedTextField(
                                value = periodNumber,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("رقم الحصة") },
                                trailingIcon = {
                                    IconButton(onClick = { periodDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { periodDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = periodDropdownExpanded,
                                onDismissRequest = { periodDropdownExpanded = false }
                            ) {
                                periodOptions.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            periodNumber = p
                                            periodDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Periods Count
                        OutlinedTextField(
                            value = "$periodsCount حصة",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("عدد الحصص") },
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { if (periodsCount > 1) periodsCount-- }) {
                                        Icon(Icons.Default.Remove, contentDescription = "تقليل")
                                    }
                                    IconButton(onClick = { if (periodsCount < 4) periodsCount++ }) {
                                        Icon(Icons.Default.Add, contentDescription = "زيادة")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.1f)
                        )

                        // Duration (Minutes)
                        OutlinedTextField(
                            value = "$durationMinutes د",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("زمن الحصة") },
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { if (durationMinutes > 30) durationMinutes -= 5 }) {
                                        Icon(Icons.Default.Remove, contentDescription = "تقليل")
                                    }
                                    IconButton(onClick = { if (durationMinutes < 90) durationMinutes += 5 }) {
                                        Icon(Icons.Default.Add, contentDescription = "زيادة")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Educational Reference / Textbook edition
                    OutlinedTextField(
                        value = textbookRef,
                        onValueChange = { textbookRef = it },
                        label = { Text("المرجع الدراسي / طبعة الكتاب المعتمدة") },
                        placeholder = { Text("كتاب $subject للصف $grade - وزارة التربية والتعليم اليمنية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Textbook / Reference Content (optional)
                    OutlinedTextField(
                        value = contentSource,
                        onValueChange = { contentSource = it },
                        label = { Text("نصوص إضافية من كتاب المنهج (اختياري)") },
                        placeholder = { Text("الصق هنا فقرات من الكتاب المدرسي لترتكز الخطة على نصوص المنهج المعتمد...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("content_source_input")
                    )

                    // Custom Objectives input (optional)
                    OutlinedTextField(
                        value = customObjectivesInput,
                        onValueChange = { customObjectivesInput = it },
                        label = { Text("أهداف إضافية يود المعلم التركيز عليها (اختياري)") },
                        placeholder = { Text("اترك فارغاً ليقوم رقيب باشتقاق الأهداف المعرفية والمهارية والوجدانية تلقائياً...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. The 4 Interactive Checkbox Boxes (صناديق الاختيار بمربعات صح تفاعلية)
            Text(
                "2. الخيارات التربوية المحددة للتحضير (مربعات صح تفاعلية)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "حدد العناصر المرغوبة بدقة. نظام رقيب لن يضيف في الخطة أو في الجدول إلا ما تختاره هنا حصراً.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Box 1: أساليب المعلم
            CheckboxGroupCard(
                title = "الصندوق الأول: أساليب المعلم",
                items = YemeniCurriculum.TEACHER_STYLES,
                selectedItems = selectedTeacherStyles,
                color = Color(0xFF1E40AF),
                tag = "styles"
            )

            // Box 2: طرق التدريس
            CheckboxGroupCard(
                title = "الصندوق الثاني: طرق التدريس",
                items = YemeniCurriculum.TEACHING_METHODS,
                selectedItems = selectedTeachingMethods,
                color = Color(0xFF047857),
                tag = "methods"
            )

            // Box 3: المهارات والقيم
            CheckboxGroupCard(
                title = "الصندوق الثالث: المهارات والقيم",
                items = YemeniCurriculum.SKILLS_AND_VALUES,
                selectedItems = selectedSkillsAndValues,
                color = Color(0xFFB45309),
                tag = "skills"
            )

            // Box 4: الوسائل التعليمية
            CheckboxGroupCard(
                title = "الصندوق الرابع: الوسائل التعليمية",
                items = YemeniCurriculum.TEACHING_AIDS,
                selectedItems = selectedTeachingAids,
                color = Color(0xFF6B21A8),
                tag = "aids"
            )

            // 3. AI Generation Button (زر توليد التحضير بالذكاء الاصطناعي)
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "يرجى كتابة عنوان وموضوع الدرس أولاً", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedTeacherStyles.isEmpty()) {
                        Toast.makeText(context, "يرجى اختيار أسلوب تدريس واحد على الأقل من الصندوق الأول", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedTeachingMethods.isEmpty()) {
                        Toast.makeText(context, "يرجى اختيار طريقة تدريس واحدة على الأقل من الصندوق الثاني", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isGenerating = true
                    isSavedNotification = false
                    coroutineScope.launch {
                        val plan = geminiService.generateLessonPlan(
                            schoolName = schoolName,
                            teacherName = teacher.fullName,
                            subject = subject,
                            grade = grade,
                            title = title,
                            date = lessonDate,
                            period = periodNumber,
                            periodsCount = periodsCount,
                            duration = durationMinutes,
                            unit = unitName,
                            term = term,
                            prepType = prepType,
                            studentLevel = studentLevel,
                            textbookRef = textbookRef,
                            selectedTeacherStyles = selectedTeacherStyles.toList(),
                            selectedTeachingMethods = selectedTeachingMethods.toList(),
                            selectedSkillsAndValues = selectedSkillsAndValues.toList(),
                            selectedTeachingAids = selectedTeachingAids.toList(),
                            objectivesInput = customObjectivesInput,
                            contentSource = contentSource
                        )
                        activePlan = plan
                        syncEditableFields(plan)
                        isGenerating = false
                        Toast.makeText(context, "تم توليد خطة الدرس وفق خياراتك المحددة بنجاح", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_lesson_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("جارٍ صياغة التحضير بالذكاء الاصطناعي وفق المنهج اليمني...", fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("توليد التحضير بالذكاء الاصطناعي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // 4. Final Preparation Result / Official Table / Manual Edit
            if (activePlan != null) {
                val plan = activePlan!!

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth().testTag("generated_lesson_container")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title bar of the result
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "خطة التحضير الرسمية: $title",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "الجمهورية اليمنية - وزارة التربية والتعليم",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // View / Edit Mode Switcher Button
                            FilledTonalButton(
                                onClick = { isManualEditMode = !isManualEditMode },
                                modifier = Modifier.testTag("toggle_edit_mode_button")
                            ) {
                                Icon(
                                    if (isManualEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isManualEditMode) "معاينة الجدول" else "تعديل يدوي")
                            }
                        }

                        HorizontalDivider()

                        if (isManualEditMode) {
                            // Manual Editing Form
                            Text(
                                "تعديل التحضير يدويًا (يمكنك تعديل أي قسم وحفظ التعديلات):",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp
                            )

                            OutlinedTextField(
                                value = editCognitive,
                                onValueChange = { editCognitive = it },
                                label = { Text("الأهداف المعرفية") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("edit_cognitive_input")
                            )

                            OutlinedTextField(
                                value = editSkill,
                                onValueChange = { editSkill = it },
                                label = { Text("الأهداف المهارية") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("edit_skill_input")
                            )

                            OutlinedTextField(
                                value = editAffective,
                                onValueChange = { editAffective = it },
                                label = { Text("الأهداف الوجدانية والقيمية") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("edit_affective_input")
                            )

                            OutlinedTextField(
                                value = editIntroduction,
                                onValueChange = { editIntroduction = it },
                                label = { Text("التهيئة والتمهيد (5 دقائق)") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("edit_intro_input")
                            )

                            OutlinedTextField(
                                value = editAssessment,
                                onValueChange = { editAssessment = it },
                                label = { Text("التقويم (المعرفي، المهاري، الوجداني)") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("edit_eval_input")
                            )

                            OutlinedTextField(
                                value = editHomework,
                                onValueChange = { editHomework = it },
                                label = { Text("الواجب المنزلي أو النشاط اللاصفي") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("edit_homework_input")
                            )

                            OutlinedTextField(
                                value = editTeacherNotes,
                                onValueChange = { editTeacherNotes = it },
                                label = { Text("ملاحظات المعلم بعد تنفيذ الدرس") },
                                placeholder = { Text("تدوين انطباعات الحصة، استجابة الطلاب، والصعوبات المعالجة...") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("edit_notes_input")
                            )
                        } else {
                            // Official Yemeni Preparation Table View (18 Full Sections)
                            OfficialPreparationView(
                                schoolName = schoolName,
                                teacherName = teacher.fullName,
                                grade = grade,
                                subject = subject,
                                title = title,
                                date = lessonDate,
                                period = periodNumber,
                                periodsCount = periodsCount,
                                durationMinutes = durationMinutes,
                                unitName = unitName,
                                term = term,
                                prepType = prepType,
                                studentLevel = studentLevel,
                                textbookRef = textbookRef,
                                cognitive = editCognitive,
                                skill = editSkill,
                                affective = editAffective,
                                selectedStyles = selectedTeacherStyles,
                                selectedMethods = selectedTeachingMethods,
                                selectedSkills = selectedSkillsAndValues,
                                selectedAids = selectedTeachingAids,
                                introduction = editIntroduction,
                                steps = editSteps,
                                assessment = editAssessment,
                                homework = editHomework,
                                notes = editTeacherNotes,
                                plan = plan
                            )
                        }

                        // Bottom Actions: حفظ التعديلات / حفظ في الأرشيف | حفظ التحضير PDF | مشاركة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val entity = buildCurrentLessonEntity(
                                            teacher = teacher,
                                            schoolName = schoolName,
                                            grade = grade,
                                            subject = subject,
                                            title = title,
                                            periodNumber = periodNumber,
                                            periodsCount = periodsCount,
                                            durationMinutes = durationMinutes,
                                            cognitive = editCognitive,
                                            skill = editSkill,
                                            affective = editAffective,
                                            intro = editIntroduction,
                                            assessment = editAssessment,
                                            homework = editHomework,
                                            notes = editTeacherNotes,
                                            steps = editSteps,
                                            styles = selectedTeacherStyles,
                                            methods = selectedTeachingMethods,
                                            skills = selectedSkillsAndValues,
                                            aids = selectedTeachingAids,
                                            savedId = currentSavedLessonId
                                        )
                                        repository.saveLesson(entity)
                                        isSavedNotification = true
                                        Toast.makeText(context, "تم حفظ التحضير وجميع التعديلات بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_modifications_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSavedNotification) "تم حفظ التعديلات ✓" else "حفظ التعديلات")
                            }

                            Button(
                                onClick = {
                                    val entity = buildCurrentLessonEntity(
                                        teacher = teacher,
                                        schoolName = schoolName,
                                        grade = grade,
                                        subject = subject,
                                        title = title,
                                        periodNumber = periodNumber,
                                        periodsCount = periodsCount,
                                        durationMinutes = durationMinutes,
                                        cognitive = editCognitive,
                                        skill = editSkill,
                                        affective = editAffective,
                                        intro = editIntroduction,
                                        assessment = editAssessment,
                                        homework = editHomework,
                                        notes = editTeacherNotes,
                                        steps = editSteps,
                                        styles = selectedTeacherStyles,
                                        methods = selectedTeachingMethods,
                                        skills = selectedSkillsAndValues,
                                        aids = selectedTeachingAids,
                                        savedId = currentSavedLessonId
                                    )
                                    PrintUtils.printLesson(context, entity)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                modifier = Modifier.weight(1f).testTag("export_pdf_button")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ التحضير PDF")
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: My Saved Preps (تحضيراتي المحفوظة)
    if (showSavedLessonsDialog) {
        val savedLessons by repository.getLessons(teacher.id).collectAsState(initial = emptyList())

        Dialog(onDismissRequest = { showSavedLessonsDialog = false }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "تحضيراتي المحفوظة (${savedLessons.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showSavedLessonsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    HorizontalDivider()

                    if (savedLessons.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "لا توجد تحضيرات محفوظة حالياً.\nقم بإعداد وتوليد درس وحفظه ليظهر هنا.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            savedLessons.forEach { saved ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                saved.title,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "${saved.durationMinutes} دقيقة",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Text(
                                            "المادة: ${saved.subject} | الصف: ${saved.grade}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Open / Edit
                                            OutlinedButton(
                                                onClick = {
                                                    // Load saved into active
                                                    title = saved.title
                                                    subject = saved.subject
                                                    grade = saved.grade
                                                    schoolName = saved.schoolName.ifBlank { schoolName }
                                                    durationMinutes = saved.durationMinutes
                                                    currentSavedLessonId = saved.id

                                                    editCognitive = saved.objectives
                                                    editSkill = ""
                                                    editAffective = ""
                                                    editIntroduction = saved.introduction
                                                    editAssessment = saved.formativeAssessment
                                                    editHomework = saved.homework
                                                    editTeacherNotes = saved.teacherNotes

                                                    activePlan = LessonPlanResult(
                                                        schoolName = saved.schoolName,
                                                        teacherName = teacher.fullName,
                                                        grade = saved.grade,
                                                        subject = saved.subject,
                                                        title = saved.title,
                                                        objectives = saved.objectives,
                                                        cognitiveObjectives = saved.objectives,
                                                        introduction = saved.introduction,
                                                        explanation = saved.detailedExplanation,
                                                        teacherActivities = saved.teacherActivities,
                                                        studentActivities = saved.studentActivities,
                                                        strategies = saved.strategies,
                                                        teachingMaterials = saved.teachingMaterials,
                                                        assessment = saved.formativeAssessment,
                                                        summary = saved.lessonSummary,
                                                        homework = saved.homework,
                                                        timeAllocation = saved.timeAllocation,
                                                        teacherNotes = saved.teacherNotes
                                                    )
                                                    showSavedLessonsDialog = false
                                                    Toast.makeText(context, "تم فتح التحضير بنجاح", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("فتح وتعديل", fontSize = 12.sp)
                                            }

                                            // Print PDF
                                            IconButton(
                                                onClick = { PrintUtils.printLesson(context, saved) }
                                            ) {
                                                Icon(Icons.Default.Print, contentDescription = "طباعة PDF", tint = MaterialTheme.colorScheme.primary)
                                            }

                                            // Delete with confirmation
                                            IconButton(
                                                onClick = { lessonToDelete = saved }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showSavedLessonsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إغلاق")
                    }
                }
            }
        }
    }

    // Confirmation Dialog before deletion
    if (lessonToDelete != null) {
        val target = lessonToDelete!!
        AlertDialog(
            onDismissRequest = { lessonToDelete = null },
            title = { Text("تأكيد حذف التحضير") },
            text = { Text("هل أنت متأكد من رغبتك في حذف تحضير درس: \"${target.title}\" نهائياً؟ لن يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteLesson(target.id)
                            if (currentSavedLessonId == target.id) {
                                currentSavedLessonId = null
                            }
                            lessonToDelete = null
                            Toast.makeText(context, "تم حذف التحضير بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، احذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { lessonToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Renders a Checkbox Box with genuine touch-friendly Checkbox items
 */
@Composable
private fun CheckboxGroupCard(
    title: String,
    items: List<String>,
    selectedItems: MutableList<String>,
    color: Color,
    tag: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("checkbox_box_$tag")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${selectedItems.size} محددة",
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Checkbox List (Wrapped in 2 columns or grid for mobile responsiveness)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { item ->
                            val isChecked = selectedItems.contains(item)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChecked) color.copy(alpha = 0.08f) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (isChecked) color.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clickable {
                                        if (isChecked) {
                                            selectedItems.remove(item)
                                        } else {
                                            selectedItems.add(item)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (!selectedItems.contains(item)) selectedItems.add(item)
                                            } else {
                                                selectedItems.remove(item)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = color,
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item,
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChecked) color else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Official Yemeni Preparation Book View (Comprehensive 18 Sections & 5-Column Table)
 */
@Composable
private fun OfficialPreparationView(
    schoolName: String,
    teacherName: String,
    grade: String,
    subject: String,
    title: String,
    date: String,
    period: String,
    periodsCount: Int,
    durationMinutes: Int,
    unitName: String,
    term: String,
    prepType: String,
    studentLevel: String,
    textbookRef: String,
    cognitive: String,
    skill: String,
    affective: String,
    selectedStyles: List<String>,
    selectedMethods: List<String>,
    selectedSkills: List<String>,
    selectedAids: List<String>,
    introduction: String,
    steps: List<LessonPlanStep>,
    assessment: String,
    homework: String,
    notes: String,
    plan: LessonPlanResult? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. البيانات الأساسية
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("1. البيانات الأساسية (المنهج اليمني الرسمي)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المدرسة: $schoolName", fontSize = 12.sp)
                    Text("المعلم: $teacherName", fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الصف: $grade", fontSize = 12.sp)
                    Text("المادة: $subject", fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الفصل: $term", fontSize = 12.sp)
                    Text("الوحدة: ${unitName.ifBlank { "الوحدة الأولى" }}", fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("رقم الحصة: $period ($periodsCount حصص)", fontSize = 12.sp)
                    Text("زمن الحصة: $durationMinutes دقيقة", fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("نوع التحضير: $prepType", fontSize = 12.sp)
                    Text("مستوى الطلاب: $studentLevel", fontSize = 12.sp)
                }
                if (textbookRef.isNotBlank()) {
                    Text("المرجع: $textbookRef", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // 2. الأهداف التعليمية العامة والخاصة
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("2. الأهداف التعليمية العامة والخاصة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                if (plan != null && plan.generalObjectives.isNotBlank()) {
                    Text("• الأهداف العامة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(plan.generalObjectives, fontSize = 12.sp, lineHeight = 18.sp)
                }
                if (cognitive.isNotBlank()) {
                    Text("• الأهداف المعرفية:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(cognitive, fontSize = 12.sp, lineHeight = 18.sp)
                }
                if (skill.isNotBlank()) {
                    Text("• الأهداف المهارية:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(skill, fontSize = 12.sp, lineHeight = 18.sp)
                }
                if (affective.isNotBlank()) {
                    Text("• الأهداف الوجدانية والقيمية:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(affective, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 3. نواتج التعلم المتوقعة
        if (plan != null && plan.learningOutcomes.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("3. نواتج التعلم المتوقعة بنهاية الحصة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.learningOutcomes, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 4. المتطلبات السابقة للدرس
        if (plan != null && plan.prerequisites.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("4. المتطلبات والخبرات السابقة اللازمة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.prerequisites, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 5. التمهيد والتهيئة
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("5. التهيئة والتمهيد لجذب انتباه الطلاب (5 دقائق)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                Text(introduction, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }

        // 6. الوسائل التعليمية وطرق التدريس
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("6. الأساليب والطرق والوسائل التعليمية المعتمدة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                Text("• أساليب المعلم: ${selectedStyles.joinToString("، ")}", fontSize = 12.sp, lineHeight = 17.sp)
                Text("• طرق التدريس: ${selectedMethods.joinToString("، ")}", fontSize = 12.sp, lineHeight = 17.sp)
                Text("• المهارات والقيم: ${selectedSkills.joinToString("، ")}", fontSize = 12.sp, lineHeight = 17.sp)
                Text("• الوسائل المعينة: ${selectedAids.joinToString("، ")}", fontSize = 12.sp, lineHeight = 17.sp)
            }
        }

        // 7. المفاهيم والمصطلحات الأساسية
        if (plan != null && plan.keyConcepts.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("7. المفاهيم والمصطلحات الأساسية في الدرس", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.keyConcepts, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 8. خطوات عرض الدرس في جدول تربوي رسمي (5 أعمدة)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "8. خطوات عرض وتدريس الحصة (جدول دفتر التحضير الرسمي)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            "↔ اسحب الجدول أفقياً",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Responsive Horizontal Scroll Table
                val tableScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tableScrollState)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                ) {
                    Column(modifier = Modifier.widthIn(min = 680.dp)) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المرحلة والزمن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                            Text("إجراءات المعلم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(2.3f), textAlign = TextAlign.Center)
                            Text("نشاط التلميذ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(2.0f), textAlign = TextAlign.Center)
                            Text("الوسيلة المستخدمة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
                            Text("طريقة التدريس / الأسلوب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
                        }

                        // Steps Rows
                        steps.forEachIndexed { index, step ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(step.stageAndTime, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                                Text(step.teacherActions, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.weight(2.3f).padding(horizontal = 6.dp))
                                Text(step.studentActivities, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.weight(2.0f).padding(horizontal = 6.dp))
                                Text(step.materialsUsed, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
                                Text(step.teachingMethod, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        // 9. دور المعلم ودور الطلاب
        if (plan != null && plan.teacherAndStudentRole.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("9. تكامل الأدوار (دور المعلم ودور الطلاب في الموقف التعليمي)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.teacherAndStudentRole, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 10. أمثلة محلولة مناسبة للصف
        if (plan != null && plan.solvedExamples.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("10. أمثلة محلولة ونماذج تطبيقية متدرجة الصعوبة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.solvedExamples, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 11. أنشطة صفية وفردية وجماعية
        if (plan != null && plan.classroomActivities.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("11. أنشطة التعلم (فردية، ثنائية، وجماعية)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.classroomActivities, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 12. أسئلة أثناء الشرح للتحقق من الفهم
        if (plan != null && plan.questionsDuringExplanation.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("12. أسئلة التغذية الراجعة والتحقق الفوري أثناء الشرح", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.questionsDuringExplanation, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 13. مراعاة الفروق الفردية
        if (plan != null && plan.individualDifferences.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("13. خطة مراعاة الفروق الفردية (دعم المتعثرين + إثراء الموهوبين)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.individualDifferences, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 14. أخطاء الطلاب المتوقعة ومعالجتها
        if (plan != null && plan.expectedErrorsAndRemediation.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("14. الأخطاء المفاهيمية الشائعة وطرق التدخل العلاجي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.expectedErrorsAndRemediation, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 15. التقويم القبلي والتكويني والختامي
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("15. استراتيجيات التقويم التربوي (قبلي، بنائي/تكويني، وختامي)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                if (plan != null && plan.assessmentPreFormativeSummative.isNotBlank()) {
                    Text(plan.assessmentPreFormativeSummative, fontSize = 12.sp, lineHeight = 18.sp)
                } else {
                    Text(assessment, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 16. الواجب المنزلي
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("16. الواجب المنزلي والأنشطة اللاصفية", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                Text(homework, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }

        // 17. ملخص الدرس
        if (plan != null && plan.lessonSummary.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("17. ملخص وخلاصة الدرس السبورية", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                    Text(plan.lessonSummary, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }

        // 18. الإجابات النموذجية وملاحظات المعلم وتوزيع الوقت
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("18. الإجابات النموذجية وتوزيع الوقت وملاحظات المعلم", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.5.sp)
                if (plan != null && plan.modelAnswers.isNotBlank()) {
                    Text("• حلول الأنشطة والأسئلة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(plan.modelAnswers, fontSize = 12.sp, lineHeight = 18.sp)
                }
                if (plan != null && plan.timeDistribution.isNotBlank()) {
                    Text("• خطة توزيع وقت الحصة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(plan.timeDistribution, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Text("• ملاحظات المعلم بعد تنفيذ الدرس:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                Text(
                    if (notes.isNotBlank()) notes else "مساحة مخصصة للمعلم لتدوين الملاحظات والانطباعات بعد انتهاء الحصة...",
                    fontSize = 12.sp,
                    color = if (notes.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun buildCurrentLessonEntity(
    teacher: TeacherEntity,
    schoolName: String,
    grade: String,
    subject: String,
    title: String,
    periodNumber: String,
    periodsCount: Int,
    durationMinutes: Int,
    cognitive: String,
    skill: String,
    affective: String,
    intro: String,
    assessment: String,
    homework: String,
    notes: String,
    steps: List<LessonPlanStep>,
    styles: List<String>,
    methods: List<String>,
    skills: List<String>,
    aids: List<String>,
    savedId: Long?
): LessonEntity {
    val combinedObjectives = "الأهداف المعرفية:\n$cognitive\n\nالأهداف المهارية:\n$skill\n\nالأهداف الوجدانية:\n$affective"
    val stepsExplanation = steps.joinToString("\n\n") { "• ${it.stageAndTime}: ${it.teacherActions} (الوسيلة: ${it.materialsUsed} | الطريقة: ${it.teachingMethod})" }
    val teacherAct = steps.joinToString("\n") { "• ${it.stageAndTime}: ${it.teacherActions}" }
    val studentAct = steps.joinToString("\n") { "• ${it.stageAndTime}: ${it.studentActivities}" }

    return LessonEntity(
        id = savedId ?: 0,
        teacherId = teacher.id,
        title = title.ifBlank { "درس نموذجي" },
        subject = subject,
        grade = grade,
        durationMinutes = durationMinutes,
        schoolName = schoolName,
        directorate = teacher.directorate,
        governorate = teacher.governorate,
        term = "الفصل الدراسي الأول",
        unitNumber = teacher.fullName, // Storing teacher full name in unitNumber for official printout
        lessonNumber = "$periodsCount",
        bookPages = "المقررة",
        period = periodNumber,
        objectives = combinedObjectives,
        introduction = intro,
        previousKnowledge = skills.joinToString("، "),
        detailedExplanation = stepsExplanation,
        teacherActivities = teacherAct,
        studentActivities = studentAct,
        strategies = methods.joinToString("، "),
        teachingMaterials = aids.joinToString("، "),
        formativeAssessment = assessment,
        finalAssessment = "فحص استيعاب المخرجات ومناقشة بطاقة الخروج",
        questionsAndAnswers = "",
        lessonSummary = "تثبيت المفاهيم المحورية لدرس $title",
        homework = homework,
        timeAllocation = "التهيئة: 5 د | الشرح: 20 د | التطبيق: 12 د | التقويم: 8 د",
        teacherNotes = notes
    )
}
