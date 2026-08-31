package com.savingcoach.app.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.savingcoach.app.data.repository.AppLanguage

interface Strings {
    // General
    val save: String
    val cancel: String
    val confirm: String
    val delete: String
    val edit: String
    val close: String
    val back: String
    val loading: String
    val errorGeneric: String
    val seeAll: String
    val day: String
    val days: String
    val all: String
    val retry: String
    val isBurmese: Boolean
    fun formatNumber(text: String): String
    fun formatNumber(number: Number): String
    fun formatAmount(amount: Double, currencyPreference: String = "MMK", rate: Double = 1.0, isInvestment: Boolean = false): String

    // Navigation
    val navDashboard: String
    val navExpenses: String
    val navChallenges: String
    val navInvestment: String
    val navAssistant: String

    // Dashboard
    val dashboardTitle: String
    val budget: String
    val spent: String
    val remaining: String
    val savings: String
    val investmentValue: String
    val rate: String
    val savingChallenges: String
    val activeLabel: String
    val monthlyOverview: String
    val recentExpenses: String
    val categorySpending: String

    // Expenses
    val expense: String
    val expenses: String
    val investments: String
    val expensesTitle: String
    val monthlyOverallBudget: String
    val setBudget: String
    val editBudget: String
    val globalBudgetLimit: String
    val addExpense: String
    val categories: String
    val target: String
    val addCategory: String
    val categoryName: String
    val targetLimit: String
    val merchant: String
    val notes: String
    val amount: String
    val category: String
    val date: String
    val spendingBuckets: String
    val newBucket: String
    val allBuckets: String
    val noExpensesInCategory: String
    val noExpensesYet: String
    val logExpenseTitle: String
    val selectSpendingBucketRequired: String
    val merchantOptional: String
    val noteOptional: String
    val saveExpense: String
    val addCustomBucket: String
    val deleteBucketTitle: String
    fun deleteBucketConfirm(name: String): String
    fun localizeCategory(name: String): String

    // Challenges
    val challengesTitle: String
    val totalSaved: String
    val active: String
    val completed: String
    val stopped: String
    val failed: String
    val createChallenge: String
    val editChallenge: String
    val challengeName: String
    val targetAmount: String
    val durationDays: String
    val deposit: String
    val savePerDay: String
    val depositHistory: String
    val myChallenge: String

    // Investment
    val investmentTitle: String
    val portfolioSummary: String
    val portfolioValue: String
    val allHoldings: String
    val stocksAndEtfs: String
    val crypto: String
    val marketNews: String
    val addAsset: String
    val unitsOwned: String
    val avgBuyPrice: String
    val currentValue: String
    val realizedValue: String
    val unrealizedPL: String
    val realizedPL: String
    val totalROI: String
    val searchHoldingsAndNews: String
    val news: String
    val soldOut: String
    val stop: String
    val avgBuyExit: String
    val totalCostBasis: String
    val unitsOrSharesOwned: String
    val saveToPortfolio: String
    val searchAsset: String
    val assetType: String
    val stockOrEtf: String
    val noInvestmentsLogged: String
    val loadingMarketNews: String
    val deleteAssetTitle: String
    val deleteAssetConfirmMsg: String
    val typeCryptoPlaceholder: String
    val typeStockPlaceholder: String
    fun noResultsFoundFor(query: String): String

    // Settings
    val settingsTitle: String
    val account: String
    val username: String
    val age: String
    val gender: String
    val fieldOfWork: String
    val salaryRange: String
    val email: String
    val resetPassword: String
    val data: String
    val exportDataCsv: String
    val preferences: String
    val theme: String
    val light: String
    val dark: String
    val currency: String
    val language: String
    val notifications: String
    val deleteNotificationTitle: String
    val deleteNotificationConfirmMsg: String
    val selectAllNotifications: String
    val selectNotifications: String
    val noNewNotifications: String
    val aboutAndInfo: String
    val about: String
    val version: String
    val signOut: String
    val editLanguage: String
    val selectDisplayLanguage: String

    // Edit Sub-Screens
    val editUsernameTitle: String
    val yourUsername: String
    val usernameDesc: String
    val usernameLabel: String
    val editAgeTitle: String
    val yourAge: String
    val ageDesc: String
    val ageLabel: String
    val editGenderTitle: String
    val selectGender: String
    val male: String
    val female: String
    val preferNotToSay: String
    val editSalaryTitle: String
    val selectSalaryRange: String
    val salaryUnder1M: String
    val salary1MTo3M: String
    val salary3MTo54M: String
    val salary54MTo10M: String
    val salaryAbove10M: String
    val editFieldOfWorkTitle: String
    val selectFieldOfWork: String
    val fieldSoftware: String
    val fieldHealthcare: String
    val fieldEducation: String
    val fieldFinance: String
    val fieldMarketing: String
    val fieldDesign: String
    val fieldSales: String
    val fieldBusiness: String
    val fieldOther: String
    val editCurrencyTitle: String
    val selectCurrency: String
    val currencyMMKLabel: String
    val currencyUSDLabel: String
    val currencyMixedLabel: String
    val editEmailTitle: String
    val yourEmail: String
    val emailDesc: String
    val changePasswordTitle: String
    val currentPassword: String
    val newPassword: String

    // Export Data Screen
    val exportDataTitle: String
    val spendingTab: String
    val savingsTab: String
    val investmentsTab: String
    val dateRange: String
    val from: String
    val to: String
    val selectDate: String
    val clearDates: String
    val filterByCategory: String
    val filterByChallenge: String
    val filterByInvestment: String
    val leaveUncheckedHint: String
    val searchPlaceholder: String
    val exportToCsv: String
    val categoryBillsAndUtilities: String
    val categoryFoodAndDining: String
    val categoryShopping: String
    val categoryTransportation: String
    val categoryEntertainment: String
    val categoryEducation: String
    val categoryHealth: String
    val categoryOther: String
    val calendarFilterAll: String
    val calendarFilterAllCategories: String
    val dayHeaders: List<String>
    val challengeEmergencyFund: String
    val challengeVacation: String
    val monthView: String
    val yearView: String
    val highSaver: String
    val lowSaver: String
    val highExpense: String
    val lowExpense: String
    val highInvestment: String
    val lowInvestment: String
    fun formatMonthYear(month: java.time.YearMonth): String
    fun formatMonthName(month: java.time.YearMonth): String
    fun formatExpenseDateTime(createdAt: Long, dateFallback: String = ""): String

    // Message Boxes, Dialogs & Alerts
    fun deleteExpenseConfirmMsg(amountStr: String): String
    val deleteExpenseConfirmTitle: String
    val budgetZeroWarning: String
    fun budgetExceedWarning(maxStr: String): String
    fun availableCapacityMsg(capStr: String): String
    val categoryNameEmptyError: String
    val setGlobalBudgetFirstError: String
    val stopChallengeTitle: String
    fun stopChallengeMsg(amountStr: String): String
    fun stoppedChallengeRecorded(amountStr: String): String
    val stopChallengeConfirm: String
    val stopChallengeKeep: String
    val deleteChallengeTitle: String
    val deleteChallengeMsg: String
    val deleteHoldingTitle: String
    val deleteHoldingMsg: String
    val settlementDialogTitle: String
    val exitMarketPrice: String
    val confirmSettlement: String
    val signOutConfirmTitle: String
    val signOutConfirmMsg: String
    val passwordResetSent: String

