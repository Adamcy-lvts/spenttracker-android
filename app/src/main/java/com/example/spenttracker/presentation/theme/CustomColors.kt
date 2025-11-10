package com.example.spenttracker.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Custom color scheme without Material Design 3
 * Based on shadcn/ui color tokens from markdown specifications
 */
object SpentTrackerCustomColors {
    
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
    
    object Dark {
        // True dark backgrounds (matching your actual web appearance)
        val background = Color(0xFF0A0A0A) // Very close to pure black
        val foreground = Color(0xFFFBFBFB) // Near white text
        
        // Primary colors (white on dark)
        val primary = Color(0xFFFBFBFB) // White primary
        val primaryForeground = Color(0xFF1B1B18) // Dark text on white
        
        // Secondary/muted surfaces
        val secondary = Color(0xFF242330) // Dark purple-gray
        val secondaryForeground = Color(0xFFFBFBFB) // White text
        val muted = Color(0xFF161615) // Very dark gray for cards
        val mutedForeground = Color(0xFFA1A09A) // Light gray text
        
        // Interactive elements
        val accent = Color(0xFF242330)
        val accentForeground = Color(0xFFFBFBFB)
        
        // Status colors
        val destructive = Color(0xFFEF4444) // Error red
        val destructiveForeground = Color(0xFFFBFBFB)
        
        // Borders and inputs (darker than web due to contrast requirements)
        val border = Color(0xFF3E3E3A) // Subtle border
        val input = Color(0xFF1A1A1A) // Dark gray input background (no purple)
        val ring = Color(0xFF62605B) // Focus ring
    }
}

/**
 * Custom color scheme data class
 */
data class CustomColorScheme(
    val background: Color,
    val foreground: Color,
    val primary: Color,
    val primaryForeground: Color,
    val secondary: Color,
    val secondaryForeground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val accent: Color,
    val accentForeground: Color,
    val destructive: Color,
    val destructiveForeground: Color,
    val border: Color,
    val input: Color,
    val ring: Color,
)

val LightCustomColorScheme = CustomColorScheme(
    background = SpentTrackerCustomColors.Light.background,
    foreground = SpentTrackerCustomColors.Light.foreground,
    primary = SpentTrackerCustomColors.Light.primary,
    primaryForeground = SpentTrackerCustomColors.Light.primaryForeground,
    secondary = SpentTrackerCustomColors.Light.secondary,
    secondaryForeground = SpentTrackerCustomColors.Light.secondaryForeground,
    muted = SpentTrackerCustomColors.Light.muted,
    mutedForeground = SpentTrackerCustomColors.Light.mutedForeground,
    accent = SpentTrackerCustomColors.Light.accent,
    accentForeground = SpentTrackerCustomColors.Light.accentForeground,
    destructive = SpentTrackerCustomColors.Light.destructive,
    destructiveForeground = SpentTrackerCustomColors.Light.destructiveForeground,
    border = SpentTrackerCustomColors.Light.border,
    input = SpentTrackerCustomColors.Light.input,
    ring = SpentTrackerCustomColors.Light.ring,
)

val DarkCustomColorScheme = CustomColorScheme(
    background = SpentTrackerCustomColors.Dark.background,
    foreground = SpentTrackerCustomColors.Dark.foreground,
    primary = SpentTrackerCustomColors.Dark.primary,
    primaryForeground = SpentTrackerCustomColors.Dark.primaryForeground,
    secondary = SpentTrackerCustomColors.Dark.secondary,
    secondaryForeground = SpentTrackerCustomColors.Dark.secondaryForeground,
    muted = SpentTrackerCustomColors.Dark.muted,
    mutedForeground = SpentTrackerCustomColors.Dark.mutedForeground,
    accent = SpentTrackerCustomColors.Dark.accent,
    accentForeground = SpentTrackerCustomColors.Dark.accentForeground,
    destructive = SpentTrackerCustomColors.Dark.destructive,
    destructiveForeground = SpentTrackerCustomColors.Dark.destructiveForeground,
    border = SpentTrackerCustomColors.Dark.border,
    input = SpentTrackerCustomColors.Dark.input,
    ring = SpentTrackerCustomColors.Dark.ring,
)