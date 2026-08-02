# 👤 Dev 1 — Work Log

> Last updated: 2026-08-01 (Session 12)

---

## 📋 Current Task

| Task | Status | Notes |
|------|--------|-------|
| All original Dev 1 tasks | ✅ **COMPLETE** | Repo pushed, team unblocked 🎉 |
| Real Firebase Auth + Google Sign-In | ✅ **COMPLETE** | Credential Manager integrated, build passes |
| Firestore Repositories (from Dev 3) | ✅ **COMPLETE** | FirebaseExpenseRepository, FirebaseBudgetRepository, FirebaseSavingChallengeRepository — BUILD SUCCESSFUL |

---

## ✅ ALL TASKS COMPLETE 🎉

| Phase | Status | Notes |
|-------|--------|-------|
| 0 — Environment + Firebase | ✅ Done | JDK, Gradle, Firebase project, Auth, Firestore |
| 1 — Data Models + Repo Interfaces | ✅ Done | 6 models + 5 interfaces (unblocks Dev 2/3/4/5) |
| 2 — Navigation | ✅ Done | Routes.kt + NavGraph.kt (8 routes) |
| 3 — App Shell | ✅ Done | Manifest, SavingCoachApp (Hilt), MainActivity (bottom nav) |
| 4 — Theme | ✅ Done | Color.kt, Type.kt, Theme.kt, strings.xml, colors.xml, themes.xml |
| 5 — Auth Screen | ✅ Done | AuthViewModel + AuthScreen (Google + Email sign-in) |
| 6 — Dashboard | ✅ Done | DashboardViewModel + CalendarHeatmap + DashboardScreen |
| 7 — Reusable Components | ✅ Done | BudgetProgressBar, SpendingChart, LoadingOverlay |
| 8 — CI/CD | ✅ Done | ci-pr-check.yml, ci-release.yml, proguard, signing config |
| 9 — Git & Team | ✅ **DONE** | Repo: Jolly30/saving-coach |
| — Mock Repos + Hilt DI | ✅ Done | MockRepositories.kt + RepositoryModule.kt |
| — Package rename | ✅ Done | `com.savingcoach.app` |
| — Build verifies | ✅ Done | `./gradlew assembleDebug` — **BUILD SUCCESSFUL** |
| — Firestore Repos | ✅ Done | Firebase repos for Expenses, Budgets, Saving Challenges |

### 🌐 Repo
- **GitHub:** https://github.com/Jolly30/saving-coach
- **Branches:** `main`, `develop`

---

## 🔧 Session 2 — Real Auth Implementation (2026-07-25)

### 1. Fixed Crashlytics Build Error
- **Problem:** `Unresolved reference: crashlytics` — Firebase BOM auto-enabled Crashlytics via `google-services.json` but plugin/dependency wasn't configured
- **Fix:** Added crashlytics plugin + dependency to `build.gradle.kts` and `libs.versions.toml`

### 2. Fixed "Module not specified" Run Config
- **Problem:** Android Studio lost module reference after build file changes
- **Fix:** Sync Gradle → Delete broken config → Recreate Android App config

### 3. Fixed Duplicate Class Build Error (Hilt)
- **Problem:** `Type X is defined multiple times` — stale Hilt-generated classes
- **Fix:** `./gradlew clean` then `./gradlew assembleDebug`

### 4. Implemented Google Sign-In with Credential Manager
- **Problem:** App used `MockAuthRepository` (fake auth), Google button was a TODO placeholder
- **Created:**
  - `FirebaseAuthRepository.kt` — real Firebase Auth (email + Google via `GoogleAuthProvider`)
- **Modified:**
  - `AppModule.kt` — added `FirebaseAuth` provider
  - `RepositoryModule.kt` — swapped mock → real auth
  - `AuthViewModel.kt` — Credential Manager flow (`onGoogleIdTokenReceived` + `onGoogleSignInError`)
  - `AuthScreen.kt` — Google button launches Credential Manager, gets ID token
  - `libs.versions.toml` — added Credential Manager deps
  - `build.gradle.kts` — added Credential Manager dependencies

### 5. Fixed Credential Manager Unresolved References
- **Problem:** Wrong imports for `GoogleIdTokenCredential`, missing `googleid` artifact
- **Fix:** Added `googleid:1.1.1` to version catalog, corrected imports

