# 👤 Dev 3 — Work Log

> **Role:** Data Layer — Auth + Firestore + Repositories  
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

## 🔜 Up Next

- [ ] `data/firestore/FirestoreModule.kt` — Firestore DI provider
- [ ] `data/firestore/FirestorePaths.kt` — Collection/document paths
- [ ] Implement `ExpenseRepository` (real Firestore)
- [ ] Implement `BudgetRepository` (real Firestore)
- [ ] Implement `ChatRepository` (real Firestore)
- [ ] Implement `SavingChallengeRepository` (real Firestore)
- [ ] Implement `AuthRepository` (real Firebase Auth)
- [ ] `di/AppModule.kt` — Hilt app-wide bindings
- [ ] `di/RepositoryModule.kt` — Swap mocks → real implementations

---

## 📦 Depends On Dev 1
- ✅ All 6 data models (`Expense`, `Budget`, `SavingChallenge`, `SavingsDeposit`, `SavingsAnalytics`, `ChatMessage`)
- ✅ All 5 repository interfaces (you implement these)

## 🔧 Your Job: Replace Mocks with Real Firestore

Dev 1 shipped in-memory mocks. You swap them with real implementations:

| Interface | Mock (remove) | Real (you build) |
|-----------|--------------|-----------------|
| `ExpenseRepository` | `MockExpenseRepository` | Real Firestore CRUD + `SnapshotListener` |
| `BudgetRepository` | `MockBudgetRepository` | Real Firestore + spending computation |
| `ChatRepository` | `MockChatRepository` | Real Firestore chat history |
| `SavingChallengeRepository` | `MockSavingChallengeRepository` | Real Firestore challenges + deposits |
| `AuthRepository` | `MockAuthRepository` | Real Firebase Auth (Google + Email) |

**To swap:** Edit `RepositoryModule.kt` → change `@Binds` targets from `Mock*` to real implementations.

### Firestore Paths
```
users/{userId}/expenses/{expenseId}
users/{userId}/budgets/{YYYY-MM}
users/{userId}/chat/{messageId}
users/{userId}/challenges/{challengeId}
users/{userId}/challenges/{challengeId}/deposits/{depositId}
```

---

## 📝 Scratch Notes
```
```
