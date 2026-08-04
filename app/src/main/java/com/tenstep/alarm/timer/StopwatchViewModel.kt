package com.tenstep.alarm.timer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Simple stopwatch with laps (in-app only; no background requirement). */
class StopwatchViewModel(application: Application) : AndroidViewModel(application) {

    val elapsedMs = MutableStateFlow(0L)
    val running = MutableStateFlow(false)
    val laps = MutableStateFlow<List<Long>>(emptyList())

    private var ticker: Job? = null
    private var startElapsedMs = 0L
    private var accumulatedMs = 0L

    fun start() {
        if (running.value) return
        startElapsedMs = SystemClock.elapsedRealtime()
        running.value = true
        ticker = viewModelScope.launch {
            while (isActive) {
                elapsedMs.value = StopwatchEngine.elapsedMs(
                    accumulatedMs, startElapsedMs, SystemClock.elapsedRealtime()
                )
                delay(100)
            }
        }
    }

    fun pause() {
        ticker?.cancel()
        ticker = null
        if (running.value) {
            accumulatedMs = StopwatchEngine.elapsedMs(
                accumulatedMs, startElapsedMs, SystemClock.elapsedRealtime()
            )
            elapsedMs.value = accumulatedMs
            running.value = false
        }
    }

    fun reset() {
        pause()
        accumulatedMs = 0L
        elapsedMs.value = 0L
        laps.value = emptyList()
    }

    fun lap() {
        if (running.value) {
            laps.value = laps.value + elapsedMs.value
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}