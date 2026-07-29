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

## 🔜 Up Next (6 files)

| File | Task | Difficulty |
|------|------|:----------:|
| `ui/expenses/ExpenseScreen.kt` | Budget & Expense Hub (combined) | Hard |
| `ui/expenses/AddExpenseScreen.kt` | Manual expense form | Medium |
| `ui/expenses/ExpenseViewModel.kt` | Expense + budget state | Medium |
| `ui/challenges/ChallengesScreen.kt` | Challenge cards + detail view | Hard |
| `ui/challenges/ChallengeViewModel.kt` | Challenges state | Medium |
| `ui/dashboard/CalendarHeatmap.kt` | Enhanced calendar with click + filters | Medium |

---

## 💰 Budget & Expense Hub (Expense Tab)

### Screen Layout

```
┌─────────────────────────────────────────┐
│  BUDGET & EXPENSE HUB                   │
├─────────────────────────────────────────┤
│  🎯 Monthly Overall Budget    [Edit ⚙️] │
│  $1,850 Spent  /  $3,000 Target        │
│  =========================>  61% Used   │
│  Remaining: $1,150  |  12 Days Left     │
├─────────────────────────────────────────┤
│  📁 CATEGORIES            [+ New]       │
│                                         │
│  🍔 Food & Dining                       │
│  ================>  $320 / $600 target  │
│                                         │
│  🚗 Transportation                      │
│  ======>  $110 / $300 target            │
├─────────────────────────────────────────┤
│  📋 RECENT EXPENSES                     │
│  (Tap category above to filter)         │
│                                         │
│  ☕ Starbucks         -$4.50  Today     │
│  🛒 Target Store     -$68.20  Yesterday │
│  ⛽ Shell Gas        -$45.00  Jul 26   │
└─────────────────────────────────────────┘
```

### Section 1: Monthly Overall Budget

| Element | Description |
|---------|-------------|
| **Title** | "Monthly Overall Budget" |
| **Spent / Target** | "$1,850 / $3,000" |
| **Progress Bar** | Green (<50%), Yellow (50-80%), Orange (80-100%), Red (>100%) |
| **Percentage** | "61% Used" or "108%" when over |
| **Remaining** | "$1,150 left" or "-$250" when over |
| **Days Left** | "12 Days Left" in month |
| **Edit Button** | Opens budget editor |

### Over Budget Alerts

| Alert | Trigger | UI |
|-------|---------|-----|
| **Warning** | 75% spent | 🟡 Yellow banner |
| **Critical** | 90% spent | 🟠 Orange banner |
| **Over Budget** | 100%+ spent | 🔴 Red banner + push notification |

**Over Budget Example:**
```
┌─────────────────────────────────────────┐
│  🎯 Monthly Overall Budget    [Edit ⚙️] │
│  $3,250 Spent  /  $3,000 Target        │
│  ==========================> 🔴 108%    │
│  Remaining: -$250  |  5 Days Left       │
└─────────────────────────────────────────┘
```

**Push Notification:**
```
⚠️ Saving Coach — Budget Alert
You've exceeded your monthly budget by $250!
```

### Section 2: Categories

| Element | Description |
|---------|-------------|
| **Header** | "📁 CATEGORIES" + "+ New" button |
| **Category Card** | Emoji + Name + Progress bar + Spent / Target |
| **Tap Category** | Filters recent expenses below |
| **"+ New"** | Add custom category |

**Default Categories:**
| Emoji | Category | Default Target |
|-------|----------|----------------|
| 🍔 | Food & Dining | $600 |
| 🚗 | Transportation | $300 |
| 🛍️ | Shopping | $400 |
| 📱 | Bills & Utilities | $200 |
| 🎬 | Entertainment | $200 |
| 📚 | Education | $150 |
| 💊 | Health | $150 |
| 📦 | Other | $200 |

### Section 3: Recent Expenses

| Element | Description |
|---------|-------------|
| **Header** | "📋 RECENT EXPENSES" |
| **Filter Note** | "Tap any category above to filter" |
| **Expense Item** | Icon + Merchant + Amount + Date |
| **Tap Expense** | Opens edit screen |
| **Swipe Left** | Delete with confirmation |

---

## 🎯 Challenges Feature — Detailed Requirements

### User Flow
```
Challenges Tab → Challenge Cards → Click Card → Detail View
```

### Screen Layout

```
┌─────────────────────────────────────────┐
│  SAVING CHALLENGES                      │
├─────────────────────────────────────────┤
│  💰 Total Saved: $1,250                 │
│  📊 3 Active  |  1 Completed            │
├─────────────────────────────────────────┤
│  🎯 Challenge Cards                     │
│                                         │
│  ┌─────────┐  ┌─────────┐              │
│  │ $1/Day  │  │ 7-Day   │              │
│  │ $12/$30 │  │ $45/$100│              │
│  │ ████░░░ │  │ █████░░ │              │
│  └─────────┘  └─────────┘              │
│                                         │
│  ┌─────────┐  ┌─────────┐              │
│  │100 Env. │  │ + New   │              │
│  │$2k/$5k  │  │ Create  │              │
│  │████░░░░ │  │ Challenge│              │
│  └─────────┘  └─────────┘              │
└─────────────────────────────────────────┘
```

**Summary Header:**
- 💰 **Total Saved:** Sum of all deposits across all challenges
- 📊 **Active:** Number of active challenges
- ✅ **Completed:** Number of completed challenges

**Challenge Cards View:**
- Grid/list of challenge cards
- Preset cards (4 built-in)
- Custom cards (user-created)
- "+ Create" card → opens wizard

**Card Content:**
- Emoji/Icon
- Challenge name
- Progress: deposited / target
- Progress bar

**Detail View (When card clicked):**
- Challenge-specific UI (dot grid, envelope grid, timeline, etc.)
- Deposit history list
- Add deposit button
- Settings/edit button
- Delete button

---

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
