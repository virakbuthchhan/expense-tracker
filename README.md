# Expense Tracker

A modern, 100% offline personal finance and expense tracking Android application built with Kotlin and Jetpack Compose. Designed with a **Geometric Balance** aesthetic (warm terracotta and neutral tones), the app provides seamless transaction tracking, monthly budget planning, interactive analytics visualizations, and secure local data export.

---

## Key Features

- **100% Offline & Private**: All data is stored locally on-device using an encrypted Room SQLite database. No account creation, cloud sync, or external tracking required.
- **Transaction Management**:
  - Quickly log income and expense entries with custom amounts, categories, dates, and notes.
  - Multi-criteria filtering by date range (Day, Week, Month, Year, Custom), transaction type, and category.
  - Full-text search across transaction notes and category names.
- **Smart Budgeting**:
  - Set monthly spending limits per category.
  - Real-time progress bars with dynamic warning states (Safe, Approaching Limit, Over Budget).
  - Monthly budget rollover and remaining spend calculations.
- **Rich Financial Analytics**:
  - Interactive category spending breakdown (Donut chart with slice selection).
  - Daily and weekly expense trend bar charts.
  - Income vs. Expense monthly comparison metrics.
- **Category Customization**:
  - Pre-configured default categories for everyday expenses and income sources.
  - Ability to create custom categories with tailored colors and Material icons.
- **Data Export & Portability**:
  - Export transactions and financial history to standard CSV and JSON formats.
  - Native Android share sheet integration to save or transfer backup files.
- **App Security & Privacy**:
  - Optional 4-digit PIN lock overlay to protect financial data from unauthorized local access.
  - Currency selection support (USD $, EUR €, GBP £, JPY ¥, and more).

---

## Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite) with reactive Kotlin Coroutines `Flow`
- **Preferences**: SharedPreferences for user settings and security flags
- **Design System**: Geometric Balance theme with adaptive light & dark color schemes

---

## Project Structure

```text
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/              # Room database, DAOs, Entities, Preferences
│   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   ├── Entities.kt
│   │   │   │   │   │   ├── TransactionDao.kt
│   │   │   │   │   │   ├── CategoryDao.kt
│   │   │   │   │   │   ├── BudgetDao.kt
│   │   │   │   │   │   └── PreferenceRepository.kt
│   │   │   │   │   └── repository/         # Repository abstraction layer
│   │   │   │   │       └── ExpenseRepository.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/         # Custom charts, balance cards, PIN lock
│   │   │   │   │   │   ├── Charts.kt
│   │   │   │   │   │   ├── FintechCards.kt
│   │   │   │   │   │   ├── AppLockOverlay.kt
│   │   │   │   │   │   └── CategoryIconHelper.kt
│   │   │   │   │   ├── navigation/         # Type-safe bottom navigation and routes
│   │   │   │   │   ├── screens/            # UI Screens (Dashboard, Transactions, Budgets, Analytics, Settings, Export)
│   │   │   │   │   ├── theme/              # Color schemes, Typography, M3 Theme
│   │   │   │   │   └── viewmodel/          # ExpenseViewModel state holder
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                        # Drawables, strings, and launcher icons
│   │   └── test/                           # Unit & JVM tests
│   └── build.gradle.kts
├── metadata.json
├── settings.gradle.kts
└── README.md
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK with minimum API 26 (Android 8.0 Oreo) and target API 34

### Building & Running
1. Open the project root directory in **Android Studio**.
2. Sync the project with Gradle files.
3. Select an Android device or emulator running API 26+.
4. Click **Run** (`Shift + F10`) to build and launch the application.

Alternatively, compile from the command line:
```bash
gradle assembleDebug
```
