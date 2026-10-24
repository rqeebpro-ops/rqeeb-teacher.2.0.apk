package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.math.PrimaryMathEngine
import com.example.data.math.PrimaryMathEngine.AdditionCarryStep
import com.example.data.math.PrimaryMathEngine.BorrowOperation
import com.example.data.math.PrimaryMathEngine.SubtractionBorrowStep
import com.example.data.model.ExamQuestion
import com.example.data.model.QuestionType
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryMathScreen(
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: الخطوات التفاعلية (حمل واستلاف), 1: تدريبات العمليات, 2: مولد اختبارات الرياضيات

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "رياضيات المرحلة الابتدائية (الصفوف 1-6)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            when (selectedTab) {
                                0 -> "شرح تفاعلي للجمع بالحمل والطرح بالاستلاف عبر الأصفار"
                                1 -> "تدريبات تفاعلية متدرجة مع تلميحات فورية"
                                else -> "توليد اختبارات ومسائل رياضيات وفق المنهج اليمني"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("primary_math_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Timeline, contentDescription = null) },
                    label = { Text("الشرح والخطوات", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                    label = { Text("تدريبات تفاعلية", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null) },
                    label = { Text("مولد الاختبارات", fontSize = 11.sp) }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> StepByStepExplanationTab()
                1 -> InteractiveDrillsTab()
                2 -> PrimaryMathExamGeneratorTab(repository = repository)
            }
        }
    }
}

// =========================================================================
// TAB 0: STEP-BY-STEP EXPLANATION (الجمع بالحمل والطرح بالاستلاف عبر الأصفار)
// =========================================================================

