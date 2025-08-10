package com.kasahirotech.dashcamapp.settings

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.kasahirotech.dashcamapp.interfaces.SettingItem

class SettingsTest {
}




class SettingOneTest: SettingItem{
    override val icon: String = ""

    override val title: String = "Setting 1"

    override val id: Int = 1
    override fun onItemClick(
        context: Context,
        fragmentManager: FragmentManager?
    ) {
        Log.d("SettingOneTest", "Setting One Clicked! Context: $context")
        Toast.makeText(context, "$title clicked!", Toast.LENGTH_SHORT).show()
    }


}
class SettingTwoTest: SettingItem{
    override val icon: String = ""

    override val title: String = "Setting 2"

    override val id: Int = 2
    override fun onItemClick(
        context: Context,
        fragmentManager: FragmentManager?
    ) {
        Log.d("SettingTwoTest", "Setting Two Clicked! Context: $context")
        Toast.makeText(context, "$title clicked!", Toast.LENGTH_SHORT).show()
    }


}