# Design Document

## Overview

This design implements video deletion functionality for the DashCamApp gallery, following Android's Material Design guidelines and common gallery app patterns. The solution adds selection mode capabilities to the existing RecyclerView-based gallery, enabling both single and bulk video deletion through intuitive touch interactions.

## Architecture

### Component Structure

The implementation extends the existing gallery architecture with minimal changes:

```
Gallery Activity (existing)
├── GalleryAdapter (modified)
│   ├── Selection state management
│   ├── Long-press detection
│   └── Visual selection indicators
├── ActionMode (new)
│   ├── Selection count display
│   └── Delete action
└── DeletionService (new interface + implementation)
    ├── MediaStore deletion
    └── File system cleanup
```

### State Management

The gallery will operate in two distinct modes:

1. **Normal Mode**: Default state where tapping videos plays them
2. **Selection Mode**: Activated by long-press, allows multi-selection and deletion

State transitions:
- Normal → Selection: Long-press on any video
- Selection → Normal: Back button, cancel action, or successful deletion

## Components and Interfaces

### 1. VideoDeletionService Interface

```kotlin
interface VideoDeletionService {
    /**
     * Deletes videos from MediaStore and file system
     * @param context Android context for ContentResolver access
     * @param videos List of videos to delete
     * @return Result indicating success or failure with error details
     */
    suspend fun deleteVideos(context: Context, videos: List<AppVideo>): DeletionResult
    
    /**
     * Checks if the app has permission to delete media files
     * @param context Android context
     * @return true if permission is granted
     */
    fun hasDeletePermission(context: Context): Boolean
}

data class DeletionResult(
    val success: Boolean,
    val deletedCount: Int,
    val failedVideos: List<AppVideo> = emptyList(),
    val errorMessage: String? = null
)
```

### 2. Modified GalleryAdapter

The adapter will be enhanced to support selection state:

```kotlin
class GalleryAdapter(
    private var videos: List<AppVideo>,
    private val onItemClicked: (AppVideo) -> Unit,
    private val onItemLongClicked: (AppVideo) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.VideoGalleryViewHolder>() {
    
    private val selectedVideos = mutableSetOf<AppVideo>()
    var selectionMode = false
        private set
    
    fun toggleSelection(video: AppVideo)
    fun clearSelection()
    fun getSelectedVideos(): List<AppVideo>
    fun enterSelectionMode(video: AppVideo)
    fun exitSelectionMode()
}
```

### 3. Enhanced Gallery Activity

The activity will manage ActionMode and coordinate deletion:

```kotlin
class Gallery : AppCompatActivity() {
    private var actionMode: ActionMode? = null
    private val deletionService: VideoDeletionService = VideoDeletion()
    
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean
        override fun onDestroyActionMode(mode: ActionMode)
    }
    
    private fun showDeleteConfirmationDialog(count: Int)
    private suspend fun deleteSelectedVideos()
    private fun refreshGallery()
}
```

## Data Models

No changes to existing `AppVideo` model required. The model already contains all necessary information:
- `uri`: Used for MediaStore deletion
- `name`: Used in confirmation dialogs
- `duration`: Existing display property

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Normal mode preserves play behavior
*For any* video in the gallery when in Normal Mode, tapping that video should trigger the play action and not enter selection mode
**Validates: Requirements 1.1**

### Property 2: Long-press enters selection mode
*For any* video in Normal Mode, long-pressing that video should enter Selection Mode and mark that video as selected
**Validates: Requirements 2.1**

### Property 3: Selection visual consistency
*For any* video, when it is marked as selected in the internal state, it should display a visual indicator, and when deselected, the indicator should be removed
**Validates: Requirements 2.2, 3.3, 3.4**

### Property 4: Mode exit clears all selections
*For any* selection state in Selection Mode, exiting to Normal Mode should clear all selections and remove all visual indicators
**Validates: Requirements 2.4, 2.5**

### Property 5: Selection toggle correctness
*For any* video in Selection Mode, tapping an unselected video should add it to selection, and tapping a selected video should remove it from selection
**Validates: Requirements 3.1, 3.2**

### Property 6: Action bar count accuracy
*For any* sequence of selection operations, the action bar should always display the correct count of currently selected videos
**Validates: Requirements 2.3, 3.5**

### Property 7: Deletion confirmation for non-empty selection
*For any* non-empty set of selected videos, initiating deletion should display a confirmation dialog with the correct count
**Validates: Requirements 1.4, 4.1**

### Property 8: Confirmed deletion removes videos
*For any* set of selected videos, after confirming deletion, those videos should be removed from device storage and not appear in the refreshed gallery
**Validates: Requirements 1.5, 4.2, 5.1**

### Property 9: Cancellation preserves selection
*For any* selection state, canceling the deletion dialog should maintain the current selection and remain in Selection Mode
**Validates: Requirements 4.3**

### Property 10: Successful deletion exits selection mode
*For any* successful deletion operation, the system should exit Selection Mode and refresh the gallery display
**Validates: Requirements 4.4**

