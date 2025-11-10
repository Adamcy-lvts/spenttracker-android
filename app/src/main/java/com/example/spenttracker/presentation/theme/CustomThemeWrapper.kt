package com.example.spenttracker.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Wrapper that applies our custom dark colors to Material3 components
 * This allows existing screens to use Material3 components with our dark theme
 */
@Composable
fun CustomMaterialTheme(
    content: @Composable () -> Unit
) {
    val customDarkColorScheme = darkColorScheme(
        // Main surfaces - true dark like your web app
        background = SpentTrackerCustomColors.Dark.background, // Pure black
        onBackground = SpentTrackerCustomColors.Dark.foreground, // White text
        surface = SpentTrackerCustomColors.Dark.muted, // Dark gray for cards
        onSurface = SpentTrackerCustomColors.Dark.foreground, // White text
        
        // Primary - white on dark (matches your login/register)
        primary = SpentTrackerCustomColors.Dark.primary, // White primary
        onPrimary = SpentTrackerCustomColors.Dark.primaryForeground, // Dark text
        
        // Secondary surfaces
        secondary = SpentTrackerCustomColors.Dark.secondary, // Dark gray
        onSecondary = SpentTrackerCustomColors.Dark.secondaryForeground, // White text
        
        // Surface variants for depth
        surfaceVariant = SpentTrackerCustomColors.Dark.secondary, // Slightly raised surfaces
        onSurfaceVariant = SpentTrackerCustomColors.Dark.mutedForeground, // Muted text
        
        // Containers
        primaryContainer = SpentTrackerCustomColors.Dark.secondary,
        onPrimaryContainer = SpentTrackerCustomColors.Dark.foreground,
        secondaryContainer = SpentTrackerCustomColors.Dark.muted,
        onSecondaryContainer = SpentTrackerCustomColors.Dark.foreground,
        
        // Borders and outlines
        outline = SpentTrackerCustomColors.Dark.border, // Subtle borders
        outlineVariant = SpentTrackerCustomColors.Dark.border,
        
        // Status colors
        error = SpentTrackerCustomColors.Dark.destructive,
        onError = SpentTrackerCustomColors.Dark.destructiveForeground,
        errorContainer = SpentTrackerCustomColors.Dark.secondary,
        onErrorContainer = SpentTrackerCustomColors.Dark.destructive,
        
        // Surface tint (for Material3 elevation)
        surfaceTint = SpentTrackerCustomColors.Dark.primary
    )
    
    MaterialTheme(
        colorScheme = customDarkColorScheme,
        typography = Typography, // Use existing typography
        content = content
    )
}