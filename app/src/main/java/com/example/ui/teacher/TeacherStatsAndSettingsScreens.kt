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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiService
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("إحصائيات وتقارير المعلم") },
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "ملخص الإنجاز التعليمي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard("الدروس المحضرة", "${lessons.size}", Icons.Default.Description, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatMetricCard("الاختبارات المنشأة", "${exams.size}", Icons.Default.Quiz, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard("الطلاب المضافون", "${students.size}", Icons.Default.People, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                        StatMetricCard("الواجبات اليومية", "${homeworks.size}", Icons.Default.Assignment, Color(0xFF0D9488), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
    var subject by remember { mutableStateOf(teacher.subject.ifBlank { "الرياضيات" }) }
    var gov by remember { mutableStateOf(teacher.governorate.ifBlank { "أمانة العاصمة" }) }
    var grade by remember { mutableStateOf(YemeniCurriculum.toDisplayGrade(teacher.gradeTaught.ifBlank { "9/ إعدادي" })) }

    val standardShortSections = listOf("أ", "ب", "ج", "د", "هـ", "و")
    val initialOption = when {
        teacher.section.isBlank() -> "لم تُحدد"
        standardShortSections.contains(teacher.section.trim()) -> teacher.section.trim()
        else -> "أخرى"
    }
    var selectedSectionOption by remember { mutableStateOf(initialOption) }
    var customSection by remember { mutableStateOf(if (initialOption == "أخرى") teacher.section else "") }
    var isSaved by remember { mutableStateOf(false) }

    var govExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    val sectionDropdownOptions = listOf("لم تُحدد", "أ", "ب", "ج", "د", "هـ", "و", "أخرى")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "إعدادات المعلم والملف الشخصي",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Profile Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = teacher.fullName.ifBlank { "المعلم" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "رقم الهاتف: ${teacher.phone} • ${teacher.status}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "طبعة المنهج: ${teacher.curriculumEdition}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Professional Data Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "البيانات المهنية والتعليمية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // 1. Full Name
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; isSaved = false },
                                label = { Text("الاسم الكامل") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_teacher_name")
                            )

                            // 2. School Name
                            OutlinedTextField(
                                value = school,
                                onValueChange = { school = it; isSaved = false },
                                label = { Text("المدرسة / المؤسسة التعليمية") },
                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_school_name")
                            )

                            // 3. Paired Cards: Governorate & Subject (Strictly Equal Width and Height)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Governorate Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = govExpanded,
                                    onExpandedChange = { govExpanded = !govExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = gov,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("المحافظة") },
                                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = govExpanded) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("settings_governorate_field")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = govExpanded,
                                        onDismissRequest = { govExpanded = false }
                                    ) {
                                        YemeniCurriculum.GOVERNORATES.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text(item) },
                                                onClick = {
                                                    gov = item
                                                    govExpanded = false
                                                    isSaved = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Subject Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = subjectExpanded,
                                    onExpandedChange = { subjectExpanded = !subjectExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = subject,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("مادة التدريس") },
                                        leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("settings_subject_field")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = subjectExpanded,
                                        onDismissRequest = { subjectExpanded = false }
                                    ) {
                                        YemeniCurriculum.SUBJECTS.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text(item) },
                                                onClick = {
                                                    subject = item
                                                    subjectExpanded = false
                                                    isSaved = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Paired Cards: Grade & Section (Strictly Equal Width and Height)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Grade Dropdown (Using 12 official display names)
                                ExposedDropdownMenuBox(
                                    expanded = gradeExpanded,
                                    onExpandedChange = { gradeExpanded = !gradeExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = grade,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("الصف الدراسي") },
                                        leadingIcon = { Icon(Icons.Default.Class, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("settings_grade_field")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = gradeExpanded,
                                        onDismissRequest = { gradeExpanded = false }
                                    ) {
                                        YemeniCurriculum.DISPLAY_GRADES.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text(item) },
                                                onClick = {
                                                    grade = item
                                                    gradeExpanded = false
                                                    isSaved = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Section Dropdown (أ، ب، ج، د، هـ، و، أخرى، لم تُحدد)
                                ExposedDropdownMenuBox(
                                    expanded = sectionExpanded,
                                    onExpandedChange = { sectionExpanded = !sectionExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val sectionDisplayValue = when {
                                        selectedSectionOption == "لم تُحدد" -> "لم تُحدد"
                                        selectedSectionOption == "أخرى" -> if (customSection.isNotBlank()) customSection else "أخرى"
                                        else -> "شعبة $selectedSectionOption"
                                    }
                                    OutlinedTextField(
                                        value = sectionDisplayValue,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("الشعبة") },
                                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("settings_section_field")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = sectionExpanded,
                                        onDismissRequest = { sectionExpanded = false }
                                    ) {
                                        sectionDropdownOptions.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text(if (item == "لم تُحدد" || item == "أخرى") item else "شعبة $item") },
                                                onClick = {
                                                    selectedSectionOption = item
                                                    sectionExpanded = false
                                                    isSaved = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Custom Section Name Input (Shown when 'أخرى' is selected)
                            if (selectedSectionOption == "أخرى") {
                                OutlinedTextField(
                                    value = customSection,
                                    onValueChange = { customSection = it; isSaved = false },
                                    label = { Text("اسم الشعبة المخصصة") },
                                    placeholder = { Text("مثال: شعبة المتفوقين أو د/2") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_custom_section_input")
                                )
                            }

                            if (isSaved) {
                                Surface(
                                    color = StatusApproved.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusApproved)
                                        Text(
                                            "تم حفظ بيانات المعلم والشعبة والصف بنجاح!",
                                            color = StatusApproved,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Save Changes Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val finalSection = when (selectedSectionOption) {
                                            "لم تُحدد" -> ""
                                            "أخرى" -> customSection.trim()
                                            else -> selectedSectionOption.trim()
                                        }
                                        val updated = teacher.copy(
                                            fullName = name.trim(),
                                            schoolName = school.trim(),
                                            subject = subject.trim(),
                                            governorate = gov.trim(),
                                            gradeTaught = grade.trim(),
                                            section = finalSection,
                                            stage = YemeniCurriculum.getStageForDisplayGrade(grade.trim())
                                        )
                                        repository.updateTeacher(updated)
                                        onTeacherUpdated(updated)
                                        isSaved = true
                                        Toast.makeText(context, "تم حفظ التعديلات بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("save_profile_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ التغييرات", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Logout Button
                item {
                    OutlinedButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل الخروج من الحساب", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
