package com.example.spenttracker.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Exact Shadcn/ui color scheme matching Vue.js web app design
 * Using the exact OKLCH color values from CSS custom properties
 */

// Light Theme Colors (matching CSS --light theme)
object SpentTrackerColors {
    object Light {
        val background = Color(0xFFFFFFFF) // oklch(1 0 0)
        val foreground = Color(0xFF242330) // oklch(0.141 0.005 285.823)
        val primary = Color(0xFF373548) // oklch(0.21 0.006 285.885)
        val primaryForeground = Color(0xFFFBFBFB) // oklch(0.985 0 0)
        val secondary = Color(0xFFF7F7F8) // oklch(0.967 0.001 286.375)
        val secondaryForeground = Color(0xFF373548)
        val muted = Color(0xFFF7F7F8) // oklch(0.967 0.001 286.375)
        val mutedForeground = Color(0xFF8C8C95) // oklch(0.552 0.016 285.938)
        val accent = Color(0xFFF7F7F8)
        val accentForeground = Color(0xFF373548)
        val destructive = Color(0xFFDC2626) // oklch(0.577 0.245 27.325)
        val destructiveForeground = Color(0xFFDC2626)
        val border = Color(0xFFEAEAEB) // oklch(0.92 0.004 286.32)
        val input = Color(0xFFEAEAEB)
        val ring = Color(0xFFB4B4BA) // oklch(0.705 0.015 286.067)
    }
    
    // Dark Theme Colors (matching CSS --dark theme)
    object Dark {
        val background = Color(0xFF242330) // oklch(0.141 0.005 285.823)
        val foreground = Color(0xFFFBFBFB) // oklch(0.985 0 0)
        val primary = Color(0xFFFBFBFB) // oklch(0.985 0 0)
        val primaryForeground = Color(0xFF373548) // oklch(0.21 0.006 285.885)
        val secondary = Color(0xFF454351) // oklch(0.274 0.006 286.033)
        val secondaryForeground = Color(0xFFFBFBFB)
        val muted = Color(0xFF454351) // oklch(0.274 0.006 286.033)
        val mutedForeground = Color(0xFFB4B4BA) // oklch(0.705 0.015 286.067)
        val accent = Color(0xFF454351)
        val accentForeground = Color(0xFFFBFBFB)
        val destructive = Color(0xFF991B1B) // oklch(0.396 0.141 25.723)
        val destructiveForeground = Color(0xFFEF4444)
        val border = Color(0xFF454351) // oklch(0.274 0.006 286.033)
        val input = Color(0xFF454351)
        val ring = Color(0xFF6F6F78) // oklch(0.442 0.017 285.786)
    }
}

val SpentTrackerLightColorScheme = lightColorScheme(
    primary = SpentTrackerColors.Light.primary,
    onPrimary = SpentTrackerColors.Light.primaryForeground,
    primaryContainer = SpentTrackerColors.Light.secondary,
    onPrimaryContainer = SpentTrackerColors.Light.foreground,
    
    secondary = SpentTrackerColors.Light.secondary,
    onSecondary = SpentTrackerColors.Light.secondaryForeground,
    secondaryContainer = SpentTrackerColors.Light.muted,
    onSecondaryContainer = SpentTrackerColors.Light.mutedForeground,
    
    tertiary = SpentTrackerColors.Light.accent,
    onTertiary = SpentTrackerColors.Light.accentForeground,
    tertiaryContainer = SpentTrackerColors.Light.accent,
    onTertiaryContainer = SpentTrackerColors.Light.accentForeground,
    
    background = SpentTrackerColors.Light.background,
    onBackground = SpentTrackerColors.Light.foreground,
    
    surface = SpentTrackerColors.Light.background,
    onSurface = SpentTrackerColors.Light.foreground,
    surfaceVariant = SpentTrackerColors.Light.muted,
    onSurfaceVariant = SpentTrackerColors.Light.mutedForeground,
    
    error = SpentTrackerColors.Light.destructive,
    onError = SpentTrackerColors.Light.primaryForeground,
    
    outline = SpentTrackerColors.Light.border,
    outlineVariant = SpentTrackerColors.Light.input
)

val SpentTrackerDarkColorScheme = darkColorScheme(
    primary = SpentTrackerColors.Dark.primary,
    onPrimary = SpentTrackerColors.Dark.primaryForeground,
    primaryContainer = SpentTrackerColors.Dark.secondary,
    onPrimaryContainer = SpentTrackerColors.Dark.foreground,
    
    secondary = SpentTrackerColors.Dark.secondary,
    onSecondary = SpentTrackerColors.Dark.secondaryForeground,
    secondaryContainer = SpentTrackerColors.Dark.muted,
    onSecondaryContainer = SpentTrackerColors.Dark.mutedForeground,
    
    tertiary = SpentTrackerColors.Dark.accent,
    onTertiary = SpentTrackerColors.Dark.accentForeground,
    tertiaryContainer = SpentTrackerColors.Dark.accent,
    onTertiaryContainer = SpentTrackerColors.Dark.accentForeground,
    
    background = SpentTrackerColors.Dark.background,
    onBackground = SpentTrackerColors.Dark.foreground,
    
    surface = SpentTrackerColors.Dark.background,
    onSurface = SpentTrackerColors.Dark.foreground,
    surfaceVariant = SpentTrackerColors.Dark.muted,
    onSurfaceVariant = SpentTrackerColors.Dark.mutedForeground,
    
    error = SpentTrackerColors.Dark.destructive,
    onError = SpentTrackerColors.Dark.destructiveForeground,
    
    outline = SpentTrackerColors.Dark.border,
    outlineVariant = SpentTrackerColors.Dark.input
)