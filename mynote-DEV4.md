# 👤 Dev 4 — Work Log

> **Role:** Expense Forms + Budget Settings + Savings Screens  
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

## 🔜 Up Next

- [ ] `ui/expenses/ExpenseListScreen.kt` — List with search/filter/swipe-delete
- [ ] `ui/expenses/AddExpenseScreen.kt` — Manual add form
- [ ] `ui/expenses/ExpenseViewModel.kt` — Expense list + add state
- [ ] `ui/budget/BudgetScreen.kt` — Budget limit settings
- [ ] `ui/budget/BudgetViewModel.kt` — Budget state

### 📋 Expense Categories
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
- ✅ `Expense.kt` (data model)
- ✅ `Budget.kt` (data model)
- ✅ `SavingChallenge.kt` + `SavingsDeposit.kt` (data models)
- ✅ `ExpenseRepository` interface
- ✅ `BudgetRepository` interface
- ✅ `SavingChallengeRepository` interface
- ✅ `DashboardScreen.kt` (your screens navigate to/from here)
- ✅ `BudgetProgressBar`, `SpendingChart`, `LoadingOverlay` (reuse these!)

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
