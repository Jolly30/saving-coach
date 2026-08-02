# 🐷 Saving Coach

> **AI-Powered Personal Finance Manager** — Take control of your money.

![PR Check](https://github.com/Jolly30/saving-coach/actions/workflows/ci-pr-check.yml/badge.svg)
![Release](https://github.com/Jolly30/saving-coach/actions/workflows/ci-release.yml/badge.svg)

---

## ✨ Features

| Feature | Description | Dev | Status |
|---------|-------------|:---:|:------:|
| 📊 **Dashboard** | Budget progress, calendar heatmap, spending overview | 1 | ✅ |
| 🔐 **Auth** | Google Sign-In + Email/Password | 1 | ✅ |
| 🤖 **AI Chat** | Natural language expense logging via Gemini AI | 1 | ✅ |
| 🔥 **Firestore Repos** | Real Firebase data layer | 1 | ✅ |
| 📸 **Receipt Scanner** | Snap a receipt → AI extracts data | 2 | ⏳ |
| 💰 **Budget & Expense Hub** | Monthly budget, categories, recent expenses, log expense | 4 | ⏳ |
| 🎯 **Saving Challenges** | Set goals, track deposits, visualize progress | 3 | ⏳ |
| 📤 **Export** | Monthly spending & saving history in Excel | 5 | ⏳ |
| ⚙️ **Settings** | Profile, theme, language, notifications | 5 | ⏳ |

---

## 🏗 Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Kotlin | 2.0.21 |
| **UI** | Jetpack Compose BOM | 2024.12.01 |
| **UI** | Material 3 | (via BOM) |
| **Navigation** | Navigation Compose | 2.8.5 |
| **DI** | Hilt | 2.53.1 |
| **Backend** | Firebase Auth | 23.1.0 |
| **Backend** | Firebase Firestore | 25.1.1 |
| **Backend** | Firebase BOM | 33.7.0 |
| **Backend** | Google Services Plugin | 4.4.2 |
| **Backend** | Crashlytics Plugin | 3.0.2 |
| **AI** | Gemini Generative AI | 0.9.0 |
| **Camera** | CameraX | 1.4.1 |
| **Network** | OkHttp | 4.12.0 |
| **Auth** | Credential Manager | 1.3.0 |
| **Auth** | Play Services Auth | 2.1.0 |
| **Format** | Kotlin Serialization | 1.7.3 |
| **Async** | Coroutines | 1.9.0 |
| **Export** | Apache Commons CSV | 1.11.0 |
| **Build** | AGP | 8.8.0 |
| **Build** | Gradle | 8.11.1 |
| **Build** | Secrets Plugin | 2.0.1 |
| **Build** | JDK | 17 |
| **Target** | compileSdk / targetSdk | 35 |
| **Min** | minSdk | 26 |
| **CI/CD** | GitHub Actions | — |

---

## 🚀 Setup & Environment (Step by Step)

### Step 1: Install Android Studio

1. Download **Android Studio Ladybug (2024.3+)** from [developer.android.com/studio](https://developer.android.com/studio)
2. Open the installer and follow the setup wizard
3. During setup, make sure to install:
   - **Android SDK** → check **API 35**
   - **Android SDK Build-Tools** → check **36+**
   - **Android Emulator**
   - **Android SDK Platform-Tools**
4. Open Android Studio → **Plugins** → search "Kotlin" → install it

### Step 2: Install JDK 17

**Mac:**
```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc
java -version    # Should show 17.x
```

**Windows (Git Bash / PowerShell as Admin):**
```bash
choco install openjdk.17
java -version    # Should show 17.x
```

### Step 3: Install Git

**Mac:**
```bash
brew install git
git --version
```

**Windows:**
```bash
choco install git
git --version
```

### Step 4: Clone the Repo

**Mac / Windows (Git Bash):**
```bash
git clone https://github.com/Jolly30/saving-coach.git
cd saving-coach
```

### Step 5: Create `local.properties`

**Mac / Windows (Git Bash):**
```bash
cp local.defaults.properties local.properties
```

Then open `local.properties` and edit:

**Mac:**
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
proxy.url=https://proxy-lake-xi-82.vercel.app
```

**Windows:**
```properties
sdk.dir=C:/Users/YOUR_USERNAME/AppData/Local/Android/sdk
proxy.url=https://proxy-lake-xi-82.vercel.app
```

> ⚠️ Replace `YOUR_USERNAME` with your actual computer username.

### Step 6: Get Firebase Config

1. Ask team lead for `google-services.json`
2. Place it in the `app/` folder:
   ```
   saving-coach/app/google-services.json
   ```

### Step 7: Connect Your Phone (WiFi Debugging)

1. On your phone: **Settings → About Phone → tap Build Number 7 times** (enables Developer Options)
2. **Settings → Developer Options → turn on Wireless Debugging → tap Allow**
3. Note your phone's **IP address** and **pairing port** from Developer Options

**Mac / Windows (Git Bash):**
```bash
# Pair (enter the pairing code shown on your phone)
adb pair YOUR_PHONE_IP:PAIRING_PORT

# Connect
adb connect YOUR_PHONE_IP:CONNECT_PORT

# Verify
adb devices
```

Should show:
```
List of devices attached
192.168.1.xxx:5555    device
```

### Step 8: Get SHA-1 Fingerprint

**Mac / Windows (Git Bash):**
```bash
./gradlew signingReport
```

Copy the `SHA1` from the `debug` variant → send to team lead → they add it in Firebase Console.

> ⚠️ **Without this, Google Sign-In fails** with error `12500` or `10`.

### Step 9: Build & Install

**Mac / Windows (Git Bash):**
```bash
# Build the app
./gradlew assembleDebug

# Install on your phone (phone must be connected)
./gradlew installDebug
```

Or open in Android Studio → click **Run** ▶️ → select your phone.

### Step 10: Test the App

1. Open **Saving Coach** app on your phone
2. Tap **Continue with Google**
3. Sign in with your Google account
4. You should see the **Dashboard**

### Troubleshooting

| Issue | Solution |
|-------|----------|
| `adb devices` shows nothing | Check WiFi debugging is on, phone & computer on same WiFi |
| Google Sign-In fails (error 12500) | SHA-1 not registered — send your SHA-1 to team lead |
| `google-services.json` not found | Make sure it's in `app/` folder, not `app/src/` |
| Build fails "SDK not found" | Edit `local.properties` → set correct `sdk.dir` path |
| App crashes on launch | Check Logcat in Android Studio for error details |

---

## 🤖 AI Chat (Proxy)

Gemini API is not supported in Myanmar. We use a **Vercel proxy**:

```
Android App → Vercel Proxy (Singapore) → Gemini API
```

✅ **No setup needed!** Just run `cp local.defaults.properties local.properties`.

---

## 📊 Dev Reports

Work logs in [`reports/`](./reports/):

| File | Dev | Role |
|------|:---:|------|
| [mynote.md](./reports/mynote.md) | 1 | UI Skeleton + Auth + Dashboard + AI Proxy + Firestore |
| [mynote-DEV2.md](./reports/mynote-DEV2.md) | 2 | Receipt Scanner + Camera |
| [mynote-DEV3.md](./reports/mynote-DEV3.md) | 3 | Saving Challenges |
| [mynote-DEV4.md](./reports/mynote-DEV4.md) | 4 | Budget & Expense Hub |
| [mynote-DEV5.md](./reports/mynote-DEV5.md) | 5 | Export + Settings |

---

*Built with ❤️ by Jolly30 & Team*
