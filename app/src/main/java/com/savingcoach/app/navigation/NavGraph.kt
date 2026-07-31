package com.savingcoach.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.savingcoach.app.ui.auth.AuthScreen
import com.savingcoach.app.ui.chat.ChatScreen
import com.savingcoach.app.ui.dashboard.CalendarHistoryScreen
import com.savingcoach.app.ui.dashboard.DashboardScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Auth.route,
        modifier = modifier
    ) {
        composable(Routes.Auth.route) {
            AuthScreen(
                onSignedIn = {
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Dashboard.route) {
            DashboardScreen(
                onNavigateToChallenges = {
                    navController.navigate(Routes.Challenges.route)
                },
                onNavigateToCalendarHistory = {
                    navController.navigate(Routes.CalendarHistory.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Routes.Notifications.route)
                }
            )
        }

        composable(Routes.Expenses.route) {
            // TODO: Dev 4 — Replace with ExpenseListScreen
            PlaceholderScreen("Expenses")
        }

        composable(Routes.Challenges.route) {
            // TODO: Dev 3 — Replace with ChallengesScreen
            PlaceholderScreen("Challenges")
        }

        composable(Routes.AddExpense.route) {
            // TODO: Dev 4 — Replace with AddExpenseScreen
            PlaceholderScreen("Add Expense")
        }

        composable(Routes.Chat.route) {
            ChatScreen()
        }

        composable(Routes.Camera.route) {
            // TODO: Dev 2 — Replace with CameraScreen
            PlaceholderScreen("Camera")
        }

        composable(Routes.Budget.route) {
            // TODO: Dev 4 — Replace with BudgetScreen
            PlaceholderScreen("Budget")
        }

        composable(Routes.Settings.route) {
            // TODO: Dev 5 — Replace with SettingsScreen
            PlaceholderScreen("Settings")
        }

        composable(Routes.CalendarHistory.route) {
            CalendarHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Notifications.route) {
            com.savingcoach.app.ui.notifications.NotificationsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$name — Coming Soon")
    }
}
