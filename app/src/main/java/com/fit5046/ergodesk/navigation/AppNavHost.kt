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

        composable(
            Screen.Login.route
        ) {

            LoginScreen(

                onLoginSuccess = {

                    navController.navigate(
                        Screen.Dashboard.route
                    ) {

                        popUpTo(
                            Screen.Login.route
                        ) {
                            inclusive = true
                        }
                    }
                },

                onNavigateToSignUp = {

                    navController.navigate(
                        Screen.SignUp.route
                    )
                }
            )
        }


        composable(
            Screen.SignUp.route
        ) {

            SignUpScreen(

                onSignUpSuccess = {

                    // Final agreed flow:
                    // Sign Up -> Login -> Home

                    navController.navigate(
                        Screen.Login.route
                    ) {

                        popUpTo(
                            Screen.SignUp.route
                        ) {
                            inclusive = true
                        }
                    }
                },

                onNavigateToLogin = {

                    navController.popBackStack()
                }
            )
        }


        // ---------------------------------------------------------------------
        // Person 2
        // ---------------------------------------------------------------------

        composable(
            Screen.Dashboard.route
        ) {

            // Settings is now a bottom-nav tab, so Dashboard no longer needs
            // an in-screen navigation callback.
            DashboardScreen()
        }


        composable(
            Screen.Settings.route
        ) {

            SettingsScreen(

                onLogout = {

                    navController.navigate(
                        Screen.Login.route
                    ) {

                        popUpTo(0) {
                            inclusive = true
                        }
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