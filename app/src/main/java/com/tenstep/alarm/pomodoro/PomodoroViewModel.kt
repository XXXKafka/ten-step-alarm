package com.tenstep.alarm.pomodoro

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tenstep.alarm.R
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.util.Notifications
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PomodoroMode { FOCUS, BREAK }

/** Pure pomodoro math (unit-testable). */
object PomodoroEngine {
    fun durationMs(minutes: Int): Long = minutes.coerceAtLeast(1) * 60_000L

    fun remainingMs(totalMs: Long, startElapsedMs: Long, nowElapsedMs: Long): Long =
        (totalMs - (nowElapsedMs - startElapsedMs)).coerceAtLeast(0L)

    fun nextMode(mode: PomodoroMode): PomodoroMode =
        if (mode == PomodoroMode.FOCUS) PomodoroMode.BREAK else PomodoroMode.FOCUS
}

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TenStepApplication).container
    private val context = application

    private var focusMinutes = 25
    private var breakMinutes = 5
    private var focusRingtone: String = ""
    private var breakRingtone: String = ""
    private var ticker: Job? = null
    private var startElapsedMs = 0L

    val mode = MutableStateFlow(PomodoroMode.FOCUS)
    val totalMs = MutableStateFlow(PomodoroEngine.durationMs(25))
    val remainingMs = MutableStateFlow(PomodoroEngine.durationMs(25))
    val running = MutableStateFlow(false)
    val sessionsCompleted = MutableStateFlow(0)

    /** Incremented every time a phase completes; drives the end prompt page. */
    val completionTick = MutableStateFlow(0)

    /** The mode that just finished (for the end prompt page title). */
    val lastFinishedMode = MutableStateFlow<PomodoroMode?>(null)


    init {
        viewModelScope.launch {
            combine(
                container.settingsStore.focusMinutes,
                container.settingsStore.breakMinutes
            ) { focus, brk -> focus to brk }
                .collect { (focus, brk) ->
                    focusMinutes = focus
                    breakMinutes = brk
                    if (!running.value) {
                        totalMs.value = durationFor(mode.value)
                        remainingMs.value = totalMs.value
                    }
                }
        }
        viewModelScope.launch {
            container.settingsStore.pomodoroFocusRingtone.collect { focusRingtone = it }
        }
        viewModelScope.launch {
            container.settingsStore.pomodoroBreakRingtone.collect { breakRingtone = it }
        }
    }

    private fun durationFor(mode: PomodoroMode): Long = PomodoroEngine.durationMs(
        if (mode == PomodoroMode.FOCUS) focusMinutes else breakMinutes
    )

    fun start() {
        if (running.value) return
        if (remainingMs.value <= 0L) remainingMs.value = totalMs.value
        startElapsedMs = SystemClock.elapsedRealtime()
        running.value = true
        ticker = viewModelScope.launch {
            while (isActive) {
                remainingMs.value = PomodoroEngine.remainingMs(
                    totalMs.value, startElapsedMs, SystemClock.elapsedRealtime()
                )
                if (remainingMs.value <= 0L) {
                    onPhaseComplete()
                    break
                }
                delay(250)
            }
        }
    }

    fun pause() {
        ticker?.cancel()
        ticker = null
        if (running.value) {
            remainingMs.value = PomodoroEngine.remainingMs(
                totalMs.value, startElapsedMs, SystemClock.elapsedRealtime()
            )
            running.value = false
        }
    }

    fun reset() {
        pause()
        remainingMs.value = totalMs.value
    }

    fun skipToNextPhase() {
        onPhaseComplete()
    }

    /** Dismisses the end prompt page. */
    fun acknowledgeCompletion() {
        completionTick.value = 0
        lastFinishedMode.value = null
    }

    private fun onPhaseComplete() {
        val finishedMode = mode.value
        pause()
        if (finishedMode == PomodoroMode.FOCUS) {
            sessionsCompleted.update { it + 1 }
        }
        mode.value = PomodoroEngine.nextMode(finishedMode)
        totalMs.value = durationFor(mode.value)
        remainingMs.value = totalMs.value
        playRingtone(finishedMode)
        notifyPhaseEnded(finishedMode)
        lastFinishedMode.value = finishedMode
        completionTick.update { it + 1 }
    }

    private fun playRingtone(finishedMode: PomodoroMode) {
        val uri = if (finishedMode == PomodoroMode.FOCUS) focusRingtone else breakRingtone
        runCatching {
            val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uri))
                ?: RingtoneManager.getRingtone(
                    context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                )
            ringtone?.play()
        }
    }

    private fun notifyPhaseEnded(finishedMode: PomodoroMode) {
        val text = context.getString(
            if (finishedMode == PomodoroMode.FOCUS) {
                R.string.pomodoro_done_focus
            } else {
                R.string.pomodoro_done_break
            }
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        manager.notify(POMODORO_NOTIFICATION_ID, Notifications.pomodoroNotification(context, text))
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }

    companion object {
        const val POMODORO_NOTIFICATION_ID = 2001
    }
}