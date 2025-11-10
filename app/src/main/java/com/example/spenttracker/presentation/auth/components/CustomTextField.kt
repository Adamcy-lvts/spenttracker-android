package com.example.spenttracker.presentation.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.spenttracker.presentation.theme.CustomTheme

/**
 * Custom text field component without Material3 dependencies
 * Based on AuthTextField specifications from markdown
 */
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helperText: String? = null,
    errorText: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceSm)
    ) {
        // Label
        if (label.isNotEmpty()) {
            CustomText(
                text = label,
                style = CustomTheme.typography.labelLarge,
                color = CustomTheme.colors.foreground
            )
        }
        
        // Input Field Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CustomTheme.dimensions.inputHeight + 16.dp)
                .background(
                    color = CustomTheme.colors.input,
                    shape = RoundedCornerShape(CustomTheme.dimensions.radiusLg)
                )
                .border(
                    width = 1.dp,
                    color = if (errorText != null) CustomTheme.colors.destructive else CustomTheme.colors.border,
                    shape = RoundedCornerShape(CustomTheme.dimensions.radiusLg)
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon
            leadingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CustomTheme.colors.mutedForeground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Text Input
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = CustomTheme.typography.bodyMedium.copy(
                        color = if (enabled) CustomTheme.colors.foreground else CustomTheme.colors.mutedForeground
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                    visualTransformation = if (isPassword && !passwordVisible)
                        PasswordVisualTransformation()
                    else
                        VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Placeholder
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    CustomText(
                        text = placeholder,
                        style = CustomTheme.typography.bodyMedium,
                        color = CustomTheme.colors.mutedForeground
                    )
                }
            }
            
            // Password Visibility Toggle
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Filled.VisibilityOff
                        else
                            Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible)
                            "Hide password"
                        else
                            "Show password",
                        tint = CustomTheme.colors.mutedForeground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Helper/Error Text
        when {
            errorText != null -> {
                CustomText(
                    text = errorText,
                    style = CustomTheme.typography.labelMedium,
                    color = CustomTheme.colors.destructive
                )
            }
            helperText != null -> {
                CustomText(
                    text = helperText,
                    style = CustomTheme.typography.labelMedium,
                    color = CustomTheme.colors.mutedForeground
                )
            }
        }
    }
}