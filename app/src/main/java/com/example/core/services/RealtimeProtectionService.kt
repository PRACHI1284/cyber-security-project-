package com.example.core.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.core.algorithms.AhoCorasick
import com.example.core.algorithms.BloomFilter
import com.example.core.algorithms.RiskScorer
import com.example.data.SecurityDatabase
import com.example.data.SecurityLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RealtimeProtectionService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: SecurityDatabase
    private val bloomFilter = BloomFilter()
    private val signatureEngine = AhoCorasick()

    override fun onCreate() {
        super.onCreate()
        database = SecurityDatabase.getDatabase(this)
        
        // Initialize signatures
        val knownThreats = listOf(
            "com.hack.banker", "com.spy.sms", "overlay.trojan", "miner.coin"
        )
        knownThreats.forEach {
            bloomFilter.add(it)
            signatureEngine.addKeyword(it)
        }
        signatureEngine.buildFailureLinks()

        createNotificationChannel()
        startForeground(1, createNotification())
        
        // Register BroadcastReceiver for package installs
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            scanPackage(packageName)
        }
    }

    private fun scanPackage(packageName: String) {
        scope.launch {
            try {
                val pm = packageManager
                val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                val requestedPermissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()
                
                val riskScore = RiskScorer.calculateRiskScore(requestedPermissions)
                
                var signatureHit = ""
                if (bloomFilter.mightContain(packageName)) {
                    val matches = signatureEngine.search(packageName)
                    if (matches.isNotEmpty()) {
                        signatureHit = "Signature matched: " + matches.keys.joinToString(", ")
                    }
                }

                val isMalicious = riskScore > 75 || signatureHit.isNotEmpty()
                
                if (isMalicious) {
                    val appName = pkgInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
                    val log = SecurityLog(
                        eventType = "THREAT",
                        title = "Real-Time Threat Detected!",
                        description = "Intercepted malicious installation: \$appName. Risk: \$riskScore%",
                        severity = "HIGH"
                    )
                    database.securityDao().insertLog(log)
                    
                    showThreatNotification(appName)
                }
            } catch (e: Exception) {
                Log.e("RealtimeProtection", "Error scanning package: \$packageName", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "realtime_protection",
                "Real-Time Protection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            
            val threatChannel = NotificationChannel(
                "threat_alerts",
                "Threat Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(threatChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "realtime_protection")
            .setContentTitle("VigilantGuard is Active")
            .setContentText("Real-time background protection is monitoring apps.")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun showThreatNotification(appName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "threat_alerts")
            .setContentTitle("Malicious App Blocked")
            .setContentText("VigilantGuard intercepted \$appName during installation.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
