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
    small = RoundedCornerShape(10.dp),
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
    monochrome: Boolean = false,
    seedColor: Color = TealPrimary,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        themeMode == ThemeMode.AMOLED -> amoledColorScheme(seedColor)
        monochrome -> if (darkTheme) monochromeColorScheme().second else monochromeColorScheme().first
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
    val colorSource by settingsStore.themeColorSource.collectAsStateWithLifecycle("mono")
    val presetIndex by settingsStore.themePresetIndex.collectAsStateWithLifecycle(0)
    val customArgb by settingsStore.themeCustomColor
        .collectAsStateWithLifecycle(SettingsStore.DEFAULT_ACCENT_ARGB)

    val seedColor = when (colorSource) {
        "preset" -> ThemePresetSeeds.getOrElse(presetIndex) { TealPrimary }
        "custom" -> Color(customArgb.toInt())
        // "mono" (the default) and any unknown value use the neutral gray seed.
        else -> MonoSeed
    }
    TenStepTheme(
        themeMode = themeMode,
        useDynamicColor = colorSource == "dynamic",
        monochrome = colorSource == "mono",
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

/**
 * True-black ("AMOLED") dark scheme: surfaces are pure black so OLED pixels
 * stay off; accents keep the seed hue.
 */
fun amoledColorScheme(seed: Color): ColorScheme {
    val hsl = seed.toHslComponents()
    val hue = hsl[0]
    val sat = hsl[1]
    val s = { f: Float -> (sat * f).coerceIn(0f, 1f) }
    val h2 = (hue + 40f) % 360f
    return darkColorScheme(
        primary = Color.hsl(hue, s(0.6f), 0.8f),
        onPrimary = Color.hsl(hue, s(0.9f), 0.12f),
        primaryContainer = Color.hsl(hue, s(0.55f), 0.2f),
        onPrimaryContainer = Color.hsl(hue, s(0.45f), 0.92f),
        secondary = Color.hsl(hue, s(0.35f), 0.72f),
        onSecondary = Color.hsl(hue, s(0.8f), 0.12f),
        secondaryContainer = Color.hsl(hue, s(0.4f), 0.18f),
        onSecondaryContainer = Color.hsl(hue, s(0.3f), 0.9f),
        tertiary = Color.hsl(h2, s(0.35f), 0.76f),
        onTertiary = Color.hsl(h2, s(0.75f), 0.14f),
        tertiaryContainer = Color.hsl(h2, s(0.4f), 0.2f),
        onTertiaryContainer = Color.hsl(h2, s(0.3f), 0.92f),
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF181818),
        onSurfaceVariant = Color(0xFFB3B3B3),
        outline = Color(0xFF3D3D3D),
        outlineVariant = Color(0xFF262626),
        surfaceContainer = Color(0xFF0C0C0C),
        surfaceContainerLow = Color.Black,
        surfaceContainerHigh = Color(0xFF121212),
        surfaceContainerHighest = Color(0xFF1A1A1A)
    )
}
/**
 * Neutral gray ("monochrome") palette: black / white / gray surfaces and
 * accents (saturation ~ 0). Used as the default theme color so the app looks
 * black-white-gray out of the box while dynamic / preset / custom remain
 * optional.
 */
fun monochromeColorScheme(): Pair<ColorScheme, ColorScheme> {
    val light = lightColorScheme(
        primary = Color(0xFF1B1B1F),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE3E3E6),
        onPrimaryContainer = Color(0xFF1B1B1F),
        secondary = Color(0xFF44474F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCDEE3),
        onSecondaryContainer = Color(0xFF1B1B1F),
        tertiary = Color(0xFF5A5D66),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE2E2E6),
        onTertiaryContainer = Color(0xFF1B1B1F),
        background = Color(0xFFFDFBFF),
        onBackground = Color(0xFF1B1B1F),
        surface = Color(0xFFFDFBFF),
        onSurface = Color(0xFF1B1B1F),
        surfaceVariant = Color(0xFFE3E3E6),
        onSurfaceVariant = Color(0xFF44474F),
        outline = Color(0xFF74777F),
        outlineVariant = Color(0xFFC7C6D0),
        surfaceContainer = Color(0xFFF0EFF4),
        surfaceContainerLow = Color(0xFFF7F6FA),
        surfaceContainerHigh = Color(0xFFEBE9EE),
        surfaceContainerHighest = Color(0xFFE5E3E9)
    )
    val dark = darkColorScheme(
        primary = Color(0xFFC9C9D0),
        onPrimary = Color(0xFF2A2A2F),
        primaryContainer = Color(0xFF404046),
        onPrimaryContainer = Color(0xFFE5E5EA),
        secondary = Color(0xFFB3B4BC),
        onSecondary = Color(0xFF2B2C31),
        secondaryContainer = Color(0xFF414247),
        onSecondaryContainer = Color(0xFFCFD0D8),
        tertiary = Color(0xFFB9BAC2),
        onTertiary = Color(0xFF232429),
        tertiaryContainer = Color(0xFF3A3B40),
        onTertiaryContainer = Color(0xFFD9D9DF),
        background = Color(0xFF121316),
        onBackground = Color(0xFFE4E2E9),
        surface = Color(0xFF121316),
        onSurface = Color(0xFFE4E2E9),
        surfaceVariant = Color(0xFF424247),
        onSurfaceVariant = Color(0xFFC5C4CC),
        outline = Color(0xFF8F8E97),
        outlineVariant = Color(0xFF424247),
        surfaceContainer = Color(0xFF1E1E22),
        surfaceContainerLow = Color(0xFF1A1B1E),
        surfaceContainerHigh = Color(0xFF29292D),
        surfaceContainerHighest = Color(0xFF333438)
    )
    return light to dark
}