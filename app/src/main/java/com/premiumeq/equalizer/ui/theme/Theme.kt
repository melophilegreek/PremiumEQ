package com.premiumeq.equalizer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = EqPrimary,
    secondary = EqSecondary,
    background = EqBackgroundLight,
    surface = EqSurfaceLight,
    error = EqError
)

private val DarkColors = darkColorScheme(
    primary = EqPrimaryVariant,
    secondary = EqSecondary,
    background = EqBackgroundDark,
    surface = EqSurfaceDark,
    error = EqError
)

private val AmoledColors = darkColorScheme(
    primary = EqPrimaryVariant,
    secondary = EqSecondary,
    background = EqAmoledBackground,
    surface = EqAmoledSurface,
    error = EqError
)

/**
 * @param useDynamicColor Material You - only actually applied on API 31+; silently
 *   falls back to the static brand palette below that, so callers don't need to
 *   check the OS version themselves.
 * @param useAmoledMode true black surfaces for OLED power savings; only meaningful
 *   when [darkTheme] is also true.
 * @param accentColor when non-null, overrides the color scheme's primary color -
 *   this takes priority over both Material You and the static brand color, since
 *   it represents an explicit user choice.
 * @param cornerRadiusDp drives every Card/Button/Chip corner radius app-wide via
 *   [MaterialTheme.shapes], so "adjustable corner radius" actually changes the UI
 *   rather than being a setting that does nothing.
 */
@Composable
fun PremiumEQTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    useAmoledMode: Boolean = false,
    accentColor: Color? = null,
    cornerRadiusDp: Int = 16,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var colorScheme = when {
        useDynamicColor && dynamicSupported && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && dynamicSupported && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme && useAmoledMode -> AmoledColors
        darkTheme -> DarkColors
        else -> LightColors
    }

    if (accentColor != null) {
        colorScheme = colorScheme.copy(primary = accentColor, secondary = accentColor)
    }

    val shapes = Shapes(
        extraSmall = RoundedCornerShape((cornerRadiusDp * 0.25f).dp),
        small = RoundedCornerShape((cornerRadiusDp * 0.5f).dp),
        medium = RoundedCornerShape(cornerRadiusDp.dp),
        large = RoundedCornerShape((cornerRadiusDp * 1.5f).dp),
        extraLarge = RoundedCornerShape((cornerRadiusDp * 2f).dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EqTypography,
        shapes = shapes,
        content = content
    )
}

