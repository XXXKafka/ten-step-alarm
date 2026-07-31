package com.tenstep.alarm.alarm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StepMode { STEP_COUNTER, ACCELEROMETER, UNAVAILABLE }

/**
 * Tracks steps while the alarm is ringing.
 *
 * Prefers the hardware step counter (TYPE_STEP_COUNTER); when the sensor is
 * missing or ACTIVITY_RECOGNITION was denied it falls back to accelerometer
 * estimation. If no sensor at all is present the mode is [StepMode.UNAVAILABLE].
 */
class StepGate(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _mode = MutableStateFlow(StepMode.UNAVAILABLE)
    private val _steps = MutableStateFlow(0)

    val mode: StateFlow<StepMode> = _mode.asStateFlow()
    val steps: StateFlow<Int> = _steps.asStateFlow()

    private var listener: SensorEventListener? = null
    private var stepCounterBaseline: Long = -1L
    private var detectorState = StepDetector.State()

    fun start() {
        stop()
        _steps.value = 0
        _mode.value = StepMode.UNAVAILABLE

        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val recognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (stepCounter != null && recognitionGranted) {
            _mode.value = StepMode.STEP_COUNTER
            stepCounterBaseline = -1L
            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val total = event.values[0].toLong()
                    if (stepCounterBaseline < 0L) stepCounterBaseline = total
                    _steps.value = (total - stepCounterBaseline).coerceAtLeast(0L).toInt()
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, stepCounter, SensorManager.SENSOR_DELAY_UI)
        } else {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometer != null) {
                _mode.value = StepMode.ACCELEROMETER
                detectorState = StepDetector.State()
                listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val magnitude = StepDetector.magnitude(
                            event.values[0], event.values[1], event.values[2]
                        )
                        _steps.value = StepDetector.process(
                            magnitude, SystemClock.elapsedRealtime(), detectorState
                        )
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            // else: StepMode.UNAVAILABLE stays set.
        }
    }

    /** Debug/test helper: adds simulated steps on top of the real count. */
    fun simulateSteps(extra: Int) {
        _steps.value += extra
    }

    fun stop() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
        stepCounterBaseline = -1L
    }
}