package com.example.ui.owner

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.BookAiProcessor
import com.example.data.ai.GeminiService
import com.example.data.cloud.CloudBookStorageService
import com.example.data.model.BookEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerBookManagementScreen(
    repository: RqeebRepository,
    geminiService: GeminiService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageService = remember { CloudBookStorageService(context) }
    val aiProcessor = remember { BookAiProcessor(context, repository) }

    val booksList by repository.getBooks().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedGradeFilter by remember { mutableStateOf("ALL") }

    // Upload & Form State
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var currentUriIndex by remember { mutableIntStateOf(0) }
    var showMetadataDialog by remember { mutableStateOf(false) }

    // Progress State
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var uploadStatusMessage by remember { mutableStateOf("") }

    // Book Preview Dialog
    var previewBook by remember { mutableStateOf<BookEntity?>(null) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    // Document Picker Launcher for PDFs
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingUris = uris
            currentUriIndex = 0
            showMetadataDialog = true
        }
    }

    val filteredBooks = remember(booksList, searchQuery, selectedGradeFilter) {
        booksList.filter { b ->
            val matchesGrade = (selectedGradeFilter == "ALL" || b.grade == selectedGradeFilter)
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    b.title.lowercase().contains(q) ||
                    b.subject.lowercase().contains(q) ||
                    b.grade.lowercase().contains(q)
            matchesGrade && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Action Card: «إدارة الكتب والمناهج اليمنية»
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("إدارة الكتب والمناهج اليمنية (Supabase Storage)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            Text("رفع وربط كتب المنهج الدراسي وفهرستها بالذكاء الاصطناعي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isUploading
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفع كتاب PDF جديد", fontWeight = FontWeight.Bold)
                    }
                }

                // Progress Indicator Banner during upload
                if (isUploading) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(uploadStatusMessage, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("$uploadProgress%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { uploadProgress / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }

        // Search & Filter Row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث باسم الكتاب أو المادة أو الصف...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Books List
        if (filteredBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Text("لا توجد كتب مرفوعة حالياً في المكتبة السحابية", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("انقر على زر «رفع كتاب PDF جديد» لرفع وفهرسة كتب المنهج اليمني.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Column {
                                        Text(book.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${book.subject} • ${book.grade} • ${book.term}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (book.processingStatus == "COMPLETED") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        if (book.processingStatus == "COMPLETED") "مفهرس 100%" else "جاري المعالجة",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (book.processingStatus == "COMPLETED") Color(0xFF15803D) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (book.description.isNotBlank()) {
                                Text(book.description, fontSize = 11.sp, color = Color.Gray, maxLines = 2)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "طبعة: ${book.editionYear} • ${book.fileSize / (1024 * 1024)} ميجابايت",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                aiProcessor.processAndIndexBook(book.id) { progress, status ->
                                                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "إعادة الفهرسة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { previewBook = book },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { bookToDelete = book },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Book Metadata Dialog before upload
    if (showMetadataDialog && pendingUris.isNotEmpty()) {
        val currentUri = pendingUris.getOrNull(currentUriIndex)
        var bookNameInput by remember(currentUri) { mutableStateOf("كتاب المنهج اليمني") }
        var stageInput by remember(currentUri) { mutableStateOf("التعليم الأساسي") }
        var gradeInput by remember(currentUri) { mutableStateOf("الصف التاسع الأساسي") }
        var subjectInput by remember(currentUri) { mutableStateOf("الرياضيات") }
        var termInput by remember(currentUri) { mutableStateOf("الفصل الدراسي الأول") }
        var editionYearInput by remember(currentUri) { mutableStateOf("طبعة 2024-2025م") }
        var descriptionInput by remember(currentUri) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                if (!isUploading) {
                    showMetadataDialog = false
                    pendingUris = emptyList()
                }
            },
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    "بيانات كتاب المنهج اليمني (${currentUriIndex + 1} من ${pendingUris.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = bookNameInput,
                        onValueChange = { bookNameInput = it },
                        label = { Text("اسم الكتاب المعتمد *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stageInput,
                            onValueChange = { stageInput = it },
                            label = { Text("المرحلة الدراسية") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gradeInput,
                            onValueChange = { gradeInput = it },
                            label = { Text("الصف الدراسي") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { subjectInput = it },
                            label = { Text("المادة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = termInput,
                            onValueChange = { termInput = it },
                            label = { Text("الفصل الدراسي") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editionYearInput,
                        onValueChange = { editionYearInput = it },
                        label = { Text("سنة الطبعة والمعلومات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("وصف الكتاب (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUri = currentUri ?: return@Button
                        coroutineScope.launch {
                            try {
                                isUploading = true
                                uploadStatusMessage = "رفع ملف PDF إلى Supabase Storage..."
                                uploadProgress = 5

                                val uploadResult = storageService.uploadPdfBook(
                                    pdfUri = targetUri,
                                    originalFileName = "$bookNameInput.pdf"
                                ) { progress ->
                                    uploadProgress = progress
                                }

                                if (uploadResult.isSuccess) {
                                    val (storagePath, localPath) = uploadResult.getOrThrow()
                                    val newBook = BookEntity(
                                        title = bookNameInput,
                                        stage = stageInput,
                                        grade = gradeInput,
                                        subject = subjectInput,
                                        term = termInput,
                                        editionYear = editionYearInput,
                                        description = descriptionInput,
                                        fileName = "$bookNameInput.pdf",
                                        fileSize = 1024L * 1024 * 12, // approx 12MB
                                        storagePath = storagePath,
                                        fileUrl = localPath,
                                        uploadedAt = System.currentTimeMillis(),
                                        processingStatus = "PROCESSING",
                                        processingProgress = 20,
                                        isPublished = true
                                    )

                                    val bookId = repository.insertBook(newBook)

                                    uploadStatusMessage = "بدء معالجة وفهرسة محتوى الكتاب بالذكاء الاصطناعي..."
                                    uploadProgress = 50

                                    aiProcessor.processAndIndexBook(bookId) { p, msg ->
                                        uploadProgress = p
                                        uploadStatusMessage = msg
                                    }

                                    Toast.makeText(context, "تم رفع وفهرسة الكتاب بنجاح!", Toast.LENGTH_SHORT).show()

                                    if (currentUriIndex < pendingUris.size - 1) {
                                        currentUriIndex++
                                    } else {
                                        showMetadataDialog = false
                                        pendingUris = emptyList()
                                    }
                                } else {
                                    Toast.makeText(context, "خطأ في الرفع: ${uploadResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل الرفع: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    enabled = !isUploading
                ) {
                    Text(if (currentUriIndex < pendingUris.size - 1) "حفظ ومتابعة الكتاب التالي" else "بدء الرفع والفهرسة")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMetadataDialog = false
                        pendingUris = emptyList()
                    },
                    enabled = !isUploading
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (bookToDelete != null) {
        val b = bookToDelete!!
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("حذف الكتاب المعتمد", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت تأكد من حذف كتاب «${b.title}» من قاعدة البيانات والمكتبة السحابية؟") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteBookById(b.id)
                            bookToDelete = null
                            Toast.makeText(context, "تم حذف الكتاب بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Book Preview Dialog
    if (previewBook != null) {
        val b = previewBook!!
        AlertDialog(
            onDismissRequest = { previewBook = null },
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(b.title, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("المادة: ${b.subject} • الصف: ${b.grade}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("الفصل: ${b.term} • السنة: ${b.editionYear}", fontSize = 11.sp, color = Color.Gray)
                    Text("حالة الفهرسة: ${b.processingStatus} (${b.processingProgress}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("معاينة الفهرس المحتوى:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        if (b.extractedTextJson.isNotBlank()) "تمت الفهرسة لجميع الدروس والوحدات. الكتاب متصل كمرجع تلقائي لمساعد إعداد الدروس." else "الكتاب بانتظار الفهرسة الكاملة.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { previewBook = null }) { Text("إغلاق المعاينة") }
            }
        )
    }
}
