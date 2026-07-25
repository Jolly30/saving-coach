# 🐷 Saving Coach

> **AI-Powered Personal Finance Manager** — Take control of your money.

![PR Check](https://github.com/Jolly30/saving-coach/actions/workflows/ci-pr-check.yml/badge.svg)
![Release](https://github.com/Jolly30/saving-coach/actions/workflows/ci-release.yml/badge.svg)

---

## ✨ Features

| Feature | Description | Dev |
|---------|-------------|:---:|
| 📊 **Dashboard** | Budget progress, calendar heatmap, spending overview | Dev 1 |
| 🔐 **Auth** | Google Sign-In + Email/Password | Dev 1 |
| 🤖 **AI Chat** | Natural language expense logging via Gemini AI | Dev 2 |
| 📸 **Receipt Scanner** | Snap a receipt → AI extracts data automatically | Dev 2 |
| 💰 **Expense Tracker** | Manual add, edit, swipe-delete, search & filter | Dev 4 |
| 📅 **Budget Planner** | Set monthly limits, track spending vs budget | Dev 4 |
| 🎯 **Saving Challenges** | Set goals, track deposits, visualize progress | Dev 4 |
| 📤 **CSV Export** | Export data, share via email | Dev 5 |
| ⚙️ **Settings** | Profile, export, sign out | Dev 5 |

---

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin 2.0.21 |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Repository Pattern |
| **DI** | Hilt 2.53.1 |
| **Navigation** | Navigation Compose 2.8.5 |
| **Backend** | Firebase Auth + Firestore |
| **AI** | Gemini API (generativeai 0.9.0) |
| **Camera** | CameraX 1.4.1 |
| **Build** | Gradle 8.11.1, AGP 8.8.0 |
| **CI/CD** | GitHub Actions |

---

## 🚀 Quick Start

> You need **5 things** to get started. Steps 1-3 are **manual** (one-time install on your machine). Steps 4-5 are project-specific.

### ⚙️ Step 1: Install Android Studio (One-Time)

- Download: [developer.android.com/studio](https://developer.android.com/studio)
- Version: **Ladybug (2024.3+)**
- Open Android Studio → SDK Manager → Install **Android SDK API 35** + **Build-Tools**

### ☕ Step 2: Install JDK 17 (One-Time)

**Mac (Homebrew):**
```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc
java -version
```

**Windows (Git Bash):**
```bash
# Option A: Download from Oracle
# Go to https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
# Download Windows x64 .msi → install → restart Git Bash

# Option B: Using Chocolatey (if installed)
choco install openjdk.17

# Check version
java -version
```

> **⚠️ Git Bash users:** If `java` is not found after installing, add it manually:
> ```bash
# echo 'export JAVA_HOME="/c/Program Files/Java/jdk-17"' >> ~/.bashrc
# echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bashrc
# source ~/.bashrc
# ```

### 🔑 Step 3: Get Your API Keys (One-Time)

| Key | Where to Get It |
|-----|----------------|
| **Gemini API Key** | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) — click "Create API Key" |
| **Firebase google-services.json** | [console.firebase.google.com](https://console.firebase.google.com) → Project Settings → Your apps → Download |

### 📦 Step 4: Clone & Setup

```bash
git clone https://github.com/Jolly30/saving-coach.git
cd saving-coach
```

**For ALL platforms (Mac, Windows Git Bash, Linux):**
```bash
cp local.defaults.properties local.properties
```

Then edit `local.properties` with your values:

```properties
sdk.dir=C\\:/Users/YOUR_USERNAME/AppData/Local/Android/sdk
gemini.api.key=AIzaSy...
```

> **🪟 Windows path note:** Use forward slashes `C:/Users/...` or escaped backslashes `C:\\Users\\...`

Then **place** `google-services.json` (from Firebase) into the `app/` folder.

### 🏃 Step 5: Build & Run

```bash
# Gradle auto-downloads ALL dependencies (no manual install needed!)
./gradlew assembleDebug
```

> ✅ **Done.** Gradle will download everything else automatically — Compose, Firebase, Hilt, Gemini SDK, CameraX, etc.

### 🔄 What Auto-Installs vs What's Manual

| Item | Auto-Installed? | How |
|------|:---------------:|-----|
| Android Studio | ❌ **Manual** | Download from developer.android.com |
| JDK 17 | ❌ **Manual** | `brew install`, Oracle download, or `choco install` |
| Android SDK 35 | ❌ **Manual** | Android Studio SDK Manager |
| Gemini API key | ❌ **Manual** | Get from aistudio.google.com |
| Firebase config | ❌ **Manual** | Download from Firebase Console |
| **Gradle 8.11.1** | ✅ **Auto** | `gradle-wrapper.jar` downloads it |
| **All dependencies** | ✅ **Auto** | `libs.versions.toml` → Gradle resolves them |
| **Compose, Firebase, Hilt, etc.** | ✅ **Auto** | Listed in `libs.versions.toml`, downloaded from Google Maven |

---

## 👥 Team

| Dev | Role | Branch | What They Build |
|:---:|------|--------|----------------|
| **1** | UI Skeleton | `feature/ui-skeleton` ✅ Done | Theme, Navigation, Auth, Dashboard, CI/CD |
| **2** | AI & Camera | `feature/ai-chat-receipt` | Gemini Chat, Receipt Scanner, Camera |
| **3** | Data Layer | `feature/data-layer` | Firestore repos, Auth, DI modules |
| **4** | Forms & Budget | `feature/expense-budget` | Expense CRUD, Budget, Savings |
| **5** | Export & Release | `feature/export-settings-ci` | CSV Export, Settings, APK signing |

### 🔄 How We Work Together

```
Dev 1 defines interfaces + mocks (shipped) → All devs code in parallel
Dev 3 builds real Firestore later → Others swap mocks with zero code changes
```

### 🌿 Git Workflow

```bash
# Each dev clones and checks out their branch:
git clone https://github.com/Jolly30/saving-coach.git
cd saving-coach
git checkout feature/your-branch-name

# Make changes, then:
git add .
git commit -m "Describe your change"
git push origin feature/your-branch-name

# Then create a Pull Request to develop on GitHub
```

---

## 📁 Project Structure

```
app/src/main/java/com/savingcoach/app/
├── SavingCoachApp.kt           # Hilt Application class
├── MainActivity.kt             # Single activity + bottom nav
├── navigation/                 # Routes + NavGraph
│   ├── Routes.kt
│   └── NavGraph.kt
├── data/
│   ├── model/                  # 6 data classes
│   ├── repository/             # 5 interfaces
│   ├── mock/                   # In-memory mocks (Dev 1)
│   └── firestore/              # Firestore (Dev 3)
├── ai/                         # Gemini client (Dev 2)
├── export/                     # CSV exporter (Dev 5)
├── ui/
│   ├── theme/                  # Material 3 theme
│   ├── auth/                   # Login screen
│   ├── dashboard/              # Main dashboard
│   ├── expenses/               # Expense list + form (Dev 4)
│   ├── camera/                 # Receipt camera (Dev 2)
│   ├── chat/                   # AI chat (Dev 2)
│   ├── budget/                 # Budget settings (Dev 4)
│   ├── settings/               # Settings + export (Dev 5)
│   └── components/             # Shared UI components
└── di/                         # Hilt modules
```

---

## 🌿 Branch Strategy

```
main
  └── develop
       ├── feature/ui-skeleton           ← Dev 1 ✅ Done
       ├── feature/ai-chat-receipt       ← Dev 2
       ├── feature/data-layer            ← Dev 3
       ├── feature/expense-budget        ← Dev 4
       └── feature/export-settings-ci    ← Dev 5
```

---

## 📱 Screens

| Screen | Route | Status |
|--------|-------|--------|
| Auth / Login | `auth` | ✅ Dev 1 |
| Dashboard | `dashboard` | ✅ Dev 1 |
| Expense List | `expenses` | ⏳ Dev 4 |
| Add Expense | `add_expense` | ⏳ Dev 4 |
| AI Chat | `chat` | ⏳ Dev 2 |
| Receipt Camera | `camera` | ⏳ Dev 2 |
| Budget Settings | `budget` | ⏳ Dev 4 |
| Settings | `settings` | ⏳ Dev 5 |

---

## 🔒 Security

- **API keys**: Stored in `local.properties` (gitignored)
- **Firebase config**: `google-services.json` (gitignored)
- **Auth**: Firestore rules require `request.auth.uid == userId`
- **Secrets template**: `local.defaults.properties` shows what's needed

---

## 📄 License

Private — Internal team use.

---

*Built with ❤️ by Jolly30 & Team*
