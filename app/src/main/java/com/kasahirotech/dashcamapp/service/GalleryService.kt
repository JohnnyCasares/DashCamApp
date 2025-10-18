package com.kasahirotech.dashcamapp.service

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.kasahirotech.dashcamapp.models.AppVideo


class GalleryService {

    fun getMyAppVideos(context: Context): List<AppVideo> {
        val videoList = mutableListOf<AppVideo>()

        // The columns we want to retrieve
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )

        // The folder path you used when saving. Must be exact.
        val relativePath = "Movies/DashCam/"

        // The 'selection' is the WHERE clause of our query.
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(relativePath)

        // The order to sort the results
        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

        val query = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)


            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)

                // Create the content URI for the specific video
                val contentUri: Uri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                videoList.add(AppVideo(contentUri, name, duration))
            }
        }
        return videoList
    }


}