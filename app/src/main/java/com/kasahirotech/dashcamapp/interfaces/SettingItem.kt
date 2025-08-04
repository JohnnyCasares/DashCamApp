package com.kasahirotech.dashcamapp.interfaces

import android.content.Context
import androidx.fragment.app.FragmentManager

interface SettingItem {
    val icon: String
   val title: String // Resource ID for the menu item title
    val id: Int // ID in the xml or unique identifier

//  fun openScreen(context: Context, fragmentManager: FragmentManager?)
}