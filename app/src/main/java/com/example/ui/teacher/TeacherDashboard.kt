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

enum class TeacherSection {
    LESSON_PREP,
    EXAMS,
    PRIMARY_MATH,
    LIBRARY,
    TIMETABLE,
    HOMEWORK,
    STUDENT_GRADES,
    TERM_PLAN,
    AI_TOOLS,
    LESSON_ARCHIVE,
    ORAL_EXAMS,
    SETTINGS,
    STATS
}

data class DashboardItem(
    val section: TeacherSection,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

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
    var currentSection by remember { mutableStateOf<TeacherSection?>(null) }
    var activeTeacher by remember { mutableStateOf(teacher) }

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
            TeacherSection.EXAMS -> {
                ExamGeneratorScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    geminiService = geminiService,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.PRIMARY_MATH -> {
                PrimaryMathScreen(
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.LIBRARY -> {
                EducationalLibraryScreen(
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
            TeacherSection.TERM_PLAN -> {
                TermPlanScreen(
                    teacher = activeTeacher,
                    repository = repository,
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
            TeacherSection.LESSON_ARCHIVE -> {
                LessonArchiveScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
            TeacherSection.ORAL_EXAMS -> {
                OralExamScreen(
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
            TeacherSection.STATS -> {
                TeacherStatsScreen(
                    teacher = activeTeacher,
                    repository = repository,
                    onBack = { currentSection = null }
                )
            }
        }
        return
    }

    val dashboardItems = listOf(
        DashboardItem(TeacherSection.LESSON_PREP, "تحضير الدروس", "الخطة القياسية والذكاء الاصطناعي", Icons.Default.EditNote, Color(0xFF006874)),
        DashboardItem(TeacherSection.EXAMS, "صانع الاختبارات", "بنك الأسئلة والطباعة والرسومات", Icons.Default.Quiz, Color(0xFF0284C7)),
        DashboardItem(TeacherSection.PRIMARY_MATH, "رياضيات الابتدائية", "شرح تفاعلي للحمل والاستلاف (1-6)", Icons.Default.Calculate, Color(0xFFD97706)),
        DashboardItem(TeacherSection.LIBRARY, "المكتبة التعليمية", "المناهج والكتب والوحدات اليمنية", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF0D9488)),
        DashboardItem(TeacherSection.STUDENT_GRADES, "كشف الدرجات والطلاب", "سجل أعمال السنة والملاحظات", Icons.Default.Grade, Color(0xFF8B5CF6)),
        DashboardItem(TeacherSection.TIMETABLE, "الجدول الأسبوعي", "توزيع الحصص والصفوف", Icons.Default.CalendarMonth, Color(0xFF2563EB)),
        DashboardItem(TeacherSection.HOMEWORK, "الواجبات المنزلية", "التكليفات وتوثيق التمارين", Icons.Default.Assignment, Color(0xFF059669)),
        DashboardItem(TeacherSection.TERM_PLAN, "الخطة الفصلية", "التوزيع الزمني للمقرر الدراسي", Icons.Default.ViewTimeline, Color(0xFF4F46E5)),
        DashboardItem(TeacherSection.AI_TOOLS, "المساعد الذكي (AI)", "صياغة الأهداف والأنشطة والوسائل", Icons.Default.AutoAwesome, Color(0xFFEC4899)),
        DashboardItem(TeacherSection.LESSON_ARCHIVE, "أرشيف الدروس", "سجل التحضيرات المحفوظة", Icons.Default.Folder, Color(0xFF64748B)),
        DashboardItem(TeacherSection.ORAL_EXAMS, "الاختبارات الشفوية", "تقييم التلاوة والمطالعة الشفوية", Icons.Default.RecordVoiceOver, Color(0xFFE11D48)),
        DashboardItem(TeacherSection.STATS, "الإحصائيات والتقارير", "بيانات الإنجاز والمتابعة", Icons.Default.BarChart, Color(0xFF10B981)),
        DashboardItem(TeacherSection.SETTINGS, "إعدادات المعلم", "الملف الشخصي، الصف والشعبة", Icons.Default.Settings, Color(0xFF475569))
    )

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
                        onClick = { ContactUtils.openOwnerWhatsApp(context) },
                        modifier = Modifier.testTag("whatsapp_support_button")
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
                        val displayGrade = YemeniCurriculum.toDisplayGrade(activeTeacher.gradeTaught)
                        val sectionStr = if (activeTeacher.section.isNotBlank()) " | شعبة ${activeTeacher.section}" else ""
                        Text(
                            "${activeTeacher.schoolName} • $displayGrade$sectionStr",
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

            // Grid of Hub Modules
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(dashboardItems) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentSection = item.section }
                            .testTag("dashboard_item_${item.section.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = item.color.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        item.icon,
                                        contentDescription = null,
                                        tint = item.color,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = item.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
