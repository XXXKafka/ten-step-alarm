package com.tenstep.alarm.alarm

import com.tenstep.alarm.data.AlarmEntity
import com.tenstep.alarm.data.ChallengeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Global state of the currently ringing alarm, shared by service and UI. */
object RingingSession {

    /** Default number of shakes for a SHAKE challenge. */
    const val DEFAULT_SHAKE_TARGET = 20

    private val _active = MutableStateFlow(false)
    private val _alarm = MutableStateFlow<AlarmEntity?>(null)
    private val _isSnooze = MutableStateFlow(false)
    private val _steps = MutableStateFlow(0)
    private val _stepMode = MutableStateFlow(StepMode.UNAVAILABLE)
    private val _stepTarget = MutableStateFlow(ChallengeType.DEFAULT_STEP_TARGET)
    private val _shakes = MutableStateFlow(0)
    private val _shakeTarget = MutableStateFlow(DEFAULT_SHAKE_TARGET)

    val active: StateFlow<Boolean> = _active.asStateFlow()
    val alarm: StateFlow<AlarmEntity?> = _alarm.asStateFlow()
    val isSnooze: StateFlow<Boolean> = _isSnooze.asStateFlow()
    val steps: StateFlow<Int> = _steps.asStateFlow()
    val stepMode: StateFlow<StepMode> = _stepMode.asStateFlow()
    val stepTarget: StateFlow<Int> = _stepTarget.asStateFlow()
    val shakes: StateFlow<Int> = _shakes.asStateFlow()
    val shakeTarget: StateFlow<Int> = _shakeTarget.asStateFlow()

    /** Active [StepGate] owned by the ringing service (used by the debug simulator). */
    @Volatile
    var activeGate: StepGate? = null

    /** Active [ShakeGate] owned by the ringing service (used by the debug simulator). */
    @Volatile
    var activeShakeGate: ShakeGate? = null

    fun start(alarm: AlarmEntity, snooze: Boolean) {
        _alarm.value = alarm
        _isSnooze.value = snooze
        _steps.value = 0
        _stepMode.value = StepMode.UNAVAILABLE
        _stepTarget.value = alarm.stepTarget.coerceAtLeast(1)
        _shakes.value = 0
        _shakeTarget.value = DEFAULT_SHAKE_TARGET
        activeGate = null
        activeShakeGate = null
        _active.value = true
    }

    fun updateSteps(steps: Int) {
        _steps.value = steps
    }

    fun updateStepMode(mode: StepMode) {
        _stepMode.value = mode
    }

    fun updateShakes(shakes: Int) {
        _shakes.value = shakes
    }

    fun stop() {
        _active.value = false
        _alarm.value = null
        _isSnooze.value = false
        _steps.value = 0
        _stepMode.value = StepMode.UNAVAILABLE
        _stepTarget.value = ChallengeType.DEFAULT_STEP_TARGET
        _shakes.value = 0
        _shakeTarget.value = DEFAULT_SHAKE_TARGET
        activeGate = null
        activeShakeGate = null
    }
}