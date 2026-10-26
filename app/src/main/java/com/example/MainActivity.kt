package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.example.data.ai.GeminiService
import com.example.data.model.TeacherEntity
import com.example.data.repository.RqeebRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.owner.OwnerDashboard
import com.example.ui.teacher.TeacherDashboard
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class AppDestination {
    AUTH,
    TEACHER,
    OWNER
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        RqeebAppContent()
                    }
                }
            }
        }
    }
}

@Composable
fun RqeebAppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val repository = remember { RqeebRepository.getInstance(context) }
    val geminiService = remember { GeminiService.getInstance(context) }

    var currentDestination by remember { mutableStateOf(AppDestination.AUTH) }
    var currentTeacher by remember { mutableStateOf<TeacherEntity?>(null) }

    // Request Notification permission for Android 13+ (TIRAMISU)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled gracefully
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    when (currentDestination) {
        AppDestination.AUTH -> {
            AuthScreen(
                repository = repository,
                onTeacherLoggedIn = { teacher ->
                    currentTeacher = teacher
                    currentDestination = AppDestination.TEACHER
                },
                onOwnerLoggedIn = {
                    currentDestination = AppDestination.OWNER
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        AppDestination.TEACHER -> {
            if (currentTeacher != null) {
                TeacherDashboard(
                    teacher = currentTeacher!!,
                    repository = repository,
                    geminiService = geminiService,
                    onTeacherUpdated = { updatedTeacher ->
                        currentTeacher = updatedTeacher
                        coroutineScope.launch {
                            repository.updateTeacher(updatedTeacher)
                        }
                    },
                    onLogout = {
                        currentTeacher = null
                        currentDestination = AppDestination.AUTH
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                currentDestination = AppDestination.AUTH
            }
        }

        AppDestination.OWNER -> {
            OwnerDashboard(
                repository = repository,
                onLogout = {
                    currentDestination = AppDestination.AUTH
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
