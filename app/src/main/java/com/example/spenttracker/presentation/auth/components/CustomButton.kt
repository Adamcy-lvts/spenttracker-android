package com.example.spenttracker.presentation.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.spenttracker.presentation.theme.CustomTheme

/**
 * Custom button component without Material3 dependencies
 * Based on AuthButton specifications from markdown
 */
@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CustomTheme.dimensions.buttonHeight + 8.dp)
            .clip(RoundedCornerShape(CustomTheme.dimensions.radiusLg))
            .background(
                color = if (enabled && !loading) 
                    CustomTheme.colors.primary 
                else 
                    CustomTheme.colors.muted
            )
            .clickable(enabled = enabled && !loading) { onClick() }
            .padding(
                horizontal = CustomTheme.dimensions.spaceMd,
                vertical = CustomTheme.dimensions.spaceSm + 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = CustomTheme.colors.primaryForeground,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) CustomTheme.colors.primaryForeground else CustomTheme.colors.mutedForeground,
                        modifier = Modifier.size(16.dp)
                    )
                }
                CustomText(
                    text = text,
                    style = CustomTheme.typography.labelLarge,
                    color = if (enabled) CustomTheme.colors.primaryForeground else CustomTheme.colors.mutedForeground
                )
            }
        }
    }
}