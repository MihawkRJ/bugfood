package com.bugfood

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class BugFoodApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Restaura preferência de tema
        val prefs = getSharedPreferences("bugfood_prefs", MODE_PRIVATE)
        val theme = prefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(theme)
    }
}
