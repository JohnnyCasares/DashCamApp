# Requirements Document

## Introduction

This feature enables users to configure the video recording quality for the DashCam application. Currently, the application hardcodes video quality to `Quality.HIGHEST`, which may not be suitable for all users due to storage constraints, device capabilities, or user preferences. This feature will provide a settings interface allowing users to select from six quality levels: LOWEST, SD, HD, FHD, HIGHEST, and UHD.

## Glossary

- **DashCamApp**: The Android dashcam application that records video using the device's camera
- **Camera_Service**: The service component responsible for camera initialization and video recording operations
- **Settings_Screen**: The activity that displays configurable application settings
- **Quality_Selector**: The CameraX component that determines video recording quality
- **Preference_Manager**: The service that persists and retrieves user preferences using SharedPreferences
- **Quality_Setting_Item**: The UI component in the settings list that allows quality selection
- **Video_Quality**: One of six predefined quality levels from the CameraX Quality enum (LOWEST, SD, HD, FHD, HIGHEST, UHD)

## Requirements

### Requirement 1

**User Story:** As a dashcam user, I want to select the video recording quality from available options, so that I can balance video clarity with storage consumption based on my needs.

#### Acceptance Criteria

1. WHEN the Settings_Screen is displayed, THE Settings_Screen SHALL present a Video_Quality setting item with an icon and title
2. WHEN the user taps the Video_Quality setting item, THE Settings_Screen SHALL display a selection dialog containing all six quality options (LOWEST, SD, HD, FHD, HIGHEST, UHD)
3. WHEN the user selects a Video_Quality option from the dialog, THE Preference_Manager SHALL persist the selected quality value
4. WHEN the user selects a Video_Quality option from the dialog, THE Settings_Screen SHALL update the displayed current quality value
5. WHEN the Settings_Screen is displayed, THE Settings_Screen SHALL show the currently selected Video_Quality as a subtitle or secondary text

### Requirement 2

**User Story:** As a dashcam user, I want the camera to use my selected video quality when recording, so that my recordings match my quality preferences.

#### Acceptance Criteria

1. WHEN the Camera_Service initializes the Quality_Selector, THE Camera_Service SHALL retrieve the user's selected Video_Quality from Preference_Manager
2. WHEN the Camera_Service builds the Recorder, THE Camera_Service SHALL configure the Quality_Selector with the user's selected Video_Quality
3. IF no Video_Quality preference exists, THEN THE Camera_Service SHALL use HIGHEST as the default quality
4. WHEN the user changes the Video_Quality setting, THE Camera_Service SHALL apply the new quality on the next camera initialization

### Requirement 3

**User Story:** As a dashcam user, I want to understand what each quality level means, so that I can make an informed decision about which quality to select.

#### Acceptance Criteria

1. WHEN the quality selection dialog is displayed, THE Settings_Screen SHALL show descriptive labels for each quality option (e.g., "UHD (4K)", "FHD (1080p)", "HD (720p)")
2. WHEN the quality selection dialog is displayed, THE Settings_Screen SHALL display quality options in descending order from highest to lowest quality
3. WHEN the quality selection dialog is displayed, THE Settings_Screen SHALL indicate the currently selected quality option with a visual marker

### Requirement 4

**User Story:** As a dashcam user, I want the app to handle cases where my device doesn't support certain quality levels, so that I only see quality options that will work on my device.

#### Acceptance Criteria

1. WHEN the Camera_Service initializes, THE Camera_Service SHALL query the device's supported quality levels
2. WHEN the quality selection dialog is displayed, THE Settings_Screen SHALL only show quality options that the device supports
3. IF the user's saved Video_Quality preference is not supported by the device, THEN THE Camera_Service SHALL fall back to the highest supported quality
4. WHEN a fallback quality is used, THE Preference_Manager SHALL update the stored preference to the fallback quality value
