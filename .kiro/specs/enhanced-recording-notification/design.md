# Design Document

## Overview

This design enhances the DashCamApp recording notification UI from a basic foreground service notification to a polished, MediaStyle notification similar to Spotify and other media players. The current notification is functional and reliable - this enhancement focuses primarily on improving the visual presentation and user experience while preserving all existing functionality.

The enhancement focuses on two key areas:
1. Upgrading the notification UI to use MediaStyle for a more polished, modern appearance
2. Improving visual feedback with better icons, layout, and status indicators

**Important:** The core recording functionality already works correctly. This is a UI/UX enhancement, not a functional rewrite.

## Architecture

### Current Architecture
```
MainActivity → RecordingService → NotificationHelper
                     ↓
              BackgroundCameraManager
```

### Enhanced Architecture (UI-Focused)
```
MainActivity → RecordingService ← Action Intents (from notification)
                     ↓
              BackgroundCameraManager (unchanged)
                     ↓
              NotificationHelper (MediaStyle builder - ENHANCED)
```

**Key Changes:**
- NotificationHelper upgraded to build MediaStyle notifications with improved visual presentation
- Action intents already target RecordingService correctly (no changes needed)
- BackgroundCameraManager remains unchanged (functionality already works)
- Focus is on notification UI/UX improvements, not functional changes

## Components and Interfaces

### 1. NotificationHelper (Enhanced)

**Responsibilities:**
- Create notification channel with appropriate settings
- Build MediaStyle notifications with action buttons
- Provide notification updates with current state

**New Methods:**
```kotlin
fun buildMediaStyleNotification(
    context: Context,
    elapsedTime: String,
    isPaused: Boolean,
    isLowStorage: Boolean
): Notification
```

**MediaStyle Configuration:**
- Uses `androidx.media.app.NotificationCompat.MediaStyle()`
- Shows up to 3 actions in compact view
- Includes large icon for better visibility
- Configured as ongoing and non-dismissible

### 2. RecordingService (Minimal Changes)

**Changes:**
- Update notification building to use new MediaStyle notification
- No changes to recording logic or state management

**Note:** Existing ACTION_START_RECORDING and ACTION_STOP_RECORDING already work correctly and remain unchanged.

### 3. BackgroundCameraManager (No Changes)

No changes required - existing recording functionality works correctly.

### 4. RecordingCallback (No Changes)

No changes required - existing callback interface is sufficient.

## Data Models

### NotificationState (Optional)

For future refactoring, state could be encapsulated:
```kotlin
data class NotificationState(
    val elapsedTime: String,
    val isLowStorage: Boolean
)
```

However, for this UI-focused enhancement, we can continue passing parameters directly to the notification builder.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Acceptence Criteria Testing Prework:

1.1 WHEN the recording starts THEN the system SHALL create a pinned notification that cannot be dismissed by swiping
Thoughts: This is about the notification configuration. We can test that when we create a notification, it has the ongoing flag set to true, which makes it non-dismissible.
Testable: yes - property

1.2 WHEN the user attempts to dismiss the notification THEN the system SHALL prevent dismissal and maintain the notification visibility
Thoughts: This is the same as 1.1 - it's testing that the ongoing flag prevents dismissal. This is redundant with 1.1.
Testable: redundant with 1.1

1.3 WHEN the recording is active THEN the system SHALL display the notification with IMPORTANCE_HIGH priority
Thoughts: This is about notification channel configuration. We can test that the channel is created with IMPORTANCE_HIGH.
Testable: yes - property

1.4 WHEN the recording stops THEN the system SHALL remove the notification from the notification shade
Thoughts: This is testing that when stopRecording is called, the notification is removed. We can test this by verifying stopForeground is called.
Testable: yes - property

2.1 WHEN the notification is displayed THEN the system SHALL use MediaStyle notification format with expanded controls
Thoughts: This is testing that the notification builder uses MediaStyle. We can verify the notification has the MediaStyle set.
Testable: yes - property

2.2 WHEN the notification is collapsed THEN the system SHALL show at least one primary action button
Thoughts: This is about MediaStyle configuration. We can test that setShowActionsInCompactView is called with at least one action index.
Testable: yes - property

2.3 WHEN the notification is expanded THEN the system SHALL display all available action buttons
Thoughts: This is testing that all actions are added to the notification. We can count the actions in the built notification.
Testable: yes - property

2.4 WHEN displaying the notification THEN the system SHALL include the app icon and recording status prominently
Thoughts: This is testing that the notification has a small icon and content text set. We can verify these fields are non-null.
Testable: yes - property

3.1 WHEN the notification is displayed THEN the system SHALL provide a Stop Recording action button with the same icon used in MainActivity
Thoughts: This is testing that the notification includes the Stop action with the ic_stop_recording drawable. We can verify the action exists with the correct icon.
Testable: yes - example

