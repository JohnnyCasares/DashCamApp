# Design Document

## Overview

This feature adds an audio recording toggle to the DashCam application's settings screen. The toggle allows users to control whether audio is recorded during video capture sessions. The implementation will extend the existing settings infrastructure with a new toggle-style setting item, integrate with Android's SharedPreferences for persistence, and modify the Camera service to respect the user's audio preference.

## Architecture

### Component Interaction Flow

```
User Interaction → Settings Screen → AudioToggleSetting
                                            ↓
                                    SharedPreferences
                                            ↓
Recording Action → Camera Service → Read Preference → Apply to Recording
```

### Key Components

1. **AudioToggleSetting**: New SettingItem implementation with toggle functionality
2. **PreferenceManager**: Utility class for managing audio preference persistence
3. **Camera Service**: Modified to check and apply audio preference
4. **Settings Screen**: Updated to include the new audio toggle setting

## Components and Interfaces

### 1. PreferenceManager

A utility class to centralize SharedPreferences operations for audio recording preference.

**Responsibilities:**
- Read audio recording preference state
- Write audio recording preference state
- Provide default value (enabled) for first launch

**Interface:**
```kotlin
object PreferenceManager {
    fun isAudioRecordingEnabled(context: Context): Boolean
    fun setAudioRecordingEnabled(context: Context, enabled: Boolean)
    
    private const val PREFS_NAME = "dashcam_preferences"
    private const val KEY_AUDIO_ENABLED = "audio_recording_enabled"
    private const val DEFAULT_AUDIO_ENABLED = true
}
```

### 2. AudioToggleSetting

A new SettingItem implementation that displays a toggle switch for audio recording control.

**Responsibilities:**
- Display current audio recording state
- Handle toggle state changes
- Persist preference changes immediately
- Provide visual feedback

**Implementation Approach:**
- Extends the SettingItem interface
- Uses a custom layout with a Switch/SwitchCompat widget
- Updates SharedPreferences on toggle change
- No navigation or dialog required (inline toggle)

**Key Differences from Existing Settings:**
- Current settings use click handlers that show toasts
- Audio toggle needs a switch widget in the layout
- State must be persisted and reflected in UI

### 3. Modified Camera Service

The existing Camera class will be updated to check the audio preference before enabling audio.

**Changes Required:**
- Import PreferenceManager
- Check audio preference in `captureVideo()` method
- Apply audio only if both permission granted AND preference enabled

**Current Logic:**
```kotlin
.apply {
    if (PermissionChecker.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) ==
        PermissionChecker.PERMISSION_GRANTED) {
        withAudioEnabled()
    }
}
```

**New Logic:**
```kotlin
.apply {
    if (PermissionChecker.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) ==
        PermissionChecker.PERMISSION_GRANTED &&
        PreferenceManager.isAudioRecordingEnabled(activity)) {
        withAudioEnabled()
    }
}
```

### 4. Settings Screen Updates

The Settings activity will be updated to include the AudioToggleSetting in the settings list.

**Changes Required:**
- Remove placeholder settings (SettingOneTest, SettingTwoTest)
- Add AudioToggleSetting to the settings list
- No other structural changes needed

## Data Models

### SharedPreferences Schema

```
Preference File: "dashcam_preferences"
├── "audio_recording_enabled": Boolean (default: true)
```

**Rationale:**
- Simple boolean flag sufficient for toggle state
- Default to true maintains current behavior for existing users
- Single preference file can accommodate future settings

### Setting Item Extension

The AudioToggleSetting will need to handle state differently than click-based settings:

```kotlin
class AudioToggleSetting(private val context: Context) : SettingItem {
    override val icon: Int = R.drawable.ic_mic  // New icon resource needed
    override val title: String = "Record Audio"
    
    var isEnabled: Boolean = PreferenceManager.isAudioRecordingEnabled(context)
        private set
    
    fun toggle() {
        isEnabled = !isEnabled
        PreferenceManager.setAudioRecordingEnabled(context, isEnabled)
    }
    
    override fun onItemClick(context: Context, fragmentManager: FragmentManager?) {
        // Toggle handled by switch widget, not click
    }
}
```

