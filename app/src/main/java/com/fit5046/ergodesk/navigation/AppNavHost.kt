package com.fit5046.ergodesk.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.fit5046.ergodesk.ui.screens.AnalyticsScreen
import com.fit5046.ergodesk.ui.screens.ErgoFormScreen
import com.fit5046.ergodesk.ui.screens.HomeScreen
import com.fit5046.ergodesk.ui.screens.LoginScreen
import com.fit5046.ergodesk.ui.screens.SettingsScreen
import com.fit5046.ergodesk.ui.screens.SignUpScreen
import com.fit5046.ergodesk.ui.screens.StretchDetailScreen
import com.fit5046.ergodesk.ui.screens.StretchListScreen

import com.google.firebase.auth.FirebaseAuth

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

        // ---------------------------------------------------------------------
        // Person 1
        // ---------------------------------------------------------------------

        composable("login") {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate("signup")
                },
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") {inclusive = true}
                    }
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("signup") {inclusive = true}
                    }
                },
                onSignUpSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") {inclusive = true}
                    }
                }
            )
        }


        // ---------------------------------------------------------------------
        // Person 2
        // ---------------------------------------------------------------------

        composable(
            Screen.Dashboard.route
        ) {

            // Screen.Dashboard is the "Home" tab. Settings is its own bottom-nav
            // tab now, so Home needs no in-screen navigation callback.
            HomeScreen()
        }


        composable(Screen.Settings.route) {
            SettingsScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) {inclusive = true}
                        launchSingleTop = true
                    }
                }
            )
        }


        // ---------------------------------------------------------------------
        // Person 3
        // ---------------------------------------------------------------------

        composable(
            Screen.ErgoForm.route
        ) {

            ErgoFormScreen()
        }


        composable(
            Screen.Analytics.route
        ) {

            AnalyticsScreen()
        }


        // ---------------------------------------------------------------------
        // Person 4
        // ---------------------------------------------------------------------

        composable(
            Screen.StretchList.route
        ) {

            StretchListScreen(

                onStretchClick = { stretchId ->

                    navController.navigate(
                        Screen.StretchDetail
                            .createRoute(stretchId)
                    )
                }
            )
        }


        composable(
            Screen.StretchDetail.route
        ) { backStackEntry ->

            val stretchId =
                backStackEntry
                    .arguments
                    ?.getString("stretchId")
                    ?: ""

            StretchDetailScreen(

                stretchId = stretchId,

                onBack = {

                    navController.popBackStack()
                }
            )
        }
    }
}