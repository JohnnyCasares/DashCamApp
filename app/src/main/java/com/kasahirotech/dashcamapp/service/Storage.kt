package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.os.Environment
import com.kasahirotech.dashcamapp.interfaces.StorageService
import java.io.File

class Storage(
    private var context: Context
) : StorageService {

    override fun getPrivateRecordingsDirectory(): File? {
        // 1. Get the base private external directory: /.../files/Movies
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)

        if (moviesDir == null) return null

        // 2. Append the specific subdirectory "DashCam"
        val mediaDir = File(moviesDir, "DashCam")

        if (!mediaDir.exists()) {
            mediaDir.mkdirs() // Create the directory if it doesn't exist
        }
        return mediaDir
    }
    
    override fun getTripLogsDirectory(): File? {
        // Get the base private external directory: /.../files/Documents
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        
        if (documentsDir == null) return null
        
        // Create the specific subdirectory "TripLogs"
        val logsDir = File(documentsDir, "TripLogs")
        
        if (!logsDir.exists()) {
            logsDir.mkdirs() // Create the directory if it doesn't exist
        }
        return logsDir
    }
}