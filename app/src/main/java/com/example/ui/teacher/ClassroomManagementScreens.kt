package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuspended
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// MODULE 9: الواجبات المنزلية (Homework Screen)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val homeworkList by repository.getHomework(teacher.id).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة الواجبات والمهام المنزلية") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("hw_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_hw")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة واجب")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (homeworkList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد واجبات منزلية مسجلة حالياً. اضغط على (+) لإضافة واجب جديد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(homeworkList, key = { it.id }) { hw ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(hw.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text("الشعبة: ${hw.className} | موعد التسليم: ${hw.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = {
                                        coroutineScope.launch { repository.deleteHomework(hw.id) }
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(hw.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = hw.isCompleted,
                                        onCheckedChange = { checked ->
                                            coroutineScope.launch {
                                                repository.updateHomework(hw.copy(isCompleted = checked))
                                            }
                                        }
                                    )
                                    Text(if (hw.isCompleted) "تم الاستلام وتصحيح الواجب" else "قيد المتابعة والتسليم", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var hTitle by remember { mutableStateOf("") }
        var hClass by remember { mutableStateOf("9/1") }
        var hDesc by remember { mutableStateOf("") }
        var hDue by remember { mutableStateOf("الحصة القادمة") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إسناد واجب منزلي جديد", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = hTitle, onValueChange = { hTitle = it }, label = { Text("عنوان الواجب *") }, placeholder = { Text("مثال: حل تمارين ص 45") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hClass, onValueChange = { hClass = it }, label = { Text("الشعبة / الصف") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = hDue, onValueChange = { hDue = it }, label = { Text("موعد التسليم") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = hDesc, onValueChange = { hDesc = it }, label = { Text("تفاصيل وتعليمات الواجب") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) { Text("إلغاء") }
                        Button(
                            onClick = {
                                if (hTitle.isNotBlank()) {
                                    coroutineScope.launch {
                                        repository.saveHomework(
                                            HomeworkEntity(
                                                teacherId = teacher.id,
                                                className = hClass.trim(),
                                                title = hTitle.trim(),
                                                description = hDesc.trim(),
                                                dueDate = hDue.trim()
                                            )
                                        )
                                        showAddDialog = false
                                        Toast.makeText(context, "تم إسناد الواجب بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("إسناد الواجب") }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 10: درجات الطلاب (Student Grades)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradesScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val students by repository.getStudents(teacher.id).collectAsState(initial = emptyList())
    var selectedClass by remember { mutableStateOf("9/1") }

    val classStudents = remember(students, selectedClass) {
        students.filter { it.className == selectedClass }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("كشف درجات الطلاب وأعمال السنة") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("grades_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (classStudents.isNotEmpty()) {
                        IconButton(onClick = {
                            val report = StringBuilder("كشف درجات الطلاب - مدرسة ${teacher.schoolName} - شعبة $selectedClass\n")
                            classStudents.forEach { s ->
                                report.append("${s.studentName}: اختبار قصير ${s.quizScore} | واجبات ${s.homeworkScore} | اختبار فصلي ${s.examScore} | المجموع: ${s.totalScore.toInt()}/40 (${s.percentage}%)\n")
                            }
                            PrintUtils.shareText(context, "كشف الدرجات", report.toString())
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "تصدير الكشف")
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("اختر الشعبة:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                listOf("9/1", "9/2", "9/3", "1/أ").forEach { cls ->
                    FilterChip(
                        selected = selectedClass == cls,
                        onClick = { selectedClass = cls },
                        label = { Text(cls) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Class Average Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${classStudents.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("الطلاب", fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val avg = if (classStudents.isNotEmpty()) classStudents.map { it.percentage }.average().toInt() else 0
                        Text("$avg%", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StatusApproved)
                        Text("متوسط التحصيل", fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val topScore = classStudents.maxOfOrNull { it.totalScore } ?: 0.0
                        Text("${topScore.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("أعلى درجة", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(classStudents, key = { it.id }) { s ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("${s.totalScore.toInt()}/${s.totalMaxScore.toInt()} (${s.percentage}%)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("قصير: ${s.quizScore}/10", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("واجبات: ${s.homeworkScore}/10", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("فصلي: ${s.examScore}/20", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 11: الحضور والغياب (Attendance Screen)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val coroutineScope = rememberCoroutineScope()
    val students by repository.getStudents(teacher.id).collectAsState(initial = emptyList())
    var selectedClass by remember { mutableStateOf("9/1") }

    val classStudents = remember(students, selectedClass) {
        students.filter { it.className == selectedClass }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سجل الحضور والغياب اليومي") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("attendance_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الشعبة:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                listOf("9/1", "9/2", "9/3", "1/أ").forEach { cls ->
                    FilterChip(
                        selected = selectedClass == cls,
                        onClick = { selectedClass = cls },
                        label = { Text(cls) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attendance KPI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val present = classStudents.count { it.attendanceStatus == StudentEntity.ATTENDANCE_PRESENT }
                val late = classStudents.count { it.attendanceStatus == StudentEntity.ATTENDANCE_LATE }
                val absent = classStudents.count { it.attendanceStatus == StudentEntity.ATTENDANCE_ABSENT }

                MiniKpi("الحاضرون", "$present", StatusApproved, Modifier.weight(1f))
                MiniKpi("المتأخرون", "$late", StatusPending, Modifier.weight(1f))
                MiniKpi("الغائبون", "$absent", StatusSuspended, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(classStudents, key = { it.id }) { s ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(s.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AttendanceStatusBtn(
                                    label = "حاضر",
                                    selected = s.attendanceStatus == StudentEntity.ATTENDANCE_PRESENT,
                                    color = StatusApproved,
                                    onClick = {
                                        coroutineScope.launch { repository.updateStudent(s.copy(attendanceStatus = StudentEntity.ATTENDANCE_PRESENT)) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AttendanceStatusBtn(
                                    label = "متأخر",
                                    selected = s.attendanceStatus == StudentEntity.ATTENDANCE_LATE,
                                    color = StatusPending,
                                    onClick = {
                                        coroutineScope.launch { repository.updateStudent(s.copy(attendanceStatus = StudentEntity.ATTENDANCE_LATE)) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                AttendanceStatusBtn(
                                    label = "غائب",
                                    selected = s.attendanceStatus == StudentEntity.ATTENDANCE_ABSENT,
                                    color = StatusSuspended,
                                    onClick = {
                                        coroutineScope.launch { repository.updateStudent(s.copy(attendanceStatus = StudentEntity.ATTENDANCE_ABSENT)) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniKpi(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AttendanceStatusBtn(label: String, selected: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) color else color.copy(alpha = 0.12f),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// -------------------------------------------------------------
// MODULE 12: جدول الحصص الأسبوعي (Weekly Timetable)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val timetableSlots by repository.getTimetable(teacher.id).collectAsState(initial = emptyList())
    var selectedDay by remember { mutableIntStateOf(0) }
    val days = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس")

    val daySlots = remember(timetableSlots, selectedDay) {
        timetableSlots.filter { it.dayOfWeek == selectedDay }.sortedBy { it.periodNumber }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جدول الحصص الأسبوعي المدرسي") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("timetable_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedDay, modifier = Modifier.fillMaxWidth()) {
                days.forEachIndexed { index, dayName ->
                    Tab(
                        selected = selectedDay == index,
                        onClick = { selectedDay = index },
                        text = { Text(dayName, fontSize = 11.sp, fontWeight = if (selectedDay == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("حصص يوم ${days[selectedDay]}:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..7).toList()) { period ->
                    val slot = daySlots.find { it.periodNumber == period }
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (slot != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = if (slot != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("$period", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            if (slot != null) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(slot.subject, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("شعبة: ${slot.className} | المكان: ${slot.room}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Text("فترة راحة أو فراغ مكتبي", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 8: الاختبارات الشفوية (Oral Exam Rubric & Random Selector)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OralExamScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var studentName by remember { mutableStateOf("") }
    val yemeniSampleRoster = listOf(
        "محمد عبد الله الحاشدي", "أسامة علي الصنعاني", "إبراهيم وليد الهمداني",
        "حمزة صالح الحضرمي", "يحيى قاسم التعزي", "بلال سالم العدني", "عمر فؤاد الإبي"
    )

    // Rubric Scores (0..5)
    var pronunciationScore by remember { mutableIntStateOf(4) }
    var comprehensionScore by remember { mutableIntStateOf(5) }
    var fluencyScore by remember { mutableIntStateOf(4) }
    var accuracyScore by remember { mutableIntStateOf(5) }

    val totalOralScore = pronunciationScore + comprehensionScore + fluencyScore + accuracyScore

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الاختبارات الشفوية ومصفوفة التقييم (Rubric)") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("oral_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("اسم الطالب المقيم") },
                    placeholder = { Text("مثال: محمد الحاشدي") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    studentName = yemeniSampleRoster.random()
                    Toast.makeText(context, "تم اختيار الطالب عشوائياً", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("سحب عشوائي")
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("معايير التقييم الشفوي المعتمدة (الحد الأقصى 20 درجة)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    CriteriaRow("سلامة النطق ومخارج الحروف (0-5)", pronunciationScore) { pronunciationScore = it }
                    CriteriaRow("الفهم والاستيعاب للمضمون العلمي (0-5)", comprehensionScore) { comprehensionScore = it }
                    CriteriaRow("الاسترسال والطلاقة التعبيرية (0-5)", fluencyScore) { fluencyScore = it }
                    CriteriaRow("دقة الإجابة وصحة القواعد (0-5)", accuracyScore) { accuracyScore = it }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الدرجة الشفوية النهائية:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("$totalOralScore / 20", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (studentName.isNotBlank()) {
                        Toast.makeText(context, "تم رصد نتيجة التقييم الشفوي للطالب $studentName بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "يرجى تحديد أو سحب اسم الطالب أولاً", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("اعتماد ورصد الدرجة الشفوية")
            }
        }
    }
}

@Composable
fun CriteriaRow(label: String, score: Int, onScoreChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp)
            Text("$score نقاط", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
        Slider(
            value = score.toFloat(),
            onValueChange = { onScoreChange(it.toInt()) },
            valueRange = 0f..5f,
            steps = 4,
            modifier = Modifier.height(26.dp)
        )
    }
}
