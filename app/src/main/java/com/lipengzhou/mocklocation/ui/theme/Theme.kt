package com.lipengzhou.mocklocation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ColorOnLightPrimary = Color.White
private val ColorOnLightSecondary = Color.White
private val ColorOnLightTertiary = Color.White
private val ColorOnLightError = Color.White
private val ColorOnDarkPrimary = Color(0xFF003825)
private val ColorOnDarkSecondary = Color(0xFF003550)
private val ColorOnDarkTertiary = Color(0xFF4A2E00)
private val ColorOnDarkError = Color(0xFF690005)
private val ColorLightOutline = Color(0xFF6F7971)
private val ColorLightOutlineVariant = Color(0xFFC0CAC0)
private val ColorDarkOutline = Color(0xFF89938B)
private val ColorDarkOutlineVariant = Color(0xFF414942)
private val ColorLightError = Color(0xFFBA1A1A)
private val ColorLightErrorContainer = Color(0xFFFFDAD6)
private val ColorOnLightErrorContainer = Color(0xFF410002)
private val ColorDarkError = Color(0xFFFFB4AB)
private val ColorDarkErrorContainer = Color(0xFF93000A)
private val ColorOnDarkErrorContainer = Color(0xFFFFDAD6)

private val DarkColorScheme = darkColorScheme(
    primary = LocationGreen80,
    onPrimary = ColorOnDarkPrimary,
    primaryContainer = LocationGreen30,
    onPrimaryContainer = LocationGreen90,
    secondary = NavigationBlue80,
    onSecondary = ColorOnDarkSecondary,
    secondaryContainer = NavigationBlue30,
    onSecondaryContainer = NavigationBlue90,
    tertiary = SignalAmber80,
    onTertiary = ColorOnDarkTertiary,
    tertiaryContainer = SignalAmber30,
    onTertiaryContainer = SignalAmber90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral80,
    surfaceContainer = Neutral20,
    surfaceContainerHigh = Neutral30,
    outline = ColorDarkOutline,
    outlineVariant = ColorDarkOutlineVariant,
    error = ColorDarkError,
    onError = ColorOnDarkError,
    errorContainer = ColorDarkErrorContainer,
    onErrorContainer = ColorOnDarkErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = LocationGreen40,
    onPrimary = ColorOnLightPrimary,
    primaryContainer = LocationGreen95,
    onPrimaryContainer = LocationGreen30,
    secondary = NavigationBlue40,
    onSecondary = ColorOnLightSecondary,
    secondaryContainer = NavigationBlue95,
    onSecondaryContainer = NavigationBlue30,
    tertiary = SignalAmber40,
    onTertiary = ColorOnLightTertiary,
    tertiaryContainer = SignalAmber95,
    onTertiaryContainer = SignalAmber30,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = Neutral94,
    onSurfaceVariant = Neutral30,
    surfaceContainer = Neutral96,
    surfaceContainerHigh = Neutral94,
    outline = ColorLightOutline,
    outlineVariant = ColorLightOutlineVariant,
    error = ColorLightError,
    onError = ColorOnLightError,
    errorContainer = ColorLightErrorContainer,
    onErrorContainer = ColorOnLightErrorContainer
)

@Composable
fun MockLocationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
