package com.example.core.algorithms

import android.Manifest
import kotlin.math.max
import kotlin.math.min

/**
 * Heuristic Risk Scoring Engine.
 * Evaluates the risk profile of an application based on its requested permissions.
 */
object RiskScorer {

    private val PERMISSION_WEIGHTS = mapOf(
        Manifest.permission.BIND_ACCESSIBILITY_SERVICE to 40,
        Manifest.permission.SYSTEM_ALERT_WINDOW to 35,
        Manifest.permission.REQUEST_INSTALL_PACKAGES to 30,
        Manifest.permission.RECEIVE_BOOT_COMPLETED to 10,
        Manifest.permission.READ_SMS to 25,
        Manifest.permission.SEND_SMS to 25,
        Manifest.permission.READ_CONTACTS to 15,
        Manifest.permission.RECORD_AUDIO to 20,
        Manifest.permission.CAMERA to 20,
        Manifest.permission.ACCESS_FINE_LOCATION to 15,
        Manifest.permission.READ_EXTERNAL_STORAGE to 10,
        Manifest.permission.WRITE_EXTERNAL_STORAGE to 10
    )

    // Dangerous combinations that exponentially increase the risk score
    private val SYNERGY_PENALTIES = listOf(
        // Accessibility + Overlay (Classic Banking Trojan combo)
        Pair(listOf(Manifest.permission.BIND_ACCESSIBILITY_SERVICE, Manifest.permission.SYSTEM_ALERT_WINDOW), 40),
        
        // SMS + Internet + Boot (Classic SMS Spyware)
        Pair(listOf(Manifest.permission.READ_SMS, Manifest.permission.INTERNET, Manifest.permission.RECEIVE_BOOT_COMPLETED), 30),
        
        // Install + Boot (Persistence Dropper)
        Pair(listOf(Manifest.permission.REQUEST_INSTALL_PACKAGES, Manifest.permission.RECEIVE_BOOT_COMPLETED), 25)
    )

    /**
     * Analyzes a list of permissions and returns a normalized risk score between 0 and 100.
     */
    fun calculateRiskScore(requestedPermissions: List<String>): Int {
        if (requestedPermissions.isEmpty()) return 0
        
        var baseScore = 0
        
        // Calculate base additive risk
        for (permission in requestedPermissions) {
            baseScore += PERMISSION_WEIGHTS[permission] ?: 0
        }

        // Apply synergistic penalties for dangerous combinations
        var penaltyScore = 0
        for (synergy in SYNERGY_PENALTIES) {
            val (combo, penalty) = synergy
            if (requestedPermissions.containsAll(combo)) {
                penaltyScore += penalty
            }
        }

        val totalRisk = baseScore + penaltyScore
        
        // Normalize to 1-100
        return min(max(totalRisk, 0), 100)
    }
}
