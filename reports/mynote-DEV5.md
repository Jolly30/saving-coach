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

## ✅ Already Built by Dev 1

| File | Purpose |
|------|---------|
| `proxy/` | Vercel serverless proxy for Gemini API |
| `.github/workflows/` | CI/CD pipelines |
| `app/proguard-rules.pro` | ProGuard rules |
| `app/build.gradle.kts` | Signing config (release) |

---

## 🔜 Up Next (4 files)

| File | Task | Difficulty |
|------|------|:----------:|
| `export/CsvExporter.kt` | Generate CSV from expenses | Easy |
| `export/ShareManager.kt` | Android share/email intent | Easy |
| `ui/settings/SettingsScreen.kt` | Profile + export + sign out | Easy |
| `ui/settings/SettingsViewModel.kt` | Settings state | Easy |

---

## 📋 CSV Export Columns
```
Date, Category, Merchant, Amount, Currency, Notes
```

---

## 📋 Settings Sections
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
- ✅ CI/CD pipelines ready
- ✅ Signing config ready

---

## 🔗 Integration

- CSV export uses `ExpenseRepository` from Dev 3
- Profile data from `FirebaseAuthRepository` (Dev 1)
- Sign out calls `AuthRepository.signOut()`

---

## 🔗 If Proxy URL Changes

```bash
# Update local.defaults.properties
proxy.url=https://new-vercel-url.vercel.app
```

---

## 📝 Scratch Notes
```
```