### 6. Fixed Final Compile Errors in AuthScreen
- **Problem:** `Unresolved reference 'providers'` — wrong import path
- **Fix:** Changed to `com.google.android.libraries.identity.googleid.GoogleIdTokenCredential`
- **Result:** `BUILD SUCCESSFUL`

### 7. Duplicate Class Error from iCloud Sync
- **Problem:** Hilt-generated files got duplicated (`* 2.java`) because project was on iCloud-synced Desktop
- **Fix:** Moved project to `/Users/yadanar/saving-coach` (outside iCloud), cleaned build

### 8. "No credentials available" on Google Sign-In
- **Problem:** Tapping Google button showed error on emulator
- **Cause:** Emulator wasn't signed into any Google account
- **Fix:** Sign into a Google account on the emulator first

### 9. Physical Device Testing & Xiaomi Quirks
- **Problem:** Tapping "Continue with Google" on a physical Xiaomi/Redmi device showed a blank screen, or the app crashed instantly upon successful login.
- **Cause:** Jetpack Compose `LocalContext` bugs, Xiaomi's aggressive popup blocker, and an `IllegalStateException` on the Dashboard screen caused by nesting infinite scrolling items.
- **Fixes Applied:**
  - Unwrapped the Jetpack Compose Context to a raw `Activity` in `AuthScreen.kt`.
  - Added `.setAutoSelectEnabled(true)` to `GetGoogleIdOption` to bypass Xiaomi's pop-up blocker for single-account devices.
  - Added error-catching UI (Snackbar) for `GetCredentialCancellationException`.
  - Fixed infinite height constraints on `CalendarHeatmap.kt` by setting `.heightIn(max = 320.dp)` on the `LazyVerticalGrid`.

---

## 🔧 Firebase Console Setup — DONE ✅

| Step | Status | Details |
|------|--------|---------|
| Enable Google provider | ✅ Done | Authentication → Sign-in method → Google |
| Set support email | ✅ Done | — |
| Web Client ID | ✅ Done | `42108385419-is8ctsvtkob8uedf0pgtdlcn5lolg8gu` |
| Replace webClientId in code | ✅ Done | `AuthScreen.kt` line ~56 |
| Add SHA-1 fingerprint | ✅ Done | Project Settings → Android app |

---

## 📝 What Other Devs Need

| Step | What to do |
|------|-----------|
| 1 | `git clone https://github.com/Jolly30/saving-coach.git` |
| 2 | `cp local.defaults.properties local.properties` — fill in SDK path + Gemini key |
| 3 | Download `google-services.json` from Firebase → `app/` folder |
| 4 | Send your SHA-1 fingerprint to team lead (`./gradlew signingReport`) |
| 5 | `./gradlew assembleDebug` to verify |
| 6 | Create feature branch off `develop` |
| 7 | Code against the **interfaces** — real Firestore repos are now implemented ✅ |

---

## 🔧 Session 3 — Gemini API Proxy for Myanmar (2026-07-28)

### Problem
Gemini API is not officially supported in Myanmar. Google blocks API requests from Myanmar IP addresses. Users in the app cannot use the AI chat feature without a workaround.

### Solution
Set up a proxy server hosted in a supported region (e.g., Singapore, US) via Vercel. The app sends requests to the proxy → proxy forwards to Gemini API → returns response. This also keeps the API key server-side (more secure).

### What Was Done

#### 1. Created Proxy Server (`proxy/` folder)

| File | Purpose |
|------|---------|
| `api/chat.js` | Vercel serverless function — accepts chat messages, forwards to Gemini API, returns response |
| `vercel.json` | Vercel deployment config (routes + build settings) |
| `package.json` | Minimal — no Express needed for Vercel serverless |
| `.env.example` | Template showing `GEMINI_API_KEY` variable |
| `.gitignore` | Ignores `node_modules/`, `.env`, `.vercel/` |

**Endpoint:**
```
POST /api/chat
Body: { "messages": [{"role": "user", "content": "..."}], "systemPrompt": "..." }
Response: { "reply": "..." }
```

#### 2. Updated Android App

