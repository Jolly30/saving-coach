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
import com.savingcoach.app.ui.onboarding.AgeScreen
import com.savingcoach.app.ui.onboarding.GenderScreen
import com.savingcoach.app.ui.onboarding.SalaryScreen
import com.savingcoach.app.ui.onboarding.FieldOfWorkScreen
import com.savingcoach.app.ui.settings.EditAgeScreen
import com.savingcoach.app.ui.settings.EditEmailScreen
import com.savingcoach.app.ui.settings.EditGenderScreen
import com.savingcoach.app.ui.settings.EditSalaryScreen
import com.savingcoach.app.ui.settings.EditFieldOfWorkScreen
import com.savingcoach.app.ui.auth.ForgotPasswordScreen

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
                    navController.navigate(Routes.OnboardingAge.route) {
                        popUpTo(Routes.Auth.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Routes.ForgotPassword.route)
                }
            )
        }

        composable(Routes.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Dashboard.route) {
            DashboardScreen(
                onNavigateToChallenges = { challengeId ->
                    if (challengeId != null) {
                        navController.navigate("${Routes.Challenges.route}?challengeId=$challengeId")
                    } else {
                        navController.navigate(Routes.Challenges.route)
                    }
                },
                onNavigateToCalendarHistory = {
                    navController.navigate(Routes.CalendarHistory.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Routes.Notifications.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.Settings.route)
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

        composable(
            route = "${Routes.Challenges.route}?challengeId={challengeId}",
            arguments = listOf(androidx.navigation.navArgument("challengeId") {
                type = androidx.navigation.NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId")
            com.savingcoach.app.ui.challenges.ChallengesScreen(
                initialChallengeId = challengeId,
                onChallengeClick = { id ->
                    navController.navigate("challenge_detail/$id")
                }
            )
        }

        composable(Routes.Investment.route) {
            com.savingcoach.app.ui.investment.InvestmentScreen(
                onNavigateBack = { navController.popBackStack() }
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
                currencyPreference = uiState.currencyPreference,
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

        composable(
            route = Routes.Settings.route,
            enterTransition = {
                slideIntoContainer(
                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
            }
        ) {
            com.savingcoach.app.ui.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditUsername = {
                    navController.navigate(Routes.EditUsername.route)
                },
                onNavigateToEditAge = {
                    navController.navigate(Routes.EditAge.route)
                },
                onNavigateToEditGender = {
                    navController.navigate(Routes.EditGender.route)
                },
                onNavigateToEditSalary = {
                    navController.navigate(Routes.EditSalary.route)
                },
                onNavigateToEditFieldOfWork = {
                    navController.navigate(Routes.EditFieldOfWork.route)
                },
                onNavigateToEditEmail = {
                    navController.navigate(Routes.EditEmail.route)
                },
                onNavigateToChangePassword = {
                    navController.navigate(Routes.ChangePassword.route)
                },
                onNavigateToExportData = {
                    navController.navigate(Routes.ExportData.route)
                },
                onNavigateToEditCurrency = {
                    navController.navigate(Routes.EditCurrency.route)
                },
                onNavigateToEditLanguage = {
                    navController.navigate(Routes.EditLanguage.route)
                },
                onNavigateToAuth = {
                    navController.navigate(Routes.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.EditCurrency.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.Settings.route) }
            com.savingcoach.app.ui.settings.EditCurrencyScreen(
                viewModel = hiltViewModel(parentEntry),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EditLanguage.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.Settings.route) }
            com.savingcoach.app.ui.settings.EditLanguageScreen(
                viewModel = hiltViewModel(parentEntry),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EditUsername.route) { backStackEntry ->

            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.Settings.route) }
            com.savingcoach.app.ui.settings.EditUsernameScreen(
                viewModel = hiltViewModel(parentEntry),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EditEmail.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.Settings.route) }
            EditEmailScreen(
                viewModel = hiltViewModel(parentEntry),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EditAge.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.Settings.route) }
            EditAgeScreen(
                viewModel = hiltViewModel(parentEntry),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EditGender.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.Settings.route) }
            EditGenderScreen(
                viewModel = hiltViewModel(parentEntry),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EditSalary.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.Settings.route) }
            EditSalaryScreen(
                viewModel = hiltViewModel(parentEntry),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EditFieldOfWork.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.Settings.route) }
            EditFieldOfWorkScreen(
                viewModel = hiltViewModel(parentEntry),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ChangePassword.route) {
            com.savingcoach.app.ui.settings.ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToForgotPassword = {
                    navController.navigate(Routes.ForgotPassword.route)
                }
            )
        }

        composable(Routes.ExportData.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.Settings.route) }
            com.savingcoach.app.ui.settings.ExportScreen(
                viewModel = hiltViewModel(parentEntry),
                onNavigateBack = { navController.popBackStack() }
            )
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

        composable(Routes.OnboardingAge.route) {
            AgeScreen(
                onNavigateNext = {
                    navController.navigate(Routes.OnboardingGender.route) {
                        popUpTo(Routes.OnboardingAge.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.OnboardingGender.route) {
            GenderScreen(
                onNavigateNext = {
                    navController.navigate(Routes.OnboardingFieldOfWork.route) {
                        popUpTo(Routes.OnboardingGender.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.OnboardingFieldOfWork.route) {
            FieldOfWorkScreen(
                onNavigateNext = {
                    navController.navigate(Routes.OnboardingSalary.route) {
                        popUpTo(Routes.OnboardingFieldOfWork.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.OnboardingSalary.route) {
            SalaryScreen(
                onNavigateNext = {
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.OnboardingSalary.route) { inclusive = true }
                    }
                }
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
