package com.example.ui.owner

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ai.GeminiService
import com.example.data.model.AuditLogEntity
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Teachers, 1: Stats, 2: Audit Logs, 3: Settings & Contact

    // Teachers List State
    val teachers by repository.allTeachers.collectAsState(initial = emptyList())
    val totalTeachers by repository.totalTeachersCount.collectAsState(initial = 0)
    val totalLessons by repository.totalLessonsCount.collectAsState(initial = 0)
    val totalExams by repository.totalExamsCount.collectAsState(initial = 0)
    val auditLogs by repository.getAuditLogsFlow().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") }
    var filterGov by remember { mutableStateOf("ALL") }
    var isSyncingCloud by remember { mutableStateOf(false) }

    // Dialog states
    var teacherToDelete by remember { mutableStateOf<TeacherEntity?>(null) }
    var teacherDetailsToShow by remember { mutableStateOf<TeacherEntity?>(null) }
    var teacherToApprove by remember { mutableStateOf<TeacherEntity?>(null) }
    var approvalCodeInput by remember { mutableStateOf("") }
    var teacherToReject by remember { mutableStateOf<TeacherEntity?>(null) }
    var rejectReasonInput by remember { mutableStateOf("") }
    var teacherToSuspend by remember { mutableStateOf<TeacherEntity?>(null) }
    var suspendReasonInput by remember { mutableStateOf("") }

    // Settings States
    var autoApproveState by remember { mutableStateOf(true) }
    var newOwnerPassword by remember { mutableStateOf("") }
    var changePasswordSuccess by remember { mutableStateOf(false) }

    // Contact info
    var ownerPhoneInput by remember { mutableStateOf(YemeniCurriculum.OWNER_PHONE_DEFAULT) }
    var ownerWhatsAppUrlInput by remember { mutableStateOf(YemeniCurriculum.OWNER_WHATSAPP_URL_DEFAULT) }
    var contactSaveSuccess by remember { mutableStateOf(false) }

    // Real AI Key & Model Configuration States
    var savedApiKey by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }
    var isEditingApiKey by remember { mutableStateOf(false) }
    var selectedAiModel by remember { mutableStateOf("gemini-2.5-flash") }
    var aiDailyLimit by remember { mutableIntStateOf(100) }
    var aiMaxTokens by remember { mutableIntStateOf(2048) }
    var aiStats by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var saveKeySuccess by remember { mutableStateOf(false) }

    var apiTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingApi by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        autoApproveState = repository.isAutoApproveEnabled()
        ownerPhoneInput = repository.getOwnerPhone()
        ownerWhatsAppUrlInput = repository.getOwnerWhatsAppUrl()
        savedApiKey = repository.getAiApiKey()
        selectedAiModel = repository.getAiModelName()
        aiDailyLimit = repository.getAiDailyLimit()
        aiMaxTokens = repository.getAiMaxTokens()
        aiStats = repository.getAiUsageStats()
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
                        onClick = {
                            coroutineScope.launch {
                                isSyncingCloud = true
                                val syncedCount = repository.syncFromCloud()
                                isSyncingCloud = false
                                Toast.makeText(context, "تمت المزامنة السحابية بنجاح ($syncedCount معلماً)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSyncingCloud,
                        modifier = Modifier.testTag("owner_cloud_sync_button")
                    ) {
                        if (isSyncingCloud) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "مزامنة سحابية مع فايربيس",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

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
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("$pendingCount")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.People, contentDescription = null)
                        }
                    },
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
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("سجل العمليات (${auditLogs.size})") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
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

                        // Pending Requests Alert Notification Banner for Realme Owner
                        if (pendingCount > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { filterStatus = "PENDING" }
                                    .testTag("pending_requests_alert_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🔔 يوجد $pendingCount طلب تفعيل معلم جديد بانتظار المراجعة",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "اضغط هنا لتصفية الطلبات المعلقة واعتماد أكواد التفعيل فوراً",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
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
                                            val randomCode = "RQ-${(1000..9999).random()}-${('A'..'Z').random()}"
                                            approvalCodeInput = randomCode
                                            teacherToApprove = teacher
                                        },
                                        onSuspend = {
                                            suspendReasonInput = "إيقاف مؤقت للحساب"
                                            teacherToSuspend = teacher
                                        },
                                        onReject = {
                                            rejectReasonInput = "بيانات التسجيل غير مكتملة"
                                            teacherToReject = teacher
                                        },
                                        onReactivate = {
                                            coroutineScope.launch {
                                                repository.reactivateTeacher(teacher)
                                                Toast.makeText(context, "تمت إعادة تفعيل حساب المعلم بنجاح", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onDelete = { teacherToDelete = teacher },
                                        onCopyCode = {
                                            if (teacher.activationCode.isNotBlank()) {
                                                ContactUtils.copyToClipboard(context, "كود التفعيل", teacher.activationCode)
                                            }
                                        },
                                        onSendWhatsApp = {
                                            if (teacher.phone.isNotBlank()) {
                                                val msg = if (teacher.activationCode.isNotBlank()) {
                                                    "مرحباً يا أستاذ ${teacher.fullName}، كود التفعيل الخاص بحسابك في تطبيق «رقيب – مساعد المعلم الذكي» هو:\n${teacher.activationCode}"
                                                } else {
                                                    "مرحباً يا أستاذ ${teacher.fullName}، نود التواصل معك بشأن حسابك في تطبيق «رقيب – مساعد المعلم الذكي»."
                                                }
                                                ContactUtils.openTeacherWhatsApp(context, teacher.phone, msg)
                                            }
                                        }
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
                    // Audit Logs Section
                    OwnerAuditLogsSection(
                        auditLogs = auditLogs
                    )
                }

                3 -> {
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
                        savedApiKey = savedApiKey,
                        apiKeyInput = apiKeyInput,
                        onApiKeyInputChange = { apiKeyInput = it },
                        isEditingApiKey = isEditingApiKey,
                        onToggleEditApiKey = { isEditingApiKey = !isEditingApiKey },
                        onSaveApiKey = {
                            if (apiKeyInput.isNotBlank()) {
                                coroutineScope.launch {
                                    repository.setAiApiKey(apiKeyInput.trim())
                                    savedApiKey = apiKeyInput.trim()
                                    apiKeyInput = ""
                                    isEditingApiKey = false
                                    saveKeySuccess = true
                                    Toast.makeText(context, "تم حفظ وتشفير مفتاح الذكاء الاصطناعي بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        saveKeySuccess = saveKeySuccess,
                        selectedModel = selectedAiModel,
                        onModelChange = { model ->
                            selectedAiModel = model
                            coroutineScope.launch { repository.setAiModelName(model) }
                        },
                        dailyLimit = aiDailyLimit,
                        onDailyLimitChange = { limit ->
                            aiDailyLimit = limit
                            coroutineScope.launch { repository.setAiDailyLimit(limit) }
                        },
                        maxTokens = aiMaxTokens,
                        onMaxTokensChange = { tokens ->
                            aiMaxTokens = tokens
                            coroutineScope.launch { repository.setAiMaxTokens(tokens) }
                        },
                        aiStats = aiStats,
                        isTestingApi = isTestingApi,
                        apiTestResult = apiTestResult,
                        onTestApi = {
                            isTestingApi = true
                            apiTestResult = null
                            coroutineScope.launch {
                                val diag = geminiService.testConnection()
                                isTestingApi = false
                                apiTestResult = "${if (diag.isSuccess) "✅ " else "❌ "}${diag.message}\n(الحالة: ${diag.statusCode} | النموذج: ${diag.modelName} | المزود: ${diag.provider})"
                                aiStats = repository.getAiUsageStats()
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

    // Approve & Generate Activation Code Dialog
    if (teacherToApprove != null) {
        val t = teacherToApprove!!
        AlertDialog(
            onDismissRequest = { teacherToApprove = null },
            icon = {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = StatusApproved,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "الموافقة وتوليد كود التفعيل",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "المعلم: ${t.fullName}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "المدرسة: ${t.schoolName} | المحافظة: ${t.governorate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "هاتف المعلم: ${t.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (t.deviceModel.isNotBlank()) {
                        Text(
                            "هاتف التسجيل: ${t.deviceModel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = approvalCodeInput,
                        onValueChange = { approvalCodeInput = it.uppercase() },
                        label = { Text("كود التفعيل المخصص") },
                        trailingIcon = {
                            IconButton(onClick = {
                                approvalCodeInput = "RQ-${(1000..9999).random()}-${('A'..'Z').random()}"
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "توليد كود جديد")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("approval_code_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ContactUtils.copyToClipboard(context, "كود التفعيل", approvalCodeInput)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الكود", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val greeting = "أهلاً وسهلاً بك يا أستاذ ${t.fullName} في منصة «رقيب – مساعد المعلم الذكي».\nتمت الموافقة على طلب تسجيلك بنجاح!\nكود التفعيل الخاص بحسابك هو:\n$approvalCodeInput\nيمكنك الآن إدخال الكود في التطبيق واستخدامه بالكامل."
                                ContactUtils.openTeacherWhatsApp(context, t.phone, greeting)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = approvalCodeInput.trim().ifBlank {
                            "RQ-${(1000..9999).random()}-${('A'..'Z').random()}"
                        }
                        coroutineScope.launch {
                            repository.approveTeacher(t, code)
                            teacherToApprove = null
                            Toast.makeText(context, "تم اعتماد التفعيل ومزامنته سحابياً بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                    modifier = Modifier.testTag("confirm_approval_button")
                ) {
                    Text("تأكيد التفعيل وحفظه سحابياً")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToApprove = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Reject Dialog
    if (teacherToReject != null) {
        val t = teacherToReject!!
        AlertDialog(
            onDismissRequest = { teacherToReject = null },
            icon = {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "رفض طلب تسجيل المعلم",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("المعلم: ${t.fullName} (${t.schoolName})")
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("سبب الرفض (اختياري)") },
                        placeholder = { Text("مثال: بيانات غير مكتملة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.rejectTeacher(t, rejectReasonInput)
                            teacherToReject = null
                            Toast.makeText(context, "تم رفض الطلب وحفظ القرار سحابياً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الرفض")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToReject = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Suspend Dialog
    if (teacherToSuspend != null) {
        val t = teacherToSuspend!!
        AlertDialog(
            onDismissRequest = { teacherToSuspend = null },
            icon = {
                Icon(
                    Icons.Default.PauseCircle,
                    contentDescription = null,
                    tint = StatusSuspended,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "تعليق حساب المعلم",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("المعلم: ${t.fullName}")
                    OutlinedTextField(
                        value = suspendReasonInput,
                        onValueChange = { suspendReasonInput = it },
                        label = { Text("سبب الإيقاف المؤقت (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.suspendTeacher(t, suspendReasonInput)
                            teacherToSuspend = null
                            Toast.makeText(context, "تم تعليق الحساب ومزامنة القرار سحابياً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuspended)
                ) {
                    Text("تأكيد التعليق")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToSuspend = null }) {
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
                    ProfileItemRow("طراز الهاتف:", if (t.deviceModel.isNotBlank()) t.deviceModel else "هاتف أندرويد")
                    ProfileItemRow("كود التفعيل:", if (t.activationCode.isNotBlank()) t.activationCode else "لم يصدر بعد")
                    ProfileItemRow("المعرف السحابي:", if (t.cloudId.isNotBlank()) t.cloudId else "محلي")
                    if (t.reviewNotes.isNotBlank()) {
                        ProfileItemRow("ملاحظات المراجعة:", t.reviewNotes)
                    }
                    ProfileItemRow("تاريخ التسجيل:", dateStr)
                    ProfileItemRow("حالة الحساب:", when (t.status) {
                        TeacherEntity.STATUS_APPROVED -> "مفعل ومتاح للاستخدام"
                        TeacherEntity.STATUS_PENDING -> "بانتظار موافقة الإدارة"
                        TeacherEntity.STATUS_SUSPENDED -> "معلق وموقوف مؤقتاً"
                        else -> "مرفوض"
                    })

                    if (t.activationCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    ContactUtils.copyToClipboard(context, "كود التفعيل", t.activationCode)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ الكود", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    val msg = "مرحباً يا أستاذ ${t.fullName}، كود التفعيل الخاص بحسابك في تطبيق «رقيب – مساعد المعلم الذكي» هو:\n${t.activationCode}"
                                    ContactUtils.openTeacherWhatsApp(context, t.phone, msg)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("واتساب", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

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
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
    onCopyCode: (() -> Unit)? = null,
    onSendWhatsApp: (() -> Unit)? = null,
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
                "📱 ${teacher.phone} | 🏫 ${teacher.curriculumEdition}${if (teacher.deviceModel.isNotBlank()) " | 📲 ${teacher.deviceModel}" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Activation Code Badge if available
            if (teacher.activationCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "كود التفعيل: ${teacher.activationCode}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (onCopyCode != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onCopyCode, modifier = Modifier.size(22.dp)) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "نسخ الكود",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        if (onSendWhatsApp != null) {
                            IconButton(onClick = onSendWhatsApp, modifier = Modifier.size(22.dp)) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "إرسال واتساب",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

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
                            Text("موافقة وتوليد كود", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onReject,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("رفض", fontSize = 11.sp)
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
                            onClick = onReactivate,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("إعادة تفعيل", fontSize = 12.sp)
                        }
                    }
                    TeacherEntity.STATUS_REJECTED -> {
                        Button(
                            onClick = onReactivate,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("إعادة تفعيل", fontSize = 12.sp)
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
    savedApiKey: String,
    apiKeyInput: String,
    onApiKeyInputChange: (String) -> Unit,
    isEditingApiKey: Boolean,
    onToggleEditApiKey: () -> Unit,
    onSaveApiKey: () -> Unit,
    saveKeySuccess: Boolean,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    dailyLimit: Int,
    onDailyLimitChange: (Int) -> Unit,
    maxTokens: Int,
    onMaxTokensChange: (Int) -> Unit,
    aiStats: Triple<Int, Int, Int>?,
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

        // Dedicated Real AI Key & Model Management Card (إعدادات وربط مفتاح الذكاء الاصطناعي)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "إعدادات وربط مفتاح الذكاء الاصطناعي (Gemini API)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )

                        Surface(
                            shape = CircleShape,
                            color = if (savedApiKey.isNotBlank() || geminiConfigured) StatusApproved.copy(alpha = 0.15f) else StatusPending.copy(alpha = 0.15f)
                        ) {
                            Text(
                                if (savedApiKey.isNotBlank() || geminiConfigured) "متصل ومفعل" else "قيد الانتظار",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = if (savedApiKey.isNotBlank() || geminiConfigured) StatusApproved else StatusPending,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        "يتم حفظ المفتاح بأمان تام في مساحة تخزين المالك المشفرة، ولا يظهر للمعلمين ولا يتم تسجيله في سجلات النظام.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Display Masked Key or Edit Box
                    if (!isEditingApiKey && savedApiKey.isNotBlank()) {
                        val masked = "••••••••••••" + savedApiKey.takeLast(4)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("المفتاح الحالي النشط:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(masked, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }

                                TextButton(onClick = onToggleEditApiKey) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تغيير المفتاح")
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = onApiKeyInputChange,
                            label = { Text("أدخل مفتاح Gemini API الحقيقي (AIzaSy...)") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            trailingIcon = {
                                if (savedApiKey.isNotBlank()) {
                                    IconButton(onClick = onToggleEditApiKey) {
                                        Icon(Icons.Default.Close, contentDescription = "إلغاء")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = onSaveApiKey,
                            enabled = apiKeyInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ وتشفير مفتاح الذكاء الاصطناعي")
                        }
                    }

                    if (saveKeySuccess) {
                        Text("تم حفظ المفتاح بنجاح وسيبدأ النظام في استخدامه فوراً!", color = StatusApproved, fontSize = 12.sp)
                    }

                    HorizontalDivider()

                    // Model Selection
                    Text("نموذج الذكاء الاصطناعي المعتمد:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("gemini-2.5-flash", "Gemini 2.5 Flash (موصى به)"),
                            Pair("gemini-1.5-pro", "Gemini 1.5 Pro"),
                            Pair("gemini-1.5-flash", "Gemini 1.5 Flash")
                        ).forEach { (model, title) ->
                            FilterChip(
                                selected = selectedModel == model,
                                onClick = { onModelChange(model) },
                                label = { Text(title, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Daily Limit & Tokens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الحد اليومي للطلبات:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(100, 250, 500).forEach { lim ->
                                    FilterChip(
                                        selected = dailyLimit == lim,
                                        onClick = { onDailyLimitChange(lim) },
                                        label = { Text("$lim", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("أقصى عدد توكنات:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(2048, 4096, 8192).forEach { tok ->
                                    FilterChip(
                                        selected = maxTokens == tok,
                                        onClick = { onMaxTokensChange(tok) },
                                        label = { Text("$tok", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // Usage Stats Banner
                    if (aiStats != null) {
                        val (total, success, failed) = aiStats
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Text("إجمالي الطلبات: $total", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("الناجحة: $success", fontSize = 11.sp, color = StatusApproved, fontWeight = FontWeight.SemiBold)
                                Text("المتعثرة: $failed", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Real Connection Test Button
                    Button(
                        onClick = onTestApi,
                        enabled = !isTestingApi,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingApi) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ فحص استجابة الخادم...")
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فحص واختبار اتصال الذكاء الاصطناعي الآن")
                        }
                    }

                    if (apiTestResult != null) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (apiTestResult.contains("✅")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                        ) {
                            Text(
                                apiTestResult,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (apiTestResult.contains("✅")) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
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

@Composable
fun OwnerAuditLogsSection(
    auditLogs: List<AuditLogEntity>,
    modifier: Modifier = Modifier
) {
    if (auditLogs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "لا توجد عمليات إدارية مسجلة بعد",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "يتم توثيق وحفظ جميع قرارات الموافقة، الرفض، التعليق، وإعادة التفعيل سحابياً في فايربيس فور إجرائها.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل الإجراءات الإدارية الموثقة سحابياً (${auditLogs.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "مرتبة زمنياً",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(auditLogs, key = { it.id.ifBlank { "${it.timestamp}_${it.action}_${it.targetTeacherId}" } }) { log ->
                val timeStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (log.action) {
                                    "APPROVE" -> StatusApproved.copy(alpha = 0.15f)
                                    "REJECT" -> MaterialTheme.colorScheme.errorContainer
                                    "SUSPEND" -> StatusSuspended.copy(alpha = 0.15f)
                                    "REACTIVATE" -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = when (log.action) {
                                        "APPROVE" -> "موافقة وتفعيل"
                                        "REJECT" -> "رفض الطلب"
                                        "SUSPEND" -> "إيقاف / تعليق"
                                        "REACTIVATE" -> "إعادة تفعيل"
                                        else -> log.action
                                    },
                                    color = when (log.action) {
                                        "APPROVE" -> StatusApproved
                                        "REJECT" -> MaterialTheme.colorScheme.error
                                        "SUSPEND" -> StatusSuspended
                                        "REACTIVATE" -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "المعلم: ${log.targetTeacherName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (log.details.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "بواسطة: ${log.performedBy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
