package com.tenstep.alarm.ui.clock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = (application as TenStepApplication).container.settingsStore

    val showSeconds: StateFlow<Boolean> = settings.clockShowSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val showDate: StateFlow<Boolean> = settings.clockShowDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val is24Hour: StateFlow<Boolean> = settings.clock24Hour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val style: StateFlow<String> = settings.clockStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "auto")

    val customColorArgb: StateFlow<Long> = settings.clockCustomColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_ACCENT_ARGB)

    val fontScale: StateFlow<Float> = settings.clockFontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)

    val fullscreen: StateFlow<Boolean> = settings.clockFullscreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setShowSeconds(enabled: Boolean) {
        viewModelScope.launch { settings.setClockShowSeconds(enabled) }
    }

    fun setShowDate(enabled: Boolean) {
        viewModelScope.launch { settings.setClockShowDate(enabled) }
    }

    fun set24Hour(enabled: Boolean) {
        viewModelScope.launch { settings.setClock24Hour(enabled) }
    }

    fun setStyle(style: String) {
        viewModelScope.launch { settings.setClockStyle(style) }
    }

    fun setCustomColor(argb: Long) {
        viewModelScope.launch { settings.setClockCustomColor(argb) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settings.setClockFontScale(scale) }
    }

    fun setFullscreen(enabled: Boolean) {
        viewModelScope.launch { settings.setClockFullscreen(enabled) }
    }
}