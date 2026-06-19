package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speech_logs")
data class SpeechLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val speakerName: String,
    val speechRole: String, // e.g. "Table Topics", "Evaluation", "Ice Breaker", "Prepared Speech (5-7)", "Custom"
    val durationSeconds: Int, // The total recorded speech duration under timing
    val greenSeconds: Int,
    val yellowSeconds: Int,
    val redSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)
