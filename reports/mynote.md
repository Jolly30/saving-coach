# 👤 Dev 1 — Work Log

> Last updated: 2026-07-25

---

## 📋 Current Task

| Task | Status | Notes |
|------|--------|-------|
| All Dev 1 tasks | ✅ **COMPLETE** | Repo pushed, team unblocked 🎉 |
| Real Firebase Auth + Google Sign-In | ✅ **COMPLETE** | Credential Manager integrated, build passes |

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

## 📝 Scratch Notes

```
Project: Saving Coach | Package: com.savingcoach.app
Repo: https://github.com/Jolly30/saving-coach
Dev 1 Role: UI Skeleton — Theme + Nav + Auth + Dashboard + CI/CD
Status: ✅ ALL 48 TASKS COMPLETE + Real Auth Implemented
```
