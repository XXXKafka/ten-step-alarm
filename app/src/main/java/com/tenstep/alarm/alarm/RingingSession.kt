package com.tenstep.alarm.alarm

import com.tenstep.alarm.data.AlarmEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Global state of the currently ringing alarm, shared by service and UI. */
object RingingSession {

    /** Number of steps required before the alarm can be dismissed. */
    const val STEPS_TARGET = 10

    private val _active = MutableStateFlow(false)
    private val _alarm = MutableStateFlow<AlarmEntity?>(null)
    private val _isSnooze = MutableStateFlow(false)
    private val _steps = MutableStateFlow(0)
    private val _stepMode = MutableStateFlow(StepMode.UNAVAILABLE)

    val active: StateFlow<Boolean> = _active.asStateFlow()
    val alarm: StateFlow<AlarmEntity?> = _alarm.asStateFlow()
    val isSnooze: StateFlow<Boolean> = _isSnooze.asStateFlow()
    val steps: StateFlow<Int> = _steps.asStateFlow()
    val stepMode: StateFlow<StepMode> = _stepMode.asStateFlow()

    /** Active [StepGate] owned by the ringing service (used by the debug simulator). */
    @Volatile
    var activeGate: StepGate? = null

    fun start(alarm: AlarmEntity, snooze: Boolean) {
        _alarm.value = alarm
        _isSnooze.value = snooze
        _steps.value = 0
        _stepMode.value = StepMode.UNAVAILABLE
        activeGate = null
        _active.value = true
    }

    fun updateSteps(steps: Int) {
        _steps.value = steps
    }

    fun updateStepMode(mode: StepMode) {
        _stepMode.value = mode
    }

    fun stop() {
        _active.value = false
        _alarm.value = null
        _isSnooze.value = false
        _steps.value = 0
        _stepMode.value = StepMode.UNAVAILABLE
        activeGate = null
    }
}