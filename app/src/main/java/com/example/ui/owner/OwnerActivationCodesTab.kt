package com.example.ui.owner

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivationCodeEntity
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusSuspended
import com.example.util.ContactUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerActivationCodesTab(
    repository: RqeebRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") } // ALL, AVAILABLE, USED, REVOKED, EXPIRED
    var isTableView by remember { mutableStateOf(false) }

    // Loading states
    var isSyncing by remember { mutableStateOf(false) }
    var isGeneratingBatch by remember { mutableStateOf(false) }
    var isSeedingOfficial100 by remember { mutableStateOf(false) }
    var isCreatingSingle by remember { mutableStateOf(false) }
    var syncErrorMessage by remember { mutableStateOf<String?>(null) }

    // Dialogs
    var showCreateDialog by remember { mutableStateOf(false) }
    var codeToRevoke by remember { mutableStateOf<ActivationCodeEntity?>(null) }
    var revokeReasonInput by remember { mutableStateOf("إلغاء بناءً على طلب الإدارة") }

    // Single code creation form state
    var newTeacherName by remember { mutableStateOf("") }
    var newTeacherPhone by remember { mutableStateOf("") }
    var newDurationDays by remember { mutableIntStateOf(365) }
    var newCustomCode by remember { mutableStateOf("") }

    val codesList by repository.getAllActivationCodes().collectAsState(initial = emptyList())

    // Initial load: ensure official codes exist and attempt sync
    LaunchedEffect(Unit) {
        repository.ensureOfficialCodesLoaded()
        isSyncing = true
        val syncRes = repository.syncActivationCodes()
        isSyncing = false
        if (syncRes.isFailure) {
            syncErrorMessage = syncRes.exceptionOrNull()?.message
        }
    }

    val totalCodes = codesList.size
    val availableCount = codesList.count { it.status == "AVAILABLE" && !it.isUsed && !it.isRevoked }
    val usedCount = codesList.count { it.isUsed || it.status == "USED" }
    val revokedCount = codesList.count { it.isRevoked || it.status == "REVOKED" }
    val expiredCount = codesList.count { it.status == "EXPIRED" }

    val latestAvailableCode = remember(codesList) {
        codesList.firstOrNull { it.status == "AVAILABLE" && !it.isUsed && !it.isRevoked }
    }

    val filteredCodes = remember(codesList, filterStatus, searchQuery) {
        codesList.filter { c ->
            val matchesStatus = when (filterStatus) {
                "AVAILABLE" -> c.status == "AVAILABLE" && !c.isUsed && !c.isRevoked
                "USED" -> c.isUsed || c.status == "USED"
                "REVOKED" -> c.isRevoked || c.status == "REVOKED"
                "EXPIRED" -> c.status == "EXPIRED"
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    c.code.lowercase().contains(q) ||
                    c.teacherName.lowercase().contains(q) ||
                    c.teacherPhone.contains(q)
            matchesStatus && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // 1. SECTION TITLE: إدارة أكواد التفعيل
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "إدارة أكواد التفعيل",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "التحكم المركزي في أكواد التفعيل الأحادية الاستخدام (صلاحية عام كامل)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "$totalCodes كود",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Cloud Connection Warning Banner if failed
        if (syncErrorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "تنبيه السحابة: الأكواد محفوظة محلياً. (${syncErrorMessage})",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                isSyncing = true
                                val res = repository.syncActivationCodes()
                                isSyncing = false
                                if (res.isSuccess) {
                                    syncErrorMessage = null
                                    Toast.makeText(context, "تم الاتصال بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("إعادة المحاولة", fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hero Card: Quick Copy Latest Available Code
        if (latestAvailableCode != null) {
            val latest = latestAvailableCode
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        ContactUtils.copyToClipboard(context, "كود التفعيل", latest.code)
                        Toast.makeText(context, "تم نسخ الكود (${latest.code}) إلى الحافظة بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("latest_available_code_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = StatusApproved,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "كود جاهز للإرسال الفوري للمعلم:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusApproved.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "متاح ($availableCount كود)",
                                color = StatusApproved,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = latest.code,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    ContactUtils.copyToClipboard(context, "كود التفعيل", latest.code)
                                    Toast.makeText(context, "تم نسخ الكود (${latest.code}) إلى الحافظة بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("quick_copy_latest_code_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ الكود", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val msg = "مرحباً يا أستاذ، كود التفعيل المخصص لحسابك في تطبيق «المعلم التربوي الذكي» هو:\n${latest.code}\nصالح لمدة عام كامل. يرجى إدخال الكود لتفعيل حسابك مباشرة."
                                    ContactUtils.openTeacherWhatsApp(context, "", msg)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF059669))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("واتساب", fontSize = 12.sp, color = Color(0xFF059669))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // PRIMARY ACTION BUTTONS: إنشاء كود جديد | توليد 100 كود | تثبيت 100 كود المعتمدة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // زر إنشاء كود جديد (Requirement 2)
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.2f).testTag("create_new_code_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إنشاء كود جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // زر توليد 100 كود (Requirement 3)
            OutlinedButton(
                onClick = {
                    isGeneratingBatch = true
                    coroutineScope.launch {
                        val res = repository.generateBatchCodes(100, "RQ")
                        isGeneratingBatch = false
                        if (res.isSuccess) {
                            Toast.makeText(context, "تم توليد 100 كود تفعيل جديدة لمدة عام كامل!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "تم الحفظ محلياً: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isGeneratingBatch,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.1f).testTag("generate_batch_100_codes_button")
            ) {
                if (isGeneratingBatch) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("توليد 100 كود", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // زر تثبيت الـ 100 كود المعتمدة (T001 - T100)
            FilledTonalButton(
                onClick = {
                    isSeedingOfficial100 = true
                    coroutineScope.launch {
                        val res = repository.seedOfficial100Codes()
                        isSeedingOfficial100 = false
                        if (res.isSuccess) {
                            Toast.makeText(context, "تم تفعيل وتثبيت الـ 100 كود المعتمدة (T001 - T100) بنجاح!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "تم التثبيت محلياً", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isSeedingOfficial100,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.2f).testTag("seed_official_100_button")
            ) {
                if (isSeedingOfficial100) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFD97706))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("100 كود المعتمدة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary Action Row: نسخ كل المتاح | تصدير | تبديل جدول/بطاقات | تحديث
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // زر نسخ جميع الأكواد المتاحة (Requirement 3)
            OutlinedButton(
                onClick = {
                    val availableCodes = codesList.filter { it.status == "AVAILABLE" && !it.isUsed && !it.isRevoked }
                    if (availableCodes.isEmpty()) {
                        Toast.makeText(context, "لا توجد أكواد متاحة للنسخ حاليًا", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    val sb = StringBuilder()
                    sb.append("=== قائمة أكواد تفعيل تطبيق المعلم التربوي الذكي (${availableCodes.size} كود) ===\n\n")
                    availableCodes.forEachIndexed { i, c ->
                        sb.append("${i + 1}. ${c.code}  (صلاحية: سنة كاملة)\n")
                    }
                    ContactUtils.copyToClipboard(context, "أكواد تفعيل المعلم التربوي الذكي", sb.toString().trim())
                    Toast.makeText(context, "تم نسخ جميع الأكواد المتاحة (${availableCodes.size} كود) إلى الحافظة!", Toast.LENGTH_LONG).show()
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).testTag("copy_all_available_codes_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("نسخ كل المتاح", fontSize = 11.sp)
            }

            // تصدير ملف
            OutlinedButton(
                onClick = {
                    if (codesList.isEmpty()) {
                        Toast.makeText(context, "لا توجد أكواد لتصديرها", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    val sb = StringBuilder()
                    sb.append("الكود,اسم المعلم,رقم الهاتف,تاريخ الإنشاء,الحالة,مدة الصلاحية\n")
                    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    codesList.forEach { c ->
                        val dateStr = df.format(Date(c.createdAt))
                        sb.append("${c.code},\"${c.teacherName}\",${c.teacherPhone},$dateStr,${c.status},${c.durationDays} يوم\n")
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, sb.toString())
                        putExtra(Intent.EXTRA_SUBJECT, "أكواد تفعيل تطبيق المعلم التربوي الذكي")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة قائمة الأكواد"))
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(0.9f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصدير", fontSize = 11.sp)
            }

            // Toggle Table View vs Cards View (Requirement 4)
            IconButton(
                onClick = { isTableView = !isTableView },
                modifier = Modifier.size(36.dp).testTag("toggle_table_view_button")
            ) {
                Icon(
                    if (isTableView) Icons.Default.ViewAgenda else Icons.Default.TableChart,
                    contentDescription = if (isTableView) "عرض بطاقات" else "عرض جدول",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Refresh from cloud
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        isSyncing = true
                        val res = repository.syncActivationCodes()
                        isSyncing = false
                        if (res.isSuccess) {
                            syncErrorMessage = null
                            Toast.makeText(context, "تم تحديث الأكواد من السحابة", Toast.LENGTH_SHORT).show()
                        } else {
                            syncErrorMessage = res.exceptionOrNull()?.message
                            Toast.makeText(context, "تم التحديث محلياً", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.size(36.dp).testTag("refresh_codes_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Bar (Requirement 7)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث بالكود، اسم المعلم، أو رقم الهاتف...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("search_codes_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = filterStatus == "ALL",
                    onClick = { filterStatus = "ALL" },
                    label = { Text("الكل ($totalCodes)", fontSize = 10.sp) }
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == "AVAILABLE",
                    onClick = { filterStatus = "AVAILABLE" },
                    label = { Text("متاح ($availableCount)", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusApproved.copy(alpha = 0.2f),
                        selectedLabelColor = StatusApproved
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == "USED",
                    onClick = { filterStatus = "USED" },
                    label = { Text("مستخدم ($usedCount)", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == "REVOKED",
                    onClick = { filterStatus = "REVOKED" },
                    label = { Text("ملغى ($revokedCount)", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusRejected.copy(alpha = 0.2f),
                        selectedLabelColor = StatusRejected
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == "EXPIRED",
                    onClick = { filterStatus = "EXPIRED" },
                    label = { Text("منتهي ($expiredCount)", fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. DISPLAY CODES: Table View OR Cards View (Requirement 4)
        if (filteredCodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VpnKeyOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد أكواد تفعيل مطابقة للبحث", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                repository.seedOfficial100Codes()
                            }
                        }
                    ) {
                        Text("تثبيت الـ 100 كود المعتمدة الآن")
                    }
                }
            }
        } else if (isTableView) {
            // Table View containing: الكود، اسم المعلم، رقم الهاتف، تاريخ الإنشاء، حالة الكود، مدة الصلاحية
            ActivationCodesTable(
                codes = filteredCodes,
                onCopy = { code ->
                    ContactUtils.copyToClipboard(context, "كود التفعيل", code.code)
                    Toast.makeText(context, "تم نسخ الكود (${code.code})!", Toast.LENGTH_SHORT).show()
                },
                onWhatsApp = { code ->
                    val msg = "مرحباً يا أستاذ، كود التفعيل الخاص بحسابك في تطبيق «المعلم التربوي الذكي» هو:\n${code.code}\nصلاحية عام كامل."
                    ContactUtils.openTeacherWhatsApp(context, code.teacherPhone, msg)
                },
                onRevoke = { code ->
                    revokeReasonInput = "إلغاء بواسطة المالك"
                    codeToRevoke = code
                }
            )
        } else {
            // Detailed Cards View with all 6 required fields clearly displayed
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCodes, key = { it.id + it.code }) { codeItem ->
                    ActivationCodeDetailedCard(
                        codeItem = codeItem,
                        onCopy = {
                            ContactUtils.copyToClipboard(context, "كود التفعيل", codeItem.code)
                            Toast.makeText(context, "تم نسخ الكود (${codeItem.code})!", Toast.LENGTH_SHORT).show()
                        },
                        onSendWhatsApp = {
                            val msg = "مرحباً يا أستاذ، كود التفعيل المخصص لحسابك في تطبيق «المعلم التربوي الذكي» هو:\n${codeItem.code}\nصالح لمدة عام كامل."
                            ContactUtils.openTeacherWhatsApp(context, codeItem.teacherPhone, msg)
                        },
                        onRevoke = {
                            revokeReasonInput = "إلغاء بواسطة المالك"
                            codeToRevoke = codeItem
                        }
                    )
                }
            }
        }
    }

    // ==============================================================================
    // DIALOG: إنشاء كود جديد (Requirement 2)
    // ==============================================================================
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreatingSingle) showCreateDialog = false },
            icon = {
                Icon(Icons.Default.AddModerator, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("إنشاء كود تفعيل جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "حدد اسم المعلم، رقم الهاتف، ومدة التفعيل لإنشاء كود مخصص ومعتمد:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newTeacherName,
                        onValueChange = { newTeacherName = it },
                        label = { Text("اسم المعلم (اختياري)", fontSize = 12.sp) },
                        placeholder = { Text("مثال: أ. محمد عبد الله", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_code_teacher_name_input")
                    )

                    OutlinedTextField(
                        value = newTeacherPhone,
                        onValueChange = { newTeacherPhone = it },
                        label = { Text("رقم الهاتف (اليمن)", fontSize = 12.sp) },
                        placeholder = { Text("مثال: 785502919", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_code_teacher_phone_input")
                    )

                    // مدة التفعيل (Requirement 2)
                    Text("مدة التفعيل:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = newDurationDays == 365,
                            onClick = { newDurationDays = 365 },
                            label = { Text("سنة كاملة (365 يوم)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1.3f)
                        )
                        FilterChip(
                            selected = newDurationDays == 90,
                            onClick = { newDurationDays = 90 },
                            label = { Text("فصل (90)", fontSize = 10.sp) },
                            modifier = Modifier.weight(0.9f)
                        )
                        FilterChip(
                            selected = newDurationDays == 30,
                            onClick = { newDurationDays = 30 },
                            label = { Text("شهر (30)", fontSize = 10.sp) },
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    OutlinedTextField(
                        value = newCustomCode,
                        onValueChange = { newCustomCode = it.uppercase() },
                        label = { Text("كود مخصص (اختياري - أو يولد تلقائياً)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCreatingSingle = true
                        coroutineScope.launch {
                            val res = repository.createSingleActivationCode(
                                teacherName = newTeacherName.trim(),
                                teacherPhone = newTeacherPhone.trim(),
                                durationDays = newDurationDays,
                                customCode = newCustomCode.trim().takeIf { it.isNotBlank() }
                            )
                            isCreatingSingle = false
                            if (res.isSuccess) {
                                val created = res.getOrThrow()
                                showCreateDialog = false
                                newTeacherName = ""
                                newTeacherPhone = ""
                                newCustomCode = ""
                                ContactUtils.copyToClipboard(context, "كود التفعيل", created.code)
                                Toast.makeText(context, "تم إنشاء الكود (${created.code}) ونسخه إلى الحافظة بنجاح!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "خطأ: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isCreatingSingle,
                    modifier = Modifier.testTag("submit_create_new_code_button")
                ) {
                    if (isCreatingSingle) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("إنشاء واعتماد الكود")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false },
                    enabled = !isCreatingSingle
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ==============================================================================
    // DIALOG: إلغاء كود تفعيل (Requirement 7)
    // ==============================================================================
    if (codeToRevoke != null) {
        val c = codeToRevoke!!
        AlertDialog(
            onDismissRequest = { codeToRevoke = null },
            icon = { Icon(Icons.Default.Block, contentDescription = null, tint = StatusRejected, modifier = Modifier.size(36.dp)) },
            title = { Text("إلغاء كود التفعيل", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هل أنت متأكد من إلغاء الكود التالي؟ لن يتمكن المعلم من استخدامه بعد الإلغاء.")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            c.code,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (c.teacherName.isNotBlank() && c.teacherName != "كود عام متاح") {
                        Text("المعلم المرتبط: ${c.teacherName} (${c.teacherPhone})", fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = revokeReasonInput,
                        onValueChange = { revokeReasonInput = it },
                        label = { Text("سبب الإلغاء") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = repository.revokeActivationCode(c.id, revokeReasonInput.trim())
                            codeToRevoke = null
                            if (res.isSuccess) {
                                Toast.makeText(context, "تم إلغاء كود التفعيل بنجاح", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "تم الإلغاء محلياً", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("تأكيد الإلغاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { codeToRevoke = null }) {
                    Text("تراجع")
                }
            }
        )
    }
}

// ==============================================================================
// TABLE VIEW COMPONENT (Requirement 4)
// Contains: الكود، اسم المعلم، رقم الهاتف، تاريخ الإنشاء، حالة الكود، مدة الصلاحية
// ==============================================================================
@Composable
private fun ActivationCodesTable(
    codes: List<ActivationCodeEntity>,
    onCopy: (ActivationCodeEntity) -> Unit,
    onWhatsApp: (ActivationCodeEntity) -> Unit,
    onRevoke: (ActivationCodeEntity) -> Unit
) {
    val scrollState = rememberScrollState()
    val df = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الكود", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(130.dp), textAlign = TextAlign.Center)
                Text("اسم المعلم", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(120.dp), textAlign = TextAlign.Center)
                Text("رقم الهاتف", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                Text("تاريخ الإنشاء", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(95.dp), textAlign = TextAlign.Center)
                Text("حالة الكود", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(85.dp), textAlign = TextAlign.Center)
                Text("مدة الصلاحية", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(110.dp), textAlign = TextAlign.Center)
                Text("إجراءات", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
            }

            HorizontalDivider()

            // Table Rows
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(codes, key = { it.id + it.code }) { codeItem ->
                    val (statusLabel, statusColor) = getCodeStatusInfo(codeItem)
                    val isAvailable = !codeItem.isUsed && !codeItem.isRevoked && codeItem.status != "REVOKED" && codeItem.status != "EXPIRED"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopy(codeItem) }
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = codeItem.code,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isAvailable) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.width(130.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (codeItem.teacherName.isNotBlank()) codeItem.teacherName else "كود عام متاح",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(120.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (codeItem.teacherPhone.isNotBlank()) codeItem.teacherPhone else "—",
                            fontSize = 11.sp,
                            modifier = Modifier.width(100.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = df.format(Date(codeItem.createdAt)),
                            fontSize = 10.sp,
                            modifier = Modifier.width(95.dp),
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.width(85.dp), contentAlignment = Alignment.Center) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    statusLabel,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${codeItem.durationDays} يوم (سنة)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(110.dp),
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.width(100.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onCopy(codeItem) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            if (isAvailable && codeItem.teacherPhone.isNotBlank()) {
                                IconButton(onClick = { onWhatsApp(codeItem) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = "واتساب", tint = Color(0xFF059669), modifier = Modifier.size(15.dp))
                                }
                            }
                            if (isAvailable) {
                                IconButton(onClick = { onRevoke(codeItem) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Block, contentDescription = "إلغاء", tint = StatusRejected, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

// ==============================================================================
// DETAILED CARD VIEW COMPONENT (Requirement 4)
// Contains: الكود، اسم المعلم، رقم الهاتف، تاريخ الإنشاء، حالة الكود، مدة الصلاحية
// ==============================================================================
@Composable
private fun ActivationCodeDetailedCard(
    codeItem: ActivationCodeEntity,
    onCopy: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onRevoke: () -> Unit
) {
    val (statusLabel, statusColor) = getCodeStatusInfo(codeItem)
    val isAvailable = !codeItem.isUsed && !codeItem.isRevoked && codeItem.status != "REVOKED" && codeItem.status != "EXPIRED"
    val df = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAvailable) 2.dp else 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy() }
            .testTag("code_card_${codeItem.code}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Code + Status Badge + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        codeItem.code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isAvailable) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            statusLabel,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onCopy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("نسخ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isAvailable && codeItem.teacherPhone.isNotBlank()) {
                        IconButton(onClick = onSendWhatsApp, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "واتساب", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                        }
                    }

                    if (isAvailable) {
                        IconButton(onClick = onRevoke, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Block, contentDescription = "إلغاء", tint = StatusRejected, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Row 2: Teacher Name & Phone (Requirement 4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (codeItem.teacherName.isNotBlank() && codeItem.teacherName != "كود عام متاح") {
                            "المعلم: ${codeItem.teacherName}"
                        } else {
                            "كود عام متاح لأي معلم"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (codeItem.teacherPhone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(codeItem.teacherPhone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Row 3: Creation Date + Validity Duration (Requirement 4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "تاريخ الإنشاء: ${df.format(Date(codeItem.createdAt))}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "الصلاحية: ${codeItem.durationDays} يوم (سنة كاملة)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun getCodeStatusInfo(codeItem: ActivationCodeEntity): Pair<String, Color> {
    return when {
        codeItem.isRevoked || codeItem.status == "REVOKED" -> Pair("ملغى", StatusRejected)
        codeItem.isUsed || codeItem.status == "USED" -> Pair("مستخدم", Color(0xFF64748B))
        codeItem.status == "EXPIRED" -> Pair("منتهي", StatusSuspended)
        else -> Pair("متاح للنسخ", StatusApproved)
    }
}
