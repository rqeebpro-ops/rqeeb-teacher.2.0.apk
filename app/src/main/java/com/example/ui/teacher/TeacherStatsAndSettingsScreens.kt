package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiService
import com.example.data.model.TeacherEntity
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.StatusApproved
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// MODULE 16: الإحصائيات والتقارير (Teacher Statistics)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStatsScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val lessons by repository.getLessons(teacher.id).collectAsState(initial = emptyList())
    val exams by repository.getExams(teacher.id).collectAsState(initial = emptyList())
    val students by repository.getStudents(teacher.id).collectAsState(initial = emptyList())
    val homeworks by repository.getHomework(teacher.id).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تقارير وإحصائيات المعلم") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("stats_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "مؤشرات الأداء التربوي والإنتاجية",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "ملخص ما تم إنجازه عبر منصة رقيب خلال الفصل الدراسي",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("الدروس المحضرة", "${lessons.size}", Icons.Default.MenuBook, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatCard("نماذج الاختبارات", "${exams.size}", Icons.Default.Quiz, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("الطلاب المسجلون", "${students.size}", Icons.Default.People, StatusApproved, Modifier.weight(1f))
                    StatCard("الواجبات المسندة", "${homeworks.size}", Icons.Default.Assignment, Color(0xFFD97706), Modifier.weight(1f))
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("مستويات حضور الطلاب العامة", fontWeight = FontWeight.Bold)
                        val presentCount = students.count { it.attendanceStatus == "PRESENT" }
                        val lateCount = students.count { it.attendanceStatus == "LATE" }
                        val absentCount = students.count { it.attendanceStatus == "ABSENT" }
                        val total = students.size.coerceAtLeast(1)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("نسبة الحضور الإجمالية:", fontSize = 12.sp)
                            Text("${(presentCount * 100) / total}%", fontWeight = FontWeight.Bold, color = StatusApproved, fontSize = 12.sp)
                        }
                        LinearProgressIndicator(
                            progress = { presentCount.toFloat() / total.toFloat() },
                            color = StatusApproved,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------
// MODULE 17: إعدادات المعلم (Teacher Settings & Profile)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSettingsScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    geminiService: GeminiService,
    onTeacherUpdated: (TeacherEntity) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(teacher.fullName) }
    var school by remember { mutableStateOf(teacher.schoolName) }
    var subject by remember { mutableStateOf(teacher.subject) }
    var gov by remember { mutableStateOf(teacher.governorate) }
    var isSaved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات المعلم والملف الشخصي") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("البيانات المهنية للمعلم", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(value = name, onValueChange = { name = it; isSaved = false }, label = { Text("الاسم الكامل") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = school, onValueChange = { school = it; isSaved = false }, label = { Text("المدرسة / المؤسسة التعليمية") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = subject, onValueChange = { subject = it; isSaved = false }, label = { Text("مادة التدريس") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = gov, onValueChange = { gov = it; isSaved = false }, label = { Text("المحافظة") }, singleLine = true, modifier = Modifier.weight(1f))
                        }

                        if (isSaved) {
                            Text("تم حفظ التعديلات بنجاح!", color = StatusApproved, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val updated = teacher.copy(
                                        fullName = name.trim(),
                                        schoolName = school.trim(),
                                        subject = subject.trim(),
                                        governorate = gov.trim()
                                    )
                                    repository.updateTeacher(updated)
                                    onTeacherUpdated(updated)
                                    isSaved = true
                                    Toast.makeText(context, "تم تحديث الملف الشخصي", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ التغييرات")
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("حالة خادم الذكاء الاصطناعي (Gemini)", fontWeight = FontWeight.Bold)
                        Text(
                            if (geminiService.isConfigured()) "مفتاح Gemini API متصل ونشط." else "يعمل في وضع المحاكي الذكي المدمج (يمكنك ضبط GEMINI_API_KEY في لوحة Secrets).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج من الحساب")
                }
            }
        }
    }
}
