# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SpentTracker is an Android expense tracking application built with Kotlin and Jetpack Compose. This is a basic Android project created using the "Empty Activity" template with minimal functionality currently implemented.

## Development Commands

### Build Commands
```bash
# Build debug APK (requires JAVA_HOME environment variable)
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### Testing Commands
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run all tests
./gradlew check
```

### Lint and Code Quality
```bash
# Run Android lint
./gradlew lint

# Run lint for debug build
./gradlew lintDebug
```

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/spenttracker/
│   │   │   ├── MainActivity.kt              # Main activity with basic Compose UI
│   │   │   └── ui/theme/                    # Compose theme files
│   │   ├── res/                             # Android resources (layouts, strings, etc.)
│   │   └── AndroidManifest.xml              # App manifest
│   ├── androidTest/                         # Instrumented tests
│   └── test/                                # Unit tests
├── build.gradle.kts                         # App-level Gradle build script
└── proguard-rules.pro                       # ProGuard configuration
```

## Architecture

Currently implements basic Android architecture:
- **MainActivity**: Main entry point using Jetpack Compose
- **Compose UI**: Modern declarative UI toolkit for Android
- **Material Design 3**: Latest Material Design components

## Key Technologies

- **Language**: Kotlin 2.0.0
- **UI Framework**: Jetpack Compose with Material 3
- **Build Tool**: Gradle with Kotlin DSL
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34

## Development Environment Setup

1. Ensure JAVA_HOME is set in your environment
2. Use Android Studio for development
3. Create AVD (Android Virtual Device) or connect physical device for testing
4. Enable developer options and USB debugging on physical devices

## Current Implementation Status

This is a starter project with:
- Basic Activity using Jetpack Compose
- Default Material 3 theming
- Simple "Hello World" UI
- Standard Android project structure

The project has an extensive development guide (ANDROID_APP_GUIDE.md) that outlines plans for building a full expense tracking app with features like API integration, local database, offline support, and modern Android architecture patterns (MVVM, Repository pattern, etc.).

## Notes

- The project package name is `com.example.spenttracker`
- Currently no external dependencies beyond standard Android/Compose libraries
- No network connectivity or database implementation yet
- Build requires proper Java environment configuration