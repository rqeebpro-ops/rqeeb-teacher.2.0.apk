package com.example.ui.teacher

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EducationalResourceEntity
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationalLibraryScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Curriculum Books, 1: Cloud Resources
    var resourceTypeFilter by remember { mutableStateOf("ALL") }

    val books by repository.getBooks().collectAsState(initial = emptyList())
    val cloudResources by repository.getEducationalResourcesFlow().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        repository.syncEducationalResources()
    }

    var previewResource by remember { mutableStateOf<EducationalResourceEntity?>(null) }

    val filteredResources = remember(cloudResources, resourceTypeFilter) {
        if (resourceTypeFilter == "ALL") cloudResources
        else cloudResources.filter { it.resourceType == resourceTypeFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المكتبة والمناهج اليمنية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("library_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("الكتب المدرسية المقررة (${books.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("المذكرات والمصادر السحابية (${cloudResources.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "الكتب المدرسية المعتمدة لوزارة التربية والتعليم (طبعة صنعاء وعدن):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(books) { book ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(book.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${book.subject} • ${book.grade} (${book.edition})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (book.summary.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(book.summary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Cloud Resources Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = resourceTypeFilter == "ALL",
                        onClick = { resourceTypeFilter = "ALL" },
                        label = { Text("الكل", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = resourceTypeFilter == "PDF",
                        onClick = { resourceTypeFilter = "PDF" },
                        label = { Text("PDF وكتب", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = resourceTypeFilter == "IMAGE",
                        onClick = { resourceTypeFilter = "IMAGE" },
                        label = { Text("رسومات ومصورات", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = resourceTypeFilter == "WORKSHEET",
                        onClick = { resourceTypeFilter = "WORKSHEET" },
                        label = { Text("أوراق عمل", fontSize = 11.sp) }
                    )
                }

                if (filteredResources.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "لا توجد مصادر أو مذكرات مرفوعة حالياً من قبل الإدارة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredResources) { res ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { previewResource = res }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = when (res.resourceType) {
                                            "PDF" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                            "IMAGE" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        },
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when (res.resourceType) {
                                                    "PDF" -> Icons.Default.PictureAsPdf
                                                    "IMAGE" -> Icons.Default.Image
                                                    "WORKSHEET" -> Icons.Default.Description
                                                    else -> Icons.Default.Quiz
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(res.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        val gradeStr = YemeniCurriculum.toDisplayGrade(res.grade)
                                        Text("${res.subject} • $gradeStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (res.unitName.isNotBlank()) {
                                            Text("الوحدة: ${res.unitName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            if (res.fileUrl.isNotBlank()) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.fileUrl))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                previewResource = res
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "فتح الملف", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (previewResource != null) {
        val res = previewResource!!
        AlertDialog(
            onDismissRequest = { previewResource = null },
            title = { Text(res.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• المادة: ${res.subject} (${YemeniCurriculum.toDisplayGrade(res.grade)})")
                    if (res.unitName.isNotBlank()) Text("• الوحدة: ${res.unitName}")
                    if (res.lessonName.isNotBlank()) Text("• الدرس: ${res.lessonName}")
                    if (res.description.isNotBlank()) Text("• الوصف: ${res.description}")

                    if (res.resourceType == "IMAGE" && res.fileUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = res.fileUrl,
                            contentDescription = res.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (res.fileUrl.isNotBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.fileUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                            }
                        }
                        previewResource = null
                    }
                ) {
                    Text(if (res.fileUrl.isNotBlank()) "فتح الملف" else "إغلاق")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewResource = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
