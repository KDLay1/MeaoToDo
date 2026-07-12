package com.kdlay.meaotodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF147E82),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEEEE),
    onPrimaryContainer = Color(0xFF0A5154),
    secondary = Color(0xFFB57631),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF8EBD8),
    onSecondaryContainer = Color(0xFF704514),
    tertiary = Color(0xFF4F8750),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE4F6E9),
    onTertiaryContainer = Color(0xFF174A2A),
    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF16242B),
    surface = Color(0xFFFFFEFC),
    onSurface = Color(0xFF16242B),
    surfaceVariant = Color(0xFFF3F5F3),
    onSurfaceVariant = Color(0xFF667075),
    outline = Color(0xFFE1E3DF),
    outlineVariant = Color(0xFFEBECE8),
    error = Color(0xFFC95D48),
    errorContainer = Color(0xFFFBE5DF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD0D1),
    onPrimary = Color(0xFF003738),
    primaryContainer = Color(0xFF164F51),
    onPrimaryContainer = Color(0xFFB9F0F0),
    secondary = Color(0xFFE2B276),
    onSecondary = Color(0xFF402A0D),
    secondaryContainer = Color(0xFF5B421F),
    onSecondaryContainer = Color(0xFFFFDDB5),
    tertiary = Color(0xFFA2D39D),
    onTertiary = Color(0xFF123814),
    tertiaryContainer = Color(0xFF2E5130),
    onTertiaryContainer = Color(0xFFC9F5C3),
    background = Color(0xFF111514),
    onBackground = Color(0xFFE8ECE9),
    surface = Color(0xFF191E1C),
    onSurface = Color(0xFFE8ECE9),
    surfaceVariant = Color(0xFF242B28),
    onSurfaceVariant = Color(0xFFBCC7C1),
    outline = Color(0xFF48534E),
    outlineVariant = Color(0xFF303936),
    error = Color(0xFFFFB4A7),
    errorContainer = Color(0xFF77352A)
)

private val MeaoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MeaoTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = MeaoShapes,
        content = content
    )
}