@Composable
private fun StepByStepExplanationTab() {
    var operationType by remember { mutableStateOf("SUBTRACTION") } // "ADDITION" or "SUBTRACTION"
    var num1Input by remember { mutableStateOf("504") }
    var num2Input by remember { mutableStateOf("238") }
    var num1 by remember { mutableIntStateOf(504) }
    var num2 by remember { mutableIntStateOf(238) }

    // Stepping control
    var currentStepIndex by remember { mutableIntStateOf(0) }

    // Calculate steps whenever inputs or op changes
    val additionSteps = remember(num1, num2) {
        PrimaryMathEngine.solveAdditionWithCarries(num1, num2)
    }
    val subtractionSteps = remember(num1, num2) {
        if (num1 >= num2) PrimaryMathEngine.solveSubtractionWithBorrowing(num1, num2) else null
    }

    val maxSteps = if (operationType == "ADDITION") additionSteps.size else (subtractionSteps?.steps?.size ?: 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = operationType == "SUBTRACTION",
                    onClick = {
                        operationType = "SUBTRACTION"
                        num1Input = "504"
                        num2Input = "238"
                        num1 = 504
                        num2 = 238
                        currentStepIndex = 0
                    },
                    leadingIcon = { Icon(Icons.Default.Remove, contentDescription = null) },
                    label = { Text("الطرح بالاستلاف (معالجة الأصفار)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = operationType == "ADDITION",
                    onClick = {
                        operationType = "ADDITION"
                        num1Input = "468"
                        num2Input = "375"
                        num1 = 468
                        num2 = 375
                        currentStepIndex = 0
                    },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("الجمع بالحمل (إعادة التجميع)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Preset Examples
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "أمثلة شائعة في المنهج اليمني (اضغط للمعاينة):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (operationType == "SUBTRACTION") {
                            listOf(
                                Pair(504, 238),
                                Pair(600, 247),
                                Pair(1000, 456),
                                Pair(5002, 1384),
                                Pair(803, 467)
                            ).forEach { (a, b) ->
                                SuggestionChip(
                                    onClick = {
                                        num1 = a
                                        num2 = b
                                        num1Input = a.toString()
                                        num2Input = b.toString()
                                        currentStepIndex = 0
                                    },
                                    label = { Text("$a - $b") }
                                )
                            }
                        } else {
                            listOf(
                                Pair(468, 375),
                                Pair(589, 243),
                                Pair(786, 457),
                                Pair(1485, 2678),
                                Pair(99, 99)
                            ).forEach { (a, b) ->
                                SuggestionChip(
                                    onClick = {
                                        num1 = a
                                        num2 = b
                                        num1Input = a.toString()
                                        num2Input = b.toString()
                                        currentStepIndex = 0
                                    },
                                    label = { Text("$a + $b") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Inputs
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل عددين للتطبيق خطوة بخطوة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = num1Input,
                            onValueChange = {
                                num1Input = it.filter { char -> char.isDigit() }
                                val parsed = num1Input.toIntOrNull()
                                if (parsed != null && parsed > 0) {
                                    num1 = parsed
                                    currentStepIndex = 0
                                }
                            },
                            label = { Text(if (operationType == "ADDITION") "العدد الأول" else "المطروح منه (الأكبر)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            if (operationType == "ADDITION") "+" else "-",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = num2Input,
                            onValueChange = {
                                num2Input = it.filter { char -> char.isDigit() }
                                val parsed = num2Input.toIntOrNull()
                                if (parsed != null && parsed > 0) {
                                    num2 = parsed
                                    currentStepIndex = 0
                                }
                            },
                            label = { Text(if (operationType == "ADDITION") "العدد الثاني" else "المطروح (الأصغر)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (operationType == "SUBTRACTION" && num1 < num2) {
                        Text(
                            "تنبيه: في المرحلة الابتدائية يجب أن يكون المطروح منه أكبر من أو يساوي المطروح.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        // Visual Arithmetic Board & Stepper
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header of the problem
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (operationType == "ADDITION") "عملية الجمع الرأسي بالحمل" else "عملية الطرح الرأسي بالاستلاف",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "الخطوة ${if (maxSteps > 0) currentStepIndex + 1 else 0} من $maxSteps",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    HorizontalDivider()

                    // Vertical Math Presentation
                    if (operationType == "ADDITION") {
                        VerticalAdditionView(
                            num1 = num1,
                            num2 = num2,
                            steps = additionSteps,
                            currentStepIndex = currentStepIndex
                        )
                    } else {
                        if (subtractionSteps != null) {
                            VerticalSubtractionView(
                                result = subtractionSteps,
                                currentStepIndex = currentStepIndex
                            )
                        } else {
                            Text("المطروح منه أقل من المطروح", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    HorizontalDivider()

                    // Detailed Explanation Box for the current step
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "تفسير الخطوة الحالية للطلاب:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val explanation = if (operationType == "ADDITION") {
                                if (currentStepIndex < additionSteps.size) {
                                    additionSteps[currentStepIndex].explanation
                                } else {
                                    "اكتملت جميع خطوات الجمع بنجاح!"
                                }
                            } else {
                                val steps = subtractionSteps?.steps ?: emptyList()
                                if (currentStepIndex < steps.size) {
                                    val s = steps[currentStepIndex]
                                    var text = "منزلة ${s.columnName}: ${s.explanation}"
                                    if (s.borrowNeeded && s.borrowStepsTrace.isNotEmpty()) {
                                        text += "\n• مسار الاستلاف عبر الأصفار:\n" + s.borrowStepsTrace.joinToString("\n") { "  - $it" }
                                    }
                                    text
                                } else {
                                    "اكتملت جميع خطوات الطرح بالاستلاف بنجاح!"
                                }
                            }

                            Text(
                                explanation,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Stepper Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentStepIndex > 0) currentStepIndex-- },
                            enabled = currentStepIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الخطوة السابقة")
                        }

                        Button(
                            onClick = {
                                if (currentStepIndex < maxSteps - 1) {
                                    currentStepIndex++
                                } else {
                                    currentStepIndex = 0 // loop or restart
                                }
                            },
                            enabled = maxSteps > 0
                        ) {
                            Text(if (currentStepIndex < maxSteps - 1) "الخطوة التالية" else "إعادة من البداية")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                if (currentStepIndex < maxSteps - 1) Icons.Default.ArrowForward else Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vertical presentation of addition with carries displayed at the top
 */
@Composable
private fun VerticalAdditionView(
    num1: Int,
    num2: Int,
    steps: List<AdditionCarryStep>,
    currentStepIndex: Int
) {
    val num1Str = num1.toString()
    val num2Str = num2.toString()
    val maxLen = maxOf(num1Str.length, num2Str.length) + 1

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // Carry row (small numbers in circles above)
        Row(horizontalArrangement = Arrangement.End) {
            for (i in (maxLen - 1) downTo 0) {
                // Find if there was a carry into this position up to currentStepIndex
                val carryVal = steps.filterIndexed { idx, s -> idx <= currentStepIndex && s.columnIndex == i - 1 }
                    .lastOrNull()?.carryOut ?: 0

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (carryVal > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "$carryVal",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top number row
        Row(horizontalArrangement = Arrangement.End) {
            val padded1 = num1Str.padStart(maxLen, ' ')
            padded1.forEach { char ->
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (char != ' ') char.toString() else "",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Bottom number row with "+"
        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            val padded2 = num2Str.padStart(maxLen - 1, ' ')
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            padded2.forEach { char ->
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (char != ' ') char.toString() else "",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .width((maxLen * 36).dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.onSurface)
        )

        // Result row up to current step
        Row(horizontalArrangement = Arrangement.End) {
            val fullResult = (num1 + num2).toString()
            val revealedDigits = steps.filterIndexed { idx, _ -> idx <= currentStepIndex }
                .map { it.digitResult }

            for (i in (maxLen - 1) downTo 0) {
                val digitToDisplay = if (i < revealedDigits.size) revealedDigits[i].toString() else ""
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digitToDisplay,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Vertical presentation of subtraction with borrow slashes and new values displayed
 */
@Composable
private fun VerticalSubtractionView(
    result: BorrowOperation,
    currentStepIndex: Int
) {
    val num1 = result.minuend
    val num2 = result.subtrahend
    val num1Str = num1.toString()
    val num2Str = num2.toString()
    val maxLen = maxOf(num1Str.length, num2Str.length)

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // Row 1: Modified digits after borrowing (numbers that replaced borrowed/slashed ones)
        Row(horizontalArrangement = Arrangement.End) {
            for (pos in (maxLen - 1) downTo 0) {
                // Determine if this position has been modified up to currentStepIndex
                var modifiedVal: Int? = null
                val relevantSteps = result.steps.filterIndexed { idx, _ -> idx <= currentStepIndex }
                for (s in relevantSteps) {
                    if (s.columnIndex == pos && s.borrowNeeded) {
                        modifiedVal = s.minuendDigit
                    }
                }

                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (modifiedVal != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE53935).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFE53935))
                        ) {
                            Text(
                                text = "$modifiedVal",
                                color = Color(0xFFE53935),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Row 2: Original top number (slashed if modified)
        Row(horizontalArrangement = Arrangement.End) {
            val padded1 = num1Str.padStart(maxLen, ' ')
            padded1.forEachIndexed { idx, char ->
                val colIndex = (maxLen - 1) - idx
                val isSlashed = result.steps.any { it.columnIndex == colIndex && it.borrowNeeded && it.columnIndex <= currentStepIndex }

                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (char != ' ') {
                        Text(
                            text = char.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textDecoration = if (isSlashed) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (isSlashed) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Row 3: Bottom number with "-"
        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            val padded2 = num2Str.padStart(maxLen, ' ')
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            padded2.forEach { char ->
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (char != ' ') {
                        Text(
                            text = char.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .width(((maxLen + 1) * 36).dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.onSurface)
        )

        // Row 4: Subtraction result
        Row(horizontalArrangement = Arrangement.End) {
            val revealedDigits = result.steps.filterIndexed { idx, _ -> idx <= currentStepIndex }
                .map { it.digitResult }

            for (i in (maxLen - 1) downTo 0) {
                val digitToDisplay = if (i < revealedDigits.size) revealedDigits[i].toString() else ""
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digitToDisplay,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// =========================================================================
// TAB 1: INTERACTIVE DRILLS (تدريبات تفاعلية مع تلميحات ذكية)
// =========================================================================

@Composable
private fun InteractiveDrillsTab() {
    var selectedGrade by remember { mutableIntStateOf(3) }
    var selectedSkill by remember { mutableStateOf("SUBTRACTION_ZEROS") }
    var currentQuestion by remember {
        mutableStateOf(PrimaryMathEngine.generatePracticeDrill(3, "SUBTRACTION_ZEROS"))
    }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var showFullSteps by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Grade selector
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
                                    showFullSteps = false
                                },
                                label = { Text("الصف $g") }
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
                            Pair("MULTIPLICATION", "جدول الضرب والضرب في منزلتين"),
                            Pair("ROUNDING", "التقريب لأقرب 10 و 100"),
                            Pair("FRACTIONS", "الكسور المتكافئة والتبسيط")
                        ).forEach { (code, title) ->
                            FilterChip(
                                selected = selectedSkill == code,
                                onClick = {
                                    selectedSkill = code
                                    currentQuestion = PrimaryMathEngine.generatePracticeDrill(selectedGrade, code)
                                    selectedAnswer = null
                                    isSubmitted = false
                                    showHint = false
                                    showFullSteps = false
                                },
                                label = { Text(title) }
                            )
                        }
                    }
                }
            }
        }

        // Score Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "النقاط: $score من $attempts محاولة",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        currentQuestion = PrimaryMathEngine.generatePracticeDrill(selectedGrade, selectedSkill)
                        selectedAnswer = null
                        isSubmitted = false
                        showHint = false
                        showFullSteps = false
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مسألة جديدة", fontSize = 12.sp)
                }
            }
        }

        // Question Presentation Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "السؤال:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Text(
                        currentQuestion.questionText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )

                    // Options Buttons
                    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
                    val surfaceColor = MaterialTheme.colorScheme.surface
                    val primaryBorderColor = MaterialTheme.colorScheme.primary
                    val outlineVariantBorderColor = MaterialTheme.colorScheme.outlineVariant

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentQuestion.options.forEach { opt ->
                            val isChosen = selectedAnswer == opt
                            val isCorrect = opt == currentQuestion.correctAnswer
                            val containerColor = when {
                                !isSubmitted && isChosen -> primaryContainerColor
                                isSubmitted && isCorrect -> Color(0xFFE8F5E9)
                                isSubmitted && isChosen && !isCorrect -> Color(0xFFFFEBEE)
                                else -> surfaceColor
                            }
                            val borderColor = when {
                                !isSubmitted && isChosen -> primaryBorderColor
                                isSubmitted && isCorrect -> Color(0xFF2E7D32)
                                isSubmitted && isChosen && !isCorrect -> Color(0xFFC62828)
                                else -> outlineVariantBorderColor
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                border = BorderStroke(1.5.dp, borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSubmitted) {
                                        selectedAnswer = opt
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        opt,
                                        fontSize = 16.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                                    )

                                    if (isSubmitted) {
                                        if (isCorrect) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                        } else if (isChosen) {
                                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFC62828))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showHint = !showHint },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showHint) "إخفاء التلميح" else "تلميح ذكي")
                        }

                        Button(
                            onClick = {
                                if (selectedAnswer != null && !isSubmitted) {
                                    isSubmitted = true
                                    attempts++
                                    if (selectedAnswer == currentQuestion.correctAnswer) {
                                        score++
                                    }
                                }
                            },
                            enabled = selectedAnswer != null && !isSubmitted,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تحقق من الإجابة")
                        }
                    }

                    // Hint Box
                    if (showHint) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                            border = BorderStroke(1.dp, Color(0xFFFFB300))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(currentQuestion.hint, fontSize = 12.sp, color = Color(0xFF5D4037))
                            }
                        }
                    }

                    // Submission Result & Step-by-Step button
                    if (isSubmitted) {
                        val isCorrect = selectedAnswer == currentQuestion.correctAnswer

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    if (isCorrect) "إجابة صحيحة وممتازة! أحسنت." else "إجابة خاطئة. الإجابة الصحيحة هي: ${currentQuestion.correctAnswer}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )

                                OutlinedButton(
                                    onClick = { showFullSteps = !showFullSteps },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (showFullSteps) "إخفاء خطوات الحل النموذجية" else "عرض خطوات الحل النموذجية كاملة")
                                }

                                if (showFullSteps) {
                                    HorizontalDivider()
                                    Text(
                                        currentQuestion.detailedSteps,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
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

// =========================================================================
// TAB 2: PRIMARY MATH EXAM GENERATOR (مولد اختبارات الرياضيات الابتدائية)
// =========================================================================

@Composable
private fun PrimaryMathExamGeneratorTab(
    repository: RqeebRepository
) {
    val context = LocalContext.current
    var selectedGrade by remember { mutableIntStateOf(4) }
    var examType by remember { mutableStateOf("اختبار شهري") }
    var questionCount by remember { mutableIntStateOf(10) }
    var difficulty by remember { mutableStateOf("متوسط") }

    val selectedSkills = remember {
        mutableStateListOf("SUBTRACTION_ZEROS", "ADDITION_REGROUPING", "MULTIPLICATION", "ROUNDING", "FRACTIONS")
    }

    var generatedExamQuestions by remember { mutableStateOf<List<ExamQuestion>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("STUDENT") } // "STUDENT" or "TEACHER"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Exam Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "إعداد اختبار رياضيات المرحلة الابتدائية (المنهج اليمني)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Grade Chips
                    Text("الصف الدراسي:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..6).forEach { g ->
                            FilterChip(
                                selected = selectedGrade == g,
                                onClick = { selectedGrade = g },
                                label = { Text("الصف $g") }
                            )
                        }
                    }

                    // Exam Type Chips
                    Text("نوع الاختبار:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("اختبار قصير", "اختبار شهري", "اختبار فصلي", "اختبار تدريبي").forEach { t ->
                            FilterChip(
                                selected = examType == t,
                                onClick = { examType = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    // Difficulty & Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("عدد الأسئلة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(5, 10, 15, 20).forEach { cnt ->
                                    FilterChip(
                                        selected = questionCount == cnt,
                                        onClick = { questionCount = cnt },
                                        label = { Text("$cnt") }
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("الصعوبة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("سهل", "متوسط", "متقدم").forEach { d ->
                                    FilterChip(
                                        selected = difficulty == d,
                                        onClick = { difficulty = d },
                                        label = { Text(d) }
                                    )
                                }
                            }
                        }
                    }

                    // Skills checkboxes
                    Text("المهارات المتضمنة في الاختبار:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        Pair("SUBTRACTION_ZEROS", "الطرح بالاستلاف عبر الأصفار المتعددة"),
                        Pair("ADDITION_REGROUPING", "الجمع بالحمل الرأسي والأفقي"),
                        Pair("MULTIPLICATION", "حقائق الضرب والضرب في منزلة ومنزلتين"),
                        Pair("ROUNDING", "التقريب لأقرب 10 و 100 و 1000"),
                        Pair("FRACTIONS", "الكسور الاعتيادية والمقارنة والتبسيط")
                    ).forEach { (code, title) ->
                        val isChecked = selectedSkills.contains(code)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        if (selectedSkills.size > 1) selectedSkills.remove(code)
                                    } else {
                                        selectedSkills.add(code)
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedSkills.add(code)
                                    else if (selectedSkills.size > 1) selectedSkills.remove(code)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(title, fontSize = 12.5.sp)
                        }
                    }

                    Button(
                        onClick = {
                            isGenerating = true
                            generatedExamQuestions = PrimaryMathEngine.generatePrimaryMathExam(
                                grade = selectedGrade,
                                count = questionCount,
                                skills = selectedSkills.toList(),
                                difficulty = difficulty
                            )
                            isGenerating = false
                            Toast.makeText(context, "تم توليد ${generatedExamQuestions.size} سؤال بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توليد اختبار الرياضيات الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Exam Results Card
        if (generatedExamQuestions.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$examType – رياضيات الصف $selectedGrade (${generatedExamQuestions.size} أسئلة)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )

                            // Toggle Student View vs Teacher Key
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = viewMode == "STUDENT",
                                    onClick = { viewMode = "STUDENT" },
                                    label = { Text("ورقة الطالب") }
                                )
                                FilterChip(
                                    selected = viewMode == "TEACHER",
                                    onClick = { viewMode = "TEACHER" },
                                    label = { Text("نموذج الإجابة") }
                                )
                            }
                        }

                        HorizontalDivider()

                        // Questions listing
                        generatedExamQuestions.forEachIndexed { index, q ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("س ${index + 1}: ${q.questionText}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text("${q.marks} درجات", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                                        }
                                    }

                                    if (q.options.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            q.options.forEach { opt ->
                                                Text("• $opt", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (viewMode == "TEACHER") {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE8F5E9),
                                            border = BorderStroke(1.dp, Color(0xFF2E7D32))
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("الإجابة الصحيحة: ${q.correctAnswer}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 12.sp)
                                                if (q.explanation.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text("خطوات الحل: ${q.explanation}", fontSize = 11.sp, color = Color(0xFF1B5E20))
                                                }
                                            }
                                        }
                                    } else {
                                        // Student empty working space
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(6.dp)
                                        ) {
                                            Text("مساحة الحل: .....................................................................", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // PDF Export Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val dummyExam = com.example.data.model.ExamEntity(
                                        teacherId = 1,
                                        title = "$examType – رياضيات الصف $selectedGrade",
                                        subject = "الرياضيات",
                                        grade = "الصف $selectedGrade الابتدائي",
                                        difficulty = difficulty,
                                        durationMinutes = 45,
                                        totalMarks = generatedExamQuestions.sumOf { it.marks },
                                        examCode = "MATH-${selectedGrade}00",
                                        questionsJson = ExamQuestion.listToJsonString(generatedExamQuestions)
                                    )
                                    PrintUtils.printExam(context, dummyExam)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير ورقة الاختبار PDF", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
