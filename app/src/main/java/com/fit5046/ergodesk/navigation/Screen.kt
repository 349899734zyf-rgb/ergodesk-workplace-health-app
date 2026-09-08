package com.fit5046.ergodesk.navigation

/**
 * Defines every screen (route) in the app in one place.
 * To add a new screen, just add one more object here.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")
    object ErgoForm : Screen("ergo_form")
    object Analytics : Screen("analytics")
    object StretchList : Screen("stretch_list")
    object StretchDetail : Screen("stretch_detail/{stretchId}") {
        fun createRoute(stretchId: String) = "stretch_detail/$stretchId"
    }
}

// The 4 main tabs shown in the bottom Navigation Bar.
// (Login/SignUp/Settings/StretchDetail are not bottom-tab destinations — they're
// reached via in-screen buttons instead.)
val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.ErgoForm,
    Screen.Analytics,
    Screen.StretchList
)
