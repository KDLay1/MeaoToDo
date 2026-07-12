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
    primary = Color(0xFFACBFEB),
    onPrimary = Color(0xFF1F2D59),
    primaryContainer = Color(0xFF35446F),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFFFD8D6E),
    onSecondary = Color(0xFF3E1508),
    secondaryContainer = Color(0xFF5D2D20),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFF9ADBC5),
    onTertiary = Color(0xFF0C3026),
    tertiaryContainer = Color(0xFF244D42),
    onTertiaryContainer = Color(0xFFC6F3E4),
    background = Color(0xFF111318),
    onBackground = Color(0xFFF1EEF6),
    surface = Color(0xFF1B1D24),
    onSurface = Color(0xFFF1EEF6),
    surfaceVariant = Color(0xFF262A35),
    onSurfaceVariant = Color(0xFFC9C4D0),
    outline = Color(0xFF4A4E5C),
    error = Color(0xFFFFB4A8),
    errorContainer = Color(0xFF7A2B20)
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
