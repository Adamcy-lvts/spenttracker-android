package com.example.spenttracker.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Custom theme system without Material Design 3
 * Provides theming based on markdown specifications
 */

// Composition locals for custom theme
val LocalCustomColorScheme = staticCompositionLocalOf { LightCustomColorScheme }
val LocalCustomTypography = staticCompositionLocalOf { CustomTypography }
val LocalCustomDimensions = staticCompositionLocalOf { SpentTrackerDimensions }

/**
 * Custom theme composable that provides theming without Material3
 */
@Composable
fun CustomSpentTrackerTheme(
    darkTheme: Boolean = true, // Default to dark mode as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkCustomColorScheme else LightCustomColorScheme
    
    CompositionLocalProvider(
        LocalCustomColorScheme provides colorScheme,
        LocalCustomTypography provides CustomTypography,
        LocalCustomDimensions provides SpentTrackerDimensions,
        content = content
    )
}

/**
 * Object to access current theme values
 */
object CustomTheme {
    val colors: CustomColorScheme
        @Composable
        get() = LocalCustomColorScheme.current
    
    val typography: CustomTypography
        @Composable
        get() = LocalCustomTypography.current
        
    val dimensions: SpentTrackerDimensions
        @Composable
        get() = LocalCustomDimensions.current
}