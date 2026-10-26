package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var term by remember { mutableStateOf(YemeniCurriculum.TERMS.first()) }
    var unitName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var lessonDate by remember { mutableStateOf("1446/04/15هـ") }
    var periodNumber by remember { mutableStateOf("الحصة الأولى") }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var prepType by remember { mutableStateOf(YemeniCurriculum.PREPARATION_TYPES.first()) }
    var studentLevel by remember { mutableStateOf(YemeniCurriculum.STUDENT_LEVELS[1]) }
    var textbookRef by remember { mutableStateOf("") }

    var cognitiveObjectives by remember { mutableStateOf("") }
    var skillObjectives by remember { mutableStateOf("") }
    var affectiveObjectives by remember { mutableStateOf("") }
    var introduction by remember { mutableStateOf("") }
    var teachingSteps by remember { mutableStateOf("") }
    var formativeAssessment by remember { mutableStateOf("") }
    var homework by remember { mutableStateOf("") }
    var teacherNotes by remember { mutableStateOf("") }

    var isAiGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var termDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعداد وتحضير الدرس (المنهج اليمني)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("lesson_prep_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val entity = LessonEntity(
                                id = 0,
                                teacherId = teacher.id,
                                title = title.ifBlank { "درس غير معنون" },
                                subject = subject,
                                grade = grade,
                                durationMinutes = durationMinutes,
                                schoolName = schoolName,
                                directorate = teacher.directorate,
                                governorate = teacher.governorate,
                                term = term,
                                unitName = unitName,
                                lessonDate = lessonDate,
                                periodNumber = periodNumber,
                                studentLevel = studentLevel,
                                preparationType = prepType,
                                cognitiveObjectives = cognitiveObjectives,
                                skillObjectives = skillObjectives,
                                affectiveObjectives = affectiveObjectives,
                                introduction = introduction,
                                teachingSteps = teachingSteps,
                                formativeAssessment = formativeAssessment,
                                homework = homework,
                                teacherNotes = teacherNotes,
                                textbookReference = textbookRef,
                                curriculumEdition = teacher.curriculumEdition
                            )
                            PrintUtils.printLesson(context, entity)
                        },
                        modifier = Modifier.testTag("print_lesson_pdf_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "طباعة PDF")
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
            // Header Teacher Info Badge
            Card(
                shape = RoundedCornerShape(14.dp),
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
                    Column {
                        Text(
                            "الجمهورية اليمنية - وزارة التربية والتعليم",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "الخطة النموذجية للتحضير اليومي",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        val sec = if (teacher.section.isNotBlank()) " | شعبة ${teacher.section}" else ""
                        Text(
                            "المعلم: ${teacher.fullName}$sec",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Lesson Metadata Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("1. البيانات الأساسية للدرس", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان أو موضوع الدرس *") },
                        placeholder = { Text("مثال: حل المعادلات التربيعية بالتحليل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("lesson_title_input")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Grade Selector (12 display names)
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
                                            val validSubjects = YemeniCurriculum.getSubjectsForGrade(g)
                                            if (!validSubjects.contains(subject)) {
                                                subject = validSubjects.first()
                                            }
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
                                label = { Text("المادة *") },
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
                                YemeniCurriculum.getSubjectsForGrade(grade).forEach { sub ->
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = unitName,
                            onValueChange = { unitName = it },
                            label = { Text("الوحدة التعليمية") },
                            placeholder = { Text("الوحدة الأولى") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = lessonDate,
                            onValueChange = { lessonDate = it },
                            label = { Text("التاريخ") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = textbookRef,
                        onValueChange = { textbookRef = it },
                        label = { Text("مرجع الكتاب المدرسي وطبعته") },
                        placeholder = { Text("كتاب الرياضيات - الصف التاسع - طبعة صنعاء ص 40-45") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // AI Objectives Generator Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "يرجى كتابة عنوان الدرس أولاً لتوليد الأهداف", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isAiGenerating = true
                    coroutineScope.launch {
                        val (cog, sk, aff) = geminiService.generateLessonObjectives(subject, grade, unitName, title)
                        cognitiveObjectives = cog
                        skillObjectives = sk
                        affectiveObjectives = aff
                        if (introduction.isBlank()) {
                            introduction = "تهيئة صفيّة مستوحاة من البيئة اليمنية ومراجعة المفاهيم السابقة المرتبطة بـ ($title)."
                        }
                        if (teachingSteps.isBlank()) {
                            teachingSteps = "1. عرض تمهيدي ومناقشة أمثلة الكتاب المدرسي.\n2. استنتاج القواعد الأساسية وتدوينها على السبورة.\n3. نشاط فردي وجماعي لحل تمارين الدرس."
                        }
                        if (formativeAssessment.isBlank()) {
                            formativeAssessment = "تطبيق صفي سريع للتحقق من استيعاب الأهداف السلوكية."
                        }
                        if (homework.isBlank()) {
                            homework = "حل التمارين المقررة في الكتاب المدرسي لكراسة الواجب."
                        }
                        isAiGenerating = false
                        Toast.makeText(context, "تم توليد الأهداف ومحتوى الدرس بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ai_generate_objectives_button")
            ) {
                if (isAiGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("توليد الأهداف والأنشطة ذكياً (AI)")
                }
            }

            // Objectives Inputs Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("2. الأهداف السلوكية والأنشطة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = cognitiveObjectives,
                        onValueChange = { cognitiveObjectives = it },
                        label = { Text("الأهداف المعرفية") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = skillObjectives,
                        onValueChange = { skillObjectives = it },
                        label = { Text("الأهداف المهارية") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = affectiveObjectives,
                        onValueChange = { affectiveObjectives = it },
                        label = { Text("الأهداف الوجدانية والقيم") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = introduction,
                        onValueChange = { introduction = it },
                        label = { Text("التهيئة والمقدمة والربط بالدرس السابق") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = teachingSteps,
                        onValueChange = { teachingSteps = it },
                        label = { Text("سير خطوات الدرس والأنشطة الصفية") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formativeAssessment,
                        onValueChange = { formativeAssessment = it },
                        label = { Text("التقويم التكويني") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = homework,
                        onValueChange = { homework = it },
                        label = { Text("الواجب المنزلي والتكليفات") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Save Lesson Plan Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "يرجى إدخال عنوان الدرس", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    coroutineScope.launch {
                        val entity = LessonEntity(
                            id = 0,
                            teacherId = teacher.id,
                            title = title.trim(),
                            subject = subject,
                            grade = grade,
                            durationMinutes = durationMinutes,
                            schoolName = schoolName.trim(),
                            directorate = teacher.directorate,
                            governorate = teacher.governorate,
                            term = term,
                            unitName = unitName.trim(),
                            lessonDate = lessonDate,
                            periodNumber = periodNumber,
                            studentLevel = studentLevel,
                            preparationType = prepType,
                            cognitiveObjectives = cognitiveObjectives,
                            skillObjectives = skillObjectives,
                            affectiveObjectives = affectiveObjectives,
                            introduction = introduction,
                            teachingSteps = teachingSteps,
                            formativeAssessment = formativeAssessment,
                            homework = homework,
                            teacherNotes = teacherNotes,
                            textbookReference = textbookRef,
                            curriculumEdition = teacher.curriculumEdition
                        )
                        repository.insertLesson(entity)
                        isSaving = false
                        Toast.makeText(context, "تم حفظ خطة الدرس بنجاح في قاعدة البيانات!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_lesson_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ خطة الدرس", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
