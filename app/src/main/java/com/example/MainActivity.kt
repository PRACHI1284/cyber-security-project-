package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ScannedFile
import com.example.data.SecurityLog
import com.example.ui.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Intent
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start Real-Time Protection Service
        val serviceIntent = Intent(this, com.example.core.services.RealtimeProtectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: SecurityViewModel = viewModel()) {
    val currentTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isBiometricLocked by viewModel.isBiometricLocked.collectAsStateWithLifecycle()
    val isBiometricDialogVisible by viewModel.isBiometricDialogVisible.collectAsStateWithLifecycle()
    val biometricMessage by viewModel.biometricMessage.collectAsStateWithLifecycle()

    // Dialog state for Encryption Vault
    var isVaultDialogVisible by remember { mutableStateOf(false) }
    
    val isAuditDialogVisible by viewModel.isAuditDialogVisible.collectAsStateWithLifecycle()
    val auditResults by viewModel.auditResults.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight),
        bottomBar = {
            BottomNavigationBar(
                selectedTab = currentTab,
                onTabSelected = { viewModel.selectedTab.value = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content rendering based on active tab
            Crossfade(
                targetState = currentTab,
                modifier = Modifier.fillMaxSize(),
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    Tab.Home -> HomeScreen(
                        viewModel = viewModel,
                        onOpenVault = { isVaultDialogVisible = true }
                    )
                    Tab.Security -> SecurityScreen(viewModel = viewModel)
                    Tab.DarkWeb -> DarkWebScreen()
                    Tab.Logs -> LogsScreen(viewModel = viewModel)
                    Tab.Settings -> SettingsScreen(viewModel = viewModel)
                }
            }

            // Simulated Biometric Authentication Dialog
            if (isBiometricDialogVisible) {
                BiometricSimulatedDialog(
                    message = biometricMessage,
                    onDismiss = { viewModel.closeBiometricDialog() },
                    onConfirm = { viewModel.authenticateBiometrics(true) },
                    onFail = { viewModel.authenticateBiometrics(false) }
                )
            }

            // Local Cryptography Vault Dialog
            if (isVaultDialogVisible) {
                EncryptionVaultDialog(
                    viewModel = viewModel,
                    onDismiss = { isVaultDialogVisible = false }
                )
            }
            
            if (isAuditDialogVisible) {
                DeviceAuditDialog(
                    results = auditResults,
                    onDismiss = { viewModel.closeAuditDialog() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = SurfaceLight,
        border = BorderStroke(1.dp, MutedSlateBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                NavigationItem(Tab.Home, "Home", Icons.Default.Home, "tab_home"),
                NavigationItem(Tab.Security, "Security", Icons.Default.Security, "tab_security"),
                NavigationItem(Tab.DarkWeb, "Dark Web", Icons.Default.Search, "tab_darkweb"),
                NavigationItem(Tab.Logs, "Logs", Icons.Default.History, "tab_logs"),
                NavigationItem(Tab.Settings, "Settings", Icons.Default.Settings, "tab_settings")
            )

            tabs.forEach { item ->
                val isActive = selectedTab == item.tab
                val targetBg = if (isActive) SoftShieldBlue else Color.Transparent
                val targetText = if (isActive) DarkTextBlue else MutedGrayText

                Column(
                    modifier = Modifier
                        .testTag(item.tag)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(item.tab) }
                        .padding(vertical = 6.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(targetBg)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = targetText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = targetText
                    )
                }
            }
        }
    }
}

data class NavigationItem(
    val tab: Tab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
)

// ==========================================
// SCREEN 1: HOME TAB (Bento Grid Theme)
// ==========================================
@Composable
fun HomeScreen(
    viewModel: SecurityViewModel,
    onOpenVault: () -> Unit
) {
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()
    val isBiometricLocked by viewModel.isBiometricLocked.collectAsStateWithLifecycle()
    val logs by viewModel.repositoryAllLogs.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Vigilant Guard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Advanced Cybersecurity Suite",
                    fontSize = 12.sp,
                    color = MutedGrayText,
                    fontWeight = FontWeight.Normal
                )
            }
            // Rounded User Initials badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MutedSlateBg)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "JD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText
                )
            }
        }

        // BENTO GRID CONTAINER
        // Bento Cell 1: Shield System Status (Large, Col-Span 2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SoftShieldBlue)
                .border(1.dp, BorderBlue, RoundedCornerShape(28.dp))
                .clickable { viewModel.startScan() }
                .testTag("cell_status_shield")
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (scanStatus == ScanStatus.SCANNING) Icons.Default.Loop else Icons.Default.Shield,
                            contentDescription = "Shield Icon",
                            tint = BrandBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Badge Indicator
                    Surface(
                        color = if (scanStatus == ScanStatus.SCANNING) ThreatYellow else BrandBlue,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = if (scanStatus == ScanStatus.SCANNING) "SCANNING" else "LIVE PROTECTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (scanStatus) {
                        ScanStatus.SCANNING -> "Scanning Device..."
                        else -> "System Shielded"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextBlue
                )
                Text(
                    text = when (scanStatus) {
                        ScanStatus.SCANNING -> "Evaluating system libraries and packages"
                        else -> "Real-time heuristic analysis active"
                    },
                    fontSize = 13.sp,
                    color = BrandBlue.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar or Last Scan indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (scanStatus == ScanStatus.SCANNING) {
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(100.dp)),
                            color = BrandBlue,
                            trackColor = BorderBlue
                        )
                        Text(
                            text = "${(scanProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(BorderBlue)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(1f)
                                    .background(BrandBlue)
                            )
                        }
                        Text(
                            text = "Last scan: Just now",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bento Cell 2 & Cell 3 Row (Side by side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cell 2: Biometrics Lock (Col-Span 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SoftPurpleBg)
                    .border(1.dp, PurpleBorder, RoundedCornerShape(28.dp))
                    .clickable { viewModel.showBiometricDialog() }
                    .testTag("cell_biometrics")
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PurpleBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint",
                            tint = PurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Biometrics",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                    Text(
                        text = "App-level shield protection",
                        fontSize = 10.sp,
                        color = MutedGrayText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated toggle widget inside card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBiometricLocked) "ACTIVE" else "DISABLED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 12.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (isBiometricLocked) PurpleAccent else MutedSlateBorder)
                                .padding(horizontal = 2.dp),
                            contentAlignment = if (isBiometricLocked) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            // Cell 3: Encryption (Col-Span 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SoftVioletBg)
                    .border(1.dp, VioletBorder, RoundedCornerShape(28.dp))
                    .clickable { onOpenVault() }
                    .testTag("cell_encryption")
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Lock",
                            tint = DarkPurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Encryption",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextBlue
                    )
                    Text(
                        text = "E2E Protocol v2.4 initialized",
                        fontSize = 10.sp,
                        color = MutedGrayText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secured tunnel status indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ThreatGreen)
                        )
                        Text(
                            text = "Secured Vault",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkPurpleAccent
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bento Cell 4: Threat Intelligence Widget (Col-Span 2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MutedSlateBg)
                .border(1.dp, MutedSlateBorder, RoundedCornerShape(28.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Threat Intelligence",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                    Text(
                        text = "Weekly Market Trend",
                        fontSize = 10.sp,
                        color = MutedGrayText,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Graphic Bar Chart represented in columns (Row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    viewModel.weeklyIntel.forEach { chart ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Text hover indicating threats
                            Text(
                                text = "${chart.threatsCount}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            // Styled rounded column
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height((chart.threatsCount * 4).dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                BrandBlue,
                                                BrandBlue.copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chart.label,
                                fontSize = 9.sp,
                                color = MutedGrayText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Bento Cell 5: OS Security Audit (Col-Span 2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ThreatGreen.copy(alpha = 0.1f))
                .border(1.dp, ThreatGreen.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                .clickable { viewModel.runDeviceAudit(context) }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SecurityUpdateGood, contentDescription = null, tint = ThreatGreen)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Device Security Audit", fontWeight = FontWeight.Bold, color = CharcoalText)
                        Text("Scan OS for root, ADB, & exploits", fontSize = 11.sp, color = MutedGrayText)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutedGrayText)
            }
        }
    }
}

// ==========================================
// SCREEN 2: ACTIVE SCAN / MALWARE DETECTOR
// ==========================================
@Composable
fun SecurityScreen(viewModel: SecurityViewModel) {
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val currentFile by viewModel.currentScanningFile.collectAsStateWithLifecycle()
    val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()
    val scannedResults by viewModel.scannedResults.collectAsStateWithLifecycle()
    val consoleLog by viewModel.activeConsoleLog.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Malware & Threat Scanner",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTextBlue
        )
        Text(
            text = "Active signature hash & sandbox heuristics engine",
            fontSize = 12.sp,
            color = MutedGrayText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Control Panel card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SoftShieldBlue.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderBlue.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Signature Database",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextBlue
                        )
                        Text(
                            text = "v4.12.9 (Daily Updated)",
                            fontSize = 11.sp,
                            color = BrandBlue
                        )
                    }

                    Button(
                        onClick = { viewModel.startScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("scan_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Scan Icon",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (scanStatus == ScanStatus.SCANNING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing: ${currentFile ?: "Initializing..."}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(100.dp)),
                        color = BrandBlue,
                        trackColor = BorderBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Console logger output box
        if (consoleLog.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                color = CharcoalText,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = consoleLog,
                        color = Color.Green,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live threat result list
        Text(
            text = "Scanned Elements (${scannedResults.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (scannedResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MutedSlateBg.copy(alpha = 0.3f))
                    .border(
                        BorderStroke(1.dp, MutedSlateBorder.copy(alpha = 0.5f)),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield placeholder",
                        tint = MutedSlateBorder,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No scanning sequence launched yet.",
                        fontSize = 12.sp,
                        color = MutedGrayText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scannedResults) { file ->
                    ThreatItemCard(
                        file = file,
                        onQuarantine = { viewModel.quarantineFile(file) },
                        onDelete = { viewModel.deleteFileFromDisk(file) },
                        onExplain = { viewModel.explainThreatWithGemini(file) }
                    )
                }
            }
        }
        
        // AI Explanation Dialog
        val aiExplanation by viewModel.aiExplanation.collectAsStateWithLifecycle()
        if (aiExplanation != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearAiExplanation() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI",
                            tint = BrandBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini AI Analysis", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = aiExplanation!!,
                        fontSize = 13.sp,
                        color = CharcoalText
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearAiExplanation() }) {
                        Text("Got it", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun ThreatItemCard(
    file: ScannedFile,
    onQuarantine: () -> Unit,
    onDelete: () -> Unit,
    onExplain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (file.isMalicious) ThreatRed.copy(alpha = 0.4f) else MutedSlateBorder.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (file.isMalicious) ThreatRed.copy(alpha = 0.05f) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (file.isMalicious) ThreatRed.copy(alpha = 0.15f) else ThreatGreen.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (file.isMalicious) Icons.Default.BugReport else Icons.Default.CheckCircle,
                            contentDescription = "File Health Status",
                            tint = if (file.isMalicious) ThreatRed else ThreatGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${file.path} | ${(file.sizeBytes / 1024)} KB",
                            fontSize = 10.sp,
                            color = MutedGrayText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Severity / Status text
                Text(
                    text = if (file.isMalicious) "HIGH RISK (${file.riskScore}%)" else "CLEAN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (file.isMalicious) ThreatRed else ThreatGreen
                )
            }

            if (file.isMalicious) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = ThreatRed.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Threat: ${file.detectedThreatName}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThreatRed
                )
                Text(
                    text = file.flagReason,
                    fontSize = 10.sp,
                    color = MutedGrayText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onExplain,
                        border = BorderStroke(1.dp, BrandBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Explain", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = onQuarantine,
                        border = BorderStroke(1.dp, ThreatYellow),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThreatYellow),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Quarantine", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = ThreatRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Shred/Delete", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: SECURITY EVENT AUDIT LOGS (SQLite Room)
// ==========================================
@Composable
fun LogsScreen(viewModel: SecurityViewModel) {
    val logs by viewModel.repositoryAllLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Security Events Feed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextBlue
                )
                Text(
                    text = "SQLite auditing of internal security states",
                    fontSize = 12.sp,
                    color = MutedGrayText
                )
            }

            IconButton(
                onClick = { viewModel.clearLogs() },
                modifier = Modifier
                    .testTag("clear_logs_button")
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear logs",
                    tint = ThreatRed
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "No logs",
                        tint = MutedSlateBorder,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "History logs are empty",
                        fontSize = 12.sp,
                        color = MutedGrayText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LogEntryCard(log = log)
                }
            }
        }
    }
}

