# 👤 Dev 5 — Work Log

> **Role:** Export, Settings + Release  
> **Branch:** `feature/export-settings-ci`

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

- [ ] `export/CsvExporter.kt` — CSV generation from expenses
- [ ] `export/ShareManager.kt` — Android share + email intent
- [ ] `ui/settings/SettingsScreen.kt` — Profile + export + about
- [ ] `ui/settings/SettingsViewModel.kt` — Settings state

### 📋 CSV Export Columns
```
Date, Category, Merchant, Amount, Currency, Notes
```

### 📋 Settings Sections
- Profile card (name, email, photo from Google Auth)
- Export data button → CSV
- Sign out → navigate to Auth
- App version + About

---

## 📦 Depends On Dev 1
- ✅ `Expense.kt` (data model)
- ✅ `ExpenseRepository` interface
- ✅ Navigation routes + MainActivity scaffold
- ✅ Theme + reusable components

## 🔗 Interfaces You Code Against
```kotlin
interface ExpenseRepository {
    fun getAllExpenses(userId: String): Flow<List<Expense>>
    // Read-only for export — you mostly need this one method
}
```
> **Note:** Dev 3 implements real Firestore later. Your export uses `ExpenseRepository` interface.

---

## 📝 Scratch Notes
```
```