## UI Design

### Layout Modifications

A new layout file `item_setting_toggle.xml` will be created for toggle-style settings:

```xml
<androidx.constraintlayout.widget.ConstraintLayout>
    <ImageView id="ivIcon" />
    <TextView id="tvTitle" />
    <androidx.appcompat.widget.SwitchCompat 
        id="switchToggle"
        layout_constraintEnd_toEndOf="parent"
        layout_constraintTop_toTopOf="parent"
        layout_constraintBottom_toBottomOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

### Adapter Modifications

The SettingsAdapter will need to support multiple view types:

1. **Click-based settings** (existing): Use `item_setting.xml`
2. **Toggle-based settings** (new): Use `item_setting_toggle.xml`

**Implementation Strategy:**
- Override `getItemViewType()` to distinguish setting types
- Create separate ViewHolder classes for each type
- Bind toggle state and change listener in toggle ViewHolder

### Visual Feedback

- Toggle switch in "on" position: Material Design default enabled state
- Toggle switch in "off" position: Material Design default disabled state
- Icon: Microphone icon to represent audio recording
- Title: "Record Audio" for clarity

## Error Handling

### Edge Cases

1. **SharedPreferences unavailable**: 
   - Fallback to default value (true)
   - Log error but don't crash
   - Continue with audio enabled

2. **Permission revoked during recording**:
   - Existing CameraX error handling applies
   - No additional handling needed

3. **Preference corruption**:
   - Use try-catch when reading preferences
   - Default to enabled state on exception

4. **Rapid toggle changes**:
   - Each toggle immediately persists
   - No debouncing needed (SharedPreferences is fast)
   - Next recording session will use latest value

### Error Handling Strategy

```kotlin
object PreferenceManager {
    fun isAudioRecordingEnabled(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_AUDIO_ENABLED, DEFAULT_AUDIO_ENABLED)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading audio preference", e)
            DEFAULT_AUDIO_ENABLED
        }
    }
}
```

## Testing Strategy

### Unit Testing

1. **PreferenceManager Tests**:
   - Test default value on first read
   - Test write and read cycle
   - Test error handling with corrupted preferences

2. **AudioToggleSetting Tests**:
   - Test initial state reflects saved preference
   - Test toggle updates preference
   - Test icon and title properties

### Integration Testing

1. **Settings Screen Tests**:
   - Verify AudioToggleSetting appears in list
   - Verify toggle interaction updates preference
   - Verify state persists across activity recreation

2. **Camera Service Tests**:
   - Verify audio enabled when permission granted AND preference enabled
   - Verify audio disabled when preference disabled (regardless of permission)
   - Verify audio disabled when permission denied (regardless of preference)

### Manual Testing Scenarios

1. Enable toggle → Record video → Verify audio present
2. Disable toggle → Record video → Verify no audio
3. Toggle setting → Close app → Reopen → Verify state persisted
4. Revoke microphone permission → Verify toggle still functional
5. Grant microphone permission with toggle disabled → Verify no audio recorded

## Implementation Notes

### Resource Requirements

- New drawable resource: `ic_mic.xml` (microphone icon)
- New layout file: `item_setting_toggle.xml`
- New string resources for accessibility (content descriptions)

### Backward Compatibility

- First-time users: Audio enabled by default (current behavior)
- Existing users: Audio enabled by default (maintains current behavior)
- No migration needed

### Performance Considerations

- SharedPreferences read on each recording start: Negligible performance impact
- SharedPreferences write on toggle change: Asynchronous, no UI blocking
- No impact on recording performance

### Future Extensibility

This design establishes patterns for future toggle-based settings:
- Video quality selection
- Storage location preference
- Auto-start recording on app launch
- Loop recording duration

The PreferenceManager can be extended to handle additional preferences, and the adapter's multi-view-type support enables easy addition of new setting types.
