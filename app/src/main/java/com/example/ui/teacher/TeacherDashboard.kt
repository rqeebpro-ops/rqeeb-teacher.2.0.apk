package com.example.ui.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ai.GeminiService
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.util.ContactUtils
import kotlinx.coroutines.launch

enum class TeacherSection(
    val titleAr: String,
    val subtitleAr: String,
    val icon: ImageVector,
    val color: Color
) {
    LESSON_PREP("تحضير درس جديد", "وفق المعايير والنموذج اليمني المعتمد", Icons.Default.MenuBook, Color(0xFF0D47A1)),
    AI_TOOLS("مساعد المعلم بالذكاء الاصطناعي", "تبسيط وتوقع الأسئلة وبودكاست تعليمي", Icons.Default.AutoAwesome, Color(0xFF0284C7)),
    LIBRARY("مكتبة الكتب والمناهج اليمنية", "طبعات صنعاء وعدن والمقررات الرسمية", Icons.Default.LibraryBooks, Color(0xFFD97706)),
    LESSON_ARCHIVE("أرشيف الدروس", "تصفح وتحميل وطباعة الخطط بصيغة PDF", Icons.Default.FolderSpecial, Color(0xFF2563EB)),
    EXAMS("الاختبارات", "مولد الاختبارات بالذكاء الاصطناعي", Icons.Default.Quiz, Color(0xFF0D9488)),
    MANUAL_EXAM_EDITOR("محرر الاختبارات اليدوي", "صياغة وتعديل وتنسيق الأسئلة يدوياً", Icons.Default.EditNote, Color(0xFF7C3AED)),
    ONLINE_EXAMS("الاختبارات الإلكترونية", "تعيين كود وزمن ونشر لطلاب الفصل", Icons.Default.Public, Color(0xFF059669)),
    ORAL_EXAMS("الاختبارات الشفوية", "سحب عشوائي للطلاب ومصفوفة تقييم Rubric", Icons.Default.RecordVoiceOver, Color(0xFFCA8A04)),
    HOMEWORK("الواجبات المنزلية", "إسناد ومتابعة تسليم المهام الصفية", Icons.Default.Assignment, Color(0xFF4F46E5)),
    STUDENT_GRADES("درجات الطلاب", "كشوفات أعمال السنة والاختبارات والمعدل", Icons.Default.Score, Color(0xFF16A34A)),
    ATTENDANCE("الحضور والغياب", "رصد الحضور اليومي وتتبع نسب الغياب", Icons.Default.Checklist, Color(0xFF0891B2)),
    TIMETABLE("جدول الحصص", "تنظيم الحصص الأسبوعية والقاعات الدراسية", Icons.Default.CalendarMonth, Color(0xFF0284C7)),
    TERM_PLAN("الخطة الفصلية", "توزيع المنهج اليمني على أسابيع الفصل", Icons.Default.CalendarViewWeek, Color(0xFF10B981)),
    WHITEBOARD("السبورة التفاعلية", "سبورة خضراء وبيضاء للشرح باللمس", Icons.Default.Draw, Color(0xFFE11D48)),
    MIND_MAPS("الملخصات والخرائط الذهنية", "هياكل بصرية وملخصات مركزة للمفاهيم", Icons.Default.Hub, Color(0xFF059669)),
    EXAM_GRADER("مصحح الاختبارات بالصور", "تصحيح الإجابات آلياً ومقارنتها بالنموذج", Icons.Default.FactCheck, Color(0xFF9333EA)),
    EXAM_SOLVER("حل نماذج الاختبارات", "حل المسائل خطوة بخطوة مع توضيح القواعد", Icons.Default.Calculate, Color(0xFF0284C7)),
    STUDENT_PORTAL("بوابة الطلاب", "محاكاة خوض الطلاب للاختبارات الإلكترونية", Icons.Default.School, Color(0xFF10B981)),
    STATISTICS("الإحصائيات", "لوحة قياس الأداء التربوي وتحصيل الصفوف", Icons.Default.BarChart, Color(0xFF3B82F6)),
    PRIMARY_MATH("رياضيات المرحلة الابتدائية", "إتقان الجمع بالحمل والطرح بالاستلاف وبنك مسائل", Icons.Default.Calculate, Color(0xFFE65100)),
    SETTINGS("الإعدادات", "الملف الشخصي والبيانات والمظهر", Icons.Default.Settings, Color(0xFF64748B))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboard(
    teacher: TeacherEntity,
    repository: RqeebRepository,
    geminiService: GeminiService,
    onTeacherUpdated: (TeacherEntity) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentSection by remember { mutableStateOf<TeacherSection?>(null) }
    var activeTeacher by remember { mutableStateOf(teacher) }
    var injectedLessonContent by remember { mutableStateOf("") }

    if (currentSection != null) {
        when (currentSection!!) {
            TeacherSection.LESSON_PREP -> {
                LessonPrepScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    geminiService = geminiService,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.AI_TOOLS -> {
                AiToolsScreen(
                    teacher = activeTeacher,
                    geminiService = geminiService,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.LIBRARY -> {
                EducationalLibraryScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null },
                    onUseInLesson = { extracted ->
                        injectedLessonContent = extracted
                        currentSection = TeacherSection.LESSON_PREP
                    }
                )
            }
            TeacherSection.LESSON_ARCHIVE -> {
                LessonArchiveScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.EXAMS, TeacherSection.MANUAL_EXAM_EDITOR, TeacherSection.ONLINE_EXAMS -> {
                ExamGeneratorScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    geminiService = geminiService,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.ORAL_EXAMS -> {
                OralExamScreen(
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.HOMEWORK -> {
                HomeworkScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.STUDENT_GRADES -> {
                StudentGradesScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.ATTENDANCE -> {
                AttendanceScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.TIMETABLE -> {
                TimetableScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.TERM_PLAN -> {
                TermPlanScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.WHITEBOARD -> {
                InteractiveWhiteboardScreen(
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.MIND_MAPS -> {
                MindMapScreen(
                    teacher = activeTeacher,
                    geminiService = geminiService,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.EXAM_GRADER -> {
                ExamGraderScreen(
                    geminiService = geminiService,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.EXAM_SOLVER -> {
                ExamSolverScreen(
                    geminiService = geminiService,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.STUDENT_PORTAL -> {
                StudentPortalScreen(
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.STATISTICS -> {
                TeacherStatsScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.PRIMARY_MATH -> {
                PrimaryMathScreen(
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.SETTINGS -> {
                TeacherSettingsScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    geminiService = geminiService,
                    onTeacherUpdated = {
                        activeTeacher = it
                        onTeacherUpdated(it)
                    },
                    onLogout = onLogout,
                    onBack = { currentSection = null }
                )
            }
        }
        return
    }

    // Main Hub View with all 20 working sections
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.rqeeb_logo),
                                contentDescription = "شعار رقيب",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "رقيب - مساعد المعلم الذكي (اليمن)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val displayGrade = YemeniCurriculum.toDisplayGrade(activeTeacher.gradeTaught)
                            val sectionStr = if (activeTeacher.section.isNotBlank()) " | شعبة ${activeTeacher.section}" else ""
                            Text(
                                "${activeTeacher.fullName} | ${activeTeacher.subject} - $displayGrade$sectionStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val url = repository.getOwnerWhatsAppUrl()
                                ContactUtils.openOwnerWhatsApp(context, url)
                            }
                        },
                        modifier = Modifier.testTag("teacher_whatsapp_help_button")
                    ) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = "تواصل مع إدارة رقيب (واتساب)",
                            tint = Color(0xFF059669)
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("teacher_logout_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "تسجيل خروج",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Teacher Profile Badge Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "مرحباً بك، ${activeTeacher.fullName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            "${activeTeacher.schoolName} • ${activeTeacher.governorate} (${activeTeacher.directorate})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            activeTeacher.curriculumEdition,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            "معلم معتمد",
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "أقسام المنظومة التعليمية (20 قسماً متكاملاً):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val url = repository.getOwnerWhatsAppUrl()
                            ContactUtils.openOwnerWhatsApp(context, url)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تواصل مع الإدارة", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 20 Working Modules Grid (Responsive Auto-fit minmax layout)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(TeacherSection.values()) { section ->
                    DashboardModuleCard(
                        section = section,
                        onClick = { currentSection = section }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardModuleCard(
    section: TeacherSection,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .defaultMinSize(minHeight = 124.dp)
            .testTag("module_${section.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = section.color.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = section.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = section.titleAr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    softWrap = true
                )
                Text(
                    text = section.subtitleAr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 13.5.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    softWrap = true
                )
            }
        }
    }
}
