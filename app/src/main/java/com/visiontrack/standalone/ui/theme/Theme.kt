package com.visiontrack.standalone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF080B12)
val Surface = Color(0xFF10151F)
val SurfaceHigh = Color(0xFF171E2B)
val Accent = Color(0xFF6BE7C8)
val AccentBlue = Color(0xFF71A7FF)
val Muted = Color(0xFF97A3B6)
val Line = Color(0xFF293142)
val Success = Color(0xFF6BE7C8)
val Warning = Color(0xFFFFCA6B)

private val VisionTrackColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF002019),
    secondary = AccentBlue,
    onSecondary = Color(0xFF04152F),
    background = Ink,
    onBackground = Color(0xFFF1F5FA),
    surface = Surface,
    onSurface = Color(0xFFF1F5FA),
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Color(0xFFFF8A8A)
)

@Composable
fun VisionTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VisionTrackColors,
        content = content
    )
}
