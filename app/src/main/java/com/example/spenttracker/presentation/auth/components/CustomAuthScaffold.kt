package com.example.spenttracker.presentation.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.spenttracker.R
import com.example.spenttracker.presentation.theme.CustomTheme

/**
 * Custom auth scaffold without Material3 dependencies
 * Based on AuthSimpleLayout from markdown specifications
 */
@Composable
fun CustomAuthScaffold(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CustomTheme.colors.background)
            .padding(
                horizontal = CustomTheme.dimensions.screenPadding,
                vertical = CustomTheme.dimensions.screenPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CustomTheme.dimensions.formMaxWidth)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section (Logo + Title + Description)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceMd)
            ) {
                // Logo Section with App Name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceSm)
                ) {
                    CustomAppLogoSection()
                    CustomText(
                        text = "SpentTracker",
                        style = CustomTheme.typography.titleMedium,
                        color = CustomTheme.colors.foreground,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Title and Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceSm)
                ) {
                    CustomText(
                        text = title,
                        style = CustomTheme.typography.headlineMedium,
                        color = CustomTheme.colors.foreground,
                        textAlign = TextAlign.Center
                    )
                    CustomText(
                        text = description,
                        style = CustomTheme.typography.bodyMedium,
                        color = CustomTheme.colors.mutedForeground,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(CustomTheme.dimensions.spaceXl))
            
            // Content (Form)
            content()
        }
    }
}

@Composable
private fun CustomAppLogoSection() {
    Box(
        modifier = Modifier.size(CustomTheme.dimensions.logoSize),
        contentAlignment = Alignment.Center
    ) {
        // SpentTracker app logo - wallet design optimized for dark mode
        Image(
            painter = painterResource(id = R.drawable.app_logo_dark),
            contentDescription = "SpentTracker Wallet Logo",
            modifier = Modifier.size(CustomTheme.dimensions.logoSize)
        )
    }
}