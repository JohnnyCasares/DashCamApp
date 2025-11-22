# Implementation Plan

- [x] 1. Set up project dependencies and configuration



  - Add Google Drive API, Play Services Auth, and WorkManager dependencies to build.gradle.kts
  - Add Kotest property testing framework for testing
  - Configure OAuth 2.0 client ID in resources
  - Add required permissions to AndroidManifest.xml (INTERNET, ACCESS_NETWORK_STATE)
  - _Requirements: 8.1, 8.2_

- [x] 2. Create core data models


  - Create CloudFileInfo data class with file metadata
  - Create StorageQuota data class with usage information
  - Create UploadQueueItem data class with queue item details
  - Create UploadStatus enum with all status states
  - Create UploadPriority enum with priority levels
  - Create UploadSettings data class with configuration options
  - _Requirements: 8.4_

- [ ]* 2.1 Write property test for data model serialization
  - **Property 13: Settings Persistence**
  - **Validates: Requirements 4.5**



- [ ] 3. Define CloudStorageService interface
  - Create CloudStorageService interface with authentication methods
  - Add upload operation methods (uploadFile, cancelUpload)
  - Add file management methods (deleteFile, getFileInfo, listFiles)


  - Add storage quota method (getStorageQuota)
  - _Requirements: 8.1_

- [ ] 4. Implement GoogleDriveManager authentication
  - Implement OAuth 2.0 authentication flow using Google Sign-In
  - Implement credential storage using EncryptedSharedPreferences
  - Implement isAuthenticated check
  - Implement getAccountInfo to retrieve connected account details
  - Implement disconnect to clear credentials
  - _Requirements: 1.1, 1.2, 1.3, 1.5_

- [ ]* 4.1 Write property test for credential storage
  - **Property 1: Credential Storage on Successful Authentication**
  - **Validates: Requirements 1.3**

- [ ]* 4.2 Write unit tests for authentication flow
  - Test OAuth success scenario


  - Test OAuth failure scenario
  - Test credential retrieval
  - Test disconnect functionality
  - _Requirements: 1.2, 1.3, 1.4, 1.5_

- [ ] 5. Implement GoogleDriveManager upload operations
  - Implement uploadFile with resumable upload protocol
  - Add progress callback support for upload tracking
  - Implement upload cancellation
  - Create or get DashCamVideos folder in Google Drive
  - Handle upload errors and retries
  - _Requirements: 2.2, 2.4, 5.1_

- [ ]* 5.1 Write property test for upload completion marking
  - **Property 4: Upload Completion Marking**
  - **Validates: Requirements 2.4**

- [ ]* 5.2 Write property test for upload progress display
  - **Property 14: Upload Progress Display**
  - **Validates: Requirements 5.1**



- [ ]* 5.3 Write unit tests for upload operations
  - Test successful upload flow
  - Test upload cancellation
  - Test upload error handling
  - Test progress callback invocation
  - _Requirements: 2.2, 2.4, 2.5_

- [ ] 6. Implement GoogleDriveManager file management
  - Implement deleteFile to remove files from Google Drive
  - Implement getFileInfo to retrieve file metadata
  - Implement listFiles to get all videos in DashCamVideos folder
  - Implement getStorageQuota to retrieve Drive storage information
  - _Requirements: 6.1, 6.3, 6.5_

- [ ]* 6.1 Write property test for cloud deletion synchronization
  - **Property 20: Cloud Deletion Synchronization**


  - **Validates: Requirements 6.3**

- [ ]* 6.2 Write unit tests for file management
  - Test file deletion
  - Test file info retrieval
  - Test file listing
  - Test storage quota retrieval
  - _Requirements: 6.1, 6.3, 6.5_

- [x] 7. Extend PreferenceService for upload settings


  - Add methods for auto-upload enabled setting
  - Add methods for WiFi-only mode setting
  - Add methods for minimum battery level setting
  - Add methods for max retry attempts setting
  - Add methods for delete local after upload setting
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 7.1 Write property test for settings persistence
  - **Property 13: Settings Persistence**
  - **Validates: Requirements 4.5**

- [ ] 8. Implement UploadQueueManager
  - Implement addToQueue to add videos to upload queue
  - Implement removeFromQueue to remove videos from queue
  - Implement getQueue to retrieve all queue items
  - Implement clearQueue to remove all items
  - Store queue in persistent storage (JSON file or database)
  - _Requirements: 2.2, 2.3, 5.5_

- [ ]* 8.1 Write property test for upload queue addition
  - **Property 2: Upload Queue Addition**

  - **Validates: Requirements 2.2**

- [ ]* 8.2 Write property test for queue display completeness
  - **Property 17: Queue Display Completeness**
  - **Validates: Requirements 5.5**

- [ ]* 8.3 Write unit tests for queue management
  - Test adding items to queue
  - Test removing items from queue
  - Test queue persistence
  - Test queue retrieval
  - _Requirements: 2.2, 2.3, 5.5_

