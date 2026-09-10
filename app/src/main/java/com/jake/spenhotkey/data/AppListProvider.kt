package com.jake.spenhotkey.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object AppListProvider {

    fun loadLaunchableApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        return resolveInfos
            .map { info ->
                InstalledApp(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString()
                )
            }
            .distinctBy { it.packageName }
            .filterNot { it.packageName == context.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
