package com.example.photomanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF3949AB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001258),
    secondary = Color(0xFF535D7E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2FF),
    onSecondaryContainer = Color(0xFF0F1A37),
    tertiary = Color(0xFF8C4A60),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3A071D),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191B23),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191B23),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF002388),
    primaryContainer = Color(0xFF1E3293),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFBBC6EA),
    onSecondary = Color(0xFF252F4D),
    secondaryContainer = Color(0xFF3B4565),
    onSecondaryContainer = Color(0xFFDAE2FF),
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF541D32),
    tertiaryContainer = Color(0xFF6F3348),
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFF11131A),
    onBackground = Color(0xFFE2E2EC),
    surface = Color(0xFF11131A),
    onSurface = Color(0xFFE2E2EC),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099)
)

@Composable
fun PhotoManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
