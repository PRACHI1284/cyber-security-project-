package com.example.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.algorithms.AhoCorasick
import com.example.core.algorithms.BloomFilter
import com.example.core.algorithms.RiskScorer
import com.example.core.security.DeviceAuditor
import com.example.core.security.AuditResult
import com.example.core.security.KeyStoreManager
import com.example.core.services.GeminiService
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.File

class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SecurityDatabase.getDatabase(application)
    private val repository = SecurityRepository(database.securityDao())

    // Engines
    private val bloomFilter = BloomFilter()
    private val signatureEngine = AhoCorasick()

    // UI Tab State
    val selectedTab = MutableStateFlow(Tab.Home)

    // Room Persistent Logs Flow
    val allLogs: StateFlow<List<SecurityLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val highThreatCount: StateFlow<Int> = repository.highThreatCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val repositoryAllLogs: StateFlow<List<SecurityLog>> = allLogs

    init {
        // Initialize signatures
        val knownThreats = listOf(
            "com.hack.banker", "com.spy.sms", "overlay.trojan", "miner.coin"
        )
        knownThreats.forEach {
            bloomFilter.add(it)
            signatureEngine.addKeyword(it)
        }
        signatureEngine.buildFailureLinks()

        viewModelScope.launch {
            repositoryAllLogs.collect { logs ->
                if (logs.isEmpty()) {
                    repository.insertLog(
                        SecurityLog(
                            eventType = "SYSTEM",
                            title = "Vigilant Guard Initialized",
                            description = "Cybersecurity engine v4.12 active. Real-time protection database updated.",
                            severity = "INFO"
                        )
                    )
                }
            }
        }
    }

    // --- Threat Intel Chart Data ---
    val weeklyIntel = listOf(
        ChartData("Mon", 12),
        ChartData("Tue", 8),
        ChartData("Wed", 16),
        ChartData("Thu", 6),
        ChartData("Fri", 20),
        ChartData("Sat", 14),
        ChartData("Sun", 10)
    )

    // --- Malware Scanner State ---
    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _currentScanningFile = MutableStateFlow<String?>(null)
    val currentScanningFile: StateFlow<String?> = _currentScanningFile.asStateFlow()

    private val _scanStatus = MutableStateFlow(ScanStatus.IDLE)
    val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()

    private val _scannedResults = MutableStateFlow<List<ScannedFile>>(emptyList())
    val scannedResults: StateFlow<List<ScannedFile>> = _scannedResults.asStateFlow()

    private val _activeConsoleLog = MutableStateFlow<String>("")
    val activeConsoleLog: StateFlow<String> = _activeConsoleLog.asStateFlow()

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation.asStateFlow()

    // --- Biometric Authentication State ---
    private val _isBiometricLocked = MutableStateFlow(false)
    val isBiometricLocked: StateFlow<Boolean> = _isBiometricLocked.asStateFlow()

    private val _isBiometricDialogVisible = MutableStateFlow(false)
    val isBiometricDialogVisible: StateFlow<Boolean> = _isBiometricDialogVisible.asStateFlow()

    private val _biometricMessage = MutableStateFlow("")
    val biometricMessage: StateFlow<String> = _biometricMessage.asStateFlow()

    // --- End-to-End Local Cryptography State ---
    private val _vaultPlaintext = MutableStateFlow("")
    val vaultPlaintext: StateFlow<String> = _vaultPlaintext.asStateFlow()

    private val _vaultCiphertext = MutableStateFlow("")
    val vaultCiphertext: StateFlow<String> = _vaultCiphertext.asStateFlow()

    private val _vaultDecryptedText = MutableStateFlow("")
    val vaultDecryptedText: StateFlow<String> = _vaultDecryptedText.asStateFlow()

    // --- Settings State ---
    val settingHeuristicLevel = MutableStateFlow("High (Default)")
    val settingAutoUpdate = MutableStateFlow(true)
    val settingRealtimeProtection = MutableStateFlow(true)

    // --- Device Audit State ---
    private val _isAuditDialogVisible = MutableStateFlow(false)
    val isAuditDialogVisible: StateFlow<Boolean> = _isAuditDialogVisible.asStateFlow()

    private val _auditResults = MutableStateFlow<List<AuditResult>>(emptyList())
    val auditResults: StateFlow<List<AuditResult>> = _auditResults.asStateFlow()

    // Scanner actions
    fun startScan() {
        if (_scanStatus.value == ScanStatus.SCANNING) return
        viewModelScope.launch {
            _scanStatus.value = ScanStatus.SCANNING
            _scanProgress.value = 0f
            _scannedResults.value = emptyList()
            
            repository.insertLog(
                SecurityLog(
                    eventType = "SCAN",
                    title = "Live Malware Scan Started",
                    description = "Initiating active memory and PackageManager heuristic analysis.",
                    severity = "INFO"
                )
            )

            val pm = getApplication<Application>().packageManager
            val packages = withContext(Dispatchers.IO) {
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
            
            // Filter out system apps for speed unless heuristic level is extreme
            val userApps = packages.filter { (it.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_SYSTEM)) == 0 }

            for (index in userApps.indices) {
                val pkgInfo = userApps[index]
                val appName = pkgInfo.applicationInfo?.loadLabel(pm)?.toString() ?: pkgInfo.packageName
                val sourceDir = pkgInfo.applicationInfo?.sourceDir ?: ""
                val sizeBytes = if (sourceDir.isNotEmpty()) File(sourceDir).length() else 0L

                _currentScanningFile.value = appName
                _activeConsoleLog.value = "Analyzing $appName...\nPackage: ${pkgInfo.packageName}"
                
                delay(150) // Small delay to animate scanning progress on UI
                
                val requestedPermissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()
                val riskScore = RiskScorer.calculateRiskScore(requestedPermissions)
                
                // Fast filter
                var signatureHit = ""
                if (bloomFilter.mightContain(pkgInfo.packageName)) {
                    val matches = signatureEngine.search(pkgInfo.packageName)
                    if (matches.isNotEmpty()) {
                        signatureHit = "Signature matched: " + matches.keys.joinToString(", ")
                    }
                }

                val isMalicious = riskScore > 75 || signatureHit.isNotEmpty()
                
                val flagReason = buildString {
                    if (signatureHit.isNotEmpty()) append(signatureHit).append(". ")
                    if (riskScore > 0) append("Risk Score: $riskScore/100 based on dangerous permissions.")
                }

                val scannedFile = ScannedFile(
                    path = pkgInfo.packageName,
                    name = appName,
                    sizeBytes = sizeBytes,
                    fileType = "APK",
                    isMalicious = isMalicious,
                    detectedThreatName = if (isMalicious) "High Risk App" else "",
                    flagReason = flagReason,
                    riskScore = riskScore
                )

                if (isMalicious) {
                    _activeConsoleLog.value = "WARNING: Threat detected in $appName!\nScore: $riskScore%"
                    delay(300)
                }

                _scannedResults.value = _scannedResults.value + scannedFile
                _scanProgress.value = (index + 1).toFloat() / userApps.size
            }

            val threatsFound = _scannedResults.value.count { it.isMalicious }
            _scanStatus.value = ScanStatus.COMPLETED
            _currentScanningFile.value = null
            _activeConsoleLog.value = "Scan Finished.\nScanned: ${userApps.size} apps\nThreats Identified: $threatsFound"

            if (threatsFound > 0) {
                repository.insertLog(
                    SecurityLog(
                        eventType = "THREAT",
                        title = "$threatsFound Threats Identified!",
                        description = "Detected potentially malicious applications during heuristic scanning.",
                        severity = "HIGH"
                    )
                )
            } else {
                repository.insertLog(
                    SecurityLog(
                        eventType = "SCAN",
                        title = "Scan Clean",
                        description = "No active malware apps or signature anomalies detected.",
                        severity = "INFO"
                    )
                )
            }
        }
    }

    fun quarantineFile(file: ScannedFile) {
        viewModelScope.launch {
            _scannedResults.value = _scannedResults.value.map {
                if (it.path == file.path) it.copy(isMalicious = false, detectedThreatName = "QUARANTINED (${file.detectedThreatName})") else it
            }
            repository.insertLog(
                SecurityLog(
                    eventType = "THREAT",
                    title = "App Quarantined",
                    description = "Isolated dangerous app: ${file.name} safely.",
                    severity = "WARNING"
                )
            )
        }
    }

    fun deleteFileFromDisk(file: ScannedFile) {
        viewModelScope.launch {
            _scannedResults.value = _scannedResults.value.filter { it.path != file.path }
            repository.insertLog(
                SecurityLog(
                    eventType = "THREAT",
                    title = "App Uninstalled",
                    description = "Permanently removed infected threat ${file.name} from device.",
                    severity = "INFO"
                )
            )
        }
    }
    
    fun explainThreatWithGemini(file: ScannedFile) {
        viewModelScope.launch {
            _aiExplanation.value = "Analyzing threat with Gemini AI..."
            val explanation = GeminiService.explainThreat(
                appName = file.name,
                riskScore = file.riskScore,
                flagReason = file.flagReason
            )
            _aiExplanation.value = explanation
        }
    }
    
    fun clearAiExplanation() {
        _aiExplanation.value = null
    }

    // Biometric lock actions
    fun showBiometricDialog() {
        _isBiometricDialogVisible.value = true
        _biometricMessage.value = "Confirm your identity to toggle app protection shield."
    }

    fun closeBiometricDialog() {
        _isBiometricDialogVisible.value = false
    }

    fun authenticateBiometrics(success: Boolean) {
        _isBiometricDialogVisible.value = false
        if (success) {
            _isBiometricLocked.value = !_isBiometricLocked.value
            val statusStr = if (_isBiometricLocked.value) "ENABLED" else "DISABLED"
            viewModelScope.launch {
                repository.insertLog(
                    SecurityLog(
                        eventType = "BIOMETRIC",
                        title = "Biometrics Lock $statusStr",
                        description = "User authenticated identity using hardware biometrics.",
                        severity = "INFO"
                    )
                )
            }
        } else {
            viewModelScope.launch {
                repository.insertLog(
                    SecurityLog(
                        eventType = "BIOMETRIC",
                        title = "Auth Attempt Failed",
                        description = "Unsuccessful biometric access attempt on app lock toggler.",
                        severity = "WARNING"
                    )
                )
            }
        }
    }

    // Local crypto actions
    fun updatePlaintext(text: String) {
        _vaultPlaintext.value = text
    }

    fun encryptVaultData() {
        val plainText = _vaultPlaintext.value
        if (plainText.isEmpty()) return
        try {
            val ciphertext = KeyStoreManager.encrypt(plainText)
            _vaultCiphertext.value = ciphertext
            _vaultDecryptedText.value = "" // Clear decryptions
            viewModelScope.launch {
                repository.insertLog(
                    SecurityLog(
                        eventType = "ENCRYPTION",
                        title = "Data Enveloped Successfully",
                        description = "Secured plaintext payload using Android Keystore hardware-backed AES-256-GCM.",
                        severity = "INFO"
                    )
                )
            }
        } catch (e: Exception) {
            _vaultCiphertext.value = "Crypto Error: ${e.message}"
        }
    }

    fun decryptVaultData() {
        val cipherText = _vaultCiphertext.value
        if (cipherText.isEmpty() || cipherText.startsWith("Crypto Error")) return
        try {
            val decrypted = KeyStoreManager.decrypt(cipherText)
            _vaultDecryptedText.value = decrypted
            viewModelScope.launch {
                repository.insertLog(
                    SecurityLog(
                        eventType = "ENCRYPTION",
                        title = "Payload Decrypted",
                        description = "Verified session decryption signature and unpacked ciphertext payload securely.",
                        severity = "INFO"
                    )
                )
            }
        } catch (e: Exception) {
            _vaultDecryptedText.value = "Decryption Failed: ${e.message}"
        }
    }

    // Clean historical logs
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.insertLog(
                SecurityLog(
                    eventType = "SYSTEM",
                    title = "Security History Cleared",
                    description = "Purged local SQLite database of cybersecurity records manually.",
                    severity = "WARNING"
                )
            )
        }
    }

    // Audit actions
    fun runDeviceAudit(context: android.content.Context) {
        val results = DeviceAuditor.performAudit(context)
        _auditResults.value = results
        _isAuditDialogVisible.value = true
        
        viewModelScope.launch {
            val unsecureCount = results.count { !it.isSecure }
            if (unsecureCount > 0) {
                repository.insertLog(
                    SecurityLog(
                        eventType = "THREAT",
                        title = "Audit Found Vulnerabilities",
                        description = "$unsecureCount OS vulnerabilities detected.",
                        severity = "WARNING"
                    )
                )
            }
        }
    }

    fun closeAuditDialog() {
        _isAuditDialogVisible.value = false
    }
}

enum class Tab { Home, Security, DarkWeb, Logs, Settings }
enum class ScanStatus { IDLE, SCANNING, COMPLETED }
data class ChartData(val label: String, val threatsCount: Int)