@Composable
fun LogEntryCard(log: SecurityLog) {
    val severityColor = when (log.severity) {
        "HIGH" -> ThreatRed
        "WARNING" -> ThreatYellow
        else -> BrandBlue
    }

    val icon = when (log.eventType) {
        "SCAN" -> Icons.Default.Search
        "THREAT" -> Icons.Default.BugReport
        "BIOMETRIC" -> Icons.Default.Fingerprint
        "ENCRYPTION" -> Icons.Default.Lock
        else -> Icons.Default.Notifications
    }

    val formattedDate = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MutedSlateBorder.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(severityColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = log.eventType,
                    tint = severityColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(severityColor)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.description,
                    fontSize = 11.sp,
                    color = MutedGrayText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formattedDate,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedGrayText.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==========================================
// SCREEN 4: DETAILED APP SETTINGS
// ==========================================
@Composable
fun SettingsScreen(viewModel: SecurityViewModel) {
    val heuristicLevel by viewModel.settingHeuristicLevel.collectAsStateWithLifecycle()
    val isAutoUpdate by viewModel.settingAutoUpdate.collectAsStateWithLifecycle()
    val isRealtime by viewModel.settingRealtimeProtection.collectAsStateWithLifecycle()

    var showHeuristicMenu by remember { mutableStateOf(false) }
    val heuristicOptions = listOf("Low (Signatures Only)", "Medium (Basic Sandboxing)", "High (Default)")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Engine Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTextBlue
        )
        Text(
            text = "Configure engine sensitivities and auto protection shields",
            fontSize = 12.sp,
            color = MutedGrayText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Protection Toggles Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MutedSlateBorder.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACTIVE GUARD POLICIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Toggle 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-time Sandbox Protection",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Text(
                            text = "Analyzes active APK installers dynamically",
                            fontSize = 11.sp,
                            color = MutedGrayText
                        )
                    }
                    Switch(
                        checked = isRealtime,
                        onCheckedChange = { viewModel.settingRealtimeProtection.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandBlue)
                    )
                }

                Divider(color = MutedSlateBorder.copy(alpha = 0.3f))

                // Toggle 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Threat Base Updates",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Text(
                            text = "Synchronize definitions with world threat servers",
                            fontSize = 11.sp,
                            color = MutedGrayText
                        )
                    }
                    Switch(
                        checked = isAutoUpdate,
                        onCheckedChange = { viewModel.settingAutoUpdate.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandBlue)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Advanced Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MutedSlateBorder.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HEURISTIC SENSITIVITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MutedSlateBg.copy(alpha = 0.3f))
                        .border(1.dp, MutedSlateBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { showHeuristicMenu = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Severity Mode",
                                fontSize = 11.sp,
                                color = MutedGrayText
                            )
                            Text(
                                text = heuristicLevel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Options",
                            tint = CharcoalText
                        )
                    }

                    DropdownMenu(
                        expanded = showHeuristicMenu,
                        onDismissRequest = { showHeuristicMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        heuristicOptions.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level, fontSize = 13.sp, color = CharcoalText) },
                                onClick = {
                                    viewModel.settingHeuristicLevel.value = level
                                    showHeuristicMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SIMULATED DIALOG: BIOMETRICS LOCKOUT
// ==========================================
@Composable
fun BiometricSimulatedDialog(
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFail: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint icon",
                    tint = PurpleAccent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Biometric Authentication", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = MutedGrayText
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftPurpleBg)
                        .border(1.dp, PurpleBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Touch Sensor Area",
                            tint = PurpleAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Touch Sensor to Simulate Lock",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = PurpleAccent),
                modifier = Modifier.testTag("biometric_success_btn")
            ) {
                Text("Simulate Success", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onFail,
                colors = ButtonDefaults.textButtonColors(contentColor = ThreatRed),
                modifier = Modifier.testTag("biometric_fail_btn")
            ) {
                Text("Simulate Fail", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

// ==========================================
// INTERACTIVE VAULT DIALOG: AES ENCRYPTION
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptionVaultDialog(
    viewModel: SecurityViewModel,
    onDismiss: () -> Unit
) {
    val plainText by viewModel.vaultPlaintext.collectAsStateWithLifecycle()
    val cipherText by viewModel.vaultCiphertext.collectAsStateWithLifecycle()
    val decryptedText by viewModel.vaultDecryptedText.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Vault Lock",
                    tint = DarkPurpleAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("End-to-End Encrypted Vault", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Encrypt credentials or sensitive configuration tokens locally on disk using hardware-backed AES-256 protocols.",
                    fontSize = 11.sp,
                    color = MutedGrayText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Input box
                TextField(
                    value = plainText,
                    onValueChange = { viewModel.updatePlaintext(it) },
                    placeholder = { Text("Enter sensitive text to secure...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_plaintext_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftVioletBg.copy(alpha = 0.4f),
                        unfocusedContainerColor = SoftVioletBg.copy(alpha = 0.2f),
                        focusedIndicatorColor = DarkPurpleAccent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Actions Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.encryptVaultData() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPurpleAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("encrypt_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Encrypt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.decryptVaultData() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("decrypt_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Decrypt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Ciphertext result display card
                if (cipherText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "CIPHERTEXT ENVELOPE (AES-128)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPurpleAccent
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SoftVioletBg.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, VioletBorder.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = cipherText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = DarkPurpleAccent,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Decrypted result display card
                if (decryptedText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "DECRYPTED VALUE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThreatGreen
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ThreatGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ThreatGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = decryptedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ThreatGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = DarkPurpleAccent)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun DeviceAuditDialog(
    results: List<com.example.core.security.AuditResult>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SecurityUpdateGood, contentDescription = null, tint = ThreatGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Device Audit Results", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (result.isSecure) ThreatGreen.copy(alpha = 0.1f) else ThreatRed.copy(alpha = 0.1f))
                            .border(1.dp, if (result.isSecure) ThreatGreen.copy(alpha = 0.3f) else ThreatRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (result.isSecure) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (result.isSecure) ThreatGreen else ThreatRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(result.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalText)
                            Text(result.description, fontSize = 11.sp, color = MutedGrayText, lineHeight = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
