package com.example.ui.owner

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ai.GeminiService
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.*
import com.example.util.ContactUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboard(
    repository: RqeebRepository,
    geminiService: GeminiService,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Teachers, 1: Stats, 2: Settings & Contact

    // Teachers List State
    val teachers by repository.allTeachers.collectAsState(initial = emptyList())
    val totalTeachers by repository.totalTeachersCount.collectAsState(initial = 0)
    val totalLessons by repository.totalLessonsCount.collectAsState(initial = 0)
    val totalExams by repository.totalExamsCount.collectAsState(initial = 0)

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") }
    var filterGov by remember { mutableStateOf("ALL") }

    // Dialog states
    var teacherToDelete by remember { mutableStateOf<TeacherEntity?>(null) }
    var teacherDetailsToShow by remember { mutableStateOf<TeacherEntity?>(null) }

    // Settings States
    var autoApproveState by remember { mutableStateOf(true) }
    var newOwnerPassword by remember { mutableStateOf("") }
    var changePasswordSuccess by remember { mutableStateOf(false) }

    // Contact info
    var ownerPhoneInput by remember { mutableStateOf(YemeniCurriculum.OWNER_PHONE_DEFAULT) }
    var ownerWhatsAppUrlInput by remember { mutableStateOf(YemeniCurriculum.OWNER_WHATSAPP_URL_DEFAULT) }
    var contactSaveSuccess by remember { mutableStateOf(false) }

    var apiTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingApi by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        autoApproveState = repository.isAutoApproveEnabled()
        ownerPhoneInput = repository.getOwnerPhone()
        ownerWhatsAppUrlInput = repository.getOwnerWhatsAppUrl()
    }

    val filteredTeachers = remember(teachers, searchQuery, filterStatus, filterGov) {
        teachers.filter { teacher ->
            val matchesQuery = searchQuery.isBlank() ||
                    teacher.fullName.contains(searchQuery, ignoreCase = true) ||
                    teacher.email.contains(searchQuery, ignoreCase = true) ||
                    teacher.phone.contains(searchQuery, ignoreCase = true) ||
                    teacher.schoolName.contains(searchQuery, ignoreCase = true) ||
                    teacher.directorate.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (filterStatus) {
                "APPROVED" -> teacher.status == TeacherEntity.STATUS_APPROVED
                "PENDING" -> teacher.status == TeacherEntity.STATUS_PENDING
                "SUSPENDED" -> teacher.status == TeacherEntity.STATUS_SUSPENDED
                "REJECTED" -> teacher.status == TeacherEntity.STATUS_REJECTED
                else -> true
            }

            val matchesGov = filterGov == "ALL" || teacher.governorate.contains(filterGov, ignoreCase = true)

            matchesQuery && matchesStatus && matchesGov
        }
    }

    val activeCount = teachers.count { it.status == TeacherEntity.STATUS_APPROVED }
    val pendingCount = teachers.count { it.status == TeacherEntity.STATUS_PENDING }
    val suspendedCount = teachers.count { it.status == TeacherEntity.STATUS_SUSPENDED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "لوحة تحكم مالك النظام",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "إدارة منظومة رقيب للمناهج والمدارس اليمنية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("owner_logout_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "تسجيل خروج",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("المعلمون ($totalTeachers)") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("الإحصائيات") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("الإعدادات والتواصل") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> {
                    // Teachers Management Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Quick KPI Banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KpiMiniCard("النشطون", "$activeCount", StatusApproved, Modifier.weight(1f))
                            KpiMiniCard("بانتظار المراجعة", "$pendingCount", StatusPending, Modifier.weight(1f))
                            KpiMiniCard("المعلقون", "$suspendedCount", StatusSuspended, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث بالاسم، المدرسة، المحافظة، أو الهاتف...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("owner_search_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter Status Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = filterStatus == "ALL",
                                onClick = { filterStatus = "ALL" },
                                label = { Text("الكل ($totalTeachers)", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = filterStatus == "PENDING",
                                onClick = { filterStatus = "PENDING" },
                                label = { Text("بانتظار المراجعة ($pendingCount)", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = filterStatus == "APPROVED",
                                onClick = { filterStatus = "APPROVED" },
                                label = { Text("مفعل ($activeCount)", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = filterStatus == "SUSPENDED",
                                onClick = { filterStatus = "SUSPENDED" },
                                label = { Text("معلق ($suspendedCount)", fontSize = 11.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredTeachers.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "لا توجد حسابات معلمين مطابقة لبحثك",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredTeachers, key = { it.id }) { teacher ->
                                    TeacherAccountCard(
                                        teacher = teacher,
                                        onViewDetails = { teacherDetailsToShow = teacher },
                                        onApprove = {
                                            coroutineScope.launch {
                                                repository.updateTeacherStatus(teacher.id, TeacherEntity.STATUS_APPROVED)
                                                Toast.makeText(context, "تم تفعيل حساب المعلم بنجاح", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onSuspend = {
                                            coroutineScope.launch {
                                                repository.updateTeacherStatus(teacher.id, TeacherEntity.STATUS_SUSPENDED)
                                                Toast.makeText(context, "تم تعليق حساب المعلم", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onReject = {
                                            coroutineScope.launch {
                                                repository.updateTeacherStatus(teacher.id, TeacherEntity.STATUS_REJECTED)
                                                Toast.makeText(context, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onDelete = { teacherToDelete = teacher }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Application Statistics
                    OwnerStatsSection(
                        totalTeachers = totalTeachers,
                        activeTeachers = activeCount,
                        pendingTeachers = pendingCount,
                        suspendedTeachers = suspendedCount,
                        totalLessons = totalLessons,
                        totalExams = totalExams
                    )
                }

                2 -> {
                    // Owner Settings, WhatsApp Contact, and System Diagnostics
                    OwnerSettingsAndContactSection(
                        autoApprove = autoApproveState,
                        onAutoApproveChange = { newValue ->
                            autoApproveState = newValue
                            coroutineScope.launch {
                                repository.setAutoApprove(newValue)
                            }
                        },
                        ownerPhone = ownerPhoneInput,
                        onOwnerPhoneChange = {
                            ownerPhoneInput = it
                            contactSaveSuccess = false
                        },
                        ownerWhatsAppUrl = ownerWhatsAppUrlInput,
                        onOwnerWhatsAppUrlChange = {
                            ownerWhatsAppUrlInput = it
                            contactSaveSuccess = false
                        },
                        onSaveContact = {
                            if (ownerPhoneInput.isNotBlank()) {
                                coroutineScope.launch {
                                    val formattedUrl = if (ownerWhatsAppUrlInput.isNotBlank()) {
                                        ownerWhatsAppUrlInput
                                    } else {
                                        "https://wa.me/967${ownerPhoneInput.trim().removePrefix("0")}"
                                    }
                                    repository.updateOwnerContactInfo(ownerPhoneInput, formattedUrl)
                                    contactSaveSuccess = true
                                    Toast.makeText(context, "تم تحديث بيانات التواصل بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        contactSaveSuccess = contactSaveSuccess,
                        newPassword = newOwnerPassword,
                        onNewPasswordChange = { newOwnerPassword = it },
                        onSavePassword = {
                            if (newOwnerPassword.length >= 6) {
                                coroutineScope.launch {
                                    repository.updateOwnerPassword(newOwnerPassword)
                                    newOwnerPassword = ""
                                    changePasswordSuccess = true
                                    Toast.makeText(context, "تم تغيير كلمة مرور المالك بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        passwordSuccess = changePasswordSuccess,
                        geminiConfigured = geminiService.isConfigured(),
                        isTestingApi = isTestingApi,
                        apiTestResult = apiTestResult,
                        onTestApi = {
                            isTestingApi = true
                            apiTestResult = null
                            coroutineScope.launch {
                                val result = geminiService.generateContent("اختبار اتصال رقيب للمنهج اليمني: اذكر نصيحة تربوية لمعلمي اليمن.")
                                isTestingApi = false
                                apiTestResult = if (result.isSuccess) {
                                    "الاتصال ناجح تماماً! استجابة Gemini: ${result.getOrThrow()}"
                                } else {
                                    "فشل الاتصال: ${result.exceptionOrNull()?.message}"
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (teacherToDelete != null) {
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("حذف حساب المعلم نهائياً") },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف حساب المعلم (${teacherToDelete?.fullName})؟ سيؤدي هذا الإجراء إلى حذف جميع بياناته نهائياً من قاعدة البيانات.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = teacherToDelete?.id ?: return@Button
                        coroutineScope.launch {
                            repository.deleteTeacher(id)
                            teacherToDelete = null
                            Toast.makeText(context, "تم حذف الحساب نهائياً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، حذف الحساب")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { teacherToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Teacher Details Dialog
    if (teacherDetailsToShow != null) {
        val t = teacherDetailsToShow!!
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(t.createdAt))
        Dialog(onDismissRequest = { teacherDetailsToShow = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "الملف المهني للمعلم (اليمن)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { teacherDetailsToShow = null }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    ProfileItemRow("الاسم الكامل:", t.fullName)
                    ProfileItemRow("البريد الإلكتروني:", t.email)
                    ProfileItemRow("رقم الهاتف:", t.phone)
                    ProfileItemRow("المدرسة:", t.schoolName)
                    ProfileItemRow("المحافظة:", t.governorate)
                    ProfileItemRow("المديرية:", if (t.directorate.isNotBlank()) t.directorate else "غير محددة")
                    ProfileItemRow("المادة التخصصية:", t.subject)
                    ProfileItemRow("المرحلة والصف:", "${t.stage} - ${t.gradeTaught}")
                    ProfileItemRow("طبعة المنهج المعتمدة:", t.curriculumEdition)
                    ProfileItemRow("تاريخ التسجيل:", dateStr)
                    ProfileItemRow("حالة الحساب:", when (t.status) {
                        TeacherEntity.STATUS_APPROVED -> "مفعل ومتاح للاستخدام"
                        TeacherEntity.STATUS_PENDING -> "بانتظار موافقة الإدارة"
                        TeacherEntity.STATUS_SUSPENDED -> "معلق وموقوف مؤقتاً"
                        else -> "مرفوض"
                    })

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { teacherDetailsToShow = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تم")
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherAccountCard(
    teacher: TeacherEntity,
    onViewDetails: () -> Unit,
    onApprove: () -> Unit,
    onSuspend: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        teacher.fullName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        softWrap = true
                    )
                    Text(
                        "${teacher.subject} • ${teacher.schoolName} (${teacher.governorate})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        softWrap = true
                    )
                }

                StatusBadge(teacher.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "📱 ${teacher.phone} | 🏫 ${teacher.curriculumEdition}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick = onViewDetails,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("التفاصيل", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                when (teacher.status) {
                    TeacherEntity.STATUS_PENDING -> {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("تفعيل الحساب", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onReject,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("رفض", fontSize = 12.sp)
                        }
                    }
                    TeacherEntity.STATUS_APPROVED -> {
                        OutlinedButton(
                            onClick = onSuspend,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusSuspended),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("تعليق", fontSize = 12.sp)
                        }
                    }
                    TeacherEntity.STATUS_SUSPENDED -> {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("إعادة تفعيل", fontSize = 12.sp)
                        }
                    }
                    TeacherEntity.STATUS_REJECTED -> {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("قبول", fontSize = 12.sp)
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        TeacherEntity.STATUS_APPROVED -> Triple(Color(0xFFDCFCE7), Color(0xFF166534), "مفعل")
        TeacherEntity.STATUS_PENDING -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "بانتظار المراجعة")
        TeacherEntity.STATUS_SUSPENDED -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "معلق")
        TeacherEntity.STATUS_REJECTED -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "مرفوض")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), status)
    }

    Surface(shape = RoundedCornerShape(20.dp), color = bgColor) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun KpiMiniCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                label,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                softWrap = true,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ProfileItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun OwnerStatsSection(
    totalTeachers: Int,
    activeTeachers: Int,
    pendingTeachers: Int,
    suspendedTeachers: Int,
    totalLessons: Int,
    totalExams: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "نظرة عامة على نشاط المنصة باليمن",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "إحصائيات حسابات المعلمين",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    StatBarRow("إجمالي حسابات المعلمين المسجلين", totalTeachers, totalTeachers, RqeebPrimary)
                    StatBarRow("المعلمون المفعلون والنشطون", activeTeachers, totalTeachers, StatusApproved)
                    StatBarRow("طلبات التسجيل الجديدة بانتظار المراجعة", pendingTeachers, totalTeachers, StatusPending)
                    StatBarRow("الحسابات المعلقة مؤقتاً", suspendedTeachers, totalTeachers, StatusSuspended)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "المحتوى التعليمي المنشأ وفق المنهج اليمني",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("$totalLessons", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("خطة درس منجزة", fontSize = 12.sp)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("$totalExams", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Text("اختبار معد للطلاب", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBarRow(label: String, count: Int, total: Int, color: Color) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$count", fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun OwnerSettingsAndContactSection(
    autoApprove: Boolean,
    onAutoApproveChange: (Boolean) -> Unit,
    ownerPhone: String,
    onOwnerPhoneChange: (String) -> Unit,
    ownerWhatsAppUrl: String,
    onOwnerWhatsAppUrlChange: (String) -> Unit,
    onSaveContact: () -> Unit,
    contactSaveSuccess: Boolean,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    onSavePassword: () -> Unit,
    passwordSuccess: Boolean,
    geminiConfigured: Boolean,
    isTestingApi: Boolean,
    apiTestResult: String?,
    onTestApi: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "إعدادات التواصل والدعم الفني لمنصة رقيب",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Owner Contact Configuration (Mandatory specification)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("بيانات تواصل المالك وخدمة العملاء (اليمن)", fontWeight = FontWeight.Bold)
                    Text(
                        "يتم استخدام هذا الرقم لرابط زر 'تواصل مع إدارة رقيب' (واتساب) ومساعدة المعلمين في استعادة الحسابات.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = ownerPhone,
                        onValueChange = onOwnerPhoneChange,
                        label = { Text("رقم هاتف المالك (اليمن)") },
                        placeholder = { Text("785502919") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerWhatsAppUrl,
                        onValueChange = onOwnerWhatsAppUrlChange,
                        label = { Text("رابط محادثة واتساب المعتمد") },
                        placeholder = { Text("https://wa.me/967785502919") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (contactSaveSuccess) {
                        Text("تم حفظ وتحديث بيانات التواصل بنجاح!", color = StatusApproved, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onSaveContact,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتحديث بيانات التواصل")
                    }
                }
            }
        }

        // Auto approve policy
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الموافقة التلقائية على المعلمين الجدد", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (autoApprove) "يتم تفعيل حساب المعلم فور التسجيل مباشرة" else "تتطلب الحسابات مراجعة المالك وتفعيلها يدوياً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = autoApprove, onCheckedChange = onAutoApproveChange)
                }
            }
        }

        // Gemini AI Diagnostics
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("حالة خادم الذكاء الاصطناعي للمنهج اليمني (Gemini)", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (geminiConfigured) StatusApproved else StatusPending,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (geminiConfigured) "مفتاح Gemini مهيأ بنجاح عبر لوحة Secrets" else "المفتاح غير موجود (يعمل بنظام المعلم الذكي المدمج)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onTestApi,
                        enabled = !isTestingApi,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingApi) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("اختبار اتصال الذكاء الاصطناعي الآن")
                        }
                    }

                    if (apiTestResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            apiTestResult,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (apiTestResult.contains("ناجح")) StatusApproved else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Change Owner Password
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تغيير كلمة مرور المالك", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = onNewPasswordChange,
                        label = { Text("كلمة المرور الجديدة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordSuccess) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("تم تحديث كلمة المرور بنجاح!", color = StatusApproved, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onSavePassword,
                        enabled = newPassword.length >= 6,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ كلمة المرور")
                    }
                }
            }
        }
    }
}