    // Challenge Screens
    val allTime: String
    val yourChallenges: String
    val searchChallenges: String
    val noChallengesYet: String
    val noChallengesDesc: String
    val challengeCompletedTitle: String
    val challengeCompletedMsg: String
    val challengeStoppedTitle: String
    val cannotDepositStoppedMsg: String
    val challengeFailedTitle: String
    val challengeFailedMsg: String
    val challengeFailedDetail: String
    val ok: String
    val newChallenge: String
    val newChallengeDesc: String
    val chooseTemplate: String
    val yourTemplate: String
    val templateConstant: String
    val templateFlexi: String
    val templateEnvelope: String
    val templateNoSpend: String
    val emojiLabel: String
    val challengeNameLabel: String
    val targetAmountLabel: String
    val durationDaysLabel: String
    val startChallenge: String
    val editChallengeTitle: String
    val saveChanges: String
    val challenge52Week: String
    val challengeNoSpendWeek: String
    val progressMap: String
    val enterAmount: String
    val habitTracker: String
    val zeroSpendDay: String
    val fullProgressMap: String
    val skipped: String
    val missed: String
    val alreadyCheckedInMsg: String
    val alreadyOpenedEnvelopeMsg: String
    val challengeNameExists: String
    val youSaved: String
    val missedThisDay: String
    val challengeEnvelopeDesc: String
    val challenge7DayDesc: String
    val challengeNoSpendDesc: String
    val challengeCustomDesc: String
    val challenge100Envelope: String
    val challenge7DaySprint: String
    val challenge1KADay: String
    fun daysLeftCount(days: Long): String
    fun stepsDoneCount(done: Int, total: Int): String
    fun daysCompletedCount(done: Int, total: Int): String
    fun percentComplete(percent: Int): String
    fun mustBeAtMostDays(days: Int): String
    fun mustBeAtLeast(min: Long): String
    fun mustBeGreaterThan(amount: String): String
    fun savePerDay(amount: String): String
    fun savedEnvelopeNumber(step: Int): String
    fun zeroSpendDayTitle(day: Int): String
    fun localizeChallengeTitle(title: String): String
}

object EnglishStrings : Strings {
    override val save = "Save"
    override val cancel = "Cancel"
    override val confirm = "Confirm"
    override val delete = "Delete"
    override val edit = "Edit"
    override val close = "Close"
    override val back = "Back"
    override val loading = "Loading..."
    override val errorGeneric = "Something went wrong"
    override val seeAll = "See All"
    override val day = "day"
    override val days = "days"
    override val all = "All"
    override val retry = "Retry"
    override val isBurmese = false
    override fun formatNumber(text: String): String = text
    override fun formatNumber(number: Number): String = number.toString()
    override fun formatAmount(amount: Double, currencyPreference: String, rate: Double, isInvestment: Boolean): String =
        com.savingcoach.app.utils.InvestmentCalculations.formatValue(amount, currencyPreference, rate, isInvestment)

    override val navDashboard = "Dashboard"
    override val navExpenses = "Expenses"
    override val navChallenges = "Challenges"
    override val navInvestment = "Investment"
    override val navAssistant = "Assistant"

    override val dashboardTitle = "Dashboard"
    override val budget = "Budget"
    override val spent = "Spent"
    override val remaining = "Remaining"
    override val savings = "Savings"
    override val investmentValue = "Investment Value"
    override val rate = "Rate"
    override val savingChallenges = "Saving Challenges"
    override val activeLabel = "active"
    override val monthlyOverview = "Monthly Overview"
    override val recentExpenses = "Recent Expenses"
    override val categorySpending = "Category Spending"

    override val expense = "Expense"
    override val expenses = "Expenses"
    override val investments = "Investments"
    override val expensesTitle = "Expenses"
    override val monthlyOverallBudget = "Monthly Overall Budget"
    override val setBudget = "Set Budget"
    override val editBudget = "Edit Budget"
    override val globalBudgetLimit = "Global Budget Limit"
    override val addExpense = "Add Expense"
    override val categories = "Categories"
    override val target = "Target"
    override val addCategory = "Add Category"
    override val categoryName = "Category Name"
    override val targetLimit = "Monthly Target Limit"
    override val merchant = "Merchant / Place"
    override val notes = "Notes / Description"
    override val amount = "Amount"
    override val category = "Category"
    override val date = "Date"
    override val spendingBuckets = "SPENDING BUCKETS"
    override val newBucket = "New Bucket"
    override val allBuckets = "All Buckets"
    override val noExpensesInCategory = "No expenses in this category."
    override val noExpensesYet = "No expenses yet. Tap + to log your first expense!"
    override val logExpenseTitle = "Log Expense"
    override val selectSpendingBucketRequired = "Select Spending Bucket (Required) *"
    override val merchantOptional = "Merchant / Store (Optional)"
    override val noteOptional = "Note / Description (Optional)"
    override val saveExpense = "Save Expense"
    override val addCustomBucket = "Add Custom Spending Bucket"
    override val deleteBucketTitle = "Delete Spending Bucket"
    override fun deleteBucketConfirm(name: String) = "Are you sure you want to delete '$name'? This bucket will be removed, but your logged expenses will NOT be deleted."
    override fun localizeCategory(name: String): String = name

    override val challengesTitle = "Saving Challenges"
    override val totalSaved = "Total Saved"
    override val active = "Active"
    override val completed = "Completed"
    override val stopped = "Stopped"
    override val failed = "Failed"
    override val createChallenge = "Create Challenge"
    override val editChallenge = "Edit Challenge"
    override val challengeName = "Challenge Name"
    override val targetAmount = "Target Amount"
    override val durationDays = "Duration (Days)"
    override val deposit = "Deposit"
    override val savePerDay = "Save"
    override val depositHistory = "Deposit History"
    override val myChallenge = "My Challenge"

    override val investmentTitle = "Investment Analysis"
    override val portfolioSummary = "Portfolio & Market Overview"
    override val portfolioValue = "PORTFOLIO VALUE"
    override val allHoldings = "All Holdings"
    override val stocksAndEtfs = "Stocks & ETFs"
    override val crypto = "Crypto"
    override val marketNews = "Market News"
    override val addAsset = "Add Asset"
    override val unitsOwned = "Units owned"
    override val avgBuyPrice = "Avg buy price"
    override val currentValue = "Current value"
    override val realizedValue = "Realized value"
    override val unrealizedPL = "Unrealized P/L"
    override val realizedPL = "Realized Profit / Loss"
    override val totalROI = "Total Return"
    override val searchHoldingsAndNews = "Search holdings and news..."
    override val news = "News"
    override val soldOut = "SOLD OUT"
    override val stop = "Stop"
    override val avgBuyExit = "Avg buy / Exit"
    override val totalCostBasis = "Total Cost Basis"
    override val unitsOrSharesOwned = "Units / Shares Owned"
    override val saveToPortfolio = "Save to Portfolio"
    override val searchAsset = "Search Asset"
    override val assetType = "Asset Type"
    override val stockOrEtf = "Stock / ETF"
    override val noInvestmentsLogged = "No investments logged yet.\nTap + to add your first asset."
    override val loadingMarketNews = "Loading market news..."
    override val deleteAssetTitle = "Delete Asset"
    override val deleteAssetConfirmMsg = "Are you sure you want to permanently delete this asset? This action cannot be undone."
    override val typeCryptoPlaceholder = "Type crypto name (e.g., bitcoin, solana)..."
    override val typeStockPlaceholder = "Type stock ticker (e.g., AAPL, TSLA)..."
    override fun noResultsFoundFor(query: String) = "No results found for \"$query\""

