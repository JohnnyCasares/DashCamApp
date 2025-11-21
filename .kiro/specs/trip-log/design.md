# Trip Log Feature Design

## Overview

The Trip Log feature extends the DashCamApp to record GPS coordinates and speed data during video recording sessions. The feature integrates seamlessly with the existing architecture by leveraging established service interfaces (SpeedTrackingService, PreferenceService, StorageService) and the SettingItem pattern.

Trip logs are stored as plain text files in a dedicated directory within app storage, separate from video files. Users control the feature through a toggle setting and can access their logs through an action in the settings screen.

## Architecture

### High-Level Architecture

The trip log feature follows the existing layered architecture:

```
┌─────────────────────────────────────────┐
│         UI Layer (Settings)             │
│  - TripLogToggleSetting (SettingItem)   │
│  - TripLogViewerActivity                │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│         Service Layer                   │
│  - TripLogService (new interface)       │
│  - TripLogger (implementation)          │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│      Existing Services (reused)         │
│  - SpeedTrackingService                 │
│  - PreferenceService                    │
│  - StorageService                       │
└─────────────────────────────────────────┘
```

### Integration Points

1. **MainActivity**: Manages TripLogger lifecycle (start/stop) based on recording state
2. **PreferenceService**: Stores trip log enabled/disabled preference
3. **SpeedTrackingService**: Provides GPS location and speed data
4. **StorageService**: Extended to provide trip logs directory
5. **Settings Screen**: Displays toggle and view logs action


## Components and Interfaces

### 1. TripLogService Interface

New service interface following the established pattern:

```kotlin
interface TripLogService {
    /**
     * Starts logging trip data to a new log file.
     * @param context Application context
     * @return true if logging started successfully, false otherwise
     */
    fun startLogging(context: Context): Boolean
    
    /**
     * Stops logging and finalizes the current log file.
     */
    fun stopLogging()
    
    /**
     * Checks if trip logging is currently active.
     * @return true if logging is active, false otherwise
     */
    fun isLogging(): Boolean
    
    /**
     * Gets list of all trip log files.
     * @param context Application context
     * @return List of trip log files, sorted by date (newest first)
     */
    fun getTripLogFiles(context: Context): List<File>
}
```

### 2. TripLogger Implementation

Concrete implementation of TripLogService:

**Responsibilities:**
- Create and manage trip log files
- Subscribe to location updates from SpeedTrackingService
- Format and write log entries at regular intervals
- Handle file I/O operations safely
- Manage logging lifecycle

**Key Design Decisions:**
- Log interval: 5 seconds (balances detail vs. file size)
- File naming: `trip_log_YYYYMMDD_HHMMSS.txt`
- Location updates: Reuse existing SpeedTracker with custom callback
- Thread safety: All file operations on background thread


### 3. TripLogToggleSetting

Settings item for enabling/disabling trip logging:

```kotlin
class TripLogToggleSetting(
    private val context: Context
) : SettingItem {
    override val icon: Int = R.drawable.ic_trip_log
    override val title: String = "Trip Log"
    
    var isEnabled: Boolean
    
    fun toggle()
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?)
}
```

**Behavior:**
- Toggle switch controls enabled/disabled state
- Click action opens TripLogViewerActivity to view logs
- Follows pattern established by SpeedDisplayToggleSetting

### 4. TripLogViewerActivity

Activity for viewing and managing trip logs:

**Features:**
- List all trip log files with timestamps
- View individual log file contents
- Share log files via Android share sheet
- Delete individual log files
- Empty state when no logs exist

**UI Components:**
- RecyclerView for log file list
- Detail view for log contents
- Action buttons (share, delete)

### 5. StorageService Extension

Extend existing StorageService interface:

```kotlin
interface StorageService {
    fun getPrivateRecordingsDirectory(): File?
    
    /**
     * Gets the directory for storing trip log files.
     * Creates the directory if it doesn't exist.
     * @return File object representing the logs directory, or null if unavailable
     */
    fun getTripLogsDirectory(): File?
}
```

### 6. PreferenceService Extension

Extend existing PreferenceService interface:

