@file:Suppress("DEPRECATION")

package com.velox.jewelvault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val minimalLightScheme = lightColorScheme(
    primary = Color(0xFF141414),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0F0EC),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF4F4F4F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7E7E2),
    onSecondaryContainer = Color(0xFF212121),
    tertiary = Color(0xFF666666),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F8F5),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFECECE7),
    onSurfaceVariant = Color(0xFF4B4B47),
    outline = Color(0xFF777773),
    outlineVariant = Color(0xFFC7C7C1)
)

private val minimalDarkScheme = darkColorScheme(
    primary = Color(0xFFEAEAEA),
    onPrimary = Color(0xFF141414),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFF2F2F2),
    secondary = Color(0xFFD4D4D4),
    onSecondary = Color(0xFF1C1C1C),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFE8E8E8),
    tertiary = Color(0xFFB9B9B9),
    onTertiary = Color(0xFF202020),
    background = Color(0xFF101010),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF171717),
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFF969696),
    outlineVariant = Color(0xFF464646)
)

@Composable
fun JewelVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeStyle: AppThemeStyle = AppThemeStyle.GOLD,
    forceSoftKeyboardWithHid: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
        themeStyle == AppThemeStyle.MINIMAL && darkTheme -> minimalDarkScheme
        themeStyle == AppThemeStyle.MINIMAL -> minimalLightScheme
        darkTheme -> darkScheme
        else -> lightScheme
    }

    val fixedCorner = if (themeStyle == AppThemeStyle.MINIMAL) 0 else 12
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((fixedCorner - 6).coerceAtLeast(0).dp),
        small = RoundedCornerShape((fixedCorner - 4).coerceAtLeast(0).dp),
        medium = RoundedCornerShape(fixedCorner.dp),
        large = RoundedCornerShape((fixedCorner + 4).coerceAtMost(32).dp),
        extraLarge = RoundedCornerShape((fixedCorner + 8).coerceAtMost(36).dp)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val systemBarColor = colorScheme.surface.toArgb()
            val useDarkIcons = colorScheme.surface.luminance() > 0.5f
            window.statusBarColor = systemBarColor
            window.navigationBarColor = systemBarColor
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = useDarkIcons
                isAppearanceLightNavigationBars = useDarkIcons
            }
        }
    }

    CompositionLocalProvider(
        LocalAppCornerRadius provides fixedCorner,
        LocalAppThemeStyle provides themeStyle,
        LocalForceSoftKeyboardWithHid provides forceSoftKeyboardWithHid
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}
