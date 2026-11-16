package com.example.ui.owner

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TeacherEntity
import com.example.data.model.YemeniCurriculum
import com.example.data.repository.RqeebRepository
import com.example.ui.theme.*
import com.example.util.ContactUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class GeneratedTeacherCode(val name: String, val phone: String, val code: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerTeacherManagementTab(
    repository: RqeebRepository,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val teachers by repository.getCloudTeachersFlow().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") }

    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var teacherToEdit by remember { mutableStateOf<TeacherEntity?>(null) }
    var teacherToDelete by remember { mutableStateOf<TeacherEntity?>(null) }
    var teacherToSuspend by remember { mutableStateOf<TeacherEntity?>(null) }
    var suspendReason by remember { mutableStateOf("إيقاف إداري مؤقت") }
    var teacherToApprove by remember { mutableStateOf<TeacherEntity?>(null) }
    var approvalCodeInput by remember { mutableStateOf("") }
    var teacherToReject by remember { mutableStateOf<TeacherEntity?>(null) }
    var rejectReason by remember { mutableStateOf("بيانات غير مستوفية للشروط") }
    var teacherDetailsToShow by remember { mutableStateOf<TeacherEntity?>(null) }
    var newlyGeneratedCodeInfo by remember { mutableStateOf<GeneratedTeacherCode?>(null) }

    val pendingCount = teachers.count { it.status == TeacherEntity.STATUS_PENDING }
    val activeCount = teachers.count { it.status == TeacherEntity.STATUS_APPROVED && it.isActivated }
    val manualPendingCount = teachers.count { it.status == TeacherEntity.STATUS_APPROVED && !it.isActivated }
    val suspendedCount = teachers.count { it.status == TeacherEntity.STATUS_SUSPENDED }
    val rejectedCount = teachers.count { it.status == TeacherEntity.STATUS_REJECTED }
    val totalCount = teachers.size

    val filteredTeachers = remember(teachers, filterStatus, searchQuery) {
        teachers.filter { teacher ->
            val matchesStatus = when (filterStatus) {
                "PENDING" -> teacher.status == TeacherEntity.STATUS_PENDING
                "MANUAL_PENDING" -> teacher.status == TeacherEntity.STATUS_APPROVED && !teacher.isActivated
                "ACTIVE" -> teacher.status == TeacherEntity.STATUS_APPROVED && teacher.isActivated
                "SUSPENDED" -> teacher.status == TeacherEntity.STATUS_SUSPENDED
                "REJECTED" -> teacher.status == TeacherEntity.STATUS_REJECTED
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    teacher.fullName.lowercase().contains(q) ||
                    teacher.phone.contains(q) ||
                    teacher.email.lowercase().contains(q) ||
                    teacher.schoolName.lowercase().contains(q) ||
                    teacher.governorate.lowercase().contains(q) ||
                    teacher.subject.lowercase().contains(q)
            matchesStatus && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Search & Add Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث بالاسم، الهاتف، المدرسة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("owner_teacher_search_input")
            )

            Button(
                onClick = { showAddTeacherDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_teacher_manual_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة معلم", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = filterStatus == "ALL",
                    onClick = { filterStatus = "ALL" },
                    label = { Text("الكل ($totalCount)", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == "MANUAL_PENDING",
                    onClick = { filterStatus = "MANUAL_PENDING" },
                    label = { Text("لم يسجل بعد ($manualPendingCount)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == "PENDING",
                    onClick = { filterStatus = "PENDING" },
                    label = { Text("بانتظار التفعيل ($pendingCount)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusPending.copy(alpha = 0.2f),
                        selectedLabelColor = StatusPending
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == "ACTIVE",
                    onClick = { filterStatus = "ACTIVE" },
                    label = { Text("نشط ($activeCount)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusApproved.copy(alpha = 0.2f),
                        selectedLabelColor = StatusApproved
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == "SUSPENDED",
                    onClick = { filterStatus = "SUSPENDED" },
                    label = { Text("موقوف ($suspendedCount)", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == "REJECTED",
                    onClick = { filterStatus = "REJECTED" },
                    label = { Text("مرفوض ($rejectedCount)", fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredTeachers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PeopleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "لا يوجد معلمين مطابقين لخيارات البحث أو التصفية",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTeachers, key = { it.id.toString() + it.cloudId }) { teacher ->
                    TeacherItemCard(
                        teacher = teacher,
                        onViewDetails = { teacherDetailsToShow = teacher },
                        onEdit = { teacherToEdit = teacher },
                        onApprove = {
                            approvalCodeInput = "RQ-${(1000..9999).random()}-${('A'..'Z').random()}"
                            teacherToApprove = teacher
                        },
                        onSuspend = {
                            suspendReason = "إيقاف إداري مؤقت"
                            teacherToSuspend = teacher
                        },
                        onReject = {
                            rejectReason = "بيانات غير مستوفية للشروط"
                            teacherToReject = teacher
                        },
                        onReactivate = {
                            coroutineScope.launch {
                                repository.reactivateTeacher(teacher)
                                Toast.makeText(context, "تمت إعادة تفعيل المعلم بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReissueCode = {
                            coroutineScope.launch {
                                val res = repository.reissueActivationCode(teacher)
                                if (res.isSuccess) {
                                    val newCode = res.getOrThrow()
                                    Toast.makeText(context, "تم إصدار كود بديل: $newCode", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "فشل إصدار الكود: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onDelete = { teacherToDelete = teacher },
                        onCopyCode = {
                            if (teacher.activationCode.isNotBlank()) {
                                ContactUtils.copyToClipboard(context, "كود التفعيل", teacher.activationCode)
                                Toast.makeText(context, "تم نسخ كود المعلم (${teacher.fullName}): ${teacher.activationCode} بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSendWhatsApp = {
                            val msg = if (teacher.activationCode.isNotBlank()) {
                                "مرحباً يا أستاذ ${teacher.fullName}، كود التفعيل المخصص لحسابك في تطبيق «المعلم التربوي الذكي» هو:\n${teacher.activationCode}\nيرجى تثبيت التطبيق وإدخال رقم هاتفك والكود لتفعيل حسابك مباشرة."
                            } else {
                                "مرحباً أستاذ ${teacher.fullName}، نود التواصل معك بشأن حسابك في تطبيق «المعلم التربوي الذكي»."
                            }
                            ContactUtils.openTeacherWhatsApp(context, teacher.phone, msg)
                        }
                    )
                }
            }
        }
    }

    // Dialog: Add Teacher Manually
    if (showAddTeacherDialog) {
        ManualTeacherDialog(
            title = "إضافة معلم جديد يدويًا",
            initialTeacher = null,
            onDismiss = { showAddTeacherDialog = false },
            onConfirm = { name, phone, school, gov, dist, subj, stage, gr, sec, edition ->
                coroutineScope.launch {
                    val res = repository.addTeacherManually(
                        fullName = name,
                        phone = phone,
                        schoolName = school,
                        governorate = gov,
                        district = dist,
                        subject = subj,
                        stage = stage,
                        grade = gr,
                        section = sec,
                        curriculumEdition = edition
                    )
                    showAddTeacherDialog = false
                    if (res.isSuccess) {
                        val (_, code) = res.getOrThrow()
                        ContactUtils.copyToClipboard(context, "كود التفعيل", code)
                        newlyGeneratedCodeInfo = GeneratedTeacherCode(name, phone, code)
                        Toast.makeText(context, "تمت إضافة المعلم ونسخ الكود: $code", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "خطأ: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Dialog: Edit Teacher
    if (teacherToEdit != null) {
        val t = teacherToEdit!!
        ManualTeacherDialog(
            title = "تعديل بيانات المعلم",
            initialTeacher = t,
            onDismiss = { teacherToEdit = null },
            onConfirm = { name, phone, school, gov, dist, subj, stage, gr, sec, edition ->
                coroutineScope.launch {
                    val updated = t.copy(
                        fullName = name,
                        phone = phone,
                        schoolName = school,
                        governorate = gov,
                        directorate = dist,
                        subject = subj,
                        stage = stage,
                        gradeTaught = gr,
                        section = sec,
                        curriculumEdition = edition
                    )
                    val res = repository.updateTeacherManually(updated)
                    teacherToEdit = null
                    if (res.isSuccess) {
                        Toast.makeText(context, "تم حفظ التعديلات بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "خطأ في التحديث: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Dialog: Approve
    if (teacherToApprove != null) {
        val t = teacherToApprove!!
        AlertDialog(
            onDismissRequest = { teacherToApprove = null },
            icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusApproved, modifier = Modifier.size(36.dp)) },
            title = { Text("الموافقة وتوليد كود التفعيل", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المعلم: ${t.fullName}", fontWeight = FontWeight.Bold)
                    Text("المدرسة: ${t.schoolName} (${t.governorate})", fontSize = 13.sp)
                    Text("المادة والصف: ${t.subject} - ${YemeniCurriculum.toDisplayGrade(t.gradeTaught)}", fontSize = 12.sp)
                    Text("الهاتف: ${t.phone}", fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = approvalCodeInput,
                        onValueChange = { approvalCodeInput = it.uppercase() },
                        label = { Text("كود التفعيل") },
                        trailingIcon = {
                            IconButton(onClick = { approvalCodeInput = "RQ-${(1000..9999).random()}-${('A'..'Z').random()}" }) {
                                Icon(Icons.Default.Refresh, contentDescription = "توليد")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("approval_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val (success, code) = repository.approveTeacher(t, approvalCodeInput.trim())
                            teacherToApprove = null
                            if (success) {
                                Toast.makeText(context, "تمت الموافقة وتوليد الكود: $code", Toast.LENGTH_LONG).show()
                                val msg = "مرحباً يا أستاذ ${t.fullName}، تمت الموافقة على طلب انضمامك لتطبيق «المعلم التربوي الذكي». كود التفعيل الخاص بك هو:\n$code"
                                ContactUtils.openTeacherWhatsApp(context, t.phone, msg)
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_approve_button")
                ) {
                    Text("موافقة وإرسال واتساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToApprove = null }) { Text("إلغاء") }
            }
        )
    }

    // Dialog: Suspend
    if (teacherToSuspend != null) {
        val t = teacherToSuspend!!
        AlertDialog(
            onDismissRequest = { teacherToSuspend = null },
            icon = { Icon(Icons.Default.PauseCircle, contentDescription = null, tint = StatusSuspended, modifier = Modifier.size(36.dp)) },
            title = { Text("تعليق حساب المعلم", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هل أنت متأكد من تعليق حساب: ${t.fullName}؟", fontSize = 13.sp)
                    OutlinedTextField(
                        value = suspendReason,
                        onValueChange = { suspendReason = it },
                        label = { Text("سبب التعليق") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("suspend_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.suspendTeacher(t, suspendReason)
                            teacherToSuspend = null
                            Toast.makeText(context, "تم تعليق الحساب بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuspended),
                    modifier = Modifier.testTag("confirm_suspend_button")
                ) {
                    Text("تأكيد التعليق")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToSuspend = null }) { Text("إلغاء") }
            }
        )
    }

    // Dialog: Reject
    if (teacherToReject != null) {
        val t = teacherToReject!!
        AlertDialog(
            onDismissRequest = { teacherToReject = null },
            icon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = StatusRejected, modifier = Modifier.size(36.dp)) },
            title = { Text("رفض طلب المعلم", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المعلم: ${t.fullName} (${t.phone})", fontSize = 13.sp)
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("سبب الرفض") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reject_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.rejectTeacher(t, rejectReason)
                            teacherToReject = null
                            Toast.makeText(context, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("تأكيد الرفض")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToReject = null }) { Text("إلغاء") }
            }
        )
    }

    // Dialog: Delete Teacher Confirmation
    if (teacherToDelete != null) {
        val t = teacherToDelete!!
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
            title = { Text("حذف حساب المعلم نهائيًا", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف المعلم «${t.fullName}» نهائيًا من النظام وقاعدة البيانات؟ هذا الإجراء لا يمكن التراجع عنه.", fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = repository.deleteTeacherManually(t)
                            teacherToDelete = null
                            if (res.isSuccess) {
                                Toast.makeText(context, "تم حذف المعلم بنجاح", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "خطأ أثناء الحذف: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحذف النهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    // Dialog: Teacher Details
    if (teacherDetailsToShow != null) {
        val t = teacherDetailsToShow!!
        AlertDialog(
            onDismissRequest = { teacherDetailsToShow = null },
            title = { Text("تفاصيل سجل المعلم", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("الاسم: ${t.fullName}", fontWeight = FontWeight.Bold)
                    Text("الهاتف: ${t.phone}")
                    if (t.email.isNotBlank() && !t.email.endsWith("@smartteacher.ye") && !t.email.endsWith("@rqeeb.edu")) {
                        Text("البريد: ${t.email}")
                    }
                    Text("المدرسة: ${t.schoolName}")
                    Text("المحافظة والمديرية: ${t.governorate} - ${t.directorate.ifBlank { "غير محددة" }}")
                    Text("المادة والمرحلة: ${t.subject} (${t.stage})")
                    Text("الصف والشعبة: ${YemeniCurriculum.toDisplayGrade(t.gradeTaught)}${if (t.section.isNotBlank()) " - شعبة ${t.section}" else ""}")
                    Text("طبعة المنهج: ${t.curriculumEdition}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    Text("كود التفعيل: ${t.activationCode.ifBlank { "لم يولد كود بعد" }}", fontWeight = FontWeight.Bold)
                    Text("حالة الحساب: ${t.status} (مفعل: ${t.isActivated})")
                    if (t.reviewNotes.isNotBlank()) {
                        Text("ملاحظات الإدارة: ${t.reviewNotes}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { teacherDetailsToShow = null }) { Text("إغلاق") }
            }
        )
    }

    // Dialog: Newly Generated Code Success & One-Tap Copy
    if (newlyGeneratedCodeInfo != null) {
        val info = newlyGeneratedCodeInfo!!
        AlertDialog(
            onDismissRequest = { newlyGeneratedCodeInfo = null },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusApproved,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text("تم إنشاء كود التفعيل بنجاح!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("المعلم: ${info.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("الهاتف: ${info.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ContactUtils.copyToClipboard(context, "كود التفعيل", info.code)
                                Toast.makeText(context, "تم نسخ الكود (${info.code}) إلى الحافظة بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("كود تفعيل المعلم المخصص:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = info.code,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("(تم نسخه تلقائيًا إلى الحافظة)", fontSize = 10.sp, color = StatusApproved, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                ContactUtils.copyToClipboard(context, "كود التفعيل", info.code)
                                Toast.makeText(context, "تم نسخ الكود (${info.code}) بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الكود", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                val msg = "مرحباً يا أستاذ ${info.name}، تم إنشاء وتفعيل حسابك في تطبيق «المعلم التربوي الذكي».\nكود التفعيل الخاص بك هو:\n${info.code}\nيرجى فتح التطبيق واختيار 'تفعيل بكود المالك' لبدء الاستخدام."
                                ContactUtils.openTeacherWhatsApp(context, info.phone, msg)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF059669))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 12.sp, color = Color(0xFF059669))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { newlyGeneratedCodeInfo = null }) {
                    Text("تم وإغلاق", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun TeacherItemCard(
    teacher: TeacherEntity,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onApprove: () -> Unit,
    onSuspend: () -> Unit,
    onReject: () -> Unit,
    onReactivate: () -> Unit,
    onReissueCode: () -> Unit,
    onDelete: () -> Unit,
    onCopyCode: () -> Unit,
    onSendWhatsApp: () -> Unit
) {
    val displayGrade = YemeniCurriculum.toDisplayGrade(teacher.gradeTaught)
    val sectionStr = if (teacher.section.isNotBlank()) " | شعبة ${teacher.section}" else ""

    val (statusLabel, statusColor) = when {
        teacher.status == TeacherEntity.STATUS_APPROVED && !teacher.isActivated -> Pair("لم يسجل بعد", Color(0xFFD97706))
        teacher.status == TeacherEntity.STATUS_APPROVED && teacher.isActivated -> Pair("نشط ومفعل", StatusApproved)
        teacher.status == TeacherEntity.STATUS_PENDING -> Pair("بانتظار التفعيل", StatusPending)
        teacher.status == TeacherEntity.STATUS_SUSPENDED -> Pair("موقوف مؤقتاً", StatusSuspended)
        teacher.status == TeacherEntity.STATUS_REJECTED -> Pair("مرفوض", StatusRejected)
        else -> Pair(teacher.status, Color.Gray)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("teacher_card_${teacher.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(teacher.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${teacher.schoolName} • ${teacher.governorate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${teacher.subject} • $displayGrade$sectionStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text("الهاتف: ${teacher.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (teacher.activationCode.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopyCode() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "كود التفعيل المخصص:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                teacher.activationCode,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = onCopyCode,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = onSendWhatsApp,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("واتساب", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = onReissueCode,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("كود بديل", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (teacher.status == TeacherEntity.STATUS_PENDING) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("موافقة وتفعيل", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("رفض", fontSize = 12.sp)
                    }
                } else if (teacher.status == TeacherEntity.STATUS_APPROVED && teacher.isActivated) {
                    OutlinedButton(
                        onClick = onSuspend,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusSuspended),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تعليق الحساب", fontSize = 12.sp)
                    }
                } else if (teacher.status == TeacherEntity.STATUS_SUSPENDED || teacher.status == TeacherEntity.STATUS_REJECTED) {
                    Button(
                        onClick = onReactivate,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إعادة التفعيل", fontSize = 12.sp)
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = onViewDetails) {
                    Icon(Icons.Default.Info, contentDescription = "تفاصيل الحساب")
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualTeacherDialog(
    title: String,
    initialTeacher: TeacherEntity?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        phone: String,
        school: String,
        gov: String,
        dist: String,
        subj: String,
        stage: String,
        grade: String,
        section: String,
        edition: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialTeacher?.fullName ?: "") }
    var phone by remember { mutableStateOf(initialTeacher?.phone ?: "") }
    var school by remember { mutableStateOf(initialTeacher?.schoolName ?: "") }
    var governorate by remember { mutableStateOf(initialTeacher?.governorate ?: "أمانة العاصمة") }
    var district by remember { mutableStateOf(initialTeacher?.directorate ?: "") }
    var subject by remember { mutableStateOf(initialTeacher?.subject ?: "الرياضيات") }
    var stage by remember { mutableStateOf(initialTeacher?.stage ?: "مرحلة التعليم الأساسي") }
    var grade by remember { mutableStateOf(initialTeacher?.gradeTaught ?: "9/ إعدادي") }
    var section by remember { mutableStateOf(initialTeacher?.section ?: "أ") }
    var edition by remember { mutableStateOf(initialTeacher?.curriculumEdition ?: YemeniCurriculum.EDITION_SANAA) }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    val governorates = listOf(
        "أمانة العاصمة", "محافظة صنعاء", "تعز", "إب", "الحديدة", "ذمار", "عدن",
        "حضرموت (المكلا)", "حضرموت (سيئون)", "لحج", "أبين", "صعدة", "حجة", "عمران",
        "شبوة", "المهرة", "مأرب", "الجوف", "البيضاء", "الضالع", "ريمة", "سقطرى"
    )

    val subjects = listOf("الرياضيات", "القرآن الكريم وعلومه", "التربية الإسلامية", "اللغة العربية", "العلوم", "الاجتماعيات", "اللغة الإنجليزية", "الفيزياء", "الكيمياء", "الأحياء")
    val sections = listOf("أ", "ب", "ج", "د", "هـ", "و", "عامة")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = null },
                        label = { Text("اسم المعلم الرباعي *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_teacher_name_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMsg = null },
                        label = { Text("رقم الهاتف (اليمن) *") },
                        placeholder = { Text("مثال: 785502919") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_teacher_phone_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = school,
                        onValueChange = { school = it; errorMsg = null },
                        label = { Text("اسم المدرسة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_teacher_school_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("المديرية التعليمية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("المحافظة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var govExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = govExpanded,
                        onExpandedChange = { govExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = governorate,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = govExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = govExpanded,
                            onDismissRequest = { govExpanded = false }
                        ) {
                            governorates.forEach { gov ->
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
                }

                item {
                    Text("المادة الدراسية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var subjExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = subjExpanded,
                        onExpandedChange = { subjExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subjExpanded,
                            onDismissRequest = { subjExpanded = false }
                        ) {
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = {
                                        subject = s
                                        subjExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("الصف الدراسي (المناهج اليمنية 1-12):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var gradeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = gradeExpanded,
                        onExpandedChange = { gradeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = YemeniCurriculum.toDisplayGrade(grade),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = gradeExpanded,
                            onDismissRequest = { gradeExpanded = false }
                        ) {
                            YemeniCurriculum.ALL_GRADES.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(YemeniCurriculum.toDisplayGrade(g)) },
                                    onClick = {
                                        grade = g
                                        stage = if (g.contains("ثانوي") || g == "10" || g == "11" || g == "12") "مرحلة التعليم الثانوي" else "مرحلة التعليم الأساسي"
                                        gradeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("الشعبة الدراسية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var secExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = secExpanded,
                        onExpandedChange = { secExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = section,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = secExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = secExpanded,
                            onDismissRequest = { secExpanded = false }
                        ) {
                            sections.forEach { sc ->
                                DropdownMenuItem(
                                    text = { Text("شعبة $sc") },
                                    onClick = {
                                        section = sc
                                        secExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("طبعة المنهج المعتمدة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = edition == YemeniCurriculum.EDITION_SANAA,
                            onClick = { edition = YemeniCurriculum.EDITION_SANAA }
                        )
                        Text("طبعة صنعاء", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        RadioButton(
                            selected = edition == YemeniCurriculum.EDITION_ADEN,
                            onClick = { edition = YemeniCurriculum.EDITION_ADEN }
                        )
                        Text("طبعة عدن", fontSize = 12.sp)
                    }
                }

                if (errorMsg != null) {
                    item {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || school.isBlank()) {
                        errorMsg = "يرجى تعبئة الحقول الإلزامية (الاسم، الهاتف، المدرسة)"
                        return@Button
                    }
                    onConfirm(name.trim(), phone.trim(), school.trim(), governorate, district.trim(), subject, stage, grade, section, edition)
                },
                modifier = Modifier.testTag("submit_manual_teacher_button")
            ) {
                Text("حفظ وتأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
