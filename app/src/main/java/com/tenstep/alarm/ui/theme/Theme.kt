package com.tenstep.alarm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenstep.alarm.data.SettingsStore
import com.tenstep.alarm.data.ThemeMode

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

/**
 * App theme: Material 3, flat design.
 *
 * @param useDynamicColor use the system dynamic palette on API 31+.
 * @param seedColor when dynamic color is off/unsupported, expand this seed into
 *   a full light/dark scheme.
 */
@Composable
fun TenStepTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    seedColor: Color = TealPrimary,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> seedColorSchemes(seedColor).second
        else -> seedColorSchemes(seedColor).first
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = AppShapes,
        content = content
    )
}

/**
 * Collects the theme settings (mode + color source) and renders the app theme,
 * shared by MainActivity and the ringing activity so both follow the user's
 * theme/language choices.
 */
@Composable
fun AppSettingsTheme(
    settingsStore: SettingsStore,
    content: @Composable () -> Unit
) {
    val themeMode by settingsStore.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val colorSource by settingsStore.themeColorSource.collectAsStateWithLifecycle("dynamic")
    val presetIndex by settingsStore.themePresetIndex.collectAsStateWithLifecycle(0)
    val customArgb by settingsStore.themeCustomColor
        .collectAsStateWithLifecycle(SettingsStore.DEFAULT_ACCENT_ARGB)

    val seedColor = when (colorSource) {
        "preset" -> ThemePresetSeeds.getOrElse(presetIndex) { TealPrimary }
        "custom" -> Color(customArgb.toInt())
        else -> TealPrimary
    }
    TenStepTheme(
        themeMode = themeMode,
        useDynamicColor = colorSource == "dynamic",
        seedColor = seedColor,
        content = content
    )
}

/**
 * Expands a seed color into a complete light/dark Material 3 scheme using
 * HSL-based derivation (primary keeps the seed hue; secondary/tertiary are
 * toned variants of the same hue).
 */
fun seedColorSchemes(seed: Color): Pair<ColorScheme, ColorScheme> {
    val hsl = seed.toHslComponents()
    val hue = hsl[0]
    val sat = hsl[1]
    val light = hsl[2]
    val s = { f: Float -> (sat * f).coerceIn(0f, 1f) }
    val h2 = (hue + 40f) % 360f

    val lightScheme = lightColorScheme(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = Color.hsl(hue, s(0.5f), 0.9f),
        onPrimaryContainer = Color.hsl(hue, s(0.9f), 0.14f),
        secondary = Color.hsl(hue, s(0.4f), 0.42f),
        onSecondary = Color.White,
        secondaryContainer = Color.hsl(hue, s(0.35f), 0.88f),
        onSecondaryContainer = Color.hsl(hue, s(0.75f), 0.14f),
        tertiary = Color.hsl(h2, s(0.4f), 0.48f),
        onTertiary = Color.White,
        tertiaryContainer = Color.hsl(h2, s(0.35f), 0.9f),
        onTertiaryContainer = Color.hsl(h2, s(0.7f), 0.16f),
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        surfaceContainer = LightSurfaceContainer
    )

    val darkScheme = darkColorScheme(
        primary = Color.hsl(hue, s(0.6f), 0.78f),
        onPrimary = Color.hsl(hue, s(0.9f), 0.12f),
        primaryContainer = Color.hsl(hue, s(0.6f), 0.26f),
        onPrimaryContainer = Color.hsl(hue, s(0.45f), 0.9f),
        secondary = Color.hsl(hue, s(0.35f), 0.7f),
        onSecondary = Color.hsl(hue, s(0.8f), 0.12f),
        secondaryContainer = Color.hsl(hue, s(0.4f), 0.24f),
        onSecondaryContainer = Color.hsl(hue, s(0.3f), 0.88f),
        tertiary = Color.hsl(h2, s(0.35f), 0.74f),
        onTertiary = Color.hsl(h2, s(0.75f), 0.14f),
        tertiaryContainer = Color.hsl(h2, s(0.4f), 0.26f),
        onTertiaryContainer = Color.hsl(h2, s(0.3f), 0.9f),
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
        surfaceContainer = DarkSurfaceContainer
    )
    return lightScheme to darkScheme
}
