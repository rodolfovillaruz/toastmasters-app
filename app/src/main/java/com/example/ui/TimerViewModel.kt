package com.example.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SpeechLog
import com.example.data.SpeechLogRepository
import com.example.data.SpeechProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TimerState {
    IDLE, RUNNING, PAUSED
}

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SpeechLogRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SpeechLogRepository(database.speechLogDao())
    }

    val speechLogs: StateFlow<List<SpeechLog>> = repository.allSpeechLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentSpeechProfile = MutableStateFlow<SpeechProfile>(SpeechProfile.TableTopics)
    val currentSpeechProfile: StateFlow<SpeechProfile> = _currentSpeechProfile.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _elapsedMills = MutableStateFlow(0)
    val elapsedMills: StateFlow<Int> = _elapsedMills.asStateFlow()

    private var timerJob: Job? = null
    private var accumulatedTimeMillis: Long = 0L
    private var lastTickTime: Long = 0L

    // For custom input temporary variables
    val customName = MutableStateFlow("Custom Speech")
    val customGreenMin = MutableStateFlow("1")
    val customGreenSec = MutableStateFlow("0")
    val customYellowMin = MutableStateFlow("1")
    val customYellowSec = MutableStateFlow("30")
    val customRedMin = MutableStateFlow("2")
    val customRedSec = MutableStateFlow("0")
    val customMaxMin = MutableStateFlow("2")
    val customMaxSec = MutableStateFlow("30")

    fun selectProfile(profile: SpeechProfile) {
        if (_timerState.value == TimerState.IDLE) {
            _currentSpeechProfile.value = profile
            resetTimer()
        }
    }

    fun startTimer() {
        if (_timerState.value == TimerState.RUNNING) return

        _timerState.value = TimerState.RUNNING
        lastTickTime = SystemClock.elapsedRealtime()

        timerJob = viewModelScope.launch {
            while (isActive && _timerState.value == TimerState.RUNNING) {
                val now = SystemClock.elapsedRealtime()
                val delta = now - lastTickTime
                lastTickTime = now
                accumulatedTimeMillis += delta

                _elapsedSeconds.value = (accumulatedTimeMillis / 1000).toInt()
                _elapsedMills.value = (accumulatedTimeMillis % 1000).toInt()

                delay(30) // Refresh ~33 times per second for fluid progress and milli displays
            }
        }
    }

    fun pauseTimer() {
        if (_timerState.value != TimerState.RUNNING) return
        _timerState.value = TimerState.PAUSED
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        _timerState.value = TimerState.IDLE
        timerJob?.cancel()
        timerJob = null
        accumulatedTimeMillis = 0L
        _elapsedSeconds.value = 0
        _elapsedMills.value = 0
    }

    fun applyCustomProfile() {
        try {
            val name = customName.value.ifBlank { "Custom Speech" }
            val green = (customGreenMin.value.toIntOrNull() ?: 0) * 60 + (customGreenSec.value.toIntOrNull() ?: 0)
            val yellow = (customYellowMin.value.toIntOrNull() ?: 0) * 60 + (customYellowSec.value.toIntOrNull() ?: 0)
            val red = (customRedMin.value.toIntOrNull() ?: 0) * 60 + (customRedSec.value.toIntOrNull() ?: 0)
            val max = (customMaxMin.value.toIntOrNull() ?: 0) * 60 + (customMaxSec.value.toIntOrNull() ?: 0)

            // Validate sequence to maintain logical consistency (Green < Yellow < Red <= Max)
            if (green > 0 && yellow >= green && red >= yellow && max >= red) {
                val newProfile = SpeechProfile(
                    name = name,
                    greenSeconds = green,
                    yellowSeconds = yellow,
                    redSeconds = red,
                    maxSeconds = max,
                    isCustom = true
                )
                selectProfile(newProfile)
            }
        } catch (_: Exception) {
            // Squelch validation error and fall back safely
        }
    }

    fun saveSpeechLog(speakerName: String, role: String) {
        val finalSpeaker = speakerName.ifBlank { "Anonymous Speaker" }
        val profile = _currentSpeechProfile.value
        val log = SpeechLog(
            speakerName = finalSpeaker,
            speechRole = role,
            durationSeconds = _elapsedSeconds.value,
            greenSeconds = profile.greenSeconds,
            yellowSeconds = profile.yellowSeconds,
            redSeconds = profile.redSeconds
        )
        viewModelScope.launch {
            repository.insert(log)
        }
    }

    fun deleteLog(speechLog: SpeechLog) {
        viewModelScope.launch {
            repository.delete(speechLog)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class TimerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimerViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
