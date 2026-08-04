package com.tenstep.alarm.ui.alarm

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.AlarmEntity
import com.tenstep.alarm.data.ChallengeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val alarmId: Long = savedStateHandle.get<Long>("alarmId") ?: 0L
    private val repository = (application as TenStepApplication).container.alarmRepository
    private val settings = (application as TenStepApplication).container.settingsStore

    val isNew = MutableStateFlow(alarmId == 0L)
    val loaded = MutableStateFlow(false)

    val hour = MutableStateFlow(7)
    val minute = MutableStateFlow(0)
    val days = MutableStateFlow(0)
    val label = MutableStateFlow("")
    val ringtoneUri = MutableStateFlow("")
    val volume = MutableStateFlow(70)
    val vibrate = MutableStateFlow(true)
    val snoozeEnabled = MutableStateFlow(true)
    val challengeType = MutableStateFlow(ChallengeType.STEPS)
    val stepTarget = MutableStateFlow(ChallengeType.DEFAULT_STEP_TARGET)

    /** Follows the global "24-hour format" setting (shared with the clock). */
    val is24Hour: StateFlow<Boolean> = settings.clock24Hour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        viewModelScope.launch {
            if (alarmId > 0L) {
                repository.getAlarm(alarmId)?.let { alarm ->
                    hour.value = alarm.hour
                    minute.value = alarm.minute
                    days.value = alarm.daysOfWeek
                    label.value = alarm.label
                    ringtoneUri.value = alarm.ringtoneUri
                    volume.value = alarm.volume
                    vibrate.value = alarm.vibrate
                    snoozeEnabled.value = alarm.snoozeEnabled
                    challengeType.value = alarm.challengeType
                    stepTarget.value = alarm.stepTarget
                }
            } else {
                ringtoneUri.value = settings.defaultRingtone.first()
            }
            loaded.value = true
        }
    }

    fun setHour(value: Int) { hour.value = value.coerceIn(0, 23) }
    fun setMinute(value: Int) { minute.value = value.coerceIn(0, 59) }

    fun toggleDay(bit: Int) {
        days.value = days.value xor bit
    }

    fun setLabel(value: String) { label.value = value }
    fun setRingtone(uri: String) { ringtoneUri.value = uri }
    fun setVolume(value: Int) { volume.value = value.coerceIn(0, 100) }
    fun setVibrate(value: Boolean) { vibrate.value = value }
    fun setSnoozeEnabled(value: Boolean) { snoozeEnabled.value = value }
    fun setChallengeType(value: ChallengeType) { challengeType.value = value }
    fun setStepTarget(value: Int) {
        stepTarget.value = value.coerceIn(1, 100)
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val uri = ringtoneUri.value.ifBlank { settings.defaultRingtone.first() }
            val alarm = AlarmEntity(
                id = if (alarmId == 0L) 0L else alarmId,
                hour = hour.value,
                minute = minute.value,
                daysOfWeek = days.value,
                label = label.value.trim(),
                ringtoneUri = uri,
                volume = volume.value,
                vibrate = vibrate.value,
                snoozeEnabled = snoozeEnabled.value,
                enabled = true,
                oneShot = days.value == 0,
                challengeType = challengeType.value,
                stepTarget = if (challengeType.value == ChallengeType.STEPS) {
                    stepTarget.value
                } else {
                    ChallengeType.DEFAULT_STEP_TARGET
                }
            )
            repository.upsert(alarm)
            if (alarmId == 0L) {
                // First alarm creation: ask once for the battery-optimization
                // exemption (helps exact alarms survive Doze/OEM battery savers).
                promptBatteryOptimizationIfNeeded()
            }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.getAlarm(alarmId)?.let { repository.delete(it) }
            onDeleted()
        }
    }

    private fun promptBatteryOptimizationIfNeeded() {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(app.packageName)) return
        runCatching {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${app.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        }
    }
}