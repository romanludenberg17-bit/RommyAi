package com.rommy.ai.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/** Resolves and launches apps that are actually installed for the current user. */
class AppLauncher(context: Context) {
    private val packageManager = context.packageManager
    private val appContext = context.applicationContext

    fun find(query: String): List<InstalledApp> {
        if (query.isBlank()) return emptyList()
        val normalized = query.trim().lowercase()
        return installedApps()
            .filter { app ->
                app.label.lowercase().contains(normalized) ||
                    app.packageName.lowercase().contains(normalized)
            }
            .sortedWith(compareBy({ !it.label.equals(query.trim(), ignoreCase = true) }, { it.label }))
    }

    fun launch(query: String): Boolean {
        val app = find(query).firstOrNull() ?: return false
        return launchPackage(app.packageName)
    }

    fun launchPackage(packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
        return true
    }

    fun installedApps(): List<InstalledApp> {
        val apps = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(PackageManager.MATCH_ALL)
        }

        return apps.asSequence()
            .mapNotNull { info ->
                val launchIntent = packageManager.getLaunchIntentForPackage(info.packageName)
                if (launchIntent == null) null
                else InstalledApp(
                    packageName = info.packageName,
                    label = info.loadLabel(packageManager).toString(),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}

data class InstalledApp(
    val packageName: String,
    val label: String,
)
