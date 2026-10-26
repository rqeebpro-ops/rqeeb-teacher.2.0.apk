package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.ai.GeminiService
import com.example.data.model.*
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamGeneratorScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(teacher.subject.ifBlank { "الرياضيات" }) }
    var grade by remember {
        mutableStateOf(
            if (teacher.gradeTaught.isNotBlank()) {
                YemeniCurriculum.toDisplayGrade(teacher.gradeTaught)
            } else {
                "9/ إعدادي"
            }
        )
    }
    var examType by remember { mutableStateOf(YemeniCurriculum.EXAM_TYPES[1]) }
    var difficulty by remember { mutableStateOf("متوسط") }
    var questionCount by remember { mutableIntStateOf(5) }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var totalScore by remember { mutableIntStateOf(20) }

    val questionsList = remember { mutableStateListOf<ExamQuestion>() }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("صانع الاختبارات (المنهج اليمني)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("exam_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val entity = ExamEntity(
                                id = 0,
                                teacherId = teacher.id,
                                title = title.ifBlank { "اختبار في مادة $subject" },
                                subject = subject,
                                grade = grade,
                                examType = examType,
                                durationMinutes = durationMinutes,
                                totalScore = totalScore,
                                schoolName = teacher.schoolName
                            )
                            PrintUtils.printExam(context, entity, questionsList)
                        },
                        modifier = Modifier.testTag("print_exam_pdf_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "طباعة الاختبار PDF")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("بيانات ومواصفات الاختبار", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("موضوع أو عنوان الاختبار *") },
                            placeholder = { Text("مثال: اختبار شهر صفر في الرياضيات") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("exam_title_input")
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Grade Selector (12 display names)
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = grade,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("الصف الدراسي") },
                                    trailingIcon = {
                                        IconButton(onClick = { gradeDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { gradeDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = gradeDropdownExpanded,
                                    onDismissRequest = { gradeDropdownExpanded = false }
                                ) {
                                    YemeniCurriculum.DISPLAY_GRADES.forEach { g ->
                                        DropdownMenuItem(
                                            text = { Text(g) },
                                            onClick = {
                                                grade = g
                                                gradeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Exam Type Selector
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = examType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("نوع الاختبار") },
                                    trailingIcon = {
                                        IconButton(onClick = { typeDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { typeDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = typeDropdownExpanded,
                                    onDismissRequest = { typeDropdownExpanded = false }
                                ) {
                                    YemeniCurriculum.EXAM_TYPES.forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t) },
                                            onClick = {
                                                examType = t
                                                typeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "يرجى تحديد موضوع الاختبار أولاً", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isGenerating = true
                                coroutineScope.launch {
                                    val qs = geminiService.generateExamQuestions(subject, grade, title, examType, questionCount, difficulty)
                                    questionsList.clear()
                                    questionsList.addAll(qs)
                                    isGenerating = false
                                    Toast.makeText(context, "تم توليد أسئلة الاختبار بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("generate_exam_questions_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("توليد أسئلة الاختبار ذكياً (AI)")
                            }
                        }
                    }
                }
            }

            if (questionsList.isNotEmpty()) {
                item {
                    Text("أسئلة الاختبار المعتمدة (${questionsList.size} أسئلة):", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                itemsIndexed(questionsList) { index, question ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("س${index + 1}: ${question.text}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("${question.score} درجات", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp)
                                }
                            }
                            if (question.options.isNotEmpty()) {
                                question.options.forEach { opt ->
                                    Text("• $opt", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("الإجابة النموذجية: ${question.correctAnswer}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                val entity = ExamEntity(
                                    id = 0,
                                    teacherId = teacher.id,
                                    title = title.trim(),
                                    subject = subject,
                                    grade = grade,
                                    examType = examType,
                                    durationMinutes = durationMinutes,
                                    totalScore = totalScore,
                                    schoolName = teacher.schoolName
                                )
                                repository.insertExam(entity)
                                isSaving = false
                                Toast.makeText(context, "تم حفظ الاختبار في سجلات المعلم بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_exam_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ الاختبار في السجلات", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
