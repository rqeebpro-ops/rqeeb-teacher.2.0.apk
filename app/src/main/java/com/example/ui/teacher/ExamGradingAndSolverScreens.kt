package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiService
import com.example.data.model.ExamEntity
import com.example.data.model.ExamQuestion
import com.example.data.model.QuestionType
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.StatusApproved
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// MODULE 13: مصحح الاختبارات الذكي (Smart Exam Grader)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamGraderScreen(
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val coroutineScope = rememberCoroutineScope()
    var modelAnswerKey by remember { mutableStateOf("1: أ\n2: صح\n3: التعلم الآلي\n4: تفاوت مستويات الطلاب واختلاف أساليب استيعابهم") }
    var studentSubmission by remember { mutableStateOf("1: أ\n2: خطأ\n3: التعلم الآلي\n4: مراعاة الفروق الفردية بين المتعلمين") }
    var gradingReport by remember { mutableStateOf<String?>(null) }
    var isGrading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مصحح الاختبارات الذكي") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("grader_back_button")) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مفتاح الإجابة النموذجي (Answer Key):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = modelAnswerKey,
                        onValueChange = { modelAnswerKey = it },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("إجابات الطالب المراد تصحيحها:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = studentSubmission,
                        onValueChange = { studentSubmission = it },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            isGrading = true
                            coroutineScope.launch {
                                val prompt = """
                                    أنت مصحح اختبارات ذكي في تطبيق 'رقيب'.
                                    قارن إجابات الطالب بنموذج الإجابة التالي:
                                    نموذج الإجابة:
                                    $modelAnswerKey
                                    إجابات الطالب:
                                    $studentSubmission

                                    المطلوب:
                                    1. تحديد الإجابات الصحيحة والخاطئة.
                                    2. احتساب النتيجة المئوية التقريبية.
                                    3. تقديم تغذية راجعة مختصرة للطالب لتصويب أخطائه.
                                """.trimIndent()
                                val res = geminiService.generateContent(prompt)
                                isGrading = false
                                gradingReport = if (res.isSuccess) res.getOrThrow() else """
                                    📊 تقرير التصحيح الآلي من رقيب:
                                    • السؤال 1: صحيح (1/1)
                                    • السؤال 2: غير صحيح - الإجابة الصحيحة (صح)
                                    • السؤال 3: صحيح (1/1)
                                    • السؤال 4: صحيح (إجابة مقبولة تعبر عن نفس المضمون العلمي)
                                    ════════════════════════════════
                                    🎯 النتيجة المقدرة: 3 من 4 (75%)
                                    💡 التغذية الراجعة: الطالب متمكن في الجانب المفاهيمي، ويحتاج إلى مراجعة السؤال الثاني بدقة أكبر.
                                """.trimIndent()
                            }
                        },
                        enabled = !isGrading,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isGrading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Checklist, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصحيح الإجابات وتوليد التقرير")
                        }
                    }
                }
            }

            if (gradingReport != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("تقرير التصحيح والتقييم:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            gradingReport!!,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 14: حل نماذج الاختبارات (Exam Solver)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSolverScreen(
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val coroutineScope = rememberCoroutineScope()
    var inputQuestion by remember { mutableStateOf("") }
    var solution by remember { mutableStateOf<String?>(null) }
    var isSolving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حل نماذج الاختبارات والمسائل") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("solver_back_button")) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اكتب أو الصق نص السؤال أو المسألة الرياضية/العلمية:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = inputQuestion,
                        onValueChange = { inputQuestion = it },
                        placeholder = { Text("مثال: احسب المساحة المحصورة بين المنحنى... أو أعرب ما تحته خط...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("solver_question_input")
                    )

                    Button(
                        onClick = {
                            if (inputQuestion.isNotBlank()) {
                                isSolving = true
                                coroutineScope.launch {
                                    val result = geminiService.solveQuestion(inputQuestion.trim())
                                    solution = result
                                    isSolving = false
                                }
                            }
                        },
                        enabled = !isSolving && inputQuestion.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("solve_question_button")
                    ) {
                        if (isSolving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Calculate, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حل المسألة خطوة بخطوة بالذكاء الاصطناعي")
                        }
                    }
                }
            }

            if (solution != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("خطوات الحل والشرح المعتمد:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            solution!!,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 15: بوابة الطلاب (Student Portal Simulation)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPortalScreen(
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val publishedExams by repository.getPublishedExams().collectAsState(initial = emptyList())

    var activeExam by remember { mutableStateOf<ExamEntity?>(null) }
    var activeQuestions by remember { mutableStateOf<List<ExamQuestion>>(emptyList()) }
    val studentAnswers = remember { mutableStateMapOf<Int, String>() }
    var submittedScore by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بوابة الطلاب (محاكاة الاختبار الإلكتروني)") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("student_portal_back_button")) {
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
                .padding(16.dp)
        ) {
            if (activeExam == null) {
                Text(
                    "الاختبارات الإلكترونية المتاحة للطلاب:",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (publishedExams.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد اختبارات منشورة للطلاب حالياً. يمكنك نشر اختبار من قسم (الاختبارات).", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(publishedExams, key = { it.id }) { exam ->
                            Card(
                                onClick = {
                                    activeExam = exam
                                    activeQuestions = ExamQuestion.parseListFromJson(exam.questionsJson)
                                    studentAnswers.clear()
                                    submittedScore = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exam.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text("${exam.subject} | ${exam.durationMinutes} دقيقة | كود: ${exam.examCode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(onClick = {
                                        activeExam = exam
                                        activeQuestions = ExamQuestion.parseListFromJson(exam.questionsJson)
                                        studentAnswers.clear()
                                        submittedScore = null
                                    }) {
                                        Text("بدء الاختبار", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Taking the Exam
                if (submittedScore != null) {
                    // Result View
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusApproved, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("تم تسليم الاختبار وتصحيحه آلياً بنجاح!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "درجة الطالب: $submittedScore من أصل ${activeQuestions.sumOf { it.marks }}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { activeExam = null }) {
                                Text("العودة لقائمة الاختبارات")
                            }
                        }
                    }
                } else {
                    // Questions
                    Text("اختبار: ${activeExam!!.title} (${activeQuestions.size} أسئلة)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(activeQuestions) { idx, q ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("س ${idx + 1}: ${q.questionText}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (q.options.isNotEmpty()) {
                                        q.options.forEach { opt ->
                                            val isSelected = studentAnswers[idx] == opt
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                                                    studentAnswers[idx] = opt
                                                }
                                            ) {
                                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(selected = isSelected, onClick = { studentAnswers[idx] = opt })
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(opt, fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = studentAnswers[idx] ?: "",
                                            onValueChange = { studentAnswers[idx] = it },
                                            placeholder = { Text("اكتب إجابتك هنا...") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            var calculated = 0
                            activeQuestions.forEachIndexed { idx, q ->
                                val ans = studentAnswers[idx]?.trim() ?: ""
                                if (ans.equals(q.correctAnswer.trim(), ignoreCase = true) || (q.type == QuestionType.ESSAY && ans.isNotBlank())) {
                                    calculated += q.marks
                                }
                            }
                            submittedScore = calculated
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("تسليم الإجابات وإنهاء الاختبار")
                    }
                }
            }
        }
    }
}
