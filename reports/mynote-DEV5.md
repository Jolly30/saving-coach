# 👤 Dev 5 — Work Log

> **Role:** Settings, Onboarding + Release  
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
| `ui/auth/AuthScreen.kt` | Login screen |

---

## 🔜 Up Next (8 files)

| File | Task | Difficulty |
|------|------|:----------:|
| `export/ExcelExporter.kt` | Generate Excel from expenses + savings | Medium |
| `export/ShareManager.kt` | Android share/email intent | Easy |
| `ui/settings/SettingsScreen.kt` | Profile + export + settings | Medium |
| `ui/settings/SettingsViewModel.kt` | Settings state + preferences | Medium |
| `data/repository/SettingsRepository.kt` | Save/load user preferences | Easy |
| `ui/theme/ThemeManager.kt` | Switch between themes | Easy |
| `ui/onboarding/OnboardingScreen.kt` | New user profile form | Medium |
| `ui/onboarding/OnboardingViewModel.kt` | Onboarding state | Easy |

---

## 👤 Onboarding Screen (After Sign Up)

### User Profile Fields

| Field | Type | Options | Required |
|-------|------|---------|:--------:|
| Career | Text | "Software Engineer" | ✅ |
| Age | Number | "25" | ✅ |
| Gender | Select | "Male", "Female", "Rather not answer" | ✅ |
| Salary Range | Select | See below | ✅ |

### Salary Range Options
| Range | Value |
|-------|-------|
| Less than 100k | `< 100,000 MMK` |
| 100k - 200k | `100,000 - 200,000 MMK` |
| 200k - 300k | `200,000 - 300,000 MMK` |
| 300k - 500k | `300,000 - 500,000 MMK` |
| 500k - 1M | `500,000 - 1,000,000 MMK` |
| More than 1M | `> 1,000,000 MMK` |
| Prefer not to say | `null` |

### Onboarding Flow
```
Sign Up → Onboarding Screen → Dashboard
                ↓
        Save profile to Firestore
        users/{userId}/profile
```

### Firestore Profile Structure
```
users/{userId}/profile {
    career: "Software Engineer",
    age: 25,
    gender: "male",          // "male", "female", or "not_specified"
    salaryRange: "100k-200k", // or null if prefer not to say
    createdAt: timestamp
}
```

---

## 📋 Settings Screen Sections

| Section | Options | Implementation |
|---------|---------|----------------|
| **Profile** | Name, email, photo, career, salary, age, gender | From `FirebaseAuthRepository` + profile |
| **Theme** | Light, Pink, Dark | `ThemeManager.kt` + DataStore |
| **Language** | English, Myanmar | Android locale switching |
| **Notifications** | On/Off toggle | `SettingsRepository.kt` + DataStore |
| **Export** | Monthly history (spending + saving) in Excel | `ExcelExporter.kt` |
| **Account** | Sign out | `AuthRepository.signOut()` |

---

## 📊 Export Feature

### Export Types

| Export | Content | Format |
|--------|---------|--------|
| **Spending History** | Expenses per month | `.xlsx` Excel |
| **Saving History** | Challenge deposits per month | `.xlsx` Excel |

### Export Flow
```
Settings → Export → Select Month → Select Type (Spending/Saving) → Download Excel
```

### Excel Columns (Spending)
| Date | Category | Merchant | Amount (MMK) | Notes |
|------|----------|----------|--------------|-------|

### Excel Columns (Saving)
| Start Date | End Date | Challenge Name | Deposit Amount (MMK) | Target Amount (MMK) |
|------------|----------|----------------|----------------------|---------------------|

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
- ✅ AuthScreen.kt ready

---

## 🔗 Integration

- CSV export uses `ExpenseRepository` from Dev 1
- Profile data from `FirebaseAuthRepository` (Dev 1)
- Sign out calls `AuthRepository.signOut()`
- Theme switching via `ThemeManager.kt` in `ui/theme/`
- Language switching via Android locale API
- Settings stored in DataStore
- Onboarding saves profile to Firestore

---

## 📝 Scratch Notes
```
```
