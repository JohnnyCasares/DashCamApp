# Design Document

## Overview

This feature integrates Google Drive cloud storage into the DashCamApp, enabling users to back up their dashcam videos to the cloud. The implementation uses the Google Drive REST API v3 with OAuth 2.0 authentication, follows the existing interface-based architecture, and provides both manual and automatic upload capabilities with robust error handling and network resilience.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Gallery    │  │   Settings   │  │ Upload Queue │      │
│  │   Screen     │  │   Screen     │  │    Screen    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     Service Layer                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         CloudStorageService Interface                 │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         GoogleDriveManager Implementation             │   │
│  │  • Authentication (OAuth 2.0)                         │   │
│  │  • Upload Operations                                  │   │
│  │  • File Management                                    │   │
│  │  • Network Resilience                                 │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         UploadQueueManager                            │   │
│  │  • Queue Management                                   │   │
│  │  • Upload Scheduling                                  │   │
│  │  • Retry Logic                                        │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   External Services                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Google Drive │  │ Google Auth  │  │ WorkManager  │      │
│  │   REST API   │  │   OAuth 2.0  │  │  (Background)│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

1. **Authentication Flow**: User initiates connection → OAuth 2.0 flow → Store credentials → Enable upload features
2. **Manual Upload Flow**: User selects video → Add to queue → UploadQueueManager schedules → GoogleDriveManager uploads
3. **Auto Upload Flow**: Video recorded → Check settings → Add to queue if enabled → Upload when conditions met
4. **Background Upload**: WorkManager triggers → Check network/battery → Process queue → Update status

## Components and Interfaces

### CloudStorageService Interface

```kotlin
interface CloudStorageService {
    // Authentication
    suspend fun authenticate(context: Context): Result<Boolean>
    suspend fun disconnect(context: Context): Result<Boolean>
    fun isAuthenticated(context: Context): Boolean
    fun getAccountInfo(context: Context): String?
    
    // Upload Operations
    suspend fun uploadFile(
        context: Context,
        filePath: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<String> // Returns file ID
    
    suspend fun cancelUpload(uploadId: String): Result<Boolean>
    
    // File Management
    suspend fun deleteFile(context: Context, fileId: String): Result<Boolean>
    suspend fun getFileInfo(context: Context, fileId: String): Result<CloudFileInfo>
    suspend fun listFiles(context: Context): Result<List<CloudFileInfo>>
    
    // Storage Info
    suspend fun getStorageQuota(context: Context): Result<StorageQuota>
}
```

### GoogleDriveManager Implementation

```kotlin
object GoogleDriveManager : CloudStorageService {
    private const val FOLDER_NAME = "DashCamVideos"
    private var driveService: Drive? = null
    private val activeUploads = mutableMapOf<String, Job>()
    
    // Implementation of CloudStorageService methods
    // Uses Google Drive REST API v3
    // Handles OAuth 2.0 authentication
    // Manages upload sessions with resumable uploads
}
```

### UploadQueueManager

```kotlin
object UploadQueueManager {
    // Queue Management
    fun addToQueue(context: Context, videoPath: String, priority: UploadPriority = UploadPriority.NORMAL)
    fun removeFromQueue(context: Context, videoPath: String)
    fun getQueue(context: Context): List<UploadQueueItem>
    fun clearQueue(context: Context)
    
    // Upload Processing
    suspend fun processQueue(context: Context)
    fun pauseQueue(context: Context)
    fun resumeQueue(context: Context)
    
    // Status Management
    fun getUploadStatus(context: Context, videoPath: String): UploadStatus
    fun updateUploadStatus(context: Context, videoPath: String, status: UploadStatus)
    
    // Conditions Check
    fun canUpload(context: Context): Boolean // Checks WiFi, battery, etc.
}
```

### UploadWorker (Background Processing)

```kotlin
class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // Check upload conditions
        if (!UploadQueueManager.canUpload(applicationContext)) {
            return Result.retry()
        }
        
        // Process upload queue
        UploadQueueManager.processQueue(applicationContext)
        
        return Result.success()
    }
}
```

