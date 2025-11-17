# Speed Display Feature Design

## Overview

This design document outlines the implementation of a GPS-based speed display feature for the DashCamApp. The feature will show real-time vehicle speed above the camera preview on the main recording screen, with user-configurable units (mph or km/h) and an enable/disable toggle in settings.

The implementation follows the existing app architecture with feature-based organization, using the established patterns for settings management, permission handling, and UI components.

## Architecture

### High-Level Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        MainActivity                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Speed Display TextView                     │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Camera Preview (CardView)                  │ │
│  └────────────────────────────────────────────────────────┘ │
│                           │                                  │
│                           ▼                                  │
│                  ┌─────────────────┐                        │
│                  │  SpeedTracker   │                        │
│                  │   (Service)     │                        │
│                  └─────────────────┘                        │
│                           │                                  │
│                           ▼                                  │
│                  Android LocationManager                     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     Settings Screen                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  SpeedDisplayToggleSetting (Enable/Disable)            │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  SpeedUnitSetting (mph/km/h)                           │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                  ┌─────────────────┐
                  │ PreferenceManager│
                  │  (Persistence)   │
                  └─────────────────┘
```

### Data Flow

1. User enables speed display in Settings → Preference saved → Location permission requested
2. MainActivity checks if speed display is enabled → Initializes SpeedTracker
3. SpeedTracker registers location listener → Receives GPS updates
4. GPS provides speed data → SpeedTracker converts to user's preferred unit
5. SpeedTracker updates UI via callback → TextView displays formatted speed

## Components and Interfaces

### 1. SpeedTracker Service

**Location:** `app/src/main/java/com/kasahirotech/dashcamapp/service/SpeedTracker.kt`

**Responsibility:** Manages GPS location updates and speed calculation

```kotlin
class SpeedTracker(
    private val context: Context,
    private val onSpeedUpdate: (speed: Float, unit: SpeedUnit) -> Unit
) {
    enum class SpeedUnit { MPH, KMH }
    
    private val locationManager: LocationManager
    private var isTracking: Boolean = false
    
    fun startTracking()
    fun stopTracking()
    fun hasLocationPermission(): Boolean
    private fun convertSpeed(metersPerSecond: Float, unit: SpeedUnit): Float
    private val locationListener: LocationListener
}
```

**Key Methods:**
- `startTracking()`: Registers location listener with GPS provider
- `stopTracking()`: Unregisters location listener and cleans up resources
- `hasLocationPermission()`: Checks if ACCESS_FINE_LOCATION permission is granted
- `convertSpeed()`: Converts m/s to mph or km/h based on user preference
- `locationListener`: Receives location updates and extracts speed data

**Implementation Details:**
- Uses `LocationManager.GPS_PROVIDER` for accurate speed data
- Requests updates with minimum time interval of 1000ms (1 second)
- Minimum distance change of 0 meters (update on any movement)
- Extracts speed directly from `Location.getSpeed()` (returns m/s)
- Applies threshold of 0.447 m/s (1 mph) to filter stationary noise
- Conversion factors: 1 m/s = 2.237 mph, 1 m/s = 3.6 km/h

### 2. MainActivity Updates

**Location:** `app/src/main/java/com/kasahirotech/dashcamapp/MainActivity.kt`

**New Components:**
- `speedTracker: SpeedTracker?` - Instance of speed tracking service
- Speed display TextView (added to layout)
- Location permission handling in existing permission flow

**Lifecycle Integration:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // Existing initialization...
    initializeSpeedDisplay()
}

override fun onResume() {
    super.onResume()
    // Existing camera initialization...
    startSpeedTrackingIfEnabled()
}

override fun onPause() {
    super.onPause()
    speedTracker?.stopTracking()
}
```

**Permission Handling:**
- Add `ACCESS_FINE_LOCATION` to permission requests when speed display is enabled
- Extend existing `PermissionRequestContext` enum with `SPEED_DISPLAY` case
- Check speed display preference before requesting location permission
- Handle permission denial gracefully (hide speed display, show message)

### 3. Settings Items

#### SpeedDisplayToggleSetting

**Location:** `app/src/main/java/com/kasahirotech/dashcamapp/settings/items/SpeedDisplayToggleSetting.kt`

**Responsibility:** Toggle to enable/disable speed display feature

```kotlin
class SpeedDisplayToggleSetting(
    private val context: Context,
    private val onToggle: () -> Unit
) : SettingItem {
    override val icon: Int = R.drawable.ic_speed
    override val title: String = "Show Speed"
    var isEnabled: Boolean
    
    fun toggle()
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?)
}
```

**Behavior:**
- When toggled ON: Check location permission, request if not granted
- When toggled OFF: Disable speed tracking, no permission request
- Persists state via PreferenceManager
- Triggers callback to refresh MainActivity if needed

#### SpeedUnitSetting

**Location:** `app/src/main/java/com/kasahirotech/dashcamapp/settings/items/SpeedUnitSetting.kt`

**Responsibility:** Selection dialog for speed unit (mph/km/h)