## Error Handling

### Permission Errors
- **Scenario**: App lacks permission to delete media files (Android 10+)
- **Handling**: Request permission via MediaStore's createDeleteRequest() or show error message
- **User Feedback**: Toast message explaining permission requirement

### Deletion Failures
- **Scenario**: Individual video deletion fails (file locked, corrupted, etc.)
- **Handling**: Continue deleting other videos, track failures
- **User Feedback**: Show count of successfully deleted vs failed videos

### MediaStore Sync Issues
- **Scenario**: Video deleted from file system but MediaStore not updated
- **Handling**: Force MediaStore rescan after deletion
- **User Feedback**: Transparent to user, handled automatically

### Empty Selection
- **Scenario**: User taps delete with no videos selected
- **Handling**: Show brief toast message
- **User Feedback**: "No videos selected"

## Testing Strategy

### Unit Testing Framework
- **Framework**: JUnit 4.13.2 (already in project)
- **Mocking**: MockK or Mockito for Android dependencies
- **Coroutines**: kotlinx-coroutines-test for suspend function testing

### Unit Tests
1. **Selection State Tests**
   - Test selection/deselection of individual videos
   - Test selection mode entry/exit
   - Test selection clearing

2. **Deletion Service Tests**
   - Test successful deletion of single video
   - Test successful deletion of multiple videos
   - Test partial failure scenarios
   - Test permission checking

3. **Adapter Tests**
   - Test selection count updates
   - Test visual indicator state
   - Test mode transitions

### Property-Based Testing Framework
- **Framework**: Kotest Property Testing (kotest-property-jvm)
- **Configuration**: Minimum 100 iterations per property test
- **Integration**: Add to app/build.gradle.kts dependencies

### Property-Based Tests
Each property test must be tagged with: `**Feature: gallery-video-deletion, Property {number}: {property_text}**`

1. **Property 1: Normal mode preserves play behavior**
   - Generate random videos
   - Verify tapping in Normal Mode triggers play, not selection

2. **Property 2: Long-press enters selection mode**
   - Generate random videos
   - Verify long-press enters Selection Mode and selects the video

3. **Property 3: Selection visual consistency**
   - Generate random selection/deselection operations
   - Verify visual indicators match internal selection state

4. **Property 4: Mode exit clears all selections**
   - Generate random selection states
   - Verify exiting Selection Mode clears all selections

5. **Property 5: Selection toggle correctness**
   - Generate random videos and tap sequences
   - Verify tapping toggles selection state correctly

6. **Property 6: Action bar count accuracy**
   - Generate random selection operations
   - Verify action bar count always matches selection size

7. **Property 7: Deletion confirmation for non-empty selection**
   - Generate random non-empty selections
   - Verify confirmation dialog shows correct count

8. **Property 8: Confirmed deletion removes videos**
   - Generate random video sets and selections
   - Verify deleted videos don't appear after deletion

9. **Property 9: Cancellation preserves selection**
   - Generate random selection states
   - Verify canceling deletion maintains selection

10. **Property 10: Successful deletion exits selection mode**
    - Generate random successful deletions
    - Verify system exits Selection Mode after deletion

### Integration Testing
- Test full deletion flow from selection to gallery refresh
- Test ActionMode lifecycle with selection changes
- Test confirmation dialog interaction

## Implementation Notes

### Android API Considerations

**Android 10+ (API 29+)**:
- Use MediaStore.createDeleteRequest() for scoped storage compliance
- Handle IntentSender for user confirmation
- No WRITE_EXTERNAL_STORAGE permission needed

**Deletion Flow**:
1. Build list of URIs to delete
2. Call ContentResolver.delete() or createDeleteRequest()
3. Handle system permission dialog (API 29+)
4. Remove from local list on success
5. Refresh RecyclerView

### Visual Design

**Selection Indicator**:
- Semi-transparent overlay on selected thumbnails
- Checkmark icon in corner
- Material Design elevation change

**ActionMode**:
- Replace action bar with contextual action bar
- Show selection count as title
- Delete icon as primary action
- Close/cancel icon to exit

**Confirmation Dialog**:
- Material AlertDialog
- Title: "Delete videos?"
- Message: "Delete X video(s)? This cannot be undone."
- Actions: "Cancel" and "Delete"

### Performance Considerations

- Deletion operations run on background thread (coroutines)
- RecyclerView updates use DiffUtil for efficient animations
- Thumbnail loading unchanged (existing implementation)
- Selection state stored in Set for O(1) lookup

## Dependencies

### New Dependencies Required

```kotlin
// Coroutines for async deletion
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Property-based testing
testImplementation("io.kotest:kotest-property-jvm:5.8.0")
testImplementation("io.kotest:kotest-runner-junit5-jvm:5.8.0")

// Unit testing utilities
testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
```

### Existing Dependencies Used
- AndroidX AppCompat (ActionMode)
- Material Components (AlertDialog, icons)
- RecyclerView (existing gallery implementation)
