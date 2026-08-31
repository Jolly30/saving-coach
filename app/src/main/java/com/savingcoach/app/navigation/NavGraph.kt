package com.savingcoach.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.savingcoach.app.ui.auth.AuthScreen
import com.savingcoach.app.ui.chat.ChatScreen
import com.savingcoach.app.ui.dashboard.CalendarHistoryScreen
import com.savingcoach.app.ui.dashboard.DashboardScreen
import com.savingcoach.app.ui.expenses.AddExpenseScreen
import com.savingcoach.app.ui.expenses.ExpenseScreen
import com.savingcoach.app.ui.expenses.ExpenseViewModel

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
                },
                onNeedsOnboarding = {
                    navController.navigate(Routes.Onboarding.route) {
                        popUpTo(Routes.Auth.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Routes.ForgotPassword.route)
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

        composable(Routes.Expenses.route) { backStackEntry ->
            val viewModel: ExpenseViewModel = hiltViewModel(backStackEntry)
            ExpenseScreen(
                viewModel = viewModel,
                onNavigateToAddExpense = {
                    navController.navigate(Routes.AddExpense.route)
                }
            )
        }

        composable(Routes.Challenges.route) {
            com.savingcoach.app.ui.challenges.ChallengesScreen(
                onChallengeClick = { id ->
                    navController.navigate("challenge_detail/$id")
                }
            )
        }

        composable(Routes.AddExpense.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                try {
                    navController.getBackStackEntry(Routes.Expenses.route)
                } catch (e: Exception) {
                    backStackEntry
                }
            }
            val viewModel: ExpenseViewModel = hiltViewModel(parentEntry)
            val uiState = viewModel.uiState.collectAsState().value
            AddExpenseScreen(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { amount, category, merchant, description ->
                    viewModel.addExpense(amount, category, merchant, description)
                    navController.popBackStack()
                },
                availableCategories = uiState.categories,
                onAddCategory = { emoji, name, target ->
                    viewModel.addCustomCategory(emoji, name, target)
                },
                onDeleteCategory = { categoryName ->
                    viewModel.deleteCategory(categoryName)
                }
            )
        }

        composable(Routes.Chat.route) {
            ChatScreen()
        }

        composable(Routes.Camera.route) {
            // TODO: Dev 2 — Replace with CameraScreen
            PlaceholderScreen("Camera")
        }

        composable(Routes.Budget.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                try {
                    navController.getBackStackEntry(Routes.Expenses.route)
                } catch (e: Exception) {
                    backStackEntry
                }
            }
            val viewModel: ExpenseViewModel = hiltViewModel(parentEntry)
            ExpenseScreen(
                viewModel = viewModel,
                onNavigateToAddExpense = {
                    navController.navigate(Routes.AddExpense.route)
                }
            )
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
