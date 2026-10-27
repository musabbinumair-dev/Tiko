package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.TikoBottomBar
import com.example.ui.screens.AddEditTaskScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ListsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskDetailScreen
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
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = if (hasCustomFloatingNav) creamBackground else TikoBackground,
        bottomBar = {
            if (isMainScreen && !hasCustomFloatingNav) {
                TikoBottomBar(
                    currentTab = selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (hasCustomFloatingNav) creamBackground else TikoBackground)
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
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
                    TaskDetailScreen(
                        taskId = screen.taskId,
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) },
                        onBack = { viewModel.goBack() }
                    )
                }
                is Screen.AddTask -> {
                    AddEditTaskScreen(
                        taskId = null,
                        viewModel = viewModel,
                        onBack = { viewModel.goBack() }
                    )
                }
                is Screen.EditTask -> {
                    AddEditTaskScreen(
                        taskId = screen.taskId,
                        viewModel = viewModel,
                        onBack = { viewModel.goBack() }
                    )
                }
            }
        }
    }
}
