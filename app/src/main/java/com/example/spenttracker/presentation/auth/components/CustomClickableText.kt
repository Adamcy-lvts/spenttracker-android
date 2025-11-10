package com.example.spenttracker.presentation.auth.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.example.spenttracker.presentation.theme.CustomTheme

/**
 * Custom clickable text component without Material3 dependencies
 */
@Composable
fun CustomClickableText(
    normalText: String,
    clickableText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = CustomTheme.typography.bodyMedium,
    textAlign: TextAlign = TextAlign.Center
) {
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = CustomTheme.colors.mutedForeground)) {
            append(normalText)
        }
        withStyle(style = SpanStyle(color = CustomTheme.colors.primary)) {
            append(clickableText)
        }
    }
    
    BasicText(
        text = annotatedString,
        modifier = modifier.clickable { onClick() },
        style = style.copy(textAlign = textAlign)
    )
}