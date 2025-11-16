# Implementation Plan

- [x] 1. Create PreferenceManager utility for audio preference persistence


  - Implement PreferenceManager object in `service/` package
  - Add methods for reading and writing audio recording preference
  - Include error handling with fallback to default value (enabled)
  - Use SharedPreferences with key "audio_recording_enabled" in "dashcam_preferences" file
  - _Requirements: 1.2, 1.3, 1.4_

- [x] 2. Create microphone icon resource


  - Add vector drawable `ic_mic.xml` in `res/drawable/`
  - Use Material Design microphone icon or create simple microphone vector
  - _Requirements: 3.3_

- [x] 3. Create toggle setting layout


  - Create `item_setting_toggle.xml` layout file in `res/layout/`
  - Include ImageView for icon, TextView for title, and SwitchCompat for toggle
  - Position switch on the right side of the layout
  - Match styling with existing `item_setting.xml`
  - _Requirements: 3.1, 3.2, 3.3_

- [x] 4. Implement AudioToggleSetting class


  - Create AudioToggleSetting class in `settings/` package implementing SettingItem interface
  - Set icon to microphone drawable and title to "Record Audio"
  - Store reference to current toggle state
  - Implement toggle() method that updates preference via PreferenceManager
  - _Requirements: 1.1, 1.2, 1.3, 3.1, 3.2, 3.3_

- [x] 5. Update SettingsAdapter to support multiple view types


  - Modify SettingsAdapter to distinguish between click-based and toggle-based settings
  - Override getItemViewType() to return different types for different SettingItem implementations
  - Create ToggleSettingsViewHolder class for toggle settings
  - Implement toggle state binding and change listener in ToggleSettingsViewHolder
  - Update onCreateViewHolder() and onBindViewHolder() to handle both view types
  - _Requirements: 1.1, 1.3, 3.1, 3.2_

- [x] 6. Update Settings activity to include AudioToggleSetting


  - Remove placeholder settings (SettingOneTest, SettingTwoTest) from Settings.kt
  - Instantiate AudioToggleSetting and add to settings list
  - _Requirements: 1.1_

- [x] 7. Modify Camera service to respect audio preference



  - Import PreferenceManager in Camera.kt
  - Update captureVideo() method to check both RECORD_AUDIO permission AND audio preference
  - Apply withAudioEnabled() only when both permission granted and preference enabled
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 4.1, 4.2_

- [ ]* 8. Add string resources for accessibility
  - Add content description strings for toggle switch
  - Add accessibility labels for audio setting
  - _Requirements: 3.3_
