# UnixTick ⏱️

**UnixTick** is a modern, fast, and 100% offline Android application built with Jetpack Compose for converting Unix timestamps (seconds & milliseconds) and human-readable date/time values across global timezones.

---

## 🌟 Key Features

- 🔄 **Bidirectional Conversion**
  - **Timestamp → Date**: Instantly converts Unix seconds or milliseconds to formatted UTC and local date/time strings.
  - **Date → Timestamp**: Pick any date, time, and timezone to compute exact Unix epoch seconds and milliseconds.
- 🤖 **Smart Unit Auto-Detection**
  - Automatically identifies whether an input timestamp is in **Seconds** (10-digit) or **Milliseconds** (13-digit) with intelligent handling for ambiguous values.
- ⚡ **Live Current Epoch Time**
  - Displays real-time current Unix seconds, milliseconds, UTC, and local time. Tap **Use Current Time** to populate fields instantly.
- 🌍 **Full Timezone & DST Intelligence**
  - Searchable selection across all IANA global timezones (`ZoneId`).
  - Detects Daylight Saving Time (DST) gaps (non-existent local times) and overlaps (ambiguous hours) with helpful notifications.
- 📜 **Saved History & Search**
  - Bookmark and tag conversions with custom labels and notes.
  - Search history by title, notes, ISO 8601 strings, or raw numbers.
  - Star favorite conversions and duplicate or export values with one tap.
- 🎨 **Material 3 & Personalization**
  - Full support for Dynamic Color (Android 12+ Monet theme), System Light/Dark themes, customizable date/time formats, and technical monospace font styling.
- 🔒 **100% Offline & Private**
  - No network permissions required. Zero analytics, no cloud sync—all data stays locally on your device via Jetpack DataStore.

---

## 🛠️ Tech Stack & Architecture

UnixTick is built following modern Android development best practices and guidelines:

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel) with `StateFlow` and unidirectional data flow
- **DateTime Engine**: `java.time` APIs (`Instant`, `ZonedDateTime`, `ZoneId`, `OffsetDateTime`) with 64-bit integer precision
- **Storage**: [Jetpack DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **Build System**: Gradle with Version Catalogs (`libs.versions.toml`) & Kotlin DSL (`build.gradle.kts`)
- **Min SDK**: 26 (Android 8.0) | **Target SDK**: 37 (Android 16 / Latest)

---

## 📁 Project Structure

```
UnixTick/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/unixtick/
│   │   │   │   ├── data/
│   │   │   │   │   ├── converter/      # Core UnixTickConverter logic
│   │   │   │   │   ├── model/          # Data models (UnixTickResult, SavedTimestamp, UserSettings)
│   │   │   │   │   └── storage/        # UnixTickPreferences via DataStore
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/     # Reusable Compose UI cards & modals
│   │   │   │   │   ├── navigation/     # NavHost and screen routes
│   │   │   │   │   ├── screens/        # Converter, History, Settings, About, Splash screens
│   │   │   │   │   ├── theme/          # Material 3 Theme, Color, and Typography
│   │   │   │   │   └── viewmodel/      # UnixTickViewModel & ViewModelFactory
│   │   │   │   └── MainActivity.kt     # Main entry point & Theme host
│   │   │   └── AndroidManifest.xml
│   │   └── test/                       # Unit tests for conversion & persistence
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml              # Dependency version catalog
├── build.gradle.kts
└── README.md
```

---

## 🧪 Testing

UnixTick includes comprehensive unit test suites covering timestamp logic, unit detection edge cases, DST transitions, and DataStore persistence logic:

- `UnixTickConverterTest`: Tests epoch conversions, auto-detection algorithms, edge cases, and ISO formatting.
- `UnixTickPersistenceTest`: Tests history saving, limits, favorites, search filtering, and settings reset.

Run unit tests locally with Gradle:

```bash
./gradlew test
```

---

## 🚀 Getting Started

### Prerequisites

- [Android Studio Ladybug | 2024.2.1](https://developer.android.com/studio) or newer
- JDK 17
- Android SDK 37 (or configured in Android Studio)

### Building & Running

1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/UnixTick.git
   cd UnixTick
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync dependencies.
4. Select a target device or emulator (Android 8.0 / API 26+) and run the project (`Shift + F10` / `Control + R`).

---

## 📄 License

```text
Copyright 2025 Abdullah Ateeq

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
