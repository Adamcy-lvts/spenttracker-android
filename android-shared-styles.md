# Shared Styling Guide for Android & Web

This document outlines the shared design system between your Vue.js web app and Android app to ensure visual consistency.

## Design Tokens

### Colors (from CSS variables)
```kotlin
// Light Theme Colors
object LightColors {
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
    val border = Color(0xFFEAEAEB) // oklch(0.92 0.004 286.32)
    val input = Color(0xFFEAEAEB)
    val ring = Color(0xFFB4B4BA) // oklch(0.705 0.015 286.067)
}

// Dark Theme Colors
object DarkColors {
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
    val border = Color(0xFF454351) // oklch(0.274 0.006 286.033)
    val input = Color(0xFF454351)
    val ring = Color(0xFF6F6F78) // oklch(0.442 0.017 285.786)
}
```

### Typography
```kotlin
// Font: Instrument Sans (fallback: system default)
object Typography {
    val displayLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Medium
    )
    val headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium
    )
    val bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )
    val bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    )
    val labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
    val labelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    )
}
```

### Layout & Spacing
```kotlin
object Dimensions {
    val borderRadius = 10.dp // --radius: 0.625rem
    val borderRadiusMd = 8.dp // calc(var(--radius) - 2px)
    val borderRadiusSm = 6.dp // calc(var(--radius) - 4px)
    
    // Standard spacing scale
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 16.dp
    val spaceLg = 24.dp
    val spaceXl = 32.dp
    val space2xl = 48.dp
    
    // Component specific
    val inputHeight = 40.dp
    val buttonHeight = 40.dp
    val formMaxWidth = 400.dp // max-w-sm (384px)
}
```

## Layout Structure

### Web Layout Analysis
Your web login/register screens have this structure:
```
AuthSimpleLayout
├── Centered container (min-h-svh, max-w-sm)
│   ├── Logo + Title section
│   │   ├── App Logo Icon (9x9, rounded)
│   │   ├── Title (text-xl font-medium) 
│   │   └── Description (text-sm text-muted-foreground)
│   └── Form
│       ├── Input fields (gap-2, gap-6 between groups)
│       ├── Submit button (w-full, mt-4)
│       └── Bottom link (text-center, text-sm)
```

### Key Design Elements
1. **Centered Layout**: Full-height center alignment with padding
2. **Logo Section**: Icon + title + description in vertical stack
3. **Form Spacing**: 24dp gap between field groups, 8dp within groups
4. **Input Style**: Outlined inputs with subtle borders
5. **Button Style**: Full-width primary button
6. **Typography Hierarchy**: Clear size and weight differentiation
7. **Color Scheme**: Uses CSS custom properties for theming

## Android Implementation Structure

```
presentation/
├── theme/
│   ├── Color.kt (design tokens)
│   ├── Typography.kt
│   ├── Dimension.kt
│   └── Theme.kt
├── auth/
│   ├── login/
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── register/
│   │   ├── RegisterScreen.kt
│   │   └── RegisterViewModel.kt
│   └── components/
│       ├── AuthScaffold.kt (equivalent to AuthSimpleLayout)
│       ├── AuthTextField.kt (styled input field)
│       └── AuthButton.kt (styled button)
```

This ensures pixel-perfect consistency between web and Android versions.