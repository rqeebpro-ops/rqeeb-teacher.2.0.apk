package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiService
import com.example.data.model.AiToolType
import com.example.data.model.TeacherEntity
import com.example.util.PrintUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsScreen(
    teacher: TeacherEntity,
    geminiService: GeminiService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTool by remember { mutableStateOf<AiToolType?>(null) }
    var inputTopic by remember { mutableStateOf("") }
    var inputSubject by remember { mutableStateOf(teacher.subject) }
    var inputGrade by remember { mutableStateOf(teacher.stage) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Conversational Chat Messages for TEACHER_ASSISTANT
    val chatMessages = remember {
        mutableStateListOf(
            Pair("assistant", "أهلاً بك يا أستاذ ${teacher.fullName} في مساعد رقيب الذكي! كيف أستطيع دعمك اليوم في تحضير الحصص أو صياغة الأسئلة أو حل الإشكاليات التربوية؟")
        )
    }
    var chatInput by remember { mutableStateOf("") }
    var isChatLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (activeTool == null) "أدوات الذكاء الاصطناعي للمعلم" else activeTool!!.titleAr)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeTool != null) {
                                activeTool = null
                                resultText = null
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("ai_tools_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (resultText != null) {
                        IconButton(onClick = {
                            PrintUtils.shareText(context, activeTool?.titleAr ?: "مخرجات رقيب", resultText!!)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (activeTool == null) {
            // Tools Catalog Grid / List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "اختر الأداة الذكية لمساعدتك في الفصل المدرسي:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                items(AiToolType.values()) { tool ->
                    AiToolCard(
                        tool = tool,
                        onClick = {
                            activeTool = tool
                            resultText = null
                        }
                    )
                }
            }
        } else if (activeTool == AiToolType.TEACHER_ASSISTANT) {
            // Conversational AI Teaching Assistant
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(14.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatMessages) { msg ->
                        val isUser = msg.first == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Text(
                                    text = msg.second,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    if (isChatLoading) {
                        item {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("رقيب يكتب الآن...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("اكتب سؤالك أو استشارتك التربوية هنا...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                val query = chatInput.trim()
                                chatMessages.add(Pair("user", query))
                                chatInput = ""
                                isChatLoading = true
                                coroutineScope.launch {
                                    val prompt = """
                                        أنت 'رقيب' المساعد التربوي الذكي الخاص بالمعلم ${teacher.fullName} (مادة ${teacher.subject}).
                                        أجب بمهنية ودقة علمية ولطافة باللغة العربية:
                                        السؤال: $query
                                    """.trimIndent()
                                    val res = geminiService.generateContent(prompt)
                                    isChatLoading = false
                                    val reply = if (res.isSuccess) res.getOrThrow() else "أعتذر يا أستاذ، تعذر الاتصال بمحرك الذكاء الاصطناعي حالياً. يمكنك التحقق من إعدادات المفتاح أو المحاولة مجدداً."
                                    chatMessages.add(Pair("assistant", reply))
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال")
                    }
                }
            }
        } else {
            // General Single-Tool View
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            activeTool!!.descriptionAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = inputTopic,
                            onValueChange = { inputTopic = it },
                            label = { Text("المفهوم أو الموضوع أو السؤال المطلوب *") },
                            placeholder = { Text("مثال: نظرية فيثاغورس، التمثيل الضوئي، إعراب الأفعال الخمسة...") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_tool_input")
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputSubject,
                                onValueChange = { inputSubject = it },
                                label = { Text("المادة") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = inputGrade,
                                onValueChange = { inputGrade = it },
                                label = { Text("الصف") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                if (inputTopic.isBlank()) {
                                    Toast.makeText(context, "يرجى كتابة الموضوع أو المفهوم أولاً", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isLoading = true
                                resultText = null
                                coroutineScope.launch {
                                    val out = geminiService.executeTeachingTool(
                                        toolTitle = activeTool!!.titleAr,
                                        topic = inputTopic,
                                        subject = inputSubject,
                                        grade = inputGrade
                                    )
                                    resultText = out
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("run_ai_tool_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("رقيب يجهز الإجابة...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("توليد الإجابة بالذكاء الاصطناعي")
                            }
                        }
                    }
                }

                if (resultText != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "نتيجة ${activeTool!!.titleAr}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                IconButton(onClick = {
                                    PrintUtils.shareText(context, activeTool!!.titleAr, resultText!!)
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة")
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = resultText!!,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiToolCard(tool: AiToolType, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getToolIcon(tool.iconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(tool.titleAr, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    tool.descriptionAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getToolIcon(name: String): ImageVector {
    return when (name) {
        "Lightbulb" -> Icons.Default.Lightbulb
        "FormatListNumbered" -> Icons.Default.FormatListNumbered
        "Psychology" -> Icons.Default.Psychology
        "Assignment" -> Icons.Default.Assignment
        "Quiz" -> Icons.Default.Quiz
        "Public" -> Icons.Default.Public
        "Mic" -> Icons.Default.Mic
        else -> Icons.Default.SmartToy
    }
}
