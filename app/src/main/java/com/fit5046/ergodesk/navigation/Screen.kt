package com.fit5046.ergodesk.navigation

sealed class Screen(
    val route: String
) {

    object Login : Screen("login")

    object SignUp : Screen("signup")

    object Dashboard : Screen("dashboard")

    object Settings : Screen("settings")

    object ErgoForm : Screen("ergo_form")

    object Analytics : Screen("analytics")

    object StretchList : Screen("stretch_list")

    object StretchDetail :
        Screen("stretch_detail/{stretchId}") {

        fun createRoute(
            stretchId: String
        ): String {
            return "stretch_detail/$stretchId"
        }
    }
}


// -----------------------------------------------------------------------------
// Final team-agreed Bottom Navigation
//
// Home | Exercises | ErgoForm | Analytics | Settings
//
// Login, Sign Up and Stretch Detail are not Bottom Navigation destinations.
// -----------------------------------------------------------------------------

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.StretchList,
    Screen.ErgoForm,
    Screen.Analytics,
    Screen.Settings
)