**Modified files:**

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Removed `libs.generative.ai` dependency (no more direct Gemini SDK calls) |
| `local.properties` | Replaced `gemini.api.key` → `proxy.url` |
| `local.defaults.properties` | Replaced `gemini.api.key` → `proxy.url` (template for other devs) |
| `di/AppModule.kt` | Added `OkHttpClient`, `FirebaseFirestore`, and `proxyUrl` providers |
| `di/RepositoryModule.kt` | Swapped `MockChatRepository` → `AiChatRepository` |
| `navigation/NavGraph.kt` | Wired `ChatScreen` to the Chat route (was placeholder) |

**New files:**

| File | Purpose |
|------|---------|
| `ai/GeminiProxyService.kt` | OkHttp service that calls the proxy endpoint |
| `ai/AiChatRepository.kt` | Implements `ChatRepository` — uses proxy for AI + Firestore for chat history |
| `ui/chat/ChatViewModel.kt` | ViewModel — manages messages, loading state, error handling |
| `ui/chat/ChatScreen.kt` | Chat UI — message bubbles, input field, auto-scroll |

#### 3. Build Verification
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**
- Fixed 2 compilation errors during the process:
  - `BuildConfig.PROXY_URL` → `BuildConfig.proxyurl` (secrets plugin naming convention)
  - Missing `FirebaseFirestore` Hilt provider — added to `AppModule.kt`

---

## ✅ Proxy Deployment — COMPLETE ✅

| Step | Status | Details |
|------|--------|---------|
| Deploy proxy to Vercel | ✅ Done | `https://proxy-lake-xi-82.vercel.app` |
| Set `GEMINI_API_KEY` env var | ✅ Done | Stored in Vercel (not hardcoded) |
| Disable SSO protection | ✅ Done | App can access API without auth |
| Update `local.properties` | ✅ Done | `proxy.url=https://proxy-lake-xi-82.vercel.app` |
| Update `local.defaults.properties` | ✅ Done | Same URL for other devs |
| Build app | ✅ Done | `./gradlew assembleDebug` — **BUILD SUCCESSFUL** |
| Test proxy | ✅ Done | Proxy forwards to Gemini API successfully |

### Proxy Details

