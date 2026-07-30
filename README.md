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
| 🔥 **Firestore Repositories** | Real Firebase data layer (Expense, Budget, SavingChallenge) | ⏳ Dev 1 |
| 📸 **Receipt Scanner** | Snap a receipt → AI extracts data automatically | ⏳ Dev 2 |
| 💰 **Budget & Expense Hub** | Monthly budget tracker, category progress, recent expenses, log new expense | ⏳ Dev 4 |
| 📅 **Budget Planner** | Set monthly limits, track spending vs budget | ⏳ Dev 4 |
| 🎯 **Saving Challenges** | Set goals, track deposits, visualize progress | ⏳ Dev 3 |
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

### Step 1: Install Prerequisites

| Tool | Version | How to Install |
|------|---------|----------------|
| **Android Studio** | Ladybug (2024.3+) | [Download](https://developer.android.com/studio) |
| **JDK** | 17 LTS | See below |
| **Android SDK** | API 35 | Android Studio → SDK Manager → Install API 35 |
| **Git** | Latest | See below |

**Mac:**
```bash
brew install openjdk@17
brew install git
```

**Windows (Git Bash):**
```bash
choco install openjdk.17
choco install git
```

---

### Step 2: Clone & Setup Project

**Mac / Windows (Git Bash):**
```bash
git clone https://github.com/Jolly30/saving-coach.git
cd saving-coach
cp local.defaults.properties local.properties
```

---

### Step 3: Edit local.properties

Open `local.properties` and fill in your SDK path:

**Mac:**
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
proxy.url=https://proxy-topaz-ten-36.vercel.app
```

**Windows:**
```properties
sdk.dir=C:/Users/YOUR_USERNAME/AppData/Local/Android/sdk
proxy.url=https://proxy-topaz-ten-36.vercel.app
```

> ⚠️ Replace `YOUR_USERNAME` with your actual username.

---

### Step 4: Get Firebase Config

1. Ask team lead for `google-services.json`
2. Place it in `app/` folder:
   ```
   saving-coach/app/google-services.json
   ```

---

### Step 5: Setup Physical Phone (WiFi)

#### 5a. Enable Developer Options

1. Go to **Settings → About Phone**
2. Tap **Build Number** 7 times
3. You'll see "You are now a developer!"

#### 5b. Enable WiFi Debugging

1. Go to **Settings → Developer Options**
2. Turn on **Wireless Debugging** (or **WiFi Debugging**)
3. Tap **Allow** when prompted

#### 5c. Pair Phone with Computer

**Mac:**
```bash
# Find your phone's IP address (shown in Developer Options → Wireless Debugging)
adb pair YOUR_PHONE_IP:PAIRING_PORT
# Enter the pairing code shown on your phone

# Connect to your phone
adb connect YOUR_PHONE_IP:CONNECT_PORT
```

**Windows (Git Bash):**
```bash
# Find your phone's IP address (shown in Developer Options → Wireless Debugging)
adb pair YOUR_PHONE_IP:PAIRING_PORT
# Enter the pairing code shown on your phone

# Connect to your phone
adb connect YOUR_PHONE_IP:CONNECT_PORT
```

#### 5d. Verify Connection

```bash
adb devices
```

Should show your phone like:
```
List of devices attached
192.168.1.xxx:5555    device
```

#### 5e. Install Google Account (for Google Sign-In)

1. Make sure you're signed into a Google account on your phone
2. Go to **Settings → Accounts → Google** → verify you're signed in

---

### Step 6: Get SHA-1 Fingerprint

**Mac / Windows (Git Bash):**
```bash
./gradlew signingReport
```

Copy the `SHA1` from the `debug` variant → send to team lead → they add it in Firebase Console.

> ⚠️ **Without this, Google Sign-In fails** with error `12500` or `10`.

---

### Step 7: Build & Install on Phone

**Mac / Windows (Git Bash):**
```bash
# Build the app
./gradlew assembleDebug

# Install on your phone (phone must be connected via WiFi)
./gradlew installDebug
```

Or open in Android Studio → Click **Run** → Select your phone.

---

### Step 8: Test the App

1. Open **Saving Coach** app on your phone
2. Tap **Continue with Google**
3. Sign in with your Google account
4. You should see the **Dashboard**

---

## 🔧 Troubleshooting

| Issue | Solution |
|-------|----------|
| `adb devices` shows nothing | Check WiFi debugging is on, phone and computer on same WiFi |
| Google Sign-In fails (error 12500) | SHA-1 not registered in Firebase. Send your SHA-1 to team lead |
| `google-services.json` not found | Make sure it's in `app/` folder, not `app/src/` |
| Build fails with "SDK not found" | Edit `local.properties` → set correct `sdk.dir` path |
| App crashes on launch | Check Logcat in Android Studio for error details |
| WiFi pairing fails | Make sure phone and computer are on same WiFi network |

---

## 🤖 AI Chat (Proxy for Myanmar)

Gemini API is not supported in Myanmar. We use a **Vercel proxy** to bypass this.

```
Android App → Vercel Proxy (Singapore) → Gemini API
```

**Proxy URL:** `https://proxy-topaz-ten-36.vercel.app`

> ✅ **No setup needed for other devs!** The proxy is already deployed. Just run `cp local.defaults.properties local.properties` — the proxy URL is pre-filled.

The API key is stored securely in Vercel (not in the app). See [SETUP_GUIDE.md](SETUP_GUIDE.md#8-gemini-api-setup-proxy-for-myanmar) for details.

---

## 👥 Team

| Dev | Role | Branch | Status |
|:---:|------|--------|:------:|
| **1** | UI Skeleton + Auth + Dashboard + AI Proxy + Firestore | `main` | ✅ Done |
| **2** | Receipt Scanner + Camera | `feature/ai-chat-receipt` | ⏳ Pending |
| **3** | Saving Challenges | `feature/saving-challenges` | ⏳ Pending |
| **4** | Budget & Expense Hub | `feature/expense-budget` | ⏳ Pending |
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

## 📱 Navigation

### Bottom Nav (4 tabs)

```
┌─────────────────────────────────────────┐
│  💰 Saving Coach           🔔 (notif)  │
├─────────────────────────────────────────┤
│                                         │
│         Content Area                    │
│                                    💬   │ ← Chat bubble (FAB)
│                                         │
├─────────────────────────────────────────┤
│  📊      🧾      🎯      ⚙️           │
│ Dashboard Expense Challenges Settings   │
└─────────────────────────────────────────┘
```

| Tab | Icon | What's Inside | Status |
|-----|------|---------------|:------:|
| **Dashboard** | 📊 | Budget progress, spending overview, calendar heatmap | ✅ Done |
| **Expense** | 🧾 | Budget & Expense Hub — monthly budget, categories, recent expenses, log new expense | ⏳ Dev 4 |
| **Challenges** | 🎯 | Saving challenges, deposits, progress | ⏳ Dev 3 |
| **Settings** | ⚙️ | Profile, theme, language, notifications, export | ⏳ Dev 5 |

**Chat Bubble (FAB):**
- Floating button on all screens
- Tap to open AI chat
- Can be dragged or fixed position

### Budget & Expense Hub (Expense Tab)

```
┌──────────────────────────────────────────────────────────────────┐
│                     BUDGET & EXPENSE HUB                         │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ 🎯 Monthly Overall Budget                 [ Edit Budget ⚙️ ] │  │
│  │                                                            │  │
│  │ 1,850 MMK Spent  /  3,000 MMK Target                      │  │
│  │ ==========================>................   61% Used     │  │
│  │                                                            │  │
│  │ Remaining: 1,150 MMK  |  12 Days Left                      │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│            ┌────────────────────────────────────────┐            │
│            │        ➕ LOG NEW EXPENSE               │            │
│            └────────────────────────────────────────┘            │
│                                                                  │
│  🏷️ CATEGORIES                                [ + New Category ] │
│                                                                  │
│  🍔 Food & Dining                                                │
│  =======================>...................  320 / 600 MMK      │
│                                                                  │
│  🚗 Transportation                                              │
│  =========>.................................  110 / 300 MMK      │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ 🧾 RECENT EXPENSES                                               │
│ (Tap any category above to filter this list)                     │
│                                                                  │
│  ☕ Starbucks                          -4,500 MMK  │ Today       │
│  🛒 Target Store                     -68,200 MMK  │ Yesterday   │
│  ⛽ Shell Gas Station               -45,000 MMK  │ Jul 26      │
└──────────────────────────────────────────────────────────────────┘
```

**Log Expense Bottom Sheet:**

```
┌──────────────────────────────────────────────────────────────────┐
│ ➕ Log Expense                           [✕ Close]              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Amount (MMK):                                                   │
│  [ 0                                                         ]  │
│                                                                  │
│  Select Category (Required):                                     │
│  [ 🍔 Food ]  [ 🚗 Trans. ]  [ 🛒 Groceries ]  [ 🎬 Fun ]      │
│                                                                  │
│  Note / Merchant (Optional):                                     │
│  [ e.g., Starbucks Coffee                                      ]│
│                                                                  │
│  [ Save Expense ]                                                │
└──────────────────────────────────────────────────────────────────┘
```

> ⚠️ Important: Saving vs Expense & Budget

**Saving challenges are COMPLETELY ISOLATED from expenses and budget.**

| Feature | Expense & Budget | Saving Challenges |
|---------|:----------------:|:-----------------:|
| **Tracks** | Spending (money OUT) | Savings (money SET ASIDE) |
| **Budget** | Monthly limit | No limit |
| **Categories** | Yes (Food, Transport, etc.) | No |
| **Calendar** | Expense heatmap | Not shown |
| **Affects Each Other** | ❌ No | ❌ No |

> Saving 1,000 MMK does NOT reduce your expense total. Spending 5,000 MMK does NOT reduce your savings. They are separate systems.

### Other Screens

| Screen | Route | Access | Status |
|--------|-------|--------|:------:|
| Auth / Login | `auth` | Start | ✅ Done |
| Onboarding | `onboarding` | After sign up | ⏳ Dev 5 |
| Add Expense | `add_expense` | From Expense tab | ⏳ Dev 4 |
| Receipt Camera | `camera` | From Chat | ⏳ Dev 2 |

### Dashboard Features

| Feature | Description |
|---------|-------------|
| **Budget Progress** | Monthly budget vs spent with color indicator |
| **Saving Challenges** | Active challenges with progress bars |
| **Expense Chart** | Pie/bar chart of expenses by category |
| **Calendar Heatmap** | Daily spending with color rating (green/yellow/red) |

### Calendar Heatmap — Click to See Details

When user clicks a day (e.g., July 1st):
```
┌─────────────────────────────────┐
│  📅 July 1, 2026                │
├─────────────────────────────────┤
│  💰 Saving:    10,000 MMK       │
│  💸 Expense:    5,000 MMK       │
│  📊 Net:        5,000 MMK       │
└─────────────────────────────────┘
```

**Color Rating:**
| Daily Spending vs Budget | Color |
|--------------------------|-------|
| < 50% | 🟢 Green |
| 50-80% | 🟡 Yellow |
| 80-100% | 🟠 Orange |
| > 100% | 🔴 Red |

### Calendar Filters

User can filter calendar to see:
- **All** — Combined view
- **Budget** — Budget progress only
- **Expenses** — Expenses only
- **Savings** — Savings only

### Onboarding (After Sign Up)

New users fill in profile:
| Field | Type | Options |
|-------|------|---------|
| Career | Text | "Software Engineer" |
| Age | Number | "25" |
| Gender | Select | "Male", "Female", "Rather not answer" |
| Salary Range | Select | "< 100k", "100k-200k", "200k-300k", "300k-500k", "500k-1M", "> 1M", "Prefer not to say" |

This data is stored in user profile for personalized advice.

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
| [mynote-DEV4.md](./reports/mynote-DEV4.md) | 4 | Budget & Expense Hub |
| [mynote-DEV5.md](./reports/mynote-DEV5.md) | 5 | Export + Settings |

---

*Built with ❤️ by Jolly30 & Team*
