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
| `proxy/` | Vercel serverless proxy |
| `.github/workflows/` | CI/CD pipelines |
| `app/proguard-rules.pro` | ProGuard rules |
| `app/build.gradle.kts` | Signing config (release) |
| `ui/theme/Theme.kt` | Material 3 theme |

---

## 🔜 Up Next (6 files)

| File | Task | Difficulty |
|------|------|:----------:|
| `export/CsvExporter.kt` | Generate CSV from expenses | Easy |
| `export/ShareManager.kt` | Android share/email intent | Easy |
| `ui/settings/SettingsScreen.kt` | Profile + export + settings | Medium |
| `ui/settings/SettingsViewModel.kt` | Settings state + preferences | Medium |
| `data/repository/SettingsRepository.kt` | Save/load user preferences | Easy |
| `ui/theme/ThemeManager.kt` | Switch between themes | Easy |

---

## 📋 Settings Screen Sections

| Section | Options | Implementation |
|---------|---------|----------------|
| **Profile** | Name, email, photo | From `FirebaseAuthRepository` |
| **Theme** | Light, Pink, Dark | `ThemeManager.kt` + DataStore |
| **Language** | English, Myanmar | Android locale switching |
| **Notifications** | On/Off toggle | `SettingsRepository.kt` + DataStore |
| **Export** | CSV export button | `CsvExporter.kt` |
| **Account** | Sign out | `AuthRepository.signOut()` |

---

## 🎨 Theme Options

| Theme | Mode | Colors |
|-------|------|--------|
| Light | `ThemeMode.Light` | White background, dark text |
| Pink | `ThemeMode.Light` | Pink primary color, light background |
| Dark | `ThemeMode.Dark` | Dark background, light text |

---

## 🌐 Language Options

| Language | Locale Code | Resources |
|----------|-------------|-----------|
| English | `en` | `res/values/` (default) |
| Myanmar | `my` | `res/values-my/` (need to create) |

---

## 📦 Depends On Dev 1
- ✅ `Expense.kt` (data model)
- ✅ `ExpenseRepository` interface
- ✅ Navigation routes + MainActivity scaffold
- ✅ Theme + reusable components
- ✅ CI/CD pipelines ready
- ✅ Signing config ready
- ✅ Theme.kt ready

---

## 🔗 Integration

- CSV export uses `ExpenseRepository` from Dev 3
- Profile data from `FirebaseAuthRepository` (Dev 1)
- Sign out calls `AuthRepository.signOut()`
- Theme switching via `ThemeManager.kt` in `ui/theme/`
- Language switching via Android locale API
- Settings stored in DataStore

---

## 📝 Scratch Notes
```
```