```kotlin
interface PreferenceService {
    // ... existing methods ...
    
    /**
     * Checks if trip logging is enabled in user preferences.
     */
    fun isTripLogEnabled(context: Context): Boolean
    
    /**
     * Sets the trip logging preference.
     */
    fun setTripLogEnabled(context: Context, enabled: Boolean)
}
```


## Data Models

### Trip Log File Format

Plain text format with human-readable structure:

```
Trip Log
Start Time: 2025-11-20 14:30:15
Format Version: 1.0
---
Timestamp,Latitude,Longitude,Speed (mph)
2025-11-20 14:30:20,37.7749,-122.4194,25.3
2025-11-20 14:30:25,37.7750,-122.4195,26.1
2025-11-20 14:30:30,37.7751,-122.4196,27.5
...
```

**Format Specification:**
- Header section with metadata (start time, format version)
- Separator line (`---`)
- CSV-style data with column headers
- Timestamp in ISO 8601 format (YYYY-MM-DD HH:MM:SS)
- Coordinates in decimal degrees (6 decimal places)
- Speed with unit label in header
- One entry per log interval (5 seconds)

### File Naming Convention

Pattern: `trip_log_YYYYMMDD_HHMMSS.txt`

Examples:
- `trip_log_20251120_143015.txt`
- `trip_log_20251120_160530.txt`

**Benefits:**
- Sortable by filename
- Human-readable timestamps
- Unique per recording session
- No special characters

### Location Data Point

Internal data structure for log entries:

