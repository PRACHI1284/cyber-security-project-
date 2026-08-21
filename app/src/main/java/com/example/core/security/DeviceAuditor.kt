package com.example.core.security

import android.content.Context
import android.provider.Settings
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

data class AuditResult(
    val title: String,
    val isSecure: Boolean,
    val description: String
)

object DeviceAuditor {
    
    fun performAudit(context: Context): List<AuditResult> {
        val results = mutableListOf<AuditResult>()
        
        // Check Root Access
        results.add(checkRootAccess())
        
        // Check Developer Options
        results.add(checkDeveloperOptions(context))
        
        // Check USB Debugging
        results.add(checkAdbEnabled(context))
        
        // Check Unknown Sources (For older devices, on newer ones it's per-app but we check global setting)
        results.add(checkUnknownSources(context))

        return results
    }

    private fun checkRootAccess(): AuditResult {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        
        var isRooted = false
        for (path in paths) {
            if (File(path).exists()) {
                isRooted = true
                break
            }
        }
        
        // Check test-keys
        val buildTags = android.os.Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            isRooted = true
        }

        return AuditResult(
            title = "Root Access",
            isSecure = !isRooted,
            description = if (isRooted) "Device is rooted, which bypasses Android's security sandbox." else "Device is not rooted. OS sandbox is intact."
        )
    }

    private fun checkDeveloperOptions(context: Context): AuditResult {
        val devOptionsEnabled = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) != 0
        
        return AuditResult(
            title = "Developer Options",
            isSecure = !devOptionsEnabled,
            description = if (devOptionsEnabled) "Developer options are enabled. This can expose advanced settings to malware." else "Developer options are disabled."
        )
    }

    private fun checkAdbEnabled(context: Context): AuditResult {
        val adbEnabled = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED, 0
        ) != 0

        return AuditResult(
            title = "USB Debugging",
            isSecure = !adbEnabled,
            description = if (adbEnabled) "USB Debugging is enabled. Attackers with physical access can compromise the device." else "USB Debugging is disabled."
        )
    }
    
    private fun checkUnknownSources(context: Context): AuditResult {
        val unknownSources = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS
            ) != 0
        } catch (e: Settings.SettingNotFoundException) {
            false
        }

        return AuditResult(
            title = "Unknown Sources",
            isSecure = !unknownSources,
            description = if (unknownSources) "Installation from unknown sources is allowed globally." else "Global unknown sources installation is restricted."
        )
    }
}
