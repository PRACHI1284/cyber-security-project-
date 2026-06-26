package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_logs")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "SCAN", "THREAT", "BIOMETRIC", "ENCRYPTION", "SYSTEM"
    val title: String,
    val description: String,
    val severity: String // "INFO", "WARNING", "HIGH"
)

data class ScannedFile(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val fileType: String, // "APK", "BIN", "SH", "PDF", "JPG"
    val isMalicious: Boolean,
    val detectedThreatName: String = "",
    val flagReason: String = "",
    val riskScore: Int = 0 // 0-100
)
