package com.tenstep.alarm.alarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks shakes while the alarm is ringing (SHAKE challenge) using the
 * accelerometer and the pure [ShakeDetector] logic.
 */
class ShakeGate(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _shakes = MutableStateFlow(0)
    private val _mode = MutableStateFlow(StepMode.UNAVAILABLE)

    val shakes: StateFlow<Int> = _shakes.asStateFlow()
    val mode: StateFlow<StepMode> = _mode.asStateFlow()

    private var listener: SensorEventListener? = null
    private var detectorState = ShakeDetector.State()

    fun start() {
        stop()
        _shakes.value = 0
        _mode.value = StepMode.UNAVAILABLE

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            _mode.value = StepMode.ACCELEROMETER
            detectorState = ShakeDetector.State()
            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val magnitude = StepDetector.magnitude(
                        event.values[0], event.values[1], event.values[2]
                    )
                    _shakes.value = ShakeDetector.process(
                        magnitude, SystemClock.elapsedRealtime(), detectorState
                    )
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /** Debug/test helper: adds simulated shakes on top of the real count. */
    fun simulateShakes(extra: Int) {
        _shakes.value += extra
    }

    fun stop() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
    }
}