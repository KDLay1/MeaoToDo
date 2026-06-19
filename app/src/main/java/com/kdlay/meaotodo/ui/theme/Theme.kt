package com.kdlay.meaotodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF778CCC),
    secondary = Color(0xFFFDB78E),
    tertiary = Color(0xFF9ADBC5),
    background = Color(0xFFFFF8F3),
    surface = Color(0xFFFFF5D9)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFACBFEB),
    secondary = Color(0xFFFD8D6E),
    tertiary = Color(0xFF9ADBC5),
    background = Color(0xFF111318),
    surface = Color(0xFF1B1D24)
)

@Composable
fun MeaoTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
