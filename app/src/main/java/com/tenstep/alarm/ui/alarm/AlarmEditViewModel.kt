package com.tenstep.alarm.ui.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.AlarmEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
                enabled = true,
                oneShot = days.value == 0
            )
            repository.upsert(alarm)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.getAlarm(alarmId)?.let { repository.delete(it) }
            onDeleted()
        }
    }
}