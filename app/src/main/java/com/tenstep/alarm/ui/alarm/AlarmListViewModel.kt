package com.tenstep.alarm.ui.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.AlarmEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as TenStepApplication).container.alarmRepository

    val alarms: StateFlow<List<AlarmEntity>> = repository.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(alarm: AlarmEntity) {
        viewModelScope.launch { repository.setEnabled(alarm, !alarm.enabled) }
    }

    fun delete(alarm: AlarmEntity) {
        viewModelScope.launch { repository.delete(alarm) }
    }
}