package com.wfx.warungpos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Amber40,
    onPrimary = WarmWhite,
    primaryContainer = Amber90,
    onPrimaryContainer = Amber10,
    secondary = Stone40,
    onSecondary = WarmWhite,
    secondaryContainer = Stone90,
    onSecondaryContainer = NearBlack,
    tertiary = Green40,
    onTertiary = WarmWhite,
    tertiaryContainer = Green90,
    onTertiaryContainer = Green30,
    error = Red40,
    onError = WarmWhite,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = WarmWhite,
    onBackground = NearBlack,
    surface = WarmWhite,
    onSurface = NearBlack,
    surfaceVariant = Stone95,
    onSurfaceVariant = Stone40,
    outline = Stone80,
)

private val DarkColorScheme = darkColorScheme(
    primary = Amber80,
    onPrimary = Amber10,
    primaryContainer = Amber40,
    onPrimaryContainer = Amber90,
    secondary = Stone80,
    onSecondary = NearBlack,
    secondaryContainer = Stone40,
    onSecondaryContainer = Stone90,
    tertiary = Green80,
    onTertiary = Green30,
    tertiaryContainer = Green40,
    onTertiaryContainer = Green90,
    error = Red90,
    onError = Red10,
    errorContainer = Red40,
    onErrorContainer = Red90,
)

@Composable
fun WarungPosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic color disabled — consistent POS branding takes priority over system wallpaper colors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
