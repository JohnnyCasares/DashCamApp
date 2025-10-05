package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.os.Environment
import java.io.File

class Storage(
    private var context: Context
) {

    fun getPrivateRecordingsDirectory(): File? {
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



}