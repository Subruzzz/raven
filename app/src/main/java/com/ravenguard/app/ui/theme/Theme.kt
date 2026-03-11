package com.ravenguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RavenColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = RavenText,
    background = RavenBackground,
    surface = GlassSurface,
    onSurface = RavenText,
    error = EmergencyRed,
    onError = RavenText
)

@Composable
fun RavenGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RavenColorScheme,
        typography = RavenTypography,
        content = content
    )
}
