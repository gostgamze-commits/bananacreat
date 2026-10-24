package com.bananacreat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB900),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332E00),
    onPrimaryContainer = Color(0xFFFFE082),
    secondary = Color(0xFFDCC6FF),
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF4F378A),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFFFB9A4),
    onTertiary = Color(0xFF45210B),
    tertiaryContainer = Color(0xFF622E1F),
    onTertiaryContainer = Color(0xFFFFDBCF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC7D0),
    outline = Color(0xFF938F99),
)

@Composable
fun BananaCreatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
