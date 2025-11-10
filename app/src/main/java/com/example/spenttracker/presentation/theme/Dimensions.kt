package com.example.spenttracker.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Design system dimensions matching the Vue Shadcn web app
 * All values correspond to the CSS custom properties and Tailwind scale
 */
object SpentTrackerDimensions {
    // Border radius matching --radius: 0.625rem (10px)
    val radiusLg = 10.dp // --radius: 0.625rem
    val radiusMd = 8.dp // calc(var(--radius) - 2px)
    val radiusSm = 6.dp // calc(var(--radius) - 4px)
    
    // Standard spacing scale (Tailwind spacing)
    val spaceXs = 4.dp // space-1
    val spaceSm = 8.dp // space-2
    val spaceMd = 16.dp // space-4
    val spaceLg = 24.dp // space-6
    val spaceXl = 32.dp // space-8
    val space2xl = 48.dp // space-12
    
    // Component sizes matching web version
    val inputHeight = 36.dp // Slimmer to match web appearance
    val buttonHeight = 40.dp // h-10 = 40px (2.5rem)
    val logoSize = 64.dp // Larger logo to match web version better
    val formMaxWidth = 384.dp // max-w-sm = 384px (24rem)
    
    // Padding matching web layout
    val screenPadding = 24.dp // p-6 = 24dp
    val screenPaddingMd = 40.dp // md:p-10 = 40dp
}