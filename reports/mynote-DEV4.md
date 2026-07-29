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
| `ui/challenges/ChallengesScreen.kt` | Challenge cards + wizard | Hard |
| `ui/challenges/ChallengeViewModel.kt` | Challenges state | Medium |
| `ui/dashboard/CalendarHeatmap.kt` | Enhanced calendar with click + filters | Medium |

---

## 🎯 Challenges Feature — Detailed Requirements

### 1. PRESET CHALLENGES

#### $1 a Day for 1 Month (Constant Saving)
- **Goal:** Deposit $1 every day for 30/31 days ($30-$31 target)
- **UI:** 30-day calendar/dot grid tracker
- **Interaction:** Tap dot → marks $1 deposited → turns emerald green

#### 7-Day Flexible Weekly Sprint
- **Goal:** 1-week timeline to deposit whatever extra cash
- **UI:** 7-day progress timeline + "+ Log Any Deposit" button
- **Features:** Daily logged breakdown

#### 100 Envelope Challenge ($5,050 Target)
- **Goal:** Complete 100 envelopes
- **UI:** Interactive 10x10 envelope tile grid (numbered 1-100)
- **Interaction:** "Draw Random Envelope" button → card flip animation → logs deposit → highlights tile

#### No-Spend Week Challenge (7-Day Sprint)
- **Goal:** Resist impulse spending for 7 days
- **UI:** 7-day countdown tracker widget
- **Features:** "Log Resisted Impulse" button → type money saved → adds to "Total Money Defended" counter

---

### 2. CUSTOM CHALLENGE WIZARD (`/create-custom-challenge`)

#### Step 1: Goal Details
| Field | Type | Example |
|-------|------|---------|
| Goal Name | Text | "New Laptop Fund" |
| Target Amount | Number | "$1,200" |
| Emoji/Icon | Picker | 💻 ✈️ 🚗 🏡 🛡️ |

#### Step 2: Deposit Style
| Style | Input | Calculation |
|-------|-------|-------------|
| **Envelope / Tile Grid** | Number of tiles (e.g., 50) | Target ÷ tiles = amount per tile |
| **Constant / Fixed** | Amount + frequency (e.g., $100/2 weeks) | Fixed schedule |
| **Flexible** | None | Ad-hoc deposits anytime |

#### Step 3: Duration & Timeline
| Option | Example |
|--------|---------|
| 1 Week | "Deposit $7/day to hit $50" |
| 1 Month | "Deposit $10/day to hit $300" |
| 3 Months | "Deposit $100/week to hit $1,200" |
| 6 Months | "Deposit $50/week to hit $1,200" |
| 1 Year | "Complete ~1 envelope/week ($100 each)" |
| Custom Date | Pick specific date |

**Dynamic Pace Projection:**
- "To hit $1,200 in 6 months using Constant Saving, deposit $50/week."
- "To hit $5,000 using 50 Envelopes in 1 year, complete ~1 envelope per week ($100 each)."

---

### 3. DEPOSIT TRACKING

| Feature | Description |
|---------|-------------|
| **Add Deposit** | Log amount to any challenge |
| **Deposit History** | View all deposits per challenge |
| **Progress Bar** | Deposited / Target with percentage |
| **Auto-Complete** | Mark as done when target reached |
| **Undo Last** | Remove last deposit if mistake |

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
