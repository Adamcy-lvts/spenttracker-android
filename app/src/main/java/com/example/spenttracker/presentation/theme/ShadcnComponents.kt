package com.example.spenttracker.presentation.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shadcn/ui inspired button components for consistent styling
 */

@Composable
fun ShadcnButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ShadcnButtonVariant = ShadcnButtonVariant.Default,
    size: ShadcnButtonSize = ShadcnButtonSize.Default,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    val colors = when (variant) {
        ShadcnButtonVariant.Default -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        )
        ShadcnButtonVariant.Destructive -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.38f)
        )
        ShadcnButtonVariant.Outline -> ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        ShadcnButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.38f)
        )
        ShadcnButtonVariant.Ghost -> ButtonDefaults.textButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }

    val buttonModifier = modifier.then(
        when (size) {
            ShadcnButtonSize.Small -> Modifier.height(32.dp)
            ShadcnButtonSize.Default -> Modifier.height(36.dp)
            ShadcnButtonSize.Large -> Modifier.height(40.dp)
            ShadcnButtonSize.Icon -> Modifier.size(36.dp)
        }
    )

    val contentPadding = when (size) {
        ShadcnButtonSize.Small -> PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ShadcnButtonSize.Default -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ShadcnButtonSize.Large -> PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ShadcnButtonSize.Icon -> PaddingValues(0.dp)
    }

    when (variant) {
        ShadcnButtonVariant.Outline -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = colors,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(6.dp),
                contentPadding = contentPadding
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                content()
            }
        }
        ShadcnButtonVariant.Ghost -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = colors,
                shape = RoundedCornerShape(6.dp),
                contentPadding = contentPadding
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                content()
            }
        }
        else -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors = colors,
                shape = RoundedCornerShape(6.dp),
                contentPadding = contentPadding,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    disabledElevation = 0.dp
                )
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun ShadcnTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        errorLabelColor = MaterialTheme.colorScheme.error,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        errorTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        errorLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        errorTrailingIconColor = MaterialTheme.colorScheme.error
    ),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Normal
        ),
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = RoundedCornerShape(6.dp),
        colors = colors
    )
}

/**
 * Button variant types matching shadcn/ui
 */
enum class ShadcnButtonVariant {
    Default,
    Destructive,
    Outline,
    Secondary,
    Ghost
}

/**
 * Button size types matching shadcn/ui
 */
enum class ShadcnButtonSize {
    Small,
    Default,
    Large,
    Icon
}