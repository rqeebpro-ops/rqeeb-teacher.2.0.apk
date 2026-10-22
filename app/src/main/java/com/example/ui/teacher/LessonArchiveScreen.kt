package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.LessonEntity
import com.example.data.model.TeacherEntity
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonArchiveScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val lessons by repository.getLessons(teacher.id).collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedLessonForView by remember { mutableStateOf<LessonEntity?>(null) }
    var lessonToDelete by remember { mutableStateOf<LessonEntity?>(null) }

    val filteredLessons = remember(lessons, searchQuery) {
        lessons.filter { l ->
            searchQuery.isBlank() ||
                    l.title.contains(searchQuery, ignoreCase = true) ||
                    l.subject.contains(searchQuery, ignoreCase = true) ||
                    l.grade.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أرشيف الدروس المحضرة (${lessons.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("archive_back_button")) {
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في الدروس السابقة حسب العنوان أو المادة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredLessons.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لا توجد خطط دروس محفوظة في الأرشيف حالياً",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLessons, key = { it.id }) { lesson ->
                        val dateFormatted = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(lesson.createdAt))
                        Card(
                            onClick = { selectedLessonForView = lesson },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            lesson.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            "${lesson.subject} • ${lesson.grade} • $dateFormatted",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            PrintUtils.printLesson(context, lesson)
                                        }) {
                                            Icon(Icons.Default.Print, contentDescription = "طباعة / PDF", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { lessonToDelete = lesson }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    lesson.objectives,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { selectedLessonForView = lesson },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    ) {
                                        Text("معاينة الخطة الكاملة", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            PrintUtils.shareText(context, lesson.title, "${lesson.title}\n${lesson.objectives}\n${lesson.detailedExplanation}")
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("مشاركة", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // View Lesson Dialog
    if (selectedLessonForView != null) {
        val l = selectedLessonForView!!
        Dialog(onDismissRequest = { selectedLessonForView = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(l.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("${l.subject} | ${l.grade} | ${l.durationMinutes} دقيقة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("الأهداف التعليمية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(l.objectives, fontSize = 12.sp)
                        }
                        item {
                            Text("التهيئة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(l.introduction, fontSize = 12.sp)
                        }
                        item {
                            Text("الشرح والتفصيل:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(l.detailedExplanation, fontSize = 12.sp)
                        }
                        item {
                            Text("التقويم التكويني:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(l.formativeAssessment, fontSize = 12.sp)
                        }
                        item {
                            Text("الواجب المنزلي:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(l.homework, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                PrintUtils.printLesson(context, l)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة / PDF", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { selectedLessonForView = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إغلاق", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation
    if (lessonToDelete != null) {
        AlertDialog(
            onDismissRequest = { lessonToDelete = null },
            title = { Text("حذف خطة الدرس") },
            text = { Text("هل أنت متأكد من حذف درس (${lessonToDelete?.title}) من الأرشيف؟") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = lessonToDelete?.id ?: return@Button
                        coroutineScope.launch {
                            repository.deleteLesson(id)
                            lessonToDelete = null
                            Toast.makeText(context, "تم حذف الدرس", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { lessonToDelete = null }) { Text("إلغاء") }
            }
        )
    }
}
