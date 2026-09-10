package com.fit5046.ergodesk.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fit5046.ergodesk.ui.screens.AnalyticsScreen
import com.fit5046.ergodesk.ui.screens.DashboardScreen
import com.fit5046.ergodesk.ui.screens.ErgoFormScreen
import com.fit5046.ergodesk.ui.screens.LoginScreen
import com.fit5046.ergodesk.ui.screens.SettingsScreen
import com.fit5046.ergodesk.ui.screens.SignUpScreen
import com.fit5046.ergodesk.ui.screens.StretchDetailScreen
import com.fit5046.ergodesk.ui.screens.StretchListScreen

/**
 * The app's full navigation graph.
 * Each member only needs to fill in the body of their own composable (LoginScreen,
 * DashboardScreen, etc.). If this file (how screens are wired together) needs to
 * change, coordinate with the team first to avoid merge conflicts.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        // Member 1
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // Member 2
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Member 3 (Suzie)
        composable(Screen.ErgoForm.route) {
            ErgoFormScreen()
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }

        // Member 4
        composable(Screen.StretchList.route) {
            StretchListScreen(
                onStretchClick = { stretchId ->
                    navController.navigate(Screen.StretchDetail.createRoute(stretchId))
                }
            )
        }
        composable(Screen.StretchDetail.route) { backStackEntry ->
            val stretchId = backStackEntry.arguments?.getString("stretchId") ?: ""
            StretchDetailScreen(
                stretchId = stretchId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
