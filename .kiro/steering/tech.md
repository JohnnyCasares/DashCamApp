# Technology Stack

## Build System

- **Gradle** with Kotlin DSL (`.gradle.kts` files)
- **Android Gradle Plugin**: 8.9.1
- Version catalog managed in `gradle/libs.versions.toml`

## Language & Runtime

- **Kotlin**: 2.0.21
- **JVM Target**: Java 11
- **Compile SDK**: 35 (Android 15)
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 35

## Key Libraries & Frameworks

### AndroidX Core
- `androidx.appcompat`: 1.7.1
- `androidx.core:core-ktx`: 1.16.0
- `androidx.activity`: 1.10.1
- `androidx.constraintlayout`: 2.2.1

### CameraX (1.5.0-rc01)
- `camera-core`
- `camera-camera2`
- `camera-video`
- `camera-lifecycle`
- `camera-view`
- `camera-extensions`

### UI
- Material Design Components: 1.12.0
- View Binding enabled

### Testing
- JUnit: 4.13.2
- AndroidX Test (JUnit): 1.3.0
- Espresso: 3.7.0

## Common Commands

### Build
```bash
gradlew build
```

### Clean Build
```bash
gradlew clean build
```

### Install Debug APK
```bash
gradlew installDebug
```

### Run Tests
```bash
gradlew test
gradlew connectedAndroidTest
```

### Generate Release APK
```bash
gradlew assembleRelease
```

## Build Configuration

- View Binding is enabled for type-safe view access
- ProGuard disabled in debug builds
- Application ID: `com.kasahirotech.dashcamapp`
