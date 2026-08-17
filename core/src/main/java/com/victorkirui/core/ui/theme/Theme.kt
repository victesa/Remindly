package com.victorkirui.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Evergreen,
    onPrimary = FreshSnow,
    secondary = MutedSage,
    tertiary = SagePill,
    background = DarkPine,
    surface = DarkPine,
    onBackground = FreshSnow,
    onSurface = FreshSnow,
    error = MountainBerry
)

private val LightColorScheme = lightColorScheme(
    primary = Evergreen,
    onPrimary = FreshSnow,
    primaryContainer = SoftMint,
    onPrimaryContainer = Evergreen,
    
    secondary = MutedSage,
    onSecondary = FreshSnow,
    secondaryContainer = SagePill,
    onSecondaryContainer = DarkPine,

    background = FreshSnow,
    onBackground = DarkPine,

    surface = Frost,
    onSurface = DarkPine,
    surfaceVariant = LightEvergreen,
    onSurfaceVariant = MutedSage,
    
    outline = NeutralBorder,
    outlineVariant = FrostLine,
    
    error = MountainBerry,
    onError = FreshSnow,
    errorContainer = SoftBerry,
    onErrorContainer = MountainBerry
)

@Composable
fun RemindlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false to prioritize your custom design system over system dynamic colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