    override val settingsTitle = "Settings"
    override val account = "ACCOUNT"
    override val username = "Username"
    override val age = "Age"
    override val gender = "Gender"
    override val fieldOfWork = "Field of Work"
    override val salaryRange = "Salary Range"
    override val email = "Email"
    override val resetPassword = "Reset Password"
    override val data = "DATA"
    override val exportDataCsv = "Export Data (CSV)"
    override val preferences = "PREFERENCES"
    override val theme = "Theme"
    override val light = "Light"
    override val dark = "Dark"
    override val currency = "Currency"
    override val language = "Language"
    override val notifications = "Notifications"
    override val deleteNotificationTitle = "Delete Notification"
    override val deleteNotificationConfirmMsg = "Are you sure you want to delete this notification?"
    override val selectAllNotifications = "Select all notifications"
    override val selectNotifications = "Select notifications"
    override val noNewNotifications = "No new notifications"
    override val aboutAndInfo = "ABOUT & INFO"
    override val about = "About"
    override val version = "Version"
    override val signOut = "Sign Out"
    override val editLanguage = "Edit Language"
    override val selectDisplayLanguage = "Select display language"

    // Edit Sub-Screens
    override val editUsernameTitle = "Edit Username"
    override val yourUsername = "Your username"
    override val usernameDesc = "This is the name that will be displayed on your profile and across the app."
    override val usernameLabel = "Username"
    override val editAgeTitle = "Edit Age"
    override val yourAge = "Your age"
    override val ageDesc = "Providing your age helps us tailor your budgeting recommendations."
    override val ageLabel = "Age"
    override val editGenderTitle = "Edit Gender"
    override val selectGender = "Select your gender"
    override val male = "Male"
    override val female = "Female"
    override val preferNotToSay = "Prefer not to say"
    override val editSalaryTitle = "Edit Salary Range"
    override val selectSalaryRange = "Select your salary range"
    override val salaryUnder1M = "Under 1,000,000 MMK"
    override val salary1MTo3M = "1,000,000 - 3,000,000 MMK"
    override val salary3MTo54M = "3,000,000 - 5,400,000 MMK"
    override val salary54MTo10M = "5,400,000 - 10,000,000 MMK"
    override val salaryAbove10M = "Above 10,000,000 MMK"
    override val editFieldOfWorkTitle = "Edit Field of Work"
    override val selectFieldOfWork = "Select your field of work"
    override val fieldSoftware = "Software Engineering"
    override val fieldHealthcare = "Healthcare"
    override val fieldEducation = "Education"
    override val fieldFinance = "Finance"
    override val fieldMarketing = "Marketing"
    override val fieldDesign = "Design"
    override val fieldSales = "Sales"
    override val fieldBusiness = "Business"
    override val fieldOther = "Other"
    override val editCurrencyTitle = "Edit Currency"
    override val selectCurrency = "Select currency"
    override val currencyMMKLabel = "Burmese Kyat (MMK)"
    override val currencyUSDLabel = "Dollars ($)"
    override val currencyMixedLabel = "Mixed (Investments in $, others in MMK)"
    override val editEmailTitle = "Edit Email"
    override val yourEmail = "Your email"
    override val emailDesc = "Used for account login and notifications."
    override val changePasswordTitle = "Change Password"
    override val currentPassword = "Current Password"
    override val newPassword = "New Password"