## Data Models

### CloudFileInfo

```kotlin
data class CloudFileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val createdTime: Long,
    val modifiedTime: Long,
    val webViewLink: String?
)
```

### StorageQuota

```kotlin
data class StorageQuota(
    val limit: Long,
    val usage: Long,
    val usageInDrive: Long
) {
    val availableSpace: Long
        get() = limit - usage
    
    val usagePercentage: Float
        get() = if (limit > 0) (usage.toFloat() / limit.toFloat()) * 100 else 0f
}
```

### UploadQueueItem

```kotlin
data class UploadQueueItem(
    val id: String = UUID.randomUUID().toString(),
    val videoPath: String,
    val fileName: String,
    val fileSize: Long,
    val addedTime: Long,
    val priority: UploadPriority,
    val status: UploadStatus,
    val progress: Float = 0f,
    val cloudFileId: String? = null,
    val retryCount: Int = 0,
    val lastError: String? = null
)
```

### UploadStatus

```kotlin
enum class UploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED
}
```

### UploadPriority

```kotlin
enum class UploadPriority {
    LOW,
    NORMAL,
    HIGH
}
```

### UploadSettings

```kotlin
data class UploadSettings(
    val autoUploadEnabled: Boolean = false,
    val wifiOnlyMode: Boolean = true,
    val minimumBatteryLevel: Int = 20,
    val maxRetryAttempts: Int = 3,
    val deleteLocalAfterUpload: Boolean = false
)
```

## C
orrectness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Credential Storage on Successful Authentication

*For any* successful OAuth authentication response, the system should store the authentication credentials securely in persistent storage.
**Validates: Requirements 1.3**

### Property 2: Upload Queue Addition

*For any* video selected for upload, the video should appear in the upload queue with PENDING status.
**Validates: Requirements 2.2**

### Property 3: Queue Status Display

*For any* video in the upload queue, the system should display its current upload status (PENDING, UPLOADING, COMPLETED, or FAILED).
**Validates: Requirements 2.3**

### Property 4: Upload Completion Marking

*For any* video that completes upload successfully, the system should mark the video's status as COMPLETED and store the cloud file ID.
**Validates: Requirements 2.4**

### Property 5: Auto-Upload Queue Addition

*For any* newly recorded video when auto-upload is enabled, the system should automatically add the video to the upload queue.
**Validates: Requirements 3.1**

### Property 6: WiFi Upload Initiation

*For any* queued video when the device is connected to WiFi and auto-upload is enabled, the system should begin the upload process.
**Validates: Requirements 3.2**

### Property 7: WiFi-Only Mode Enforcement

*For any* upload in progress when WiFi-only mode is enabled and WiFi disconnects, the system should pause the upload.
**Validates: Requirements 3.3**

### Property 8: Battery Threshold Enforcement

*For any* upload attempt when the device battery is below the configured threshold, the system should pause or prevent the upload.
**Validates: Requirements 3.4**

### Property 9: Upload Resumption on Condition Met

*For any* paused upload when all upload conditions (WiFi, battery) are met, the system should resume the upload.
**Validates: Requirements 3.5**

### Property 10: WiFi-Only Setting Enforcement

*For any* upload attempt when WiFi-only mode is enabled, the upload should only proceed if the device is connected to WiFi.
**Validates: Requirements 4.2**

### Property 11: Battery Level Setting Enforcement

*For any* upload attempt when a minimum battery level is configured, the upload should only proceed if the battery level exceeds the threshold.
**Validates: Requirements 4.3**

### Property 12: Auto-Upload Disable Behavior

*For any* newly recorded video when auto-upload is disabled, the video should not be automatically added to the upload queue.
**Validates: Requirements 4.4**

### Property 13: Settings Persistence

*For any* upload settings change, the new settings should be persisted to storage and applied immediately to ongoing operations.
**Validates: Requirements 4.5**

### Property 14: Upload Progress Display