```kotlin
class SpeedUnitSetting(
    private val context: Context,
    private val onUnitChange: () -> Unit
) : SettingItem {
    override val icon: Int = R.drawable.ic_speed_unit
    override val title: String = "Speed Unit"
    
    fun getCurrentUnit(): String
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?)
}
```

**Behavior:**
- Displays current selection as subtitle (e.g., "mph" or "km/h")
- Shows dialog with radio buttons for unit selection
- Persists selection via PreferenceManager
- Triggers callback to update speed display immediately
- Only enabled when speed display is enabled

### 4. PreferenceManager Extensions

**Location:** `app/src/main/java/com/kasahirotech/dashcamapp/service/PreferenceManager.kt`

**New Methods:**
```kotlin
object PreferenceManager {
    private const val KEY_SPEED_DISPLAY_ENABLED = "speed_display_enabled"
    private const val KEY_SPEED_UNIT = "speed_unit"
    private const val DEFAULT_SPEED_DISPLAY_ENABLED = false
    private const val DEFAULT_SPEED_UNIT = "mph"
    
    fun isSpeedDisplayEnabled(context: Context): Boolean
    fun setSpeedDisplayEnabled(context: Context, enabled: Boolean)
    fun getSpeedUnit(context: Context): String
    fun setSpeedUnit(context: Context, unit: String)
}
```

### 5. PermissionHandler Extensions

**Location:** `app/src/main/java/com/kasahirotech/dashcamapp/service/PermissionHandler.kt`

**New Methods:**
```kotlin
class PermissionHandler(private val context: Context) {
    fun getLocationPermissions(): Array<String>
    fun hasLocationPermission(): Boolean
    
    companion object {
        const val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    }
}
```

