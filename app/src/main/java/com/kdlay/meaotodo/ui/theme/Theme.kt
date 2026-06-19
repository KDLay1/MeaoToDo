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
    primary = Color(0xFF778CCC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9D4F7),
    onPrimaryContainer = Color(0xFF26345F),
    secondary = Color(0xFFFDB78E),
    onSecondary = Color(0xFF4A2210),
    secondaryContainer = Color(0xFFFFE1D2),
    onSecondaryContainer = Color(0xFF4A2210),
    tertiary = Color(0xFF9ADBC5),
    onTertiary = Color(0xFF12352B),
    tertiaryContainer = Color(0xFFDAF1EE),
    onTertiaryContainer = Color(0xFF12352B),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF27252C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF27252C),
    surfaceVariant = Color(0xFFFFF5D9),
    onSurfaceVariant = Color(0xFF6F6875),
    outline = Color(0xFFE6D9CC),
    error = Color(0xFFB85C4D),
    errorContainer = Color(0xFFFFDAD2)
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
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
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