    // Export Data Screen
    override val exportDataTitle = "Export Data"
    override val spendingTab = "Spending"
    override val savingsTab = "Savings"
    override val investmentsTab = "Investments"
    override val dateRange = "Date Range"
    override val from = "From"
    override val to = "To"
    override val selectDate = "Select Date"
    override val clearDates = "Clear Dates"
    override val filterByCategory = "Filter by Category"
    override val filterByChallenge = "Filter by Challenge"
    override val filterByInvestment = "Filter by Investment"
    override val leaveUncheckedHint = "Leave all unchecked to export everything."
    override val searchPlaceholder = "Search..."
    override val exportToCsv = "Export to CSV"
    override val categoryBillsAndUtilities = "Bills & Utilities"
    override val categoryFoodAndDining = "Food & Dining"
    override val categoryShopping = "Shopping"
    override val categoryTransportation = "Transportation"
    override val categoryEntertainment = "Entertainment"
    override val categoryEducation = "Education"
    override val categoryHealth = "Health"
    override val categoryOther = "Other"
    override val calendarFilterAll = "All"
    override val calendarFilterAllCategories = "All Categories"
    override val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    override val challengeEmergencyFund = "Emergency Fund"
    override val challengeVacation = "Vacation"
    override val monthView = "Month"
    override val yearView = "Year"
    override val highSaver = "High Saver"
    override val lowSaver = "Low Saver"
    override val highExpense = "High Expense"
    override val lowExpense = "Low Expense"
    override val highInvestment = "High Investment"
    override val lowInvestment = "Low Investment"
    override fun formatMonthYear(month: java.time.YearMonth): String =
        month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
    override fun formatMonthName(month: java.time.YearMonth): String =
        month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM"))
    override fun formatExpenseDateTime(createdAt: Long, dateFallback: String): String {
        return try {
            val dateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", java.util.Locale.US)
            val dateOnlyFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.US)
            if (createdAt > 0L) {
                val instant = java.time.Instant.ofEpochMilli(createdAt)
                val zdt = instant.atZone(java.time.ZoneId.systemDefault())
                zdt.format(dateTimeFormatter)
            } else if (dateFallback.isNotEmpty()) {
                val parsed = java.time.LocalDate.parse(dateFallback)
                parsed.format(dateOnlyFormatter)
            } else {
                "Today"
            }
        } catch (e: Exception) {
            dateFallback.ifEmpty { "Today" }
        }
    }

    override fun deleteExpenseConfirmMsg(amountStr: String) = "Are you sure you want to delete this expense of $amountStr?"
    override val deleteExpenseConfirmTitle = "Delete Expense"
    override val budgetZeroWarning = "⚠️ Monthly Overall Budget is currently 0. Please set Global Budget first."
    override fun budgetExceedWarning(maxStr: String) = "⚠️ Cannot exceed Global Budget! Max available for this category: $maxStr."
    override fun availableCapacityMsg(capStr: String) = "Available Global Capacity: $capStr"
    override val categoryNameEmptyError = "Category name cannot be empty."
    override val setGlobalBudgetFirstError = "Please set a Monthly Overall Budget before setting category targets."
    override val stopChallengeTitle = "Stop Challenge"
    override fun stopChallengeMsg(amountStr: String) = "Your saved amount of $amountStr will be recorded, but you won't be able to make any more deposits."
    override fun stoppedChallengeRecorded(amountStr: String) = "This challenge has been stopped. Your saved amount of $amountStr has been recorded."
    override val stopChallengeConfirm = "Stop Challenge"
    override val stopChallengeKeep = "Keep Challenge"
    override val deleteChallengeTitle = "Delete Challenge"
    override val deleteChallengeMsg = "Are you sure you want to delete this challenge? This action cannot be undone."
    override val deleteHoldingTitle = "Delete Asset"
    override val deleteHoldingMsg = "Are you sure you want to remove this holding from your portfolio?"
    override val settlementDialogTitle = "Stop / Settle Asset"
    override val exitMarketPrice = "Exit Market Price"
    override val confirmSettlement = "Confirm Settlement"
    override val signOutConfirmTitle = "Sign Out"
    override val signOutConfirmMsg = "Are you sure you want to sign out?"
    override val passwordResetSent = "Password reset email sent!"

    // Challenge Screens
    override val allTime = "All Time"
    override val yourChallenges = "Your Challenges"
    override val searchChallenges = "Search challenges..."
    override val noChallengesYet = "No challenges yet"
    override val noChallengesDesc = "Start your first saving challenge and watch your savings grow!"
    override val challengeCompletedTitle = "🎉 Challenge Completed! 🎉"
    override val challengeCompletedMsg = "Amazing job! You have successfully reached your saving goal."
    override val challengeStoppedTitle = "⏸️ Challenge Stopped"
    override val cannotDepositStoppedMsg = "You can no longer make deposits to this challenge."
    override val challengeFailedTitle = "❌ Challenge Failed"
    override val challengeFailedMsg = "The challenge ended before reaching the target goal."
    override val challengeFailedDetail = "The saving target was not reached in time."
    override val ok = "OK"
    override val newChallenge = "New Challenge"
    override val newChallengeDesc = "Design your next savings streak"
    override val chooseTemplate = "CHOOSE TEMPLATE"
    override val yourTemplate = "YOUR TEMPLATE"
    override val templateConstant = "Constant"
    override val templateFlexi = "Flexi"
    override val templateEnvelope = "Envelope"
    override val templateNoSpend = "No-Spend"
    override val emojiLabel = "EMOJI"
    override val challengeNameLabel = "CHALLENGE NAME"
    override val targetAmountLabel = "TARGET AMOUNT"
    override val durationDaysLabel = "DURATION (DAYS)"
    override val startChallenge = "Start Challenge"
    override val editChallengeTitle = "Edit Challenge"
    override val saveChanges = "Save Changes"
    override val challenge52Week = "52-Week Challenge"
    override val challengeNoSpendWeek = "No-Spend Day"
    override val progressMap = "PROGRESS MAP"
    override val enterAmount = "Enter amount"
    override val habitTracker = "HABIT TRACKER"
    override val zeroSpendDay = "Zero Spend Day"
    override val fullProgressMap = "Full Progress Map"
    override val skipped = "Skipped"
    override val missed = "Missed"
    override val alreadyCheckedInMsg = "You've already completed today's check-in! Come back tomorrow."
    override val alreadyOpenedEnvelopeMsg = "You've already opened an envelope today! Come back tomorrow."
    override val challengeNameExists = "Challenge name already exists"
    override val youSaved = "YOU SAVED"
    override val missedThisDay = "Oops! You missed this day."
    override val challengeEnvelopeDesc = "Pick an envelope, save the number"
    override val challenge7DayDesc = "One intense week of saving"
    override val challengeNoSpendDesc = "Zero non-essentials for 7 days"
    override val challengeCustomDesc = "A custom saving streak to hit your goal"
    override val challenge100Envelope = "100 Envelope"
    override val challenge7DaySprint = "7-Day Sprint"
    override val challenge1KADay = "1K a Day"
    override fun daysLeftCount(days: Long) = "$days days left"
    override fun stepsDoneCount(done: Int, total: Int) = "$done of $total steps done"
    override fun daysCompletedCount(done: Int, total: Int) = "$done of $total days completed"
    override fun percentComplete(percent: Int) = "$percent% complete"
    override fun mustBeAtMostDays(days: Int) = "Must be at most $days days (end of month)"
    override fun mustBeAtLeast(min: Long) = "Must be >= $min"
    override fun mustBeGreaterThan(amount: String) = "Must be > $amount"
    override fun savePerDay(amount: String) = "Save $amount / day"
    override fun savedEnvelopeNumber(step: Int) = "Saved Envelope #$step"
    override fun zeroSpendDayTitle(day: Int) = "Zero Spend Day $day"
    override fun localizeChallengeTitle(title: String): String = title
}

object BurmeseStrings : Strings {
    override val save = "သိမ်းမည်"
    override val cancel = "မလုပ်တော့ပါ"
    override val confirm = "အတည်ပြုမည်"
    override val delete = "ဖျက်မည်"
    override val edit = "ပြင်ဆင်မည်"
    override val close = "ပိတ်မည်"
    override val back = "နောက်သို့"
    override val loading = "ခေတ္တစောင့်ပါ..."
    override val errorGeneric = "တစ်ခုခု မှားယွင်းနေပါသည်"
    override val seeAll = "အားလုံးကြည့်ရန်"
    override val day = "ရက်"
    override val days = "ရက်"
    override val all = "အားလုံး"
    override val retry = "ထပ်စမ်းကြည့်မည်"
    override val isBurmese = true
    override fun formatNumber(text: String): String = text
    override fun formatNumber(number: Number): String = number.toString()
    override fun formatAmount(amount: Double, currencyPreference: String, rate: Double, isInvestment: Boolean): String =
        com.savingcoach.app.utils.InvestmentCalculations.formatValue(amount, currencyPreference, rate, isInvestment)

    override val navDashboard = "ပင်မ"
    override val navExpenses = "အသုံးစရိတ်"
    override val navChallenges = "စိန်ခေါ်မှုများ"
    override val navInvestment = "ရင်းနှီးမြှုပ်နှံမှု"
    override val navAssistant = "လက်ထောက်"

    override val dashboardTitle = "ပင်မစာမျက်နှာ"
    override val budget = "ဘတ်ဂျက်"
    override val spent = "သုံးစွဲငွေ"
    override val remaining = "ကျန်ငွေ"
    override val savings = "စုဆောင်းငွေ"
    override val investmentValue = "ရင်းနှီးမြှုပ်နှံမှုတန်ဖိုး"
    override val rate = "ငွေလဲနှုန်း"
    override val savingChallenges = "ငွေစုစိန်ခေါ်မှုများ"
    override val activeLabel = "ခု လုပ်ဆောင်နေဆဲ"
    override val monthlyOverview = "လစဉ်သုံးသပ်ချက်"
    override val recentExpenses = "လတ်တလော အသုံးစရိတ်များ"
    override val categorySpending = "ကဏ္ဍအလိုက် သုံးစွဲမှု"

