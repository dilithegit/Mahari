# Mahari — M-Pesa Financial Intelligence & Luxury Ledger 🇰🇪📱

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-navy.svg)](https://developer.android.com/jetpack/compose)
[![Design](https://img.shields.io/badge/Design-Quiet%20Luxury-gold.svg)](#-quiet-luxury-design-system)
[![Database](https://img.shields.io/badge/Database-Room-emerald.svg)](https://developer.android.com/training/data-storage/room)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20First-success.svg)](#-privacy--security)

**Mahari** is a quiet luxury, offline-first Android financial intelligence application engineered to parse Safaricom M-Pesa SMS notifications, extract real transaction timestamps, structure cashflow, categorize spend, calculate daily budgets, track Fuliza overdrafts, and present monthly behavioral insights via on-device machine learning (XGBoost exported to ONNX).

> [!NOTE]
> Handcrafted by **Strathmore University** students **Okwudili Ujubuonu** and **Mark Gitau Irungu**.

---

## 🌟 Key Features

### 📨 1. Automated M-Pesa SMS Capture & Real Date Parser
- **Live Broadcast Receiver & Historical Backfill**: Intercepts incoming notifications and scans past device inbox via SMS ContentProvider.
- **7 M-Pesa Format Patterns**: Parses Paybill / Buy Goods, Person-to-Person Transfers, Agent Withdrawals/Deposits, Airtime/Data purchases, and Fuliza Overdraft allocations.
- **Real SMS Date Extraction**: Extracts exact transaction timestamps from message text (e.g., `"on 26/7/26 at 2:34 PM"`) with `Telephony.Sms.DATE` metadata fallback — **never import time**.
- **Retroactive One-Time Migration**: Automatically re-parses and updates historical transactions' timestamps on startup.
- **De-duplication**: Code hash verification prevents duplicate entry creation.

### 🗓️ 2. Global Date-Scope Engine
- **Current Month Default**: Defaults to current calendar month (e.g. July 2026) on fresh launch. Past/custom selections do not persist across app sessions.
- **Month Stepper (`< July 2026 >`)**: Fast month navigation arrows capped at the current real-world month.
- **Custom Range Picker**: 📆 Calendar picker for custom start & end date ranges with a one-tap **"Back to Current Month"** snap-back button.
- **Search All-Time Override**: 🌐 Search bar respects current date-scope by default, with an explicit **"Search All Time"** toggle chip.

### 🏛️ 3. Quiet Luxury Visual Design System
- **OLED Warm Charcoal Background**: `#16181A` (`WarmCharcoalBgDark`) for warm, rich OLED dark mode.
- **Warm Metal Accents**: Antique Brass (`#B08D57` Dark / `#8C6D3F` Light) for card borders, thin dividers, and structural accents.
- **Precious Hero Emerald Restraint**: `#34D399` Dark / `#059669` Light reserved **strictly** for the single hero budget figure per screen.
- **Eased Motion**: Cubic bezier transitions (`FastOutSlowInEasing`) for progress fills and count-up figures.
- **Premium Biometric Lock Screen**: Custom pre-auth screen with centered Mahari cowrie mark inside a Warm Metal ring.

### 📷 4. In-App Image Capture & Share Sheet
- Renders custom Mahari share cards with theme-matching background, Warm Metal borders, and cowrie branding mark (`🐚 MAHARI`).
- **Balance Masking**: Includes a toggle to mask sensitive balances (`Ksh ••••••`) before sharing.
- **FLAG_SECURE Compliance**: `FLAG_SECURE` remains **100% active** to block OS-level screenshots while supporting in-app image export via Android Share Sheet (`FileProvider`).

### ☁️ 5. Optional Opt-In Cloud Sync & Python FastAPI ML Backend
- **100% Offline-First Default**: Cloud sync is off by default and requires explicit user consent via confirmation modal.
- **Data Minimization Guarantee**: Raw SMS text, phone numbers, and contact names **never leave the device**. Only structured DTOs (`amount`, `category`, `merchant`, `timestamp`, `isExpense`) are uploaded over HTTPS/TLS.
- **Python FastAPI Backend** (`mahari_backend/`): Runs XGBoost + SHAP feature attribution engine. Encrypts data at rest using `cryptography.fernet.Fernet`.
- **Permanent Data Deletion**: Includes `DELETE /api/v1/user-data/{deviceId}` endpoint to permanently purge all synced cloud data.

### 🧠 6. Dynamic Categorization & Persistent Memory
- **Dynamic Keyword Matrix**: Auto-categorizes merchants, dining, supermarkets, fuel, and transport.
- **Learning Memory**: Remembers user recategorizations in a local Room database (`merchant_category_mappings`) and applies them to future transactions from that merchant.

---

## 🎨 Design System & Palette

Mahari's visual identity balances financial clarity with luxury restraint:

| Token Role | Color (Dark Mode) | Color (Light Mode) | Usage |
| :--- | :--- | :--- | :--- |
| **Background** | `#16181A` (Warm Charcoal) | `#F4F3EF` (Warm Bone) | OLED near-black warm canvas |
| **Card Surface** | `#1F2226` | `#FFFFFF` | Primary card containers |
| **Warm Metal Accent** | `#B08D57` (Antique Brass) | `#8C6D3F` (Soft Copper) | Card borders, dividers, structural lines |
| **Hero Emerald** | `#34D399` | `#059669` | **Reserved strictly** for hero numbers |
| **Alert Red** | `#EF4444` | `#EF4444` | Overspending & Fuliza debt warnings |

---

## 🛠️ Architecture & Tech Stack

```
+-----------------------------------------------------------------------+
|                              MAHARI APP                               |
+-----------------------------------------------------------------------+
|  UI Layer (Jetpack Compose + Quiet Luxury Palette)                    |
|   ├── Screens: DashboardScreen, OnboardingScreen, BiometricAuthScreen |
|   ├── Components: DateScopeSelectorBar, DateRangePickerDialog         |
|   └── ViewModels: DashboardViewModel (Unified Flow Combine)           |
+-----------------------------------------------------------------------+
|  Domain & Intelligence Layer                                          |
|   ├── MpesaParser (Regex timestamp & 7 message format parsing)        |
|   ├── Categorizer (Dynamic keyword matrix + Room learning memory)     |
|   ├── DateScopeMode (Month Stepper, Custom Range, Search All-Time)    |
|   ├── TransactionMigrationManager (Retroactive date migration)        |
|   └── ImageCaptureUtils (Share card generator & FileProvider)         |
+-----------------------------------------------------------------------+
|  Data & Security Layer                                                |
|   ├── Room Database (Transactions, Mappings, Budgets, Goals)          |
|   ├── SecurityManager (EncryptedSharedPreferences, Biometrics, PIN)  |
|   └── CloudSyncManager (HTTPS client, Data minimization DTOs)        |
+-----------------------------------------------------------------------+
                                   │ (Optional Opt-In Sync)
                                   ▼
+-----------------------------------------------------------------------+
|                    PYTHON FASTAPI ML BACKEND                          |
|  ├── Endpoints: POST /api/v1/sync, DELETE /api/v1/user-data/{id}      |
|  ├── Security: Fernet symmetric encryption at rest                   |
|  └── Engine: XGBoost + SHAP feature importance analysis               |
+-----------------------------------------------------------------------+
```

---

## 📂 Project Structure

```
Mahari/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/mahari/
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/           # Room Database, Entities, DAOs
│   │   │   │   │   ├── migration/    # Retroactive Date Migration Manager
│   │   │   │   │   ├── model/        # DateScopeMode Data Models
│   │   │   │   │   ├── parser/       # M-Pesa Parser & SMS Backfill Manager
│   │   │   │   │   ├── security/     # Encrypted Prefs & Security Manager
│   │   │   │   │   └── sync/         # Cloud Sync Manager & Minimization DTOs
│   │   │   │   ├── theme/            # Warm Charcoal, Warm Metal & Type Tokens
│   │   │   │   ├── ui/               # Dashboard, Share, Onboarding & Security UI
│   │   │   │   ├── util/             # Image Capture & FileProvider Utils
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── MahariApplication.kt
│   │   └── test/                     # Unit test suites (MpesaParserTest)
├── mahari_backend/                   # Optional Python FastAPI Server
│   ├── main.py                       # FastAPI Endpoints (Sync & Purge)
│   ├── ml_engine.py                  # XGBoost + SHAP Analysis Engine
│   ├── security.py                   # Fernet At-Rest Encryption & Deletion
│   └── requirements.txt              # FastAPI, XGBoost, SHAP, Fernet
├── build.gradle.kts
├── settings.gradle.kts
├── LICENSE                           # MIT License
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or higher) or JDK 17+
- Android SDK Platform 35/36
- Physical Android phone or emulator running Android 7.0 (API 24) or higher

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/dilithegit/Mahari.git
   cd Mahari
   ```

2. **Build the APK using Gradle**:
   ```bash
   ./gradlew assembleDebug
   ```
   Output APK: `app/build/outputs/apk/debug/app-debug.apk`

3. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Install directly via ADB**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

5. **(Optional) Run Python FastAPI Backend**:
   ```bash
   cd mahari_backend
   pip install -r requirements.txt
   python main.py
   ```

---

## 👥 Authors & Credits

Handcrafted with precision by **Strathmore University** students:
- **Okwudili Ujubuonu**
- **Mark Gitau Irungu**

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for details.
