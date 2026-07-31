package com.tenstep.alarm.ui.ringing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.alarm.AlarmRingingService
import com.tenstep.alarm.alarm.RingingSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RingingViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TenStepApplication).container
    private val appContext = application

    val active = RingingSession.active
    val alarm = RingingSession.alarm
    val isSnooze = RingingSession.isSnooze
    val steps = RingingSession.steps
    val stepMode = RingingSession.stepMode

    val snoozeMinutes: StateFlow<Int> = container.settingsStore.snoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    val debugSimulateSteps: StateFlow<Boolean> = container.settingsStore.debugSimulateSteps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Snooze the current alarm and stop ringing. */
    fun snooze() {
        viewModelScope.launch {
            val alarm = RingingSession.alarm.value ?: return@launch
            val minutes = container.settingsStore.snoozeMinutes.first()
            container.scheduler.scheduleSnooze(alarm.id, minutes)
            AlarmRingingService.stop(appContext)
        }
    }

    fun dismiss() {
        AlarmRingingService.stop(appContext)
    }

    /** Debug/test helper: adds simulated steps (see settings debug switch). */
    fun simulateSteps(n: Int = 10) {
        val gate = RingingSession.activeGate
        if (gate != null) {
            gate.simulateSteps(n)
        } else {
            RingingSession.updateSteps(RingingSession.steps.value + n)
        }
    }
}