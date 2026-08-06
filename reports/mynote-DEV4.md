# 👤 Dev 4 — Work Log

> **Role:** Budget & Expense Hub  
> **Branch:** `feature/expense-budget`

---

## 📋 Current Task

| Task | Status | Started | Notes |
|------|--------|---------|-------|
| Budget & Expense Hub | ✅ Completed | 2026-08-04 | All UI components, ViewModel, State, and Navigation wired |

---

## ✅ Completed

| # | Task | Files | Done |
|---|------|-------|------|
| 1 | Create ExpenseUiState & ExpenseCategory | `ui/expenses/ExpenseUiState.kt` | ✅ |
| 2 | Implement ExpenseViewModel state & logic | `ui/expenses/ExpenseViewModel.kt` | ✅ |
| 3 | Build LogExpenseBottomSheet | `ui/expenses/LogExpenseBottomSheet.kt` | ✅ |
| 4 | Build AddExpenseScreen | `ui/expenses/AddExpenseScreen.kt` | ✅ |
| 5 | Build ExpenseScreen (Budget Hub UI) | `ui/expenses/ExpenseScreen.kt` | ✅ |
| 6 | Wire up Navigation | `navigation/NavGraph.kt` | ✅ |
| 7 | Custom Category in Log Expense | `ui/expenses/LogExpenseBottomSheet.kt`, `AddExpenseScreen.kt` | ✅ |

---

## ❌ Not Done / Blocked

| # | Task | Blocked By | Why |
|---|------|------------|-----|
| — | —    | —          | —   |

---

## 🚧 In Progress

| # | Task | Started | Notes |
|---|------|---------|-------|
| — | —    | —       | —     |

---

## ✅ Already Built by Dev 1

| File | Purpose |
|------|---------|
| `data/repository/ExpenseRepository.kt` | Expense interface |
| `data/repository/BudgetRepository.kt` | Budget interface |
| `ui/dashboard/DashboardScreen.kt` | Dashboard with calendar heatmap |
| `ui/components/BudgetProgressBar.kt` | Progress bar component |
| `ui/components/SpendingChart.kt` | Chart component |

---

## 🔜 Up Next (4 files)

| File | Task | Difficulty |
|------|------|:----------:|
| `ui/expenses/ExpenseScreen.kt` | Budget & Expense Hub (combined) | Hard |
| `ui/expenses/LogExpenseBottomSheet.kt` | Bottom sheet modal for logging expenses | Medium |
| `ui/expenses/AddExpenseScreen.kt` | Manual expense form (full screen) | Medium |
| `ui/expenses/ExpenseViewModel.kt` | Expense + budget + category state | Medium |

---

## 💰 Budget & Expense Hub (Expense Tab)

### Screen Layout

```
┌──────────────────────────────────────────────────────────────────┐
│                     BUDGET & EXPENSE HUB                         │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ 🎯 Monthly Overall Budget                 [ Edit Budget ⚙️ ] │  │
│  │                                                            │  │
│  │ 1,850 MMK Spent  /  3,000 MMK Target                      │  │
│  │ ==========================>................   61% Used     │  │
│  │                                                            │  │
│  │ Remaining: 1,150 MMK  |  12 Days Left                      │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│            ┌────────────────────────────────────────┐            │
│            │        ➕ LOG NEW EXPENSE               │            │
│            └────────────────────────────────────────┘            │
│                                                                  │
│  🏷️ CATEGORIES                                [ + New Category ] │
│                                                                  │
│  🍔 Food & Dining                                                │
│  =======================>...................  320 / 600 MMK      │
│                                                                  │
│  🚗 Transportation                                              │
│  =========>.................................  110 / 300 MMK      │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ 🧾 RECENT EXPENSES                                               │
│ (Tap any category above to filter this list)                     │
│                                                                  │
│  ☕ Starbucks                          -4,500 MMK  │ Today       │
│  🛒 Target Store                     -68,200 MMK  │ Yesterday   │
│  ⛽ Shell Gas Station               -45,000 MMK  │ Jul 26      │
└──────────────────────────────────────────────────────────────────┘
```

### Section 1: Monthly Overall Budget

| Element | Description |
|---------|-------------|
| **Title** | "Monthly Overall Budget" |
| **Edit Button** | ⚙️ icon → opens budget editor |
| **Spent / Target** | "1,850 MMK Spent / 3,000 MMK Target" |
| **Progress Bar** | Reuse `BudgetProgressBar` component. Color: Green (<50%), Yellow (50-80%), Orange (80-100%), Red (>100%) |
| **Percentage** | "61% Used" |
| **Remaining** | "Remaining: 1,150 MMK" (goes negative if over budget) |
| **Days Left** | "12 Days Left" — calculated from current day to end of month |

### Over Budget Alerts

| Alert | Trigger | UI |
|-------|---------|-----|
| **Warning** | 75% spent | 🟡 Yellow banner |
| **Critical** | 90% spent | 🟠 Orange banner |
| **Over Budget** | 100%+ spent | 🔴 Red banner + push notification |

**Over Budget Example:**
```
┌────────────────────────────────────────────────────────────┐
│ 🎯 Monthly Overall Budget                 [ Edit Budget ⚙️ ] │
│ 3,250 MMK Spent  /  3,000 MMK Target                      │
│ ==========================> 🔴 108%                        │
│ Remaining: -250 MMK  |  5 Days Left                       │
└────────────────────────────────────────────────────────────┘
```

**Push Notification:**
```
⚠️ Saving Coach — Budget Alert
You've exceeded your monthly budget by 250,000 MMK!
```

### Section 2: Log New Expense CTA

| Element | Description |
|---------|-------------|
| **Button** | Centered, primary filled button |
| **Text** | "➕ LOG NEW EXPENSE" |
| **Action** | Opens `LogExpenseBottomSheet` |

