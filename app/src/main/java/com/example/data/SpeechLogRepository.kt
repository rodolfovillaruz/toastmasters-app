package com.example.data

import kotlinx.coroutines.flow.Flow

class SpeechLogRepository(private val speechLogDao: SpeechLogDao) {
    val allSpeechLogs: Flow<List<SpeechLog>> = speechLogDao.getAllSpeechLogs()

    suspend fun insert(speechLog: SpeechLog) {
        speechLogDao.insertSpeechLog(speechLog)
    }

    suspend fun delete(speechLog: SpeechLog) {
        speechLogDao.deleteSpeechLog(speechLog)
    }

    suspend fun clearAll() {
        speechLogDao.clearAll()
    }
}
