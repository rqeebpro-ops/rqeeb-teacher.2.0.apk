package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.math.PrimaryMathEngine
import com.example.data.repository.RqeebRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryMathScreen(
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: خطوات تفاعلية, 1: تدريبات متدرجة

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("رياضيات المرحلة الابتدائية (1-6)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("primary_math_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Timeline, contentDescription = null) },
                    label = { Text("الشرح التفاعلي (الحمل والاستلاف)") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                    label = { Text("تدريبات العمليات") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (selectedTab == 0) {
            InteractiveStepsTab(Modifier.padding(innerPadding))
        } else {
            PracticeDrillsTab(Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun InteractiveStepsTab(modifier: Modifier = Modifier) {
    var num1Input by remember { mutableStateOf("704") }
    var num2Input by remember { mutableStateOf("386") }
    var operation by remember { mutableStateOf("-") } // "+" or "-"
    var currentStep by remember { mutableIntStateOf(0) }

    val n1 = num1Input.toIntOrNull() ?: 704
    val n2 = num2Input.toIntOrNull() ?: 386

    val additionSteps = remember(n1, n2, operation) {
        if (operation == "+") PrimaryMathEngine.solveAdditionWithSteps(n1, n2) else emptyList()
    }
    val subtractionSteps = remember(n1, n2, operation) {
        if (operation == "-") PrimaryMathEngine.solveSubtractionWithSteps(maxOf(n1, n2), minOf(n1, n2)) else emptyList()
    }

    val totalSteps = if (operation == "+") additionSteps.size else subtractionSteps.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إعداد العملية الحسابية:", fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = num1Input,
                            onValueChange = { num1Input = it; currentStep = 0 },
                            label = { Text("العدد الأول") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = num2Input,
                            onValueChange = { num2Input = it; currentStep = 0 },
                            label = { Text("العدد الثاني") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = operation == "-",
                            onClick = { operation = "-"; currentStep = 0 },
                            label = { Text("الطرح بالاستلاف عبر الأصفار (-)") }
                        )
                        FilterChip(
                            selected = operation == "+",
                            onClick = { operation = "+"; currentStep = 0 },
                            label = { Text("الجمع بالحمل (+)") }
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$n1 $operation $n2",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "الخطوة الحالية: ${currentStep + 1} من $totalSteps",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (operation == "+" && additionSteps.isNotEmpty() && currentStep < additionSteps.size) {
            val step = additionSteps[currentStep]
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("شرح المنزلة (${step.colName}):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(step.explanation, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
            }
        } else if (operation == "-" && subtractionSteps.isNotEmpty() && currentStep < subtractionSteps.size) {
            val step = subtractionSteps[currentStep]
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("شرح المنزلة (${step.colName}):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(step.explanation, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { if (currentStep > 0) currentStep-- },
                    enabled = currentStep > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("الخطوة السابقة")
                }
                Button(
                    onClick = { if (currentStep < totalSteps - 1) currentStep++ },
                    enabled = currentStep < totalSteps - 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("الخطوة التالية")
                }
            }
        }
    }
}

@Composable
private fun PracticeDrillsTab(modifier: Modifier = Modifier) {
    var selectedGrade by remember { mutableIntStateOf(3) }
    var selectedSkill by remember { mutableStateOf("SUBTRACTION_ZEROS") }
    var currentQuestion by remember { mutableStateOf(PrimaryMathEngine.generatePracticeDrill(selectedGrade, selectedSkill)) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Grade selector using the required "$g/ أساسي" names
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر الصف الدراسي:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..6).forEach { g ->
                            FilterChip(
                                selected = selectedGrade == g,
                                onClick = {
                                    selectedGrade = g
                                    currentQuestion = PrimaryMathEngine.generatePracticeDrill(g, selectedSkill)
                                    selectedAnswer = null
                                    isSubmitted = false
                                    showHint = false
                                },
                                label = { Text("$g/ أساسي") }
                            )
                        }
                    }

                    Text("اختر المهارة المستهدفة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("SUBTRACTION_ZEROS", "الطرح بالاستلاف عبر الأصفار"),
                            Pair("ADDITION_REGROUPING", "الجمع بالحمل (إعادة التجميع)"),
                            Pair("MULTIPLICATION", "جدول الضرب")
                        ).forEach { (code, title) ->
                            FilterChip(
                                selected = selectedSkill == code,
                                onClick = {
                                    selectedSkill = code
                                    currentQuestion = PrimaryMathEngine.generatePracticeDrill(selectedGrade, code)
                                    selectedAnswer = null
                                    isSubmitted = false
                                    showHint = false
                                },
                                label = { Text(title) }
                            )
                        }
                    }
                }
            }
        }

        // Question Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(currentQuestion.questionText, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    currentQuestion.options.forEach { opt ->
                        OutlinedButton(
                            onClick = {
                                if (!isSubmitted) {
                                    selectedAnswer = opt
                                    isSubmitted = true
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = when {
                                    isSubmitted && opt == currentQuestion.correctAnswer -> Color(0xFFDCFCE7)
                                    isSubmitted && selectedAnswer == opt -> Color(0xFFFEE2E2)
                                    selectedAnswer == opt -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$opt", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isSubmitted) {
                        val isCorrect = selectedAnswer == currentQuestion.correctAnswer
                        Text(
                            text = if (isCorrect) "إجابة صحيحة! أحسنت يا بطل 🎉" else "إجابة غير صحيحة. الإجابة هي: ${currentQuestion.correctAnswer}",
                            color = if (isCorrect) Color(0xFF166534) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { showHint = !showHint }) {
                            Text(if (showHint) "إخفاء التلميح" else "عرض تلميح الحل 💡")
                        }

                        Button(
                            onClick = {
                                currentQuestion = PrimaryMathEngine.generatePracticeDrill(selectedGrade, selectedSkill)
                                selectedAnswer = null
                                isSubmitted = false
                                showHint = false
                            }
                        ) {
                            Text("مسألة جديدة")
                        }
                    }

                    if (showHint) {
                        Text(currentQuestion.hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
