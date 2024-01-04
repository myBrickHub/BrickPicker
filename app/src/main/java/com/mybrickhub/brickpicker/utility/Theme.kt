package com.mybrickhub.brickpicker.utility

import androidx.appcompat.app.AppCompatDelegate
import com.mybrickhub.brickpicker.MyApplication

class Theme {
    fun setAppTheme(theme: String) {
        when (theme) {
            "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        MyApplication.sharedPreferences.edit().putString(Settings.KEY_THEME, theme).apply()
    }
}
