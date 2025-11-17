# Implementation Plan

## Speed Display Feature Tasks

- [ ] 1. Create SpeedTracker service for GPS-based speed monitoring
  - Implement SpeedTracker class in `service/SpeedTracker.kt` with LocationManager integration
  - Add SpeedUnit enum (MPH, KMH) and speed conversion methods
  - Implement location listener with 1-second update interval
  - Add permission checking method and GPS availability detection
  - Apply stationary threshold (< 1 mph displays as 0) and accuracy filtering
  - Implement lifecycle methods: startTracking() and stopTracking()
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 2. Extend PreferenceManager for speed display settings
  - Add KEY_SPEED_DISPLAY_ENABLED constant and getter/setter methods
  - Add KEY_SPEED_UNIT constant with getter/setter methods
  - Set default values: speed display disabled, unit = "mph"
  - Add error handling consistent with existing preference methods
  - _Requirements: 2.3, 3.5_

- [ ] 3. Extend PermissionHandler for location permissions
  - Add ACCESS_FINE_LOCATION constant to companion object
  - Implement getLocationPermissions() method returning location permission array
  - Implement hasLocationPermission() method to check location permission status
  - Ensure location permission is NOT added to REQUIRED_PERMISSIONS (it's optional)
  - _Requirements: 3.2, 5.1_

- [ ] 4. Create speed display UI in MainActivity layout
  - Add TextView (tvSpeed) above camera preview CardView in activity_main.xml
  - Position at top center with 16dp top margin using ConstraintLayout
  - Set initial visibility to "gone"
  - Create bg_speed_display.xml drawable with semi-transparent black background and rounded corners
  - Style with 24sp bold text, appropriate padding
  - _Requirements: 1.1_

- [ ] 5. Integrate SpeedTracker into MainActivity
  - Add speedTracker nullable property to MainActivity
  - Create initializeSpeedDisplay() method to check preferences and initialize tracker
  - Implement speed update callback to update tvSpeed TextView
  - Add startSpeedTrackingIfEnabled() method called in onResume()
  - Stop speed tracking in onPause() lifecycle method
  - Format speed display as integer with unit label (e.g., "45 mph")
  - Handle placeholder display ("-- mph") when GPS unavailable
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 6. Add location permission handling to MainActivity
  - Extend PermissionRequestContext enum with SPEED_DISPLAY case
  - Add location permission request flow when speed display is enabled
  - Handle permission granted: start speed tracking and show display
  - Handle permission denied: hide speed display and show Toast message
  - Check speed display preference before requesting location permission
  - _Requirements: 3.2, 3.3, 3.4, 5.1, 5.2_

- [ ] 7. Create SpeedDisplayToggleSetting for settings screen
  - Implement SpeedDisplayToggleSetting class in `settings/items/SpeedDisplayToggleSetting.kt`
  - Add ic_speed.xml drawable resource for speedometer icon
  - Implement toggle() method to persist enabled state via PreferenceManager
  - Add callback parameter to notify when toggle changes
  - Integrate with PermissionHandler to request location permission when enabled
  - _Requirements: 3.1, 3.2, 3.5, 3.6_

- [ ] 8. Create SpeedUnitSetting for unit selection
  - Implement SpeedUnitSetting class in `settings/items/SpeedUnitSetting.kt`
  - Add ic_speed_unit.xml drawable resource for unit icon
  - Implement getCurrentUnit() method to display current selection as subtitle
  - Create AlertDialog with radio buttons for mph/km/h selection in onItemClick()
  - Persist unit selection via PreferenceManager
  - Add callback to notify MainActivity of unit changes for immediate display update
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 9. Register settings items in Settings activity
  - Add SpeedDisplayToggleSetting instance to settingsList in Settings.kt
  - Add SpeedUnitSetting instance to settingsList in Settings.kt
  - Pass appropriate callbacks for handling toggle and unit changes
  - Ensure proper ordering in settings list (after existing settings)
  - _Requirements: 2.1, 3.1_

- [ ] 10. Add location permission to AndroidManifest
  - Add ACCESS_FINE_LOCATION permission declaration to AndroidManifest.xml
  - _Requirements: 3.2, 5.1_

- [ ] 11. Create string resources for speed display feature
  - Add strings for setting titles: "Show Speed", "Speed Unit"
  - Add strings for unit labels: "mph", "km/h"
  - Add strings for permission messages and error states
  - Add strings for unit selection dialog title and options
  - _Requirements: 1.5, 2.1, 2.2, 3.1, 3.3, 5.2_

- [ ] 12. Handle edge cases and error scenarios
  - Implement permanent permission denial detection in MainActivity
  - Add dialog with guidance to open system settings when permission permanently denied
  - Handle permission revocation while app is running (check in onResume)
  - Implement GPS error handling with retry logic (max 3 attempts, 5-second delay)
  - Add speed spike filtering (reject changes > 20 mph from previous reading)
  - Implement moving average smoothing (window of 3 readings) for accuracy
  - _Requirements: 3.3, 3.4, 4.5, 5.2, 5.3_