### Section 3: Categories

| Element | Description |
|---------|-------------|
| **Header** | "🏷️ CATEGORIES" + "[ + New Category ]" button on right |
| **Category Card** | Emoji + Category Name + Progress bar + "Spent / Target MMK" |
| **Tap Category** | Filters the Recent Expenses list below to that category |
| **"+ New"** | Opens dialog to add custom category with name, emoji, and target |

**Default Categories:**

| Emoji | Category | Default Target |
|-------|----------|----------------|
| 🍔 | Food & Dining | 600,000 MMK |
| 🚗 | Transportation | 300,000 MMK |
| 🛍️ | Shopping | 400,000 MMK |
| 📱 | Bills & Utilities | 200,000 MMK |
| 🎬 | Entertainment | 200,000 MMK |
| 📚 | Education | 150,000 MMK |
| 💊 | Health | 150,000 MMK |
| 📦 | Other | 200,000 MMK |

### Section 4: Recent Expenses

| Element | Description |
|---------|-------------|
| **Header** | "🧾 RECENT EXPENSES" |
| **Filter Note** | "Tap any category above to filter this list" |
| **Expense Item** | Category Emoji + Merchant Name + Amount (negative) + Date |
| **Date Format** | "Today" / "Yesterday" / "Jul 26" for older |
| **Tap Expense** | Opens edit screen |
| **Swipe Left** | Delete with confirmation dialog |
| **Empty State** | "No expenses yet. Tap + to log your first expense!" |

### Log Expense Bottom Sheet

```
┌──────────────────────────────────────────────────────────────────┐
│ ➕ Log Expense                           [✕ Close]              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Amount (MMK):                                                   │
│  [ 0                                                         ]  │
│                                                                  │
│  Select Category (Required):                                     │
│  [ 🍔 Food ]  [ 🚗 Trans. ]  [ 🛒 Groceries ]  [ 🎬 Fun ]      │
│                                                                  │
│  Note / Merchant (Optional):                                     │
│  [ e.g., Starbucks Coffee                                      ]│
│                                                                  │
│  [ Save Expense ]                                                │
└──────────────────────────────────────────────────────────────────┘
```

| Element | Description |
|---------|-------------|
| **Header** | "➕ Log Expense" with ✕ close button |
| **Amount Input** | Numeric keyboard, MMK currency, required |
| **Category Selection** | Horizontal scrollable chips with emoji + short name, required |
| **Note/Merchant** | Optional text field with placeholder |
| **Save Button** | Validates amount + category → saves → closes sheet → refreshes list |
| **Validation** | Amount must be > 0, category must be selected |

---

## 📊 ExpenseViewModel State

```kotlin
data class ExpenseUiState(
    // Monthly budget
    val monthlyBudget: Budget? = null,
    val totalSpent: Double = 0.0,
    val daysLeftInMonth: Int = 0,

    // Categories
    val categories: List<ExpenseCategory> = emptyList(),
    val categorySpending: Map<String, Double> = emptyMap(),
    val selectedCategoryFilter: String? = null,  // null = show all

    // Recent expenses
    val recentExpenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),

    // Log expense bottom sheet
    val showLogSheet: Boolean = false,
    val logAmount: String = "",
    val logCategory: String = "",
    val logMerchant: String = "",

    // Loading & error
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ExpenseCategory(
    val emoji: String,
    val name: String,
    val target: Double,     // category budget limit in MMK
    val spent: Double       // computed from expenses
)
```

---

## 🔄 Interaction Flow

```
Expense Tab Loaded
    │
    ├── Fetch monthly budget from BudgetRepository
    ├── Fetch expenses for current month from ExpenseRepository
    ├── Compute category spending totals
    │
    ├── Tap "LOG NEW EXPENSE" → Opens bottom sheet
    │       ├── Enter amount
    │       ├── Select category
    │       ├── Enter note (optional)
    │       └── Tap "Save" → addExpense() → refresh list
    │
    ├── Tap category → Sets selectedCategoryFilter
    │       └── Filters recentExpenses to that category
    │
    ├── Tap "All" category → Clears filter → shows all
    │
    ├── Tap expense → Opens edit screen
    │
    └── Swipe expense left → Delete with confirmation
```

---

## 📦 Depends On Dev 1
- ✅ All data models (Expense, Budget)
- ✅ Repository interfaces (ExpenseRepository, BudgetRepository)
- ✅ Reusable components (BudgetProgressBar, SpendingChart)
- ✅ Auth already done (FirebaseAuthRepository)

---

## 🔗 Interfaces You Code Against

```kotlin
interface ExpenseRepository {
    fun getExpensesForMonth(userId: String, yearMonth: String): Flow<List<Expense>>
    fun getExpensesForDate(userId: String, date: String): Flow<List<Expense>>
    fun getAllExpenses(userId: String): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): String
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)
}

interface BudgetRepository {
    fun getBudget(userId: String, yearMonth: String): Flow<Budget?>
    suspend fun setBudget(userId: String, budget: Budget)
    suspend fun updateLimit(userId: String, yearMonth: String, newLimit: Double)
}
```

> **Note:** Dev 1 implements real Firestore repos. Your code works against the interfaces + in-memory mocks.

---

## 💰 Expense Categories

```kotlin
enum class ExpenseCategory(val displayName: String) {
    FOOD("Food & Dining"),
    TRANSPORT("Transportation"),
    SHOPPING("Shopping"),
    BILLS("Bills & Utilities"),
    ENTERTAINMENT("Entertainment"),
    EDUCATION("Education"),
    HEALTH("Health"),
    OTHER("Other")
}
```

---

## 📝 Scratch Notes
```
```
