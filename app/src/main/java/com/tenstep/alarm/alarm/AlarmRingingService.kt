package com.tenstep.alarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.AlarmEntity
import com.tenstep.alarm.util.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the alarm ringing (sound + vibration + step
 * counting) until it is snoozed or dismissed, independent of the Activity.
 */
class AlarmRingingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var stepGate: StepGate? = null
    private var stateJob: Job? = null
    private var activeAlarmId: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRinging()
            return START_NOT_STICKY
        }
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        val snooze = intent?.getBooleanExtra(AlarmReceiver.EXTRA_SNOOZE, false) ?: false
        if (alarmId < 0L) {
            stopSelf()
            return START_NOT_STICKY
        }

        val app = application as TenStepApplication
        scope.launch {
            val alarm = app.container.alarmRepository.getAlarm(alarmId)
            if (alarm == null) {
                stopSelf()
                return@launch
            }
            startRinging(alarm, snooze)
        }
        return START_NOT_STICKY
    }

    private fun startRinging(alarm: AlarmEntity, snooze: Boolean) {
        if (activeAlarmId == alarm.id) return

        stopInternal()
        activeAlarmId = alarm.id
        RingingSession.start(alarm, snooze)
        startForegroundCompat(alarm, snooze)

        startSound(alarm)
        startVibration(alarm)
        acquireWakeLock()

        // Second chance to bring the ringing page to the front. Some OEMs
        // (e.g. Xiaomi MIUI/HyperOS) block the receiver-side start; starting
        // from the foreground service context works more reliably there.
        runCatching {
            val activity = Intent(this, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(activity)
        }

        val gate = StepGate(this)
        stepGate = gate
        RingingSession.activeGate = gate
        gate.start()
        stateJob = scope.launch {
            launch { gate.steps.collect { RingingSession.updateSteps(it) } }
            launch { gate.mode.collect { RingingSession.updateStepMode(it) } }
        }
    }

    private fun startForegroundCompat(alarm: AlarmEntity, snooze: Boolean) {
        val notification = Notifications.alarmNotification(this, alarm, snooze)
        ServiceCompat.startForeground(
            this,
            Notifications.ALARM_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        // Re-assert the full-screen intent (some OEMs drop the first post),
        // so the ringing page reliably pops up from the background.
        runCatching {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
            manager.notify(Notifications.ALARM_NOTIFICATION_ID, notification)
        }
    }

    private fun startSound(alarm: AlarmEntity) {
        try {
            val player = MediaPlayer().apply {
                // USAGE_ALARM + setAlarmClock (see AlarmScheduler) let the alarm
                // ring during Do Not Disturb when "alarms & reminders" are
                // allowed; the notification channel also calls setBypassDnd().
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                val volume = alarm.volume.coerceIn(0, 100) / 100f
                setVolume(volume, volume)
                setDataSource(applicationContext, resolveRingtoneUri(alarm.ringtoneUri))
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (_: Exception) {
            // No playable ringtone: vibration still runs.
            mediaPlayer = null
        }
    }

    private fun resolveRingtoneUri(raw: String): Uri {
        if (raw.isNotBlank()) {
            runCatching { return Uri.parse(raw) }
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    private fun startVibration(alarm: AlarmEntity) {
        if (!alarm.vibrate) return
        val v = vibrator()
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
        }
    }

    private fun vibrator(): Vibrator? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            return manager?.defaultVibrator
        }
        @Suppress("DEPRECATION")
        return getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TenStepAlarm:Ringing"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }
    }

    /** Public stop entry point used by the UI. */
    fun stopRinging() {
        stopInternal()
        activeAlarmId = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopInternal() {
        stateJob?.cancel()
        stateJob = null
        stepGate?.stop()
        stepGate = null
        RingingSession.activeGate = null
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
        vibrator()?.cancel()
        wakeLock?.let { runCatching { it.release() } }
        wakeLock = null
        RingingSession.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        stopInternal()
    }

    companion object {
        const val ACTION_STOP = "com.tenstep.alarm.action.STOP_RINGING"

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }
    }
}