package com.example.spenttracker.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Shadcn/ui inspired color scheme
 * Matches the Vue.js web app design
 */

// Primary colors (matching shadcn primary)
private val Primary = Color(0xFF0F172A) // slate-900
private val PrimaryVariant = Color(0xFF1E293B) // slate-800
private val Secondary = Color(0xFF64748B) // slate-500
private val SecondaryVariant = Color(0xFF475569) // slate-600

// Background colors
private val Background = Color(0xFFFAFAFA) // neutral-50
private val Surface = Color(0xFFFFFFFF) // white
private val SurfaceVariant = Color(0xFFF1F5F9) // slate-100

// Text colors
private val OnPrimary = Color(0xFFFFFFFF)
private val OnSecondary = Color(0xFFFFFFFF)
private val OnBackground = Color(0xFF0F172A) // slate-900
private val OnSurface = Color(0xFF0F172A) // slate-900
private val OnSurfaceVariant = Color(0xFF64748B) // slate-500

// Accent colors
private val Accent = Color(0xFF3B82F6) // blue-500 (matches shadcn accent)
private val AccentVariant = Color(0xFF2563EB) // blue-600

// Status colors
private val Error = Color(0xFFEF4444) // red-500
private val Success = Color(0xFF10B981) // emerald-500
private val Warning = Color(0xFFF59E0B) // amber-500

// Border colors
private val Outline = Color(0xFFE2E8F0) // slate-200
private val OutlineVariant = Color(0xFFCBD5E1) // slate-300

val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = OnPrimary,
    
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = OnPrimary,
    
    tertiary = Accent,
    onTertiary = OnPrimary,
    tertiaryContainer = AccentVariant,
    onTertiaryContainer = OnPrimary,
    
    background = Background,
    onBackground = OnBackground,
    
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    
    error = Error,
    onError = OnPrimary,
    
    outline = Outline,
    outlineVariant = OutlineVariant
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE5E7EB), // Light text for dark theme
    onPrimary = Color(0xFF0F0F0F), // Very dark
    primaryContainer = Color(0xFF1E1E1E), // Card backgrounds
    onPrimaryContainer = Color(0xFFE5E7EB),
    
    secondary = Color(0xFF9CA3AF), // Muted text
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF1E1E1E), 
    onSecondaryContainer = Color(0xFFE5E7EB),
    
    tertiary = Color(0xFF60A5FA), // Accent blue
    onTertiary = Color(0xFF0F0F0F),
    tertiaryContainer = Color(0xFF3B82F6),
    onTertiaryContainer = Color(0xFFFFFFFF),
    
    background = Color(0xFF0F0F0F), // True dark background
    onBackground = Color(0xFFE5E7EB), // Light text
    
    surface = Color(0xFF121212), // Card/surface background
    onSurface = Color(0xFFE5E7EB), // Text on surfaces
    surfaceVariant = Color(0xFF1E1E1E), // Elevated surfaces
    onSurfaceVariant = Color(0xFF9CA3AF), // Muted text
    
    error = Color(0xFFF87171), // Error red
    onError = Color(0xFF0F0F0F),
    
    outline = Color(0xFF374151), // Borders
    outlineVariant = Color(0xFF4B5563) // Subtle borders
)