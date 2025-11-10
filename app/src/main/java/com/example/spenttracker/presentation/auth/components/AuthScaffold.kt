package com.example.spenttracker.presentation.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.example.spenttracker.R
import com.example.spenttracker.presentation.theme.SpentTrackerDimensions

/**
 * Auth scaffold matching the Vue Shadcn AuthSimpleLayout
 * Provides consistent layout structure for login/register screens
 */
@Composable
fun AuthScaffold(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
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
                // Logo Section
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
    Box(
        modifier = Modifier.size(SpentTrackerDimensions.logoSize),
        contentAlignment = Alignment.Center
    ) {
        // SpentTracker app logo - wallet design
        Image(
            painter = painterResource(id = R.drawable.app_logo_card),
            contentDescription = "SpentTracker Wallet Logo",
            modifier = Modifier.size(SpentTrackerDimensions.logoSize)
        )
    }
}