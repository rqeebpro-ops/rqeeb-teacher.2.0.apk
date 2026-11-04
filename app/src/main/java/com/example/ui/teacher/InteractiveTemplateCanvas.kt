package com.example.ui.teacher

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamEntity
import com.example.data.model.ExamQuestion
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Data Model for Interactive Draggable Text Elements on the Exam Template Paper
 */
data class DraggableTextElement(
    val id: String,
    val label: String,
    var text: String,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var fontSizeSp: Int = 12,
    var isBold: Boolean = true,
    var textColorHex: String = "#006874",
    var isVisible: Boolean = true,
    val section: String = "HEADER_RIGHT" // HEADER_RIGHT, HEADER_CENTER, HEADER_LEFT, INSTRUCTIONS, QUESTION_BOX, FOOTER_SCORE
)

/**
 * Interactive Compose Component representing the Exam Template Canvas matching the attached reference image.
 * Features:
 * - In-place text editing & drag-and-drop position manipulation (تحريك وتعديل العناصر)
 * - Real-time sync with Supabase / Cloud Teacher Profile (`RqeebRepository`)
 * - Live A4 paper rendering with drag handles, font scale sliders, and position resets
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InteractiveTemplateCanvas(
    teacher: TeacherEntity,
    exam: ExamEntity,
    questions: List<ExamQuestion>,
    repository: RqeebRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSyncingSupabase by remember { mutableStateOf(false) }

    // Live Teacher Data State (Bound to Supabase / Local Cache)
    var currentTeacher by remember { mutableStateOf(teacher) }

    // Initialize Draggable Text Elements from Teacher and Exam Data
    val elementsMap = remember {
        mutableStateMapOf<String, DraggableTextElement>().apply {
            put("country", DraggableTextElement("country", "اسم الدولة", "الجمهورية اليمنية", 0f, 0f, 11, true, "#006874", true, "HEADER_RIGHT"))
            put("ministry", DraggableTextElement("ministry", "الوزارة", "وزارة التربية والتعليم والبحث العلمي", 0f, 0f, 10, true, "#0F172A", true, "HEADER_RIGHT"))
            put("office", DraggableTextElement("office", "المكتب الرئيسي", "مكتب التربية والتعليم", 0f, 0f, 10, false, "#334155", true, "HEADER_RIGHT"))
            put("directorate", DraggableTextElement("directorate", "اسم المديرية", "مديرية: ${currentTeacher.directorate.ifBlank { "معين التعليمية" }}", 0f, 0f, 10, true, "#0F172A", true, "HEADER_RIGHT"))
            put("school", DraggableTextElement("school", "اسم المدرسة (Supabase)", "المدرسة: ${currentTeacher.schoolName.ifBlank { exam.schoolName.ifBlank { "مدرسة المختار النموذجية" } }}", 0f, 0f, 11, true, "#006874", true, "HEADER_RIGHT"))
            put("teacher_name", DraggableTextElement("teacher_name", "اسم المعلم (Supabase)", "أستاذ المادة: ${currentTeacher.fullName.ifBlank { "أ. المعلم القدير" }}", 0f, 0f, 10, true, "#1E293B", true, "HEADER_RIGHT"))

            put("subject_title", DraggableTextElement("subject_title", "عنوان المادة والاختبار", "اختبار مادة : ${exam.subject}", 0f, 0f, 13, true, "#006874", true, "HEADER_CENTER"))
            put("exam_type", DraggableTextElement("exam_type", "نوع الاختبار والعام", "${exam.examType.ifBlank { "اختبار نهاية الفصل الأول" }} (2025/2026م)", 0f, 0f, 10, true, "#FFFFFF", true, "HEADER_CENTER"))

            put("exam_date", DraggableTextElement("exam_date", "التاريخ", "التاريخ: 1446 / 05 / 15 هـ", 0f, 0f, 10, true, "#0F172A", true, "HEADER_LEFT"))
            put("grade_section", DraggableTextElement("grade_section", "الصف والشعبة", "الصف: ${YemeniCurriculum.toDisplayGrade(exam.grade)} (شعبة أ)", 0f, 0f, 10, true, "#006874", true, "HEADER_LEFT"))
            put("duration", DraggableTextElement("duration", "زمن الاختبار", "الزمن: ${exam.durationMinutes} دقيقة", 0f, 0f, 10, false, "#0F172A", true, "HEADER_LEFT"))
            put("student_name", DraggableTextElement("student_name", "اسم الطالب", "اسم الطالب: ...................................................", 0f, 0f, 10, false, "#475569", true, "HEADER_LEFT"))

            put("instructions", DraggableTextElement("instructions", "تعليمات الاختبار", "أجب مستعيناً بالله عن جميع الأسئلة التالية بخط جميل وواضح في نفس الورقة:", 0f, 0f, 12, true, "#006874", true, "INSTRUCTIONS"))
        }
    }

    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var editingElement by remember { mutableStateOf<DraggableTextElement?>(null) }
    var primaryThemeColorHex by remember { mutableStateOf("#006874") }
    val primaryThemeColor = remember(primaryThemeColorHex) {
        try { Color(android.graphics.Color.parseColor(primaryThemeColorHex)) } catch (_: Throwable) { Color(0xFF006874) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("محرر القالب التفاعلي (سحب وتعديل العناصر)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("مرتبط ببيانات المعلم والمدرسة في Supabase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                },
                actions = {
                    // Supabase Live Sync Button
                    IconButton(
                        onClick = {
                            isSyncingSupabase = true
                            coroutineScope.launch {
                                try {
                                    val updatedTeacher = repository.getTeacherById(teacher.id)
                                    if (updatedTeacher != null) {
                                        currentTeacher = updatedTeacher
                                        // Update dynamic elements
                                        elementsMap["school"]?.let { it.text = "المدرسة: ${updatedTeacher.schoolName}" }
                                        elementsMap["teacher_name"]?.let { it.text = "أستاذ المادة: ${updatedTeacher.fullName}" }
                                        elementsMap["directorate"]?.let { it.text = "مديرية: ${updatedTeacher.directorate}" }
                                        Toast.makeText(context, "تمت مزامنة واسترجاع بيانات المعلم والمدرسة من Supabase بنجاح!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "البيانات الحالية محدثة من السحابة.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "خطأ في المزامنة: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSyncingSupabase = false
                                }
                            }
                        },
                        modifier = Modifier.testTag("supabase_sync_btn")
                    ) {
                        if (isSyncingSupabase) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = "مزامنة Supabase", tint = Color(0xFF0284C7))
                        }
                    }

                    // Reset Drag Offsets Button
                    IconButton(
                        onClick = {
                            elementsMap.values.forEach {
                                it.offsetX = 0f
                                it.offsetY = 0f
                            }
                            Toast.makeText(context, "تم إعادة الموضعة التلقائية لجميع العناصر!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "إعادة ضبط المحاذاة", tint = Color(0xFFD97706))
                    }
                }
            )
        },
        bottomBar = {
            // Selected Element Quick Control Strip
            selectedElementId?.let { selId ->
                elementsMap[selId]?.let { elem ->
                    Surface(
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryThemeColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("العنصر المSelected: ${elem.label}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = primaryThemeColor)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { editingElement = elem },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل النص", tint = primaryThemeColor)
                                    }
                                    IconButton(
                                        onClick = {
                                            elem.offsetX = 0f
                                            elem.offsetY = 0f
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.CenterFocusWeak, contentDescription = "توسيط", tint = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = { selectedElementId = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد", tint = Color.Red)
                                    }
                                }
                            }

                            // Font Scale Slider & Drag Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("حجم الخط (${elem.fontSizeSp}sp):", fontSize = 11.sp)
                                Slider(
                                    value = elem.fontSizeSp.toFloat(),
                                    onValueChange = { elem.fontSizeSp = it.roundToInt() },
                                    valueRange = 8f..22f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("موقع X:${elem.offsetX.roundToInt()} Y:${elem.offsetY.roundToInt()}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE2E8F0))
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instructions Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF16A34A))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🖐️ السحب والتعديل التفاعلي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15803D))
                        Text("اضغط واسحب أي عنصر نصي لتغيير موقعه على ورقة الاختبار، أو انقر عليه لتكبير الخط وتعديل نصوص Supabase مباشرة.", fontSize = 11.sp, color = Color(0xFF166534))
                    }
                }
            }

            // Interactive A4 Paper Container
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag("interactive_canvas_paper")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 3-Section Header Box with Draggable Elements
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, primaryThemeColor),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Right Box
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, primaryThemeColor.copy(alpha = 0.5f)),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf("country", "ministry", "office", "directorate", "school", "teacher_name").forEach { key ->
                                        elementsMap[key]?.let { elem ->
                                            DraggableTextNode(
                                                element = elem,
                                                isSelected = selectedElementId == elem.id,
                                                primaryColor = primaryThemeColor,
                                                onSelect = { selectedElementId = elem.id },
                                                onEdit = { editingElement = elem }
                                            )
                                        }
                                    }
                                }
                            }

                            // Center Box (Eagle Logo & Subject Title)
                            Column(
                                modifier = Modifier.weight(1.2f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                GoldenEagleLogo(modifier = Modifier.size(52.dp))

                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = primaryThemeColor.copy(alpha = 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryThemeColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        elementsMap["subject_title"]?.let { elem ->
                                            DraggableTextNode(
                                                element = elem,
                                                isSelected = selectedElementId == elem.id,
                                                primaryColor = primaryThemeColor,
                                                onSelect = { selectedElementId = elem.id },
                                                onEdit = { editingElement = elem }
                                            )
                                        }

                                        elementsMap["exam_type"]?.let { elem ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = primaryThemeColor,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                DraggableTextNode(
                                                    element = elem,
                                                    isSelected = selectedElementId == elem.id,
                                                    primaryColor = Color.White,
                                                    onSelect = { selectedElementId = elem.id },
                                                    onEdit = { editingElement = elem }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Left Box
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, primaryThemeColor.copy(alpha = 0.5f)),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf("exam_date", "grade_section", "duration", "student_name").forEach { key ->
                                        elementsMap[key]?.let { elem ->
                                            DraggableTextNode(
                                                element = elem,
                                                isSelected = selectedElementId == elem.id,
                                                primaryColor = primaryThemeColor,
                                                onSelect = { selectedElementId = elem.id },
                                                onEdit = { editingElement = elem }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Draggable Banner Row / Instructions
                    elementsMap["instructions"]?.let { elem ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryThemeColor.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryThemeColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DraggableTextNode(
                                element = elem,
                                isSelected = selectedElementId == elem.id,
                                primaryColor = primaryThemeColor,
                                onSelect = { selectedElementId = elem.id },
                                onEdit = { editingElement = elem }
                            )
                        }
                    }

                    // Interactive Questions Preview Boxes
                    questions.take(3).forEachIndexed { qIdx, q ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryThemeColor),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Surface(
                                    color = primaryThemeColor,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("السؤال ${qIdx + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("[ ${q.score} درجات ]", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                                Text(
                                    q.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    // Interactive Bottom Score Summary Table (Matching attached reference image)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryThemeColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(primaryThemeColor.copy(alpha = 0.15f)),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                listOf("السؤال الأول", "الثاني", "الثالث", "الرابع", "المجموع").forEach { title ->
                                    Text(
                                        title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryThemeColor,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = primaryThemeColor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) {
                                    Text("-", fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Single Element Text Edit Dialog
    editingElement?.let { elem ->
        var tempText by remember { mutableStateOf(elem.text) }
        AlertDialog(
            onDismissRequest = { editingElement = null },
            title = { Text("تعديل عنصر: ${elem.label}", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                OutlinedTextField(
                    value = tempText,
                    onValueChange = { tempText = it },
                    label = { Text("النص المكتوب") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        elem.text = tempText
                        editingElement = null
                        Toast.makeText(context, "تم حفظ تعديل النص!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingElement = null }) { Text("إلغاء") }
            }
        )
    }
}

/**
 * Individual Interactive Draggable Text Node Component
 */
@Composable
private fun DraggableTextNode(
    element: DraggableTextElement,
    isSelected: Boolean,
    primaryColor: Color,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(element.offsetX) }
    var offsetY by remember { mutableFloatStateOf(element.offsetY) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    element.offsetX = offsetX
                    element.offsetY = offsetY
                }
            }
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) primaryColor else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable {
                onSelect()
            }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = element.text,
            fontSize = element.fontSizeSp.sp,
            fontWeight = if (element.isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (primaryColor == Color.White) Color.White else try { Color(android.graphics.Color.parseColor(element.textColorHex)) } catch (_: Throwable) { primaryColor },
            textAlign = TextAlign.Center
        )
    }
}
