package io.botinis.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark-only color scheme
private val BotinisColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = AccentPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = TextPrimary,
    secondary = AccentSecondary,
    onSecondary = Color.Black,
    secondaryContainer = AccentSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = TextPrimary,
    tertiary = Warning,
    onTertiary = Color.Black,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundSecondary,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundTertiary,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.15f),
    onErrorContainer = Error,
)

@Composable
fun BotinisTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = BotinisColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundPrimary.toArgb()
            window.navigationBarColor = BackgroundPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
