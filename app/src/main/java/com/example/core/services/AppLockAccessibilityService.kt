package com.example.core.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.ui.AppLockOverlayActivity

class AppLockAccessibilityService : AccessibilityService() {
    
    private val TAG = "AppLockService"
    private var lastPackage = ""
    
    // In a real app, this list comes from user preferences (SharedPreferences/Room)
    private val protectedApps = listOf(
        "com.whatsapp",
        "com.instagram.android",
        "com.google.android.apps.photos"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Avoid repeatedly launching overlay for the same app
            if (packageName != lastPackage && packageName != this.packageName) {
                lastPackage = packageName
                Log.d(TAG, "Window state changed, new package: $packageName")
                
                if (protectedApps.contains(packageName)) {
                    Log.d(TAG, "Protected app launched! Showing lock screen.")
                    
                    val intent = Intent(this, AppLockOverlayActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("locked_package", packageName)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
}
