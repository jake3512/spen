package com.jake.spenhotkey.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.jake.spenhotkey.service.SPenAccessibilityService

object AccessibilityUtils {

    fun isServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${SPenAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (enabledServices.isNullOrEmpty() || TextUtils.isEmpty(enabledServices)) return false
        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
