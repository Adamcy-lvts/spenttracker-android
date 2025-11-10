package com.example.spenttracker.presentation.auth.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Custom text component without Material3 dependencies
 */
@Composable
fun CustomText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    textAlign: TextAlign? = null
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = if (color != Color.Unspecified) color else style.color,
            textAlign = textAlign ?: style.textAlign
        )
    )
}