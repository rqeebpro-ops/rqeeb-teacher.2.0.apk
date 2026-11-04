package com.example.ui.teacher

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.util.WhatsAppUtils
import kotlinx.coroutines.launch

enum class FollowUpTab {
    STUDENTS_LIST,
    STUDENT_PROFILE,
    WHATSAPP_REPORTS,
    PARENT_DASHBOARD,
    NOTIFICATION_SETTINGS
}

/**
 * Complete Student Follow-up & Parent WhatsApp Communication Module («متابعة الطالب وولي الأمر»)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentFollowUpScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(FollowUpTab.STUDENTS_LIST) }

    // Live Student Data Streams from Repository
    val allStudents by repository.getStudents(teacher.id).collectAsState(initial = emptyList())
    var selectedGrade by remember { mutableStateOf(YemeniCurriculum.toDisplayGrade(teacher.gradeTaught)) }
    var selectedSection by remember { mutableStateOf(teacher.section.ifBlank { "أ" }) }
    var searchQuery by remember { mutableStateOf("") }

    // Filtered Students List
    val filteredStudents = remember(allStudents, selectedGrade, selectedSection, searchQuery) {
        allStudents.filter { s ->
            val matchGrade = selectedGrade.isBlank() || YemeniCurriculum.toDisplayGrade(s.grade) == selectedGrade || s.grade == selectedGrade
            val matchSection = selectedSection.isBlank() || s.section == selectedSection
            val matchQuery = searchQuery.isBlank() ||
                    s.fullName.contains(searchQuery, ignoreCase = true) ||
                    s.schoolNumber.contains(searchQuery, ignoreCase = true) ||
                    s.parentPhone.contains(searchQuery, ignoreCase = true)
            matchGrade && matchSection && matchQuery
        }
    }

    var selectedStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var showAddEditStudentDialog by remember { mutableStateOf(false) }

    // Communication & Attendance States
    val commLogs by repository.getCommunicationLogsByTeacher(teacher.id).collectAsState(initial = emptyList())
    val notifConfigFlow by repository.getParentNotificationConfigFlow(teacher.id).collectAsState(initial = null)

    // Primary WhatsApp Color Scheme
    val whatsappGreen = Color(0xFF25D366)
    val whatsappDarkGreen = Color(0xFF128C7E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "متابعة الطالب والتواصل مع ولي الأمر",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "${allStudents.size} طالب مسجل • ${allStudents.count { it.parentPhone.isNotBlank() }} مرتبط بواتساب ولي الأمر",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("followup_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedStudent = null
                            showAddEditStudentDialog = true
                        },
                        modifier = Modifier.testTag("add_student_top_btn")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "إضافة طالب جديد", tint = whatsappDarkGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Module Sub-Navigation Bar
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = activeTab == FollowUpTab.STUDENTS_LIST,
                    onClick = { activeTab = FollowUpTab.STUDENTS_LIST },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("الطلاب والربط", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == FollowUpTab.STUDENT_PROFILE,
                    onClick = {
                        if (selectedStudent == null && filteredStudents.isNotEmpty()) {
                            selectedStudent = filteredStudents.first()
                        }
                        activeTab = FollowUpTab.STUDENT_PROFILE
                    },
                    icon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    label = { Text("الملف الشامل", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == FollowUpTab.WHATSAPP_REPORTS,
                    onClick = {
                        if (selectedStudent == null && filteredStudents.isNotEmpty()) {
                            selectedStudent = filteredStudents.first()
                        }
                        activeTab = FollowUpTab.WHATSAPP_REPORTS
                    },
                    icon = { Icon(Icons.Default.Send, contentDescription = null, tint = whatsappDarkGreen) },
                    label = { Text("تقارير واتساب", fontSize = 10.sp, color = whatsappDarkGreen, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = activeTab == FollowUpTab.PARENT_DASHBOARD,
                    onClick = { activeTab = FollowUpTab.PARENT_DASHBOARD },
                    icon = { Icon(Icons.Default.Insights, contentDescription = null) },
                    label = { Text("سجل المتابعة", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == FollowUpTab.NOTIFICATION_SETTINGS,
                    onClick = { activeTab = FollowUpTab.NOTIFICATION_SETTINGS },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                    label = { Text("الإشعارات", fontSize = 10.sp) }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
        ) {
            when (activeTab) {
                // TAB 1: Students List & Parent Linking
                FollowUpTab.STUDENTS_LIST -> {
                    StudentsListAndLinkingTab(
                        students = filteredStudents,
                        allGrades = YemeniCurriculum.ALL_GRADES.map { YemeniCurriculum.toDisplayGrade(it) },
                        selectedGrade = selectedGrade,
                        selectedSection = selectedSection,
                        searchQuery = searchQuery,
                        whatsappGreen = whatsappGreen,
                        whatsappDarkGreen = whatsappDarkGreen,
                        onGradeSelected = { selectedGrade = it },
                        onSectionSelected = { selectedSection = it },
                        onSearchQueryChanged = { searchQuery = it },
                        onStudentSelected = { s ->
                            selectedStudent = s
                            activeTab = FollowUpTab.STUDENT_PROFILE
                        },
                        onEditStudent = { s ->
                            selectedStudent = s
                            showAddEditStudentDialog = true
                        },
                        onToggleFollowed = { s ->
                            coroutineScope.launch {
                                repository.updateStudent(s.copy(isFollowedUp = !s.isFollowedUp))
                            }
                        },
                        onAddNewStudent = {
                            selectedStudent = null
                            showAddEditStudentDialog = true
                        }
                    )
                }

                // TAB 2: Comprehensive Student Profile
                FollowUpTab.STUDENT_PROFILE -> {
                    if (selectedStudent != null) {
                        StudentComprehensiveProfileTab(
                            student = selectedStudent!!,
                            teacher = teacher,
                            repository = repository,
                            whatsappDarkGreen = whatsappDarkGreen,
                            onOpenWhatsApp = {
                                activeTab = FollowUpTab.WHATSAPP_REPORTS
                            },
                            onEditStudent = {
                                showAddEditStudentDialog = true
                            }
                        )
                    } else {
                        NoStudentSelectedPlaceholder {
                            activeTab = FollowUpTab.STUDENTS_LIST
                        }
                    }
                }

                // TAB 3: WhatsApp Reports Sending (12 Report Types)
                FollowUpTab.WHATSAPP_REPORTS -> {
                    if (selectedStudent != null) {
                        WhatsAppReportsTab(
                            student = selectedStudent!!,
                            teacher = teacher,
                            repository = repository,
                            geminiService = geminiService,
                            whatsappGreen = whatsappGreen,
                            whatsappDarkGreen = whatsappDarkGreen
                        )
                    } else {
                        NoStudentSelectedPlaceholder {
                            activeTab = FollowUpTab.STUDENTS_LIST
                        }
                    }
                }

                // TAB 4: Parent Follow-up Dashboard & Log History
                FollowUpTab.PARENT_DASHBOARD -> {
                    ParentFollowUpDashboardTab(
                        teacher = teacher,
                        allStudents = allStudents,
                        commLogs = commLogs,
                        repository = repository,
                        whatsappDarkGreen = whatsappDarkGreen
                    )
                }

                // TAB 5: Instant Notification Config Settings
                FollowUpTab.NOTIFICATION_SETTINGS -> {
                    InstantNotificationSettingsTab(
                        teacher = teacher,
                        repository = repository,
                        whatsappDarkGreen = whatsappDarkGreen
                    )
                }
            }
        }
    }

    // Add / Edit Student Dialog
    if (showAddEditStudentDialog) {
        AddEditStudentDialog(
            student = selectedStudent,
            teacher = teacher,
            onDismiss = { showAddEditStudentDialog = false },
            onSave = { updatedStudent ->
                coroutineScope.launch {
                    if (selectedStudent != null) {
                        repository.updateStudent(updatedStudent)
                        Toast.makeText(context, "تم تحديث بيانات الطالب وولي الأمر بنجاح!", Toast.LENGTH_SHORT).show()
                    } else {
                        repository.insertStudent(updatedStudent)
                        Toast.makeText(context, "تم إضافة الطالب وربطه بولي الأمر بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                    showAddEditStudentDialog = false
                }
            }
        )
    }
}

/**
 * Sub-Tab 1: Students List & Parent Linking
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentsListAndLinkingTab(
    students: List<StudentEntity>,
    allGrades: List<String>,
    selectedGrade: String,
    selectedSection: String,
    searchQuery: String,
    whatsappGreen: Color,
    whatsappDarkGreen: Color,
    onGradeSelected: (String) -> Unit,
    onSectionSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onStudentSelected: (StudentEntity) -> Unit,
    onEditStudent: (StudentEntity) -> Unit,
    onToggleFollowed: (StudentEntity) -> Unit,
    onAddNewStudent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & Filters Header Box
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("ابحث باسم الطالب، الرقم المدرسي، أو رقم الهاتف...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_students_input")
                )

                // Grade & Section Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Grade Selector
                    var expandedGrade by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expandedGrade = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (selectedGrade.isNotBlank()) selectedGrade else "كل الصفوف", fontSize = 11.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expandedGrade, onDismissRequest = { expandedGrade = false }) {
                            DropdownMenuItem(
                                text = { Text("كل الصفوف", fontSize = 11.sp) },
                                onClick = {
                                    onGradeSelected("")
                                    expandedGrade = false
                                }
                            )
                            allGrades.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g, fontSize = 11.sp) },
                                    onClick = {
                                        onGradeSelected(g)
                                        expandedGrade = false
                                    }
                                )
                            }
                        }
                    }

                    // Section Selector
                    listOf("أ", "ب", "ج", "د").forEach { sec ->
                        FilterChip(
                            selected = selectedSection == sec,
                            onClick = { onSectionSelected(if (selectedSection == sec) "" else sec) },
                            label = { Text("شعبة $sec", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Add New Student Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("قائمة الطلاب (${students.size}):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Button(
                onClick = onAddNewStudent,
                colors = ButtonDefaults.buttonColors(containerColor = whatsappDarkGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_student_main_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة طالب وولي أمر", fontSize = 11.sp)
            }
        }

        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا يوجد طلاب مسجلون يطابقون خيارات البحث.", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onAddNewStudent) {
                        Text("إضافة أول طالب برقم ولي الأمر")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(students) { student ->
                    StudentItemCard(
                        student = student,
                        whatsappGreen = whatsappGreen,
                        whatsappDarkGreen = whatsappDarkGreen,
                        onSelect = { onStudentSelected(student) },
                        onEdit = { onEditStudent(student) },
                        onToggleFollowed = { onToggleFollowed(student) }
                    )
                }
            }
        }
    }
}

/**
 * Individual Student Item Card in List
 */
