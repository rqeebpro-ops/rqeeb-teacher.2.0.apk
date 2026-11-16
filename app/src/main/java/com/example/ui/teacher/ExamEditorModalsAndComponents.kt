package com.example.ui.teacher

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import java.io.File
import java.util.UUID

/**
 * Payload used during Drag & Drop from the Toolbar onto a Question Box on the A4 Paper
 */
sealed class DragPaletteItem(val displayLabel: String, val badgeEmoji: String) {
    data class TablePreset(val rows: Int, val cols: Int, val title: String) :
        DragPaletteItem(title, "📊")

    data class GraphicPreset(val template: LibraryGraphicTemplate) :
        DragPaletteItem(template.label, template.emoji)
}

/**
 * Top Editor Toolbar directly above the A4 Exam Paper:
 * - Insert Table (إدراج جدول)
 * - Upload Image (إدراج صورة)
 * - Graphics Library (مكتبة الرسومات)
 * - Print / Print Preview (طباعة)
 * + Quick Drag & Drop Strip
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamTopEditorToolbar(
    isPrintPreviewMode: Boolean,
    onInsertTableClick: () -> Unit,
    onUploadImageClick: () -> Unit,
    onGraphicsLibraryClick: () -> Unit,
    onTemplateLibraryClick: () -> Unit = {},
    onPrintClick: () -> Unit,
    onTogglePrintPreview: () -> Unit,
    onStartDrag: (DragPaletteItem, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isPrintPreviewMode) {
        // Minimal floating bar in Print Preview Mode so user can print or return to editor
        Surface(
            color = Color(0xFF0F172A),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "معاينة الطباعة الصافية A4 (تم إخفاء جميع أزرار التعديل)",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTogglePrintPreview,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("العودة للمحرر", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onPrintClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طباعة الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Primary Toolbar Buttons: Template Library, Insert Table, Upload Image, Graphics Library, Print, Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onTemplateLibraryClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(40.dp).testTag("toolbar_template_library_btn")
                ) {
                    Icon(Icons.Default.DashboardCustomize, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("مكتبة القوالب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onInsertTableClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(40.dp).testTag("toolbar_insert_table_btn")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("إدراج جدول", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onUploadImageClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(40.dp).testTag("toolbar_upload_image_btn")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("إدراج صورة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onGraphicsLibraryClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(40.dp).testTag("toolbar_graphics_library_btn")
                ) {
                    Icon(Icons.Default.Interests, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("مكتبة الرسومات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onPrintClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006874)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(40.dp).testTag("toolbar_print_btn")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("طباعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTogglePrintPreview,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(40.dp).testTag("toolbar_print_preview_btn")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاينة A4", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Drag-and-Drop Strip
            val quickPalette = remember {
                listOf(
                    DragPaletteItem.TablePreset(3, 3, "جدول مقارنة"),
                    DragPaletteItem.GraphicPreset(ExamGraphicsLibrary.ITEMS.first { it.id == "geo_triangle" }),
                    DragPaletteItem.GraphicPreset(ExamGraphicsLibrary.ITEMS.first { it.id == "plant_sprout" }),
                    DragPaletteItem.GraphicPreset(ExamGraphicsLibrary.ITEMS.first { it.id == "ani_lion" }),
                    DragPaletteItem.GraphicPreset(ExamGraphicsLibrary.ITEMS.first { it.id == "sci_microscope" }),
                    DragPaletteItem.GraphicPreset(ExamGraphicsLibrary.ITEMS.first { it.id == "sci_bulb" }),
                    DragPaletteItem.GraphicPreset(ExamGraphicsLibrary.ITEMS.first { it.id == "sci_magnet" }),
                    DragPaletteItem.GraphicPreset(ExamGraphicsLibrary.ITEMS.first { it.id == "sci_scale" })
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "اسحب وأفلت في أي سؤال:",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                quickPalette.forEach { item ->
                    var chipTopLeft by remember { mutableStateOf(Offset.Zero) }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                chipTopLeft = coords.positionInRoot()
                            }
                            .pointerInput(item) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        onStartDrag(item, chipTopLeft + localOffset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragMove(dragAmount)
                                    },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragCancel() }
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(item.badgeEmoji, fontSize = 14.sp)
                            Text(item.displayLabel, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                            Icon(
                                Icons.Default.DragIndicator,
                                contentDescription = "اسحب",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal to specify Rows x Columns & Target Question for inserting a Table
 */
