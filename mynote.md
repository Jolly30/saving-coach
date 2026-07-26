# Session Notes — 2026-07-25

## Changes Made

### 1. Fixed Crashlytics Build Error
- **Problem:** `Unresolved reference: crashlytics` — Firebase BOM auto-enabled Crashlytics via `google-services.json` but the plugin/dependency wasn't configured
- **Fix:**
  - Added `id("com.google.firebase.crashlytics") version "3.0.2"` to `app/build.gradle.kts` plugins
  - Added `implementation(libs.firebase.crashlytics)` to dependencies
  - Added `firebase-crashlytics` entry to `gradle/libs.versions.toml`

### 2. Fixed "Module not specified" Run Config
- **Problem:** Android Studio lost module reference after build file changes — Run Configuration showed `<no module>`
- **Fix:** Sync Gradle → Delete broken config → Recreate Android App config pointing to `saving-coach.app` module

### 3. Fixed Duplicate Class Build Error (Hilt)
- **Problem:** `Type X is defined multiple times` — stale Hilt-generated classes with `* 2.class` duplicates in build cache
- **Fix:** Run `./gradlew clean` then `./gradlew assembleDebug`

### 4. Implemented Google Sign-In with Credential Manager
- **Problem:** App was using `MockAuthRepository` (fake auth), Google Sign-In button was a TODO placeholder doing nothing
- **Files Created:**
  - `app/src/main/java/com/savingcoach/app/data/repository/FirebaseAuthRepository.kt` — real Firebase Auth implementation (email + Google via `GoogleAuthProvider`)
- **Files Modified:**
  - `app/src/main/java/com/savingcoach/app/di/AppModule.kt` — added `FirebaseAuth` provider via `@Provides`
  - `app/src/main/java/com/savingcoach/app/di/RepositoryModule.kt` — swapped `MockAuthRepository` → `FirebaseAuthRepository`
  - `app/src/main/java/com/savingcoach/app/ui/auth/AuthViewModel.kt` — replaced `signInWithGoogle(idToken)` with `onGoogleIdTokenReceived(idToken)` + `onGoogleSignInError(message)` for Credential Manager flow
  - `app/src/main/java/com/savingcoach/app/ui/auth/AuthScreen.kt` — wired Google button to launch Credential Manager, get Google ID token, pass to ViewModel
  - `gradle/libs.versions.toml` — added versions: `credentials = "1.3.0"`, `credentials-play-services-auth = "1.3.0"`, `play-services-auth = "2.1.0"`, `googleid = "1.1.1"` + corresponding library entries
  - `app/build.gradle.kts` — added Credential Manager dependencies

### 5. Fixed Credential Manager Unresolved References
- **Problem:** `Unresolved reference: CustomCredentialRequest`, `GetGoogleIdOption`, `GoogleIdTokenCredential`, `GoogleIdTokenParsingException`
- **Fix:**
  - Added missing `googleid` artifact (`com.google.android.libraries.identity.googleid:googleid:1.1.1`) to version catalog and build.gradle.kts
  - Fixed imports: `GoogleIdTokenCredential` is from `androidx.credentials.providers`, not `com.google.android.libraries.identity.googleid`
  - Removed unused `CustomCredentialRequest` import
  - Correct imports now: `TYPE_GOOGLE_ID_TOKEN_CREDENTIAL` and `createFrom` from `androidx.credentials.providers.GoogleIdTokenCredential`

### 6. Fixed Final Compile Errors in AuthScreen
- **Problem:** `Unresolved reference 'providers'` and `Unresolved reference 'GoogleIdTokenCredential'` — wrong import path
- **Fix:** Changed import from `androidx.credentials.providers.GoogleIdTokenCredential` → `com.google.android.libraries.identity.googleid.GoogleIdTokenCredential`
- **Result:** `BUILD SUCCESSFUL` — app compiles clean

## Pending Manual Steps (Firebase Console)
1. ~~Go to Firebase Console → Authentication → Sign-in method → Enable **Google**~~ ✅ Done
2. ~~Set support email~~ ✅ Done
3. ~~Copy **Web client ID** (`xxxx.apps.googleusercontent.com`)~~ ✅ Done — `42108385419-is8ctsvtkob8uedf0pgtdlcn5lolg8gu`
4. ~~Replace placeholder `webClientId` in `AuthScreen.kt` line ~58~~ ✅ Done
5. ~~Add SHA-1 fingerprint (`./gradlew signingReport`) in Project Settings → Android app~~ ✅ Done

## App Start Issue Analysis

**Symptom:** Problem to start the app (either `<no module>` in IDE or runtime crash on launch).
**Build Status:** Clean (`BUILD SUCCESSFUL`).

### 1. If hitting `<no module>` Run Configuration Error in Android Studio
- **Cause:** Android Studio's `.idea` workspace lost its mapping to the Gradle modules after significant `build.gradle.kts` changes (e.g., adding Hilt, Crashlytics). Gradle sync failed silently or the cache was invalidated.
- **Resolution:** A full Gradle Sync and deleting/recreating the Run Configuration was the correct fix (as noted in step 2 above).

### 2. If hitting a Runtime Crash on Launch
- **Cause:** Firebase Initialization is failing before Hilt Dependency Injection runs.
- **Details:** The app relies on `FirebaseAuth.getInstance()` provided as a Singleton in `AppModule.kt`. If the `google-services.json` isn't processed properly, or if the emulator lacks Google Play Services, `FirebaseApp.initializeApp()` won't run.
- **Result:** When `MainActivity` loads `AuthViewModel`, Hilt attempts to inject `FirebaseAuth`. Since Firebase isn't initialized, this triggers an immediate `IllegalStateException` causing a crash.
- **Verification:** Everything else is correctly configured:
  - `SavingCoachApp` has `@HiltAndroidApp` and is registered in `AndroidManifest.xml`.
  - `MainActivity` has `@AndroidEntryPoint`.
  - `google-services.json` is correctly placed and the `applicationId` matches `com.savingcoach.app`.

### 3. Duplicate Class Error (`* 2.java`) in Android Studio
- **Problem:** Build failed with multiple duplicate class errors originating from files like `_com_savingcoach_app_di_RepositoryModule 2.java` inside the `app/build/` directory.
- **Cause:** The project was located on the macOS Desktop, which was actively being synced by iCloud Drive. As Android Studio's Hilt/KAPT compiler rapidly generated and replaced files, iCloud created conflict copies (appending ` 2` to filenames), confusing the Java compiler.
- **Resolution:** 
  - Ran `rm -rf app/build` to clear the corrupted build cache.
  - Moved the entire project out of the iCloud-synced Desktop folder to a local directory (`/Users/yadanar/saving-coach`) to permanently prevent sync conflicts.

### 4. "No credentials available" Error on Google Sign-In
- **Problem:** Tapping "Continue with Google" on the emulator immediately showed a "No credentials available" snackbar at the bottom of the screen.
- **Cause:** Android Credential Manager successfully executed the request, but the emulator itself was not signed into any Google account, so it had no credentials to offer.
- **Resolution:** Open the Play Store or Settings > Passwords & Accounts on the emulator and sign into a real (or test) Google account. Once signed in, the bottom sheet will correctly slide up to offer the account for Firebase Authentication.
