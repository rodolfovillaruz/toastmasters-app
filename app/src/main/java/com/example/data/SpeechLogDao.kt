package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeechLogDao {
    @Query("SELECT * FROM speech_logs ORDER BY timestamp DESC")
    fun getAllSpeechLogs(): Flow<List<SpeechLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeechLog(speechLog: SpeechLog)

    @Delete
    suspend fun deleteSpeechLog(speechLog: SpeechLog)

    @Query("DELETE FROM speech_logs")
    suspend fun clearAll()
}
