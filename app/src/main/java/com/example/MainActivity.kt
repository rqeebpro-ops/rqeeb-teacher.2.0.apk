package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.ai.GeminiService
import com.example.data.db.RqeebDatabase
import com.example.data.model.TeacherEntity
import com.example.data.repository.RqeebRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.owner.OwnerDashboard
import com.example.ui.teacher.TeacherDashboard
import com.example.ui.theme.RqeebTheme

sealed interface AppState {
    object Auth : AppState
    data class Teacher(val teacher: TeacherEntity) : AppState
    object Owner : AppState
}

class MainActivity : ComponentActivity() {

    private lateinit var database: RqeebDatabase
    private lateinit var repository: RqeebRepository
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

        setContent {
            var currentAppState by remember { mutableStateOf<AppState>(AppState.Auth) }

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
