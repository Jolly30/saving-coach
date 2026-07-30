# 👤 Dev 1 — Work Log

> Last updated: 2026-07-30

---

## 📋 Current Task

| Task | Status | Notes |
|------|--------|-------|
| All original Dev 1 tasks | ✅ **COMPLETE** | Repo pushed, team unblocked 🎉 |
| Real Firebase Auth + Google Sign-In | ✅ **COMPLETE** | Credential Manager integrated, build passes |
| Firestore Repositories (from Dev 3) | ⏳ **PENDING** | FirebaseExpenseRepository, FirebaseBudgetRepository, FirebaseSavingChallengeRepository |

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
| 7 | Code against the **interfaces** (Dev 3 builds real Firestore later) |

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
| Deploy proxy to Vercel | ✅ Done | `https://proxy-topaz-ten-36.vercel.app` |
| Set `GEMINI_API_KEY` env var | ✅ Done | Stored in Vercel (not hardcoded) |
| Disable SSO protection | ✅ Done | App can access API without auth |
| Update `local.properties` | ✅ Done | `proxy.url=https://proxy-topaz-ten-36.vercel.app` |
| Update `local.defaults.properties` | ✅ Done | Same URL for other devs |
| Build app | ✅ Done | `./gradlew assembleDebug` — **BUILD SUCCESSFUL** |
| Test proxy | ✅ Done | Proxy forwards to Gemini API successfully |

### Proxy Details

| Item | Value |
|------|-------|
| **Production URL** | `https://proxy-topaz-ten-36.vercel.app` |
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

## 🔧 Session 3 — Firestore Repositories (Upcoming)

### New Responsibility (Moved from Dev 3)

Dev 1 now owns the real Firestore repository implementations. Previously this was Dev 3's job.

**3 files to build:**

| File | Task | Difficulty |
|------|------|:----------:|
| `FirebaseExpenseRepository.kt` | CRUD + real-time sync for expenses via Firestore | Medium |
| `FirebaseBudgetRepository.kt` | Budget limits + spending totals | Easy |
| `FirebaseSavingChallengeRepository.kt` | Challenges + deposits CRUD | Easy |

**Firestore structure:**
```
users/{userId}/expenses/{expenseId}
users/{userId}/budgets/{YYYY-MM}
users/{userId}/challenges/{challengeId}
users/{userId}/challenges/{challengeId}/deposits/{depositId}
```

**How to finish:**
Swap mocks → real repos in `RepositoryModule.kt`:
```kotlin
// Change:
@Binds abstract fun bindExpenseRepository(impl: MockExpenseRepository): ExpenseRepository
// To:
@Binds abstract fun bindExpenseRepository(impl: FirebaseExpenseRepository): ExpenseRepository
```

**After this:**
- Dev 3 (Saving Challenges), Dev 4 (Budget & Expense Hub), Dev 5 (Export) swap their mocks to real data
- Integration test week

---

## 📝 Scratch Notes

```
Project: Saving Coach | Package: com.savingcoach.app
Repo: https://github.com/Jolly30/saving-coach
Dev 1 Role: UI Skeleton + Auth + Dashboard + AI Proxy + Firestore Repositories
Status: ✅ ALL original tasks DONE + Real Auth + Gemini Proxy (Deployed)
        ⏳ Firestore repos (next)
Proxy URL: https://proxy-topaz-ten-36.vercel.app
```
