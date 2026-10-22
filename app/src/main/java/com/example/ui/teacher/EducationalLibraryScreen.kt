package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BookEntity
import com.example.data.model.TeacherEntity
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationalLibraryScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    onUseInLesson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val books by repository.getBooks(teacher.id).collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBookForView by remember { mutableStateOf<BookEntity?>(null) }

    val filteredBooks = remember(books, searchQuery) {
        books.filter { b ->
            searchQuery.isBlank() ||
                    b.title.contains(searchQuery, ignoreCase = true) ||
                    b.subject.contains(searchQuery, ignoreCase = true) ||
                    b.grade.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مكتبة الكتب والمناهج المدرسية") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("lib_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_book")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة كتاب جديد")
            }
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
                placeholder = { Text("ابحث في الكتب والمناهج الدراسية...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredBooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LibraryBooks,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لا توجد كتب دراسية مضافة حالياً. اضغط على (+) لإضافة كتابك أو مرجعك.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredBooks, key = { it.id }) { book ->
                        Card(
                            onClick = { selectedBookForView = book },
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.MenuBook,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                book.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                "${book.subject} • ${book.grade}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            repository.deleteBook(book.id)
                                            Toast.makeText(context, "تم حذف الكتاب من المكتبة", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    book.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onUseInLesson(book.extractedText)
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("استخدام في التحضير", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { selectedBookForView = book },
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("عرض المحتوى", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Book Dialog
    if (showAddDialog) {
        var bTitle by remember { mutableStateOf("") }
        var bSubject by remember { mutableStateOf(teacher.subject) }
        var bGrade by remember { mutableStateOf(teacher.stage) }
        var bDesc by remember { mutableStateOf("") }
        var bText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
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
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إضافة كتاب أو مقرر جديد للمكتبة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = bTitle,
                        onValueChange = { bTitle = it },
                        label = { Text("عنوان الكتاب / المقرر *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = bSubject, onValueChange = { bSubject = it }, label = { Text("المادة") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = bGrade, onValueChange = { bGrade = it }, label = { Text("الصف") }, singleLine = true, modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(
                        value = bDesc,
                        onValueChange = { bDesc = it },
                        label = { Text("وصف مختصر للكتاب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bText,
                        onValueChange = { bText = it },
                        label = { Text("النص المستخرج أو نصوص الوحدات *") },
                        placeholder = { Text("الصق هنا نصوص الوحدات أو الفصول المدرسية لاستخدامها في تحضير الدروس وتوليد الاختبارات...") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                if (bTitle.isNotBlank() && bText.isNotBlank()) {
                                    coroutineScope.launch {
                                        repository.saveBook(
                                            BookEntity(
                                                teacherId = teacher.id,
                                                title = bTitle.trim(),
                                                subject = bSubject.trim(),
                                                grade = bGrade.trim(),
                                                description = bDesc.trim().ifBlank { "كتاب دراسي لمادة $bSubject" },
                                                extractedText = bText.trim()
                                            )
                                        )
                                        showAddDialog = false
                                        Toast.makeText(context, "تمت إضافة الكتاب إلى المكتبة بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ في المكتبة")
                        }
                    }
                }
            }
        }
    }

    // View Book Content Dialog
    if (selectedBookForView != null) {
        val b = selectedBookForView!!
        Dialog(onDismissRequest = { selectedBookForView = null }) {
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
                    Text(b.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("${b.subject} | ${b.grade}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    Text(
                        b.extractedText,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.heightIn(max = 300.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val text = b.extractedText
                                selectedBookForView = null
                                onUseInLesson(text)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("استخدام في التحضير")
                        }
                        OutlinedButton(
                            onClick = { selectedBookForView = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إغلاق")
                        }
                    }
                }
            }
        }
    }
}
