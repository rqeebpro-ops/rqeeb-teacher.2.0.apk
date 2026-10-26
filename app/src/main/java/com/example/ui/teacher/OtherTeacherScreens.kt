package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiService
import com.example.data.model.*
import com.example.data.repository.RqeebRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val slots by repository.getTimetable(teacher.id).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الجدول الأسبوعي للحصص") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("توزيع الحصص الأسبوعية للمدرس: ${teacher.fullName}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            val days = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
            items(days) { day ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(day, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val daySlots = slots.filter { it.dayOfWeek == day }
                        if (daySlots.isEmpty()) {
                            Text("الحصة 1-2: ${teacher.subject} - ${teacher.gradeTaught} (شعبة ${teacher.section.ifBlank { "أ" }})", fontSize = 13.sp)
                        } else {
                            daySlots.forEach { s ->
                                Text("الحصة ${s.periodIndex}: ${s.subject} (${s.grade})", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val homeworkList by repository.getHomework(teacher.id).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الواجبات والتكليفات المنزلية") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إضافة واجب مدرسي جديد", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان الواجب") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("تفاصيل التكليف والتمارين") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = page, onValueChange = { page = it }, label = { Text("الصفحة ورقم التمرين في الكتاب المدرسي") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                if (title.isBlank()) return@Button
                                coroutineScope.launch {
                                    repository.insertHomework(
                                        HomeworkEntity(
                                            id = 0,
                                            teacherId = teacher.id,
                                            title = title.trim(),
                                            subject = teacher.subject,
                                            grade = teacher.gradeTaught,
                                            section = teacher.section,
                                            description = desc.trim(),
                                            dueDate = "غداً",
                                            textbookPage = page.trim()
                                        )
                                    )
                                    title = ""
                                    desc = ""
                                    page = ""
                                    Toast.makeText(context, "تم حفظ الواجب", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ وتوثيق الواجب")
                        }
                    }
                }
            }

            items(homeworkList) { hw ->
                Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(hw.title, fontWeight = FontWeight.Bold)
                        Text(hw.description, fontSize = 13.sp)
                        if (hw.textbookPage.isNotBlank()) {
                            Text("الكتاب المدرسي: ص ${hw.textbookPage}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradesScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val students by repository.getStudents(teacher.id).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var seat by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("كشف درجات وسجل الطلاب") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إضافة طالب جديد لكشف الفصل (${teacher.gradeTaught} - شعبة ${teacher.section.ifBlank { "أ" }})", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الطالب الرباعي") }, modifier = Modifier.weight(2f))
                            OutlinedTextField(value = seat, onValueChange = { seat = it }, label = { Text("رقم الجلوس") }, modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                if (name.isBlank()) return@Button
                                coroutineScope.launch {
                                    repository.insertStudent(
                                        StudentEntity(
                                            id = 0,
                                            teacherId = teacher.id,
                                            fullName = name.trim(),
                                            seatNumber = seat.trim(),
                                            grade = teacher.gradeTaught,
                                            section = teacher.section.ifBlank { "أ" }
                                        )
                                    )
                                    name = ""
                                    seat = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إضافة الطالب")
                        }
                    }
                }
            }

            items(students) { st ->
                Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(st.fullName, fontWeight = FontWeight.Bold)
                            Text("رقم الجلوس: ${st.seatNumber.ifBlank { "غير محدد" }} • شعبة ${st.section}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { coroutineScope.launch { repository.deleteStudent(st) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermPlanScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الخطة والتوزيع الزمني للمقرر") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("توزيع مقرر مادة ${teacher.subject} لصف ${teacher.gradeTaught} (الفصل الدراسي الأول):", fontWeight = FontWeight.Bold)
            }
            val weeks = listOf(
                "الأسبوع 1: التهيئة والمفاهيم التأسيسية ومراجعة المكتسبات",
                "الأسبوع 2: الوحدة الأولى - الدرس الأول والثاني",
                "الأسبوع 3: الوحدة الأولى - الدرس الثالث وحل التدريبات",
                "الأسبوع 4: اختبار شهري قصير + تطبيقات عملية",
                "الأسبوع 5: الوحدة الثانية - الدرس الأول",
                "الأسبوع 6: الوحدة الثانية - الدرس الثاني والأنشطة الإثرائية"
            )
            items(weeks) { w ->
                Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(w, modifier = Modifier.padding(14.dp), fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsScreen(
    teacher: TeacherEntity,
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    var prompt by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المساعد التربوي الذكي") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("اسأل المساعد التربوي حول استراتيجيات التدريس والأنشطة:", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("السؤال أو الفكرة التربوية") },
                placeholder = { Text("مثال: اقترح نشاطاً استكشافياً لدرس فيزياء / رياضيات") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (prompt.isBlank()) return@Button
                    isLoading = true
                    coroutineScope.launch {
                        result = "اقتراح رقيب التربوي وفق المنهج اليمني لمادة ${teacher.subject}:\n" +
                                "1. تقسيم الطلاب إلى مجموعات ثنائية واستخدام العصف الذهني.\n" +
                                "2. ربط الفكرة بتطبيق واقعي من البيئة المحلية في اليمن.\n" +
                                "3. إعطاء الطلاب 5 دقائق للتجريب والتفكير الذاتي قبل الاستنتاج."
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("توليد الإجابة التربوية")
            }
            if (result.isNotBlank()) {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(result, modifier = Modifier.padding(14.dp), lineHeight = 22.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonArchiveScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val lessons by repository.getLessons(teacher.id).collectAsState(initial = emptyList())
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أرشيف الدروس المحضرة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(lessons) { l ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(l.title, fontWeight = FontWeight.Bold)
                            Text("${l.subject} • ${l.grade} • ${l.lessonDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { com.example.util.PrintUtils.printLesson(context, l) }) {
                            Icon(Icons.Default.Print, contentDescription = "طباعة")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OralExamScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler { onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تقييم الاختبارات الشفوية") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Text("نظام تقييم التلاوة والمطالعة الشفوية وفق سلم التقدير اليمني المعتمد (الدرجة من 10)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المعايير المعتمدة:")
                    Text("• صحة مخارج الحروف والضبط الإعرابي: (4 درجات)")
                    Text("• الطلاقة والاسترسال دون تردد: (3 درجات)")
                    Text("• تمثيل المعنى والتفاعل: (3 درجات)")
                }
            }
        }
    }
}