*For any* video with UPLOADING status, the system should display the current upload progress percentage.
**Validates: Requirements 5.1**

### Property 15: Gallery Status Indicators

*For any* video in the gallery, the system should display an upload status indicator reflecting its current state.
**Validates: Requirements 5.2**

### Property 16: Upload History Completeness

*For any* video with COMPLETED status, the video should appear in the upload history with its upload timestamp.
**Validates: Requirements 5.3**

### Property 17: Queue Display Completeness

*For any* video in the upload queue, the video should appear in the queue view in priority order.
**Validates: Requirements 5.5**

### Property 18: Uploaded Video Actions

*For any* video with COMPLETED status, the system should provide an option to view the video in Google Drive.
**Validates: Requirements 6.1**

### Property 19: Local Deletion with Cloud Retention

*For any* uploaded video that is deleted locally, the cloud copy should remain if the "keep cloud copy" option is enabled.
**Validates: Requirements 6.2**

### Property 20: Cloud Deletion Synchronization

*For any* video deleted from Google Drive through the app, the system should remove the video from Drive and update the local upload status to reflect deletion.
**Validates: Requirements 6.3**

### Property 21: Low Storage Cleanup

*For any* uploaded video when local storage is below threshold and auto-cleanup is enabled, the system should delete the local copy while retaining the cloud copy.
**Validates: Requirements 6.4**

### Property 22: Network Interruption Handling

*For any* upload in progress when network connection is lost, the system should pause the upload and retain the current progress.
**Validates: Requirements 7.1**

### Property 23: Network Restoration Resume

*For any* paused upload when network connectivity is restored, the system should resume the upload from the last successful checkpoint.
**Validates: Requirements 7.2**

### Property 24: Retry Attempt Limiting

*For any* upload that fails, the system should retry the upload up to the configured maximum number of attempts.
**Validates: Requirements 7.3**

### Property 25: Max Retry Failure Handling

*For any* upload that reaches the maximum retry attempts, the system should mark the upload as FAILED and stop retrying.
**Validates: Requirements 7.4**

### Property 26: App Lifecycle Upload Persistence

*For any* upload in progress when the app is closed, the system should resume the upload when the app is reopened.
**Validates: Requirements 7.5**

## Error Handling

### Authentication Errors

- **OAuth Failure**: Display user-friendly error message, allow retry, log error details
- **Token Expiration**: Automatically refresh tokens, re-authenticate if refresh fails
- **Network Timeout**: Retry with exponential backoff, inform user of connectivity issues
- **Invalid Credentials**: Clear stored credentials, prompt re-authentication

### Upload Errors

- **Network Interruption**: Pause upload, retain progress, resume when network available
- **Insufficient Storage**: Notify user, provide option to free space or cancel upload
- **File Not Found**: Remove from queue, log error, notify user
- **API Rate Limiting**: Implement exponential backoff, queue uploads for later
- **Large File Timeout**: Use resumable upload protocol, checkpoint progress frequently

### Queue Management Errors

- **Corrupted Queue State**: Rebuild queue from persistent storage, log corruption event
- **Duplicate Entries**: Deduplicate queue, keep most recent entry
- **Invalid Queue Item**: Remove invalid item, log error, continue processing

### Storage Errors

- **Quota Exceeded**: Notify user, pause uploads, provide storage management options
- **Permission Denied**: Check and request necessary permissions, inform user
- **File Access Error**: Retry with backoff, skip file if persistent failure

## Testing Strategy

### Unit Testing

The implementation will include unit tests for:

- **Authentication Flow**: Test OAuth token handling, credential storage, and account info retrieval
- **Upload Queue Operations**: Test adding, removing, and reordering queue items
- **Settings Persistence**: Test saving and loading upload settings
- **Status Tracking**: Test status transitions and progress updates
- **Error Handling**: Test specific error scenarios and recovery mechanisms
- **Network Condition Checks**: Test WiFi detection and battery level checking

### Property-Based Testing