@Composable
fun InsertTableModalDialog(
    questions: List<ExamQuestion>,
    defaultTargetIndex: Int,
    onDismiss: () -> Unit,
    onConfirmInsert: (targetIndex: Int, table: ExamTableBlock) -> Unit
) {
    var rowsCount by remember { mutableIntStateOf(3) }
    var colsCount by remember { mutableIntStateOf(3) }
    var tableTitle by remember { mutableStateOf("جدول مقارنة / بيانات") }
    var targetIndex by remember {
        mutableIntStateOf(defaultTargetIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("إدراج جدول داخل الاختبار", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = tableTitle,
                    onValueChange = { tableTitle = it },
                    label = { Text("عنوان الجدول (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick presets
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { rowsCount = 4; colsCount = 3; tableTitle = "جدول مقارنة" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) { Text("مقارنة 4×3", fontSize = 10.5.sp) }

                    OutlinedButton(
                        onClick = { rowsCount = 4; colsCount = 2; tableTitle = "جدول مطابقة" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) { Text("توصيل 4×2", fontSize = 10.5.sp) }
                }

                // Rows Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("عدد الصفوف:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(onClick = { if (rowsCount > 1) rowsCount-- }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "تقليل الصفوف")
                        }
                        Text("$rowsCount", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        FilledTonalIconButton(onClick = { if (rowsCount < 10) rowsCount++ }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة الصفوف")
                        }
                    }
                }

                // Columns Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("عدد الأعمدة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(onClick = { if (colsCount > 1) colsCount-- }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "تقليل الأعمدة")
                        }
                        Text("$colsCount", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        FilledTonalIconButton(onClick = { if (colsCount < 6) colsCount++ }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة الأعمدة")
                        }
                    }
                }

                // Target Question Picker
                if (questions.isNotEmpty()) {
                    Text("إدراج داخل السؤال:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        questions.forEachIndexed { idx, _ ->
                            FilterChip(
                                selected = targetIndex == idx,
                                onClick = { targetIndex = idx },
                                label = { Text("س ${idx + 1}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tbl = if (tableTitle.contains("مقارنة") && colsCount == 3) {
                        ExamTableBlock(
                            id = UUID.randomUUID().toString(),
                            title = tableTitle.trim(),
                            rows = listOf(listOf("وجه المقارنة", "العنصر الأول", "العنصر الثاني")) +
                                List((rowsCount - 1).coerceAtLeast(1)) { listOf("", "", "") }
                        )
                    } else {
                        ExamPlaceholderParser.createEmptyTable(rowsCount, colsCount, tableTitle.trim())
                    }
                    onConfirmInsert(targetIndex, tbl)
                },
                modifier = Modifier.testTag("confirm_insert_table_btn")
            ) {
                Text("إدراج الجدول ($rowsCount×$colsCount)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

/**
 * Graphics Library Modal with categorized Emojis & Geometric / Scientific Vector Diagrams
 */
@Composable
fun GraphicsLibraryModalDialog(
    questions: List<ExamQuestion>,
    defaultTargetIndex: Int,
    onDismiss: () -> Unit,
    onInsertGraphic: (targetIndex: Int, graphic: ExamGraphicBlock) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ExamGraphicsLibrary.CAT_ALL) }
    var searchText by remember { mutableStateOf("") }
    var targetIndex by remember {
        mutableIntStateOf(defaultTargetIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0)))
    }

    val filteredItems = remember(selectedCategory, searchText) {
        ExamGraphicsLibrary.ITEMS.filter { item ->
            val catOk = selectedCategory == ExamGraphicsLibrary.CAT_ALL || item.category == selectedCategory
            val q = searchText.trim().lowercase()
            val qOk = q.isEmpty() || item.label.lowercase().contains(q) || item.keywords.any { it.lowercase().contains(q) }
            catOk && qOk
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Interests, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("مكتبة الرسومات التعليمية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("ابحث عن شكل هندسي، نبات، حيوان، أداة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExamGraphicsLibrary.CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                if (questions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إدراج في:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        questions.forEachIndexed { idx, _ ->
                            FilterChip(
                                selected = targetIndex == idx,
                                onClick = { targetIndex = idx },
                                label = { Text("س ${idx + 1}", fontSize = 10.5.sp) }
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onInsertGraphic(targetIndex, ExamGraphicsLibrary.toGraphicBlock(item))
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(item.emoji, fontSize = 32.sp)
                                Text(
                                    item.label,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

/**
 * Renders Attached Graphics, Uploaded Images, and Editable Tables inside each Exam Question Box
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamQuestionRichAttachmentsView(
    question: ExamQuestion,
    isEditMode: Boolean,
    isAnswerKeyView: Boolean,
    isPrintPreviewMode: Boolean,
    onUpdateQuestion: (ExamQuestion) -> Unit
) {
    // 1. Inserted Library Graphics (Large crisp emojis / vector shapes)
    if (question.insertedGraphics.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            question.insertedGraphics.forEachIndexed { gIdx, g ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color.Black),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Box {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (g.emoji.isNotBlank()) {
                                Text(g.emoji, fontSize = (g.sizeDp * 0.65).toInt().coerceIn(28, 68).sp)
                            }
                            if (g.label.isNotBlank()) {
                                Text(g.label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        if (!isPrintPreviewMode && isEditMode) {
                            IconButton(
                                onClick = {
                                    val updated = question.insertedGraphics.toMutableList().apply { removeAt(gIdx) }
                                    onUpdateQuestion(question.copy(insertedGraphics = updated))
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "حذف الرسم", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Uploaded Photos from Device
    if (question.insertedImages.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            question.insertedImages.forEachIndexed { iIdx, img ->
                val imgFile = File(img.localPath)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color.Black),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = if (imgFile.exists()) imgFile else Uri.parse(img.localPath),
                                contentDescription = img.caption.ifBlank { "صورة مرفقة بالسؤال" },
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(img.sizeDp.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            if (!isPrintPreviewMode && isEditMode) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                ) {
                                    IconButton(
                                        onClick = {
                                            val updated = question.insertedImages.toMutableList()
                                            updated[iIdx] = img.copy(sizeDp = (img.sizeDp - 25).coerceAtLeast(70))
                                            onUpdateQuestion(question.copy(insertedImages = updated))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomOut, contentDescription = "تصغير", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val updated = question.insertedImages.toMutableList()
                                            updated[iIdx] = img.copy(sizeDp = (img.sizeDp + 25).coerceAtMost(260))
                                            onUpdateQuestion(question.copy(insertedImages = updated))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "تكبير", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val updated = question.insertedImages.toMutableList().apply { removeAt(iIdx) }
                                            onUpdateQuestion(question.copy(insertedImages = updated))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الصورة", tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                        if (img.caption.isNotBlank()) {
                            Text(img.caption, fontSize = 10.5.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }

    // 3. Editable Black-Bordered Tables
    if (question.insertedTables.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            question.insertedTables.forEachIndexed { tIdx, table ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (table.title.isNotBlank() || (!isPrintPreviewMode && isEditMode)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                table.title.ifBlank { "جدول" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            if (!isPrintPreviewMode && isEditMode) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            val colCount = table.rows.firstOrNull()?.size ?: 3
                                            val updatedRows = table.rows + listOf(List(colCount) { "" })
                                            val updatedTables = question.insertedTables.toMutableList()
                                            updatedTables[tIdx] = table.copy(rows = updatedRows)
                                            onUpdateQuestion(question.copy(insertedTables = updatedTables))
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) { Text("+ صف", fontSize = 10.sp) }

                                    TextButton(
                                        onClick = {
                                            val updatedRows = table.rows.mapIndexed { rIdx, r ->
                                                r + if (rIdx == 0) "عمود" else ""
                                            }
                                            val updatedTables = question.insertedTables.toMutableList()
                                            updatedTables[tIdx] = table.copy(rows = updatedRows)
                                            onUpdateQuestion(question.copy(insertedTables = updatedTables))
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) { Text("+ عمود", fontSize = 10.sp) }

                                    IconButton(
                                        onClick = {
                                            val updatedTables = question.insertedTables.toMutableList().apply { removeAt(tIdx) }
                                            onUpdateQuestion(question.copy(insertedTables = updatedTables))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الجدول", tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Crisp Black-Bordered A4 Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color.Black)
                    ) {
                        table.rows.forEachIndexed { rIdx, rowCells ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (rIdx == 0) Color(0xFFF1F5F9) else Color.White)
                                    .border(0.75.dp, Color.Black)
                            ) {
                                rowCells.forEachIndexed { cIdx, cellValue ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(0.5.dp, Color.Black)
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isPrintPreviewMode && isEditMode) {
                                            BasicTextField(
                                                value = cellValue,
                                                onValueChange = { newVal ->
                                                    val newRow = rowCells.toMutableList().apply { this[cIdx] = newVal }
                                                    val newRows = table.rows.toMutableList().apply { this[rIdx] = newRow }
                                                    val newTables = question.insertedTables.toMutableList()
                                                    newTables[tIdx] = table.copy(rows = newRows)
                                                    onUpdateQuestion(question.copy(insertedTables = newTables))
                                                },
                                                textStyle = TextStyle(
                                                    fontSize = 11.sp,
                                                    fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                                    color = Color.Black,
                                                    textAlign = TextAlign.Center
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Text(
                                                text = cellValue.ifBlank { " " },
                                                fontSize = 11.sp,
                                                fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rich Formatting & Direct Text Editor Strip shown when a question is active/selected for inline editing
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineQuestionRichEditorPanel(
    question: ExamQuestion,
    onUpdateQuestion: (ExamQuestion) -> Unit,
    onOpenInsertTable: () -> Unit,
    onOpenInsertImage: () -> Unit,
    onOpenGraphicsLibrary: () -> Unit,
    onDoneEditing: () -> Unit
) {
    val st = question.richStyle

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rich Formatting Controls Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = st.isBold,
                    onClick = { onUpdateQuestion(question.copy(richStyle = st.copy(isBold = !st.isBold))) },
                    label = { Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
                )
                FilterChip(
                    selected = st.isItalic,
                    onClick = { onUpdateQuestion(question.copy(richStyle = st.copy(isItalic = !st.isItalic))) },
                    label = { Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                FilterChip(
                    selected = st.isUnderline,
                    onClick = { onUpdateQuestion(question.copy(richStyle = st.copy(isUnderline = !st.isUnderline))) },
                    label = { Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )

                // Font Size Stepper
                OutlinedButton(
                    onClick = {
                        onUpdateQuestion(question.copy(richStyle = st.copy(fontSizeSp = (st.fontSizeSp - 1).coerceAtLeast(10))))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(32.dp)
                ) { Text("A-", fontSize = 11.sp) }

                OutlinedButton(
                    onClick = {
                        onUpdateQuestion(question.copy(richStyle = st.copy(fontSizeSp = (st.fontSizeSp + 1).coerceAtMost(22))))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(32.dp)
                ) { Text("A+", fontSize = 11.sp) }

                // Alignment
                IconButton(
                    onClick = { onUpdateQuestion(question.copy(richStyle = st.copy(align = "START"))) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatAlignRight,
                        contentDescription = "محاذاة لليمين",
                        tint = if (st.align == "START") MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { onUpdateQuestion(question.copy(richStyle = st.copy(align = "CENTER"))) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.FormatAlignCenter,
                        contentDescription = "توسيط",
                        tint = if (st.align == "CENTER") MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { onUpdateQuestion(question.copy(richStyle = st.copy(align = "END"))) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatAlignLeft,
                        contentDescription = "محاذاة لليسار",
                        tint = if (st.align == "END") MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Color dots
                listOf("#000000", "#006874", "#B91C1C", "#1D4ED8", "#15803D").forEach { hex ->
                    val swatch = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (st.textColorHex == hex) 2.5.dp else 1.dp,
                                color = if (st.textColorHex == hex) Color.Black else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable {
                                onUpdateQuestion(question.copy(richStyle = st.copy(textColorHex = hex)))
                            }
                    )
                }
            }

            // Inline Question Text Field
            OutlinedTextField(
                value = question.text,
                onValueChange = { newText ->
                    onUpdateQuestion(question.copy(text = newText))
                },
                label = { Text("تحرير صيغة السؤال مباشرة (يمكن كتابة [صورة: نبتة] أو [جدول مقارنة])", fontSize = 11.sp) },
                minLines = 2,
                textStyle = TextStyle(
                    fontSize = st.fontSizeSp.sp,
                    fontWeight = if (st.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (st.isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (st.isUnderline) TextDecoration.Underline else TextDecoration.None
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Quick Actions inside Question Editor
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = {
                        val parsed = ExamPlaceholderParser.parseQuestionPlaceholders(question)
                        onUpdateQuestion(parsed)
                    },
                    label = { Text("تحويل [صورة/جدول]", fontSize = 10.5.sp) },
                    leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = onOpenInsertTable,
                    label = { Text("+ جدول", fontSize = 10.5.sp) }
                )
                AssistChip(
                    onClick = onOpenInsertImage,
                    label = { Text("+ صورة", fontSize = 10.5.sp) }
                )
                AssistChip(
                    onClick = onOpenGraphicsLibrary,
                    label = { Text("+ رسم", fontSize = 10.5.sp) }
                )
                Button(
                    onClick = onDoneEditing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("إنهاء التحرير ✓", fontSize = 11.sp)
                }
            }
        }
    }
}
