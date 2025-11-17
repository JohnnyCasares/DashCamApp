# Implementation Plan

- [x] 1. Extend PreferenceManager with video quality storage methods


  - Add constants KEY_VIDEO_QUALITY and DEFAULT_VIDEO_QUALITY to PreferenceManager
  - Implement getVideoQuality() method that retrieves stored quality string and converts to Quality enum
  - Implement setVideoQuality() method that stores Quality enum as string
  - Add error handling with try-catch and fallback to HIGHEST for invalid values
  - _Requirements: 2.1, 2.3, 4.4_

- [x] 2. Create VideoQualitySetting class implementing SettingItem interface


  - Create new file VideoQualitySetting.kt in settings/items package
  - Implement SettingItem interface with icon and title properties
  - Add currentQuality property that reads from PreferenceManager
  - Implement getQualityDisplayName() method to map Quality enum to user-friendly strings (UHD→"UHD (4K)", FHD→"FHD (1080p)", etc.)
  - Implement getCurrentQualityText() method to return formatted current quality string
  - _Requirements: 1.1, 1.5, 3.1_

- [x] 3. Implement quality selection dialog in VideoQualitySetting


  - Implement onItemClick() method to show AlertDialog with quality options
  - Create array of all six Quality enum values in descending order (UHD, FHD, HD, SD, HIGHEST, LOWEST)
  - Map Quality values to display strings using getQualityDisplayName()
  - Configure dialog with single-choice list and radio buttons
  - Set currently selected quality as pre-selected item in dialog
  - Handle positive button click to save selected quality via PreferenceManager
  - Handle negative button click to dismiss without changes
  - _Requirements: 1.2, 1.3, 1.4, 3.2, 3.3_

- [x] 4. Create video quality icon drawable resource


  - Create ic_video_quality.xml vector drawable in res/drawable
  - Design icon representing video quality (e.g., HD symbol, video camera with quality indicator)
  - Ensure icon follows Material Design guidelines and matches existing icon style
  - _Requirements: 1.1_

- [x] 5. Integrate VideoQualitySetting into Settings screen


  - Instantiate VideoQualitySetting in Settings.kt onCreate() method
  - Add VideoQualitySetting instance to settingsList
  - Position in list after existing settings (audio toggle, dual camera toggle)
  - _Requirements: 1.1_

- [x] 6. Update SettingsAdapter to handle VideoQualitySetting


  - Add VideoQualitySetting case to getItemViewType() when expression
  - Return VIEW_TYPE_CLICK for VideoQualitySetting instances
  - Verify SettingsViewHolder properly displays subtitle text for current quality
  - _Requirements: 1.1, 1.5_

- [x] 7. Modify Camera service to use selected video quality


  - Update startCamera() method in Camera.kt to retrieve quality from PreferenceManager
  - Replace hardcoded Quality.HIGHEST with PreferenceManager.getVideoQuality(activity)
  - Apply retrieved quality to QualitySelector.from() when building Recorder
  - _Requirements: 2.1, 2.2, 2.3, 2.4_


- [x] 8. Update DualCameraManager to use selected video quality







  - Modify DualCameraManager to retrieve quality from PreferenceManager
  - Apply selected quality to both front and back camera recorders
  - Ensure quality consistency between single and dual camera modes
  - _Requirements: 2.1, 2.2_

- [ ]* 9. Write unit tests for video quality functionality
  - Create VideoQualitySettingTest.kt with tests for quality display name mapping
  - Test PreferenceManager quality storage and retrieval
  - Test invalid quality handling and fallback to HIGHEST
  - Test default quality value on first launch
  - _Requirements: 2.3, 4.3, 4.4_

- [ ]* 10. Write integration tests for settings UI and camera integration
  - Create VideoQualityIntegrationTest.kt to test settings screen display
  - Test dialog opening and quality selection flow
  - Test preference persistence across app restarts
  - Test camera service applies selected quality to recordings
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2_
