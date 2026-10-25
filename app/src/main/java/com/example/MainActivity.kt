package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.ai.GeminiService
import com.example.data.cloud.CloudSyncService
import com.example.data.db.RqeebDatabase
import com.example.data.model.TeacherEntity
import com.example.data.repository.RqeebRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.owner.OwnerDashboard
import com.example.ui.teacher.TeacherDashboard
import com.example.ui.theme.RqeebTheme
import com.example.util.RqeebNotificationHelper

sealed interface AppState {
    object Auth : AppState
    data class Teacher(val teacher: TeacherEntity) : AppState
    object Owner : AppState
}

class MainActivity : ComponentActivity() {

    private lateinit var database: RqeebDatabase
    private lateinit var repository: RqeebRepository
    private lateinit var cloudSyncService: CloudSyncService
    private val geminiService = GeminiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = RqeebDatabase.getDatabase(this)
        repository = RqeebRepository(
            teacherDao = database.teacherDao(),
            lessonDao = database.lessonDao(),
            examDao = database.examDao(),
            bookDao = database.bookDao(),
            studentDao = database.studentDao(),
            timetableDao = database.timetableDao(),
            homeworkDao = database.homeworkDao(),
            termPlanDao = database.termPlanDao(),
            appSettingDao = database.appSettingDao()
        )
        geminiService.setRepository(repository)

        // Initialize central Cloud Firestore sync service for Realme owner & teacher activation
        cloudSyncService = CloudSyncService(this)
        repository.setCloudSyncService(cloudSyncService)

        val navigateToOwnerFromNotification = intent?.getBooleanExtra(
            RqeebNotificationHelper.EXTRA_NAVIGATE_TO_OWNER,
            false
        ) ?: false

        setContent {
            var currentAppState by remember {
                mutableStateOf<AppState>(
                    if (navigateToOwnerFromNotification) AppState.Owner else AppState.Auth
                )
            }

            // Request Notification Permission on Android 13+ (Tiramisu)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* permission result handled gracefully */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            RqeebTheme(isRtl = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val state = currentAppState) {
                        is AppState.Auth -> {
                            AuthScreen(
                                repository = repository,
                                onTeacherLoggedIn = { teacher ->
                                    currentAppState = AppState.Teacher(teacher)
                                },
                                onOwnerLoggedIn = {
                                    currentAppState = AppState.Owner
                                }
                            )
                        }

                        is AppState.Teacher -> {
                            BackHandler {
                                currentAppState = AppState.Auth
                            }
                            TeacherDashboard(
                                teacher = state.teacher,
                                repository = repository,
                                geminiService = geminiService,
                                onTeacherUpdated = { updated ->
                                    currentAppState = AppState.Teacher(updated)
                                },
                                onLogout = {
                                    currentAppState = AppState.Auth
                                }
                            )
                        }

                        is AppState.Owner -> {
                            BackHandler {
                                currentAppState = AppState.Auth
                            }
                            OwnerDashboard(
                                repository = repository,
                                geminiService = geminiService,
                                onLogout = {
                                    currentAppState = AppState.Auth
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
