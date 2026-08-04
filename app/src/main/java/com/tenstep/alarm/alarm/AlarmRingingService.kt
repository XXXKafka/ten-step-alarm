package com.tenstep.alarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ServiceCompat
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.AlarmEntity
import com.tenstep.alarm.data.ChallengeType
import com.tenstep.alarm.util.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the alarm ringing (sound + vibration + the
 * configured challenge) until it is snoozed or dismissed, independent of the
 * Activity.
 */
class AlarmRingingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var rampJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var stepGate: StepGate? = null
    private var shakeGate: ShakeGate? = null
    private var stateJob: Job? = null
    private var reassertJob: Job? = null
    private var activeAlarmId: Long? = null
    private var activeAlarm: AlarmEntity? = null
    private var activeSnooze = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRinging()
            return START_NOT_STICKY
        }
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        val snooze = intent?.getBooleanExtra(AlarmReceiver.EXTRA_SNOOZE, false) ?: false
        Log.d(TAG, "onStartCommand action=${intent?.action} alarmId=$alarmId snooze=$snooze")
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

        // A different alarm is already ringing: never replace it (the user
        // would have to redo the challenge). Ignore this trigger.
        if (RingingSession.active.value) {
            Log.w(TAG, "Ignoring alarm ${alarm.id}: another alarm is already ringing")
            return
        }

        stopInternal()
        activeAlarmId = alarm.id
        activeAlarm = alarm
        activeSnooze = snooze
        RingingSession.start(alarm, snooze)
        startForegroundCompat(alarm, snooze)
        startFullscreenReassert()

        startSound(alarm)
        startVibration(alarm)
        acquireWakeLock()

        // Second chance to bring the ringing page to the front. Some OEMs
        // (e.g. Xiaomi MIUI/HyperOS) block the receiver-side start; starting
        // from the foreground service context works more reliably there.
        runCatching {
            val activity = Intent(this, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            startActivity(activity)
        }

        when (alarm.challengeType) {
            ChallengeType.SHAKE -> startShake()
            // QR falls back to step counting when the camera cannot be used.
            ChallengeType.STEPS, ChallengeType.QR -> startSteps()
            ChallengeType.MATH -> Unit // no sensor needed; answer is checked in the UI
        }
    }

    private fun startSteps() {
        val gate = StepGate(this)
        stepGate = gate
        RingingSession.activeGate = gate
        gate.start()
        stateJob = scope.launch {
            launch { gate.steps.collect { RingingSession.updateSteps(it) } }
            launch { gate.mode.collect { RingingSession.updateStepMode(it) } }
        }
    }

    private fun startShake() {
        val gate = ShakeGate(this)
        shakeGate = gate
        RingingSession.activeShakeGate = gate
        gate.start()
        stateJob = scope.launch {
            launch { gate.shakes.collect { RingingSession.updateShakes(it) } }
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

    private var ringtoneFellBack = false

    private fun startSound(alarm: AlarmEntity) {
        Log.d(TAG, "startSound uri=${alarm.ringtoneUri} vol=${alarm.volume}")
        requestAudioFocus()
        ringtoneFellBack = false
        mediaPlayer = createPlayer(
            uri = resolveRingtoneUri(alarm.ringtoneUri),
            volume = alarm.volume,
            onPlaybackError = { failed ->
                // A stale/stored ringtone URI (e.g. invalidated by a media
                // rescan) fails to prepare -> retry once with the default sound.
                if (!ringtoneFellBack) {
                    ringtoneFellBack = true
                    Log.w(TAG, "Ringtone failed to prepare; falling back to default")
                    runCatching { failed.release() }
                    mediaPlayer = createPlayer(
                        uri = resolveRingtoneUri(""),
                        volume = alarm.volume,
                        onPlaybackError = null
                    )
                }
            }
        )
    }

    private fun createPlayer(
        uri: Uri,
        volume: Int,
        onPlaybackError: ((MediaPlayer) -> Unit)?
    ): MediaPlayer? = try {
        val target = volume.coerceIn(0, 100) / 100f
        MediaPlayer().apply {
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
            // Start silent and ramp up over ~5 s so the alarm is less jarring.
            setVolume(0f, 0f)
            setDataSource(applicationContext, uri)
            setOnPreparedListener {
                Log.d(TAG, "onPrepared -> start")
                it.start()
                startVolumeRamp(it, target)
            }
            setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "Ringtone playback error: what=$what extra=$extra")
                onPlaybackError?.invoke(mp)
                true
            }
            // prepare() can block the main thread for slow ringtones; use
            // the async path so the ringing page appears without jank.
            prepareAsync()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Ringtone start failed", e)
        // No playable ringtone: vibration still runs.
        null
    }

    /**
     * Alarms use a transient audio focus so they are not ducked or blocked by
     * another app that is currently playing audio.
     */
    private fun requestAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .build()
        if (audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioFocusRequest = request
        }
    }

    private fun abandonAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
        audioFocusRequest = null
    }

    private fun startVolumeRamp(player: MediaPlayer, target: Float) {
        rampJob?.cancel()
        rampJob = scope.launch {
            val steps = 50
            val stepDelayMs = 100L // 5 s total
            for (i in 1..steps) {
                val volume = target * i / steps
                runCatching { player.setVolume(volume, volume) }
                delay(stepDelayMs)
            }
        }
    }

    private fun resolveRingtoneUri(raw: String): Uri {
        if (raw.isNotBlank()) {
            runCatching {
                val parsed = Uri.parse(raw)
                val resolved = resolveSettingsUri(parsed) ?: parsed
                if (canOpen(resolved)) return resolved
            }
        }
        // Mirror the system deskclock: first the *actual* configured default
        // (on MIUI/HyperOS this already points at the real media item), then
        // the generic alarm -> notification -> ringtone defaults.
        runCatching {
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?.let { resolveSettingsUri(it) ?: it }
                ?.let { if (canOpen(it)) return it }
        }
        listOf(
            RingtoneManager.TYPE_ALARM,
            RingtoneManager.TYPE_NOTIFICATION,
            RingtoneManager.TYPE_RINGTONE
        ).forEach { type ->
            runCatching {
                val uri = RingtoneManager.getDefaultUri(type) ?: return@runCatching
                val resolved = resolveSettingsUri(uri) ?: uri
                if (canOpen(resolved)) return resolved
            }
        }
        // Last resort: the bundled ringtone. This is what guarantees the alarm
        // actually makes a sound on devices whose settings/media URIs cannot be
        // opened by a third-party app (e.g. HyperOS "XiaoAI smart ringtones").
        Log.w(TAG, "All ringtone URIs unplayable; using bundled fallback")
        return bundledRingtoneUri()
    }

    /**
     * Returns true when the URI is a bundled raw resource (always playable) or
     * when a probe confirms MediaPlayer can open the data source.
     */
    private fun canOpen(uri: Uri): Boolean {
        if (uri.scheme == "android.resource") return true
        return runCatching {
            val probe = MediaPlayer()
            try {
                probe.setDataSource(applicationContext, uri)
                true
            } finally {
                probe.release()
            }
        }.getOrDefault(false)
    }

    private fun bundledRingtoneUri(): Uri {
        return Uri.Builder()
            .scheme("android.resource")
            .authority(packageName)
            .appendPath("raw")
            .appendPath("alarm_fallback")
            .build()
    }

    /**
     * MIUI/HyperOS stores the default sound as content://settings/system/<name>,
     * which MediaPlayer cannot open directly. Resolve it to the real media URI
     * the settings provider points at (e.g. content://media/external/audio/...),
     * otherwise the alarm would ring silently on Xiaomi devices.
     */
    /** Strips the user prefix (content://0@media -> content://media) and any
     *  query params from a resolved media URI so MediaPlayer can open it. */
    private fun normalizeMediaUri(uri: Uri): Uri {
        val authority = uri.authority?.substringAfterLast('@')
        if (authority.isNullOrBlank()) return uri
        return Uri.Builder()
            .scheme(uri.scheme)
            .authority(authority)
            .path(uri.path)
            .build()
    }

    private fun resolveSettingsUri(uri: Uri): Uri? {
        if (uri.authority != "settings") return null
        val name = uri.lastPathSegment ?: return null
        return runCatching {
            contentResolver.query(
                uri,
                arrayOf(Settings.NameValueTable.VALUE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.let { resolved ->
                        normalizeMediaUri(Uri.parse(resolved))
                    }
                } else {
                    null
                }
            }
        }.getOrNull()
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

    /**
     * While ringing, re-posts the full-screen-intent notification every 30 s.
     * If the ringing page was dismissed by the system, an OEM, or the user
     * pressed Home, the full-screen intent pops it back up automatically on
     * any device state (lock screen, screen off, background, DND).
     */
    private fun startFullscreenReassert() {
        reassertJob?.cancel()
        reassertJob = scope.launch {
            while (isActive) {
                delay(REASSERT_INTERVAL_MS)
                val alarm = activeAlarm ?: break
                runCatching {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                            as android.app.NotificationManager
                    manager.notify(
                        Notifications.ALARM_NOTIFICATION_ID,
                        Notifications.alarmNotification(this@AlarmRingingService, alarm, activeSnooze)
                    )
                }
                // Also re-assert the activity from the foreground service for
                // OEMs that throttle the full-screen-intent path.
                runCatching {
                    val activity = Intent(this@AlarmRingingService, AlarmRingingActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    }
                    startActivity(activity)
                }
            }
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
        reassertJob?.cancel()
        reassertJob = null
        stateJob?.cancel()
        stateJob = null
        activeAlarm = null
        activeSnooze = false
        stepGate?.stop()
        stepGate = null
        shakeGate?.stop()
        shakeGate = null
        RingingSession.activeGate = null
        RingingSession.activeShakeGate = null
        rampJob?.cancel()
        rampJob = null
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
        abandonAudioFocus()
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
        private const val TAG = "AlarmRingingService"
        private const val REASSERT_INTERVAL_MS = 8_000L
        const val ACTION_STOP = "com.tenstep.alarm.action.STOP_RINGING"

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }
    }
}
