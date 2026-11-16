package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.R
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.util.ContactUtils
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: RqeebRepository,
    onTeacherLoggedIn: (TeacherEntity) -> Unit,
    onOwnerLoggedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register
    var showOwnerDialog by remember { mutableStateOf(false) }
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    var ownerIdentifier by remember { mutableStateOf("") }
    var ownerPassword by remember { mutableStateOf("") }
    var ownerPasswordVisible by remember { mutableStateOf(false) }
    var ownerErrorMessage by remember { mutableStateOf<String?>(null) }
    var isOwnerLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "شعار المعلم التربوي الذكي",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "المعلم التربوي الذكي",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "المنصة الشاملة للمعلم اليمني وفق طبعتي صنعاء وعدن",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("تسجيل الدخول", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_login")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("حساب معلم جديد", fontWeight = FontWeight.Bold) },
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
                    onTeacherLoggedIn = onTeacherLoggedIn,
                    onRegistered = { selectedTab = 0 },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showOwnerDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isOwnerLoading) showOwnerDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "بوابة المالك المركزي (Realme Control)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "هذه البوابة مخصصة حصرياً لمالك النظام لإدارة وتفعيل حسابات المعلمين من هاتفه Realme:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = ownerIdentifier,
                        onValueChange = {
                            ownerIdentifier = it
                            ownerErrorMessage = null
                        },
                        label = { Text("معرف المالك أو البريد") },
                        placeholder = { Text("abdulrqeebfarag2018@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("owner_identifier_input")
                    )

                    OutlinedTextField(
                        value = ownerPassword,
                        onValueChange = {
                            ownerPassword = it
                            ownerErrorMessage = null
                        },
                        label = { Text("كلمة مرور المالك") },
                        visualTransformation = if (ownerPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { ownerPasswordVisible = !ownerPasswordVisible }) {
                                Icon(
                                    if (ownerPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("owner_password_input")
                    )

                    if (ownerErrorMessage != null) {
                        Text(
                            text = ownerErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isOwnerLoading = true
                        coroutineScope.launch {
                            val ok = repository.verifyOwner(ownerPassword, ownerIdentifier)
                            isOwnerLoading = false
                            if (ok) {
                                showOwnerDialog = false
                                onOwnerLoggedIn()
                            } else {
                                ownerErrorMessage = "بيانات المالك غير صحيحة"
                            }
                        }
                    },
                    enabled = !isOwnerLoading,
                    modifier = Modifier.testTag("submit_owner_login")
                ) {
                    if (isOwnerLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("دخول المالك")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOwnerDialog = false },
                    enabled = !isOwnerLoading
                ) {
                    Text("إلغاء")
                }
            }
        )
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
    var showActivationDialog by remember { mutableStateOf(false) }
    var activationIdentifierInput by remember { mutableStateOf("") }
    var activationCodeInput by remember { mutableStateOf("") }
    var activationMessage by remember { mutableStateOf<String?>(null) }
    var isActivating by remember { mutableStateOf(false) }

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
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_password_input")
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (identifier.isBlank() || password.isBlank()) {
                        errorMessage = "يرجى إدخال البريد/الهاتف وكلمة المرور"
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        val teacher = repository.getTeacherByLogin(identifier.trim())
                        isLoading = false
                        if (teacher == null) {
                            errorMessage = "الحساب غير موجود. يرجى التأكد من البيانات أو تسجيل حساب جديد"
                        } else if (teacher.passwordHash != password.trim()) {
                            errorMessage = "كلمة المرور غير صحيحة"
                        } else if (teacher.status == TeacherEntity.STATUS_PENDING || !teacher.isActivated) {
                            errorMessage = "حسابك قيد المراجعة بانتظار موافقة المالك، أو يمكنك التفعيل بكود الأمان الممنوح لك"
                        } else if (teacher.status == TeacherEntity.STATUS_SUSPENDED) {
                            errorMessage = "تم تعليق الحساب مؤقتاً: ${teacher.reviewNotes}. يرجى التواصل مع الإدارة"
                        } else if (teacher.status == TeacherEntity.STATUS_REJECTED) {
                            errorMessage = "تم رفض الطلب: ${teacher.reviewNotes}"
                        } else {
                            onSuccess(teacher)
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("teacher_login_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("تسجيل الدخول", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showForgotPasswordDialog = true },
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Text("نسيت كلمة المرور؟", fontSize = 13.sp)
                }

                TextButton(
                    onClick = {
                        activationIdentifierInput = identifier
                        showActivationDialog = true
                    },
                    modifier = Modifier.testTag("open_activation_code_dialog")
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تفعيل بكود المالك", fontSize = 13.sp)
                }
            }
        }
    }

    if (showActivationDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isActivating) showActivationDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "تفعيل الحساب بكود الأمان",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "أدخل البريد أو رقم الهاتف المسجل به، وكود التفعيل الممنوح لك من مالك النظام:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = activationIdentifierInput,
                        onValueChange = {
                            activationIdentifierInput = it
                            activationMessage = null
                        },
                        label = { Text("البريد الإلكتروني أو رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("activation_identifier_input")
                    )

                    OutlinedTextField(
                        value = activationCodeInput,
                        onValueChange = {
                            activationCodeInput = it.uppercase()
                            activationMessage = null
                        },
                        label = { Text("كود التفعيل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("activation_code_input")
                    )

                    if (activationMessage != null) {
                        Text(
                            text = activationMessage!!,
                            color = if (activationMessage!!.contains("نجاح")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (activationIdentifierInput.isBlank() || activationCodeInput.isBlank()) {
                            activationMessage = "يرجى ملء الحقول المطلوبة"
                            return@Button
                        }
                        isActivating = true
                        coroutineScope.launch {
                            val (success, msg) = repository.activateWithCode(
                                activationIdentifierInput.trim(),
                                activationCodeInput.trim()
                            )
                            isActivating = false
                            activationMessage = msg
                            if (success) {
                                showActivationDialog = false
                                val active = repository.getTeacherByLogin(activationIdentifierInput.trim())
                                if (active != null) {
                                    onSuccess(active)
                                }
                            }
                        }
                    },
                    enabled = !isActivating,
                    modifier = Modifier.testTag("submit_activation_code_button")
                ) {
                    if (isActivating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("تفعيل الآن")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showActivationDialog = false },
                    enabled = !isActivating
                ) {
                    Text("إلغاء")
                }
            }
        )
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
    onTeacherLoggedIn: (TeacherEntity) -> Unit,
    onRegistered: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var governorate by remember { mutableStateOf("أمانة العاصمة") }
    var directorate by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("الرياضيات") }
    var stage by remember { mutableStateOf("المرحلة الإعدادية") }
    var gradeTaught by remember { mutableStateOf("9/ إعدادي") }
    var section by remember { mutableStateOf("") }
    var curriculumEdition by remember { mutableStateOf("طبعة صنعاء (وزارة التربية والتعليم - صنعاء)") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var activationCodeInput by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isActivatingCode by remember { mutableStateOf(false) }

    var govExpanded by remember { mutableStateOf(false) }
    var editionExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }

    fun validateForm(): Boolean {
        if (fullName.isBlank() || phone.isBlank() || email.isBlank() ||
            schoolName.isBlank() || subject.isBlank() || password.isBlank()
        ) {
            errorMessage = "يرجى تعبئة كافة الحقول الإلزامية (*)"
            return false
        }
        if (password != confirmPassword) {
            errorMessage = "كلمتا المرور غير متطابقتين"
            return false
        }
        if (password.length < 6) {
            errorMessage = "يجب ألا تقل كلمة المرور عن 6 أحرف"
            return false
        }
        if (!termsAccepted) {
            errorMessage = "يرجى الموافقة على الشروط للمتابعة"
            return false
        }
        errorMessage = null
        return true
    }

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
                text = "انضمام معلم جديد إلى المعلم التربوي الذكي",
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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth().menuAnchor()
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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = gradeExpanded,
                        onDismissRequest = { gradeExpanded = false }
                    ) {
                        YemeniCurriculum.DISPLAY_GRADES.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    gradeTaught = g
                                    stage = YemeniCurriculum.getStageForDisplayGrade(g)
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
                    label = { Text("كلمة المرور (للدخول لاحقاً) *") },
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    modifier = Modifier.testTag("terms_checkbox")
                )
                Text(
                    text = "أقر بأنني معلم/ـة في الجمهورية اليمنية وأوافق على شروط الخدمة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // WhatsApp Subscription & Activation Section
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "خطوات الاشتراك وتفعيل الحساب:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Button 1: Send Subscription Request via WhatsApp
                    Button(
                        onClick = {
                            if (!validateForm()) return@Button
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
                                    section = section,
                                    curriculumEdition = curriculumEdition,
                                    deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
                                )
                                // Save profile in local DB
                                repository.registerTeacher(newTeacher)

                                // Open WhatsApp with subscription text
                                val msg = "السلام عليكم ورحمة الله، أنا المعلم: ${fullName.trim()}، رقم الهاتف: ${phone.trim()}، المدرسة: ${schoolName.trim()} ($governorate)، المادة: ${subject.trim()} - $gradeTaught. أرغب في الاشتراك في تطبيق «المعلم التربوي الذكي» وتفعيل حسابي. يرجى إرسال كود التفعيل."
                                ContactUtils.openOwnerWhatsApp(context, msg)

                                successMessage = "تم فتح الواتساب لإرسال طلب الاشتراك. أدخل كود التفعيل المستلم من الإدارة في الخانة أدناه واضغط (تفعيل الحساب والدخول للتطبيق)."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("whatsapp_subscription_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال طلب الاشتراك عبر الواتساب", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Activation Code Input & Activation Button
                    OutlinedTextField(
                        value = activationCodeInput,
                        onValueChange = {
                            activationCodeInput = it.uppercase()
                            errorMessage = null
                        },
                        label = { Text("أدخل كود التفعيل المستلم") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("reg_activation_code_input")
                    )

                    Button(
                        onClick = {
                            if (!validateForm()) return@Button
                            if (activationCodeInput.isBlank()) {
                                errorMessage = "يرجى إدخال كود التفعيل أولاً"
                                return@Button
                            }
                            isActivatingCode = true
                            coroutineScope.launch {
                                // 1. Save or update teacher entity
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
                                    section = section,
                                    curriculumEdition = curriculumEdition,
                                    deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
                                )
                                repository.registerTeacher(newTeacher)

                                // 2. Activate with code
                                val (ok, msg) = repository.activateWithCode(
                                    phone.trim().ifBlank { email.trim() },
                                    activationCodeInput.trim()
                                )
                                isActivatingCode = false
                                if (ok) {
                                    val activeTeacher = repository.getTeacherByLogin(phone.trim().ifBlank { email.trim() })
                                    if (activeTeacher != null) {
                                        onTeacherLoggedIn(activeTeacher)
                                    } else {
                                        successMessage = "تم التفعيل بنجاح! يرجى تسجيل الدخول بكود/هاتفك وكلمة المرور."
                                        onRegistered()
                                    }
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        enabled = !isActivatingCode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("activate_code_login_button")
                    ) {
                        if (isActivatingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تفعيل الحساب والدخول للتطبيق", fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
        }
    }
}
