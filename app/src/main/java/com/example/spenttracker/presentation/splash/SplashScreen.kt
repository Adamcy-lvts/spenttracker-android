package com.example.spenttracker.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Animated Splash Screen with Company Logo, Name, and Slogan
 * Features assembly animation for logo parts
 */
@Composable
fun AnimatedSplashScreen(
    onSplashCompleted: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Animation states
    val orangePartOffsetX by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -300f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "orange_slide"
    )
    
    val bluePartOffsetX by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 300f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "blue_slide"
    )
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = LinearEasing
        ),
        label = "logo_alpha"
    )
    
    val companyNameAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "company_name_alpha"
    )
    
    val sloganOffsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 50f,
        animationSpec = tween(
            durationMillis = 700,
            delayMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "slogan_slide_up"
    )
    
    val sloganAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 700,
            delayMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "slogan_alpha"
    )
    
    val websiteOffsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 30f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "website_slide_up"
    )
    
    val websiteAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "website_alpha"
    )
    
    // Start animations when screen loads
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000) // Show splash for 3 seconds
        onSplashCompleted()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Company Logo
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Orange part (slides in from left)
                Image(
                    painter = painterResource(id = com.example.spenttracker.R.drawable.company_logo_orange_part),
                    contentDescription = "Orange part",
                    modifier = Modifier
                        .size(120.dp)
                        .offset(x = orangePartOffsetX.dp)
                        .alpha(logoAlpha)
                )
                
                // Blue part (slides in from right)
                Image(
                    painter = painterResource(id = com.example.spenttracker.R.drawable.company_logo_blue_part),
                    contentDescription = "Blue part",
                    modifier = Modifier
                        .size(120.dp)
                        .offset(x = bluePartOffsetX.dp)
                        .alpha(logoAlpha)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Company Name
            Text(
                text = "Devcentric",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF517ABD), // Blue color from logo
                modifier = Modifier.alpha(companyNameAlpha)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Company Slogan
            Text(
                text = "Driving Digital Transformation",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280), // Gray color
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = sloganOffsetY.dp)
                    .alpha(sloganAlpha)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Company Website
            Text(
                text = "devcentricstudio.com",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF517ABD), // Blue color from logo
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = websiteOffsetY.dp)
                    .alpha(websiteAlpha)
            )
        }
    }
}