- [x] 9. Implement upload status tracking in UploadQueueManager

  - Implement getUploadStatus to retrieve status for a video
  - Implement updateUploadStatus to update video status
  - Implement status change notifications
  - Track upload progress percentage
  - Track retry count and last error
  - _Requirements: 2.3, 5.1, 5.2, 7.3_

- [ ]* 9.1 Write property test for queue status display
  - **Property 3: Queue Status Display**
  - **Validates: Requirements 2.3**

- [ ]* 9.2 Write property test for retry attempt limiting
  - **Property 24: Retry Attempt Limiting**
  - **Validates: Requirements 7.3**

- [ ]* 9.3 Write property test for max retry failure handling
  - **Property 25: Max Retry Failure Handling**
  - **Validates: Requirements 7.4**

- [x] 10. Implement upload condition checking in UploadQueueManager


  - Implement canUpload to check if upload conditions are met
  - Check WiFi connectivity when WiFi-only mode is enabled
  - Check battery level against minimum threshold
  - Check storage availability
  - _Requirements: 3.2, 3.3, 3.4, 4.2, 4.3_

- [ ]* 10.1 Write property test for WiFi-only mode enforcement
  - **Property 7: WiFi-Only Mode Enforcement**
  - **Validates: Requirements 3.3**

- [ ]* 10.2 Write property test for WiFi-only setting enforcement
  - **Property 10: WiFi-Only Setting Enforcement**
  - **Validates: Requirements 4.2**

- [ ]* 10.3 Write property test for battery threshold enforcement
  - **Property 8: Battery Threshold Enforcement**
  - **Validates: Requirements 3.4**

- [ ]* 10.4 Write property test for battery level setting enforcement
  - **Property 11: Battery Level Setting Enforcement**
  - **Validates: Requirements 4.3**

- [x] 11. Implement upload processing in UploadQueueManager


  - Implement processQueue to upload queued videos
  - Process queue items by priority order
  - Update status and progress during upload
  - Handle upload success and failure
  - Implement retry logic with exponential backoff
  - _Requirements: 2.4, 3.2, 5.1, 7.1, 7.2, 7.3_

- [ ]* 11.1 Write property test for WiFi upload initiation
  - **Property 6: WiFi Upload Initiation**
  - **Validates: Requirements 3.2**

- [ ]* 11.2 Write property test for upload resumption on condition met
  - **Property 9: Upload Resumption on Condition Met**
  - **Validates: Requirements 3.5**

- [ ]* 11.3 Write property test for network interruption handling
  - **Property 22: Network Interruption Handling**
  - **Validates: Requirements 7.1**

- [ ]* 11.4 Write property test for network restoration resume
  - **Property 23: Network Restoration Resume**
  - **Validates: Requirements 7.2**

- [x] 12. Implement auto-upload functionality


  - Hook into video recording completion to detect new videos
  - Check if auto-upload is enabled in settings
  - Automatically add new videos to upload queue when enabled
  - Respect auto-upload disabled setting
  - _Requirements: 3.1, 4.4_

- [ ]* 12.1 Write property test for auto-upload queue addition
  - **Property 5: Auto-Upload Queue Addition**
  - **Validates: Requirements 3.1**

- [ ]* 12.2 Write property test for auto-upload disable behavior
  - **Property 12: Auto-Upload Disable Behavior**
  - **Validates: Requirements 4.4**

- [x] 13. Implement UploadWorker for background processing



  - Create UploadWorker extending CoroutineWorker
  - Check upload conditions in doWork
  - Call UploadQueueManager.processQueue
  - Configure WorkManager constraints (network, battery)
  - Schedule periodic upload work
  - _Requirements: 3.2, 3.3, 3.4, 7.5_

- [ ]* 13.1 Write property test for app lifecycle upload persistence
  - **Property 26: App Lifecycle Upload Persistence**
  - **Validates: Requirements 7.5**

- [ ]* 13.2 Write unit tests for UploadWorker
  - Test work execution with valid conditions
  - Test work retry with invalid conditions
  - Test work constraints
  - _Requirements: 3.2, 3.3, 3.4_

- [ ] 14. Create Google Drive settings UI
  - Add Google Drive section to Settings activity
  - Add connect/disconnect button with account display
  - Add auto-upload toggle setting item
  - Add WiFi-only mode toggle setting item
  - Add minimum battery level slider setting item
  - Add delete local after upload toggle setting item
  - _Requirements: 1.1, 1.5, 4.1_

- [ ]* 14.1 Write unit tests for settings UI
  - Test settings display
  - Test connect button action
  - Test disconnect button action
  - Test setting changes
  - _Requirements: 1.1, 1.5, 4.1_

- [ ] 15. Update Gallery screen with upload features
  - Add upload status indicator to video items in GalleryAdapter
  - Add upload button to video item menu
  - Display upload progress for uploading videos
  - Add filter options for upload status
  - Update video item layout to show cloud icon
  - _Requirements: 2.1, 2.3, 5.1, 5.2_

