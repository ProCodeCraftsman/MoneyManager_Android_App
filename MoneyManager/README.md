# MoneyManager 💰

**MoneyManager** is a modern, privacy-first Android personal finance application built with **Jetpack Compose**, **Kotlin Coroutines**, **Room Database**, and **AI Smart Assistants**. It provides comprehensive tracking for income, expenses, investments, credit card EMIs, net worth, budgets, and savings goals.

---

## 🚀 What's New in Version 2.0.0

* 💳 **Credit Card EMI Engine ("Pay via EMI")**: Split major purchases into multi-month EMIs. Supports **No-Cost EMI** and **Standard With-Interest EMI** (calculating principal, interest, and GST components) with automated monthly cash-flow schedule generation.
* 📈 **Investment Platform Accounts**: Track investments across platforms (Zerodha, Groww, HDFC Fixed Deposits, Gold, Crypto, EPF/PPF). Moving money to investments is treated as an asset transfer, preserving your total net worth and keeping living expense statistics clean.
* 🏦 **Categorized Account Dashboard**: Accounts are cleanly grouped into **Bank & Cash**, **Investment Platforms**, and **Credit Cards & Debt Liabilities** with real-time Net Worth calculation: `Net Worth = (Liquid Assets + Investment Portfolio) - Total Debt`.
* 🔄 **Enhanced Data Migration & Backup**: Seamless backward-compatible database migration (Room v14) and versioned JSON/Drive backup import.

---

## ✨ Key Features

### 📊 Comprehensive Financial Tracking
* **Income & Expense Logging**: Fast, intuitive transaction entry with custom categories, sub-categories, tags, receipts, and notes.
* **Split Transactions**: Break down single receipts into multiple expense categories (e.g., Supermarket bill split into *Groceries* and *Household Supplies*).
* **Multi-Currency & Custom Accounts**: Manage Bank Accounts, Cash, Wallets, Credit Cards, and Investment Accounts in one place.

### 💳 Credit Card EMI Engine
* Toggle **"Pay via EMI"** on any expense purchase.
* Custom tenure ($3 \dots 24+$ months), annual interest rate, upfront merchant discount, and processing fees.
* Safety guardrails with non-credit card account warnings.
* Generates recurring monthly installment records with exact paise/cent remainder absorption.

### 📈 Investment & Wealth Management
* Dedicated **Investment Platform Accounts** (Zerodha, Groww, Bank FDs, Crypto, Gold, PPF/EPF).
* **Asset Transfer Model**: Investments debit liquid accounts and credit platform accounts, reflecting real net worth without artificial expense spikes.
* **Savings Analytics**: View portfolio allocation pie charts, monthly investment inflows, and growth progress.

### 🤖 AI Smart Drafting & Receipts
* **Receipt Scan (OCR)**: Take a photo or upload a receipt to auto-extract date, merchant, total amount, and line items.
* **Voice Memo Assistant**: Speak transactions naturally (e.g., *"Spent 450 rupees on groceries from HDFC Bank"*); AI drafts the entry automatically.
* **SMS Notification Parser**: Auto-detect bank and UPI transaction SMS messages into draft transactions.
* **Local Merchant Learning**: Saves merchant category preferences on-device to skip AI for repeat merchants.
* **Privacy-First Hybrid AI**: On-device LiteRT-LM and ML Kit GenAI processing.

### 🎯 Budgets & Savings Goals
* **Monthly Budgets**: Set category-level spending limits with progress indicators and threshold alerts.
* **Savings Goals**: Track progress toward targets (Emergency Fund, New Laptop, Vacation) with linked savings transactions.

### 🤝 Lending & Borrowing
* Keep track of money lent to or borrowed from contacts.
* Integrated contact picker with balance history per peer contact.

### 🔒 Security, Privacy & Backup
* **100% Offline-First**: Financial data stays on your device inside an encrypted Room SQLite database.
* **Biometric App Lock**: Secure the app using Fingerprint, Face Unlock, or Device PIN.
* **Automated Cloud & Local Backup**:
  * Encrypted Google Drive AppData sync.
  * Local JSON file export and restore.
  * Full CSV Export/Import for Excel/Google Sheets analysis.

---

## 🛠 Tech Stack & Architecture

* **UI**: 100% Jetpack Compose with Material 3 Dynamic Theming & Dark Mode.
* **Architecture**: MVVM + Clean Architecture with Repository pattern & Unidirectional Data Flow.
* **Database**: Room Database with versioned SQLite migrations.
* **Dependency Injection**: Hilt (Dagger).
* **Asynchrony & State**: Kotlin Coroutines & `StateFlow` / `SharedFlow`.
* **AI Engine**: ML Kit GenAI Prompt API, Google AI Edge LiteRT-LM, NanoHTTPD server integration.
* **Charts**: MPAndroidChart.
* **Async Image Loading**: Coil.

---

## 🛠 Building the App

### Requirements
* Android Studio Ladybug (2024.2.1) or newer
* JDK 17
* Android SDK 36 (Min SDK 26)

### Steps
1. Clone the repository:
   ```bash
   git clone git@github.com:ProCodeCraftsman/MoneyManager_Android_App.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and run on an Android device or emulator running Android 8.0 (API 26) or higher:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📄 License

This project is maintained by **ProCodeCraftsman**. All rights reserved.