@Composable
private fun StudentItemCard(
    student: StudentEntity,
    whatsappGreen: Color,
    whatsappDarkGreen: Color,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onToggleFollowed: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("student_card_${student.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Student Avatar Badge
            Surface(
                shape = CircleShape,
                color = if (student.parentPhone.isNotBlank()) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        student.fullName.take(1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (student.parentPhone.isNotBlank()) whatsappDarkGreen else Color(0xFFD97706)
                    )
                }
            }

            // Student Name & Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (student.schoolNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE2E8F0)
                        ) {
                            Text("#${student.schoolNumber}", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }

                Text(
                    "الصف: ${student.grade} (${student.section}) • التقدير: ${student.generalRating}",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )

                if (student.parentPhone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = whatsappDarkGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${student.parentName.ifBlank { "ولي الأمر" }} (${student.parentRelationship}): ${student.parentPhone}",
                            fontSize = 10.sp,
                            color = whatsappDarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text("⚠️ لم يتم ربط رقم واتساب ولي الأمر بعد", fontSize = 10.sp, color = Color(0xFFDC2626))
                }
            }

            // Quick Action Buttons
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                }
                Checkbox(
                    checked = student.isFollowedUp,
                    onCheckedChange = { onToggleFollowed() }
                )
            }
        }
    }
}

