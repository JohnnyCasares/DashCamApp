package com.kasahirotech.dashcamapp.interfaces

import android.content.Context
import androidx.fragment.app.FragmentManager

/**
 * This interface is a contract that every setting item option must follow
 * @property icon image that is displayed as the icon for the setting item
 * @property title text that represents a descriptive title of the setting item
 */

interface SettingItem {
    val icon: Int
    val title: String

    /**
     *[onItemClick] should be used to handle the logic when a settings item is clicked.
     *The fragmentManager parameter is used to handle dialog windows or navigation to other screens
     */
    fun onItemClick(context: Context, fragmentManager: FragmentManager?)

    /*TODO: Implement onLongItemClick to show a descriptive caption of the item*/
}