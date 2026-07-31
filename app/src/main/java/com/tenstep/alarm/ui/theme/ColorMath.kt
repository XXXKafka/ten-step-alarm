package com.tenstep.alarm.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Small pure color helpers (RGB <-> HSL) used by the theme seed derivation and
 * the custom color picker. Compose's public API only exposes Color.hsl()/hsv()
 * factories, not conversions back, so these are implemented here.
 */

/** Returns [hue (0..360), saturation (0..1), lightness (0..1)]. */
fun Color.toHslComponents(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val lightness = (maxC + minC) / 2f
    val delta = maxC - minC
    val saturation = if (delta == 0f) {
        0f
    } else {
        delta / (1f - abs(2f * lightness - 1f))
    }
    val hue = when {
        delta == 0f -> 0f
        maxC == r -> 60f * (((g - b) / delta) % 6f)
        maxC == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return floatArrayOf(
        if (hue < 0f) hue + 360f else hue,
        saturation.coerceIn(0f, 1f),
        lightness.coerceIn(0f, 1f)
    )
}

/** Builds a color from HSL components (hue 0..360, saturation/lightness 0..1). */
fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
}

/** Simple perceptual brightness check used to pick readable text colors. */
fun Color.isLight(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) > 0.55f