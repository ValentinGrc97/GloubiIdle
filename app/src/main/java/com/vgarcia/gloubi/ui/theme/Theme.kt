package com.vgarcia.gloubi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GloubiColorScheme = darkColorScheme(
    primary          = GloubiGreen,
    onPrimary        = GloubiOnSurface,
    primaryContainer = GloubiGreenDark,
    secondary        = GloubiTeal,
    onSecondary      = GloubiOnSurface,
    tertiary         = GloubiOrange,
    onTertiary       = GloubiOnSurface,
    background       = GloubiBackground,
    onBackground     = GloubiOnSurface,
    surface          = GloubiSurface,
    onSurface        = GloubiOnSurface,
    surfaceVariant   = GloubiSurface2,
    onSurfaceVariant = GloubiOnSurface2,
    error            = GloubiRed,
    onError          = GloubiOnSurface,
)

@Composable
fun GloubiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GloubiColorScheme,
        typography  = GloubiTypography,
        content     = content
    )
}