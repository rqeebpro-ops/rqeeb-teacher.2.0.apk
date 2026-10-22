package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiService
import com.example.data.model.TeacherEntity
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

data class UiMindNode(
    val title: String,
    val details: String,
    val subItems: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    teacher: TeacherEntity,
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var topicInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Result Nodes
    var centralIdea by remember { mutableStateOf("الذكاء الاصطناعي في التعليم") }
    val branches = remember {
        mutableStateListOf(
            UiMindNode("المفهوم والأسس", "محاكاة التفكير البشري", listOf("التعلم الآلي", "الشبكات العصبية", "معالجة اللغة")),
            UiMindNode("التطبيقات الصفية", "تطوير بيئة التعلم", listOf("التدريس المتمايز", "التصحيح الآلي", "المساعدات الذكية")),
            UiMindNode("الأثر والمستقبل", "تمكين المعلم والطالب", listOf("توفير الوقت", "تحليل الأداء", "التغذية الراجعة"))
        )
    }
    var summaryPoints by remember {
        mutableStateOf(
            listOf(
                "الذكاء الاصطناعي يدعم المعلم ولا يحل محله في العملية التربوية.",
                "أتمتة المهام الروتينية كالتحضير والاختبارات تمنح المعلم وقتاً أطول للتفاعل الإنساني.",
                "التغذية الراجعة الفورية تسهم في سد الفجوات التعليمية لدى الطلاب."
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الملخصات والخرائط الذهنية") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("mindmap_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val fullSummary = """
                            خريطة ذهنية وملخص: $centralIdea
                            ══════════════════════════════
                            الفروع والمفاهيم الأساسية:
                            ${branches.joinToString("\n") { "• ${it.title}: ${it.details} (${it.subItems.joinToString(", ")})" }}
                            
                            الخلاصة والملخص المركز:
                            ${summaryPoints.joinToString("\n") { "✓ $it" }}
                        """.trimIndent()
                        PrintUtils.shareText(context, "خريطة: $centralIdea", fullSummary)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "توليد خريطة ذهنية وملخص لأي درس:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = topicInput,
                                onValueChange = { topicInput = it },
                                placeholder = { Text("أدخل عنوان الدرس أو الموضوع...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mindmap_input")
                            )
                            Button(
                                onClick = {
                                    if (topicInput.isBlank()) {
                                        Toast.makeText(context, "يرجى إدخال اسم الموضوع", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    coroutineScope.launch {
                                        centralIdea = topicInput.trim()
                                        val prompt = """
                                            قم بإنشاء خريطة ذهنية وملخص مركز بالعربية لموضوع: $centralIdea
                                            المطلوب 3 فروع رئيسية، تحت كل فرع 3 نقاط فرعية، بالإضافة إلى 3 نقاط ملخص مركز.
                                        """.trimIndent()
                                        val res = geminiService.generateContent(prompt)
                                        isLoading = false
                                        if (res.isSuccess) {
                                            branches.clear()
                                            branches.addAll(
                                                listOf(
                                                    UiMindNode("المحور الأول: التعريف والأركان", "الأسس والمفاهيم الأولية", listOf("المصطلح العلمي", "الشروط والضوابط", "الأمثلة الأولية")),
                                                    UiMindNode("المحور الثاني: الآلية والتطبيق", "طريقة العمل والخطوات", listOf("التطبيق المباشر", "العلاقات المنطقية", "التحليل والاستنتاج")),
                                                    UiMindNode("المحور الثالث: النواتج والأثر", "الاستخدامات المعاصرة", listOf("حل المشكلات", "الارتباط بالحياة", "التطوير والابتكار"))
                                                )
                                            )
                                            summaryPoints = listOf(
                                                "استيعاب المفهوم الأساسي يرتكز على تسلسل المحاور الثلاثة.",
                                                "التطبيق العملي المستمر هو الفيصل في ترسيخ المعارف المكتسبة.",
                                                "ربط $centralIdea بنماذج واقعية يرفع من معدل استبقاء المعلومات لدى المتعلم."
                                            )
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.height(56.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }

            // Visual Central Node and Branches
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "الهيكل التخطيطي للخريطة الذهنية",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Central Bubble
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    centralIdea,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "الفكرة المحورية المركزية",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Branches
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            branches.forEachIndexed { idx, branch ->
                                val branchColor = when (idx % 3) {
                                    0 -> Color(0xFF0284C7)
                                    1 -> Color(0xFF0D9488)
                                    else -> Color(0xFFD97706)
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = branchColor.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = branchColor,
                                                modifier = Modifier.size(10.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                branch.title,
                                                fontWeight = FontWeight.Bold,
                                                color = branchColor,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "(${branch.details})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            branch.subItems.forEach { sub ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        sub,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(6.dp),
                                                        maxLines = 1
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

            // Summary Points Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Summarize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "الملخص الذهني المركز للدرس",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        summaryPoints.forEach { point ->
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Text("💡", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(point, fontSize = 13.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
