# 👤 Dev 3 — Work Log

> **Role:** Saving Challenges  
> **Branch:** `feature/saving-challenges`

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
| `data/model/SavingChallenge.kt` | Challenge data model |
| `data/model/SavingsDeposit.kt` | Deposit data model |
| `data/model/SavingsAnalytics.kt` | Analytics data model |
| `data/repository/SavingChallengeRepository.kt` | Repository interface |
| `data/mock/MockRepositories.kt` | In-memory mock for SavingChallengeRepository |
| `navigation/Routes.kt` | Route definitions (including `challenges`) |
| `ui/theme/Theme.kt` | Material 3 theme |
| `ui/components/BudgetProgressBar.kt` | Reusable progress bar component |

---

## 🔜 Up Next (5 files)

| File | Task | Difficulty |
|------|------|:----------:|
| `ui/challenges/ChallengesScreen.kt` | Challenge cards grid + total saved summary + create button | Medium |
| `ui/challenges/ChallengeDetailScreen.kt` | Challenge-specific UI + deposit history + add deposit | Medium |
| `ui/challenges/ChallengeViewModel.kt` | State management for challenges + deposits | Medium |
| `ui/challenges/CreateChallengeScreen.kt` | Wizard to create custom challenge | Easy |
| `ui/components/ChallengeCard.kt` | Reusable card with emoji, name, progress bar | Easy |

---

## 🎯 Screen Layout

### Challenges Screen
```
┌─────────────────────────────────────────┐
│  SAVING CHALLENGES                      │
├─────────────────────────────────────────┤
│  💰 Total Saved: 1,250,000 MMK          │
│  📊 3 Active  |  1 Completed            │
├─────────────────────────────────────────┤
│  🎯 Challenge Cards                     │
│                                         │
│  ┌─────────┐  ┌─────────┐              │
│  │1K/Day   │  │ 7-Day   │              │
│  │12k/30k  │  │ 45k/100k│              │
│  │ ████░░░ │  │ █████░░ │              │
│  └─────────┘  └─────────┘              │
│                                         │
│  ┌─────────┐  ┌─────────┐              │
│  │100 Env. │  │ + New   │              │
│  │2M/5M    │  │ Create  │              │
│  │████░░░░ │  │ Challenge│              │
│  └─────────┘  └─────────┘              │
└─────────────────────────────────────────┘
```

### Challenge Types

| Type | UI | Description |
|------|-----|-------------|
| **1K a Day** | Dot grid | Tap one dot per day, 30 dots = done |
| **7-Day Sprint** | Progress ring | Deposit target in 7 days |
| **100 Envelope** | Envelope grid | 100 envelopes, fill each with fixed amount |
| **No-Spend Week** | Checklist | 7 days, check each no-spend day |
| **Custom** | Progress bar | User-defined target and duration |

### Preset Challenges

| Name | Emoji | Target | Duration |
|------|-------|--------|----------|
| 1K a Day | 🎯 | 30,000 MMK | 30 days |
| 7-Day Sprint | ⚡ | 100,000 MMK | 7 days |
| 100 Envelope | ✉️ | 5,000,000 MMK | Flexible |
| No-Spend Week | 🚫 | 0 MMK (save your daily budget) | 7 days |

### Detail View (When card clicked)
- Challenge-specific visual UI (dot grid, envelope grid, timeline, etc.)
- Deposit history list
- Add deposit button
- Settings/edit button
- Delete button with confirmation

---

## 🎨 Challenge Card Layout

```
┌─────────────────────────┐
│  🎯  1K a Day           │
│                         │
│  12,000 / 30,000 MMK    │
│  ██████████░░░░░░  40%  │
│                         │
│  18 days left           │
└─────────────────────────┘
```

---

## 🔔 Deposit Tracking

| Feature | Description |
|---------|-------------|
| **Add Deposit** | Log amount to any challenge |
| **Deposit History** | View all deposits per challenge |
| **Progress Bar** | Deposited / Target with percentage |
| **Auto-Complete** | Mark as done when target reached |
| **Undo Last** | Remove last deposit if mistake |

---

## 📦 Depends On Dev 1
- ✅ All 3 data models (SavingChallenge, SavingsDeposit, SavingsAnalytics)
- ✅ SavingChallengeRepository interface
- ✅ MockSavingChallengeRepository (in-memory mock)
- ✅ Auth already done (FirebaseAuthRepository)
- ✅ DI setup done (AppModule + RepositoryModule)

---

## 🔗 Interface You Code Against

```kotlin
interface SavingChallengeRepository {
    fun getActiveChallenges(userId: String): Flow<List<SavingChallenge>>
    fun getAllChallenges(userId: String): Flow<List<SavingChallenge>>
    fun getDeposits(userId: String, challengeId: String): Flow<List<SavingsDeposit>>
    suspend fun createChallenge(challenge: SavingChallenge): String
    suspend fun addDeposit(userId: String, challengeId: String, deposit: SavingsDeposit)
    suspend fun completeChallenge(userId: String, challengeId: String)
    suspend fun deleteChallenge(userId: String, challengeId: String)
}
```

> **Note:** Dev 1 implements real Firestore later. Your code works against the interface + in-memory mock.

---

## 📝 Scratch Notes
```
```
