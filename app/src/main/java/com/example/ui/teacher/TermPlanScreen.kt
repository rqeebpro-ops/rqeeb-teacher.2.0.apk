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
import com.example.data.model.TeacherEntity
import com.example.data.model.TermPlanEntity
import com.example.data.repository.RqeebRepository
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermPlanScreen(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val plans by repository.getTermPlans(teacher.id).collectAsState(initial = emptyList())
    var selectedTerm by remember { mutableStateOf("الفصل الدراسي الأول") }
    var showAddDialog by remember { mutableStateOf(false) }

    val termFilteredPlans = remember(plans, selectedTerm) {
        plans.filter { it.term == selectedTerm }.sortedBy { it.weekNumber }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الخطة الفصلية وتوزيع المنهج اليمني") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("term_plan_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (termFilteredPlans.isNotEmpty()) {
                        IconButton(onClick = {
                            val text = StringBuilder("الخطة الفصلية - مادة ${teacher.subject} - ${teacher.schoolName}\n")
                            termFilteredPlans.forEach { p ->
                                text.append("الأسبوع ${p.weekNumber}: ${p.unitTitle} - ${p.lessonTitle} (الأهداف: ${p.objectives})\n")
                            }
                            PrintUtils.shareText(context, "الخطة الفصلية للمنهج", text.toString())
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة الخطة")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_term_plan")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة أسبوع للخطة")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("الفصل الدراسي الأول", "الفصل الدراسي الثاني").forEach { t ->
                    FilterChip(
                        selected = selectedTerm == t,
                        onClick = { selectedTerm = t },
                        label = { Text(t, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (termFilteredPlans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CalendarViewWeek,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لا توجد خطة مدخلة لـ ($selectedTerm). اضغط على (+) لإضافة توزيع الأسابيع.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(termFilteredPlans, key = { it.id }) { plan ->
                        Card(
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
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "الأسبوع ${plan.weekNumber}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    IconButton(onClick = {
                                        coroutineScope.launch { repository.deleteTermPlan(plan.id) }
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    "${plan.unitTitle}: ${plan.lessonTitle}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    "الأهداف التعليمية: ${plan.objectives}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (plan.assessmentPlan.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "📌 أسلوب التقويم: ${plan.assessmentPlan}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var weekNum by remember { mutableIntStateOf(termFilteredPlans.size + 1) }
        var unitTitle by remember { mutableStateOf("") }
        var lessonTitle by remember { mutableStateOf("") }
        var objectives by remember { mutableStateOf("") }
        var assessment by remember { mutableStateOf("واجب منزلي وتقويم مرحلي") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إضافة أسبوع في الخطة الفصلية", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = "$weekNum",
                            onValueChange = { weekNum = it.toIntOrNull() ?: weekNum },
                            label = { Text("رقم الأسبوع") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = selectedTerm,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الفصل الدراسي") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    OutlinedTextField(
                        value = unitTitle,
                        onValueChange = { unitTitle = it },
                        label = { Text("اسم الوحدة الدراسية *") },
                        placeholder = { Text("مثال: الوحدة الأولى: المجموعات والعلاقات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = lessonTitle,
                        onValueChange = { lessonTitle = it },
                        label = { Text("الدروس المقررة خلال الأسبوع *") },
                        placeholder = { Text("مثال: مفهوم المجموعة والعمليات عليها") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = objectives,
                        onValueChange = { objectives = it },
                        label = { Text("الأهداف التعليمية المتوقعة") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                if (unitTitle.isNotBlank() && lessonTitle.isNotBlank()) {
                                    coroutineScope.launch {
                                        repository.saveTermPlan(
                                            TermPlanEntity(
                                                teacherId = teacher.id,
                                                subject = teacher.subject,
                                                grade = teacher.stage,
                                                term = selectedTerm,
                                                weekNumber = weekNum,
                                                unitTitle = unitTitle.trim(),
                                                lessonTitle = lessonTitle.trim(),
                                                objectives = objectives.trim(),
                                                assessmentPlan = assessment.trim()
                                            )
                                        )
                                        showAddDialog = false
                                        Toast.makeText(context, "تمت إضافة الأسبوع للخطة الفصلية بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ الأسبوع")
                        }
                    }
                }
            }
        }
    }
}
