package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.content.Context
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.screens.RoleScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.StreamScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FileBrowserScreen
import com.example.ui.screens.AppBlockerScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    val prefs = getSharedPreferences("guardian_pref", Context.MODE_PRIVATE)
                    val setupComplete = prefs.getBoolean("setup_complete", false)
                    val startDest = if (setupComplete) "login" else "setup"

                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { 450 }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { -450 }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                        },
                        popEnterTransition = {
                            slideInHorizontally(initialOffsetX = { -450 }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
                        },
                        popExitTransition = {
                            slideOutHorizontally(targetOffsetX = { 450 }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                        }
                    ) {
                        composable("setup") {
                            SetupScreen(
                                onSetupComplete = {
                                    navController.navigate("login") {
                                        popUpTo("setup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                onNavigateToRole = {
                                    navController.navigate("role") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("role") {
                            RoleScreen(
                                onNavigateToPairing = { role ->
                                    navController.navigate("pairing/$role")
                                }
                            )
                        }
                        composable(
                            route = "pairing/{role}",
                            arguments = listOf(navArgument("role") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val role = backStackEntry.arguments?.getString("role") ?: "child"
                            PairingScreen(
                                role = role,
                                onNavigateToStream = { roomCode, finalRole ->
                                    if (finalRole == "parent") {
                                        navController.navigate("dashboard/$roomCode") {
                                            popUpTo("pairing/$role") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("stream/$roomCode/$finalRole")
                                    }
                                }
                            )
                        }
                        composable(
                            route = "dashboard/{roomCode}",
                            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
                            DashboardScreen(
                                roomCode = roomCode,
                                onNavigateToLiveStream = {
                                    navController.navigate("stream/$roomCode/parent")
                                },
                                onNavigateToFiles = {
                                    navController.navigate("files/$roomCode")
                                },
                                onNavigateToAppBlocker = {
                                    navController.navigate("appblocker/$roomCode")
                                },
                                onNavigateToNotifications = {
                                    navController.navigate("notifications/$roomCode")
                                },
                                onNavigateBack = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable(
                            route = "files/{roomCode}",
                            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
                            FileBrowserScreen(
                                roomCode = roomCode,
                                onNavigateBack = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable(
                            route = "appblocker/{roomCode}",
                            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
                            AppBlockerScreen(
                                roomCode = roomCode,
                                onNavigateBack = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable(
                            route = "notifications/{roomCode}",
                            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
                            NotificationsScreen(
                                roomCode = roomCode,
                                onNavigateBack = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable(
                            route = "stream/{roomCode}/{role}",
                            arguments = listOf(
                                navArgument("roomCode") { type = NavType.StringType },
                                navArgument("role") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
                            val role = backStackEntry.arguments?.getString("role") ?: "child"
                            StreamScreen(
                                roomCode = roomCode,
                                role = role,
                                onNavigateBack = {
                                    navController.navigateUp()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