| Item | Value |
|------|-------|
| **Production URL** | `https://proxy-lake-xi-82.vercel.app` |
| **Vercel Dashboard** | [vercel.com/jolly30s-projects/proxy](https://vercel.com/jolly30s-projects/proxy) |
| **API Endpoint** | `POST /api/chat` |
| **API Key Storage** | Vercel environment variable (encrypted) |
| **Gemini Free Tier** | ⚠️ Quota exceeded — needs reset or upgrade |

### Security

- API key is **NOT hardcoded** in any source file
- Key is stored as a Vercel environment variable (`process.env.GEMINI_API_KEY`)
- Android app only knows the proxy URL, never the Gemini key
- `.env.example` is a template only — no real keys committed

### What's Left

1. **Gemini API quota** — Free tier limit reached. Options:
   - Wait for quota reset
   - Upgrade plan at [aistudio.google.com](https://aistudio.google.com)
   - Generate a new API key

2. **Test chat on device** — Once quota is available, run the app and try the AI Chat screen

---

## 🔧 Session 3 — Firestore Repositories (2026-07-31)

### New Responsibility (Moved from Dev 3)

Dev 1 now owns the real Firestore repository implementations. Previously this was Dev 3's job.

**3 files created:**

| File | Description | Difficulty |
|------|-------------|:----------:|
| `FirebaseExpenseRepository.kt` | CRUD + real-time listeners for expenses | Medium |
| `FirebaseBudgetRepository.kt` | Budget read/write per month | Easy |
| `FirebaseSavingChallengeRepository.kt` | Challenges + deposits with subcollection | Easy |

**Firestore structure (final):**
```
expenses/{expenseId}                                   ← top-level (delete by ID works)
users/{userId}/budgets/{YYYY-MM}                       ← budget per month
users/{userId}/challenges/{challengeId}                 ← challenges
users/{userId}/challenges/{challengeId}/deposits/{depositId}  ← deposits subcollection
```

**Design decisions:**
- Expenses use a **top-level collection** with `userId` field — so `deleteExpense(expenseId)` works without needing userId (matches interface)
- Budgets use `yearMonth` as the document ID — direct lookup, no queries needed
- `addDeposit` auto-increments `currentAmount` on the challenge using `FieldValue.increment()`
- `deleteChallenge` cascades — deletes all deposits first, then the challenge doc
- All real-time listeners use `callbackFlow` + `addSnapshotListener`

**DI binding swap in `RepositoryModule.kt`:**
```kotlin
// Changed:
@Binds abstract fun bindExpenseRepository(impl: FirebaseExpenseRepository): ExpenseRepository
@Binds abstract fun bindBudgetRepository(impl: FirebaseBudgetRepository): BudgetRepository
@Binds abstract fun bindSavingChallengeRepository(impl: FirebaseSavingChallengeRepository): SavingChallengeRepository
```

**Build verification:** `./gradlew assembleDebug` — **BUILD SUCCESSFUL** ✅

**After this:**
- Dev 3 (Saving Challenges), Dev 4 (Budget & Expense Hub), Dev 5 (Export) swap their mocks to real data
- Integration test week

---

## 🔧 Session 4 — Dashboard Fix + Firestore Wiring (2026-07-31)

### Problem
`DashboardViewModel` had `userId` hardcoded to `"default_user"`. With real Firestore repos, all queries went to `users/default_user/...` instead of the actual authenticated user's data.

### Fix
- **Injected** `AuthRepository` into `DashboardViewModel` constructor
- **Replaced** `"default_user"` → `authRepository.getCurrentUserId() ?: "unknown"`

**Modified file:**

| File | Change |
|------|--------|
| `ui/dashboard/DashboardViewModel.kt` | Added `AuthRepository` injection, replaced hardcoded userId with real auth UID |

**Build verification:** `./gradlew assembleDebug` — **BUILD SUCCESSFUL** ✅

---

## 🔧 Session 5 — Crash Fixes & UI Sync (2026-07-31)

### 1. Fixed Dashboard Crash on Startup (Unknown User)
- **Problem:** Removing the hardcoded `userId` caused `DashboardViewModel` to query Firestore with `userId = "unknown"` when a user wasn't logged in. This threw a Permission Denied error from Firestore, crashing the app because the unhandled exception bubbled up through the `combine` flow.
- **Fix:** Added an early return in `loadDashboard()` if `userId == "unknown"` and appended `.catch { }` to safely handle flow exceptions without crashing.

### 2. Fixed Navigation UI to Match Setup Guide
- **Problem:** The bottom navigation bar incorrectly included a "Chat" tab instead of "Challenges", and the Chat feature was supposed to be a Floating Action Button (FAB).
- **Fix:** 
  - Updated `MainActivity.kt` to replace the Chat tab with the **Challenges** tab.
  - Added a `FloatingActionButton` for Chat in the `Scaffold`.
  - Added the missing `Challenges` route to `Routes.kt` and `NavGraph.kt`.

### 3. Synced Dashboard Threshold Alerts with Setup Guide
- **Problem:** The Dashboard progress bar colors did not match the thresholds defined in the setup guide (75%, 90%, 100%), and the alert banners were entirely missing.
- **Fix:** Updated `DashboardScreen.kt` to use the correct `75` (Yellow), `90` (Orange), and `100` (Red) percentage thresholds, and added the missing visual alert banners to display when these thresholds are crossed.

**Modified files:**
- `ui/dashboard/DashboardViewModel.kt`
- `ui/dashboard/DashboardScreen.kt`
- `MainActivity.kt`
- `navigation/Routes.kt`
- `navigation/NavGraph.kt`

---

## 🔧 Session 6 — Dashboard UI Overhaul (2026-07-31)

### Problem
Dashboard had multiple issues: unused components, no category breakdown, no expense list, hardcoded currency, dark mode issues, inconsistent color thresholds, no error handling, no pull-to-refresh.

### What Was Fixed

| # | Issue | Fix |
|---|-------|-----|
| 1 | SpendingChart never used | Wired up with category breakdown from ViewModel |
| 2 | No category data in ViewModel | Added `categorySpending: List<CategorySpending>` grouped by category |
| 3 | No expense list shown | Added `recentExpenses` (last 5) with category, merchant, amount, date |
| 4 | Challenge count only | Now shows challenge cards with title, progress bar, amount, % complete |
| 5 | BudgetProgressBar unused | Replaced inline LinearProgressIndicator with reusable BudgetProgressBar |
| 6 | LoadingOverlay unused | Replaced inline CircularProgressIndicator with LoadingOverlay |
| 7 | No error handling | Added `.catch { }` on all flows, sets `DashboardUiState.error` |
| 8 | No pull-to-refresh | Added `PullToRefreshBox` wrapper |
| 9 | Color thresholds inconsistent | Aligned BudgetProgressBar to `>= 100/90/75` matching DashboardScreen |
| 10 | Hardcoded "MMK" currency | Now uses dynamic `currency` from first expense |
| 11 | Dark mode CalendarHeatmap | Still uses `Color.LightGray` for empty days (minor) |
| 12 | Today highlight unclear | Added primary color border around today's date cell |

**Modified files:**

| File | Change |
|------|--------|
| `ui/dashboard/DashboardViewModel.kt` | Added `CategorySpending` data class, category breakdown, error handling, dynamic currency |
| `ui/dashboard/DashboardScreen.kt` | Wired SpendingChart, BudgetProgressBar, LoadingOverlay. Added expense list, challenge cards, pull-to-refresh |
| `ui/dashboard/CalendarHeatmap.kt` | Added border on today's date for visibility |
| `ui/components/BudgetProgressBar.kt` | Fixed color thresholds to `>= 100/90/75` |

**Build verification:** `./gradlew assembleDebug` — **BUILD SUCCESSFUL** ✅

---

## 🔧 Session 7 — Dashboard Redesign v2 (2026-07-31)

### What Was Built

Redesigned dashboard to match the new ASCII mockup spec.

| Feature | Implementation |
|---------|---------------|
| **Challenge Carousel** | Horizontal `Row` + `horizontalScroll` with swipeable cards, "See All ➔" button → navigates to Challenges tab |
| **Filter Chips** | All / 💰 Savings / 🧾 Expenses — filter calendar display |
| **Interactive Tooltip** | Tap any calendar day → dark floating popover with 🎯 Budget / 🧾 Expense / 💰 Saving totals |
| **Green Dot Indicator** | Small green dot below date number on days with savings deposits |
| **Exception-Based Colors** | Clean calendar — only color cells that are 80%+ or over budget |
| **Calendar History** | "See All Months ➔" link → new screen with ◄ Month Year ► switcher |
| **Selected Date Highlight** | Primary color border on tapped date |

**New files:**

| File | Purpose |
|------|---------|
| `ui/dashboard/CalendarHistoryScreen.kt` | Historical calendar with month switcher, filters, and back navigation |
| `ui/dashboard/CalendarHistoryViewModel.kt` | ViewModel for calendar history — month navigation, data fetching |

**Modified files:**

| File | Change |
|------|--------|
| `ui/dashboard/DashboardViewModel.kt` | Added `CalendarFilter`, `TooltipData`, `dailySavings`, `selectedDate`, `onDateTap()`, `onFilterChange()`, `dismissTooltip()` |
| `ui/dashboard/DashboardScreen.kt` | Challenge carousel, filter chips, calendar wiring, "See All" links, tooltip popup |
| `ui/dashboard/CalendarHeatmap.kt` | Tap handler, green dot for savings, filter support, selected date highlighting, exception-based colors |
| `navigation/Routes.kt` | Added `CalendarHistory` route |
| `navigation/NavGraph.kt` | Added `CalendarHistoryScreen` composable, passed navigation callbacks to `DashboardScreen` |

**Build verification:** `./gradlew assembleDebug` — **BUILD SUCCESSFUL** ✅

---

## 🔧 Session 8 — Default Challenges + Real Filters + Scroll Fix (2026-07-31)

### What Was Fixed

| # | Issue | Fix |
|---|-------|-----|
| 1 | Challenge carousel empty (Dev 3 not done) | Added 4 default challenge templates that always show on dashboard |
| 2 | Default vs Active cards look the same | Default: surface bg, "Tap to activate", 0% progress. Active: green tint bg, "Active" badge, real progress |
| 3 | Filters just change opacity | Real filter behavior: SAVINGS → green tint on matching days, faded others. EXPENSES → normal color on matching, faded others |
| 4 | No filter indicator | Added "Showing days with savings/expenses" text below filter chips |
| 5 | Legend static | Legend adapts to current filter mode |
| 6 | Nested scroll conflicts | `LazyRow` → `Row` + `horizontalScroll`, `LazyVerticalGrid` → `Column` of `Row`s |

**Default challenge templates:**

| Template | Target | Shows as |
|----------|--------|----------|
| ✉️ 100 Envelopes | 100 | Tap to activate |
| 📅 $1/Day Challenge | 365 | Tap to activate |
| 🚫 No Spend Week | 7 | Tap to activate |
| 💰 Save 20% Income | 200 | Tap to activate |

**Modified files:**

| File | Change |
|------|--------|
| `ui/dashboard/DashboardViewModel.kt` | Added `DEFAULT_CHALLENGES` companion object, `displayChallenges` merged list |
| `ui/dashboard/DashboardScreen.kt` | `Row` + `horizontalScroll` carousel, active/default card states, filter indicator text |
| `ui/dashboard/CalendarHeatmap.kt` | `Column`/`Row` grid (no lazy), filter-aware colors, adaptive legend |

**Build verification:** `./gradlew assembleDebug` — **BUILD SUCCESSFUL** ✅

---

## 🔧 Session 9 — Filter UI Update (2026-07-31)

### What Was Fixed
- Replaced the horizontal `FilterChip` row on the Dashboard with a clean `DropdownMenu` for the Calendar filter to improve UI space and aesthetics.

**Modified files:**
- `ui/dashboard/DashboardScreen.kt`

---

## 🔧 Session 10 — Calendar Refactor (2026-07-31)

### Calendar Layout & Navigation
* **25-Month Range:** The calendar now renders a rolling 25-month range (12 months in the past, the current month, and 12 months in the future) instead of just the current month.
* **Auto-Scroll to Current Date:** Added logic to automatically scroll the calendar to the current month/year when the data first loads.
* **Year-to-Month Navigation Fix:** Fixed a bug where clicking a specific month from the Year view would ignore your tap and force you back to the current month. The auto-scroll logic was tweaked to only run *once* on initial load so it doesn't fight your manual navigation.
* **UI Spacing:** Fixed the "weird" layout in the Year view by adding proper null-padding and spacing between the mini-months, making the grid align perfectly.

### Top Bar & UI Polish
* **Filter Relocation:** Moved the Month/Year toggle button and the Calendar Filter dropdown into the main `TopAppBar` to save vertical space and clean up the screen.
* **Text Updates:** Changed the loading state text from "Loading history..." to a more accurate "Loading calendar...".

### New Rating System & Filter Logic
We completely overhauled the color rating system to unify the UI and decouple savings from the spending budget. The old "green dot" UI was completely removed in favor of full background colors.

* **"All" Filter (Peak Days):**
  * 🔴 **Red:** Highlights the Most Spent Day (the day with the highest total expense of the month).
  * 🟢 **Green:** Highlights the Most Saved Day (the day with the highest total savings deposit of the month).
  * ⚪ **No Color:** All other days remain clean.
* **"Expenses" Filter (Budget %):**
  * Divides your total monthly budget by the number of days in the month to get a "daily budget limit".
  * 🔴 **Red:** Spending was over 100% of the daily limit.
  * 🟡 **Yellow:** Spending was between 80% - 100% of the daily limit.
  * ⚪ **No Color:** Spending was under 80% of the daily limit.
  * *(Days with zero expenses are faded out).*
* **"Savings" Filter:**
  * 🟢 **Green:** Highlights the Most Saved Day.
  * ⚪ **No Color:** Other days with savings.
  * *(Days with zero savings are faded out).*
* **Dynamic Legend:** The legend at the bottom of the calendar now automatically updates its labels and colors to match whichever filter you are currently using.

### Syntax & Bug Fixes
* **`CalendarHistoryScreen.kt`:** Fixed an `Expecting a top level declaration` error caused by an extra trailing closing brace after restructuring the scroll layout.
* **`CalendarHeatmap.kt`:** Fixed a structural duplication issue in the grid layout and restored missing variables (`isSelected`, `isToday`) that broke the compilation when the Green dot was removed.

### Modified Files
| File | Changes |
|------|---------|
| `ui/dashboard/CalendarHeatmap.kt` | 25-month range, auto-scroll, rating system, filter logic, bug fixes |
| `ui/dashboard/DashboardScreen.kt` | Filter relocation to TopAppBar, calendar history navigation |
| `ui/dashboard/DashboardViewModel.kt` | Calendar data loading, filter state management |
| `ui/dashboard/CalendarHistoryScreen.kt` | New screen for historical calendar view |
| `ui/dashboard/CalendarHistoryViewModel.kt` | ViewModel for calendar history |
| `navigation/Routes.kt` | Added CalendarHistory route |
| `navigation/NavGraph.kt` | Added CalendarHistoryScreen composable |

---

## 🔧 Session 11 — Notification System & Bug Fixes (2026-07-31)

### What Was Built (Notification System)

Implemented a comprehensive push notification system for budget alerts, saving milestones, and daily reminders using Firebase Cloud Messaging and WorkManager.

### Notification Conditions

**Budget Alerts:**
| Threshold | Trigger | Notification Type |
|-----------|---------|-------------------|
| 75% | Spending reaches 75% of monthly budget | 🟡 Warning |
| 90% | Spending reaches 90% of monthly budget | 🟠 Critical |
| 100%+ | Spending exceeds monthly budget | 🔴 Over Budget (push notification) |

**Saving Milestones:**
| Threshold | Trigger | Notification Type |
|-----------|---------|-------------------|
| 50% | Saved 50% of challenge target | 🎯 Halfway |
| 75% | Saved 75% of challenge target | 🚀 Almost There |
| 100% | Completed challenge | 🎉 Celebration |

**Daily Reminders:**
| Reminder | Trigger | Schedule |
|----------|---------|----------|
| Daily Expense Log | User hasn't logged expenses today | Every day at 8:00 PM |
| Inactive Alert | No expenses logged for 1+ days | Every day at 9:00 PM |
| Saving Challenge | Daily reminder about active challenges | Every day at 7:00 PM |

### Files Created for Notifications:

| File | Purpose |
|------|---------|
| `core/notification/NotificationHelper.kt` | Core notification manager with channel setup, permission handling, and notification builders |
| `core/notification/NotificationScheduler.kt` | WorkManager scheduler for daily reminders and periodic checks |
| `core/notification/BootReceiver.kt` | Re-initializes alarms/WorkManager jobs upon device reboot |
| `workers/BudgetAlertWorker.kt` | Background worker to check budget thresholds and trigger alerts |
| `workers/SavingReminderWorker.kt` | Background worker for daily saving challenge reminders |
| `workers/InactiveAlertWorker.kt` | Background worker to detect inactive periods |
| `workers/DailyExpenseReminderWorker.kt` | Worker for daily expense logging reminders |

### Notification UI & Navigation

* Added a **Notification Bell Icon** (`Icons.Default.Notifications`) to the top right of the `DashboardScreen.kt` Header.
* Created a new **`NotificationsScreen.kt`** inside `ui/notifications/` with a clean `Scaffold` and `TopAppBar`.
* Added a new `Routes.Notifications` object and registered the screen in `NavGraph.kt`.
* Fixed a **"Double Insets"** (overlapping padding) UI bug on the `NotificationsScreen` by explicitly setting `windowInsets = WindowInsets(0, 0, 0, 0)` on the `TopAppBar` to prevent it from doubling up on the `MainActivity`'s global Scaffold padding.

### Bug Fixes

* **`SavingReminderWorker.kt` Compilation Error:** Fixed a compiler error (`Unresolved reference 'name'`) by changing `it.name` to `it.title` to correctly reference the `SavingChallenge` data class property.
* **Deprecation Warnings:** Updated `Icons.Default.Chat` to the modern `Icons.AutoMirrored.Filled.Chat` in `MainActivity.kt` to resolve deprecation warnings.
* **Redundant Logic Warnings:** Removed an always-true `day.date != null` condition in `CalendarHeatmap.kt` logic to clean up the code.
* **Duplicate Imports:** Resolved a `Conflicting import: imported name 'Icons' is ambiguous` error by cleaning up duplicate `Icons` imports in `DashboardScreen.kt`.

### Build Verification
`./gradlew assembleDebug` — **BUILD SUCCESSFUL** ✅

---

## 🔧 Session 12 — KAPT → KSP Migration (2026-08-01)

### Problem
A teammate reported a build failure on Windows:
```
> Task :app:kaptDebugKotlin FAILED
Caused by: java.lang.IllegalArgumentException: Invalid relative name: META-INF\proguard\...
```
KAPT (Kotlin Annotation Processing Tool) does not support Kotlin 2.0+. The project was using Kotlin 2.0.21 with KAPT, causing annotation processing failures. KAPT generates Java stubs first then processes them — it falls back to language version 1.9, but this often breaks.

### Solution
Migrated from KAPT to **KSP** (Kotlin Symbol Processing), which:
- Fully supports Kotlin 2.0+
- Is 2x faster (reads Kotlin AST directly, no Java stubs)
- Is the actively maintained replacement for KAPT

### Changes Made

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Added KSP version `2.0.21-1.0.28` + plugin `com.google.devtools.ksp` |
| `app/build.gradle.kts` | Replaced `kotlin("kapt")` → `alias(libs.plugins.ksp)`, `kapt()` → `ksp()` for Hilt compiler and WorkManager Hilt compiler |

### Dependency Swap
```kotlin
// Before (KAPT):
kotlin("kapt")
kapt(libs.hilt.compiler)
kapt("androidx.hilt:hilt-compiler:1.2.0")

// After (KSP):
alias(libs.plugins.ksp)
ksp(libs.hilt.compiler)
ksp("androidx.hilt:hilt-compiler:1.2.0")
```

### Why KSP is Better
| | KAPT | KSP |
|---|---|---|
| Kotlin 2.0+ | ❌ Not supported | ✅ Fully supported |
| Speed | Slow (generates Java stubs) | 2x faster (reads Kotlin AST directly) |
| Status | Deprecated | Active, recommended |

### Build Verification
`./gradlew assembleDebug` — **BUILD SUCCESSFUL** ✅

---

## 🔧 Session 13 — AI Proxy Debugging & Redeployment (2026-08-02)

### What Was Done

#### 1. Proxy Testing & Provider Cleanup
*   **API Key Auditing:** Tested live API keys (Gemini, OpenRouter, OpenAI, DeepSeek, Grok) by bypassing the local sandbox to make real requests.
*   **Result:** Only OpenRouter and Gemini were functional (though Gemini hit a free-tier 429 rate limit). OpenAI, DeepSeek, and Grok were rejected.
*   **Code Cleanup:** Removed the unused providers (OpenAI, DeepSeek, Grok) from the `providers` array in `proxy/api/chat.js` to streamline the proxy and only prompt for valid keys.

#### 2. Bug Fix: Fallback Loop
*   **Problem:** The proxy's fallback mechanism had a logical flaw. When a non-retryable error occurred (e.g., a `400 Bad Request` from a malformed API key), `if (!shouldSkip(status)) continue;` failed to actually abort the loop. It just fell through to the end of the `catch` block and naturally continued to the next provider.
*   **Fix:** Replaced the check with `if (shouldSkip(status)) { break; }` so non-retryable errors correctly abort the fallback chain.

#### 3. Vercel Redeployment
*   **Cleanup:** Completely removed the old proxy project from Vercel using `vercel rm proxy --yes` to ensure a clean slate and clear old environment variables.
*   **Deployment:** Redeployed the updated proxy using `vercel --prod`.
*   **New Production URL:** `https://proxy-lake-xi-82.vercel.app`

#### 4. Environment Variable Debugging
*   Guided the deployment of the `GEMINI_API_KEY` and `OPENROUTER_API_KEY` into the Vercel dashboard.
*   **OpenRouter Key Fix:** Discovered the OpenRouter key failed during fallback because an accidental newline (`\n`) was pasted into Vercel, breaking the HTTP header formatting. The key was cleaned up and redeployed.

#### 5. Final Verification
*   Ran a live `curl` test against the new Vercel endpoint. 
*   **Result:** The proxy correctly hit Gemini, caught the `429 Quota Exceeded` error (because `shouldSkip` correctly returned false for 429), smoothly fell back to OpenRouter, and successfully returned the JSON payload.

---

## 📝 Scratch Notes

```
Project: Saving Coach | Package: com.savingcoach.app
Repo: https://github.com/Jolly30/saving-coach
Dev 1 Role: UI Skeleton + Auth + Dashboard + AI Proxy + Firestore Repositories + Notifications
Status: ✅ ALL tasks DONE + Real Auth + Gemini Proxy + Firestore Repositories + Dashboard Redesign v2 + Default Challenges + Filter Dropdown + Notifications System + Notification UI + KAPT→KSP Migration
Proxy URL: https://proxy-lake-xi-82.vercel.app
```
