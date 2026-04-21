package com.example.ididit.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ididit.data.local.AppDatabase
import com.example.ididit.ui.screen.dashboard.DashboardScreen
import com.example.ididit.ui.screen.habit.HabitScreen
import com.example.ididit.ui.screen.todo.TodoScreen
import com.example.ididit.ui.theme.Cloud
import com.example.ididit.ui.theme.Graphite
import com.example.ididit.ui.theme.Ink
import com.example.ididit.ui.theme.Mist

sealed class Screen(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Dashboard : Screen("dashboard", "看板", Icons.Filled.Home, Icons.Outlined.Home)
    data object Habit : Screen("habit", "习惯", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle)
    data object Todo : Screen("todo", "待办", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Habit,
    Screen.Todo
)

@Composable
fun AppNavigation(database: AppDatabase) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Cloud)
            ) {
                // Top border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Mist)
                )
                NavigationBar(
                    containerColor = Cloud,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                    color = if (selected) Ink else Graphite
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Mist,
                                selectedIconColor = Ink,
                                unselectedIconColor = Graphite,
                                selectedTextColor = Ink,
                                unselectedTextColor = Graphite
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(database = database)
            }
            composable(Screen.Habit.route) {
                HabitScreen(database = database)
            }
            composable(Screen.Todo.route) {
                TodoScreen(database = database)
            }
        }
    }
}