/**
 * Sub-Tab 2: Comprehensive Student Profile
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentComprehensiveProfileTab(
    student: StudentEntity,
    teacher: TeacherEntity,
    repository: RqeebRepository,
    whatsappDarkGreen: Color,
    onOpenWhatsApp: () -> Unit,
    onEditStudent: () -> Unit
) {
    val context = LocalContext.current

    // Fetch Attendance & Behavior Notes
    val attendanceList by repository.getStudentAttendance(student.id).collectAsState(initial = emptyList())
    val behaviorNotes by repository.getStudentBehaviorNotes(student.id).collectAsState(initial = emptyList())

    val presentCount = attendanceList.count { it.status == "حاضر" }
    val absentCount = attendanceList.count { it.status == "غائب" }
    val lateCount = attendanceList.count { it.status == "متأخر" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header Info Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = Color(0xFFDCFCE7), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(student.fullName.take(1), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = whatsappDarkGreen)
                            }
                        }
                        Column {
                            Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("الرقم المدرسي: ${student.schoolNumber.ifBlank { "غير مسجل" }} • الصف: ${student.grade} (${student.section})", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = onEditStudent) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل البيانات")
                    }
                }

                HorizontalDivider()

                // Parent WhatsApp Card Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ولي الأمر الرئيسي: ${student.parentName.ifBlank { "غير محدد" }} (${student.parentRelationship})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("رقم واتساب: ${student.parentPhone.ifBlank { "غير مسجل" }}", fontSize = 11.sp, color = whatsappDarkGreen)
                    }
                    Button(
                        onClick = onOpenWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = whatsappDarkGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إرسال تقرير واتساب", fontSize = 11.sp)
                    }
                }
            }
        }

        // Academic Info Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📊 1. السجل الأكاديمي والدرجات:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreBadge("اختبار الشهر", "${student.monthlyTestScore} / 20")
                    ScoreBadge("منتصف الفصل", "${student.termScore} / 20")
                    ScoreBadge("التقييم النهائي", "${student.finalExamScore} / 20")
                    ScoreBadge("الواجبات", "${student.homeworkScore} / 10")
                    ScoreBadge("المشاركة", "${student.activityScore} / 10")
                    ScoreBadge("التقدير العام", student.generalRating, isHighlight = true)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("🌟 نقاط القوة والتميز:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(student.strengths.ifBlank { "مواظبة وانضباط وتفاعل جيد بالحصة." }, fontSize = 11.sp, color = Color(0xFF15803D))

                Text("🎯 مهارات وتوصيات التحسين:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(student.weaknesses.ifBlank { "المراجعة المنزلية المستمرة وإكمال كتاب التمارين." }, fontSize = 11.sp, color = Color(0xFFC2410C))
            }
        }

        // Attendance Summary Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("⏱️ 2. ملخص الحضور والغياب والتأخر:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    StatPill("حضور", "$presentCount أيام", Color(0xFF16A34A))
                    StatPill("غياب", "$absentCount أيام", Color(0xFFDC2626))
                    StatPill("تأخر", "$lateCount مرات", Color(0xFFD97706))
                }
            }
        }

        // PDF Print / Share Action Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    PrintUtils.printStudentReport(
                        context = context,
                        student = student,
                        schoolName = teacher.schoolName,
                        teacherName = teacher.fullName,
                        subjectName = teacher.subject
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("طباعة تقرير PDF", fontSize = 11.sp)
            }

            Button(
                onClick = onOpenWhatsApp,
                colors = ButtonDefaults.buttonColors(containerColor = whatsappDarkGreen),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تجهيز رسالة واتساب", fontSize = 11.sp)
            }
        }
    }
}

/**
 * Score Badge Helper
 */
