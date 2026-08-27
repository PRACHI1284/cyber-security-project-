package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BreachRecord(
    val name: String,
    val domain: String,
    val breachDate: String,
    val description: String,
    val dataClasses: List<String>
)

class DarkWebViewModel : ViewModel() {
    private val _emailQuery = MutableStateFlow("")
    val emailQuery: StateFlow<String> = _emailQuery.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResults = MutableStateFlow<List<BreachRecord>?>(null)
    val scanResults: StateFlow<List<BreachRecord>?> = _scanResults.asStateFlow()

    fun updateEmail(email: String) {
        _emailQuery.value = email
    }

    fun startScan() {
        val email = _emailQuery.value
        if (email.isEmpty()) return
        
        _isScanning.value = true
        
        viewModelScope.launch {
            // Simulate network delay for API Call (e.g. HaveIBeenPwned)
            delay(2000)
            
            // In a real app, you would make an HTTP request to HIBP API here:
            // GET https://haveibeenpwned.com/api/v3/breachedaccount/{email}
            // require "hibp-api-key" header.
            
            // Mocking results for demonstration since we lack an API key
            val results = if (email.contains("test") || email.contains("admin")) {
                listOf(
                    BreachRecord(
                        name = "Collection #1",
                        domain = "mega.nz",
                        breachDate = "2019-01-07",
                        description = "A massive collection of credential stuffing lists discovered on a popular hacking forum.",
                        dataClasses = listOf("Email addresses", "Passwords")
                    ),
                    BreachRecord(
                        name = "LinkedIn",
                        domain = "linkedin.com",
                        breachDate = "2012-05-05",
                        description = "In May 2012, LinkedIn had 164 million email addresses and passwords exposed.",
                        dataClasses = listOf("Email addresses", "Passwords")
                    )
                )
            } else {
                emptyList()
            }
            
            _scanResults.value = results
            _isScanning.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkWebScreen(viewModel: DarkWebViewModel = viewModel()) {
    val emailQuery by viewModel.emailQuery.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanResults by viewModel.scanResults.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Dark Web Scanner",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTextBlue
        )
        Text(
            text = "Check if your credentials have been compromised in known data breaches.",
            fontSize = 12.sp,
            color = MutedGrayText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = emailQuery,
            onValueChange = { viewModel.updateEmail(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter email address...") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandBlue) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = MutedSlateBorder
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.startScan() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = !isScanning && emailQuery.isNotEmpty()
        ) {
            if (isScanning) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scanning Dark Web...")
            } else {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan for Breaches")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (scanResults != null) {
            val results = scanResults!!
            if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ThreatGreen.copy(alpha = 0.1f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = ThreatGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Good news!", fontWeight = FontWeight.Bold, color = ThreatGreen, fontSize = 16.sp)
                        Text("No breaches found for this email.", color = ThreatGreen, fontSize = 12.sp)
                    }
                }
            } else {
                Text(
                    text = "Breaches Found (${results.size})",
                    fontWeight = FontWeight.Bold,
                    color = ThreatRed,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(results) { breach ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ThreatRed.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, ThreatRed.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = ThreatRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(breach.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalText)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Domain: ${breach.domain} • Date: ${breach.breachDate}", fontSize = 11.sp, color = MutedGrayText)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(breach.description, fontSize = 12.sp, color = CharcoalText, lineHeight = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Compromised Data: ${breach.dataClasses.joinToString(", ")}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThreatRed)
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Enter an email address to check for leaked credentials.", color = MutedGrayText, fontSize = 12.sp)
            }
        }
    }
}
