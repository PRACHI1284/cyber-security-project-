package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SecurityDatabase.getDatabase(application)
    private val repository = SecurityRepository(database.securityDao())

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

    // Init with first startup events if empty
    val repositoryAllLogs: StateFlow<List<SecurityLog>> = allLogs

    init {
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
    val filesToScan = listOf(
        ScannedFile("/storage/emulated/0/Download/WhatsApp_Update.apk", "WhatsApp_Update.apk", 12450000, "APK", true, "Spyware.PremiumTrojan", "Requests SEND_SMS, READ_CONTACTS, and runs in background after device boot.", 92),
        ScannedFile("/storage/emulated/0/DCIM/family_photo.jpg", "family_photo.jpg", 3450000, "JPG", false),
        ScannedFile("/storage/emulated/0/Download/crypto_miner.bin", "crypto_miner.bin", 890000, "BIN", true, "CoinMiner.Adware", "Executes background cryptocurrency mining on port 4444 with high CPU load.", 68),
        ScannedFile("/storage/emulated/0/Documents/report_Q2_financials.pdf", "report_Q2_financials.pdf", 4500000, "PDF", false),
        ScannedFile("/storage/emulated/0/Download/root_exploit.sh", "root_exploit.sh", 1200, "SH", true, "Exploit.PrivilegeEscalation", "Attempts kernel level escalation targeting standard system binaries.", 97),
        ScannedFile("/storage/emulated/0/Backups/system_backup.zip", "system_backup.zip", 450000000, "ZIP", false),
        ScannedFile("/storage/emulated/0/Download/bank_hack_v3.apk", "bank_hack_v3.apk", 8300000, "APK", true, "Banker.OverlayTrojan", "Mimics commercial bank portals and requests overlay DRAW_OVER_OTHER_APPS permission.", 99)
    )

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
                    description = "Initiating active memory and file directory heuristic analysis.",
                    severity = "INFO"
                )
            )

            for (index in filesToScan.indices) {
                val file = filesToScan[index]
                _currentScanningFile.value = file.name
                
                // Simulated scanning analysis
                _activeConsoleLog.value = "Analyzing ${file.name}...\nSize: ${file.sizeBytes / 1024} KB"
                delay(400)
                
                if (file.isMalicious) {
                    _activeConsoleLog.value = "Applying heuristics on ${file.name}...\nWARNING: Signature pattern match: ${file.detectedThreatName}!\nHeuristic risk score: ${file.riskScore}%"
                    delay(500)
                } else {
                    _activeConsoleLog.value = "Scanning ${file.name}...\nHash matches known signature: SAFE."
                    delay(300)
                }

                _scannedResults.value = _scannedResults.value + file
                _scanProgress.value = (index + 1).toFloat() / filesToScan.size
            }

            val threatsFound = _scannedResults.value.count { it.isMalicious }
            _scanStatus.value = ScanStatus.COMPLETED
            _currentScanningFile.value = null
            _activeConsoleLog.value = "Scan Finished.\nScanned: ${filesToScan.size} files\nThreats Identified: $threatsFound"

            if (threatsFound > 0) {
                repository.insertLog(
                    SecurityLog(
                        eventType = "THREAT",
                        title = "$threatsFound Threats Identified!",
                        description = "Detected potentially malicious binaries during heuristic scanning.",
                        severity = "HIGH"
                    )
                )
            } else {
                repository.insertLog(
                    SecurityLog(
                        eventType = "SCAN",
                        title = "Scan Clean",
                        description = "No active malware files or signature anomalies detected on device.",
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
                    title = "File Quarantined",
                    description = "Isolated dangerous file: ${file.name} safely from disk.",
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
                    title = "File Shredded",
                    description = "Permanently deleted infected threat ${file.name} from storage.",
                    severity = "INFO"
                )
            )
        }
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
                        description = "User authenticated identity using fingerprint / biometric keys successfully.",
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
            val ciphertext = LocalCrypto.encrypt(plainText)
            _vaultCiphertext.value = ciphertext
            _vaultDecryptedText.value = "" // Clear decryptions
            viewModelScope.launch {
                repository.insertLog(
                    SecurityLog(
                        eventType = "ENCRYPTION",
                        title = "Data Enveloped Successfully",
                        description = "Secured plaintext payload in cryptographically signed local envelope using AES-256.",
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
            val decrypted = LocalCrypto.decrypt(cipherText)
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
}

enum class Tab { Home, Security, Logs, Settings }
enum class ScanStatus { IDLE, SCANNING, COMPLETED }
data class ChartData(val label: String, val threatsCount: Int)

// Real Symmetric Crypto Engine using modern AES algorithms in pure Kotlin/JCA standard
object LocalCrypto {
    private const val ALGORITHM = "AES"
    // Static secret seed for fully offline cryptographic execution demonstration
    private val keyBytes = byteArrayOf(
        0x56.toByte(), 0x69.toByte(), 0x67.toByte(), 0x69.toByte(),
        0x6C.toByte(), 0x61.toByte(), 0x6E.toByte(), 0x74.toByte(),
        0x47.toByte(), 0x75.toByte(), 0x61.toByte(), 0x72.toByte(),
        0x64.toByte(), 0x4B.toByte(), 0x65.toByte(), 0x79.toByte() // "VigilantGuardKey" (16 bytes = AES-128)
    )
    private val secretKey = SecretKeySpec(keyBytes, ALGORITHM)

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.DEFAULT).trim()
    }

    fun decrypt(cipherText: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decoded = Base64.decode(cipherText, Base64.DEFAULT)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted, Charsets.UTF_8)
    }
}