    override val expense = "အသုံးစရိတ်"
    override val expenses = "အသုံးစရိတ်"
    override val investments = "ရင်းနှီးမြှုပ်နှံမှု"
    override val expensesTitle = "အသုံးစရိတ်များ"
    override val monthlyOverallBudget = "လစဉ် စုစုပေါင်း ဘတ်ဂျက်"
    override val setBudget = "ဘတ်ဂျက်သတ်မှတ်ရန်"
    override val editBudget = "ဘတ်ဂျက်ပြင်ရန်"
    override val globalBudgetLimit = "စုစုပေါင်း ဘတ်ဂျက်ပမာဏ"
    override val addExpense = "အသုံးစရိတ်ထည့်ရန်"
    override val categories = "ကဏ္ဍများ"
    override val target = "ပစ်မှတ်"
    override val addCategory = "ကဏ္ဍအသစ်ထည့်ရန်"
    override val categoryName = "ကဏ္ဍအမည်"
    override val targetLimit = "လစဉ် ပစ်မှတ်ပမာဏ"
    override val merchant = "ဆိုင် / နေရာ"
    override val notes = "မှတ်စု / အကြောင်းအရာ"
    override val amount = "ပမာဏ"
    override val category = "ကဏ္ဍ"
    override val date = "ရက်စွဲ"
    override val spendingBuckets = "သုံးစွဲမှု ကဏ္ဍများ"
    override val newBucket = "ကဏ္ဍအသစ်"
    override val allBuckets = "ကဏ္ဍအားလုံး"
    override val noExpensesInCategory = "ဤကဏ္ဍတွင် အသုံးစရိတ် မရှိသေးပါ။"
    override val noExpensesYet = "အသုံးစရိတ် မရှိသေးပါ။ ပထမဆုံး အသုံးစရိတ်ထည့်ရန် + ကို နှိပ်ပါ!"
    override val logExpenseTitle = "အသုံးစရိတ် မှတ်တမ်းတင်ရန်"
    override val selectSpendingBucketRequired = "သုံးစွဲမှု ကဏ္ဍ ရွေးချယ်ပါ (မဖြစ်မနေ) *"
    override val merchantOptional = "ဆိုင် / နေရာ (စိတ်ကြိုက်)"
    override val noteOptional = "မှတ်စု / အကြောင်းအရာ (စိတ်ကြိုက်)"
    override val saveExpense = "အသုံးစရိတ် သိမ်းမည်"
    override val addCustomBucket = "ကဏ္ဍအသစ်ထည့်ရန်"
    override val deleteBucketTitle = "သုံးစွဲမှု ကဏ္ဍ ဖျက်ရန်"
    override fun deleteBucketConfirm(name: String) = "'$name' ကို ဖျက်ရန် သေချာပါသလား? ဤကဏ္ဍကို ဖယ်ရှားမည် ဖြစ်သော်လည်း မှတ်တမ်းတင်ထားသော အသုံးစရိတ်များကို ဖျက်မည်မဟုတ်ပါ။"
    override fun localizeCategory(name: String): String {
        return when (name.trim().lowercase()) {
            "bills & utilities", "bills and utilities", "bills", "utilities" -> categoryBillsAndUtilities
            "food & dining", "food and dining", "food", "dining" -> categoryFoodAndDining
            "shopping" -> categoryShopping
            "transportation", "transport" -> categoryTransportation
            "entertainment" -> categoryEntertainment
            "education" -> categoryEducation
            "health", "healthcare" -> categoryHealth
            "other", "others" -> categoryOther
            else -> name
        }
    }

    override val challengesTitle = "ငွေစုစိန်ခေါ်မှုများ"
    override val totalSaved = "စုစုပေါင်း စုငွေ"
    override val active = "လုပ်ဆောင်နေဆဲ"
    override val completed = "အောင်မြင်ပြီး"
    override val stopped = "ရပ်တန့်ထားသော"
    override val failed = "မအောင်မြင်ပါ"
    override val createChallenge = "စိန်ခေါ်မှုအသစ်ဖန်တီးရန်"
    override val editChallenge = "စိန်ခေါ်မှုပြင်ဆင်ရန်"
    override val challengeName = "စိန်ခေါ်မှုအမည်"
    override val targetAmount = "စုဆောင်းမည့် ပစ်မှတ်ငွေ"
    override val durationDays = "ကြာချိန် (ရက်ပေါင်း)"
    override val deposit = "ငွေထည့်ရန်"
    override val savePerDay = "စုရမည့်ငွေ"
    override val depositHistory = "ငွေထည့်သွင်းမှုမှတ်တမ်း"
    override val myChallenge = "ကျွန်ုပ်၏စိန်ခေါ်မှု"

    override val investmentTitle = "ရင်းနှီးမြှုပ်နှံမှု သုံးသပ်ချက်"
    override val portfolioSummary = "စုစုပေါင်း အကျဉ်းချုပ်နှင့် ဈေးကွက်"
    override val portfolioValue = "စုစုပေါင်း ရင်းနှီးမြှုပ်နှံမှု တန်ဖိုး"
    override val allHoldings = "ပိုင်ဆိုင်မှုအားလုံး"
    override val stocksAndEtfs = "စတော့နှင့် အီးတီအက်ဖ်"
    override val crypto = "ခရစ်ပတို"
    override val marketNews = "ဈေးကွက်သတင်းများ"
    override val addAsset = "ပိုင်ဆိုင်မှုအသစ်ထည့်ရန်"
    override val unitsOwned = "ပိုင်ဆိုင်သည့် အရေအတွက်"
    override val avgBuyPrice = "ပျမ်းမျှ ဝယ်ယူဈေး"
    override val currentValue = "လက်ရှိ တန်ဖိုး"
    override val realizedValue = "ထုတ်ယူရရှိငွေ"
    override val unrealizedPL = "မထုတ်ယူရသေးသော အမြတ်/အရှုံး"
    override val realizedPL = "အမှန်တကယ် အမြတ် / အရှုံး"
    override val totalROI = "စုစုပေါင်း အကျိုးအမြတ်"
    override val searchHoldingsAndNews = "ပိုင်ဆိုင်မှုများနှင့် သတင်းများ ရှာဖွေရန်..."
    override val news = "သတင်းများ"
    override val soldOut = "ရောင်းချပြီး"
    override val stop = "ရပ်တန့်မည်"
    override val avgBuyExit = "ပျမ်းမျှ ဝယ်ဈေး / ထုတ်ယူဈေး"
    override val totalCostBasis = "စုစုပေါင်း ရင်းနှီးငွေ"
    override val unitsOrSharesOwned = "ပိုင်ဆိုင်သည့် ယူနစ် / ရှယ်ယာ အရေအတွက်"
    override val saveToPortfolio = "ရင်းနှီးမြှုပ်နှံမှုတွင် သိမ်းမည်"
    override val searchAsset = "ပိုင်ဆိုင်မှု ရှာဖွေရန်"
    override val assetType = "အမျိုးအစား"
    override val stockOrEtf = "စတော့ရှယ်ယာ / ETF"
    override val noInvestmentsLogged = "ရင်းနှီးမြှုပ်နှံမှု မရှိသေးပါ။\nပထမဆုံး ပိုင်ဆိုင်မှု ထည့်သွင်းရန် + ကို နှိပ်ပါ။"
    override val loadingMarketNews = "သတင်းများ ရယူနေပါသည်..."
    override val deleteAssetTitle = "ပိုင်ဆိုင်မှု ဖျက်ရန်"
    override val deleteAssetConfirmMsg = "ဤပိုင်ဆိုင်မှုကို အပြီးအပိုင် ဖျက်ရန် သေချာပါသလား? ဖျက်ပြီးပါက ပြန်လည်ရယူ၍ မရနိုင်ပါ။"
    override val typeCryptoPlaceholder = "ခရစ်ပတို အမည် ရိုက်ထည့်ပါ (ဥပမာ bitcoin, solana)..."
    override val typeStockPlaceholder = "စတော့အမည် ရိုက်ထည့်ပါ (ဥပမာ AAPL, TSLA)..."
    override fun noResultsFoundFor(query: String) = "\"$query\" အတွက် ရလဒ် မတွေ့ပါ"

