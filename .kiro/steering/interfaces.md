# Interface Architecture

## Overview

The DashCamApp now uses interfaces for all service layer components, following SOLID principles and improving testability, maintainability, and flexibility.

## Implemented Interfaces

### 1. PreferenceService
**Implementation:** `PreferenceManager`
**Location:** `interfaces/PreferenceService.kt`

Manages all application preferences including:
- Audio recording settings
- Dual camera mode
- Video quality selection
- Speed display settings
- Speed unit preferences

**Benefits:**
- Easy to mock for unit tests
- Can swap implementations (e.g., DataStore, in-memory for tests)
- Centralized preference contract

### 2. SpeedTrackingService
**Implementation:** `SpeedTracker`
**Location:** `interfaces/SpeedTrackingService.kt`

Provides GPS-based speed tracking with:
- Start/stop tracking
- Permission checking
- Unit conversion (mph/km/h)

**Benefits:**
- Mock speed data for UI testing without GPS hardware
- Alternative implementations (OBD-II, accelerometer)
- Testable without location services

### 3. StorageService
**Implementation:** `Storage`
**Location:** `interfaces/StorageService.kt`

Handles file storage operations:
- Recording directory management

**Benefits:**
- Mock file system for tests
- Alternative storage backends (cloud, external SD)
- No actual I/O in unit tests

### 4. GalleryDataService
**Implementation:** `GalleryService`
**Location:** `interfaces/GalleryDataService.kt`

Manages video gallery data:
- Retrieves recorded videos from MediaStore

**Benefits:**
- Mock video lists for testing
- No MediaStore queries in unit tests
- Alternative data sources possible

### 5. CameraService (Existing)
**Implementation:** `Camera`
**Location:** `interfaces/CameraService.kt`

Camera operations interface (already existed):
- Start camera
- Capture video
- Take photo
- Camera state checking

## Usage Pattern

All service classes now implement their respective interfaces:

```kotlin
// Before
object PreferenceManager {
    fun isAudioRecordingEnabled(context: Context): Boolean
}

// After
object PreferenceManager : PreferenceService {
    override fun isAudioRecordingEnabled(context: Context): Boolean
}
```

Activities and other components can depend on interfaces:

```kotlin
// MainActivity uses interface type
private var speedTracker: SpeedTrackingService? = null
```

## Testing Benefits

### Unit Testing
- Mock implementations for isolated testing
- No Android dependencies in tests
- Fast test execution

### Integration Testing
- Swap real implementations with test doubles
- Control test data precisely
- Reproducible test scenarios

### Example Mock Implementation

```kotlin
class MockPreferenceService : PreferenceService {
    private var audioEnabled = true
    private var speedDisplayEnabled = false
    
    override fun isAudioRecordingEnabled(context: Context) = audioEnabled
    override fun setAudioRecordingEnabled(context: Context, enabled: Boolean) {
        audioEnabled = enabled
    }
    // ... other methods
}
```

## Future Enhancements

With interfaces in place, you can easily:
1. Add dependency injection (Hilt/Koin)
2. Implement repository pattern
3. Add cloud sync capabilities
4. Create test fixtures
5. Support multiple storage backends
6. Add analytics tracking
