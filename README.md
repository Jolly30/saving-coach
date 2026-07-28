# 🐷 Saving Coach

> **AI-Powered Personal Finance Manager** — Take control of your money.

![PR Check](https://github.com/Jolly30/saving-coach/actions/workflows/ci-pr-check.yml/badge.svg)
![Release](https://github.com/Jolly30/saving-coach/actions/workflows/ci-release.yml/badge.svg)

---

## ✨ Features

| Feature | Description | Status |
|---------|-------------|:------:|
| 📊 **Dashboard** | Budget progress, calendar heatmap, spending overview | ✅ Done |
| 🔐 **Auth** | Google Sign-In + Email/Password | ✅ Done |
| 🤖 **AI Chat** | Natural language expense logging via Gemini AI | ✅ Done |
| 📸 **Receipt Scanner** | Snap a receipt → AI extracts data automatically | ⏳ Dev 2 |
| 💰 **Expense Tracker** | Manual add, edit, swipe-delete, search & filter | ⏳ Dev 4 |
| 📅 **Budget Planner** | Set monthly limits, track spending vs budget | ⏳ Dev 4 |
| 🎯 **Saving Challenges** | Set goals, track deposits, visualize progress | ⏳ Dev 4 |
| 📤 **CSV Export** | Export data, share via email | ⏳ Dev 5 |
| ⚙️ **Settings** | Profile, export, sign out | ⏳ Dev 5 |

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
| **AI** | Gemini API via Vercel Proxy (for Myanmar) |
| **Camera** | CameraX 1.4.1 |
| **Build** | Gradle 8.11.1, AGP 8.8.0 |
| **CI/CD** | GitHub Actions |

---

## 🚀 Quick Start

### Prerequisites (One-Time Setup)

1. **Android Studio** — [Download Ladybug (2024.3+)](https://developer.android.com/studio) → SDK Manager → Install **Android SDK API 35**
2. **JDK 17**
   - Mac: `brew install openjdk@17`
   - Windows: Download from [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or `choco install openjdk.17`
3. **API Keys**
   - Firebase: [console.firebase.google.com](https://console.firebase.google.com) → Project Settings → Download `google-services.json`

### Project Setup

```bash
git clone https://github.com/Jolly30/saving-coach.git
cd saving-coach
cp local.defaults.properties local.properties
```

Edit `local.properties`:
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
proxy.url=https://proxy-topaz-ten-36.vercel.app
```

Place `google-services.json` in `app/` folder.

### Google Sign-In Setup

Google Sign-In requires your **SHA-1 fingerprint** registered in Firebase Console.

```bash
./gradlew signingReport
```

Copy the `SHA1` from the `debug` variant → send to team lead → they add it in Firebase Console → Project Settings → Your Android App → **SHA certificate fingerprints**.

> ⚠️ **Without this, Google Sign-In fails** with error `12500` or `10`.

### Build & Run

```bash
./gradlew assembleDebug
```

Open in Android Studio → Run on emulator or device.

---

## 🤖 AI Chat (Proxy for Myanmar)

Gemini API is not supported in Myanmar. We use a **Vercel proxy** to bypass this.

```
Android App → Vercel Proxy (Singapore) → Gemini API
```

**Proxy URL:** `https://proxy-topaz-ten-36.vercel.app`

The API key is stored securely in Vercel (not in the app). See [SETUP_GUIDE.md](SETUP_GUIDE.md#8-gemini-api-setup-proxy-for-myanmar) for details.

---

## 👥 Team

| Dev | Role | Branch | Status |
|:---:|------|--------|:------:|
| **1** | UI Skeleton + Auth + Dashboard + AI Proxy | `main` | ✅ Done |
| **2** | Receipt Scanner + Camera | `feature/ai-chat-receipt` | ⏳ Pending |
| **3** | Firestore Repositories | `feature/data-layer` | ⏳ Pending |
| **4** | Expense Forms + Budget | `feature/expense-budget` | ⏳ Pending |
| **5** | Export + Settings | `feature/export-settings-ci` | ⏳ Pending |

### Dev 1 Delivered ✅

- All data models + repository interfaces
- Navigation + Theme + Dashboard
- Auth (Google + Email)
- AI Chat (ChatScreen, ChatViewModel, Proxy)
- Vercel proxy deployed
- CI/CD pipelines
- ProGuard + signing config

### Git Workflow

```bash
git checkout main
git pull origin main
git checkout feature/your-branch-name
# make changes
git add .
git commit -m "Describe your change"
git push origin feature/your-branch-name
# create Pull Request to develop on GitHub
```

---

## 📁 Project Structure

```
saving-coach/
├── proxy/                          # Vercel serverless proxy (Gemini API)
│   ├── api/chat.js                 # Serverless function
│   ├── vercel.json                 # Vercel config
│   └── package.json
├── app/src/main/java/com/savingcoach/app/
│   ├── SavingCoachApp.kt           # Hilt Application class
│   ├── MainActivity.kt             # Single activity + bottom nav
│   ├── navigation/                 # Routes + NavGraph
│   ├── data/
│   │   ├── model/                  # Data classes
│   │   ├── repository/             # Repository interfaces + Firebase impl
│   │   └── mock/                   # In-memory mocks
│   ├── ai/                         # Gemini proxy service
│   ├── ui/
│   │   ├── theme/                  # Material 3 theme
│   │   ├── auth/                   # Sign up / Sign in
│   │   ├── dashboard/              # Main dashboard
│   │   ├── chat/                   # AI chat
│   │   └── components/             # Shared UI components
│   └── di/                         # Hilt modules
```

---

## 📱 Screens

| Screen | Route | Status |
|--------|-------|:------:|
| Auth / Login | `auth` | ✅ Done |
| Dashboard | `dashboard` | ✅ Done |
| AI Chat | `chat` | ✅ Done |
| Expense List | `expenses` | ⏳ Dev 4 |
| Add Expense | `add_expense` | ⏳ Dev 4 |
| Receipt Camera | `camera` | ⏳ Dev 2 |
| Budget Settings | `budget` | ⏳ Dev 4 |
| Settings | `settings` | ⏳ Dev 5 |

---

## 🔒 Security

- **API keys** → Vercel environment variables (not in code)
- **Firebase config** → `google-services.json` (gitignored)
- **Auth** → Firestore rules require `request.auth.uid == userId`
- **Template** → `local.defaults.properties` shows what's needed

---

## 📊 Dev Reports

Individual work logs are in the [`reports/`](./reports/) folder.

| File | Dev | Role |
|------|:---:|------|
| [mynote.md](./reports/mynote.md) | 1 | UI Skeleton + Auth + Dashboard + AI Proxy ✅ |
| [mynote-DEV2.md](./reports/mynote-DEV2.md) | 2 | Receipt Scanner + Camera |
| [mynote-DEV3.md](./reports/mynote-DEV3.md) | 3 | Firestore Repositories |
| [mynote-DEV4.md](./reports/mynote-DEV4.md) | 4 | Expense Forms + Budget |
| [mynote-DEV5.md](./reports/mynote-DEV5.md) | 5 | Export + Settings |

---

*Built with ❤️ by Jolly30 & Team*
