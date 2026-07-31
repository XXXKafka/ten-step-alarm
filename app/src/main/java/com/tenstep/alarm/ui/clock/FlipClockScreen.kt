package com.tenstep.alarm.ui.clock

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenstep.alarm.R
import com.tenstep.alarm.ui.theme.isLight
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/**
 * Full-screen desktop clock.
 *
 * - Plain time characters (no flip animation) on an adjustable background.
 * - Adaptive sizing: the time always scales to fit and stays centered both
 *   horizontally and vertically, with a margin from the nearest screen edge.
 * - Fullscreen mode: the adjustable background color fills the whole screen
 *   and the time is drawn directly on it (no card).
 * - Keeps the screen on and hides the system bars.
 * - The overlay (back / fullscreen / seconds / date) auto-hides after 3
 *   seconds; tapping the screen toggles it.
 */
@Composable
fun FlipClockScreen(onBack: () -> Unit) {
    val viewModel: ClockViewModel = viewModel()
    val activity = LocalContext.current as? Activity
    val view = LocalView.current

    val showSeconds by viewModel.showSeconds.collectAsStateWithLifecycle()
    val showDate by viewModel.showDate.collectAsStateWithLifecycle()
    val is24Hour by viewModel.is24Hour.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val customColorArgb by viewModel.customColorArgb.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val fullscreen by viewModel.fullscreen.collectAsStateWithLifecycle()

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var overlayVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    // Auto-hide the overlay 3 seconds after it becomes visible.
    LaunchedEffect(overlayVisible) {
        if (overlayVisible) {
            delay(3000)
            overlayVisible = false
        }
    }

    // Keep the screen on while this page is visible.
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Immersive mode: hide status + navigation bars; restore on leave.
    val insetsController = remember(activity, view) {
        activity?.let { WindowInsetsControllerCompat(it.window, view) }
    }
    DisposableEffect(insetsController) {
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val (timeBackground, timeForeground) = clockColors(style, customColorArgb)

    // Keep the system bar appearance consistent with the page background and
    // disable the Android 15 contrast scrim, so in fullscreen mode the status
    // bar area matches the background color exactly.
    val themeBackground = MaterialTheme.colorScheme.background
    DisposableEffect(activity, fullscreen, timeBackground, themeBackground) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val bgIsLight = if (fullscreen) timeBackground.isLight() else themeBackground.isLight()
        controller?.isAppearanceLightStatusBars = bgIsLight
        controller?.isAppearanceLightNavigationBars = bgIsLight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isStatusBarContrastEnforced = false
            window?.isNavigationBarContrastEnforced = false
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window?.isStatusBarContrastEnforced = true
                window?.isNavigationBarContrastEnforced = true
            }
        }
    }

    val weekdays = stringArrayResource(R.array.weekday_full)
    val weekday = weekdays[(now.dayOfWeek.value - 1).coerceIn(0, 6)]
    val dateText = stringResource(
        R.string.clock_date_format,
        now.year, now.monthValue, now.dayOfMonth, weekday
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (fullscreen) timeBackground else MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures { overlayVisible = !overlayVisible }
            }
    ) {
        // The time is always strictly centered on the screen.
        TimeDisplay(
            now = now,
            showSeconds = showSeconds,
            is24Hour = is24Hour,
            fontScale = fontScale,
            background = timeBackground,
            foreground = timeForeground,
            fullscreen = fullscreen,
            modifier = Modifier.align(Alignment.Center)
        )

        // Date (part of the overlay; auto-hides with it).
        AnimatedVisibility(
            visible = overlayVisible && showDate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.headlineSmall,
                color = if (fullscreen) timeForeground else MaterialTheme.colorScheme.onBackground
            )
        }

        // Overlay controls.
        AnimatedVisibility(
            visible = overlayVisible,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.clock_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = overlayVisible,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row {
                IconButton(onClick = { viewModel.setFullscreen(!fullscreen) }) {
                    Icon(
                        imageVector = if (fullscreen) {
                            Icons.Filled.FullscreenExit
                        } else {
                            Icons.Filled.Fullscreen
                        },
                        contentDescription = stringResource(R.string.clock_fullscreen),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.setShowSeconds(!showSeconds) }) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = stringResource(R.string.clock_show_seconds),
                        tint = if (showSeconds) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * Renders the time as plain characters. The size is adaptive: it always fits
 * within the screen with a margin from every edge. In [fullscreen] mode the
 * text is drawn directly on the page background (no card).
 */
@Composable
private fun TimeDisplay(
    now: LocalDateTime,
    showSeconds: Boolean,
    is24Hour: Boolean,
    fontScale: Float,
    background: Color,
    foreground: Color,
    fullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    val rawHour = now.hour
    val hour12 = rawHour % 12
    val displayHour = if (is24Hour) rawHour else if (hour12 == 0) 12 else hour12
    val timeText = buildString {
        append(displayHour.toString().padStart(2, '0'))
        append(':')
        append(now.minute.toString().padStart(2, '0'))
        if (showSeconds) {
            append(':')
            append(now.second.toString().padStart(2, '0'))
        }
    }
    val amPm = if (!is24Hour) {
        if (rawHour < 12) stringResource(R.string.am_label) else stringResource(R.string.pm_label)
    } else {
        null
    }

    BoxWithConstraints(modifier) {
        // Keep a comfortable margin from the nearest screen edge.
        val margin = 24.dp
        val charCount = timeText.length + (if (amPm != null) 3 else 0)
        val widthBudget = (maxWidth - margin * 2) / (charCount * 0.62f)
        val heightBudget = (maxHeight - margin * 2) / 1.8f
        val baseSize = minOf(widthBudget, heightBudget)
        val timeFontSize = with(LocalDensity.current) { (baseSize * fontScale).toSp() }

        val content: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeText,
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.Bold,
                    color = foreground,
                    maxLines = 1
                )
                if (amPm != null) {
                    Spacer(Modifier.width(baseSize * 0.2f * fontScale))
                    Text(
                        text = amPm,
                        fontSize = timeFontSize * 0.32f,
                        fontWeight = FontWeight.Bold,
                        color = foreground
                    )
                }
            }
        }

        if (fullscreen) {
            content()
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(background)
                    .padding(horizontal = baseSize * 0.35f, vertical = baseSize * 0.22f)
            ) {
                content()
            }
        }
    }
}

/**
 * Time background/text colors.
 *
 * - auto: light theme -> white background / black text; dark theme -> black
 *   background / white text.
 * - light / dark: the same two fixed schemes regardless of the theme.
 * - custom: user-picked background color; text is auto black/white by contrast.
 */
@Composable
private fun clockColors(style: String, customColorArgb: Long): Pair<Color, Color> {
    return when (style) {
        "light" -> Color.White to Color.Black
        "dark" -> Color.Black to Color.White
        "custom" -> {
            val background = Color(customColorArgb.toInt())
            val foreground = if (background.isLight()) Color.Black else Color.White
            background to foreground
        }
        else -> {
            val darkTheme = !MaterialTheme.colorScheme.background.isLight()
            if (darkTheme) Color.Black to Color.White else Color.White to Color.Black
        }
    }
}
