# 👤 Dev 4 — Work Log

> **Role:** Expenses, Budget & Challenges  
> **Branch:** `feature/expense-budget`

---

## 📋 Current Task

| Task | Status | Started | Notes |
|------|--------|---------|-------|
| —    | ⏳ Pending | — | — |

---

## ✅ Completed

| # | Task | Files | Done |
|---|------|-------|------|
| — | —    | —     | —   |

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
| `data/repository/SavingChallengeRepository.kt` | Saving challenge interface |
| `ui/dashboard/DashboardScreen.kt` | Dashboard with calendar heatmap |
| `ui/components/BudgetProgressBar.kt` | Progress bar component |
| `ui/components/SpendingChart.kt` | Chart component |

---

## 🔜 Up Next (8 files)

| File | Task | Difficulty |
|------|------|:----------:|
| `ui/expenses/ExpenseListScreen.kt` | Expense list with search/filter | Medium |
| `ui/expenses/AddExpenseScreen.kt` | Manual expense form | Medium |
| `ui/expenses/ExpenseViewModel.kt` | Expense state management | Medium |
| `ui/budget/BudgetScreen.kt` | Budget limit setting | Easy |
| `ui/budget/BudgetViewModel.kt` | Budget state | Easy |
| `ui/challenges/ChallengesScreen.kt` | Saving challenges list | Medium |
| `ui/challenges/ChallengeViewModel.kt` | Challenges state | Easy |
| `ui/dashboard/CalendarHeatmap.kt` | Enhanced calendar with click + filters | Medium |

---

## 📅 Calendar Heatmap Requirements

### Click on a Day
When user clicks a day (e.g., July 1st):
```
┌─────────────────────────────────┐
│  📅 July 1, 2026                │
├─────────────────────────────────┤
│  💰 Saving:    10,000 MMK       │
│  💸 Expense:    5,000 MMK       │
│  📊 Net:        5,000 MMK       │
└─────────────────────────────────┘
```

### Color Rating
| Daily Spending vs Budget | Color |
|--------------------------|-------|
| < 50% | 🟢 Green |
| 50-80% | 🟡 Yellow |
| 80-100% | 🟠 Orange |
| > 100% | 🔴 Red |

### Calendar Filters
| Filter | Shows |
|--------|-------|
| All | Combined view |
| Budget | Budget progress only |
| Expenses | Expenses only |
| Savings | Savings only |

---

## 💰 Expense Categories
```kotlin
enum class ExpenseCategory(val displayName: String) {
    FOOD("Food & Drinks"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    BILLS("Bills & Utilities"),
    ENTERTAINMENT("Entertainment"),
    EDUCATION("Education"),
    HEALTH("Health"),
    OTHER("Other")
}
```

---

## 📦 Depends On Dev 1
- ✅ All data models (Expense, Budget, SavingChallenge)
- ✅ Repository interfaces
- ✅ Dashboard with calendar heatmap
- ✅ Reusable components (BudgetProgressBar, SpendingChart)

---

## 🔗 Interfaces You Code Against
```kotlin
interface ExpenseRepository {
    fun getExpensesForMonth(userId: String, yearMonth: String): Flow<List<Expense>>
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
> **Note:** Dev 3 implements real Firestore later. Your code works against the interface + in-memory mock.

---

## 📝 Scratch Notes
```
```