3.2 WHEN the user taps the Stop button THEN the system SHALL execute the stop recording action immediately
Thoughts: This is about the Android system delivering the PendingIntent, which is outside our control. We can test that PendingIntents are created correctly.
Testable: yes - property

3.3 WHEN the Stop action is triggered THEN the system SHALL send the action intent to RecordingService for processing
Thoughts: This is testing that PendingIntents target RecordingService. We can verify the intent component is correct.
Testable: yes - property

3.4 WHEN displaying the Stop button THEN the system SHALL use the ic_stop_recording drawable (red square in circle) matching MainActivity
Thoughts: This is testing that the Stop button uses the correct icon resource. We can verify the icon resource ID matches ic_stop_recording.
Testable: yes - property

4.1 WHEN recording is active THEN the system SHALL display elapsed recording time updated every second
Thoughts: This is testing the timer mechanism. We can verify that the notification update is called periodically with increasing time values.
Testable: yes - property

4.2 WHEN recording is active THEN the system SHALL display current recording status text
Thoughts: This is testing that the notification content includes status text. We can verify the content text is set.
Testable: yes - property

4.3 WHEN storage space is low THEN the system SHALL display a warning indicator in the notification
Thoughts: This is testing that when isLowStorage is true, the notification includes a warning. We can test the notification builder with isLowStorage=true.
Testable: yes - property

4.5 WHEN the notification updates THEN the system SHALL perform silent updates without sound or vibration
Thoughts: This is testing notification channel configuration. We can verify the channel has sound and vibration disabled, and setOnlyAlertOnce is true.
Testable: yes - property

5.1 WHEN a Stop action is received THEN the RecordingService SHALL stop recording and release camera resources
Thoughts: This is testing the service's response to ACTION_STOP_RECORDING. This already works, so we just need to verify it continues working.
Testable: yes - property



5.3 WHEN processing actions THEN the RecordingService SHALL validate the action and handle null intents gracefully
Thoughts: This is testing error handling. We can test that when a null intent is passed to onStartCommand, the service doesn't crash.
Testable: yes - property

6.1 WHEN the notification is enhanced THEN the system SHALL maintain all existing recording functionality
Thoughts: This is testing that the UI changes don't break existing functionality. We can verify that recording still works after the notification changes.
Testable: yes - property

6.2 WHEN the notification UI is updated THEN the system SHALL preserve the current foreground service behavior
Thoughts: This is testing that the service lifecycle remains unchanged. We can verify START_STICKY is still returned and the service behaves the same.
Testable: yes - property

6.3 WHEN the MediaStyle notification is displayed THEN the system SHALL maintain the same reliability as the current notification
Thoughts: This is a general reliability requirement. We can test that the notification is displayed successfully and actions work.
Testable: yes - property

7.1 WHEN creating notifications THEN the NotificationHelper SHALL handle all notification building logic
Thoughts: This is an architectural requirement about code organization, not a functional requirement.
Testable: no

7.2 WHEN processing actions THEN the RecordingService SHALL handle all business logic for recording operations
Thoughts: This is an architectural requirement about code organization, not a functional requirement.
Testable: no

7.3 WHEN action intents are created THEN the system SHALL target RecordingService directly, not NotificationHelper
Thoughts: This is testing that PendingIntents have the correct target component. We can verify the intent's component class.
Testable: yes - property

7.4 WHEN the notification is updated THEN the system SHALL use a clear interface between NotificationHelper and RecordingService
Thoughts: This is an architectural requirement about code organization, not a functional requirement.
Testable: no

### Property Reflection:

After reviewing all testable properties, I've identified the following redundancies and consolidations:

**Redundant Properties:**
- 1.2 is redundant with 1.1 (both test ongoing flag)
- 3.1 and 3.4 both test the Stop button icon - can be consolidated

**Consolidated Properties:**
- Notification configuration properties (1.1, 1.3, 2.1, 2.2, 4.5) can be grouped as they all test notification builder configuration
- Action intent properties (3.3, 7.3) can be consolidated as they all test PendingIntent configuration
- UI enhancement properties focus on visual improvements without breaking existing functionality

After consolidation, we have the following unique properties focused on UI enhancement:

Property 1: Notification is non-dismissible (1.1)
Property 2: Notification uses MediaStyle with correct configuration (2.1, 2.2, 2.3)
Property 3: Stop action is present with correct icon (3.1, 3.4)
Property 4: Action intents target RecordingService correctly (3.3, 7.3)
Property 5: Notification updates reflect current state (4.1, 4.2, 4.3)
Property 6: Existing functionality is preserved (5.1, 5.3, 6.1, 6.2, 6.3)

### Correctness Properties:

Property 1: Notification non-dismissibility
*For any* notification created by buildMediaStyleNotification, the notification should have the ongoing flag set to true
**Validates: Requirements 1.1, 1.2**

