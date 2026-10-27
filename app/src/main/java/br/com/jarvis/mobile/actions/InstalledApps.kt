package br.com.jarvis.mobile.actions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import br.com.jarvis.mobile.domain.LaunchableApp

class InstalledApps(private val context: Context) {
    fun list(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val manager = context.packageManager
        val resolved = if (Build.VERSION.SDK_INT >= 33) manager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            else @Suppress("DEPRECATION") manager.queryIntentActivities(intent, 0)
        return resolved.mapNotNull { result ->
            val info = result.activityInfo ?: return@mapNotNull null
            if (!info.exported || !info.enabled || info.packageName == context.packageName) return@mapNotNull null
            LaunchableApp(result.loadLabel(manager).toString(), info.packageName,
                ComponentName(info.packageName, info.name).flattenToString())
        }.distinctBy { it.component }
    }
}
