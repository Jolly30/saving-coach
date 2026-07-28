# 👤 Dev 3 — Work Log

> **Role:** Data Layer — Firestore Repositories  
> **Branch:** `feature/data-layer`

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
| `data/repository/AuthRepository.kt` | Auth interface |
| `data/repository/FirebaseAuthRepository.kt` | Real Firebase Auth (Google + Email) |
| `data/repository/ChatRepository.kt` | Chat interface |
| `ai/AiChatRepository.kt` | Chat impl with proxy + Firestore |
| `di/AppModule.kt` | Hilt providers (FirebaseAuth, FirebaseFirestore, OkHttp, proxyUrl) |
| `di/RepositoryModule.kt` | Repo bindings (currently uses mocks) |

---

## 🔜 Up Next (3 files)

| File | Task | Difficulty |
|------|------|:----------:|
| `FirebaseExpenseRepository.kt` | CRUD + real-time sync for expenses | Medium |
| `FirebaseBudgetRepository.kt` | Budget limits + spending totals | Easy |
| `FirebaseSavingChallengeRepository.kt` | Challenges + deposits CRUD | Easy |

---

## 📦 Depends On Dev 1
- ✅ All 6 data models (Expense, Budget, SavingChallenge, etc.)
- ✅ All repository interfaces (you implement these)
- ✅ Auth already done (FirebaseAuthRepository)
- ✅ Chat already done (AiChatRepository)
- ✅ DI setup done (AppModule + FirebaseFirestore provider)

---

## 🔧 Your Job: Replace Mocks with Real Firestore

| Interface | Mock (remove) | Real (you build) |
|-----------|--------------|-----------------|
| `ExpenseRepository` | `MockExpenseRepository` | Firestore CRUD + `SnapshotListener` |
| `BudgetRepository` | `MockBudgetRepository` | Firestore + spending computation |
| `SavingChallengeRepository` | `MockSavingChallengeRepository` | Firestore challenges + deposits |

> **Note:** `AuthRepository` and `ChatRepository` are already done by Dev 1. Don't touch them.

---

## 🗂️ Firestore Paths

```
users/{userId}/expenses/{expenseId}
users/{userId}/budgets/{YYYY-MM}
users/{userId}/challenges/{challengeId}
users/{userId}/challenges/{challengeId}/deposits/{depositId}
```

---

## 🔗 How to Swap Mocks → Real Repos

```kotlin
// In RepositoryModule.kt, change:
@Binds abstract fun bindExpenseRepository(impl: MockExpenseRepository): ExpenseRepository
// To:
@Binds abstract fun bindExpenseRepository(impl: FirebaseExpenseRepository): ExpenseRepository
```

---

## 📝 Scratch Notes
```
```
