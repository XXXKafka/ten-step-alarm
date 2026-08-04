package com.tenstep.alarm.timer

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Countdown timer. While running it also schedules one exact alarm so the
 * end notification (with sound) fires even if the app is killed; the in-app
 * ticker only shows the completion dialog when the app is still alive.
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

    val minutes = MutableStateFlow(5)
    val totalMs = MutableStateFlow(TimerEngine.durationMs(5))
    val remainingMs = MutableStateFlow(TimerEngine.durationMs(5))
    val running = MutableStateFlow(false)
    val completionTick = MutableStateFlow(0)

    private var ticker: Job? = null
    private var startElapsedMs = 0L

    fun setMinutes(value: Int) {
        if (running.value) return
        minutes.value = value.coerceIn(1, 180)
        totalMs.value = TimerEngine.durationMs(minutes.value)
        remainingMs.value = totalMs.value
    }

    fun start() {
        if (running.value) return
        if (remainingMs.value <= 0L) {
            remainingMs.value = totalMs.value
        }
        startElapsedMs = SystemClock.elapsedRealtime()
        running.value = true
        scheduleExact()
        ticker = viewModelScope.launch {
            while (isActive) {
                remainingMs.value = TimerEngine.remainingMs(
                    totalMs.value, startElapsedMs, SystemClock.elapsedRealtime()
                )
                if (remainingMs.value <= 0L) {
                    onComplete()
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
            remainingMs.value = TimerEngine.remainingMs(
                totalMs.value, startElapsedMs, SystemClock.elapsedRealtime()
            )
            running.value = false
            cancelExact()
        }
    }

    fun reset() {
        pause()
        remainingMs.value = totalMs.value
    }

    fun acknowledgeCompletion() {
        completionTick.value = 0
    }

    private fun onComplete() {
        ticker?.cancel()
        ticker = null
        running.value = false
        remainingMs.value = 0L
        // The exact alarm (the sound notification) has already fired by now.
        completionTick.update { it + 1 }
    }

    private fun scheduleExact() {
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + remainingMs.value
        runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                timerPendingIntent()
            )
        }
    }

    private fun cancelExact() {
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching { alarmManager.cancel(timerPendingIntent()) }
    }

    private fun timerPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            app,
            TimerReceiver.REQUEST_CODE,
            Intent(app, TimerReceiver::class.java).setAction(TimerReceiver.ACTION_TIMER_FINISHED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}