**Implementation:**
- `getLocationPermissions()`: Returns array with ACCESS_FINE_LOCATION
- `hasLocationPermission()`: Checks if location permission is granted
- Does not add location to REQUIRED_PERMISSIONS (it's optional)

## Data Models

### SpeedData (Internal to SpeedTracker)

```kotlin
data class SpeedData(
    val speedMps: Float,        // Speed in meters per second (raw from GPS)
    val speedDisplay: Float,    // Converted speed in user's unit
    val unit: SpeedUnit,        // Current display unit
    val timestamp: Long,        // System time of measurement
    val accuracy: Float         // GPS accuracy in meters
)
```

### Preference Keys

```kotlin
// Stored in SharedPreferences
KEY_SPEED_DISPLAY_ENABLED: Boolean  // Default: false
KEY_SPEED_UNIT: String              // Values: "mph" or "kmh", Default: "mph"
```

## UI Design

### MainActivity Layout Updates

**File:** `app/src/main/res/layout/activity_main.xml`

Add TextView above the camera preview CardView:

```xml
<TextView
    android:id="@+id/tvSpeed"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="-- mph"
    android:textSize="24sp"
    android:textStyle="bold"
    android:textColor="@color/md_theme_onSurface"
    android:padding="8dp"
    android:background="@drawable/bg_speed_display"
    android:visibility="gone"
    app:layout_constraintTop_toTopOf="@id/cardView"
    app:layout_constraintStart_toStartOf="@id/cardView"
    app:layout_constraintEnd_toEndOf="@id/cardView"
    android:layout_marginTop="16dp"/>
```

**Visual Positioning:**
- Positioned at top center of camera preview
- Semi-transparent background for readability
- Large, bold text for at-a-glance viewing
- Hidden by default (visibility="gone")
- Shows when speed display is enabled and permission granted

### Speed Display Background

**File:** `app/src/main/res/drawable/bg_speed_display.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#CC000000"/>
    <corners android:radius="8dp"/>
    <padding android:left="12dp" android:right="12dp" 
             android:top="6dp" android:bottom="6dp"/>
</shape>
```

### Settings Icons

**Required Drawables:**
- `ic_speed.xml`: Speedometer icon for speed display toggle
- `ic_speed_unit.xml`: Unit icon for speed unit setting

### Speed Unit Selection Dialog

**Implementation:** AlertDialog with radio buttons
- Title: "Select Speed Unit"
- Options: "Miles per hour (mph)", "Kilometers per hour (km/h)"
- Single selection with immediate apply
- Follows Material Design guidelines

## Error Handling

### GPS Unavailable Scenarios

1. **Location Permission Denied:**
   - Hide speed display TextView
   - Show Toast: "Location permission required for speed display"
   - Keep setting toggle in OFF state
   - Provide guidance in settings to enable permission

2. **GPS Disabled:**
   - Display placeholder: "-- mph" or "-- km/h"
   - Continue attempting to get location updates
   - No error message (user may be indoors temporarily)

3. **No GPS Signal:**
   - Display placeholder: "-- mph" or "-- km/h"
   - Wait for signal acquisition
   - Common in tunnels, parking garages

4. **Location Service Error:**
   - Log error with TAG "SpeedTracker"
   - Display placeholder value
   - Attempt to restart tracking after 5 seconds
   - Maximum 3 retry attempts

### Permission Flow Edge Cases

1. **User Denies Permission Permanently:**
   - Detect using `shouldShowRequestPermissionRationale()`
   - Show dialog: "Enable location in Settings to use speed display"
   - Provide button to open app settings
   - Keep speed display toggle OFF

2. **Permission Granted After Denial:**
   - Automatically start speed tracking
   - Show speed display without app restart
   - Update UI immediately

3. **Permission Revoked While App Running:**
   - Detect in onResume()
   - Stop speed tracking
   - Hide speed display
   - Update settings toggle state

### Data Quality Issues

1. **Inaccurate Speed Readings:**
   - Filter speeds with accuracy > 50 meters
   - Apply smoothing with moving average (window of 3 readings)
   - Ignore first reading after GPS acquisition

2. **Speed Spikes:**
   - Reject readings that differ by > 20 mph from previous
   - Likely GPS glitches or multipath errors

3. **Stationary Drift:**
   - Apply threshold: speeds < 1 mph display as "0"
   - Prevents display of 0.2, 0.5 mph when parked

## Testing Strategy

### Unit Tests

**File:** `app/src/test/java/com/kasahirotech/dashcamapp/SpeedTrackerTest.kt`

Test cases:
- Speed conversion accuracy (m/s to mph, m/s to km/h)
- Threshold application for stationary vehicles
- Unit preference handling
- Permission check logic

**File:** `app/src/test/java/com/kasahirotech/dashcamapp/PreferenceManagerTest.kt`

Test cases:
- Speed display enabled/disabled persistence
- Speed unit preference persistence
- Default values on first launch

### Integration Tests

**File:** `app/src/androidTest/java/com/kasahirotech/dashcamapp/SpeedDisplayIntegrationTest.kt`

Test scenarios:
- Enable speed display from settings → Permission requested
- Grant permission → Speed display appears
- Change unit preference → Display updates
- Disable speed display → Display hidden
- App restart → Preferences retained

### Manual Testing Checklist

1. **Initial Setup:**
   - [ ] Fresh install, speed display OFF by default
   - [ ] Enable speed display, permission requested
   - [ ] Grant permission, speed appears
   - [ ] Deny permission, speed hidden with message

2. **Unit Switching:**
   - [ ] Change from mph to km/h, display updates
   - [ ] Values convert correctly (multiply by 1.609)
   - [ ] Unit label updates immediately

3. **GPS Scenarios:**
   - [ ] Stationary: displays "0 mph"
   - [ ] Moving: displays accurate speed
   - [ ] GPS lost: displays "-- mph"
   - [ ] GPS regained: resumes display

4. **Permission Scenarios:**
   - [ ] Deny permission, then enable in settings
   - [ ] Revoke permission while app running
   - [ ] Permanently deny, guidance shown

5. **Lifecycle:**
   - [ ] Speed tracking stops when app paused
   - [ ] Speed tracking resumes when app resumed
   - [ ] Preferences persist across app restarts

## Performance Considerations

### Battery Impact

- GPS updates at 1-second intervals (standard for navigation)
- Stop tracking when app is paused/backgrounded
- Use `GPS_PROVIDER` only (no NETWORK_PROVIDER fallback)
- Expected battery impact: ~2-3% per hour (similar to navigation apps)

### Memory Usage

- SpeedTracker: ~1KB (lightweight service)
- Location listener: Managed by Android system
- No data buffering or history storage
- Minimal UI updates (TextView only)

### UI Performance

- Speed updates on main thread via callback
- TextView updates are lightweight (no layout changes)
- No animations or complex rendering
- 60 FPS maintained during camera preview

## Dependencies

### Android Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Android APIs

- `android.location.LocationManager`: GPS access
- `android.location.LocationListener`: Location updates
- `android.location.Location`: Speed data extraction

### Existing App Components

- `PreferenceManager`: Settings persistence
- `PermissionHandler`: Permission management
- `SettingItem` interface: Settings UI pattern
- `SettingsAdapter`: Settings list display

### No New External Libraries Required

All functionality uses Android SDK APIs. No additional Gradle dependencies needed.

## Implementation Notes

### Code Style

- Follow existing Kotlin conventions in codebase
- Use KDoc comments for public methods
- Apply null safety with nullable types where appropriate
- Use `companion object` for constants

### Resource Naming

- Drawables: `ic_speed.xml`, `ic_speed_unit.xml`
- IDs: `tvSpeed`, `speedDisplayToggle`, `speedUnitSetting`
- Strings: `speed_display_title`, `speed_unit_title`, etc.
- Preferences: `speed_display_enabled`, `speed_unit`

### Localization

All user-facing strings should be in `strings.xml`:
- Setting titles and descriptions
- Permission rationale messages
- Unit labels (mph, km/h)
- Error messages

### Backwards Compatibility

- Minimum SDK 29 (Android 10) - no compatibility issues
- LocationManager API stable since API 1
- No deprecated APIs used
