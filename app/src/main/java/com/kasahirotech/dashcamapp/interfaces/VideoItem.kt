package com.kasahirotech.dashcamapp.interfaces

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentManager

/**
 * This interface is a contract that every setting item option must follow
 * @property uri string that represents the video resource
 * @property name text that represents current name of the video file
 * @property duration integer that represents video duration length
 */

interface VideoItem {
    val uri: Uri
    val name: String
    val duration: Int

}