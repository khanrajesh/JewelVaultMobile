package com.velox.jewelvault.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppThemeStyle(val key: String, val label: String) {
    GOLD("gold", "Gold"),
    MINIMAL("minimal", "Minimal")
}

fun appThemeStyleFromKey(key: String): AppThemeStyle {
    return when (key.trim().lowercase()) {
        AppThemeStyle.MINIMAL.key -> AppThemeStyle.MINIMAL
        else -> AppThemeStyle.GOLD
    }
}

val LocalAppCornerRadius = staticCompositionLocalOf { 12 }
val LocalAppThemeStyle = staticCompositionLocalOf { AppThemeStyle.GOLD }
val LocalForceSoftKeyboardWithHid = staticCompositionLocalOf { true }
