package com.fit5046.ergodesk.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fit5046.ergodesk.ui.theme.ErgoGreen


private fun screenIcon(
    screen: Screen
) = when (screen) {

    Screen.Dashboard ->
        Icons.Filled.Home

    Screen.StretchList ->
        Icons.Filled.FitnessCenter

    Screen.ErgoForm ->
        Icons.Filled.Assignment

    Screen.Analytics ->
        Icons.Filled.BarChart

    Screen.Settings ->
        Icons.Filled.Settings

    else ->
        Icons.Filled.Home
}


private fun screenLabel(
    screen: Screen
) = when (screen) {

    Screen.Dashboard ->
        "Home"

    Screen.StretchList ->
        "Exercises"

    Screen.ErgoForm ->
        "ErgoForm"

    Screen.Analytics ->
        "Analytics"

    Screen.Settings ->
        "Settings"

    else ->
        ""
}


@Composable
fun ErgoDeskBottomNavBar(
    navController: NavHostController
) {

    val navBackStackEntry by
    navController.currentBackStackEntryAsState()

    val currentDestination =
        navBackStackEntry?.destination


    NavigationBar(
        containerColor = Color.White,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {

        bottomNavItems.forEach { screen ->

            val selected =
                currentDestination
                    ?.hierarchy
                    ?.any {
                        it.route == screen.route
                    } == true


            NavigationBarItem(

                selected = selected,

                onClick = {

                    navController.navigate(
                        screen.route
                    ) {

                        popUpTo(
                            navController.graph
                                .findStartDestination()
                                .id
                        ) {
                            saveState = true
                        }

                        launchSingleTop = true

                        restoreState = true
                    }
                },

                icon = {

                    Icon(
                        imageVector =
                            screenIcon(screen),

                        contentDescription =
                            screenLabel(screen)
                    )
                },

                label = {

                    Text(
                        text =
                            screenLabel(screen)
                    )
                },

                colors =
                    NavigationBarItemDefaults.colors(

                        selectedIconColor =
                            ErgoGreen,

                        selectedTextColor =
                            ErgoGreen,

                        indicatorColor =
                            Color(0xFFE7F4DF),

                        unselectedIconColor =
                            Color(0xFF656B66),

                        unselectedTextColor =
                            Color(0xFF656B66)
                    )
            )
        }
    }
}