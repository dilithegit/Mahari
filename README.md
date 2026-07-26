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
- **Full-Rescan Pull-to-Refresh**: Pulling down on the dashboard triggers the full SMS backfill re-scan code path directly (`SmsBackfillManager.performOneTimeBackfill`).
- **Retroactive One-Time Migration**: Automatically re-parses and updates historical transactions' timestamps and running balances on startup.
- **De-duplication**: Code hash verification prevents duplicate entry creation.

### 💰 2. Current M-Pesa Balance Card & Nullability Model
- **Standalone Dashboard Card**: Displays current M-Pesa balance extracted from the most recent transaction with a valid balance reading.
- **Nullable Balance Model**: `runningBalance` is a nullable `Double?` column, distinguishing genuine zero balances (`Ksh 0.00`) from unknown/absent balance readings (`null`).
- **Broadened Regex Support**: Tollerates `Ksh`, `KES`, `Ksh.`, `KES.`, irregular spacing, negative overdraft balances (`Ksh-350.00`), and genuine zero values.
- **Fuliza & Parse-Failure Skipping**: Transactions with absent or unparseable balances (such as standalone Fuliza overdraft alerts) are transparently skipped in balance selection queries.

### 🗓️ 3. Global Date-Scope Engine
- **Current Month Default**: Defaults to current calendar month (e.g. July 2026) on fresh launch. Past/custom selections do not persist across app sessions.
- **Month Stepper (`< July 2026 >`)**: Fast month navigation arrows capped at the current real-world month.
- **Custom Range Picker**: 📆 Calendar picker for custom start & end date ranges with a one-tap **"Back to Current Month"** snap-back button.
- **Search All-Time Override**: 🌐 Search bar respects current date-scope by default, with an explicit **"Search All Time"** toggle chip.
- **Recent Transactions Top 5 Preview**: Dashboard displays a compact card showing only the 5 most recent transactions (slim rows with merchant, category icon, amount, and time), with a **"See all {N} transactions"** button that opens `FullTransactionListSheet` modal sheet. Automatically hides the button when <= 5 items exist.

### 🏛️ 4. Quiet Luxury Visual Design System
- **OLED Warm Charcoal Background**: `#16181A` (`WarmCharcoalBgDark`) for warm, rich OLED dark mode.
- **Warm Metal Accents**: Antique Brass (`#B08D57` Dark / `#8C6D3F` Light) for card borders, thin dividers, and structural accents.
- **Precious Hero Emerald Restraint**: `#34D399` Dark / `#059669` Light reserved **strictly** for the single hero budget figure per screen.
- **Eased Motion**: Cubic bezier transitions (`FastOutSlowInEasing`) for progress fills and count-up figures.
- **Premium Biometric Lock Screen**: Custom pre-auth screen with centered Mahari cowrie mark inside a Warm Metal ring.

### 📷 5. In-App Image Capture & Share Sheet
- Renders custom Mahari share cards with theme-matching background, Warm Metal borders, and cowrie branding mark (`🐚 MAHARI`).
- **Balance Masking**: Includes a toggle to mask sensitive balances (`Ksh ••••••`) before sharing.
- **FLAG_SECURE Compliance**: `FLAG_SECURE` remains **100% active** to block OS-level screenshots while supporting in-app image export via Android Share Sheet (`FileProvider`).

### ☁️ 6. Optional Opt-In Cloud Sync & Python FastAPI ML Backend
- **100% Offline-First Default**: Cloud sync is off by default and requires explicit user consent via confirmation modal.
- **Data Minimization Guarantee**: Raw SMS text, phone numbers, and contact names **never leave the device**. Only structured DTOs (`amount`, `category`, `merchant`, `timestamp`, `isExpense`) are uploaded over HTTPS/TLS.
- **Python FastAPI Backend** (`mahari_backend/`): Runs XGBoost + SHAP feature attribution engine. Encrypts data at rest using `cryptography.fernet.Fernet`.
- **Permanent Data Deletion**: Includes `DELETE /api/v1/user-data/{deviceId}` endpoint to permanently purge all synced cloud data.

### 🧠 7. Dynamic Categorization & Persistent Memory
- **Dynamic Keyword Matrix**: Auto-categorizes merchants, dining, supermarkets, fuel, and transport.
- **Learning Memory**: Remembers user recategorizations in a local Room database (`merchant_category_mappings`) and applies them to future transactions from that merchant.

---

## 🛡️ Data Integrity Safeguards & Known Issue History

To prevent transaction visibility regressions (e.g. transactions disappearing after schema changes or date parsing adjustments), Mahari enforces strict automated and runtime guardrails:

1. **Automated Regression Suite (`TransactionVisibilityRegressionTest.kt`)**:
   - Seeds transactions across distinct months, balances (`positive`, `0.0`, and `null`), and format variations.
   - Runs database migrations (`MIGRATION_4_5`) and asserts that total record count and query visibility are preserved 100%.
2. **Runtime Sanity Check (`DATA_INTEGRITY_SANITY_CHECK`)**:
   - On every dashboard calculation pass, the ViewModel compares total raw SQLite database transactions against currently visible scoped transactions.
   - Emits a high-priority warning log (`⚠️ SANITY WARNING: Database contains N transactions, but current scope returned 0`) if date filters conceal all records.
3. **Mandatory Developer Checklist**:
   - Any commit modifying `TransactionEntity`, Room schema versions, or `DateScopeMode` must execute `TransactionVisibilityRegressionTest` and verify raw DB count equality before release.

---

## 🎨 Design System & Palette

Mahari's visual identity balances financial clarity with luxury restraint:

| Token Role | Color (Dark Mode) | Color (Light Mode) | Usage |
| :--- | :--- | :--- | :--- |
| **Background** | `#16181A` (Warm Charcoal) | `#F4F3EF` (Warm Bone) | OLED near-black warm canvas |
| **Card Surface** | `#1F2226` | `#FFFFFF` | Primary card containers |
| **Warm Metal Accent** | `#B08D57` (Antique Brass) | `#8C6D3F` (Soft Copper) | Card borders, dividers, structural lines |
| **Hero Emerald** | `#34D399` | `#059669` | **Reserved strictly** for hero numbers |

---

## 🛠️ Local Development & Build Setup

### Prerequisites
- JDK 21 (bundled JetBrains Runtime `jbr` or OpenJDK 21)
- Android SDK 35 & Android CLI (`adb`)
- Gradle 8.11.1

### Build & Run
1. **Clone Repository**:
   ```bash
   git clone https://github.com/dilithegit/Mahari.git
   cd Mahari
   ```

2. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Build & Install Debug APK**:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 👥 Authors & Credits

Handcrafted with precision by **Strathmore University** students:
- **Okwudili Ujubuonu**
- **Mark Gitau Irungu**

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for details.