    override val settingsTitle = "ဆက်တင်များ"
    override val account = "အကောင့်"
    override val username = "အသုံးပြုသူအမည်"
    override val age = "အသက်"
    override val gender = "ကျား/မ"
    override val fieldOfWork = "လုပ်ငန်းနယ်ပယ်"
    override val salaryRange = "လစာပမာဏ"
    override val email = "အီးမေးလ်"
    override val resetPassword = "စကားဝှက် ပြန်လည်သတ်မှတ်ရန်"
    override val data = "ဒေတာ"
    override val exportDataCsv = "ဒေတာများကို CSV ဖြင့် ထုတ်ယူရန်"
    override val preferences = "ဦးစားပေး ဆက်တင်များ"
    override val theme = "အပြင်အဆင်"
    override val light = "အလင်း"
    override val dark = "အမှောင်"
    override val currency = "ငွေကြေး"
    override val language = "ဘာသာစကား"
    override val notifications = "အသိပေးချက်များ"
    override val deleteNotificationTitle = "အသိပေးချက် ဖျက်ရန်"
    override val deleteNotificationConfirmMsg = "ဤအသိပေးချက်ကို ဖျက်ရန် သေချာပါသလား?"
    override val selectAllNotifications = "အသိပေးချက် အားလုံးကို ရွေးရန်"
    override val selectNotifications = "အသိပေးချက်များ ရွေးချယ်ရန်"
    override val noNewNotifications = "အသိပေးချက် အသစ်များ မရှိသေးပါ"
    override val aboutAndInfo = "အက်ပ်အကြောင်းနှင့် အချက်အလက်"
    override val about = "အက်ပ်အကြောင်း"
    override val version = "ဗားရှင်း"
    override val signOut = "အကောင့်ထွက်မည်"
    override val editLanguage = "ဘာသာစကား ပြောင်းလဲရန်"
    override val selectDisplayLanguage = "ပြသလိုသော ဘာသာစကား ရွေးချယ်ပါ"

    // Edit Sub-Screens
    override val editUsernameTitle = "အသုံးပြုသူအမည် ပြင်ဆင်ရန်"
    override val yourUsername = "သင့်အသုံးပြုသူအမည်"
    override val usernameDesc = "ဤအမည်ကို သင့်ပရိုဖိုင်နှင့် အက်ပ်တစ်ခုလုံးတွင် ဖော်ပြပေးမည်ဖြစ်ပါသည်။"
    override val usernameLabel = "အသုံးပြုသူအမည်"
    override val editAgeTitle = "အသက် ပြင်ဆင်ရန်"
    override val yourAge = "သင့်အသက်"
    override val ageDesc = "သင့်အသက်ကို ထည့်သွင်းခြင်းဖြင့် သင့်တော်သော ဘတ်ဂျက်အကြံပြုချက်များကို ကူညီပေးနိုင်ပါသည်။"
    override val ageLabel = "အသက်"
    override val editGenderTitle = "ကျား/မ ပြင်ဆင်ရန်"
    override val selectGender = "ကျား/မ ရွေးချယ်ပါ"
    override val male = "ကျား"
    override val female = "မ"
    override val preferNotToSay = "ဖော်ပြလိုခြင်းမရှိပါ"
    override val editSalaryTitle = "လစာပမာဏ ပြင်ဆင်ရန်"
    override val selectSalaryRange = "သင့်လစာပမာဏ ရွေးချယ်ပါ"
    override val salaryUnder1M = "1,000,000 ကျပ် အောက်"
    override val salary1MTo3M = "1,000,000 - 3,000,000 ကျပ်"
    override val salary3MTo54M = "3,000,000 - 5,400,000 ကျပ်"
    override val salary54MTo10M = "5,400,000 - 10,000,000 ကျပ်"
    override val salaryAbove10M = "10,000,000 ကျပ် အထက်"
    override val editFieldOfWorkTitle = "လုပ်ငန်းနယ်ပယ် ပြင်ဆင်ရန်"
    override val selectFieldOfWork = "သင့်လုပ်ငန်းနယ်ပယ် ရွေးချယ်ပါ"
    override val fieldSoftware = "ဆော့ဖ်ဝဲလ် အင်ဂျင်နီယာ"
    override val fieldHealthcare = "ကျန်းမာရေးစောင့်ရှောက်မှု"
    override val fieldEducation = "ပညာရေး"
    override val fieldFinance = "ဘဏ္ဍာရေး"
    override val fieldMarketing = "စျေးကွက်ရှာဖွေရေး"
    override val fieldDesign = "ဒီဇိုင်း"
    override val fieldSales = "အရောင်း"
    override val fieldBusiness = "စီးပွားရေး"
    override val fieldOther = "အခြား"
    override val editCurrencyTitle = "ငွေကြေး ပြင်ဆင်ရန်"
    override val selectCurrency = "အသုံးပြုမည့် ငွေကြေးရွေးချယ်ပါ"
    override val currencyMMKLabel = "မြန်မာကျပ်ငွေ (MMK)"
    override val currencyUSDLabel = "ဒေါ်လာ ($)"
    override val currencyMixedLabel = "ရောနှောအသုံးပြုမည် (ရင်းနှီးမြှုပ်နှံမှုတွင် $၊ အခြားတွင် MMK)"
    override val editEmailTitle = "အီးမေးလ် ပြင်ဆင်ရန်"
    override val yourEmail = "သင့်အီးမေးလ်"
    override val emailDesc = "အကောင့်ဝင်ရောက်ရန်နှင့် အသိပေးချက်များအတွက် အသုံးပြုပါသည်။"
    override val changePasswordTitle = "စကားဝှက် ပြောင်းလဲရန်"
    override val currentPassword = "လက်ရှိ စကားဝှက်"
    override val newPassword = "စကားဝှက် အသစ်"

