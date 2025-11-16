# Requirements Document

## Introduction

This feature improves the permission handling mechanism in DashCamApp to request permissions on-demand when users interact with specific features, rather than only requesting them at app startup. This ensures that if users initially deny permissions, they can still be prompted when attempting to use features that require those permissions.

## Glossary

- **DashCamApp**: The Android dashcam application
- **Permission Handler**: The service class responsible for checking and managing runtime permissions
- **Record Button**: The UI button that initiates video recording
- **Gallery Button**: The UI button that opens the gallery view
- **Camera Permission**: Android runtime permission for accessing device camera (Manifest.permission.CAMERA)
- **Audio Permission**: Android runtime permission for recording audio (Manifest.permission.RECORD_AUDIO)
- **Media Permission**: Android runtime permission for reading media files (Manifest.permission.READ_MEDIA_VIDEO), required only on Android 13 (API 33) and above
- **Recording Permissions**: The set of permissions required for video recording (CAMERA and RECORD_AUDIO)
- **Gallery Permissions**: The set of permissions required for gallery access (READ_MEDIA_VIDEO on API 33+, no permissions needed on API 29-32 due to Scoped Storage)

## Requirements

### Requirement 1

**User Story:** As a user who initially denied permissions, I want to be prompted for permissions when I click the Record button, so that I can grant permissions and use the recording feature without restarting the app.

#### Acceptance Criteria

1. WHEN the user clicks the Record button, THE DashCamApp SHALL check if camera and audio permissions are granted
2. IF camera or audio permissions are not granted, THEN THE DashCamApp SHALL request the missing permissions from the user
3. WHEN the user grants the required permissions, THE DashCamApp SHALL initiate video recording
4. IF the user denies the required permissions, THEN THE DashCamApp SHALL display a message indicating that permissions are required for recording

### Requirement 2

**User Story:** As a user who initially denied media permissions on Android 13+, I want to be prompted for media permissions when I click the Gallery button, so that I can grant permissions and view my recorded videos.

#### Acceptance Criteria

1. WHERE the device runs Android 13 or above, WHEN the user clicks the Gallery button, THE DashCamApp SHALL check if READ_MEDIA_VIDEO permission is granted
2. WHERE the device runs Android 13 or above, IF READ_MEDIA_VIDEO permission is not granted, THEN THE DashCamApp SHALL request the permission from the user
3. WHERE the device runs Android 10 to 12, WHEN the user clicks the Gallery button, THE DashCamApp SHALL open the Gallery activity without requesting permissions
4. WHEN the user grants the required media permission, THE DashCamApp SHALL open the Gallery activity
5. IF the user denies the required media permission, THEN THE DashCamApp SHALL display a message indicating that permission is required to access the gallery

### Requirement 3

**User Story:** As a developer, I want the Permission Handler to support checking specific permission subsets, so that different features can request only the permissions they need.

#### Acceptance Criteria

1. THE Permission Handler SHALL provide a method to check if specific permissions are granted
2. THE Permission Handler SHALL provide a method to retrieve the recording permissions (Camera Permission and Audio Permission)
3. THE Permission Handler SHALL provide a method to retrieve the gallery permissions (Media Permission on API 33 and above, empty array on API 29 through 32)
4. THE Permission Handler SHALL maintain backward compatibility with the existing allPermissionsGranted method

### Requirement 4

**User Story:** As a user, I want the camera preview to start automatically when I grant camera permissions, so that I can see what I'm recording immediately.

#### Acceptance Criteria

1. WHEN the user grants Camera Permission and Audio Permission through the Record Button flow, THE DashCamApp SHALL initialize the camera preview
2. WHEN the user grants permissions at app startup, THE DashCamApp SHALL initialize the camera preview
3. THE DashCamApp SHALL initialize the camera only after Camera Permission and Audio Permission are granted
