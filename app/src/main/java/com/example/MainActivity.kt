package com.example

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SpeechLog
import com.example.data.SpeechProfile
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.TimerState
import com.example.ui.TimerViewModel
import com.example.ui.TimerViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpeechTimerApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
fun SpeechTimerApp(
    modifier: Modifier = Modifier,
    activity: Activity? = LocalContext.current as? Activity
) {
    val viewModel: TimerViewModel = viewModel(
        factory = TimerViewModelFactory(activity!!.application)
    )

    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val elapsedMills by viewModel.elapsedMills.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentSpeechProfile.collectAsStateWithLifecycle()
    val speechLogs by viewModel.speechLogs.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    // Keep screen on when timer is active
    if (timerState == TimerState.RUNNING) {
        KeepScreenOn()
    }

    // Modal dialogue flags
    if (showSaveDialog) {
        SaveSpeechLogDialog(
            elapsedSeconds = elapsedSeconds,
            speechProfileName = currentProfile.name,
            onDismiss = { showSaveDialog = false },
            onSave = { name, role ->
                viewModel.saveSpeechLog(name, role)
                showSaveDialog = false
            }
        )
    }

    if (showCustomDialog) {
        CustomProfileDialog(
            viewModel = viewModel,
            onDismiss = { showCustomDialog = false },
            onApply = {
                viewModel.applyCustomProfile()
                showCustomDialog = false
            }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth > 600.dp

        if (isWide) {
            // Landscape / Wide screen dynamic layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Timer display, standard triggers, and timing signals
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppHeader()

                    TimerCard(
                        elapsedSeconds = elapsedSeconds,
                        elapsedMills = elapsedMills,
                        profile = currentProfile,
                        timerState = timerState,
                        modifier = Modifier.weight(1f)
                    )

                    TimerControls(
                        timerState = timerState,
                        elapsedSeconds = elapsedSeconds,
                        onStart = { viewModel.startTimer() },
                        onPause = { viewModel.pauseTimer() },
                        onReset = { viewModel.resetTimer() },
                        onSave = { showSaveDialog = true }
                    )
                }

                // Right Panel: Profiles list and past logs
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SpeechProfileSelectors(
                        currentProfile = currentProfile,
                        onSelectProfile = { viewModel.selectProfile(it) },
                        onCustomClick = { showCustomDialog = true },
                        timerState = timerState,
                        modifier = Modifier.wrapContentHeight()
                    )

                    TimingHistoryBoard(
                        logs = speechLogs,
                        onDeleteLog = { viewModel.deleteLog(it) },
                        onClearAll = { viewModel.clearAllLogs() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Portrait / Compact layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppHeader()

                SpeechProfileSelectors(
                    currentProfile = currentProfile,
                    onSelectProfile = { viewModel.selectProfile(it) },
                    onCustomClick = { showCustomDialog = true },
                    timerState = timerState
                )

                TimerCard(
                    elapsedSeconds = elapsedSeconds,
                    elapsedMills = elapsedMills,
                    profile = currentProfile,
                    timerState = timerState,
                    modifier = Modifier.weight(1.2f)
                )

                TimerControls(
                    timerState = timerState,
                    elapsedSeconds = elapsedSeconds,
                    onStart = { viewModel.startTimer() },
                    onPause = { viewModel.pauseTimer() },
                    onReset = { viewModel.resetTimer() },
                    onSave = { showSaveDialog = true }
                )

                TimingHistoryBoard(
                    logs = speechLogs,
                    onDeleteLog = { viewModel.deleteLog(it) },
                    onClearAll = { viewModel.clearAllLogs() },
                    modifier = Modifier.weight(1.0f)
                )
            }
        }
    }
}

@Composable
fun AppHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Microphone",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "Toastmasters Timer",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SpeechProfileSelectors(
    currentProfile: SpeechProfile,
    onSelectProfile: (SpeechProfile) -> Unit,
    onCustomClick: () -> Unit,
    timerState: TimerState,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Speech Selection",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Dynamic grid list of speech timing options
            // Since we might be in running mode, disable profile switching to prevent disruptions
            val changeEnabled = timerState == TimerState.IDLE

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Table Topics & Evaluations
                ProfileButton(
                    profile = SpeechProfile.TableTopics,
                    isSelected = currentProfile.name == SpeechProfile.TableTopics.name && !currentProfile.isCustom,
                    enabled = changeEnabled,
                    onClick = { onSelectProfile(SpeechProfile.TableTopics) },
                    modifier = Modifier.weight(1f)
                )
                ProfileButton(
                    profile = SpeechProfile.Evaluation,
                    isSelected = currentProfile.name == SpeechProfile.Evaluation.name && !currentProfile.isCustom,
                    enabled = changeEnabled,
                    onClick = { onSelectProfile(SpeechProfile.Evaluation) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ice Breaker & Prepared Speech
                ProfileButton(
                    profile = SpeechProfile.IceBreaker,
                    isSelected = currentProfile.name == SpeechProfile.IceBreaker.name && !currentProfile.isCustom,
                    enabled = changeEnabled,
                    onClick = { onSelectProfile(SpeechProfile.IceBreaker) },
                    modifier = Modifier.weight(1f)
                )
                ProfileButton(
                    profile = SpeechProfile.PreparedSpeech,
                    isSelected = currentProfile.name == SpeechProfile.PreparedSpeech.name && !currentProfile.isCustom,
                    enabled = changeEnabled,
                    onClick = { onSelectProfile(SpeechProfile.PreparedSpeech) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Custom configuration row
            Button(
                onClick = onCustomClick,
                enabled = changeEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentProfile.isCustom) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                    contentColor = if (currentProfile.isCustom) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("custom_profile_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Custom Target Selectors",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentProfile.isCustom) "Custom: ${currentProfile.name}" else "Configure Custom Profile",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!changeEnabled) {
                Text(
                    text = "⚠️ Reset the current timer session to select another speech profile.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ProfileButton(
    profile: SpeechProfile,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .height(44.dp)
            .testTag("profile_${profile.name.lowercase().replace(" ", "_")}"),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = profile.name.substringBefore("(").trim(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${profile.greenSeconds / 60}-${profile.redSeconds / 60} Min",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TimerCard(
    elapsedSeconds: Int,
    elapsedMills: Int,
    profile: SpeechProfile,
    timerState: TimerState,
    modifier: Modifier = Modifier
) {
    // Determine target state and signal colors matching Toastmasters standards:
    // Green time is when safe minimum is hit.
    // Yellow time is when speaker is in final segment.
    // Red time is limit.
    val isGreen = elapsedSeconds >= profile.greenSeconds
    val isYellow = elapsedSeconds >= profile.yellowSeconds
    val isRed = elapsedSeconds >= profile.redSeconds
    val isOvertime = elapsedSeconds >= profile.maxSeconds

    val signalColor = when {
        isRed -> Color(0xFFC62828) // Strong Crimson
        isYellow -> Color(0xFFF9A825) // Golden Yellow/Amber
        isGreen -> Color(0xFF2E7D32) // Forest Green
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp) // Neutral Surface
    }

    val onSignalColor = when {
        isRed || isGreen -> Color.White
        isYellow -> Color.Black
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Overtime flash effect
    val infiniteTransition = rememberInfiniteTransition(label = "overtime_transition")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_pulse"
    )

    // Smooth state-to-state background transitions
    val animatedBackground by animateColorAsState(
        targetValue = signalColor,
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "tint_transition"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timer_display_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBackground),
        border = if (isOvertime) {
            BorderStroke(5.dp, Color(0xFFE53935).copy(alpha = borderAlpha))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Signal Label Top Left
            Row(
                modifier = Modifier.align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = when {
                                isRed -> Color(0xFFFF5252)
                                isYellow -> Color(0xFFFFD740)
                                isGreen -> Color(0xFF69F0AE)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                )
                Text(
                    text = when {
                        isOvertime -> "OVERTIME (Grace Period Exceeded)"
                        isRed -> "SIGNAL: RED (Wrap up Immediately)"
                        isYellow -> "SIGNAL: YELLOW (Wrap up)"
                        isGreen -> "SIGNAL: GREEN (Speech Qualified)"
                        else -> "SIGNAL: COLD (Below Target)"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = onSignalColor.copy(alpha = 0.8f)
                )
            }

            // Big Clock Counter
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val mins = elapsedSeconds / 60
                val secs = elapsedSeconds % 60
                val formattedTime = String.format("%02d:%02d", mins, secs)
                val formattedMills = String.format("%01d", elapsedMills / 100) // Tenths of seconds

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 72.sp,
                            lineHeight = 72.sp
                        ),
                        color = onSignalColor
                    )
                    Text(
                        text = ".$formattedMills",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp
                        ),
                        color = onSignalColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar indicating thresholds
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(onSignalColor.copy(alpha = 0.15f))
                ) {
                    val progressFraction = if (profile.maxSeconds > 0) {
                        (elapsedSeconds.toFloat() / profile.maxSeconds.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF66BB6A), // Emerald
                                        Color(0xFFFFCA28), // Golden
                                        Color(0xFFEF5350)  // Red
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Threshold Guideline Text
                Text(
                    text = "Current Track: ${profile.name}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = onSignalColor.copy(alpha = 0.9f)
                )
            }

            // Benchmark Indicators Row Bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TimeGuideLabel(label = "GREEN", seconds = profile.greenSeconds, textColor = onSignalColor)
                TimeGuideLabel(label = "YELLOW", seconds = profile.yellowSeconds, textColor = onSignalColor)
                TimeGuideLabel(label = "RED", seconds = profile.redSeconds, textColor = onSignalColor)
                TimeGuideLabel(label = "MAX", seconds = profile.maxSeconds, textColor = onSignalColor)
            }
        }
    }
}

@Composable
fun TimeGuideLabel(label: String, seconds: Int, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = textColor.copy(alpha = 0.6f)
        )
        val mins = seconds / 60
        val secs = seconds % 60
        Text(
            text = String.format("%d:%02d", mins, secs),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
fun TimerControls(
    timerState: TimerState,
    elapsedSeconds: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset button
        IconButton(
            onClick = onReset,
            enabled = timerState != TimerState.IDLE,
            modifier = Modifier
                .size(56.dp)
                .testTag("reset_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Timer",
                tint = if (timerState != TimerState.IDLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Play/Pause button
        Button(
            onClick = {
                if (timerState == TimerState.RUNNING) {
                    onPause()
                } else {
                    onStart()
                }
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (timerState == TimerState.RUNNING) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .size(72.dp)
                .testTag("play_pause_button"),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = if (timerState == TimerState.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (timerState == TimerState.RUNNING) "Pause Session" else "Start Session",
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Save log button
        IconButton(
            onClick = onSave,
            enabled = elapsedSeconds > 0 && timerState != TimerState.RUNNING,
            modifier = Modifier
                .size(56.dp)
                .testTag("save_button")
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save Timing Log",
                tint = if (elapsedSeconds > 0 && timerState != TimerState.RUNNING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun TimingHistoryBoard(
    logs: List<SpeechLog>,
    onDeleteLog: (SpeechLog) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Meeting Timing Logs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${logs.size} Record${if (logs.size == 1) "" else "s"} saved in database",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAll,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear All Logs", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Empty History logs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No speech logs recorded yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Select a speech type, run your session, and press the [Save] icon to log timings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs, key = { it.id }) { log ->
                        TimingLogItem(log = log, onDelete = { onDeleteLog(log) })
                    }
                }
            }
        }
    }
}

@Composable
fun TimingLogItem(
    log: SpeechLog,
    onDelete: () -> Unit
) {
    // Official Toastmasters rules allow a 30-second grace period on both sides of target timings!
    // To be qualified, the speech must be:
    // >= Green time - 30s AND <= Red time + 30s.
    // Except for Table Topics, where the lower limit is Green time (60s) - 30s = 30s. Perfect accuracy!
    val minThreshold = (log.greenSeconds - 30).coerceAtLeast(30)
    val maxThreshold = log.redSeconds + 30
    val isQualified = log.durationSeconds in minThreshold..maxThreshold

    val durationMins = log.durationSeconds / 60
    val durationSecs = log.durationSeconds % 60
    val durationText = String.format("%d:%02d", durationMins, durationSecs)

    val dateFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeLabel = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_item_${log.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = log.speakerName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = log.speechRole,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Timing signal color indicators
                    val colorIndicator = when {
                        log.durationSeconds >= log.redSeconds -> Color(0xFFC62828)
                        log.durationSeconds >= log.yellowSeconds -> Color(0xFFF57F17)
                        log.durationSeconds >= log.greenSeconds -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colorIndicator, CircleShape)
                    )

                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Qualification Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isQualified) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isQualified) "QUALIFIED" else "DISQUALIFIED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 8.sp,
                            color = if (isQualified) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete speech timing database record",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveSpeechLogDialog(
    elapsedSeconds: Int,
    speechProfileName: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var speakerName by remember { mutableStateOf("") }
    var speechRoleDetail by remember { mutableStateOf(speechProfileName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Save Timing Result",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val mins = elapsedSeconds / 60
                val secs = elapsedSeconds % 60
                Text(
                    text = String.format("Recorded Duration: %02d:%02d", mins, secs),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = speakerName,
                    onValueChange = { speakerName = it },
                    label = { Text("Speaker Name") },
                    placeholder = { Text("e.g. Distinguished John Doe") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speaker_name_input")
                )

                OutlinedTextField(
                    value = speechRoleDetail,
                    onValueChange = { speechRoleDetail = it },
                    label = { Text("Speech Assignment / Topic") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speech_assignment_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(speakerName, speechRoleDetail) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_save_button")
            ) {
                Text("Save to Log")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProfileDialog(
    viewModel: TimerViewModel,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val name by viewModel.customName.collectAsStateWithLifecycle()
    val greenMin by viewModel.customGreenMin.collectAsStateWithLifecycle()
    val greenSec by viewModel.customGreenSec.collectAsStateWithLifecycle()
    val yellowMin by viewModel.customYellowMin.collectAsStateWithLifecycle()
    val yellowSec by viewModel.customYellowSec.collectAsStateWithLifecycle()
    val redMin by viewModel.customRedMin.collectAsStateWithLifecycle()
    val redSec by viewModel.customRedSec.collectAsStateWithLifecycle()
    val maxMin by viewModel.customMaxMin.collectAsStateWithLifecycle()
    val maxSec by viewModel.customMaxSec.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = "Settings Icon", tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Custom Speech Timing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.customName.value = it },
                        label = { Text("Assignment Label") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("custom_name_input")
                    )
                }

                item {
                    Text(
                        "Threshold timings (Min & Sec):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    TimeThresholdRow(
                        label = "Green Alert",
                        minVal = greenMin,
                        secVal = greenSec,
                        onMinChange = { viewModel.customGreenMin.value = it },
                        onSecChange = { viewModel.customGreenSec.value = it }
                    )
                }

                item {
                    TimeThresholdRow(
                        label = "Yellow Alert",
                        minVal = yellowMin,
                        secVal = yellowSec,
                        onMinChange = { viewModel.customYellowMin.value = it },
                        onSecChange = { viewModel.customYellowSec.value = it }
                    )
                }

                item {
                    TimeThresholdRow(
                        label = "Red Alert",
                        minVal = redMin,
                        secVal = redSec,
                        onMinChange = { viewModel.customRedMin.value = it },
                        onSecChange = { viewModel.customRedSec.value = it }
                    )
                }

                item {
                    TimeThresholdRow(
                        label = "Max Boundary",
                        minVal = maxMin,
                        secVal = maxSec,
                        onMinChange = { viewModel.customMaxMin.value = it },
                        onSecChange = { viewModel.customMaxSec.value = it }
                    )
                }

                item {
                    val g = (greenMin.toIntOrNull() ?: 0) * 60 + (greenSec.toIntOrNull() ?: 0)
                    val y = (yellowMin.toIntOrNull() ?: 0) * 60 + (yellowSec.toIntOrNull() ?: 0)
                    val r = (redMin.toIntOrNull() ?: 0) * 60 + (redSec.toIntOrNull() ?: 0)
                    val m = (maxMin.toIntOrNull() ?: 0) * 60 + (maxSec.toIntOrNull() ?: 0)

                    val sequencesValid = g > 0 && y >= g && r >= y && m >= r

                    if (!sequencesValid) {
                        Text(
                            text = "❌ Validation Error: Ensure positive duration and thresholds keep sequence: Green <= Yellow <= Red <= Max",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            val g = (greenMin.toIntOrNull() ?: 0) * 60 + (greenSec.toIntOrNull() ?: 0)
            val y = (yellowMin.toIntOrNull() ?: 0) * 60 + (yellowSec.toIntOrNull() ?: 0)
            val r = (redMin.toIntOrNull() ?: 0) * 60 + (redSec.toIntOrNull() ?: 0)
            val m = (maxMin.toIntOrNull() ?: 0) * 60 + (maxSec.toIntOrNull() ?: 0)
            val sequencesValid = g > 0 && y >= g && r >= y && m >= r

            Button(
                onClick = onApply,
                enabled = sequencesValid,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("apply_custom_button")
            ) {
                Text("Apply Profile")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimeThresholdRow(
    label: String,
    minVal: String,
    secVal: String,
    onMinChange: (String) -> Unit,
    onSecChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.width(96.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = minVal,
            onValueChange = { onMinChange(it.take(2)) },
            label = { Text("m") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
        )

        OutlinedTextField(
            value = secVal,
            onValueChange = { onSecChange(it.take(2)) },
            label = { Text("s") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
