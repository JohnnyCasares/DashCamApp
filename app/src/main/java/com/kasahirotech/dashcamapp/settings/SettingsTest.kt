package com.kasahirotech.dashcamapp.settings

import com.kasahirotech.dashcamapp.interfaces.SettingItem

class SettingsTest {
}




class SettingOneTest: SettingItem{
    override val icon: String = ""

    override val title: String = "Setting 1"

    override val id: Int = 1


}
class SettingTwoTest: SettingItem{
    override val icon: String = ""

    override val title: String = "Setting 2"

    override val id: Int = 2


}