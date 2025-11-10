# Android Authentication Components

## 1. Color Tokens (Color.kt)
```kotlin
package com.spentracker.app.presentation.theme

import androidx.compose.ui.graphics.Color

object SpentTrackerColors {
    // Light Theme
    object Light {
        val background = Color(0xFFFFFFFF)
        val foreground = Color(0xFF242330)
        val primary = Color(0xFF373548)
        val primaryForeground = Color(0xFFFBFBFB)
        val secondary = Color(0xFFF7F7F8)
        val secondaryForeground = Color(0xFF373548)
        val muted = Color(0xFFF7F7F8)
        val mutedForeground = Color(0xFF8C8C95)
        val accent = Color(0xFFF7F7F8)
        val accentForeground = Color(0xFF373548)
        val destructive = Color(0xFFDC2626)
        val destructiveForeground = Color(0xFFDC2626)
        val border = Color(0xFFEAEAEB)
        val input = Color(0xFFEAEAEB)
        val ring = Color(0xFFB4B4BA)
    }
    
    // Dark Theme
    object Dark {
        val background = Color(0xFF242330)
        val foreground = Color(0xFFFBFBFB)
        val primary = Color(0xFFFBFBFB)
        val primaryForeground = Color(0xFF373548)
        val secondary = Color(0xFF454351)
        val secondaryForeground = Color(0xFFFBFBFB)
        val muted = Color(0xFF454351)
        val mutedForeground = Color(0xFFB4B4BA)
        val accent = Color(0xFF454351)
        val accentForeground = Color(0xFFFBFBFB)
        val destructive = Color(0xFF991B1B)
        val destructiveForeground = Color(0xFFEF4444)
        val border = Color(0xFF454351)
        val input = Color(0xFF454351)
        val ring = Color(0xFF6F6F78)
    }
}
```

## 2. Dimensions (Dimensions.kt)
```kotlin
package com.spentracker.app.presentation.theme

import androidx.compose.ui.unit.dp

object SpentTrackerDimensions {
    // Border radius
    val radiusLg = 10.dp // --radius: 0.625rem
    val radiusMd = 8.dp
    val radiusSm = 6.dp
    
    // Spacing
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 16.dp
    val spaceLg = 24.dp
    val spaceXl = 32.dp
    val space2xl = 48.dp
    
    // Component sizes
    val inputHeight = 40.dp
    val buttonHeight = 40.dp
    val logoSize = 36.dp // 9 * 4dp = 36dp
    val formMaxWidth = 384.dp // max-w-sm
    
    // Padding
    val screenPadding = 24.dp // p-6 = 24dp
    val screenPaddingMd = 40.dp // md:p-10 = 40dp
}
```

## 3. Typography (Typography.kt)
```kotlin
package com.spentracker.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SpentTrackerTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default, // Would be Instrument Sans if available
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp, // text-xl = 20px
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp, // text-sm = 14px
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp, // text-xs = 12px
        lineHeight = 16.sp
    )
)
```

## 4. Theme Configuration (Theme.kt)
```kotlin
package com.spentracker.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SpentTrackerColors.Light.primary,
    onPrimary = SpentTrackerColors.Light.primaryForeground,
    secondary = SpentTrackerColors.Light.secondary,
    onSecondary = SpentTrackerColors.Light.secondaryForeground,
    tertiary = SpentTrackerColors.Light.accent,
    onTertiary = SpentTrackerColors.Light.accentForeground,
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

private val DarkColorScheme = darkColorScheme(
    primary = SpentTrackerColors.Dark.primary,
    onPrimary = SpentTrackerColors.Dark.primaryForeground,
    secondary = SpentTrackerColors.Dark.secondary,
    onSecondary = SpentTrackerColors.Dark.secondaryForeground,
    tertiary = SpentTrackerColors.Dark.accent,
    onTertiary = SpentTrackerColors.Dark.accentForeground,
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

@Composable
fun SpentTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to use custom colors
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpentTrackerTypography,
        content = content
    )
}
```

## 5. Auth Scaffold Component (AuthScaffold.kt)
```kotlin
package com.spentracker.app.presentation.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.spentracker.app.presentation.theme.SpentTrackerDimensions

@Composable
fun AuthScaffold(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = SpentTrackerDimensions.screenPadding,
                vertical = SpentTrackerDimensions.screenPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = SpentTrackerDimensions.formMaxWidth)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section (Logo + Title + Description)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SpentTrackerDimensions.spaceMd)
            ) {
                // Logo
                AppLogoSection()
                
                // Title and Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SpentTrackerDimensions.spaceSm)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(SpentTrackerDimensions.spaceXl))
            
            // Content (Form)
            content()
        }
    }
}

@Composable
private fun AppLogoSection() {
    // Placeholder for app logo - replace with your actual logo
    Box(
        modifier = Modifier.size(SpentTrackerDimensions.logoSize),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "💰", // Placeholder - replace with actual logo
            style = MaterialTheme.typography.displayLarge
        )
    }
}
```

## 6. Auth Text Field Component (AuthTextField.kt)
```kotlin
package com.spentracker.app.presentation.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.spentracker.app.presentation.theme.SpentTrackerDimensions

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    helperText: String? = null,
    errorText: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpentTrackerDimensions.spaceSm)
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Input Field
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            isError = errorText != null,
            shape = RoundedCornerShape(SpentTrackerDimensions.radiusLg),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(SpentTrackerDimensions.inputHeight + 16.dp) // Account for padding
        )
        
        // Helper/Error Text
        when {
            errorText != null -> {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            helperText != null -> {
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

## 7. Auth Button Component (AuthButton.kt)
```kotlin
package com.spentracker.app.presentation.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.spentracker.app.presentation.theme.SpentTrackerDimensions

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(SpentTrackerDimensions.radiusLg),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(
            horizontal = SpentTrackerDimensions.spaceMd,
            vertical = SpentTrackerDimensions.spaceSm + 4.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(SpentTrackerDimensions.buttonHeight + 8.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SpentTrackerDimensions.spaceSm)
            ) {
                leadingIcon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
```

These components replicate your Vue.js styling exactly:
- Same color scheme and CSS custom properties
- Matching spacing, typography, and dimensions
- Consistent visual hierarchy and layout structure
- Material 3 adaptation while maintaining your design identity