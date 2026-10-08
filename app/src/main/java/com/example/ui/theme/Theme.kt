package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BasicDarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF030712),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF166534),
    onSecondaryContainer = Color(0xFFDCFCE7),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = Color(0xFF090D16),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF131B2E),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun BasicStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BasicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
