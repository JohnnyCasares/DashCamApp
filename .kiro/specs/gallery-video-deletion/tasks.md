# Implementation Plan

- [x] 1. Create VideoDeletionService interface and implementation


  - Define VideoDeletionService interface with deleteVideos() and hasDeletePermission() methods
  - Create DeletionResult data class for operation results
  - Implement VideoDeletion service class with MediaStore deletion logic
  - Handle Android 10+ scoped storage with createDeleteRequest()
  - Add coroutine support for async deletion operations
  - _Requirements: 1.5, 4.2_

- [ ]* 1.1 Write unit tests for VideoDeletionService
  - Test successful single video deletion
  - Test successful multiple video deletion
  - Test permission checking logic
  - Test partial failure scenarios
  - _Requirements: 1.5, 4.2_



- [ ] 2. Add selection state management to GalleryAdapter
  - Add selectedVideos Set to track selected items
  - Add selectionMode boolean flag
  - Implement toggleSelection() method
  - Implement enterSelectionMode() and exitSelectionMode() methods
  - Implement getSelectedVideos() method
  - Add onSelectionChanged callback for count updates


  - _Requirements: 2.1, 3.1, 3.2_

- [ ] 2.1 Add visual selection indicators to video thumbnails
  - Update item_video_thumbnail.xml with selection overlay and checkmark

  - Modify ViewHolder to show/hide selection indicator based on state
  - Add Material Design elevation changes for selected items
  - _Requirements: 2.2, 3.3, 3.4_

- [ ] 2.2 Implement long-press detection in adapter
  - Add OnLongClickListener to video items
  - Trigger onItemLongClicked callback
  - Enter selection mode and select the long-pressed video
  - _Requirements: 2.1_

- [ ]* 2.3 Write property test for selection visual consistency
  - **Property 3: Selection visual consistency**


  - **Validates: Requirements 2.2, 3.3, 3.4**

- [ ]* 2.4 Write property test for selection toggle correctness
  - **Property 5: Selection toggle correctness**
  - **Validates: Requirements 3.1, 3.2**

- [x] 3. Implement ActionMode in Gallery activity

  - Create ActionModeCallback with menu inflation
  - Add delete action to ActionMode menu
  - Display selection count in ActionMode title
  - Handle ActionMode lifecycle (create, destroy)
  - Update ActionMode title when selection changes
  - _Requirements: 2.3, 3.5_

- [ ] 3.1 Wire up ActionMode to adapter selection events
  - Start ActionMode on long-press callback
  - Update ActionMode title on selection changes
  - Exit ActionMode when back button pressed
  - Clear selections when ActionMode destroyed
  - _Requirements: 2.4, 2.5_


- [ ]* 3.2 Write property test for mode exit clears selections
  - **Property 4: Mode exit clears all selections**
  - **Validates: Requirements 2.4, 2.5**

- [ ]* 3.3 Write property test for action bar count accuracy
  - **Property 6: Action bar count accuracy**
  - **Validates: Requirements 2.3, 3.5**

- [ ] 4. Implement click handling for both modes
  - Modify adapter click listener to check selectionMode flag
  - In Normal Mode: trigger play video action
  - In Selection Mode: toggle video selection
  - Update visual indicators on selection change


  - _Requirements: 1.1, 3.1, 3.2_

- [ ]* 4.1 Write property test for normal mode play behavior
  - **Property 1: Normal mode preserves play behavior**
  - **Validates: Requirements 1.1**


- [ ]* 4.2 Write property test for long-press enters selection mode
  - **Property 2: Long-press enters selection mode**
  - **Validates: Requirements 2.1**

- [ ] 5. Create delete confirmation dialog
  - Build Material AlertDialog with title and message
  - Display count of videos to be deleted in message
  - Add "Cancel" and "Delete" action buttons
  - Show dialog when delete action clicked in ActionMode
  - _Requirements: 1.4, 4.1_

- [ ] 5.1 Implement deletion flow with confirmation
  - Check if videos are selected when delete icon clicked
  - Show toast if no videos selected

  - Show confirmation dialog for non-empty selection
  - On confirm: call VideoDeletionService to delete videos
  - On cancel: dismiss dialog and maintain selection
  - _Requirements: 1.3, 4.3_

- [ ]* 5.2 Write property test for deletion confirmation
  - **Property 7: Deletion confirmation for non-empty selection**
  - **Validates: Requirements 1.4, 4.1**



- [ ]* 5.3 Write property test for cancellation preserves selection
  - **Property 9: Cancellation preserves selection**
  - **Validates: Requirements 4.3**

- [ ] 6. Implement gallery refresh after deletion
  - Call GalleryService to get updated video list after deletion
  - Update adapter with new video list
  - Exit Selection Mode after successful deletion
  - Show empty state if all videos deleted
  - Display error message if deletion fails


  - _Requirements: 4.4, 4.5, 5.1, 5.2_

- [ ] 6.1 Add DiffUtil for smooth list updates
  - Create DiffUtil.Callback for AppVideo comparison
  - Use DiffUtil to calculate list changes
  - Apply changes with animations for deleted items
  - _Requirements: 5.5_

- [ ]* 6.2 Write property test for confirmed deletion removes videos
  - **Property 8: Confirmed deletion removes videos**
  - **Validates: Requirements 1.5, 4.2, 5.1**

- [x]* 6.3 Write property test for successful deletion exits selection mode



  - **Property 10: Successful deletion exits selection mode**
  - **Validates: Requirements 4.4**

- [ ] 7. Add delete icon to normal mode action bar
  - Create menu resource with trash icon
  - Inflate menu in Gallery activity
  - Handle menu item click for delete action
  - Show appropriate message when clicked with no selection
  - _Requirements: 1.2, 1.3_

- [x] 8. Add required dependencies to build.gradle.kts


  - Add kotlinx-coroutines-android for async operations
  - Add kotest-property-jvm for property-based testing
  - Add kotest-runner-junit5-jvm for test execution
  - Add mockito-kotlin for mocking in tests
  - Add kotlinx-coroutines-test for coroutine testing
  - _Requirements: All_

- [ ] 9. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
