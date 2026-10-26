package com.example.ui.owner

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLogEntity
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.*
import com.example.util.ContactUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboard(
    repository: RqeebRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onLogout() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Teachers, 1: Stats, 2: Logs, 3: Settings
    var filterStatus by remember { mutableStateOf("ALL") } // ALL, PENDING, APPROVED, SUSPENDED, REJECTED
    var searchQuery by remember { mutableStateOf("") }

    val teachers by repository.getCloudTeachersFlow().collectAsState(initial = emptyList())
    val auditLogs by repository.getAuditLogsFlow().collectAsState(initial = emptyList())
    val totalLessons by repository.getTotalLessonsCount().collectAsState(initial = 0)
    val totalExams by repository.getTotalExamsCount().collectAsState(initial = 0)

    val pendingCount = teachers.count { it.status == TeacherEntity.STATUS_PENDING }
    val activeCount = teachers.count { it.status == TeacherEntity.STATUS_APPROVED }
    val suspendedCount = teachers.count { it.status == TeacherEntity.STATUS_SUSPENDED }
    val rejectedCount = teachers.count { it.status == TeacherEntity.STATUS_REJECTED }
    val totalTeachers = teachers.size

    var teacherToApprove by remember { mutableStateOf<TeacherEntity?>(null) }
    var approvalCodeInput by remember { mutableStateOf("") }
    var teacherToSuspend by remember { mutableStateOf<TeacherEntity?>(null) }
    var suspendReasonInput by remember { mutableStateOf("") }
    var teacherToReject by remember { mutableStateOf<TeacherEntity?>(null) }
    var rejectReasonInput by remember { mutableStateOf("") }
    var teacherToDelete by remember { mutableStateOf<TeacherEntity?>(null) }
    var teacherDetailsToShow by remember { mutableStateOf<TeacherEntity?>(null) }

    var isSyncing by remember { mutableStateOf(false) }
    var showSupabaseConfigDialog by remember { mutableStateOf(false) }
    var showSupabaseGuideDialog by remember { mutableStateOf(false) }
    var supabaseUrlInput by remember { mutableStateOf(repository.getSupabaseConfig().first) }
    var supabaseAnonKeyInput by remember { mutableStateOf(repository.getSupabaseConfig().second) }
    var isTestingConnection by remember { mutableStateOf(false) }

    val filteredTeachers = remember(teachers, filterStatus, searchQuery) {
        teachers.filter { teacher ->
            val matchesStatus = when (filterStatus) {
                "PENDING" -> teacher.status == TeacherEntity.STATUS_PENDING
                "APPROVED" -> teacher.status == TeacherEntity.STATUS_APPROVED
                "SUSPENDED" -> teacher.status == TeacherEntity.STATUS_SUSPENDED
                "REJECTED" -> teacher.status == TeacherEntity.STATUS_REJECTED
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    teacher.fullName.lowercase().contains(q) ||
                    teacher.phone.contains(q) ||
                    teacher.email.lowercase().contains(q) ||
                    teacher.schoolName.lowercase().contains(q) ||
                    teacher.governorate.lowercase().contains(q)
            matchesStatus && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "لوحة تحكم المالك المركزي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = OwnerGold
                            ) {
                                Text(
                                    "Realme Control",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "المالك: أ. عبد الرقيب فرج | خادم رقيب المركزي",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSyncing = true
                            coroutineScope.launch {
                                val count = repository.syncFromCloud()
                                isSyncing = false
                                Toast.makeText(context, "تمت المزامنة بنجاح ($count سجل)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("owner_sync_cloud_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "مزامنة سحابية",
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
                }
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
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
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
                    label = { Text("الإعدادات") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث بالاسم، الهاتف، المدرسة، المحافظة...") },
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
                            label = { Text("قيد المراجعة ($pendingCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusPending.copy(alpha = 0.2f),
                                selectedLabelColor = StatusPending
                            )
                        )
                        FilterChip(
                            selected = filterStatus == "APPROVED",
                            onClick = { filterStatus = "APPROVED" },
                            label = { Text("المفعلون ($activeCount)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterStatus == "SUSPENDED",
                            onClick = { filterStatus = "SUSPENDED" },
                            label = { Text("الموقوفون ($suspendedCount)", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredTeachers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد طلبات معلمين مطابقة للبحث أو التصفية", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredTeachers, key = { it.id.toString() + it.cloudId }) { teacher ->
                                TeacherAccountCard(
                                    teacher = teacher,
                                    onViewDetails = { teacherDetailsToShow = teacher },
                                    onApprove = {
                                        approvalCodeInput = "RQ-${(1000..9999).random()}-${('A'..'Z').random()}"
                                        teacherToApprove = teacher
                                    },
                                    onSuspend = {
                                        suspendReasonInput = "إيقاف مؤقت للحساب"
                                        teacherToSuspend = teacher
                                    },
                                    onReject = {
                                        rejectReasonInput = "بيانات غير مستوفية"
                                        teacherToReject = teacher
                                    },
                                    onReactivate = {
                                        coroutineScope.launch {
                                            repository.reactivateTeacher(teacher)
                                            Toast.makeText(context, "تمت إعادة التفعيل بنجاح", Toast.LENGTH_SHORT).show()
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
                                                "مرحباً أستاذ ${teacher.fullName}، كود التفعيل الخاص بحسابك في تطبيق «رقيب – مساعد المعلم الذكي» هو:\n${teacher.activationCode}\nيرجى تسجيل الدخول وإدخاله لتفعيل الحساب."
                                            } else {
                                                "مرحباً أستاذ ${teacher.fullName}، نود التواصل معك بشأن طلب انضمامك إلى تطبيق «رقيب – مساعد المعلم الذكي»."
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
                // Statistics Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("إحصائيات المنصة المركزية (Realme Overview)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    item {
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("إجمالي المعلمين المسجلين: $totalTeachers", fontWeight = FontWeight.Bold)
                                Text("• بانتظار الموافقة والتفعيل: $pendingCount معلم", color = StatusPending)
                                Text("• الحسابات المفعلة والنشطة: $activeCount معلم", color = StatusApproved)
                                Text("• الحسابات الموقوفة مؤقتاً: $suspendedCount معلم", color = StatusSuspended)
                                Text("• الطلبات المرفوضة: $rejectedCount طلب", color = StatusRejected)
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("النشاط التعليمي للمعلمين في اليمن:", fontWeight = FontWeight.Bold)
                                Text("• إجمالي الدروس المحضرة: $totalLessons درس", color = MaterialTheme.colorScheme.primary)
                                Text("• إجمالي الاختبارات المنشأة: $totalExams اختبار", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
            2 -> {
                // Audit Logs Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("سجل الإجراءات الإدارية الموثقة (${auditLogs.size}):", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    if (auditLogs.isEmpty()) {
                        item {
                            Text("لا توجد عمليات مسجلة حتى الآن", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(auditLogs) { log ->
                            Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${log.action}: ${log.targetTeacherName}", fontWeight = FontWeight.Bold)
                                        Text(SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(log.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Settings Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("إعدادات لوحة المالك (Realme Device Settings)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    // Supabase Cloud Card
                    item {
                        val isConfigured = repository.isSupabaseConfigured()
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isConfigured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CloudQueue,
                                            contentDescription = null,
                                            tint = if (isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("قاعدة بيانات Supabase (PostgreSQL)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isConfigured) StatusApproved.copy(alpha = 0.2f) else StatusRejected.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            if (isConfigured) "متصل بالسحابة" else "بانتظار الإعداد",
                                            color = if (isConfigured) StatusApproved else StatusRejected,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                val currentUrl = repository.getSupabaseConfig().first
                                Text(
                                    text = if (currentUrl.isNotBlank()) "رابط المشروع: $currentUrl" else "لم يتم تعيين رابط Supabase بعد. انقر أدناه للربط.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            supabaseUrlInput = repository.getSupabaseConfig().first
                                            supabaseAnonKeyInput = repository.getSupabaseConfig().second
                                            showSupabaseConfigDialog = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إعدادات الربط", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            isTestingConnection = true
                                            coroutineScope.launch {
                                                val (ok, msg) = repository.testSupabaseConnection()
                                                isTestingConnection = false
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        enabled = !isTestingConnection,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isTestingConnection) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("اختبار الاتصال", fontSize = 12.sp)
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showSupabaseGuideDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("دليل إنشاء مشروع Supabase من الهاتف", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Owner Profile Card
                    item {
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("هاتف المالك الرئيسي: ${YemeniCurriculum.OWNER_PHONE_DEFAULT}", fontWeight = FontWeight.Bold)
                                Text("البريد المعتمد: ${YemeniCurriculum.OWNER_EMAIL_DEFAULT}")
                                Text("رابط الواتساب المباشر: ${YemeniCurriculum.OWNER_WHATSAPP_URL_DEFAULT}")
                                Text("هاتف الإشعارات النشط: هاتف المالك Realme", color = MaterialTheme.colorScheme.primary)
                                Text("نظام الإشعارات: تنبيه محلي فوري عند تسجيل طلب جديد بدون Firebase", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // Approve Dialog
    if (teacherToApprove != null) {
        val t = teacherToApprove!!
        AlertDialog(
            onDismissRequest = { teacherToApprove = null },
            icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusApproved, modifier = Modifier.size(36.dp)) },
            title = { Text("الموافقة وتوليد كود التفعيل", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المعلم: ${t.fullName}", fontWeight = FontWeight.Bold)
                    Text("المدرسة: ${t.schoolName} (${t.governorate})", fontSize = 13.sp)
                    val displayGrade = YemeniCurriculum.toDisplayGrade(t.gradeTaught)
                    val sectionStr = if (t.section.isNotBlank()) " | شعبة ${t.section}" else ""
                    Text("المادة والصف: ${t.subject} - $displayGrade$sectionStr", fontSize = 12.sp)
                    Text("الهاتف: ${t.phone}", fontSize = 13.sp)
                    if (t.deviceModel.isNotBlank()) {
                        Text("جهاز المعلم: ${t.deviceModel}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = approvalCodeInput,
                        onValueChange = { approvalCodeInput = it.uppercase() },
                        label = { Text("كود التفعيل المخصص") },
                        trailingIcon = {
                            IconButton(onClick = { approvalCodeInput = "RQ-${(1000..9999).random()}-${('A'..'Z').random()}" }) {
                                Icon(Icons.Default.Refresh, contentDescription = "توليد كود جديد")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("approval_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val (success, code) = repository.approveTeacher(t, approvalCodeInput.trim())
                            teacherToApprove = null
                            if (success) {
                                Toast.makeText(context, "تم تفعيل المعلم وتوليد الكود: $code", Toast.LENGTH_LONG).show()
                                val msg = "مرحباً يا أستاذ ${t.fullName}، تمت الموافقة على طلب تفعيل حسابك في تطبيق «رقيب – مساعد المعلم الذكي». كود التفعيل الخاص بك هو:\n$code"
                                ContactUtils.openTeacherWhatsApp(context, t.phone, msg)
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_approve_button")
                ) {
                    Text("موافقة وإرسال واتساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToApprove = null }) {
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
            icon = { Icon(Icons.Default.PauseCircle, contentDescription = null, tint = StatusSuspended) },
            title = { Text("إيقاف حساب المعلم مؤقتاً") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المعلم: ${t.fullName}")
                    OutlinedTextField(
                        value = suspendReasonInput,
                        onValueChange = { suspendReasonInput = it },
                        label = { Text("سبب الإيقاف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.suspendTeacher(t, suspendReasonInput.trim())
                            teacherToSuspend = null
                            Toast.makeText(context, "تم تعليق الحساب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuspended)
                ) {
                    Text("تأكيد الإيقاف")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToSuspend = null }) { Text("إلغاء") }
            }
        )
    }

    // Reject Dialog
    if (teacherToReject != null) {
        val t = teacherToReject!!
        AlertDialog(
            onDismissRequest = { teacherToReject = null },
            icon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = StatusRejected) },
            title = { Text("رفض طلب المعلم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المعلم: ${t.fullName}")
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("سبب الرفض") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.rejectTeacher(t, rejectReasonInput.trim())
                            teacherToReject = null
                            Toast.makeText(context, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("تأكيد الرفض")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToReject = null }) { Text("إلغاء") }
            }
        )
    }

    // Delete Dialog
    if (teacherToDelete != null) {
        val t = teacherToDelete!!
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("حذف الحساب نهائياً") },
            text = { Text("هل أنت متأكد من حذف حساب (${t.fullName}) نهائياً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteTeacher(t.id)
                            teacherToDelete = null
                            Toast.makeText(context, "تم الحذف نهائياً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Details Dialog
    if (teacherDetailsToShow != null) {
        val t = teacherDetailsToShow!!
        val displayGrade = YemeniCurriculum.toDisplayGrade(t.gradeTaught)
        val sectionStr = if (t.section.isNotBlank()) " | شعبة ${t.section}" else ""
        AlertDialog(
            onDismissRequest = { teacherDetailsToShow = null },
            title = { Text(t.fullName, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• الهاتف: ${t.phone}")
                    Text("• البريد: ${t.email}")
                    Text("• المدرسة: ${t.schoolName} (${t.governorate})")
                    Text("• المادة والصف: ${t.subject} - $displayGrade$sectionStr")
                    Text("• طبعة المنهج: ${t.curriculumEdition}")
                    Text("• الحالة: ${t.status} (مفعل: ${if (t.isActivated) "نعم" else "لا"})")
                    if (t.activationCode.isNotBlank()) {
                        Text("• كود التفعيل: ${t.activationCode}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (t.deviceModel.isNotBlank()) {
                        Text("• هاتف التسجيل: ${t.deviceModel}")
                    }
                    Text("• تاريخ التسجيل: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(t.createdAt))}")
                }
            },
            confirmButton = {
                Button(onClick = { teacherDetailsToShow = null }) { Text("إغلاق") }
            }
        )
    }

    // Supabase Configuration Dialog
    if (showSupabaseConfigDialog) {
        AlertDialog(
            onDismissRequest = { showSupabaseConfigDialog = false },
            icon = { Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("إعدادات ربط Supabase (PostgreSQL)", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "أدخل عنوان المشروع والمفتاح العام (anon key) المستخرج من لوحة تحكم Supabase لتفعيل المزامنة السحابية الحقيقية.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = supabaseUrlInput,
                        onValueChange = { supabaseUrlInput = it.trim() },
                        label = { Text("Project URL") },
                        placeholder = { Text("https://xxxx.supabase.co") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = supabaseAnonKeyInput,
                        onValueChange = { supabaseAnonKeyInput = it.trim() },
                        label = { Text("anon / public key") },
                        placeholder = { Text("eyJhbGciOi...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUrl = supabaseUrlInput.trim().removeSuffix("/")
                        val cleanKey = supabaseAnonKeyInput.trim()
                        if (cleanUrl.isBlank() || cleanKey.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال الرابط والمفتاح", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        repository.saveSupabaseConfig(cleanUrl, cleanKey)
                        showSupabaseConfigDialog = false
                        Toast.makeText(context, "تم حفظ إعدادات Supabase بنجاح", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            val (ok, msg) = repository.testSupabaseConnection()
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("حفظ واختبار")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupabaseConfigDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Supabase Setup Guide Dialog
    if (showSupabaseGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSupabaseGuideDialog = false },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("دليل إنشاء قاعدة بيانات Supabase من هاتفك", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. افتح متصفح هاتفك وتوجه إلى: supabase.com وسجل الدخول بحساب Google (abdulrqeebfarag2018@gmail.com).", fontSize = 12.sp)
                    Text("2. اضغط New project وأدخل اسم المشروع: rqeeb-teacher-db واختر كلمة مرور قوية.", fontSize = 12.sp)
                    Text("3. بعد إنشاء المشروع، افتح محرر SQL (SQL Editor) واضغط New query، ثم الصق كود الملف: supabase/schema.sql واضغط Run.", fontSize = 12.sp)
                    Text("4. اذهب إلى Project Settings -> API وانسخ Project URL و anon key والصقهما في إعدادات التطبيق هنا.", fontSize = 12.sp)
                    Text("5. بذلك تصبح قاعدة بيانات PostgreSQL جاهزة وتستقبل طلبات المعلمين مباشرة على هاتفك Realme!", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = { showSupabaseGuideDialog = false }) { Text("حسناً، فهمت") }
            }
        )
    }
}

@Composable
private fun TeacherAccountCard(
    teacher: TeacherEntity,
    onViewDetails: () -> Unit,
    onApprove: () -> Unit,
    onSuspend: () -> Unit,
    onReject: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
    onCopyCode: () -> Unit,
    onSendWhatsApp: () -> Unit
) {
    val displayGrade = YemeniCurriculum.toDisplayGrade(teacher.gradeTaught)
    val sectionStr = if (teacher.section.isNotBlank()) " | شعبة ${teacher.section}" else ""
    val (statusLabel, statusColor) = when (teacher.status) {
        TeacherEntity.STATUS_APPROVED -> Pair("مفعل ومعتمد", StatusApproved)
        TeacherEntity.STATUS_PENDING -> Pair("قيد المراجعة", StatusPending)
        TeacherEntity.STATUS_SUSPENDED -> Pair("موقوف مؤقتاً", StatusSuspended)
        TeacherEntity.STATUS_REJECTED -> Pair("مرفوض", StatusRejected)
        else -> Pair(teacher.status, Color.Gray)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("teacher_card_${teacher.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(teacher.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${teacher.schoolName} • ${teacher.governorate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${teacher.subject} • $displayGrade$sectionStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text("الهاتف: ${teacher.phone} • مسجل من: ${teacher.deviceModel.ifBlank { "غير محدد" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (teacher.activationCode.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("كود التفعيل: ${teacher.activationCode}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        Row {
                            TextButton(onClick = onCopyCode, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text("نسخ", fontSize = 11.sp)
                            }
                            TextButton(onClick = onSendWhatsApp, contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Text("واتساب", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (teacher.status == TeacherEntity.STATUS_PENDING) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("approve_button_${teacher.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("موافقة وتفعيل", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("رفض", fontSize = 12.sp)
                    }
                } else if (teacher.status == TeacherEntity.STATUS_APPROVED) {
                    OutlinedButton(
                        onClick = onSendWhatsApp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إرسال الكود", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onSuspend,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusSuspended),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تعليق", fontSize = 12.sp)
                    }
                } else if (teacher.status == TeacherEntity.STATUS_SUSPENDED || teacher.status == TeacherEntity.STATUS_REJECTED) {
                    Button(
                        onClick = onReactivate,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إعادة التفعيل", fontSize = 12.sp)
                    }
                }

                IconButton(onClick = onViewDetails) {
                    Icon(Icons.Default.Info, contentDescription = "تفاصيل الحساب")
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
