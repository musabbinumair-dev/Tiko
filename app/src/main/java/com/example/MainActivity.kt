package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.data.repository.AuthRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val authRepository = remember { AuthRepository() }
    val currentUser by authRepository.currentUser.collectAsState(initial = null)
    
    val currentScreen by viewModel.currentScreen.collectAsState()
    val screenStack by viewModel.screenStack.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val stats by viewModel.todayStats.collectAsState()
    val notifEnabled by viewModel.preferences.notificationsEnabled.collectAsState()
    val haptic = LocalHapticFeedback.current

    var hasShownNotificationPopup by rememberSaveable { mutableStateOf(false) }
    var hasCheckedAuth by rememberSaveable { mutableStateOf(false) }

    // Check auth state and navigate appropriately
    LaunchedEffect(currentUser, hasCheckedAuth) {
        if (!hasCheckedAuth) {
            hasCheckedAuth = true
            if (currentUser != null) {
                // User is signed in, go to Home
                val name = currentUser.displayName ?: "User"
                val email = currentUser.email ?: ""
                viewModel.saveUserProfile(name, email)
                viewModel.navigateTo(Screen.Main(NavigationTab.HOME))
            }
            // If currentUser is null, stay on Welcome screen (default)
        }
    }

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

    val currentTab = (currentScreen as? Screen.Main)?.tab
    val hasCustomFloatingNav = currentTab == NavigationTab.HOME ||
        currentTab == NavigationTab.LISTS ||
        currentTab == NavigationTab.CALENDAR ||
        currentTab == NavigationTab.SETTINGS
    val creamBackground = Color(0xFFEFEBDD)

    // Determine transition direction: push (forward) vs pop (back) vs tab switch
    val stackSize = screenStack.size

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = if (hasCustomFloatingNav) creamBackground else TikoBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (hasCustomFloatingNav) creamBackground else TikoBackground)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val isTabSwitch = targetState is Screen.Main && initialState is Screen.Main
                    val isSlideUp = targetState is Screen.AddTask ||
                        targetState is Screen.ErrorSaveFailed ||
                        targetState is Screen.NotFound
                    val isSlideDown = initialState is Screen.AddTask ||
                        initialState is Screen.ErrorSaveFailed ||
                        initialState is Screen.NotFound
                    val isPop = stackSize < 2 ||
                        (initialState !is Screen.Welcome && targetState is Screen.Main &&
                            initialState !is Screen.Main)

                    when {
                        isTabSwitch -> ContentTransform(
                            targetContentEnter = fadeIn(tween(200)),
                            initialContentExit = fadeOut(tween(200))
                        )
                        isSlideUp -> ContentTransform(
                            targetContentEnter = slideInVertically(
                                tween(300, easing = FastOutSlowInEasing)
                            ) { it } + fadeIn(tween(300)),
                            initialContentExit = fadeOut(tween(200))
                        )
                        isSlideDown -> ContentTransform(
                            targetContentEnter = fadeIn(tween(200)),
                            initialContentExit = slideOutVertically(
                                tween(300, easing = FastOutSlowInEasing)
                            ) { it } + fadeOut(tween(200))
                        )
                        isPop -> ContentTransform(
                            targetContentEnter = slideInHorizontally(
                                tween(300, easing = FastOutSlowInEasing)
                            ) { -(it * 0.24f).toInt() } + fadeIn(tween(300)),
                            initialContentExit = slideOutHorizontally(
                                tween(300, easing = FastOutSlowInEasing)
                            ) { (it * 0.24f).toInt() } + fadeOut(tween(200))
                        )
                        else -> ContentTransform(
                            targetContentEnter = slideInHorizontally(
                                tween(300, easing = FastOutSlowInEasing)
                            ) { (it * 0.24f).toInt() } + fadeIn(tween(300)),
                            initialContentExit = slideOutHorizontally(
                                tween(300, easing = FastOutSlowInEasing)
                            ) { -(it * 0.24f).toInt() } + fadeOut(tween(200))
                        )
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
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
                } // end when
            } // end AnimatedContent

            if (hasCustomFloatingNav && currentTab != null) {
                // Add haptic on tab selection — wired through AppBottomNav's onTabSelected
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                ) {
                    AppBottomNav(
                        activeTab = currentTab,
                        onTabSelected = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.selectTab(it)
                        },
                        onAddClick = { viewModel.navigateTo(Screen.AddTask) }
                    )
                }
            }

            AppOverlayHost()
        }
    }
}
