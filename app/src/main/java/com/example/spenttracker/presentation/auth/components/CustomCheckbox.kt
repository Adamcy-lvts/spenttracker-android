package com.example.spenttracker.presentation.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.spenttracker.presentation.theme.CustomTheme

/**
 * Custom checkbox component without Material3 dependencies
 */
@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                color = if (checked) CustomTheme.colors.primary else CustomTheme.colors.background
            )
            .border(
                width = 1.dp,
                color = if (checked) CustomTheme.colors.primary else CustomTheme.colors.border,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = CustomTheme.colors.primaryForeground,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}