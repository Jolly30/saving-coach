package com.savingcoach.app.navigation

sealed class Routes(val route: String) {
    object Auth : Routes("auth")
    object Dashboard : Routes("dashboard")
    object Expenses : Routes("expenses")
    object Challenges : Routes("challenges")
    object AddExpense : Routes("add_expense")
    object Chat : Routes("chat")
    object Camera : Routes("camera")
    object Budget : Routes("budget")
    object Settings : Routes("settings")
    object CalendarHistory : Routes("calendar_history")
}
