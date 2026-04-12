# MoneyManager - Android App Project

## Overview
- **Type**: Native Android Personal Finance App
- **Goal**: Convert HTML web app to Android and publish on Google Play
- **Source**: `D:\Android projec\MoneyManager.html`

## Source Analysis
Comprehensive money management web app with:
- 12 main sections: Dashboard, Accounts, Transactions, Recurring, Reports, Budgets, Goals, Templates, Tags, Categories, Settings
- PIN lock with biometric support
- Dark/light theme
- 10+ currency support
- LocalStorage-based data

## Tech Stack Decision
| Component | Choice |
|-----------|--------|
| UI | Jetpack Compose + Material Design 3 |
| Database | Room (local) + Firestore (cloud backup) |
| Auth | Google Sign-In (Firebase Auth) |
| Charts | MPAndroidChart |
| DI | Hilt |
| Architecture | MVVM + Clean Architecture |

## Configuration
- **App Name**: MoneyManager
- **Data Storage**: Both (Room local + Firebase cloud backup)
- **Monetization**: Free (all features)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)