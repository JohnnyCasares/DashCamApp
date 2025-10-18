package com.kasahirotech.dashcamapp.models

import android.net.Uri
import com.kasahirotech.dashcamapp.interfaces.VideoItem

class AppVideo(
    override val uri: Uri,
    override val name: String,
    override val duration: Int
) : VideoItem