The implementation will use property-based testing to verify correctness properties across many inputs. We will use **Kotest Property Testing** framework for Kotlin/Android.

**Configuration**:
- Each property test will run a minimum of 100 iterations
- Each test will be tagged with a comment referencing the specific correctness property from this design document
- Tag format: `**Feature: google-drive-sync, Property {number}: {property_text}**`

**Property Test Coverage**:
- **Queue Management Properties**: Test that queue operations maintain invariants (no duplicates, correct ordering, status consistency)
- **Upload State Transitions**: Test that status transitions follow valid state machine rules
- **Condition Enforcement**: Test that WiFi-only and battery threshold settings are enforced across various device states
- **Progress Tracking**: Test that progress values remain within valid ranges (0-100%) and increase monotonically
- **Retry Logic**: Test that retry counts increment correctly and max retries are enforced
- **Settings Persistence**: Test that settings round-trip correctly (save then load produces same values)

### Integration Testing

- **End-to-End Upload Flow**: Test complete flow from video selection to successful upload
- **Background Upload**: Test WorkManager integration and background processing
- **Network State Changes**: Test behavior during WiFi/cellular transitions
- **App Lifecycle**: Test upload persistence across app restarts
- **Google Drive API Integration**: Test actual API calls with test account (manual testing)

## Implementation Notes

### Dependencies

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    // Google Drive API
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    
    // WorkManager for background uploads
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Kotest for property-based testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
}
```

### OAuth 2.0 Configuration

1. Create Google Cloud Project
2. Enable Google Drive API
3. Configure OAuth consent screen
4. Create OAuth 2.0 credentials (Android client)
5. Add SHA-1 fingerprint of signing certificate
6. Store client ID in `strings.xml` or BuildConfig

### Resumable Upload Protocol

Google Drive API supports resumable uploads for large files:
- Initial request gets upload URL
- Upload in chunks (minimum 256KB)
- Track progress after each chunk
- Resume from last successful chunk on interruption

### Background Upload Constraints

WorkManager constraints for upload worker:
- Network type: CONNECTED or UNMETERED (based on WiFi-only setting)
- Battery not low (if battery threshold enabled)
- Storage not low
- Exponential backoff for retries

### Security Considerations

- Store OAuth tokens using Android EncryptedSharedPreferences
- Use HTTPS for all API calls (enforced by Google Drive API)
- Validate file paths to prevent directory traversal
- Implement rate limiting to prevent API quota exhaustion
- Clear credentials on logout/disconnect

### Performance Optimizations

- Upload videos in background using WorkManager
- Batch API calls when possible (e.g., listing files)
- Cache storage quota information (refresh periodically)
- Use coroutines for non-blocking operations
- Implement upload queue prioritization
- Compress video metadata before upload (if applicable)

## UI/UX Considerations

### Settings Screen Updates

Add new settings section for Google Drive:
- Connect/Disconnect button with account info
- Auto-upload toggle
- WiFi-only mode toggle
- Minimum battery level slider (0-100%)
- Delete local after upload toggle
- View storage usage button

### Gallery Screen Updates

Add upload indicators to video items:
- Cloud icon with status (pending, uploading, uploaded, failed)
- Progress bar for uploading videos
- Long-press menu with upload/delete options
- Filter to show only uploaded/not uploaded videos

### Upload Queue Screen (New)

Create new activity to display upload queue:
- List of pending uploads with progress
- Pause/resume/cancel buttons
- Retry failed uploads
- Clear completed uploads
- Sort by priority/date

### Notifications

Show notifications for:
- Upload completion (success/failure)
- Storage quota warnings
- Authentication expiration
- Background upload progress (optional, user configurable)

## Future Enhancements

- Support for other cloud providers (Dropbox, OneDrive) using CloudStorageService interface
- Selective sync: choose which videos to auto-upload based on criteria (duration, size, time of day)
- Video compression before upload to save bandwidth and storage
- Shared folders for fleet management
- Download videos from cloud back to device
- Cloud-to-cloud backup (Google Drive to another service)