Property 2: MediaStyle configuration completeness
*For any* notification created by buildMediaStyleNotification, the notification should use MediaStyle, show at least one action in compact view, and include all provided actions
**Validates: Requirements 2.1, 2.2, 2.3**

Property 3: Stop action presence with correct icon
*For any* notification created for active recording, the notification should include a Stop action with the ic_stop_recording drawable resource
**Validates: Requirements 3.1, 3.4**

Property 4: Action intent routing
*For any* action PendingIntent created in the notification, the intent should target RecordingService class and include the correct action string
**Validates: Requirements 3.2, 3.3, 7.3**

Property 5: State reflection in notification
*For any* notification state (elapsed time, recording status, low storage), the notification content should accurately reflect that state in the displayed text
**Validates: Requirements 4.1, 4.2, 4.3**

Property 6: Existing functionality preservation
*For any* recording operation (start, stop), when triggered from the enhanced notification, the operation should execute correctly without errors
**Validates: Requirements 5.1, 5.2, 6.1, 6.2, 6.3**

Property 7: Notification removal on stop
*For any* recording session, when stopRecording is called, the notification should be removed from the notification shade
**Validates: Requirements 1.4**

Property 8: Silent notification updates
*For any* notification update after the initial display, the update should not trigger sound or vibration
**Validates: Requirements 4.5**

Property 9: Notification channel priority
*For any* notification channel created for recording, the channel should have IMPORTANCE_HIGH priority
**Validates: Requirements 1.3**

## Error Handling

### Invalid Action Intents
- Null intent in onStartCommand: Log warning and return START_STICKY
- Unknown action string: Log warning and ignore
- Missing extras: Use default values

### Camera Operation Failures
- Pause fails: Show toast, continue recording
- Resume fails: Show toast, attempt restart
- Stop fails: Force cleanup and remove notification

### Storage Issues
- Low storage during recording: Update notification with warning, continue recording
- No storage: Stop recording immediately, show toast

### Service Lifecycle Issues
- Service killed during recording: Android restarts with START_STICKY, reinitialize state
- Task removed: Save recording and gracefully stop
- Destroy called: Clean up all resources, release camera

## Testing Strategy

### Unit Testing Framework
- **JUnit 4** for unit tests
- **Mockito** for mocking Android components
- **Robolectric** for Android framework testing without emulator

### Property-Based Testing Framework
- **Kotest Property Testing** for Kotlin
- Minimum 100 iterations per property test
- Each property test tagged with: **Feature: enhanced-recording-notification, Property {number}: {property_text}**

### Unit Tests

**NotificationHelper Tests:**
- Test notification channel creation with correct settings
- Test MediaStyle notification building with various states
- Test action button creation and configuration
- Test notification content formatting

**RecordingService Tests:**
- Test onStartCommand with each action type
- Test null intent handling
- Test state management (pause/resume)
- Test timer updates
- Test storage monitoring

**BackgroundCameraManager Tests:**
- No new tests required (existing functionality unchanged)

### Property-Based Tests

Each correctness property will be implemented as a property-based test:

**Property 1 Test:** Generate random notification states, verify ongoing flag is always true

**Property 2 Test:** Generate random notification configurations, verify MediaStyle is set and actions are configured correctly

**Property 3 Test:** Generate random recording states, verify Stop action is present with ic_stop_recording icon

**Property 4 Test:** Generate random action types, verify PendingIntents target RecordingService with correct action strings

**Property 5 Test:** Generate random state combinations (time, status, low storage), verify notification text reflects all states

**Property 6 Test:** Generate random recording operations (start, stop), verify existing functionality continues to work correctly

**Property 7 Test:** Generate random recording sessions, verify notification is removed when recording stops

**Property 8 Test:** Generate random notification update sequences, verify setOnlyAlertOnce is true for all updates

**Property 9 Test:** Generate random channel configurations, verify importance is always HIGH

### Integration Tests

- Test full recording flow with enhanced notification
- Test stop from notification
- Test notification appearance and behavior
- Test notification persistence across app backgrounding

### Manual Testing

- Test notification appearance on different Android versions
- Test notification behavior when swiping to dismiss
- Test notification in expanded and collapsed states
- Test notification actions on different devices
- Test notification during low storage conditions

## Implementation Notes

### MediaStyle Dependencies
Requires `androidx.media:media:1.6.0` or later for MediaStyle support.

### Android Version Compatibility
- MediaStyle available on API 21+
- Notification channels required on API 26+
- App targets API 29+ so all features are supported

### Performance Considerations
- Notification updates every second should be lightweight
- Use Handler for timer to avoid creating new threads
- Reuse notification builder when possible
- Minimize bitmap operations in notification

### Accessibility
- All action buttons should have content descriptions
- Notification text should be clear and concise
- Use semantic colors for status indicators

## Future Enhancements

- Add recording quality indicator to notification
- Add storage space remaining indicator
- Add thumbnail preview in notification (requires API 31+)
- Add quick settings tile for recording control
- Add notification customization in settings