```kotlin
data class TripLogEntry(
    val timestamp: Long,           // Unix timestamp in milliseconds
    val latitude: Double,          // Decimal degrees
    val longitude: Double,         // Decimal degrees
    val speed: Float,              // Speed in current unit
    val speedUnit: String          // "mph" or "kmh"
)
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, several properties can be consolidated:
- Properties 2.2 and 5.2 both test log entry structure - combined into Property 2
- Properties 3.1 and 3.2 both test file creation - combined into Property 3
- Properties 5.1, 5.3, and 5.4 all test file format - combined into Property 5

### Core Properties

**Property 1: Preference persistence**
*For any* initial preference state (enabled or disabled), toggling the trip log setting should result in the preference being persisted with the opposite value
**Validates: Requirements 1.2**

**Property 2: Log entry completeness**
*For any* location data point (timestamp, latitude, longitude, speed), when logged to a file, the written entry should contain all four fields in a parseable format
**Validates: Requirements 2.2, 5.2**

**Property 3: Interval-based logging**
*For any* logging session lasting multiple intervals, the log file should contain multiple entries with timestamps separated by approximately the log interval duration
**Validates: Requirements 2.1, 2.3**

**Property 4: File location and naming**
*For any* new logging session, the created log file should be located in the dedicated logs directory and have a filename matching the pattern `trip_log_YYYYMMDD_HHMMSS.txt`
**Validates: Requirements 3.1, 3.2**

**Property 5: File format compliance**
*For any* trip log file, it should be plain text, contain a header with start time and format version, use CSV-style data rows, and represent coordinates in decimal degrees
**Validates: Requirements 5.1, 5.2, 5.3, 5.4**


## Error Handling

### GPS Data Unavailable

**Scenario:** GPS signal is lost or location data is temporarily unavailable

**Handling:**
- Skip writing entry for that interval (don't write invalid data)
- Continue monitoring for next interval
- Log warning to Android logcat
- Don't crash or corrupt existing log file

### Storage Unavailable

**Scenario:** App storage is full or inaccessible

**Handling:**
- Fail gracefully when starting logging
- Return false from `startLogging()`
- Show toast message to user: "Unable to start trip logging: storage unavailable"
- Don't crash the app or interfere with video recording

### Permission Denied

**Scenario:** Location permission is revoked while logging is active

**Handling:**
- Stop logging gracefully
- Finalize current log file
- Update UI to reflect logging stopped
- Show toast: "Trip logging stopped: location permission required"

### File I/O Errors

**Scenario:** File write fails due to I/O error

**Handling:**
- Log error to Android logcat
- Attempt to close file gracefully
- Mark logging as stopped
- Don't retry indefinitely (avoid battery drain)

### Concurrent Access

**Scenario:** Multiple threads attempt to write to log file

**Handling:**
- Use synchronized blocks or coroutines with mutex
- Ensure only one writer at a time
- Queue writes if necessary


## Testing Strategy

### Unit Testing

Unit tests will verify specific behaviors and edge cases:

**TripLogger Tests:**
- File creation with correct naming pattern
- Directory creation when missing
- Header format correctness
- Entry formatting with various coordinate values
- Graceful handling of null/invalid location data
- Proper file closure on stop

**PreferenceManager Tests:**
- Get/set trip log enabled preference
- Default value when preference not set

**StorageService Tests:**
- Trip logs directory creation
- Directory path correctness

**TripLogToggleSetting Tests:**
- Toggle changes preference
- Initial state reflects stored preference
- Click action opens viewer activity

### Property-Based Testing

Property-based tests will verify universal properties across many inputs using a Kotlin PBT library (e.g., Kotest Property Testing or junit-quickcheck).

**Configuration:**
- Minimum 100 iterations per property test
- Random generation of location data, timestamps, and preferences
- Each test tagged with property reference from design document

**Property Tests:**

1. **Preference Persistence Property**
   - Generate random initial preference states
   - Toggle and verify persistence
   - Tag: `Feature: trip-log, Property 1: Preference persistence`

2. **Log Entry Completeness Property**
   - Generate random location data points
   - Write and parse entries
   - Verify all fields present
   - Tag: `Feature: trip-log, Property 2: Log entry completeness`

3. **Interval-Based Logging Property**
   - Generate random logging durations
   - Verify entry count and timing
   - Tag: `Feature: trip-log, Property 3: Interval-based logging`

4. **File Location and Naming Property**
   - Generate random session start times
   - Verify file paths and names
   - Tag: `Feature: trip-log, Property 4: File location and naming`

5. **File Format Compliance Property**
   - Generate random log sessions
   - Parse and validate format
   - Tag: `Feature: trip-log, Property 5: File format compliance`

### Integration Testing

Integration tests will verify end-to-end workflows:

- Enable trip logging → start recording → verify log file created
- Disable trip logging → start recording → verify no log file created
- View logs screen → verify list displays existing files
- Share log file → verify Android share sheet opens

### Edge Cases

Edge cases will be covered by property test generators:

- Empty GPS data (no location available)
- Very long logging sessions (hours)
- Rapid start/stop cycles
- Special characters in timestamps
- Boundary coordinate values (poles, date line)


## Implementation Details

### Logging Lifecycle

**Start Logging (when recording starts):**
1. Check if trip log preference is enabled
2. If disabled, skip logging
3. If enabled, check location permission
4. Create logs directory if needed
5. Generate filename with current timestamp
6. Create new log file
7. Write header section
8. Start periodic timer (5 second interval)
9. Subscribe to location updates

**During Logging:**
1. Timer fires every 5 seconds
2. Get current location from SpeedTrackingService
3. If location available, format entry and append to file
4. If location unavailable, skip this interval
5. Flush file buffer to ensure data is written

**Stop Logging (when recording stops):**
1. Cancel periodic timer
2. Unsubscribe from location updates
3. Flush and close log file
4. Mark logging as inactive

### Threading Model

- **Main Thread:** UI interactions, preference reads/writes
- **Background Thread:** File I/O operations (write, close)
- **Location Thread:** GPS updates from LocationManager

Use Kotlin Coroutines for background operations:
```kotlin
private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
```

### Memory Management

- Don't buffer all entries in memory
- Write entries immediately to file
- Use BufferedWriter for efficient I/O
- Close resources in finally blocks
- Cancel coroutines on stop

### Battery Optimization

- Reuse existing SpeedTracker instance (don't create duplicate location listeners)
- Use 5-second interval (balance detail vs. battery)
- Stop logging immediately when recording stops
- Don't retry failed writes indefinitely

### File Size Estimation

Typical entry: ~60 bytes
```
2025-11-20 14:30:20,37.774900,-122.419400,25.3
```

For 1-hour recording:
- 720 entries (3600 seconds / 5 seconds)
- ~43 KB file size
- Negligible storage impact

