package com.example.data

import kotlinx.coroutines.flow.Flow

class SecurityRepository(private val securityDao: SecurityDao) {
    val allLogs: Flow<List<SecurityLog>> = securityDao.getAllLogs()
    val highThreatCount: Flow<Int> = securityDao.getHighThreatCount()

    suspend fun insertLog(log: SecurityLog) {
        securityDao.insertLog(log)
    }

    suspend fun clearLogs() {
        securityDao.clearLogs()
    }
}
