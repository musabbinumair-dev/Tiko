package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppDialogController
import com.example.ui.components.AppOverlayHost
import com.example.ui.components.AppToastController
import com.example.ui.components.EmptyState
import com.example.ui.components.ToastType
import androidx.compose.ui.platform.LocalContext
import com.example.ui.screens.AddEditTaskScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ListsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotFoundScreen
import com.example.ui.screens.SaveErrorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.TaskDetailScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TikoBackground
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            MyApplicationTheme {
                TikoApp()
            }
        }
    }
}

@Composable
fun TikoApp(viewModel: TikoViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val stats by viewModel.todayStats.collectAsState()
    val notifEnabled by viewModel.preferences.notificationsEnabled.collectAsState()

    var hasShownNotificationPopup by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(allTasks) {
        val activity = context as? ComponentActivity
        val intent = activity?.intent
        val notifTaskId = intent?.getLongExtra("task_id", -1L)?.takeIf { it != -1L }
            ?: intent?.getLongExtra("TASK_ID", -1L)?.takeIf { it != -1L }
            ?: intent?.getLongExtra("taskId", -1L)?.takeIf { it != -1L }
        val route = intent?.getStringExtra("route")
        if (notifTaskId != null) {
            intent?.removeExtra("task_id")
            intent?.removeExtra("TASK_ID")
            intent?.removeExtra("taskId")
            viewModel.handleTaskNotificationIntent(notifTaskId)
        } else if (!route.isNullOrBlank()) {
            intent?.removeExtra("route")
            viewModel.navigateToRoute(route)
        }
    }

    LaunchedEffect(currentScreen, notifEnabled) {
        val isWelcomeOrAuth = currentScreen is Screen.Welcome ||
            currentScreen is Screen.Login ||
            currentScreen is Screen.SignUp
        val isProfilePage = currentScreen is Screen.Main && (currentScreen as Screen.Main).tab == NavigationTab.SETTINGS

        if (!hasShownNotificationPopup && !notifEnabled && !isWelcomeOrAuth && !isProfilePage) {
            hasShownNotificationPopup = true
            AppDialogController.showCustom(
                onDismiss = { AppDialogController.dismiss() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    EmptyState(
                        image = R.drawable.illus_notifications_off,
                        title = "Reminders are off",
                        caption = "Turn them on so you never miss a task.",
                        buttonText = "Turn on",
                        onButton = {
                            viewModel.preferences.setNotificationsEnabled(true)
                            AppToastController.show("Notifications enabled", ToastType.Success)
                            AppDialogController.dismiss()
                        },
                        secondaryButtonText = "Not now",
                        onSecondaryButton = {
                            AppDialogController.dismiss()
                        }
                    )
                }
            }
        }
    }

    // Handle system back navigation cleanly
    BackHandler(enabled = true) {
        val popped = viewModel.goBack()
        if (!popped && selectedTab != NavigationTab.HOME) {
            viewModel.selectTab(NavigationTab.HOME)
        }
    }

    val isMainScreen = currentScreen is Screen.Main
    val currentTab = (currentScreen as? Screen.Main)?.tab
    val hasCustomFloatingNav = currentTab == NavigationTab.HOME ||
        currentTab == NavigationTab.LISTS ||
        currentTab == NavigationTab.CALENDAR ||
        currentTab == NavigationTab.SETTINGS
    val creamBackground = Color(0xFFEFEBDD)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = if (hasCustomFloatingNav) creamBackground else TikoBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (hasCustomFloatingNav) creamBackground else TikoBackground)
        ) {
            when (val screen = currentScreen) {
                    is Screen.Welcome -> {
                        WelcomeScreen(
                            onNavigateHome = {
                                viewModel.navigateTo(Screen.Main(NavigationTab.HOME))
                            },
                            onNavigateLogin = {
                                viewModel.navigateTo(Screen.Login)
                            },
                            onNavigateSignUp = {
                                viewModel.navigateTo(Screen.SignUp)
                            }
                        )
                    }
                    is Screen.Login -> {
                        LoginScreen(
                            onBack = {
                                viewModel.goBack()
                            },
                            onLoginSuccess = { email ->
                                val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                viewModel.saveUserProfile(name, email)
                                viewModel.navigateTo(Screen.Main(NavigationTab.HOME))
                            },
                            onSignUp = {
                                viewModel.navigateTo(Screen.SignUp)
                            }
                        )
                    }
                    is Screen.SignUp -> {
                        SignUpScreen(
                            onBack = {
                                viewModel.goBack()
                            },
                            onCreateAccountSuccess = { name, email ->
                                viewModel.saveUserProfile(name, email)
                                viewModel.navigateTo(Screen.Main(NavigationTab.HOME))
                            },
                            onNavigateLogin = {
                                viewModel.navigateTo(Screen.Login)
                            }
                        )
                    }
                    is Screen.Main -> {
                        when (screen.tab) {
                            NavigationTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                tasks = allTasks,
                                stats = stats,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            NavigationTab.LISTS -> ListsScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            NavigationTab.CALENDAR -> CalendarScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            NavigationTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                    }
                    is Screen.TaskDetail -> {
                        val task = allTasks.find { it.id == screen.taskId }
                        if (task == null) {
                            NotFoundScreen(
                                onBackToHome = {
                                    viewModel.selectTab(NavigationTab.HOME)
                                },
                                onGoBack = {
                                    if (!viewModel.goBack()) {
                                        viewModel.selectTab(NavigationTab.HOME)
                                    }
                                },
                                onClose = {
                                    viewModel.selectTab(NavigationTab.HOME)
                                }
                            )
                        } else {
                            TaskDetailScreen(
                                taskId = screen.taskId,
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) },
                                onBack = { viewModel.goBack() }
                            )
                        }
                    }
                    is Screen.AddTask -> {
                        AddEditTaskScreen(
                            taskId = null,
                            viewModel = viewModel,
                            onBack = { viewModel.goBack() }
                        )
                    }
                    is Screen.EditTask -> {
                        val task = allTasks.find { it.id == screen.taskId }
                        if (task == null) {
                            NotFoundScreen(
                                onBackToHome = {
                                    viewModel.selectTab(NavigationTab.HOME)
                                },
                                onGoBack = {
                                    if (!viewModel.goBack()) {
                                        viewModel.selectTab(NavigationTab.HOME)
                                    }
                                },
                                onClose = {
                                    viewModel.selectTab(NavigationTab.HOME)
                                }
                            )
                        } else {
                            AddEditTaskScreen(
                                taskId = screen.taskId,
                                viewModel = viewModel,
                                onBack = { viewModel.goBack() }
                            )
                        }
                    }
                    is Screen.ErrorSaveFailed -> {
                        SaveErrorScreen(
                            onTryAgain = {
                                viewModel.retrySaveRequested = true
                                viewModel.goBack()
                            },
                            onClose = {
                                viewModel.selectTab(NavigationTab.HOME)
                            },
                            onGoHome = {
                                viewModel.selectTab(NavigationTab.HOME)
                            },
                            onBack = {
                                viewModel.goBack()
                            }
                        )
                    }
                    is Screen.NotFound -> {
                        NotFoundScreen(
                            onBackToHome = {
                                viewModel.selectTab(NavigationTab.HOME)
                            },
                            onGoBack = {
                                if (!viewModel.goBack()) {
                                    viewModel.selectTab(NavigationTab.HOME)
                                }
                            },
                            onClose = {
                                viewModel.selectTab(NavigationTab.HOME)
                            }
                        )
                    }
                    else -> {
                        NotFoundScreen(
                            onBackToHome = {
                                viewModel.selectTab(NavigationTab.HOME)
                            },
                            onGoBack = {
                                if (!viewModel.goBack()) {
                                    viewModel.selectTab(NavigationTab.HOME)
                                }
                            },
                            onClose = {
                                viewModel.selectTab(NavigationTab.HOME)
                            }
                        )
                    }
                }

            if (hasCustomFloatingNav && currentTab != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                ) {
                    AppBottomNav(
                        activeTab = currentTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        onAddClick = { viewModel.navigateTo(Screen.AddTask) }
                    )
                }
            }

            AppOverlayHost()
        }
    }
}