@Composable
private fun ScoreBadge(title: String, score: String, isHighlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlight) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isHighlight) Color(0xFF16A34A) else Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 9.sp, color = Color.Gray)
            Text(score, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isHighlight) Color(0xFF15803D) else Color.Black)
        }
    }
}

/**
 * Stat Pill Helper
 */
@Composable
private fun StatPill(title: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 10.sp, color = Color.Gray)
        Text(count, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * Sub-Tab 3: WhatsApp Reports Sending (12 Report Types)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhatsAppReportsTab(
    student: StudentEntity,
    teacher: TeacherEntity,
    repository: RqeebRepository,
    geminiService: GeminiService,
    whatsappGreen: Color,
    whatsappDarkGreen: Color
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedReportType by remember { mutableStateOf("FULL_REPORT") }
    var customTeacherNotes by remember { mutableStateOf("") }
    var generatedMessage by remember(student, selectedReportType, customTeacherNotes) {
        mutableStateOf(
            WhatsAppUtils.buildReportMessage(
                reportType = selectedReportType,
                student = student,
                schoolName = teacher.schoolName,
                teacherName = teacher.fullName,
                subjectName = teacher.subject,
                customNotes = customTeacherNotes
            )
        )
    }

    var isGeneratingAIReport by remember { mutableStateOf(false) }

    val reportCategories = listOf(
        "FULL_REPORT" to "📊 تقرير شامل عن الطالب",
        "GRADES_REPORT" to "📈 كشف درجات الطالب",
        "ABSENCE_NOTICE" to "⚠️ إشعار بالغياب اليومي",
        "LATE_NOTICE" to "⏰ إشعار بالتأخر الصباحي",
        "BEHAVIOR_NOTE" to "📝 ملاحظة سلوكية وتربوية",
        "HOMEWORK_NOTICE" to "📖 واجب منزلي غير منجز",
        "EXAM_RESULT" to "🏆 نتيجة اختبار محدد",
        "LOW_SCORE_ALERT" to "🔻 إشعار بانخفاض المستوى",
        "EXCELLENCE_CONGRATS" to "🌟 إشعار بالتفوق والتميز",
        "UPCOMING_EXAM_REMINDER" to "📅 تنبيه بموعد اختبار قادم",
        "THANK_YOU_PARENT" to "💌 رسالة شكر وتقدير لولي الأمر",
        "CUSTOM_NOTE" to "✍️ ملاحظة خاصة يكتبها المعلم"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Target Student Info Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("الطالب المستهدف: ${student.fullName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("ولي الأمر: ${student.parentName.ifBlank { "غير محدد" }} (${student.parentPhone.ifBlank { "لم يسجل رقم" }})", fontSize = 11.sp, color = whatsappDarkGreen)
                }
                Surface(shape = CircleShape, color = Color(0xFFDCFCE7), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = whatsappDarkGreen, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 12 Report Action Types Chips
        Text("اختر نوع التقرير أو الإشعار لولي الأمر:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            reportCategories.forEach { (typeCode, label) ->
                FilterChip(
                    selected = selectedReportType == typeCode,
                    onClick = {
                        selectedReportType = typeCode
                    },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedReportType == typeCode) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Custom Teacher Notes Field
        OutlinedTextField(
            value = customTeacherNotes,
            onValueChange = { customTeacherNotes = it },
            label = { Text("إضافة ملاحظة خاصة من المعلم للرسالة") },
            placeholder = { Text("اكتب أي توجيه أو ملاحظة ترغب في إضافتها لنص التقرير...") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        // AI Smart Drafting Button
        OutlinedButton(
            onClick = {
                isGeneratingAIReport = true
                coroutineScope.launch {
                    try {
                        val aiDraft = geminiService.generateStudentPedagogicalReport(
                            student = student,
                            schoolName = teacher.schoolName,
                            teacherName = teacher.fullName,
                            subjectName = teacher.subject,
                            reportCategory = reportCategories.find { it.first == selectedReportType }?.second ?: "تقرير عام"
                        )
                        if (aiDraft.isNotBlank()) {
                            generatedMessage = aiDraft
                            Toast.makeText(context, "تمت صياغة التقرير التربوي بواسطة الذكاء الاصطناعي بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطأ في توليد التقرير: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isGeneratingAIReport = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGeneratingAIReport) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("صياغة تقرير تربوي بالذكاء الاصطناعي (Gemini)", fontSize = 11.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
        }

        // Generated Editable Message Preview Box
        Text("معاينة وتعديل نص الرسالة قبل الإرسال:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        OutlinedTextField(
            value = generatedMessage,
            onValueChange = { generatedMessage = it },
            minLines = 8,
            modifier = Modifier.fillMaxWidth()
        )

        // Sending Action Buttons
        Button(
            onClick = {
                if (student.parentPhone.isBlank()) {
                    Toast.makeText(context, "يرجى إضافة رقم هاتف ولي الأمر أولاً لتتمكن من فتح واتساب!", Toast.LENGTH_LONG).show()
                    return@Button
                }
                coroutineScope.launch {
                    // Log communication
                    val log = CommunicationLogEntity(
                        studentId = student.id,
                        teacherId = teacher.id,
                        studentName = student.fullName,
                        parentName = student.parentName,
                        parentPhone = student.parentPhone,
                        messageType = selectedReportType,
                        messageSummary = generatedMessage.take(50),
                        fullText = generatedMessage,
                        status = "OPENED_WHATSAPP"
                    )
                    repository.insertCommunicationLog(log)

                    WhatsAppUtils.openWhatsAppChat(
                        context = context,
                        rawPhone = student.parentPhone,
                        messageText = generatedMessage
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = whatsappDarkGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("send_whatsapp_msg_btn")
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("إرسال التقرير لولي الأمر عبر واتساب", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        OutlinedButton(
            onClick = {
                PrintUtils.printStudentReport(
                    context = context,
                    student = student,
                    schoolName = teacher.schoolName,
                    teacherName = teacher.fullName,
                    subjectName = teacher.subject,
                    customNotes = customTeacherNotes
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626))
            Spacer(modifier = Modifier.width(6.dp))
            Text("مشاركة وتصدير تقرير PDF رسمي", fontSize = 11.sp, color = Color(0xFFDC2626))
        }
    }
}

/**
 * Sub-Tab 4: Parent Follow-up Dashboard & Communication Log History
 */
@Composable
private fun ParentFollowUpDashboardTab(
    teacher: TeacherEntity,
    allStudents: List<StudentEntity>,
    commLogs: List<CommunicationLogEntity>,
    repository: RqeebRepository,
    whatsappDarkGreen: Color
) {
    val linkedCount = allStudents.count { it.parentPhone.isNotBlank() }
    val followedCount = allStudents.count { it.isFollowedUp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("📊 لوحة إحصائيات متابعة أولياء الأمور:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        // Summary Stats Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("إجمالي الطلاب", "${allStudents.size}", Color(0xFF0284C7), modifier = Modifier.weight(1f))
            StatCard("مرتبط بواتساب", "$linkedCount", whatsappDarkGreen, modifier = Modifier.weight(1f))
            StatCard("تحت المتابعة", "$followedCount", Color(0xFFD97706), modifier = Modifier.weight(1f))
            StatCard("رسائل مرسلة", "${commLogs.size}", Color(0xFF7C3AED), modifier = Modifier.weight(1f))
        }

        Text("📋 سجل رسائل وتقارير أولياء الأمور الأخيرة (${commLogs.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

        if (commLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد رسائل سابقة مسجلة في السجل بعد.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            commLogs.take(15).forEach { log ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الطالب: ${log.studentName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(log.messageType, fontSize = 10.sp, color = whatsappDarkGreen, fontWeight = FontWeight.Bold)
                        }
                        Text("ولي الأمر: ${log.parentName} (${log.parentPhone})", fontSize = 10.sp, color = Color.Gray)
                        Text(log.messageSummary, fontSize = 11.sp, maxLines = 2)
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                            Text("الحالة: تم الفتح والإرسال في واتساب", fontSize = 9.sp, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sub-Tab 5: Instant Notification Settings
 */
@Composable
private fun InstantNotificationSettingsTab(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    whatsappDarkGreen: Color
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var notifyAbsence by remember { mutableStateOf(true) }
    var notifyLowGrade by remember { mutableStateOf(true) }
    var notifyExcellence by remember { mutableStateOf(true) }
    var notifyMissingHomework by remember { mutableStateOf(true) }
    var notifyTardiness by remember { mutableStateOf(true) }
    var notifyBehaviorIssue by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🔔 إعدادات التنبيهات وإشعارات أولياء الأمور:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = whatsappDarkGreen)
                Text("تتيح لك هذه الإعدادات تجهيز رسائل التنبيه الفورية بضغطة زر دون إرسال أي رسالة تلقائياً إلا بموافقة واعتِماد المعلم.", fontSize = 11.sp, color = Color.Gray)

                HorizontalDivider()

                ConfigToggleRow("تجهيز إشعار عند غياب الطالب الفوري", notifyAbsence) { notifyAbsence = it }
                ConfigToggleRow("تنسيق إشعار عند حصول الطالب على درجة منخفضة", notifyLowGrade) { notifyLowGrade = it }
                ConfigToggleRow("تجهيز تهنئة عند تفوق وتميز الطالب دراسياً", notifyExcellence) { notifyExcellence = it }
                ConfigToggleRow("تنبيه ولي الأمر عند عدم تسليم الواجب المنزلي", notifyMissingHomework) { notifyMissingHomework = it }
                ConfigToggleRow("تنبيه عند تكرار التأخر الصباحي عن الطابور", notifyTardiness) { notifyTardiness = it }
                ConfigToggleRow("تنبيه عند وجود ملاحظة سلوكية داخل الفصل", notifyBehaviorIssue) { notifyBehaviorIssue = it }
            }
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    val config = ParentNotificationConfigEntity(
                        teacherId = teacher.id,
                        notifyAbsence = notifyAbsence,
                        notifyLowGrade = notifyLowGrade,
                        notifyExcellence = notifyExcellence,
                        notifyMissingHomework = notifyMissingHomework,
                        notifyTardiness = notifyTardiness,
                        notifyBehaviorIssue = notifyBehaviorIssue
                    )
                    repository.saveParentNotificationConfig(config)
                    Toast.makeText(context, "تم حفظ إعدادات إشعارات وتنبيهات أولياء الأمور بنجاح!", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = whatsappDarkGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ التفضيلات والإعدادات")
        }
    }
}

@Composable
private fun ConfigToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 9.sp, color = color)
            Text(count, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun NoStudentSelectedPlaceholder(onGoToStudents: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text("يرجى اختيار طالب من القائمة أولاً لفتح ملفه ومتابعة ولي أمره.", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onGoToStudents) {
                Text("الانتقال إلى قائمة الطلاب")
            }
        }
    }
}

/**
 * Add / Edit Student Dialog Component
 */
@Composable
private fun AddEditStudentDialog(
    student: StudentEntity?,
    teacher: TeacherEntity,
    onDismiss: () -> Unit,
    onSave: (StudentEntity) -> Unit
) {
    val isEditing = student != null
    var name by remember { mutableStateOf(student?.fullName.orEmpty()) }
    var schoolNum by remember { mutableStateOf(student?.schoolNumber.orEmpty()) }
    var grade by remember { mutableStateOf(student?.grade.orEmpty().ifEmpty { YemeniCurriculum.toDisplayGrade(teacher.gradeTaught) }) }
    var section by remember { mutableStateOf(student?.section.orEmpty().ifEmpty { teacher.section.ifEmpty { "أ" } }) }
    var parentName by remember { mutableStateOf(student?.parentName.orEmpty()) }
    var parentPhone by remember { mutableStateOf(student?.parentPhone.orEmpty()) }
    var relationship by remember { mutableStateOf(student?.parentRelationship.orEmpty().ifEmpty { "أب" }) }
    var monthlyScore by remember { mutableStateOf(student?.monthlyTestScore?.toString() ?: "18.0") }
    var strengthsText by remember { mutableStateOf(student?.strengths.orEmpty()) }
    var weaknessesText by remember { mutableStateOf(student?.weaknesses.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "تعديل بيانات الطالب وولي الأمر" else "إضافة طالب جديد وربطه بولي الأمر", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطالب الرباعي *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = schoolNum,
                        onValueChange = { schoolNum = it },
                        label = { Text("الرقم المدرسي") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("الشعبة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("📱 بيانات التواصل بواتساب ولي الأمر:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF128C7E))
                OutlinedTextField(
                    value = parentName,
                    onValueChange = { parentName = it },
                    label = { Text("اسم ولي الأمر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = parentPhone,
                        onValueChange = { parentPhone = it },
                        label = { Text("رقم واتساب ولي الأمر *") },
                        placeholder = { Text("771234567") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it },
                        label = { Text("صلة القرابة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("📊 الأكاديميات ومستوى الطالب:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = monthlyScore,
                    onValueChange = { monthlyScore = it },
                    label = { Text("درجة اختبار الشهر (من 20)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = strengthsText,
                    onValueChange = { strengthsText = it },
                    label = { Text("نقاط القوة والتميز") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weaknessesText,
                    onValueChange = { weaknessesText = it },
                    label = { Text("جوانب التحسين المستهدفة") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val mScore = monthlyScore.toDoubleOrNull() ?: 15.0
                    val updated = StudentEntity(
                        id = student?.id ?: 0,
                        teacherId = teacher.id,
                        fullName = name,
                        schoolNumber = schoolNum,
                        grade = grade,
                        section = section,
                        schoolName = teacher.schoolName,
                        parentName = parentName,
                        parentPhone = parentPhone,
                        parentRelationship = relationship,
                        monthlyTestScore = mScore,
                        examScore = mScore,
                        strengths = strengthsText,
                        weaknesses = weaknessesText
                    )
                    onSave(updated)
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
