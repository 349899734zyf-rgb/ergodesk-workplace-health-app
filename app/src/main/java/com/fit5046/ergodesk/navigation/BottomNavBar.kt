package com.fit5046.ergodesk.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private fun screenIcon(screen: Screen) = when (screen) {
    Screen.Dashboard -> Icons.Filled.Home
    Screen.ErgoForm -> Icons.Filled.Assignment
    Screen.Analytics -> Icons.Filled.BarChart
    Screen.StretchList -> Icons.Filled.List
    Screen.Settings -> Icons.Filled.Settings
    else -> Icons.Filled.Home
}

private fun screenLabel(screen: Screen) = when (screen) {
    Screen.Dashboard -> "Dashboard"
    Screen.ErgoForm -> "ErgoForm"
    Screen.Analytics -> "Analytics"
    Screen.StretchList -> "Stretches"
    Screen.Settings -> "Settings"
    else -> ""
}

/** Bottom navigation bar switching between the 4 main screens (Dashboard/ErgoForm/Analytics/StretchList). */
@Composable
fun ErgoDeskBottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
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
                icon = { Icon(screenIcon(screen), contentDescription = screenLabel(screen)) },
                label = { Text(screenLabel(screen)) }
            )
        }
    }
}
