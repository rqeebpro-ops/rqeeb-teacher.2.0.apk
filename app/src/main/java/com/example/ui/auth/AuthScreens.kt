package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.util.ContactUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: RqeebRepository,
    onTeacherLoggedIn: (TeacherEntity) -> Unit,
    onOwnerLoggedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register

    // Hidden Owner Login State (Exactly 10 taps)
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showOwnerDialog by remember { mutableStateOf(false) }

    // Owner Login Modal Fields
    var ownerIdentifier by remember { mutableStateOf("") }
    var ownerPassword by remember { mutableStateOf("") }
    var ownerPasswordVisible by remember { mutableStateOf(false) }
    var ownerErrorMessage by remember { mutableStateOf<String?>(null) }
    var isOwnerLoading by remember { mutableStateOf(false) }

    fun handleLogoTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 3500) {
            logoTapCount = 1
        } else {
            logoTapCount += 1
        }
        lastTapTime = now

        if (logoTapCount >= 10) {
            logoTapCount = 0
            showOwnerDialog = true
        }
    }

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Rqeeb Logo with exact 10-tap hidden owner trigger
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(104.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        handleLogoTap()
                    }
                    .testTag("app_logo_tap")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.rqeeb_logo),
                        contentDescription = "شعار رقيب",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "رقيب مساعد المعلم الذكي",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "أهلاً بك في مساعدك الذكي للتعليم",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "مخصص للمنهج والمدارس اليمنية | حضّر دروسك، أنشئ اختباراتك، ونظّم عملك بذكاء",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tab Selector: تسجيل دخول المعلم / إنشاء حساب جديد
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "تسجيل دخول المعلم",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_login")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "إنشاء حساب جديد",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_register")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (selectedTab == 0) {
                TeacherLoginForm(
                    repository = repository,
                    onSuccess = onTeacherLoggedIn,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                TeacherRegisterForm(
                    repository = repository,
                    onRegistered = { selectedTab = 0 },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Owner Contact & WhatsApp Button
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val whatsappUrl = repository.getOwnerWhatsAppUrl()
                        ContactUtils.openOwnerWhatsApp(context, whatsappUrl)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                modifier = Modifier.testTag("contact_owner_button")
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تواصل مع إدارة رقيب (واتساب)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "الجمهورية اليمنية - منظومة المعلم التربوية الذكية © رقيب",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }

    // Hidden Owner Login Modal (10 taps)
    if (showOwnerDialog) {
        Dialog(onDismissRequest = {
            showOwnerDialog = false
            ownerErrorMessage = null
            ownerPassword = ""
        }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("owner_login_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "تسجيل دخول المالك",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Text(
                        text = "الإدارة المركزية لمنصة رقيب التعليمية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = ownerIdentifier,
                        onValueChange = {
                            ownerIdentifier = it
                            ownerErrorMessage = null
                        },
                        label = { Text("البريد الإلكتروني أو اسم المستخدم") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_username_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerPassword,
                        onValueChange = {
                            ownerPassword = it
                            ownerErrorMessage = null
                        },
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { ownerPasswordVisible = !ownerPasswordVisible }) {
                                Icon(
                                    imageVector = if (ownerPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "تبديل الرؤية"
                                )
                            }
                        },
                        visualTransformation = if (ownerPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_password_input")
                    )

                    if (ownerErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ownerErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showOwnerDialog = false
                                ownerErrorMessage = null
                                ownerPassword = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }

                        Button(
                            onClick = {
                                if (ownerPassword.isBlank()) {
                                    ownerErrorMessage = "يرجى إدخال كلمة المرور"
                                    return@Button
                                }
                                isOwnerLoading = true
                                coroutineScope.launch {
                                    val isVerified = repository.verifyOwner(ownerPassword, ownerIdentifier)
                                    isOwnerLoading = false
                                    if (isVerified) {
                                        showOwnerDialog = false
                                        onOwnerLoggedIn()
                                    } else {
                                        ownerErrorMessage = "بيانات الدخول غير صحيحة"
                                    }
                                }
                            },
                            enabled = !isOwnerLoading,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_submit_button")
                        ) {
                            if (isOwnerLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("دخول المالك")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherLoginForm(
    repository: RqeebRepository,
    onSuccess: (TeacherEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "مرحباً بك مجدداً يا أستاذنا",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "سجل الدخول لإدارة الدروس والاختبارات وفق المنهج اليمني",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    errorMessage = null
                },
                label = { Text("البريد الإلكتروني أو رقم الهاتف (اليمن)") },
                placeholder = { Text("مثال: 785502919 أو email@domain.com") },
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_identifier_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "إظهار كلمة المرور"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_password_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showForgotPasswordDialog = true },
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Text(
                        "نسيت كلمة المرور؟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (identifier.isBlank() || password.isBlank()) {
                        errorMessage = "يرجى إدخال البريد الإلكتروني/رقم الهاتف وكلمة المرور"
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        val teacher = repository.getTeacherByLogin(identifier)
                        isLoading = false
                        if (teacher == null) {
                            errorMessage = "بيانات الدخول غير متطابقة مع أي حساب مسجل"
                            return@launch
                        }

                        if (teacher.passwordHash != password) {
                            errorMessage = "كلمة المرور غير صحيحة"
                            return@launch
                        }

                        when (teacher.status) {
                            TeacherEntity.STATUS_PENDING -> {
                                errorMessage = "حسابك قيد المراجعة حالياً من إدارة رقيب، يرجى انتظار التفعيل."
                            }
                            TeacherEntity.STATUS_SUSPENDED -> {
                                errorMessage = "تم تعليق هذا الحساب بواسطة إدارة النظام. للتواصل: +967785502919"
                            }
                            TeacherEntity.STATUS_REJECTED -> {
                                errorMessage = "تم رفض طلب التسجيل لهذا الحساب."
                            }
                            else -> {
                                onSuccess(teacher)
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("teacher_login_submit")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        val ctx = LocalContext.current
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("استعادة كلمة المرور") },
            text = {
                Text("لاستعادة كلمة المرور، يرجى التواصل مباشرة مع إدارة منصة رقيب عبر الواتساب على الرقم: 785502919 (+967). سيتم التحقق وتزويدك برابط آمن.")
            },
            confirmButton = {
                Button(onClick = {
                    showForgotPasswordDialog = false
                    ContactUtils.openOwnerWhatsApp(ctx)
                }) {
                    Text("مراسلة الدعم واتساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherRegisterForm(
    repository: RqeebRepository,
    onRegistered: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var governorate by remember { mutableStateOf("أمانة العاصمة") }
    var directorate by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("الرياضيات") }
    var stage by remember { mutableStateOf("مرحلة التعليم الأساسي") }
    var gradeTaught by remember { mutableStateOf("الصف التاسع الأساسي") }
    var curriculumEdition by remember { mutableStateOf("طبعة صنعاء (وزارة التربية والتعليم - صنعاء)") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Dropdown expansion states
    var govExpanded by remember { mutableStateOf(false) }
    var editionExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "انضمام معلم جديد إلى رقيب (اليمن)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "أنشئ حسابك لإعداد الدروس والاختبارات وفق المنهج اليمني المعتمد",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; errorMessage = null },
                label = { Text("الاسم الكامل للمعلم/ـة *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMessage = null },
                    label = { Text("رقم الهاتف (اليمن) *") },
                    placeholder = { Text("785502919") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("reg_phone_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("البريد الإلكتروني *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("reg_email_input")
                )
            }

            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it; errorMessage = null },
                label = { Text("اسم المدرسة *") },
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("reg_school_input")
            )

            // Governorate & Directorate
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = govExpanded,
                    onExpandedChange = { govExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = governorate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المحافظة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = govExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = govExpanded,
                        onDismissRequest = { govExpanded = false }
                    ) {
                        YemeniCurriculum.GOVERNORATES.forEach { gov ->
                            DropdownMenuItem(
                                text = { Text(gov) },
                                onClick = {
                                    governorate = gov
                                    govExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = directorate,
                    onValueChange = { directorate = it },
                    label = { Text("المديرية التعليمية") },
                    placeholder = { Text("مثال: التحرير، صيرة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Curriculum Edition
            ExposedDropdownMenuBox(
                expanded = editionExpanded,
                onExpandedChange = { editionExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = curriculumEdition,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("طبعة المنهج المعتمد بالمدرسة *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editionExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = editionExpanded,
                    onDismissRequest = { editionExpanded = false }
                ) {
                    YemeniCurriculum.EDITIONS.forEach { ed ->
                        DropdownMenuItem(
                            text = { Text(ed) },
                            onClick = {
                                curriculumEdition = ed
                                editionExpanded = false
                            }
                        )
                    }
                }
            }

            // Subject and Grade
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; errorMessage = null },
                    label = { Text("مادة التدريس *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("reg_subject_input")
                )

                ExposedDropdownMenuBox(
                    expanded = gradeExpanded,
                    onExpandedChange = { gradeExpanded = it },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = gradeTaught,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الصف الدراسي *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = gradeExpanded,
                        onDismissRequest = { gradeExpanded = false }
                    ) {
                        YemeniCurriculum.ALL_GRADES.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    gradeTaught = g
                                    stage = if (g.contains("الثانوي")) "مرحلة التعليم الثانوي" else "مرحلة التعليم الأساسي"
                                    gradeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Passwords
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("كلمة المرور *") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("reg_password_input")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("تأكيد كلمة المرور *") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("reg_confirm_password_input")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { termsAccepted = !termsAccepted }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    modifier = Modifier.testTag("terms_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "أوافق على الشروط والأحكام والسياسة التعليمية لرقيب",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (successMessage != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = successMessage!!,
                        color = Color(0xFF166534),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = {
                    if (fullName.isBlank() || phone.isBlank() || email.isBlank() ||
                        schoolName.isBlank() || subject.isBlank() || password.isBlank()
                    ) {
                        errorMessage = "يرجى تعبئة كافة الحقول الإلزامية (*)"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "كلمتا المرور غير متطابقتين"
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "يجب ألا تقل كلمة المرور عن 6 أحرف"
                        return@Button
                    }
                    if (!termsAccepted) {
                        errorMessage = "يرجى الموافقة على الشروط للمتابعة"
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        val newTeacher = TeacherEntity(
                            fullName = fullName.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            passwordHash = password,
                            schoolName = schoolName.trim(),
                            governorate = governorate.trim(),
                            directorate = directorate.trim(),
                            subject = subject.trim(),
                            stage = stage,
                            gradeTaught = gradeTaught,
                            curriculumEdition = curriculumEdition
                        )
                        val (success, message) = repository.registerTeacher(newTeacher)
                        isLoading = false
                        if (success) {
                            successMessage = message
                            delay(1800)
                            onRegistered()
                        } else {
                            errorMessage = message
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("reg_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("إنشاء حساب المعلم", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
