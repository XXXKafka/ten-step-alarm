package com.tenstep.alarm.ui.ringing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.alarm.AlarmRingingService
import com.tenstep.alarm.alarm.ChallengeEvaluator
import com.tenstep.alarm.alarm.MathChallenge
import com.tenstep.alarm.alarm.RingingSession
import com.tenstep.alarm.data.ChallengeType
import kotlinx.coroutines.flow.MutableStateFlow
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
    val stepTarget = RingingSession.stepTarget
    val shakes = RingingSession.shakes
    val shakeTarget = RingingSession.shakeTarget

    val snoozeMinutes: StateFlow<Int> = container.settingsStore.snoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    val debugSimulateSteps: StateFlow<Boolean> = container.settingsStore.debugSimulateSteps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val mathChallenge = MathChallenge()

    val mathQuestion = MutableStateFlow<MathChallenge.Problem?>(null)
    val mathSolved = MutableStateFlow(false)
    val qrScanned = MutableStateFlow(false)

    init {
        // Generate the math problem once a MATH alarm starts ringing.
        viewModelScope.launch {
            RingingSession.alarm.collect { alarm ->
                if (alarm?.challengeType == ChallengeType.MATH && mathQuestion.value == null) {
                    mathQuestion.value = mathChallenge.nextProblem()
                }
            }
        }
    }

    /** Check a math answer; returns true when it just became solved. */
    fun onMathAnswer(input: Int): Boolean {
        val question = mathQuestion.value ?: return false
        if (!question.check(input)) return false
        mathSolved.value = true
        return true
    }

    /** Marks the QR challenge as scanned (any barcode counts). */
    fun onQrScanned() {
        qrScanned.value = true
    }

    /** True when the current alarm's challenge has been satisfied. */
    fun challengeSatisfied(): Boolean {
        val type = RingingSession.alarm.value?.challengeType ?: return false
        return ChallengeEvaluator.isSatisfied(
            type = type,
            steps = steps.value,
            stepTarget = stepTarget.value,
            shakes = shakes.value,
            shakeTarget = shakeTarget.value,
            mathSolved = mathSolved.value,
            qrScanned = qrScanned.value
        )
    }

    /** Snooze the current alarm and stop ringing. */
    fun snooze() {
        viewModelScope.launch {
            val alarm = RingingSession.alarm.value ?: return@launch
            if (alarm.snoozeEnabled) {
                val minutes = container.settingsStore.snoozeMinutes.first()
                container.scheduler.scheduleSnooze(alarm.id, minutes)
            }
            AlarmRingingService.stop(appContext)
        }
    }

    fun dismiss() {
        AlarmRingingService.stop(appContext)
    }

    /** Debug/test helper: adds simulated progress to the active challenge. */
    fun simulateSteps(n: Int = 10) {
        val shakeGate = RingingSession.activeShakeGate
        if (shakeGate != null) {
            shakeGate.simulateShakes(n)
            return
        }
        val gate = RingingSession.activeGate
        if (gate != null) {
            gate.simulateSteps(n)
        } else {
            RingingSession.updateSteps(RingingSession.steps.value + n)
        }
    }
}