package com.example.ui.owner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.EducationalResourceEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusPending
import kotlinx.coroutines.launch
import com.example.data.ai.GeminiService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerContentLibraryTab(
    repository: RqeebRepository,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiService() }
    
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Books, 1: Cloud Resources
    val resourcesList by repository.getAllEducationalResources().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        repository.syncEducationalResources()
    }

    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var resourceToEdit by remember { mutableStateOf<EducationalResourceEntity?>(null) }
    var resourceToDelete by remember { mutableStateOf<EducationalResourceEntity?>(null) }
    var previewImageResource by remember { mutableStateOf<EducationalResourceEntity?>(null) }

    val filteredResources = remember(resourcesList, selectedTypeFilter, searchQuery) {
        resourcesList.filter { res ->
            val matchesType = when (selectedTypeFilter) {
                "PDF" -> res.resourceType == "PDF"
                "IMAGE" -> res.resourceType == "IMAGE"
                "WORKSHEET" -> res.resourceType == "WORKSHEET"
                "EXAM" -> res.resourceType == "EXAM"
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    res.title.lowercase().contains(q) ||
                    res.subject.lowercase().contains(q) ||
                    res.grade.lowercase().contains(q) ||
                    res.unitName.lowercase().contains(q)
            matchesType && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Sub-Tab Switcher
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("📚 إدارة كتب المنهج اليمني", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("📂 المذكرات والمصادر السحابية", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        if (selectedSubTab == 0) {
            OwnerBookManagementScreen(
                repository = repository,
                geminiService = geminiService,
                modifier = Modifier.fillMaxSize()
            )
        } else {
        // Search & Add Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث في المكتبة والمواد...") },
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
                modifier = Modifier.weight(1f).testTag("search_resources_input")
            )

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_educational_resource_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("رفع مادة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Type Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedTypeFilter == "ALL",
                    onClick = { selectedTypeFilter = "ALL" },
                    label = { Text("الكل (${resourcesList.size})", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedTypeFilter == "PDF",
                    onClick = { selectedTypeFilter = "PDF" },
                    label = { Text("كتب وملازم PDF", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedTypeFilter == "IMAGE",
                    onClick = { selectedTypeFilter = "IMAGE" },
                    label = { Text("صور ورسومات", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedTypeFilter == "WORKSHEET",
                    onClick = { selectedTypeFilter = "WORKSHEET" },
                    label = { Text("أوراق عمل", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = selectedTypeFilter == "EXAM",
                    onClick = { selectedTypeFilter = "EXAM" },
                    label = { Text("نماذج اختبارات", fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredResources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد مواد تعليمية مطابقة في المكتبة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Text("إضافة مادة تعليمية الآن")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredResources, key = { it.id.toString() + it.cloudId }) { res ->
                    ResourceItemCard(
                        res = res,
                        onPreview = {
                            if (res.resourceType == "IMAGE") {
                                previewImageResource = res
                            } else if (res.fileUrl.isNotBlank()) {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(res.fileUrl))
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "الرابط: ${res.fileUrl}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "لا يوجد رابط ملف متوفر", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onEdit = { resourceToEdit = res },
                        onDelete = { resourceToDelete = res }
                    )
                }
            }
        }
    }
}

    // Dialog: Add / Edit Resource
    if (showAddDialog || resourceToEdit != null) {
        val editing = resourceToEdit
        ResourceEditDialog(
            title = if (editing == null) "إضافة مادة تعليمية جديدة للمكتبة" else "تعديل المادة التعليمية",
            initial = editing,
            onDismiss = {
                showAddDialog = false
                resourceToEdit = null
            },
            onSave = { res ->
                coroutineScope.launch {
                    val r = repository.saveEducationalResource(res)
                    showAddDialog = false
                    resourceToEdit = null
                    if (r.isSuccess) {
                        Toast.makeText(context, "تم حفظ المادة بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "فشل الحفظ: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Dialog: Delete Resource
    if (resourceToDelete != null) {
        val res = resourceToDelete!!
        AlertDialog(
            onDismissRequest = { resourceToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
            title = { Text("حذف المادة التعليمية", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف «${res.title}» نهائياً من المكتبة وقاعدة البيانات؟") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val r = repository.deleteEducationalResource(res)
                            resourceToDelete = null
                            if (r.isSuccess) {
                                Toast.makeText(context, "تم حذف المادة بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { resourceToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Dialog: Preview Image
    if (previewImageResource != null) {
        val res = previewImageResource!!
        AlertDialog(
            onDismissRequest = { previewImageResource = null },
            title = { Text(res.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (res.fileUrl.startsWith("http")) {
                        Image(
                            painter = rememberAsyncImagePainter(res.fileUrl),
                            contentDescription = res.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("الرابط: ${res.fileUrl}", fontSize = 12.sp)
                    }
                    if (res.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(res.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { previewImageResource = null }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun ResourceItemCard(
    res: EducationalResourceEntity,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (typeIcon, typeColor) = when (res.resourceType) {
        "PDF" -> Pair(Icons.AutoMirrored.Filled.MenuBook, Color(0xFFDC2626))
        "IMAGE" -> Pair(Icons.Default.Image, Color(0xFF2563EB))
        "WORKSHEET" -> Pair(Icons.Default.Assignment, Color(0xFF059669))
        "EXAM" -> Pair(Icons.Default.Quiz, Color(0xFFD97706))
        else -> Pair(Icons.Default.Description, Color(0xFF475569))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = typeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(24.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(res.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val displayGrade = YemeniCurriculum.toDisplayGrade(res.grade)
                Text("${res.subject} • $displayGrade", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (res.unitName.isNotBlank() || res.lessonName.isNotBlank()) {
                    Text("${res.unitName} - ${res.lessonName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreview) {
                    Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.secondary)
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
private fun ResourceEditDialog(
    title: String,
    initial: EducationalResourceEntity?,
    onDismiss: () -> Unit,
    onSave: (EducationalResourceEntity) -> Unit
) {
    var titleInput by remember { mutableStateOf(initial?.title ?: "") }
    var descInput by remember { mutableStateOf(initial?.description ?: "") }
    var typeInput by remember { mutableStateOf(initial?.resourceType ?: "PDF") }
    var subjectInput by remember { mutableStateOf(initial?.subject ?: "الرياضيات") }
    var gradeInput by remember { mutableStateOf(initial?.grade ?: "عام") }
    var unitInput by remember { mutableStateOf(initial?.unitName ?: "") }
    var lessonInput by remember { mutableStateOf(initial?.lessonName ?: "") }
    var fileUrlInput by remember { mutableStateOf(initial?.fileUrl ?: "") }
    var isPublicInput by remember { mutableStateOf(initial?.isPublic ?: true) }

    val types = listOf(
        Pair("PDF", "كتاب / ملزمة PDF"),
        Pair("IMAGE", "صورة / رسم بياني"),
        Pair("WORKSHEET", "ورقة عمل"),
        Pair("EXAM", "نموذج اختبار")
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
                        label = { Text("عنوان المادة التعليمية *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("resource_title_input")
                    )
                }

                item {
                    Text("نوع المادة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        types.forEach { (typeKey, typeLabel) ->
                            FilterChip(
                                selected = typeInput == typeKey,
                                onClick = { typeInput = typeKey },
                                label = { Text(typeLabel, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        label = { Text("المادة الدراسية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = gradeInput,
                        onValueChange = { gradeInput = it },
                        label = { Text("الصف الدراسي (أو عام)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = unitInput,
                        onValueChange = { unitInput = it },
                        label = { Text("الوحدة الدراسية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = lessonInput,
                        onValueChange = { lessonInput = it },
                        label = { Text("الدرس") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = fileUrlInput,
                        onValueChange = { fileUrlInput = it },
                        label = { Text("رابط الملف السحابي (URL أو Storage)") },
                        placeholder = { Text("https://... أو رابط التخزين") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("resource_url_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("الوصف أو الملاحظات") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("متاح لجميع المعلمين في التطبيق", fontSize = 13.sp)
                        Switch(
                            checked = isPublicInput,
                            onCheckedChange = { isPublicInput = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleInput.isBlank()) return@Button
                    val entity = EducationalResourceEntity(
                        id = initial?.id ?: 0,
                        cloudId = initial?.cloudId ?: "",
                        title = titleInput.trim(),
                        description = descInput.trim(),
                        resourceType = typeInput,
                        subject = subjectInput.trim(),
                        grade = gradeInput.trim(),
                        unitName = unitInput.trim(),
                        lessonName = lessonInput.trim(),
                        fileUrl = fileUrlInput.trim(),
                        isPublic = isPublicInput
                    )
                    onSave(entity)
                },
                modifier = Modifier.testTag("save_resource_submit_button")
            ) {
                Text("حفظ المادة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