    // Export Data Screen
    override val exportDataTitle = "ဒေတာ ထုတ်ယူရန်"
    override val spendingTab = "အသုံးစရိတ်"
    override val savingsTab = "စုငွေ"
    override val investmentsTab = "ရင်းနှီးမြှုပ်နှံမှု"
    override val dateRange = "ရက်စွဲအပိုင်းအခြား"
    override val from = "မှ"
    override val to = "အထိ"
    override val selectDate = "ရက်စွဲရွေးချယ်ပါ"
    override val clearDates = "ရက်စွဲများ ဖျက်မည်"
    override val filterByCategory = "ကဏ္ဍအလိုက် စစ်ထုတ်ရန်"
    override val filterByChallenge = "စိန်ခေါ်မှုအလိုက် စစ်ထုတ်ရန်"
    override val filterByInvestment = "ရင်းနှီးမြှုပ်နှံမှုအလိုက် စစ်ထုတ်ရန်"
    override val leaveUncheckedHint = "အားလုံးထုတ်ယူရန် အမှန်ခြစ်မထားဘဲ ထားခဲ့ပါ။"
    override val searchPlaceholder = "ရှာဖွေရန်..."
    override val exportToCsv = "CSV ဖြင့် ထုတ်ယူမည်"
    override val categoryBillsAndUtilities = "ဘေလ်နှင့် အသုံးစရိတ်များ"
    override val categoryFoodAndDining = "အစားအသောက်"
    override val categoryShopping = "စျေးဝယ်ခြင်း"
    override val categoryTransportation = "သယ်ယူပို့ဆောင်ရေး"
    override val categoryEntertainment = "ဖျော်ဖြေရေး"
    override val categoryEducation = "ပညာရေး"
    override val categoryHealth = "ကျန်းမာရေး"
    override val categoryOther = "အခြား"
    override val calendarFilterAll = "အားလုံး"
    override val calendarFilterAllCategories = "ကဏ္ဍအားလုံး"
    override val dayHeaders = listOf("တနင်္ဂနွေ", "တနင်္လာ", "အင်္ဂါ", "ဗုဒ္ဓဟူး", "ကြာသပတေး", "သောကြာ", "စနေ")
    override val challengeEmergencyFund = "အရေးပေါ် ရန်ပုံငွေ"
    override val challengeVacation = "အားလပ်ရက် ခရီးစဉ်"
    override val monthView = "လ"
    override val yearView = "နှစ်"
    override val highSaver = "စုငွေများ"
    override val lowSaver = "စုငွေနည်း"
    override val highExpense = "သုံးစွဲမှုများ"
    override val lowExpense = "သုံးစွဲမှုနည်း"
    override val highInvestment = "ရင်းနှီးမြှုပ်နှံမှုများ"
    override val lowInvestment = "ရင်းနှီးမြှုပ်နှံမှုနည်း"
    override fun formatMonthYear(month: java.time.YearMonth): String {
        val burmeseMonth = formatMonthName(month)
        return "$burmeseMonth ${month.year}"
    }
    override fun formatMonthName(month: java.time.YearMonth): String {
        return when (month.monthValue) {
            1 -> "ဇန်နဝါရီ"
            2 -> "ဖေဖော်ဝါရီ"
            3 -> "မတ်"
            4 -> "ဧပြီ"
            5 -> "မေ"
            6 -> "ဇွန်"
            7 -> "ဇူလိုင်"
            8 -> "သြဂုတ်"
            9 -> "စက်တင်ဘာ"
            10 -> "အောက်တိုဘာ"
            11 -> "နိုဝင်ဘာ"
            12 -> "ဒီဇင်ဘာ"
            else -> month.month.name
        }
    }
    override fun formatExpenseDateTime(createdAt: Long, dateFallback: String): String {
        return try {
            if (createdAt > 0L) {
                val instant = java.time.Instant.ofEpochMilli(createdAt)
                val zdt = instant.atZone(java.time.ZoneId.systemDefault())
                val day = zdt.dayOfMonth
                val month = formatMonthName(java.time.YearMonth.of(zdt.year, zdt.monthValue))
                val year = zdt.year
                val hour = zdt.hour
                val minute = zdt.minute
                val amPm = if (hour < 12) "AM" else "PM"
                val displayHour = if (hour % 12 == 0) 12 else hour % 12
                val hourStr = String.format("%02d", displayHour)
                val minStr = String.format("%02d", minute)
                "$day $month $year, $hourStr:$minStr $amPm"
            } else if (dateFallback.isNotEmpty()) {
                val parsed = java.time.LocalDate.parse(dateFallback)
                val day = parsed.dayOfMonth
                val month = formatMonthName(java.time.YearMonth.of(parsed.year, parsed.monthValue))
                val year = parsed.year
                "$day $month $year"
            } else {
                "ယနေ့"
            }
        } catch (e: Exception) {
            dateFallback.ifEmpty { "ယနေ့" }
        }
    }

    override fun deleteExpenseConfirmMsg(amountStr: String) = "ပမာဏ $amountStr ရှိသော ဤအသုံးစရိတ်ကို ဖျက်ရန် သေချာပါသလား?"
    override val deleteExpenseConfirmTitle = "အသုံးစရိတ် ဖျက်ရန်"
    override val budgetZeroWarning = "⚠️ လစဉ် စုစုပေါင်း ဘတ်ဂျက်သည် ၀ ဖြစ်နေပါသည်။ ဦးစွာ သတ်မှတ်ပေးပါ။"
    override fun budgetExceedWarning(maxStr: String) = "⚠️ စုစုပေါင်း ဘတ်ဂျက်ထက် မကျော်လွန်နိုင်ပါ။ အများဆုံး ရရှိနိုင်သော ပမာဏ: $maxStr."
    override fun availableCapacityMsg(capStr: String) = "ကျန်ရှိသော စုစုပေါင်း ဘတ်ဂျက် ပမာဏ: $capStr"
    override val categoryNameEmptyError = "ကဏ္ဍအမည် အလွတ်မထားရပါ။"
    override val setGlobalBudgetFirstError = "ကဏ္ဍပစ်မှတ် မသတ်မှတ်မီ လစဉ် စုစုပေါင်း ဘတ်ဂျက်ကို ဦးစွာ သတ်မှတ်ပါ။"
    override val stopChallengeTitle = "စိန်ခေါ်မှု ရပ်တန့်ရန်"
    override fun stopChallengeMsg(amountStr: String) = "သင် စုဆောင်းထားသော ငွေပမာဏ $amountStr ကို သိမ်းဆည်းပေးမည် ဖြစ်ပြီး၊ နောက်ထပ် ငွေထပ်ထည့်၍ ရတော့မည် မဟုတ်ပါ။"
    override fun stoppedChallengeRecorded(amountStr: String) = "ဤစိန်ခေါ်မှုကို ရပ်တန့်လိုက်ပါပြီ။ သင်စုဆောင်းထားသော $amountStr ကို မှတ်တမ်းတင်ထားပါသည်။"
    override val stopChallengeConfirm = "ရပ်တန့်မည်"
    override val stopChallengeKeep = "ဆက်လက်စုမည်"
    override val deleteChallengeTitle = "စိန်ခေါ်မှု ဖျက်ရန်"
    override val deleteChallengeMsg = "ဤစိန်ခေါ်မှုကို ဖျက်ရန် သေချာပါသလား? ဖျက်ပြီးပါက ပြန်လည်ရယူ၍ မရနိုင်ပါ။"
    override val deleteHoldingTitle = "ပိုင်ဆိုင်မှု ဖျက်ရန်"
    override val deleteHoldingMsg = "ဤပိုင်ဆိုင်မှုကို သင့်ရင်းနှီးမြှုပ်နှံမှုစာရင်းမှ ဖျက်ရန် သေချာပါသလား?"
    override val settlementDialogTitle = "ပိုင်ဆိုင်မှု ထုတ်ယူရှင်းတမ်း"
    override val exitMarketPrice = "ရောင်းထုတ်သည့် ဈေးကွက်ပေါက်ဈေး"
    override val confirmSettlement = "ထုတ်ယူမှု အတည်ပြုမည်"
    override val signOutConfirmTitle = "အကောင့်ထွက်ရန်"
    override val signOutConfirmMsg = "အကောင့်မှ ထွက်ရန် သေချာပါသလား?"
    override val passwordResetSent = "စကားဝှက်ပြန်လည်သတ်မှတ်ရန် အီးမေးလ် ပို့ပြီးပါပြီ။"

