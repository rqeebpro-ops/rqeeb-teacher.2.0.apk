package com.example.ui.owner

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppAnnouncementEntity
import com.example.data.model.AppDynamicContentEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusRejected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerAppContentTab(
    repository: RqeebRepository,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var subTab by remember { mutableIntStateOf(0) } // 0: Announcements, 1: Support Info, 2: Guides

    val announcements by repository.getAllAnnouncements().collectAsState(initial = emptyList())
    val dynamicContents by repository.getAllDynamicContent().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        repository.syncAnnouncements()
        repository.syncDynamicContent()
    }

    var showAddAnnouncementDialog by remember { mutableStateOf(false) }
    var announcementToEdit by remember { mutableStateOf<AppAnnouncementEntity?>(null) }
    var announcementToDelete by remember { mutableStateOf<AppAnnouncementEntity?>(null) }

    // Support Contacts state
    var whatsappInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var isSavingContacts by remember { mutableStateOf(false) }

    // Instructions state
    var guideTextInput by remember { mutableStateOf("") }
    var curriculumInfoInput by remember { mutableStateOf("") }
    var isSavingGuides by remember { mutableStateOf(false) }

    LaunchedEffect(dynamicContents) {
        val wa = dynamicContents.find { it.contentKey == "owner_whatsapp" }?.contentValue
        val ph = dynamicContents.find { it.contentKey == "owner_phone" }?.contentValue
        val gt = dynamicContents.find { it.contentKey == "app_guide_text" }?.contentValue
        val ci = dynamicContents.find { it.contentKey == "curriculum_info" }?.contentValue

        whatsappInput = wa ?: YemeniCurriculum.OWNER_WHATSAPP_URL_DEFAULT
        phoneInput = ph ?: YemeniCurriculum.OWNER_PHONE_DEFAULT
        guideTextInput = gt ?: "مرحباً بك في رقيب مساعد المعلم الذكي. يمكنك تحضير الدروس وفق الخطة القياسية وطباعة كشوفات الدرجات والاختبارات المعتمدة."
        curriculumInfoInput = ci ?: "يدعم التطبيق المناهج المدرسية الصادرة عن وزارة التربية والتعليم للجمهورية اليمنية (طبعتي صنعاء وعدن)."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Sub Tabs
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("الإعلانات والتنبيهات (${announcements.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("معلومات التواصل والدعم", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                text = { Text("إرشادات المناهج", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (subTab) {
            0 -> {
                // Announcements Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "الإعلانات والتنبيهات الشريطية داخل التطبيق:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showAddAnnouncementDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_announcement_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعلان جديد", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (announcements.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد إعلانات نشطة حاليًا", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(announcements, key = { it.id.toString() + it.cloudId }) { ann ->
                            AnnouncementItemCard(
                                ann = ann,
                                onToggleActive = {
                                    coroutineScope.launch {
                                        val updated = ann.copy(isActive = !ann.isActive)
                                        repository.saveAnnouncement(updated)
                                    }
                                },
                                onEdit = { announcementToEdit = ann },
                                onDelete = { announcementToDelete = ann }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Support Contacts Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("تعديل قنوات الدعم الفني المعروضة للمعلمين:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                OutlinedTextField(
                                    value = whatsappInput,
                                    onValueChange = { whatsappInput = it },
                                    label = { Text("رقم واتساب الإدارة والدعم الفني") },
                                    placeholder = { Text("+967785502919") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF059669)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("owner_whatsapp_input")
                                )

                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { phoneInput = it },
                                    label = { Text("رقم الهاتف للاتصال المباشر") },
                                    placeholder = { Text("+967785502919") },
                                    leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        isSavingContacts = true
                                        coroutineScope.launch {
                                            repository.saveDynamicContent(
                                                AppDynamicContentEntity(
                                                    contentKey = "owner_whatsapp",
                                                    category = "SUPPORT",
                                                    title = "واتساب الدعم",
                                                    contentValue = whatsappInput.trim()
                                                )
                                            )
                                            repository.saveDynamicContent(
                                                AppDynamicContentEntity(
                                                    contentKey = "owner_phone",
                                                    category = "SUPPORT",
                                                    title = "هاتف الإدارة",
                                                    contentValue = phoneInput.trim()
                                                )
                                            )
                                            isSavingContacts = false
                                            Toast.makeText(context, "تم حفظ معلومات التواصل سحابيًا بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isSavingContacts,
                                    modifier = Modifier.fillMaxWidth().testTag("save_support_info_button")
                                ) {
                                    if (isSavingContacts) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("حفظ التغييرات سحابيًا")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Guides & Instructions Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("إرشادات وتوجيهات المعلم اليمني:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                OutlinedTextField(
                                    value = guideTextInput,
                                    onValueChange = { guideTextInput = it },
                                    label = { Text("نص الإرشادات العامة") },
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = curriculumInfoInput,
                                    onValueChange = { curriculumInfoInput = it },
                                    label = { Text("بيان المناهج المعتمدة وطبعتي صنعاء وعدن") },
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        isSavingGuides = true
                                        coroutineScope.launch {
                                            repository.saveDynamicContent(
                                                AppDynamicContentEntity(
                                                    contentKey = "app_guide_text",
                                                    category = "INSTRUCTIONS",
                                                    title = "إرشادات المعلم",
                                                    contentValue = guideTextInput.trim()
                                                )
                                            )
                                            repository.saveDynamicContent(
                                                AppDynamicContentEntity(
                                                    contentKey = "curriculum_info",
                                                    category = "CURRICULUM",
                                                    title = "بيان المناهج",
                                                    contentValue = curriculumInfoInput.trim()
                                                )
                                            )
                                            isSavingGuides = false
                                            Toast.makeText(context, "تم حفظ الإرشادات سحابيًا بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isSavingGuides,
                                    modifier = Modifier.fillMaxWidth().testTag("save_guides_button")
                                ) {
                                    if (isSavingGuides) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("حفظ التوجيهات سحابيًا")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add/Edit Announcement
    if (showAddAnnouncementDialog || announcementToEdit != null) {
        val editing = announcementToEdit
        AnnouncementEditDialog(
            title = if (editing == null) "إضافة إعلان أو تنبيه جديد" else "تعديل الإعلان",
            initial = editing,
            onDismiss = {
                showAddAnnouncementDialog = false
                announcementToEdit = null
            },
            onSave = { ann ->
                coroutineScope.launch {
                    val r = repository.saveAnnouncement(ann)
                    showAddAnnouncementDialog = false
                    announcementToEdit = null
                    if (r.isSuccess) {
                        Toast.makeText(context, "تم حفظ الإعلان بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "خطأ: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Dialog: Delete Announcement
    if (announcementToDelete != null) {
        val ann = announcementToDelete!!
        AlertDialog(
            onDismissRequest = { announcementToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
            title = { Text("حذف الإعلان", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف الإعلان «${ann.title}»؟") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val r = repository.deleteAnnouncement(ann)
                            announcementToDelete = null
                            if (r.isSuccess) {
                                Toast.makeText(context, "تم حذف الإعلان", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { announcementToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun AnnouncementItemCard(
    ann: AppAnnouncementEntity,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ann.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("النوع: ${ann.type} • الصف: ${ann.targetGrade}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (ann.isActive) StatusApproved.copy(alpha = 0.15f) else StatusRejected.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (ann.isActive) "معروض" else "مخفي",
                            color = if (ann.isActive) StatusApproved else StatusRejected,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Switch(
                        checked = ann.isActive,
                        onCheckedChange = { onToggleActive() }
                    )
                }
            }

            Text(ann.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementEditDialog(
    title: String,
    initial: AppAnnouncementEntity?,
    onDismiss: () -> Unit,
    onSave: (AppAnnouncementEntity) -> Unit
) {
    var titleInput by remember { mutableStateOf(initial?.title ?: "") }
    var contentInput by remember { mutableStateOf(initial?.content ?: "") }
    var typeInput by remember { mutableStateOf(initial?.type ?: "BANNER") }
    var gradeInput by remember { mutableStateOf(initial?.targetGrade ?: "ALL") }
    var priorityInput by remember { mutableStateOf(initial?.priority?.toString() ?: "10") }
    var isActiveInput by remember { mutableStateOf(initial?.isActive ?: true) }

    val types = listOf(
        Pair("BANNER", "إعلان شريطي بارز"),
        Pair("POPUP", "تنبيه منبثق"),
        Pair("NOTICE", "تنويه عام")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("عنوان الإعلان أو التنبيه *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("announcement_title_input")
                    )
                }

                item {
                    Text("نوع الإعلان:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        types.forEach { (k, label) ->
                            FilterChip(
                                selected = typeInput == k,
                                onClick = { typeInput = k },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = contentInput,
                        onValueChange = { contentInput = it },
                        label = { Text("نص الإعلان أو التنبيه *") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("announcement_content_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = gradeInput,
                        onValueChange = { gradeInput = it },
                        label = { Text("الصف المستهدف (ALL للجميع)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = priorityInput,
                        onValueChange = { priorityInput = it },
                        label = { Text("أولوية العرض (رقم)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إظهار الإعلان للمستخدمين فوراً", fontSize = 13.sp)
                        Switch(
                            checked = isActiveInput,
                            onCheckedChange = { isActiveInput = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleInput.isBlank() || contentInput.isBlank()) return@Button
                    val p = priorityInput.toIntOrNull() ?: 0
                    val entity = AppAnnouncementEntity(
                        id = initial?.id ?: 0,
                        cloudId = initial?.cloudId ?: "",
                        title = titleInput.trim(),
                        content = contentInput.trim(),
                        type = typeInput,
                        targetGrade = gradeInput.trim(),
                        isActive = isActiveInput,
                        priority = p
                    )
                    onSave(entity)
                },
                modifier = Modifier.testTag("save_announcement_submit_button")
            ) {
                Text("حفظ ونشر")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
