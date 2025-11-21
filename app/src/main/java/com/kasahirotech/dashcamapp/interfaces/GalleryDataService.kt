package com.kasahirotech.dashcamapp.interfaces

import android.content.Context
import com.kasahirotech.dashcamapp.models.AppVideo

/**
 * Interface for managing gallery data operations.
 * Provides abstraction for video retrieval operations, enabling
 * testability and flexibility in data source implementation.
 */
interface GalleryDataService {
    
    /**
     * Retrieves all videos recorded by this app from the device's media store.
     * 
     * @param context Application or Activity context
     * @return List of AppVideo objects representing the recorded videos
     */
    fun getMyAppVideos(context: Context): List<AppVideo>
}
