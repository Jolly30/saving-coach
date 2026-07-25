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

### Prerequisites
- Android Studio Ladybug (2024.3+)
- JDK 17
- Android SDK API 35

### Setup

```bash
# 1. Clone
git clone https://github.com/Jolly30/saving-coach.git
cd saving-coach

# 2. Set up local properties
cp local.defaults.properties local.properties
# Edit local.properties → add your Android SDK path + Gemini API key

# 3. Firebase
# Download google-services.json from Firebase Console → place in app/

# 4. Build & run
./gradlew assembleDebug
```

> **Don't have a Gemini API key?** Get one free at [aistudio.google.com/apikey](https://aistudio.google.com/apikey)

---

## 👥 Team

| Dev | Role | Branch | What They Build |
|:---:|------|--------|----------------|
| **1** | UI Skeleton | `feature/ui-skeleton` | Theme, Navigation, Auth, Dashboard, CI/CD |
| **2** | AI & Camera | `feature/ai-chat-receipt` | Gemini Chat, Receipt Scanner, Camera |
| **3** | Data Layer | `feature/data-layer` | Firestore repos, Auth, DI modules |
| **4** | Forms & Budget | `feature/expense-budget` | Expense CRUD, Budget, Savings |
| **5** | Export & Release | `feature/export-settings-ci` | CSV Export, Settings, APK signing |

### 🔄 How We Work Together

```
Dev 1 defines interfaces + mocks (shipped) → All devs code in parallel
Dev 3 builds real Firestore later → Others swap mocks with zero code changes
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
