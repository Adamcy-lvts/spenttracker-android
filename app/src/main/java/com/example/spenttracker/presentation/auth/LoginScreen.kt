package com.example.spenttracker.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.spenttracker.presentation.auth.components.*
import com.example.spenttracker.presentation.theme.CustomSpentTrackerTheme
import com.example.spenttracker.presentation.theme.CustomTheme
import com.example.spenttracker.util.rememberLocationPermissionHandler
import com.example.spenttracker.util.PhoneNumberValidator
import kotlinx.coroutines.delay

/**
 * Login screen built from scratch without Material3
 * Based on markdown specifications for pixel-perfect design matching
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    CustomSpentTrackerTheme {
        var login by remember { mutableStateOf("") }
        var loginError by remember { mutableStateOf<String?>(null) }
        var password by remember { mutableStateOf("") }
        val context = LocalContext.current

        val uiState by viewModel.uiState.collectAsState()
        
        // Location permission handler
        val locationPermissionHandler = rememberLocationPermissionHandler(
            onPermissionGranted = {
                // Location granted, proceed with login
            },
            onPermissionDenied = {
                // Location denied, still allow login but without location data
            }
        )

        // Handle login success
        LaunchedEffect(uiState.isLoggedIn) {
            if (uiState.isLoggedIn) {
                onLoginSuccess()
            }
        }
        
        // Debounced login validation - validate after user stops typing for 800ms
        LaunchedEffect(login) {
            if (login.isNotBlank()) {
                delay(800) // Wait 800ms after user stops typing
                loginError = validateLoginField(login)
            }
        }
        
        // Location permission will be handled by sequential permission manager in MainActivity
        // Remove automatic request to avoid conflicts

        CustomAuthScaffold(
            title = "Log in to your account",
            description = "Enter your email or phone number and password below to log in"
        ) {
            LoginForm(
                login = login,
                onLoginChange = { newValue ->
                    login = newValue
                    loginError = null
                },
                password = password,
                onPasswordChange = { password = it },
                isLoading = uiState.isLoading,
                loginError = loginError,
                errorMessage = uiState.errorMessage,
                successMessage = uiState.successMessage,
                onLoginClick = { viewModel.login(login, password) },
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToForgotPassword = onNavigateToForgotPassword,
                canResetPassword = true
            )
        }

        // Show location permission dialogs
        locationPermissionHandler.LocationPermissionDialogs()
    }
}

@Composable
private fun LoginForm(
    login: String,
    onLoginChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    loginError: String?,
    errorMessage: String?,
    successMessage: String?,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    canResetPassword: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Message with Animation
        AnimatedVisibility(
            visible = successMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CustomText(
                    text = successMessage ?: "",
                    color = CustomTheme.colors.primary,
                    style = CustomTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // Error Message with Animation
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CustomText(
                    text = errorMessage ?: "",
                    color = CustomTheme.colors.destructive,
                    style = CustomTheme.typography.bodyMedium
                )
            }
        }
        
        // Form Fields
        Column(
            verticalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceLg)
        ) {
            // Login Field (Email or Phone)
            CustomTextField(
                value = login,
                onValueChange = onLoginChange,
                label = "Email or Phone",
                placeholder = "email@example.com or +234801234567",
                errorText = loginError,
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email
            )

            // Password Field with Forgot Password Link
            Column(
                verticalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceSm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomText(
                        text = "Password",
                        style = CustomTheme.typography.labelLarge,
                        color = CustomTheme.colors.foreground
                    )

                    if (canResetPassword) {
                        Box(
                            modifier = Modifier
                                .padding(0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CustomClickableText(
                                normalText = "",
                                clickableText = "Forgot password?",
                                onClick = onNavigateToForgotPassword,
                                style = CustomTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                CustomTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "", // Label is handled above
                    placeholder = "Password",
                    isPassword = true,
                    leadingIcon = Icons.Outlined.Lock,
                    keyboardType = KeyboardType.Password
                )
            }

            // Login Button
            CustomButton(
                text = "Log in",
                onClick = onLoginClick,
                loading = isLoading,
                enabled = login.isNotBlank() && password.isNotBlank() && loginError == null,
                modifier = Modifier.padding(top = CustomTheme.dimensions.spaceMd)
            )
        }

        // Register Link
        CustomClickableText(
            normalText = "Don't have an account? ",
            clickableText = "Sign up",
            onClick = onNavigateToRegister,
            style = CustomTheme.typography.bodyMedium
        )
    }
}

/**
 * Validate login field - accepts email or Nigerian phone number
 */
private fun validateLoginField(input: String): String? {
    if (input.isBlank()) return null
    
    // Check if it's an email
    val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()
    
    // Check if it's a phone number
    val isPhoneNumber = PhoneNumberValidator.isValidNigerianPhoneNumber(input)
    
    return when {
        isEmail || isPhoneNumber -> null
        input.contains("@") -> "Please enter a valid email address"
        input.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' } -> {
            PhoneNumberValidator.getValidationErrorMessage(input)
        }
        else -> "Please enter a valid email or Nigerian phone number"
    }
}