# Mahari — M-Pesa Finance Tracker 🇰🇪📱

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-navy.svg)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-indigo.svg)](https://m3.material.io)
[![Room](https://img.shields.io/badge/Database-Room-emerald.svg)](https://developer.android.com/training/data-storage/room)
[![Offline First](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg)](#privacy--security)

**Mahari** is a lightweight, fully offline Android financial intelligence application designed to parse M-Pesa SMS notifications, structure transactions, categorize spend, calculate running budgets, detect anomalies/recurring bills, and present monthly behavioral insights via on-device machine learning (XGBoost exported to ONNX).

> [!NOTE]
> Created by **Strathmore University** students **Okwudili Ujubuonu** and **Mark Gitau Irungu**.


---

## 🌟 Key Features

### 📨 1. Automated SMS Capture & Regex Parser
- Intercepts incoming Safaricom M-Pesa notifications via background `BroadcastReceiver`.
- Parses **7 distinct M-Pesa message formats**:
  - Paybill / Buy Goods (`Ksh350.00 paid to Java House...`)
  - Person-to-Person Transfers (`sent to... / received from...`)
  - Agent Withdrawals & Deposits
  - Safaricom Airtime & Data purchases
  - Fuliza Overdraft allocations & outstanding balances
- **De-duplication**: Prevents double-counting duplicate M-Pesa SMS deliveries via transaction code hash verification.

### 🧠 2. Dynamic Categorization & Persistent Memory
- **Dynamic Keyword Matrix**: Auto-categorizes new merchants, cafeterias, campus dining, supermarkets, gas stations, and ride-hailing services using multi-keyword pattern matching.
- **Learning Memory**: Remembers user recategorizations in a local Room database (`merchant_category_mappings`) and automatically applies them to all future transactions from that merchant.

### 📊 3. Modern Material 3 Financial Dashboard
- **Hero Daily Budget Card**: Real-time daily budget progress with animated count-up numbers, limit indicators, and `ON BUDGET` / `OVER BUDGET` status pills.
- **Monthly Cashflow Summary**: Income vs. Expense net balance card.
- **Category Allocation Chart**: Top category spend distribution with visual progress bars.
- **Interactive Ledger**: Search transactions by merchant, code, amount, or category with instant filtering chips.

### 🔔 4. Budget Engine & Real-Time Alert Notifications
- Computes running daily budgets (`monthlyLimit / 30.0`).
- Fires high-priority Android notification alerts immediately when an incoming SMS pushes daily spend across **80%**, **95%**, or **100%+** of budget limits.

### 🤖 5. Smart Financial Intelligence
- **Recurring Expense Detector**: Spots subscriptions, rent, and utility bills based on merchant frequency and interval cadence.
- **Fuliza Overdraft Tracker**: Separates liability debt tracking from regular discretionary spend.
- **Anomaly Detection**: Flags transactions exceeding 2 standard deviations (`> mean + 2.0 * stdDev`) from a merchant's historical average.
- **Natural Language Search**: Parses text queries like *"How much did I spend at Java House in June?"* into structured local database filters.

### 🤖 6. Monthly Recap & SHAP Behavioral Insights
- Trains an **XGBoost** classification model on transaction feature vectors (food ratio, weekend activity, spending velocity, volatility index).
- **On-Device Inference**: Runs directly on-device with precomputed SHAP plain-language reasoning templates (e.g., *"Food & Dining spending drove 60% of this month's budget variation"*).
- **Offline Statistical Fallback**: Rule-based baseline comparison when transaction history is < 10 items.

### 🛡️ 7. Privacy, Security & Export
- **100% Offline**: Zero external network requests by default. Financial data never leaves the device.
- **Biometric & PIN Lock**: Encrypted security preferences for biometric authentication and PIN lock on app launch.
- **CSV Export**: Export complete transaction ledger to CSV for personal backup or accounting.

---

## 🎨 Design System & Palette

Mahari's visual system follows Material 3 principles with deliberate financial color roles:
- **Primary / Navigation / Trust**: Deep Indigo Navy (`#0F172A`), Mid Blue (`#1D4ED8`), Powder Blue Containers (`#EFF6FF`).
- **Secondary / Growth / Positive Balance**: Emerald / Mint Green (`#059669` / `#10B981`) for income, positive balances, and progress bars.
- **Alert Warnings**: Red (`#EF4444`) & Amber (`#F59E0B`) strictly reserved for overspending and Fuliza debt alerts.
- **Light & Dark Themes**: Fully re-tuned Material 3 color roles for solid contrast in both light and dark modes.

---

## 🛠️ Architecture & Tech Stack

```
+-----------------------------------------------------------------------+
|                              MAHARI APP                               |
+-----------------------------------------------------------------------+
|  UI Layer (Jetpack Compose + Material 3)                              |
|   ├── Screens: DashboardScreen, DesignPreviewScreen                   |
|   └── ViewModels: DashboardViewModel                                  |
+-----------------------------------------------------------------------+
|  Domain & Intelligence Layer                                          |
|   ├── MpesaParser (Regex parsing for 7 M-Pesa format variations)      |
|   ├── Categorizer (Dynamic keyword matrix + Room learning memory)    |
|   ├── BudgetEngine (Running daily budget & threshold alert levels)    |
|   ├── SmartAnalytics (Recurring detector, Anomaly, Fuliza tracker)    |
|   ├── NlpSearchEngine (Natural language query parsing)                |
|   └── RecapMlEngine (XGBoost ONNX model + SHAP reasoning templates)   |
+-----------------------------------------------------------------------+
|  Data & Infrastructure Layer                                          |
|   ├── Room Database (Transactions, Mappings, Budgets, Goals)          |
|   ├── Manual DI (AppContainer - lightweight, no Hilt build overhead)  |
|   ├── SecurityManager (Encrypted preferences, Biometric / PIN)       |
|   ├── NotificationHelper (M3 High-Priority Alert notifications)       |
|   └── BroadcastReceiver (SmsReceiver with de-duplication)            |
+-----------------------------------------------------------------------+
```

---

## 📂 Project Structure

```
Mahari/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/               # Trained XGBoost model & SHAP templates
│   │   │   │   ├── mahari_recap.json
│   │   │   │   └── shap_templates.json
│   │   │   ├── java/com/example/mahari/
│   │   │   │   ├── data/
│   │   │   │   │   ├── analytics/    # Recurring & Anomaly detection
│   │   │   │   │   ├── budget/       # Budget Engine
│   │   │   │   │   ├── categorizer/  # Dynamic auto-categorizer
│   │   │   │   │   ├── db/           # Room Database, Entities, DAOs
│   │   │   │   │   ├── export/       # CSV Ledger Exporter
│   │   │   │   │   ├── nlp/          # Natural Language Query parser
│   │   │   │   │   ├── notification/ # Budget Alert notifications
│   │   │   │   │   ├── parser/       # M-Pesa Regex Parser
│   │   │   │   │   ├── recap/        # Monthly Recap ML Engine
│   │   │   │   │   ├── receiver/     # SMS Interceptor BroadcastReceiver
│   │   │   │   │   ├── repository/   # Transaction Repository
│   │   │   │   │   └── security/     # Biometric & PIN Security Manager
│   │   │   │   ├── di/               # AppContainer Manual DI
│   │   │   │   ├── theme/            # Color, Type, Theme tokens
│   │   │   │   ├── ui/               # Dashboard & Preview UI screens
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── MahariApplication.kt
│   │   └── test/                     # Unit test suites (MpesaParserTest)
├── scripts/
│   └── train_recap_model.py          # Python training script for XGBoost ONNX model
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or higher) or JDK 17+
- Android SDK Platform 35/36
- Android device or emulator running Android 7.0 (API 24) or higher

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
   The compiled APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

3. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Install directly via ADB**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 👥 Authors & Credits

Developed by **Strathmore University** students:
- **Okwudili Ujubuonu**
- **Mark Gitau Irungu**

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.