    // Challenge Screens
    override val allTime = "အချိန်တိုင်း"
    override val yourChallenges = "သင့် စိန်ခေါ်မှုများ"
    override val searchChallenges = "စိန်ခေါ်မှုများ ရှာဖွေရန်..."
    override val noChallengesYet = "စိန်ခေါ်မှု မရှိသေးပါ"
    override val noChallengesDesc = "ပထမဆုံး ငွေစုစိန်ခေါ်မှုကို စတင်ပြီး သင့်စုငွေများ တိုးပွားလာမှုကို ကြည့်ရှုလိုက်ပါ!"
    override val challengeCompletedTitle = "🎉 စိန်ခေါ်မှု အောင်မြင်ပါပြီ! 🎉"
    override val challengeCompletedMsg = "ဂုဏ်ယူပါတယ်! သင်သည် ငွေစုပစ်မှတ်ကို အောင်မြင်စွာ ရောက်ရှိခဲ့ပါပြီ။"
    override val challengeStoppedTitle = "⏸️ စိန်ခေါ်မှု ရပ်တန့်ထားသည်"
    override val cannotDepositStoppedMsg = "ဤစိန်ခေါ်မှုသို့ ငွေထပ်မံထည့်သွင်း၍ မရတော့ပါ။"
    override val challengeFailedTitle = "❌ စိန်ခေါ်မှု မအောင်မြင်ပါ"
    override val challengeFailedMsg = "သတ်မှတ်ရက်အတွင်း ငွေစုပစ်မှတ် မပြည့်မီခဲ့ပါ။"
    override val challengeFailedDetail = "သတ်မှတ်ရက်အတွင်း ငွေစုပစ်မှတ် မပြည့်မီခဲ့ပါ။"
    override val ok = "ကောင်းပါပြီ"
    override val newChallenge = "စိန်ခေါ်မှု အသစ်"
    override val newChallengeDesc = "သင့်ငွေစုခရီးစဉ်ကို စတင်ဖန်တီးပါ"
    override val chooseTemplate = "ပုံစံ ရွေးချယ်ပါ"
    override val yourTemplate = "သင့် ပုံစံ"
    override val templateConstant = "ပုံသေ"
    override val templateFlexi = "စိတ်ကြိုက်"
    override val templateEnvelope = "စာအိတ်"
    override val templateNoSpend = "မသုံးစွဲရ"
    override val emojiLabel = "အီမိုဂျီ"
    override val challengeNameLabel = "စိန်ခေါ်မှု အမည်"
    override val targetAmountLabel = "ပစ်မှတ် ပမာဏ"
    override val durationDaysLabel = "ကြာချိန် (ရက်)"
    override val startChallenge = "စိန်ခေါ်မှု စတင်မည်"
    override val editChallengeTitle = "စိန်ခေါ်မှု ပြင်ဆင်ရန်"
    override val saveChanges = "ပြင်ဆင်မှုများ သိမ်းမည်"
    override val challenge52Week = "52-ပတ် စိန်ခေါ်မှု"
    override val challengeNoSpendWeek = "ငွေမသုံးစွဲသောရက်"
    override val progressMap = "တိုးတက်မှု ပြကွက်"
    override val enterAmount = "ပမာဏ ရိုက်ထည့်ပါ"
    override val habitTracker = "အလေ့အထ မှတ်တမ်း"
    override val zeroSpendDay = "ငွေမသုံးစွဲသောရက်"
    override val fullProgressMap = "တိုးတက်မှု ပြကွက် အပြည့်အစုံ"
    override val skipped = "ကျော်ခဲ့သည်"
    override val missed = "လွတ်သွားသည်"
    override val alreadyCheckedInMsg = "ယနေ့အတွက် စာရင်းသွင်းပြီးပါပြီ။ မနက်ဖြန် ပြန်လာခဲ့ပါ။"
    override val alreadyOpenedEnvelopeMsg = "ယနေ့အတွက် စာအိတ်ဖွင့်ပြီးပါပြီ။ မနက်ဖြန် ပြန်လာခဲ့ပါ။"
    override val challengeNameExists = "စိန်ခေါ်မှု အမည် ရှိပြီးသားဖြစ်သည်"
    override val youSaved = "စုဆောင်းမိသော ပမာဏ"
    override val missedThisDay = "ယနေ့အတွက် လွတ်သွားခဲ့ပါသည်"
    override val challengeEnvelopeDesc = "စာအိတ်တစ်ခုရွေးပြီး ဖော်ပြထားသော ပမာဏကို စုဆောင်းပါ"
    override val challenge7DayDesc = "တစ်ပတ်တာ စုဆောင်းမှု ခရီးစဉ်"
    override val challengeNoSpendDesc = "7 ရက်ကြာ မလိုအပ်သောအသုံးစရိတ်များ လုံးဝမသုံးစွဲရန်"
    override val challengeCustomDesc = "သင့်ပစ်မှတ်ပြည့်မီစေရန် စိတ်ကြိုက်ငွေစုခရီးစဉ်"
    override val challenge100Envelope = "စာအိတ် 100 စိန်ခေါ်မှု"
    override val challenge7DaySprint = "7-ရက် စိန်ခေါ်မှု"
    override val challenge1KADay = "တစ်ရက် 1 ထောင် စိန်ခေါ်မှု"
    override fun daysLeftCount(days: Long) = "$days ရက်ကျန်"
    override fun stepsDoneCount(done: Int, total: Int) = "$done / $total ပြီးစီး"
    override fun daysCompletedCount(done: Int, total: Int) = "$done / $total ရက် ပြီးစီး"
    override fun percentComplete(percent: Int) = "$percent% ပြီးစီး"
    override fun mustBeAtMostDays(days: Int) = "အများဆုံး $days ရက် (လကုန်အထိ) ဖြစ်ရမည်"
    override fun mustBeAtLeast(min: Long) = "အနည်းဆုံး $min ဖြစ်ရမည်"
    override fun mustBeGreaterThan(amount: String) = "$amount ထက် ကြီးရမည်"
    override fun savePerDay(amount: String) = "တစ်ရက်လျှင် $amount စုပါ"
    override fun savedEnvelopeNumber(step: Int) = "စုဆောင်းပြီး စာအိတ် #$step"
    override fun zeroSpendDayTitle(day: Int) = "ငွေမသုံးစွဲသောရက် $day"
    override fun localizeChallengeTitle(title: String): String {
        val trimmed = title.trim()
        val daysLeftRegex = Regex("^(\\d+)\\s+Days?\\s+Left$", RegexOption.IGNORE_CASE)
        val daysMatch = daysLeftRegex.find(trimmed)
        if (daysMatch != null) {
            val days = daysMatch.groupValues[1]
            return "$days ရက်ကျန်"
        }
        return when {
            trimmed.contains("Emergency Fund", ignoreCase = true) -> "💰 " + challengeEmergencyFund
            trimmed.contains("Vacation", ignoreCase = true) -> "🏖️ " + challengeVacation
            trimmed.contains("52-Week", ignoreCase = true) -> "📅 " + challenge52Week
            trimmed.contains("No-Spend", ignoreCase = true) -> "🚫 " + challengeNoSpendWeek
            trimmed.contains("100 Envelope", ignoreCase = true) -> "✉️ " + challenge100Envelope
            trimmed.contains("7-Day Sprint", ignoreCase = true) -> "⚡ " + challenge7DaySprint
            trimmed.contains("1K a Day", ignoreCase = true) -> "🎯 " + challenge1KADay
            else -> trimmed
        }
    }
}

val LocalAppStrings = staticCompositionLocalOf<Strings> { EnglishStrings }

object AppLocale {
    val current: Strings
        @Composable
        @ReadOnlyComposable
        get() = LocalAppStrings.current

    fun getStrings(lang: AppLanguage): Strings = when (lang) {
        AppLanguage.MY -> BurmeseStrings
        AppLanguage.EN -> EnglishStrings
    }
}
