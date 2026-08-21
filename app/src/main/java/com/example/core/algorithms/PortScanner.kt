package com.example.core.algorithms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Asynchronous Network Scanner.
 * Probes open ports on a given IP address using Coroutines for fast concurrent execution.
 */
object PortScanner {

    // Common ports exploited by malware or used for unauthorized backdoors
    private val COMMON_PORTS = listOf(
        21, 22, 23, 25, 53, 80, 110, 135, 137, 138, 139, 143, 443, 445, 1433, 1434, 3306, 3389, 4444, 5900, 8080, 8443
    )

    /**
     * Scans an IP address for common open ports.
     * Uses Dispatchers.IO to concurrently attempt socket connections.
     */
    suspend fun scanPorts(ipAddress: String, timeoutMs: Int = 1000): List<Int> = withContext(Dispatchers.IO) {
        val openPorts = mutableListOf<Int>()
        
        val deferredResults = COMMON_PORTS.map { port ->
            async {
                if (isPortOpen(ipAddress, port, timeoutMs)) {
                    port
                } else {
                    null
                }
            }
        }
        
        val results = deferredResults.awaitAll()
        results.filterNotNullTo(openPorts)
        
        return@withContext openPorts
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
