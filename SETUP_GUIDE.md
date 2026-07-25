# AI Personal Finance Manager — Setup & Team Guide

> Team size: 5 developers | Platform: Android (Kotlin + Jetpack Compose)  
> Target SDK: 35 (Android 15) | Min SDK: 26 (Android 8.0)

---

## Table of Contents

1. [Environment Setup](#1-environment-setup)
2. [Project Architecture Overview](#2-project-architecture-overview)
3. [Dependencies & Versions](#3-dependencies--versions)
4. [Project Skeleton (Directory Tree)](#4-project-skeleton-directory-tree)
5. [Dependency Map — Do This First](#5-dependency-map--do-this-first)
6. [Feature Breakdown by Developer](#6-feature-breakdown-by-developer)
7. [Firebase Setup Guide](#7-firebase-setup-guide)
8. [Gemini API Setup](#8-gemini-api-setup)
9. [Data Models (Firestore Schema)](#9-data-models-firestore-schema)
10. [Navigation Routes](#10-navigation-routes)
11. [Build & Run](#11-build--run)
12. [CI/CD Pipeline](#12-cicd-pipeline-github-actions)

---

## 1. Environment Setup

### Prerequisites

| Tool | Version | Why |
|------|---------|-----|
| Android Studio | Ladybug (2024.3+) | Primary IDE |
| JDK | 17 LTS | Kotlin compilation |
| Android SDK | API 35 + build-tools 36+ | Target platform |
| Gradle | 8.11.1 | Build system |
| Git | Latest | Version control |
| Firebase CLI | Latest | Firestore + Auth setup |

### Step-by-Step

```bash
# 1. Install JDK 17 (if not present)
# macOS (Homebrew):
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc

# 2. Verify installations
java -version          # Should show 17.x
adb --version          # Should show from SDK platform-tools

# 3. Clone the repo
git clone <repo-url>
cd saving-coach

# 4. Open in Android Studio
# File → Open → select saving-coach/ folder
# Let Android Studio index and download SDK components
```

### Android Studio Setup

1. Open Android Studio → **Plugins** → Install "Kotlin" (latest)
2. **SDK Manager** → SDK Platforms → Check "Android API 35"
3. **SDK Manager** → SDK Tools → Check:
   - Android SDK Build-Tools 36+
   - Android Emulator
   - Android SDK Platform-Tools

### Verifying the Build

```bash
# From the saving-coach/ directory:
./gradlew assembleDebug

# If it builds successfully, everything is set up.
```

---

## 2. Project Architecture Overview

### Pattern: MVVM (Model-View-ViewModel)

```
┌─────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)             │
│  ─ Screens / Composables                │
│  ─ StateFlow + collectAsState()         │
├─────────────────────────────────────────┤
│  ViewModel Layer                        │
│  ─ Holds UI state (StateFlow)           │
│  ─ Calls repositories                   │
├─────────────────────────────────────────┤
│  Repository Layer                       │
│  ─ Single source of truth               │
│  ─ Coordinates local + remote data      │
├─────────────────────────────────────────┤
│  Data Layer                             │
│  ─ Firestore (remote)                   │
│  ─ Room / DataStore (local offline)     │
│  ─ Data Models                          │
├─────────────────────────────────────────┤
│  AI Layer                               │
│  ─ Gemini API client                    │
│  ─ Function Calling for chat parsing    │
│  ─ Vision for receipt scanning          │
└─────────────────────────────────────────┘
```

### Data Flow

```
User Action → ViewModel → Repository → Firestore/Room
                    ↑                        ↓
                 StateFlow ← ← ← ← ← ← ← Response
```

---

## 3. Dependencies & Versions

These go in `app/build.gradle.kts`.

| Category | Library | Version | Purpose |
|----------|---------|---------|---------|
| **UI** | Jetpack Compose BOM | 2024.12.01 | UI toolkit |
| **UI** | Material 3 | (via BOM) | Design system |
| **UI** | Navigation Compose | 2.8.5 | Screen routing |
| **UI** | Icons Extended | (via BOM) | Extra icons |
| **Auth** | Firebase Auth | 23.1.0 | Google + Email sign-in |
| **Auth** | Credential Manager | 1.5.0 | Modern Google Sign-In |
| **Cloud** | Firestore | 25.1.1 | Database + sync |
| **AI** | Generative AI (Vertex) | 1.1.0 | Gemini API |
| **AI** | CameraX | 1.4.1 | Receipt camera |
| **AI** | ML Kit Barcode | 17.3.0 | Receipt barcode scan |
| **Network** | OkHttp | 4.12.0 | HTTP logging |
| **DI** | Hilt | 2.53.1 | Dependency injection |
| **Async** | Coroutines | 1.9.0 | Async operations |
| **Format** | Kotlin Serialization | 1.7.3 | JSON parsing |
| **Export** | Apache Commons CSV | 1.11.0 | CSV generation |
| **Export** | AndroidXExif | 1.3.7 | Photo metadata |
| **Build** | Secrets Gradle Plugin | 2.0.1 | API key security |

### Version Catalog (libs.versions.toml)

```toml
[versions]
compose-bom = "2024.12.01"
firebase-auth = "23.1.0"
firebase-firestore = "25.1.1"
gemini = "0.9.0"
camerax = "1.4.1"
hilt = "2.53.1"
coroutines = "1.9.0"
navigation = "2.8.5"
secrets = "2.0.1"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Firebase
firebase-auth = { group = "com.google.firebase", name = "firebase-auth", version.ref = "firebase-auth" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore", version.ref = "firebase-firestore" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics" }

# Gemini AI
generative-ai = { group = "com.google.ai.client.generativeai", name = "generativeai", version.ref = "gemini" }

# CameraX
camerax-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
camerax-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
camerax-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }

# Coroutines
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }

# Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.7.3" }

# Commons CSV
commons-csv = { group = "org.apache.commons", name = "commons-csv", version = "1.11.0" }

[plugins]
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
# ⚠️  ALSO add to app/build.gradle.kts plugins block: kotlin("kapt")
# ⚠️  ALSO add to app/build.gradle.kts dependencies: kapt(libs.hilt.compiler)
secrets = { id = "com.google.android.libraries.mapsplatform.secrets-gradle-plugin", version.ref = "secrets" }
```

---

## 4. Project Skeleton (Directory Tree)

```
saving-coach/
├── build.gradle.kts                    # Root build file (plugins)
├── settings.gradle.kts                 # Project settings
├── gradle.properties                   # JVM + AndroidX settings
├── local.properties                    # SDK path + API keys (gitignored)
├── .gitignore
├── gradle/
│   └── libs.versions.toml              # Version catalog
├── app/
│   ├── build.gradle.kts                # App module build config
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/savingcoach/app/
│       │   │   ├── SavingCoachApp.kt                 # Application class
│       │   │   ├── MainActivity.kt               # Single activity
│       │   │   ├── navigation/
│       │   │   │   ├── NavGraph.kt               # Navigation graph
│       │   │   │   └── Routes.kt                 # Route sealed class
│       │   │   ├── data/
│       │   │   │   ├── model/
│       │   │   │   │   ├── Expense.kt            # Expense data class
│       │   │   │   │   ├── Budget.kt             # Budget data class
│       │   │   │   │   └── ChatMessage.kt        # Chat history model
│       │   │   │   ├── repository/
│       │   │   │   │   ├── AuthRepository.kt     # Firebase Auth
│       │   │   │   │   ├── ExpenseRepository.kt  # Expenses CRUD
│       │   │   │   │   ├── BudgetRepository.kt   # Budget CRUD
│       │   │   │   │   └── ChatRepository.kt     # Chat history
│       │   │   │   └── firestore/
│       │   │   │       ├── FirestoreModule.kt    # Firestore DI
│       │   │   │       └── FirestorePaths.kt     # Collection paths
│       │   │   ├── ai/
│       │   │   │   ├── GeminiClient.kt           # Gemini API wrapper
│       │   │   │   ├── ChatParser.kt             # NLP expense parser
│       │   │   │   └── ReceiptScanner.kt          # Vision receipt reader
│       │   │   ├── export/
│       │   │   │   ├── CsvExporter.kt            # CSV generation
│       │   │   │   └── ShareManager.kt           # Email/share intent
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Theme.kt              # Material 3 theme
│       │   │   │   │   ├── Color.kt              # Color palette
│       │   │   │   │   └── Type.kt               # Typography
│       │   │   │   ├── auth/
│       │   │   │   │   ├── AuthScreen.kt         # Login screen
│       │   │   │   │   └── AuthViewModel.kt      # Auth logic
│       │   │   │   ├── dashboard/
│       │   │   │   │   ├── DashboardScreen.kt    # Main dashboard
│       │   │   │   │   ├── DashboardViewModel.kt
│       │   │   │   │   └── CalendarHeatmap.kt    # Calendar composable
│       │   │   │   ├── expenses/
│       │   │   │   │   ├── ExpenseListScreen.kt  # Expense list
│       │   │   │   │   ├── AddExpenseScreen.kt   # Manual add
│       │   │   │   │   └── ExpenseViewModel.kt
│       │   │   │   ├── camera/
│       │   │   │   │   ├── CameraScreen.kt       # CameraX receipt capture
│       │   │   │   │   └── CameraViewModel.kt
│       │   │   │   ├── chat/
│       │   │   │   │   ├── ChatScreen.kt         # AI chat view
│       │   │   │   │   └── ChatViewModel.kt
│       │   │   │   ├── budget/
│       │   │   │   │   ├── BudgetScreen.kt       # Budget settings
│       │   │   │   │   └── BudgetViewModel.kt
│       │   │   │   ├── settings/
│       │   │   │   │   ├── SettingsScreen.kt     # Profile + export
│       │   │   │   │   └── SettingsViewModel.kt
│       │   │   │   └── components/
│       │   │   │       ├── BudgetProgressBar.kt  # Reusable progress bar
│       │   │   │       ├── SpendingChart.kt      # Pie/bar chart
│       │   │   │       └── LoadingOverlay.kt     # Loading indicator
│       │   │   └── di/
│       │   │       ├── AppModule.kt              # Hilt app module
│       │   │       └── RepositoryModule.kt       # Repo bindings
│       │   └── res/
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   ├── colors.xml
│       │       │   └── themes.xml
│       │       ├── drawable/
│       │       │   └── ic_launcher_foreground.xml
│       │       └── xml/
│       │           ├── backup_rules.xml
│       │           └── network_security_config.xml
│       └── test/
│           └── java/com/savingcoach/app/
│               ├── repository/
│               │   └── ExpenseRepositoryTest.kt
│               └── ai/
│                   ├── ChatParserTest.kt
│                   └── ReceiptScannerTest.kt
```

---

## 5. Dev Dependency Map

### Summary: Only Dev 1 Blocks. Everyone Else Builds in Parallel.

Dev 1 delivers **models + repo interfaces + nav skeleton + CI/CD** in ~1.5 hours on Day 1.  
After that, Dev 2/3/4/5 all build **simultaneously with zero waiting** — nobody depends on Dev 3's Firestore implementations because everyone codes against **interfaces with in-memory mocks**.

---

### What Dev 1 Must Push First (Day 1, Hour 1-2)

| Deliverable | Time | Needed By |
|-------------|------|-----------|
| `Expense.kt`, `Budget.kt`, `SavingChallenge.kt`, `SavingsDeposit.kt`, `SavingsAnalytics.kt`, `ChatMessage.kt` | 20 min | **All devs** |
| `ExpenseRepository` interface | 8 min | Dev 4, Dev 5 |
| `SavingChallengeRepository` interface | 8 min | Dev 4 |
| `BudgetRepository` interface | 5 min | Dev 4 |
| `ChatRepository` interface | 5 min | Dev 2 |
| `AuthRepository` interface | 5 min | Dev 1, Dev 4, Dev 5 |
| `Routes.kt` + `NavGraph.kt` skeleton | 20 min | **All devs** |
| `.github/workflows/*.yml` + `proguard-rules.pro` + signing | 20 min | **All devs** (CI on first push) |
| **Total** | **~1.5 hrs** | |

---

### Dependency Diagram

```
                  ┌────────────────────────────┐
                  │         DEV 1              │
                  │  Writes models, interfaces, │
                  │  routes, CI/CD (~1.5 hrs)  │
                  └────────────┬───────────────┘
                               │ pushes to GitHub
                               ▼
         ┌─────────────────────────────────────────────┐
         │         ALL DEVS START IN PARALLEL           │
         │         (Hour 2 → Week 2-3)                  │
         └─────────────────────────────────────────────┘
               │        │        │        │
               ▼        ▼        ▼        ▼
         ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
         │ DEV 2  │ │ DEV 3  │ │ DEV 4  │ │ DEV 5  │
         │        │ │        │ │        │ │        │
         │ Chat   │ │Real    │ │Expense │ │CSV     │
         │ Camera │ │Firestore│ │Forms   │ │Export  │
         │ Gemini │ │Impl    │ │Budget  │ │Settings│
         │        │ │Hilt DI │ │Savings │ │        │
         │        │ │        │ │Screens │ │        │
         └────────┘ └────────┘ └────────┘ └────────┘
              │         │          │          │
              │         ▼          │          │
              └──── Week 2-3 ──────┘──────────┘
                          │
                          ▼
                   Swap mocks → real Firestore data
                   Integration test week
```

---

### Per-Dev Dependencies Table

| Dev | Needs From Dev 1 | Needs From Others? | Can Build Without Dev 3? | What They Use Instead |
|-----|-----------------|-------------------|-------------------------|----------------------|
| **Dev 1** | Nothing (writes it) | — | ✅ | Writes interfaces, dashboard uses mocks |
| **Dev 2** | `ChatMessage` + `ChatRepository` (10 min) | None | ✅ | In-memory chat list mock |
| **Dev 3** | All models + interfaces (30 min) | None | ✅ N/A — they build the real thing | — |
| **Dev 4** | 4 models + 3 repo interfaces (20 min) | None | ✅ | In-memory expense/savings mocks |
| **Dev 5** | `Expense` model + `ExpenseRepository` (10 min) | None | ✅ | Hardcoded test data |

---

### Integration Points (What to Watch For)

| Between | Shared Thing | Risk | Mitigation |
|---------|-------------|------|-----------|
| Dev 1 ⟷ Dev 3 | Repo interface signatures match implementations | **HIGH** | Dev 1 freezes interfaces after Day 1. Dev 3 must not change method names |
| Dev 1 ⟷ Dev 4 | UI components (BudgetProgressBar, etc.) | **LOW** | Dev 4 builds temporary versions, swaps in 10 min |
| Dev 2 ⟷ Dev 3 | ChatRepository mock vs real | **LOW** | Interface contract is fixed. Just plug and play |
| Dev 4 ⟷ Dev 3 | ExpenseRepository mock vs real | **LOW** | Same pattern |
| Dev 5 ⟷ Dev 3 | ExpenseRepository mock vs real | **LOW** | Same pattern |
| Dev 1 ⟷ All | NavGraph route names | **MEDIUM** | Agree on route strings before Dev 1 writes NavGraph |

---

### Timeline

```
Day 1, Hour 1-2   ── Dev 1 writes everything, pushes
Day 1, Hour 2+    ── Dev 2, 3, 4, 5 all start coding
Week 2-3          ── Dev 3 finishes real Firestore repos
Week 3            ── Everyone swaps mocks → real data
Week 3-4          ── Integration testing + bug fixes
Week 5-6          ── Polish, edge cases, offline testing
Week 7            ── CI/CD final check, beta release
```

**Bottom line:** Only Dev 1's ~1.5 hours of setup is a true dependency. After that, all 5 devs build in parallel for 2-3 weeks before needing to integrate.


---

## 6. Feature Breakdown by Developer

### Dev 1: UI Skeleton — Theme + Nav + Auth + Dashboard

This dev builds the **app shell** first. Everyone else depends on it.

**Files owned:**
- `SavingCoachApp.kt`                          # Application class (Hilt)
- `MainActivity.kt`                        # Single activity + Compose host
- `AndroidManifest.xml`                    # Manifest with all permissions
- `navigation/Routes.kt`                   # All route definitions
- `navigation/NavGraph.kt`                 # Navigation graph (all screens wired)
- `ui/theme/Color.kt`                      # Color palette (green/yellow/red budget colors)
- `ui/theme/Type.kt`                       # Typography scale
- `ui/theme/Theme.kt`                      # Material 3 theme connector
- `ui/auth/AuthScreen.kt`                  # Login screen (Google + email)
- `ui/auth/AuthViewModel.kt`               # Auth state management
- `ui/dashboard/DashboardScreen.kt`        # Main landing screen
- `ui/dashboard/DashboardViewModel.kt`     # Dashboard data aggregation
- `ui/dashboard/CalendarHeatmap.kt`        # Calendar with spending colors
- `ui/components/BudgetProgressBar.kt`     # Green/yellow/red progress bar
- `ui/components/SpendingChart.kt`         # Category pie/bar chart
- `ui/components/LoadingOverlay.kt`        # Loading spinner overlay
- `res/values/strings.xml`                 # App strings
- `res/values/colors.xml`                  # Theme colors XML
- `res/values/themes.xml`                  # XML theme fallback
- `.github/workflows/ci-pr-check.yml`      # PR workflow YAML
- `.github/workflows/ci-release.yml`       # Release workflow YAML
- `app/proguard-rules.pro`                 # ProGuard rules for release

> **CI/CD is Dev 1's job** because Dev 1 owns the project skeleton. Setting up GitHub Actions,
> signing configs, and proguard happens once at the start and rarely changes.

**What to build (in order):**

| Step | What | Why |
|------|------|-----|
| 1 | `Routes.kt` + `NavGraph.kt` | Define every screen route and nav graph with placeholder screens |
| 2 | `SavingCoachApp.kt` + `MainActivity.kt` + `AndroidManifest.xml` | App entry point, Hilt setup, bottom nav scaffold |
| 3 | Theme (`Color.kt`, `Type.kt`, `Theme.kt`) | Material 3 design tokens — everyone theming |
| 4 | `AuthScreen.kt` + `AuthViewModel.kt` | Login with Google + Email/Password |
| 5 | `DashboardScreen.kt` + `DashboardViewModel.kt` + `CalendarHeatmap.kt` | Main screen with calendar heatmap + budget progress |
| 6 | Reusable components (`BudgetProgressBar.kt`, `SpendingChart.kt`, `LoadingOverlay.kt`) | Shared UI building blocks |
| 7 | Repository interface contracts | Define these **before** step 1 — Dev 2/4/5 code against them |
| 8 | CI/CD GitHub Actions + ProGuard + signing config | Create `.github/workflows/ci-pr-check.yml`, `ci-release.yml`, `proguard-rules.pro`, and `build.gradle.kts` signing config — set up once, rarely changes |

### 🧩 Repository Interface Contracts (Define First)

These go in `data/repository/`. Dev 1 defines the **signatures only**. Dev 3 implements the real Firestore version later. Dev 2/4/5 code against these interfaces from day 1.

```kotlin
// ExpenseRepository.kt — for Dev 4 (expense forms) and Dev 5 (export)
interface ExpenseRepository {
    fun getExpensesForMonth(userId: String, yearMonth: String): Flow<List<Expense>>
    fun getExpensesForDate(userId: String, date: String): Flow<List<Expense>>
    fun getAllExpenses(userId: String): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): String   // returns expenseId
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)
}

// BudgetRepository.kt — for Dev 4 (budget settings) and Dev 1 (dashboard)
interface BudgetRepository {
    fun getBudget(userId: String, yearMonth: String): Flow<Budget?>
    suspend fun setBudget(userId: String, budget: Budget)
    suspend fun updateLimit(userId: String, yearMonth: String, newLimit: Double)
}

// ChatRepository.kt — for Dev 2 (AI chat)
interface ChatRepository {
    fun getChatHistory(userId: String): Flow<List<ChatMessage>>
    suspend fun saveMessage(userId: String, message: ChatMessage)
}

// SavingChallengeRepository.kt ← NEW — for Dev 4 (challenge/deposit screens) and Dev 1 (dashboard)
interface SavingChallengeRepository {
    fun getActiveChallenges(userId: String): Flow<List<SavingChallenge>>
    fun getAllChallenges(userId: String): Flow<List<SavingChallenge>>
    fun getDeposits(userId: String, challengeId: String): Flow<List<SavingsDeposit>>
    suspend fun createChallenge(challenge: SavingChallenge): String
    suspend fun addDeposit(userId: String, challengeId: String, deposit: SavingsDeposit)
    suspend fun completeChallenge(userId: String, challengeId: String)
    suspend fun deleteChallenge(userId: String, challengeId: String)
}

// AuthRepository.kt — for Dev 1 (auth screen)
interface AuthRepository {
    fun isUserSignedIn(): Boolean
    fun getCurrentUserId(): String?
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String): Result<User>
    suspend fun signOut()
}
```

**Why this matters:** With these interfaces defined, Dev 2 builds the chat against `ChatRepository`, Dev 4 builds expense forms against `ExpenseRepository` + saving screens against `SavingChallengeRepository`, Dev 5 builds export against `ExpenseRepository`, and Dev 1 builds the dashboard against `BudgetRepository` + `SavingChallengeRepository` — **all without waiting for Dev 3's Firestore code**.

### 🧪 Mock Repositories (Shipped by Dev 1)

Dev 1 also provides in-memory mock implementations so the app compiles and runs immediately:

| File | Path |
|------|------|
| `MockExpenseRepository` | `data/mock/MockRepositories.kt` |
| `MockBudgetRepository` | `data/mock/MockRepositories.kt` |
| `MockChatRepository` | `data/mock/MockRepositories.kt` |
| `MockSavingChallengeRepository` | `data/mock/MockRepositories.kt` |
| `MockAuthRepository` | `data/mock/MockRepositories.kt` |

These are wired via `RepositoryModule.kt` (`di/RepositoryModule.kt`) using Hilt `@Binds`.
Dev 3 replaces them with real Firestore implementations — no changes needed in ViewModels or UI.

**To swap for real repos later:** Just change the `@Binds` target in `RepositoryModule.kt`.


**Calendar heatmap color logic:**
```
Each day cell based on: daily_spending / (monthly_budget / 30)
  < 50%  → Green  (#4CAF50)
  50-80% → Yellow (#FFC107)
  80-100%→ Orange (#FF9800)
  > 100% → Red    (#F44336)
```

---

### Dev 2: AI Chat + Receipt Scanner

**Files owned:**
- `ai/GeminiClient.kt`                    # Gemini API wrapper
- `ai/ChatParser.kt`                       # NLP → structured expense
- `ai/ReceiptScanner.kt`                  # Vision receipt reader
- `ui/chat/ChatScreen.kt`                 # Chat UI (messages, input, typing)
- `ui/chat/ChatViewModel.kt`              # Chat + expense parsing state
- `ui/camera/CameraScreen.kt`             # CameraX receipt photo
- `ui/camera/CameraViewModel.kt`          # Camera + image capture state

**What to build:**
1. **GeminiClient** — wrapper around Gemini 2.5 Flash API
   - Load API key from `local.properties` via Secrets Gradle Plugin
   - `parseExpenseFromText(message: String): ParsedExpense?`
   - `scanReceipt(imageBitmap: Bitmap): ParsedExpense?`
   - `getAdvice(query: String, context: BudgetContext): String`
2. **Receipt parsing** — structured output via Gemini Function Calling:
   ```json
   {
     "merchant": "Cafe A",
     "amount": 4500,
     "category": "Food",
     "date": "2026-07-24",
     "items": ["Iced Coffee - 2500", "Sandwich - 2000"]
   }
   ```
3. **Chat Screen** — message list with typing indicator
   - "Spent 4500 on lunch at Cafe" → parses → preview → confirm → saves to Firestore
   - "Do I have enough for iced coffee?" → AI checks budget → responds
   - Chat history persisted
4. **CameraX Screen** — take receipt photo → crop → Gemini Vision → structured result
5. **Chat history** — persist via ChatRepository (defined by Dev 3)

---

### Dev 3: Data Layer — Auth + Firestore + Repositories

**Files owned:**
- `data/model/Expense.kt`                  # Expense data class
- `data/model/Budget.kt`                   # Budget data class
- `data/model/ChatMessage.kt`              # Chat message data class
- `data/repository/AuthRepository.kt`      # Firebase Auth wrapper
- `data/repository/ExpenseRepository.kt`   # Expenses CRUD + real-time sync
- `data/repository/BudgetRepository.kt`    # Budget CRUD + spending totals
- `data/repository/ChatRepository.kt`      # Chat history read/write
- `data/firestore/FirestoreModule.kt`      # Firestore DI provider
- `data/firestore/FirestorePaths.kt`       # Collection/document path constants
- `di/AppModule.kt`                        # Hilt app-wide bindings
- `di/RepositoryModule.kt`                 # Hilt repository bindings

**What to build:**
1. All data classes (Expense, Budget, ChatMessage) with `@Serializable`
2. Firebase Auth — Google Sign-In + Email/Password
3. Firestore CRUD — create, read, update, delete for expenses
4. Firestore real-time sync — `SnapshotListener` for live updates
5. Budget logic — compute `totalSpent` from expense list for current month
6. Offline persistence — Firestore disk cache enabled
7. Hilt modules — bind all repositories, Firestore, Auth

**Key decisions:**
- Data path: `users/{userId}/expenses/{expenseId}`
- Budget path: `users/{userId}/budgets/{YYYY-MM}`
- Chat path: `users/{userId}/chat/{messageId}`
- Use Firestore `SnapshotListener` for real-time dashboard updates
- Repositories return `Flow<T>` for reactive ViewModels
- Implement the interface contracts defined by Dev 1

---

### Dev 4: Expense Forms + Budget Settings

**Files owned:**
- `ui/expenses/ExpenseListScreen.kt`       # Expense list with search/filter
- `ui/expenses/AddExpenseScreen.kt`        # Manual expense form
- `ui/expenses/ExpenseViewModel.kt`        # Expense list + add state
- `ui/budget/BudgetScreen.kt`              # Budget limit setting
- `ui/budget/BudgetViewModel.kt`           # Budget state

**What to build:**
1. **Expense List Screen**
   - Paginated list (newest first), pull-to-refresh
   - Swipe-to-delete with confirmation dialog
   - Tap to edit existing expense
   - Filter by category dropdown and month picker
   - Search by merchant name
2. **Add Expense Screen (Manual)**
   - Form: Amount, Category dropdown with icons, Merchant, Date picker, Notes
   - Validation: amount > 0, category required
   - Save → calls ExpenseRepository (from Dev 3)
3. **Budget Settings Screen**
   - Set monthly limit (numeric keyboard, formatted as currency)
   - View current spending vs limit
   - Edit existing limit
   - Reset for new month
4. **Threshold Alerts**
   - Local notification at 75% and 90% of budget
   - Dashboard banner when over budget (uses Dev 1's components)

**Expense categories:**
```kotlin
enum class ExpenseCategory(val displayName: String) {
    FOOD("Food & Drinks"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    BILLS("Bills & Utilities"),
    ENTERTAINMENT("Entertainment"),
    EDUCATION("Education"),
    HEALTH("Health"),
    OTHER("Other")
}
```

---

### Dev 5: Export, Settings + Release

**Files owned:**
- `export/CsvExporter.kt`                  # CSV generation from expenses
- `export/ShareManager.kt`                 # Android share + email intent
- `ui/settings/SettingsScreen.kt`          # Profile + export + about
- `ui/settings/SettingsViewModel.kt`       # Settings state

**What to build:**
1. **CSV Export**
   - Export all expenses (or filtered by month) as CSV
   - Columns: Date, Category, Merchant, Amount, Currency, Notes
   - Save to Downloads via Storage Access Framework (SAF)
2. **Share / Email**
   - Share CSV via Android share intent (Gmail, Telegram, etc.)
   - Direct email with CSV attachment
3. **Settings Screen**
   - Profile card: name, email, photo (from Google Auth)
   - Export data button → triggers CSV export
   - Sign out button → navigate to Auth
   - App version + "About" section
4. **APK distribution** — Upload signed builds to GitHub Releases for beta testers

---

## 7. Firebase Setup Guide

### Step 1: Create Firebase Project

1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Click **Create project** → name it `saving-coach`
3. Disable Google Analytics (optional)
4. Wait for project creation

### Step 2: Register Android App

1. In Firebase Console, click **Android icon** to add app
2. Package name: `com.savingcoach.app`
3. App nickname: `Piggy`
4. Download `google-services.json` → place in `app/` directory
5. Click **Next** (skip remaining steps — our build.gradle already has the plugin)

### Step 3: Enable Authentication

1. Firebase Console → **Authentication** → **Sign-in method**
2. Enable **Google** → configure OAuth consent (use default)
3. Enable **Email/Password**
4. Under **Authorized domains**, add your domain (or use default)

### Step 4: Create Firestore Database

1. Firebase Console → **Firestore Database** → **Create database**
2. Choose **Start in test mode** (we'll lock it down)
3. Select region (e.g., `asia-southeast1` for Myanmar)

### Step 5: Deploy Security Rules

Replace the default rules with these:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User data: only the authenticated user can access their own data
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null
                        && request.auth.uid == userId;
    }

    // Validate expense fields on write
    match /users/{userId}/expenses/{expenseId} {
      allow write: if request.auth != null
                  && request.auth.uid == userId
                  && request.resource.data.keys().hasAll(['amount', 'category', 'date', 'createdAt']);
      allow read: if request.auth != null
                 && request.auth.uid == userId;
    }
  }
}
```

### Step 6: Enable App Check (Recommended)

1. Firebase Console → **App Check** → **Enforce** → **Add**
2. Use **Play Integrity** (if releasing on Play Store) or **SafetyNet**

---

## 8. Gemini API Setup

### Get an API Key

1. Go to [aistudio.google.com/apikey](https://aistudio.google.com/apikey)
2. Click **Create API Key**
3. Copy the key

### Add to local.properties

```
gemini.api.key=AIzaSyYourActualKeyHere
```

### Secrets Gradle Plugin

The plugin reads `gemini.api.key` from `local.properties` and exposes it as a BuildConfig field:

```kotlin
// In app/build.gradle.kts:
secrets {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}
```

Then in code:

```kotlin
val apiKey = BuildConfig.GEMINI_API_KEY
```

> **⚠️ Security Warning:** `local.properties` is listed in `.gitignore`. It NEVER gets committed. The API key is only on local dev machines.

### 🔄 How Other Devs Get Their Keys

Since `local.properties` and `google-services.json` are **gitignored**, each dev must set up their own:

| File | How to get it | Committed? |
|------|--------------|:----------:|
| `local.properties` | Copy `local.defaults.properties` → `local.properties`, then fill in your SDK path + Gemini API key | ❌ No |
| `google-services.json` | Download from Firebase Console (`Project Settings → Your apps → Download`) | ❌ No |
| `local.defaults.properties` | ✅ **Already in repo** — contains placeholder template | ✅ **Yes** |

**Quick setup for a new dev machine:**
```bash
cp local.defaults.properties local.properties
# Then edit local.properties:
#   sdk.dir=/Users/yourname/Library/Android/sdk
#   gemini.api.key=AIzaSy...
```

> **Tip:** The Google Services Gradle plugin generates a `BuildConfig` field for the API key automatically. No need to share keys in chat — each dev uses their own. 

---

## 9. Data Models (Firestore Schema)

### Collection: `users/{userId}/expenses/{expenseId}`

| Field | Type | Example | Notes |
|-------|------|---------|-------|
| `amount` | `number` | `4500` | In MMK (or user's currency) |
| `category` | `string` | `"Food"` | From ExpenseCategory enum |
| `merchant` | `string` | `"Cafe A"` | Optional |
| `description` | `string` | `"Lunch with friends"` | Optional |
| `date` | `timestamp` | `July 24, 2026` | Date of expense |
| `createdAt` | `timestamp` | `July 24, 2026` | Auto-set server timestamp |
| `updatedAt` | `timestamp` | `July 24, 2026` | Auto-updated |
| `source` | `string` | `"manual"`, `"chat"`, `"receipt"` | How it was entered |
| `currency` | `string` | `"MMK"` | Currency code |

### Collection: `users/{userId}/budgets/{monthId}`

`monthId` format: `YYYY-MM` (e.g., `2026-07`)

| Field | Type | Example | Notes |
|-------|------|---------|-------|
| `limit` | `number` | `100000` | Monthly budget limit |
| `totalSpent` | `number` | `45000` | Computed from expenses |
| `month` | `string` | `"2026-07"` | YYYY-MM format |
| `createdAt` | `timestamp` | | |
| `updatedAt` | `timestamp` | | |

### Collection: `users/{userId}/chat/{messageId}`

| Field | Type | Example | Notes |
|-------|------|---------|-------|
| `role` | `string` | `"user"` or `"ai"` | Who sent it |
| `content` | `string` | `"Spent 4500 on lunch"` | Message text |
| `parsedExpense` | `map` | `{amount: 4500, ...}` | Parsed expense data (nullable) |
| `timestamp` | `timestamp` | | |
| `type` | `string` | `"expense"`, `"query"`, `"advice"` | Message type |

---

## 10. Navigation Routes

```kotlin
sealed class Route(val route: String) {
    object Auth : Route("auth")
    object Dashboard : Route("dashboard")
    object Expenses : Route("expenses")
    object AddExpense : Route("add_expense")
    object Chat : Route("chat")
    object Camera : Route("camera")
    object Budget : Route("budget")
    object Settings : Route("settings")
}
```

### Navigation Flow

```
                    ┌─────────────┐
                    │   Splash    │
                    │  (optional) │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
              ┌─────│  Authenticated? │─────┐
              │     └──────┬──────┘     │
              │            │ yes        │ no
              │     ┌──────▼──────┐     │
              │     │  Main Screen│     │
              │     │ (Nav Host)  │     │
              │     └──────┬──────┘     │
              │            │            │
        ┌─────┼─────┬──────┼──────┬─────┼─────┐
        │     │     │      │      │     │     │
   ┌────▼──┐ ┌▼───┐ ┌▼───┐ ┌▼───┐ ┌▼───┐ ┌▼───┐
   │Dashboard│ │Exp.│ │Chat│ │Cam.│ │Budg│ │Sett.│
   │        │ │List│ │    │ │    │ │et  │ │ings │
   └────────┘ └──┬─┘ └──┬─┘ └────┘ └────┘ └─────┘
                 │      │
           ┌─────▼──┐   │
           │Add Exp.│   │
           │(Manual)│   │
           └────────┘   │
                   ┌────▼────┐
                   │Receipt  │
                   │Result   │
                   └─────────┘
```

### Bottom Navigation Tabs

| Tab | Icon | Route | Badge |
|-----|------|-------|-------|
| Dashboard | `Icons.Dashboard` | `dashboard` | — |
| Expenses | `Icons.Receipt` | `expenses` | Count of today's expenses |
| Chat | `Icons.Chat` | `chat` | — |
| Settings | `Icons.Settings` | `settings` | — |

---

## 11. Build & Run

### Development Build

```bash
# Debug APK (for testing)
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

```bash
# Create keystore (one time)
keytool -genkey -v -keystore release.keystore \
  -alias savingcoach -keyalg RSA -keysize 2048 \
  -validity 10000

# Release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### Running on Emulator

```bash
# List available emulators
emulator -list-avds

# Start emulator (replace with your AVD name)
emulator -avd Pixel_9_API_35

# Build and install
./gradlew installDebug
```

### Running on Physical Device

1. Enable **Developer Options** on your Android phone
2. Enable **USB Debugging**
3. Connect via USB
4. Run: `./gradlew installDebug`

---

## 12. CI/CD Pipeline (GitHub Actions)

### Overview

Two automated workflows:

| Workflow | Trigger | What it does |
|----------|---------|-------------|
| **PR Check** | Every push to any `feature/*` branch + PR to `develop` | Lint → Unit tests → Build debug APK → Upload as artifact |
| **Release** | Tag `v*` pushed (e.g. `v1.0.0-beta`) | Build signed release APK → Create GitHub Release → Upload APK |

---

### Workflow 1: PR Check (`ci-pr-check.yml`)

Create `.github/workflows/ci-pr-check.yml`:

```yaml
name: PR Check

on:
  pull_request:
    branches: [ develop, main ]
  push:
    branches: [ develop ]

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  lint:
    name: Lint
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run lint
        run: ./gradlew lintDebug
        continue-on-error: true    # Don't block PR — warnings are advisory

      - name: Upload lint report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: lint-report
          path: app/build/reports/lint-results-debug.html

  test:
    name: Unit Tests
    runs-on: ubuntu-latest
    timeout-minutes: 15
    needs: lint
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run unit tests
        run: ./gradlew testDebugUnitTest

      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: app/build/reports/tests/testDebugUnitTest/

  build:
    name: Build Debug APK
    runs-on: ubuntu-latest
    timeout-minutes: 20
    needs: test
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Google Services
        run: |
          # Decode the base64-encoded google-services.json from GitHub Secrets
          echo '${{ secrets.GOOGLE_SERVICES_JSON }}' > app/google-services.json

      - name: Setup Local Properties
        run: |
          echo "sdk.dir=/Users/yadanar/Library/Android/sdk" > local.properties
          echo "gemini.api.key=${{ secrets.GEMINI_API_KEY }}" >> local.properties

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

### Workflow 2: Release (`ci-release.yml`)

Create `.github/workflows/ci-release.yml`:

```yaml
name: Release APK

on:
  push:
    tags:
      - 'v*'    # e.g. v1.0.0, v1.2.0-beta

jobs:
  release:
    name: Build Signed Release APK
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Decode Keystore
        run: |
          echo '${{ secrets.KEYSTORE_BASE64 }}' | base64 -d > release.keystore

      - name: Setup Google Services
        run: |
          echo '${{ secrets.GOOGLE_SERVICES_JSON }}' > app/google-services.json

      - name: Setup Local Properties
        run: |
          echo "sdk.dir=/Users/yadanar/Library/Android/sdk" > local.properties
          echo "gemini.api.key=${{ secrets.GEMINI_API_KEY }}" >> local.properties

      - name: Build signed release APK
        run: ./gradlew assembleRelease
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      - name: Sign APK (verify alignment)
        run: |
          apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          name: Release ${{ github.ref_name }}
          body_path: CHANGELOG.md
          files: |
            app/build/outputs/apk/release/app-release.apk
          prerelease: ${{ contains(github.ref_name, 'beta') || contains(github.ref_name, 'rc') }}
```

---

### Setting Up GitHub Secrets

Before CI works, add these **GitHub Secrets** (Settings → Secrets and variables → Actions → New repository secret):

| Secret Name | Value | How to Get It |
|-------------|-------|--------------|
| `GOOGLE_SERVICES_JSON` | Contents of `google-services.json` | `cat app/google-services.json \| base64` then paste the encoded string |
| `GEMINI_API_KEY` | Your Gemini API key | From [aistudio.google.com/apikey](https://aistudio.google.com/apikey) |
| `KEYSTORE_BASE64` | Keystore file as base64 | `base64 -i release.keystore` (macOS) or `base64 release.keystore` (Linux) — paste the output |
| `KEYSTORE_PASSWORD` | Keystore password | The password you set when creating the keystore |
| `KEY_ALIAS` | Key alias | Default: `savingcoach` (or whatever you used in `keytool -alias`) |
| `KEY_PASSWORD` | Key password | Usually same as keystore password |

> **⚠️ Security:** These secrets are encrypted and never exposed in logs. Only workflows on `main` and tagged releases can access the keystore secrets.

---

### Generating the Keystore (One-Time)

Run this locally — only Dev 5 (or the team lead) does this:

```bash
# In andriodpj/ directory
keytool -genkey -v -keystore release.keystore \
  -alias savingcoach \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YourKeystorePass \
  -keypass YourKeyPass

# Verify it
keytool -list -v -keystore release.keystore -storepass YourKeystorePass

# Encode for GitHub Secret (macOS)
base64 -i release.keystore | pbcopy   # copies to clipboard

# KEEP THIS FILE SAFE — without it you cannot update the app
```

---

### Adding Signing Config to `app/build.gradle.kts`

The app module needs signing config that reads from env vars (CI) or local (dev):

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "local-dev-pass"
            keyAlias = System.getenv("KEY_ALIAS") ?: "savingcoach"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "local-dev-pass"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"  // 🔴 REMOVE for Firebase (must match google-services.json package)
        }
    }
}
```

---

### Firebase App Distribution (Optional — Beta Testing)

Instead of GitHub Releases for beta, you can push directly to testers:

```yaml
# Add to ci-release.yml after assembleRelease
- name: Upload to Firebase App Distribution
  uses: wzieba/Firebase-Distribution-Github-Action@v1
  with:
    appId: ${{ secrets.FIREBASE_APP_ID }}
    serviceCredentialsFileContent: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
    groups: beta-testers
    file: app/build/outputs/apk/release/app-release.apk
    releaseNotes: "See CHANGELOG.md"
```

To get `FIREBASE_APP_ID` and `FIREBASE_SERVICE_ACCOUNT`:
1. Firebase Console → Project Settings → General → Your apps → Android → App ID
2. Firebase Console → Project Settings → Service Accounts → Generate new private key → Download JSON → `base64` encode it

---

### CI Dashboard & Badges

Add these to your `README.md` to show build status:

```markdown
![PR Check](https://github.com/<your-org>/<your-repo>/actions/workflows/ci-pr-check.yml/badge.svg)
![Release](https://github.com/<your-org>/<your-repo>/actions/workflows/ci-release.yml/badge.svg)
```

Replace `<your-org>` and `<your-repo>` with your actual GitHub path.

---

### CI Flow Summary

```
Push to feature/auth-firebase
         │
         ▼
    ┌─────────────┐
    │  CI: PR Check  │
    │  ├─ lint       │ ← catches style issues
    │  ├─ test       │ ← catches logic bugs
    │  └─ build debug│ ← catches compilation errors
    └──────┬─────────┘
           │ all green
           ▼
    Dev creates PR → Teammate reviews → Merge to develop
           │
           ▼
    (weekly) Merge develop → main
           │
           ▼
    Tag v1.0.0 on main push
           │
           ▼
    ┌────────────────┐
    │  CI: Release     │
    │  ├─ sign APK     │ ← production ready
    │  ├─ verify certs │
    │  └─ GitHub Release│ ← APK downloadable
    └────────────────┘
```

---

## Team Workflow

### Git Branch Strategy

```
main
  └── develop
       ├── feature/ui-skeleton              ← Dev 1 (theme, nav, auth, dashboard)
       ├── feature/ai-chat-receipt          ← Dev 2 (Gemini, chat, camera)
       ├── feature/data-layer               ← Dev 3 (models, repos, Firestore, DI)
       ├── feature/expense-budget           ← Dev 4 (forms, list, budget settings)
       └── feature/export-settings-ci       ← Dev 5 (export, settings, CI/CD)
```

### PR Process

1. Each dev works on their feature branch
2. Create PR to `develop` branch
3. At least 1 other dev reviews
4. After review, merge to `develop`
5. Weekly: merge `develop` → `main` for release

### Integration Points (Watch for Conflicts)

| Between | Shared Thing | Risk |
|---------|-------------|------|
| Dev 1 ⟷ Dev 3 | Repository interface signatures | **HIGH** — Dev 1 defines them, Dev 3 implements them. Any signature change breaks Dev 2/4/5 too |
| Dev 1 ⟷ All | NavGraph route names | **MEDIUM** — Agree on route strings before Dev 1 writes NavGraph |
| Dev 4 ⟷ Dev 1 | Reusable UI components | **LOW** — Dev 4 can build temp versions, replace in 10 min |

### Recommended Weekly Schedule (8-week sprint)

| Week | Focus |
|------|-------|
| 1 | Environment setup, data models, Firestore schema, navigation skeleton |
| 2 | Auth screens, dashboard scaffold, Gemini API proof-of-concept |
| 3 | Expense CRUD (manual + list), chat screen UI, calendar heatmap |
| 4 | Budget settings, AI chat parsing, receipt scanning |
| 5 | Export CSV, email share, notification thresholds |
| 6 | Polish, edge cases, offline testing |
| 7 | CI/CD, signing, beta testing |
| 8 | Bug fixes, Firebase production rules, GitHub release |

---

## Common Pitfalls & Tips

| Issue | Solution |
|-------|----------|
| `Failed to resolve: com.google.firebase` | Ensure `google-services.json` is in `app/` folder |
| Gemini API `PERMISSION_DENIED` | Check API key in `local.properties` and enable Generative Language API in Google Cloud Console |
| Firestore too slow on first launch | Enable offline persistence: `FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()` |
| CameraX preview not showing | Add `android:usesCleartextTraffic="true"` to manifest (for dev) |
| Compose recompilation issues | Run `./gradlew clean` and rebuild |
| Gradle sync failed | Check internet connection, then `File → Invalidate Caches → Restart` |
| `java.io.IOException: Cleartext HTTP traffic` | Add `android:usesCleartextTraffic="true"` in AndroidManifest.xml for dev, or implement network_security_config.xml |
| `Could not find generativeai:1.1.0` | Guide had wrong version — use `0.9.0` (check latest on Google Maven) |
| Hilt `@HiltAndroidApp` fails | Add `kotlin("kapt")` to plugins and `kapt(libs.hilt.compiler)` to dependencies in `app/build.gradle.kts` |
| Firebase "No matching client" error | Remove `applicationIdSuffix = ".debug"` — Firebase package must match exactly |
| Dagger "MissingBinding" errors | Create mock repos + `RepositoryModule.kt` with `@Binds` — see Mock Repositories section above |

---

> **Next Step for You:** Create the Firebase project, download `google-services.json`, place it in `app/`, and tell the team to run `./gradlew assembleDebug` to verify the build works. Then branch out!

---

*Generated for the AI Personal Finance Manager — Team of 5*
