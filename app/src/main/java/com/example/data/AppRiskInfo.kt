package com.example.data

/**
 * Data model representing the security analysis of an installed application.
 */
data class AppRiskInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val requestedPermissions: List<String>,
    val riskScore: Int, // 0 to 100
    val matchedSignatures: List<String> = emptyList(), // Output from Aho-Corasick
    val isMalicious: Boolean = riskScore > 75 || matchedSignatures.isNotEmpty()
)