- [ ]* 15.1 Write property test for gallery status indicators
  - **Property 15: Gallery Status Indicators**
  - **Validates: Requirements 5.2**

- [ ]* 15.2 Write unit tests for gallery upload UI
  - Test upload button visibility
  - Test status indicator display
  - Test progress bar updates
  - _Requirements: 2.1, 2.3, 5.1, 5.2_

- [ ] 16. Create upload queue viewer screen
  - Create UploadQueueActivity to display upload queue
  - Create UploadQueueAdapter for RecyclerView
  - Display queue items with status and progress
  - Add pause/resume/cancel buttons for each item
  - Add retry button for failed uploads
  - Add clear completed button
  - _Requirements: 5.5_

- [ ]* 16.1 Write unit tests for upload queue screen
  - Test queue display
  - Test pause/resume actions
  - Test cancel action
  - Test retry action
  - _Requirements: 5.5_

- [ ] 17. Create upload history viewer screen
  - Create UploadHistoryActivity to display completed uploads
  - Create UploadHistoryAdapter for RecyclerView
  - Display uploaded videos with timestamps
  - Add option to view video in Google Drive (open web link)
  - Add option to delete from cloud
  - _Requirements: 5.3, 6.1, 6.3_

- [ ]* 17.1 Write property test for upload history completeness
  - **Property 16: Upload History Completeness**
  - **Validates: Requirements 5.3**

- [ ]* 17.2 Write property test for uploaded video actions
  - **Property 18: Uploaded Video Actions**
  - **Validates: Requirements 6.1**

- [ ]* 17.3 Write unit tests for upload history screen
  - Test history display
  - Test view in Drive action
  - Test delete from cloud action
  - _Requirements: 5.3, 6.1, 6.3_

- [ ] 18. Implement local deletion with cloud retention
  - Update video deletion logic in Gallery
  - Check if video is uploaded before deletion
  - Prompt user to keep or delete cloud copy
  - Delete local file while preserving cloud copy if selected
  - Update upload status after deletion
  - _Requirements: 6.2_

- [ ]* 18.1 Write property test for local deletion with cloud retention
  - **Property 19: Local Deletion with Cloud Retention**
  - **Validates: Requirements 6.2**

- [ ]* 18.2 Write unit tests for deletion logic
  - Test local deletion with cloud retention
  - Test local deletion with cloud deletion
  - Test deletion prompt display
  - _Requirements: 6.2_

- [ ] 19. Implement automatic local cleanup for low storage
  - Monitor local storage space
  - When storage is low, identify uploaded videos
  - Delete local copies of uploaded videos (oldest first)
  - Retain cloud copies
  - Notify user of cleanup actions
  - _Requirements: 6.4_

- [ ]* 19.1 Write property test for low storage cleanup
  - **Property 21: Low Storage Cleanup**
  - **Validates: Requirements 6.4**

- [ ]* 19.2 Write unit tests for storage cleanup
  - Test low storage detection
  - Test uploaded video identification
  - Test local file deletion
  - Test cloud copy retention
  - _Requirements: 6.4_

- [ ] 20. Implement storage information display
  - Create storage info UI in settings or separate screen
  - Display local storage usage and available space
  - Display Google Drive storage quota and usage
  - Display number of uploaded videos
  - Add refresh button to update storage info
  - _Requirements: 6.5_

- [ ]* 20.1 Write unit tests for storage info display
  - Test local storage display
  - Test cloud storage display
  - Test storage info refresh
  - _Requirements: 6.5_

- [ ] 21. Add upload notifications
  - Create notification channel for uploads
  - Show notification for upload completion (success)
  - Show notification for upload failure
  - Show notification for storage quota warnings
  - Show notification for authentication expiration
  - Add notification preferences to settings
  - _Requirements: 2.4, 2.5_

- [ ]* 21.1 Write unit tests for notifications
  - Test upload success notification
  - Test upload failure notification
  - Test quota warning notification
  - Test notification preferences
  - _Requirements: 2.4, 2.5_

- [ ] 22. Implement error handling and logging
  - Add comprehensive error handling for all upload operations
  - Log errors with context (file, operation, timestamp)
  - Display user-friendly error messages
  - Implement retry mechanisms for transient errors
  - Handle API rate limiting with exponential backoff
  - _Requirements: 2.5, 5.4, 7.3, 7.4_

- [ ]* 22.1 Write unit tests for error handling
  - Test network error handling
  - Test API error handling
  - Test rate limiting handling
  - Test error message display
  - _Requirements: 2.5, 5.4_

- [ ] 23. Add navigation to new screens
  - Add menu item in Settings to access Google Drive settings
  - Add menu item in Gallery to access upload queue
  - Add menu item in Gallery to access upload history
  - Add menu item to access storage information
  - Update MainActivity navigation if needed
  - _Requirements: 1.1, 5.3, 5.5, 6.5_

- [ ] 24. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
