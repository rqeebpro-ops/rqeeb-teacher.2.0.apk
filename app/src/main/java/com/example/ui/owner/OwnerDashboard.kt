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
import androidx.compose.material.icons.automirrored.filled.MenuBook
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

    // 0: Teachers, 1: Activation Codes, 2: Content Library, 3: App Content, 4: Stats & Logs, 5: Cloud Settings
    var selectedTab by remember { mutableIntStateOf(0) }

    val teachers by repository.getCloudTeachersFlow().collectAsState(initial = emptyList())
    val auditLogs by repository.getAuditLogsFlow().collectAsState(initial = emptyList())
    val activationCodes by repository.getActivationCodesFlow().collectAsState(initial = emptyList())
    val resources by repository.getEducationalResourcesFlow().collectAsState(initial = emptyList())
    val announcements by repository.getAnnouncementsFlow().collectAsState(initial = emptyList())

    val totalLessons by repository.getTotalLessonsCount().collectAsState(initial = 0)
    val totalExams by repository.getTotalExamsCount().collectAsState(initial = 0)

    val pendingCount = teachers.count { it.status == TeacherEntity.STATUS_PENDING }
    val activeCount = teachers.count { it.status == TeacherEntity.STATUS_APPROVED }
    val suspendedCount = teachers.count { it.status == TeacherEntity.STATUS_SUSPENDED }
    val totalTeachers = teachers.size

    var isSyncing by remember { mutableStateOf(false) }
    var showSupabaseConfigDialog by remember { mutableStateOf(false) }
    var showSupabaseGuideDialog by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }

    var supabaseUrlInput by remember { mutableStateOf(repository.getSupabaseConfig().first) }
    var supabaseAnonKeyInput by remember { mutableStateOf(repository.getSupabaseConfig().second) }
    var isTestingConnection by remember { mutableStateOf(false) }

    val tabTitles = listOf(
        Pair("المعلمون", Icons.Default.People),
        Pair("إدارة الأكواد", Icons.Default.VpnKey),
        Pair("المكتبة", Icons.AutoMirrored.Filled.MenuBook),
        Pair("المحتوى", Icons.Default.Campaign),
        Pair("السجلات", Icons.Default.History),
        Pair("السحابة", Icons.Default.CloudQueue)
    )

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
                                    "إدارة رقيب",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "أ. عبد الرقيب فرج | خادم رقيب المركزي (Supabase)",
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                tabTitles.forEachIndexed { index, pair ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            if (index == 0 && pendingCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("$pendingCount")
                                        }
                                    }
                                ) {
                                    Icon(pair.second, contentDescription = pair.first)
                                }
                            } else {
                                Icon(pair.second, contentDescription = pair.first)
                            }
                        },
                        label = { Text(pair.first, fontSize = 10.sp) },
                        modifier = Modifier.testTag("owner_nav_tab_$index")
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    // Manual and registered teacher management
                    OwnerTeacherManagementTab(repository = repository)
                }
                1 -> {
                    // Batch activation codes, copy, share, export CSV, revoke
                    OwnerActivationCodesTab(repository = repository)
                }
                2 -> {
                    // Educational content library, upload PDFs, diagrams, worksheets, exams
                    OwnerContentLibraryTab(repository = repository)
                }
                3 -> {
                    // App announcements, WhatsApp support number, curriculum text
                    OwnerAppContentTab(repository = repository)
                }
                4 -> {
                    // Stats and Audit Logs
                    OwnerStatsAndLogsTab(
                        totalTeachers = totalTeachers,
                        pendingCount = pendingCount,
                        activeCount = activeCount,
                        suspendedCount = suspendedCount,
                        totalLessons = totalLessons,
                        totalExams = totalExams,
                        codesCount = activationCodes.size,
                        resourcesCount = resources.size,
                        announcementsCount = announcements.size,
                        auditLogs = auditLogs
                    )
                }
                5 -> {
                    // Cloud Settings and PIN management
                    OwnerCloudSettingsTab(
                        repository = repository,
                        onOpenConfigDialog = {
                            supabaseUrlInput = repository.getSupabaseConfig().first
                            supabaseAnonKeyInput = repository.getSupabaseConfig().second
                            showSupabaseConfigDialog = true
                        },
                        onOpenGuideDialog = { showSupabaseGuideDialog = true },
                        onOpenPinDialog = { showPinChangeDialog = true },
                        isTesting = isTestingConnection,
                        onTestConnection = {
                            isTestingConnection = true
                            coroutineScope.launch {
                                val (ok, msg) = repository.testSupabaseConnection()
                                isTestingConnection = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        }
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
            title = { Text("دليل تشغيل Supabase من هاتفك", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. افتح متصفح هاتفك وتوجه إلى: supabase.com وسجل الدخول بحساب Google الخاص بك (abdulrqeebfarag2018@gmail.com).", fontSize = 12.sp)
                    Text("2. اضغط New project وأدخل اسم المشروع: rqeeb-teacher-db واختر كلمة مرور قوية.", fontSize = 12.sp)
                    Text("3. افتح محرر SQL (SQL Editor) واضغط New query، ثم الصق كود الملف: supabase/schema.sql واضغط Run.", fontSize = 12.sp)
                    Text("4. اذهب إلى Project Settings -> API وانسخ Project URL و anon public key والصقهما في إعدادات التطبيق هنا.", fontSize = 12.sp)
                    Text("5. كل شيء يعمل مباشرة: إدارة المعلمين، الأكواد، رفع الكتب والمذكرات، والإعلانات المركزية!", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = { showSupabaseGuideDialog = false }) { Text("حسناً، فهمت") }
            }
        )
    }

    // Owner PIN Change Dialog
    if (showPinChangeDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinChangeDialog = false },
            title = { Text("تغيير رمز PIN للوحة المالك", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { currentPinInput = it },
                        label = { Text("رمز PIN الحالي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("رمز PIN الجديد (6 أرقام)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { confirmPinInput = it },
                        label = { Text("تأكيد رمز PIN الجديد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length < 6) {
                            Toast.makeText(context, "الرمز الجديد يجب أن يكون 6 أرقام على الأقل", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPinInput != confirmPinInput) {
                            Toast.makeText(context, "الرمزان غير متطابقين", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val success = repository.changeOwnerPin(currentPinInput, newPinInput)
                            if (success) {
                                showPinChangeDialog = false
                                Toast.makeText(context, "تم تغيير رمز PIN بنجاح", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "رمز PIN الحالي غير صحيح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("حفظ التغيير")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinChangeDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun OwnerStatsAndLogsTab(
    totalTeachers: Int,
    pendingCount: Int,
    activeCount: Int,
    suspendedCount: Int,
    totalLessons: Int,
    totalExams: Int,
    codesCount: Int,
    resourcesCount: Int,
    announcementsCount: Int,
    auditLogs: List<AuditLogEntity>
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Stats, 1: Audit Logs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("إحصائيات المنصة", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("سجل العمليات الموثق (${auditLogs.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        if (subTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إحصائيات حسابات المعلمين:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("• إجمالي المعلمين المسجلين: $totalTeachers معلم")
                            Text("• الحسابات المعتمدة والنشطة: $activeCount معلم", color = StatusApproved)
                            Text("• الطلبات قيد الانتظار: $pendingCount معلم", color = StatusPending)
                            Text("• الحسابات الموقوفة: $suspendedCount معلم", color = StatusSuspended)
                        }
                    }
                }

                item {
                    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إحصائيات النشاط التعليمي والمحتوى:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("• إجمالي الدروس المحضرة: $totalLessons درس")
                            Text("• إجمالي الاختبارات المنشأة: $totalExams اختبار")
                            Text("• أكواد التفعيل المسجلة: $codesCount كود")
                            Text("• المواد والمذكرات المرفوعة: $resourcesCount ملف")
                            Text("• الإعلانات والتعميمات: $announcementsCount إعلان")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (auditLogs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد إجراءات إدارية مسجلة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(auditLogs) { log ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${log.action}: ${log.targetTeacherName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                                    Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerCloudSettingsTab(
    repository: RqeebRepository,
    onOpenConfigDialog: () -> Unit,
    onOpenGuideDialog: () -> Unit,
    onOpenPinDialog: () -> Unit,
    isTesting: Boolean,
    onTestConnection: () -> Unit
) {
    val isConfigured = repository.isSupabaseConfigured()
    val currentUrl = repository.getSupabaseConfig().first

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("إعدادات الربط السحابي والأمان المركزي", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        }

        // Supabase Status Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConfigured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
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
                            Text("خادم PostgreSQL (Supabase)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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

                    Text(
                        text = if (currentUrl.isNotBlank()) "رابط المشروع: $currentUrl" else "لم يتم تعيين رابط Supabase بعد. اضغط على الزر أدناه للربط.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenConfigDialog,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعدادات الربط", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onTestConnection,
                            enabled = !isTesting,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اختبار الاتصال", fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenGuideDialog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("دليل تشغيل Supabase من هاتفك", fontSize = 12.sp)
                    }
                }
            }
        }

        // Owner Security & Credentials Card
        item {
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("بيانات المالك والأمان الصارم:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("• بريد المالك المعتمد: ${YemeniCurriculum.OWNER_EMAIL_DEFAULT}")
                    Text("• هاتف المالك للواتساب والدعم: ${YemeniCurriculum.OWNER_PHONE_DEFAULT}")
                    Text("• التحقق الأمني: دوال RPC محمية بـ Security Definer مع فحص PIN المشفر وحظر تام للوصول العام غير المصرح.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedButton(
                        onClick = onOpenPinDialog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تغيير رمز PIN للوحة المالك", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
