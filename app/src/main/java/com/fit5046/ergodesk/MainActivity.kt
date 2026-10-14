package com.fit5046.ergodesk

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.fit5046.ergodesk.work.ContextEvaluationWorker
import com.fit5046.ergodesk.work.SensorIngestWorker
import com.fit5046.ergodesk.work.WeatherSyncWorker

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Android 13+ requires runtime permission for notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        SensorIngestWorker.schedule(this)
        ContextEvaluationWorker.schedule(this)
        WeatherSyncWorker.schedule(this)

        setContent {
            ErgoDeskTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Show the bottom bar only on the five main screens.
                val showBottomBar = bottomNavItems.any { item ->
                    currentDestination?.hierarchy?.any {
                        it.route == item.route
                    } == true
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