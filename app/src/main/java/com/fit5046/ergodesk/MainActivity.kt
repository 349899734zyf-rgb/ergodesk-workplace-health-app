package com.fit5046.ergodesk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fit5046.ergodesk.navigation.AppNavHost
import com.fit5046.ergodesk.navigation.ErgoDeskBottomNavBar
import com.fit5046.ergodesk.navigation.bottomNavItems
import com.fit5046.ergodesk.ui.theme.ErgoDeskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ErgoDeskTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Hide the bottom bar on Login/SignUp/StretchDetail, and only show it on
                // the 5 main tabs (Dashboard/ErgoForm/Analytics/StretchList/Settings).
                val showBottomBar = bottomNavItems.any { item ->
                    currentDestination?.hierarchy?.any { it.route == item.route } == true
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            ErgoDeskBottomNavBar(navController)
                        }
                    }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
