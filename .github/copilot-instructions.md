# Copilot Instructions for BINGE

This document provides guidance for GitHub Copilot when working with the BINGE repository.

## Project Overview

BINGE is an Android application built with Kotlin that serves as a streaming media player. The app uses ExoPlayer (Media3) for media playback and supports both mobile and Android TV platforms via Leanback.

## Technology Stack

- **Language**: Kotlin 1.9.22
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Build System**: Gradle 8.2.2 with Groovy DSL
- **Architecture**: MVVM (Model-View-ViewModel)

### Key Dependencies

- **UI**: AndroidX AppCompat, Material Design, ConstraintLayout, Leanback (TV)
- **Media**: AndroidX Media3 (ExoPlayer) 1.5.0 with HLS/DASH support
- **Database**: Room 2.6.1
- **Networking**: Retrofit 2.9.0, OkHttp 4.12.0
- **JSON**: Moshi 1.15.0, Gson 2.10.1
- **Images**: Glide 4.16.0
- **Async**: Kotlin Coroutines 1.7.3
- **Background**: WorkManager 2.9.0

## Project Structure

```
app/src/main/java/com/example/stremiompvplayer/
├── adapters/       # RecyclerView adapters
├── data/           # Database entities and DAOs
├── models/         # Data models
├── network/        # API interfaces and network config
├── ui/             # UI components and fragments
├── utils/          # Utility classes
├── viewmodels/     # ViewModels for MVVM
├── workers/        # WorkManager workers
├── MainActivity.kt
├── PlayerActivity.kt
├── SettingsActivity.kt
├── TVSettingsActivity.kt
└── UserSelectionActivity.kt
```

## Build and Test Instructions

### Building the Project

```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

### Linting

```bash
# Run Android lint
./gradlew lint
```

## Coding Standards

### Kotlin Conventions

- Use Kotlin idioms and language features (null safety, data classes, extension functions)
- Prefer `val` over `var` when possible
- Use meaningful variable and function names
- Follow Kotlin coding conventions: https://kotlinlang.org/docs/coding-conventions.html

### Android Best Practices

- Use ViewBinding for view access (enabled in build.gradle)
- Use ViewModel and LiveData for UI state management
- Handle configuration changes properly
- Use coroutines for async operations with appropriate dispatchers
- Follow the single-activity architecture where possible

### Code Style

- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Add KDoc comments for public APIs
- Use `@Suppress` annotations sparingly and document why

## Security Guidelines

- Never commit API keys or secrets to the repository
- The file `app/src/main/java/com/example/stremiompvplayer/utils/Secrets.kt` is gitignored for sensitive data
- Use HTTPS for all network requests
- Validate all user inputs
- Handle sensitive data in memory carefully

## UI/UX Considerations

- Support both mobile and TV form factors
- Follow Material Design guidelines for mobile
- Follow Leanback design guidelines for TV
- Ensure proper D-pad navigation for TV
- Support dark theme

## APK Optimization

The project is configured to generate split APKs by architecture (armeabi-v7a, arm64-v8a, x86, x86_64) to reduce download size. A universal APK is also generated for direct installs.

## Common Tasks

### Adding a New Screen

1. Create the Activity/Fragment in the appropriate package
2. Create a corresponding ViewModel in `viewmodels/`
3. Add the layout XML in `res/layout/`
4. Register the Activity in `AndroidManifest.xml`

### Adding a New API Endpoint

1. Add the endpoint to the appropriate interface in `network/`
2. Create request/response models in `models/`
3. Handle the call in the repository or ViewModel

### Adding Database Entities

1. Create the entity class with Room annotations in `data/`
2. Create a DAO interface for database operations
3. Update the AppDatabase class to include the new entity

## Dependencies

When adding new dependencies:

1. Add to `app/build.gradle`
2. Prefer AndroidX libraries over support libraries
3. Use stable versions unless there's a specific need for alpha/beta
4. Document why the dependency is needed in commit messages
