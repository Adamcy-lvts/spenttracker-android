package com.example.spenttracker.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
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
 * Register screen built from scratch without Material3
 * Based on markdown specifications for pixel-perfect design matching
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    CustomSpentTrackerTheme {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var emailError by remember { mutableStateOf<String?>(null) }
        var phoneNumber by remember { mutableStateOf("") }
        var phoneNumberError by remember { mutableStateOf<String?>(null) }
        var generalError by remember { mutableStateOf<String?>(null) }
        var password by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf<String?>(null) }
        var confirmPassword by remember { mutableStateOf("") }
        var confirmPasswordError by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        val uiState by viewModel.uiState.collectAsState()
        
        // Location permission handler
        val locationPermissionHandler = rememberLocationPermissionHandler(
            onPermissionGranted = {
                // Location granted, proceed with registration
            },
            onPermissionDenied = {
                // Location denied, still allow registration but without location data
            }
        )

        LaunchedEffect(uiState.isRegistered) {
            if (uiState.isRegistered) {
                onRegisterSuccess()
            }
        }
        
        // Debounced email validation - validate after user stops typing for 800ms
        LaunchedEffect(email) {
            if (email.isNotBlank()) {
                delay(800) // Wait 800ms after user stops typing
                emailError = if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    "Please enter a valid email address"
                } else null
            }
        }
        
        // Debounced phone validation - validate after user stops typing for 800ms
        LaunchedEffect(phoneNumber) {
            if (phoneNumber.isNotBlank()) {
                delay(800) // Wait 800ms after user stops typing
                phoneNumberError = PhoneNumberValidator.getValidationErrorMessage(phoneNumber)
            }
        }

        // Real-time password validation
        LaunchedEffect(password) {
            passwordError = when {
                password.isEmpty() -> null
                password.length < 8 -> "Password must be at least 8 characters"
                else -> null
            }
        }

        // Real-time password match validation
        LaunchedEffect(password, confirmPassword) {
            confirmPasswordError = when {
                confirmPassword.isEmpty() -> null
                confirmPassword != password -> "Passwords do not match"
                else -> null
            }
        }
        
        // Location permission will be handled by sequential permission manager in MainActivity
        // Remove automatic request to avoid conflicts

        CustomAuthScaffold(
            title = "Create an account",
            description = "💡 You can provide either email, phone, or both for account access"
        ) {
            RegisterForm(
                name = name,
                onNameChange = { name = it },
                email = email,
                onEmailChange = { newValue ->
                    email = newValue
                    emailError = null
                    if (newValue.isNotBlank() || phoneNumber.isNotBlank()) {
                        generalError = null
                    }
                },
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { newValue ->
                    phoneNumber = newValue
                    phoneNumberError = null
                    if (email.isNotBlank() || newValue.isNotBlank()) {
                        generalError = null
                    }
                },
                password = password,
                onPasswordChange = {
                    password = it
                    passwordError = null
                },
                passwordConfirmation = confirmPassword,
                onPasswordConfirmationChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                },
                isLoading = uiState.isLoading,
                emailError = emailError,
                phoneNumberError = phoneNumberError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError,
                generalError = generalError,
                errorMessage = uiState.errorMessage,
                successMessage = uiState.successMessage,
                onRegisterClick = {
                    if (email.isBlank() && phoneNumber.isBlank()) {
                        generalError = "Please provide either an email address or phone number"
                        return@RegisterForm
                    }
                    viewModel.register(name, email, phoneNumber, password, confirmPassword)
                },
                onNavigateToLogin = onNavigateToLogin
            )
        }
        
        // Show location permission dialogs
        locationPermissionHandler.LocationPermissionDialogs()
    }
}

@Composable
private fun RegisterForm(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordConfirmation: String,
    onPasswordConfirmationChange: (String) -> Unit,
    isLoading: Boolean,
    emailError: String?,
    phoneNumberError: String?,
    passwordError: String?,
    confirmPasswordError: String?,
    generalError: String?,
    errorMessage: String?,
    successMessage: String?,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit
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

        // General validation error with Animation
        AnimatedVisibility(
            visible = generalError != null,
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
                    text = generalError ?: "",
                    color = CustomTheme.colors.destructive,
                    style = CustomTheme.typography.bodyMedium
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
            // Name Field
            CustomTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Name",
                placeholder = "Full name",
                leadingIcon = Icons.Outlined.Person,
                keyboardType = KeyboardType.Text
            )

            // Email Field
            CustomTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Email",
                placeholder = "email@example.com",
                errorText = emailError,
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email
            )

            // Phone Number Field  
            CustomTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = "Phone",
                placeholder = "+234801234567",
                errorText = phoneNumberError,
                leadingIcon = Icons.Outlined.Phone,
                keyboardType = KeyboardType.Phone
            )

            // Info text below phone field
//            CustomText(
//                text = "💡 You can provide either email, phone, or both for account access",
//                style = CustomTheme.typography.labelMedium,
//                color = CustomTheme.colors.mutedForeground,
//                modifier = Modifier.padding(start = CustomTheme.dimensions.spaceSm)
//            )

            // Password Field with Strength Indicator
            Column(
                verticalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceSm)
            ) {
                CustomTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    placeholder = "At least 8 characters",
                    errorText = passwordError,
                    isPassword = true,
                    leadingIcon = Icons.Outlined.Lock,
                    keyboardType = KeyboardType.Password
                )

                // Password Strength Indicator
                if (password.isNotEmpty()) {
                    PasswordStrengthIndicator(password = password)
                }
            }

            // Password Confirmation Field
            CustomTextField(
                value = passwordConfirmation,
                onValueChange = onPasswordConfirmationChange,
                label = "Confirm password",
                placeholder = "Re-enter your password",
                errorText = confirmPasswordError,
                isPassword = true,
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password
            )

            // Register Button
            CustomButton(
                text = "Create account",
                onClick = onRegisterClick,
                loading = isLoading,
                enabled = name.isNotBlank() &&
                         (email.isNotBlank() || phoneNumber.isNotBlank()) &&
                         password.length >= 8 &&
                         passwordConfirmation == password &&
                         emailError == null &&
                         phoneNumberError == null,
                modifier = Modifier.padding(top = CustomTheme.dimensions.spaceSm)
            )
        }

        // Login Link
        CustomClickableText(
            normalText = "Already have an account? ",
            clickableText = "Log in",
            onClick = onNavigateToLogin,
            style = CustomTheme.typography.bodyMedium
        )
    }
}

/**
 * Password Strength Indicator Component
 */
@Composable
private fun PasswordStrengthIndicator(password: String) {
    val strength = calculatePasswordStrength(password)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Strength Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (index < strength.level) strength.color else CustomTheme.colors.muted,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        // Strength Label
        CustomText(
            text = strength.label,
            style = CustomTheme.typography.labelMedium,
            color = strength.color
        )
    }
}

/**
 * Password Strength Data
 */
private data class PasswordStrength(
    val level: Int,
    val label: String,
    val color: androidx.compose.ui.graphics.Color
)

/**
 * Calculate password strength (0-4 levels)
 */
@Composable
private fun calculatePasswordStrength(password: String): PasswordStrength {
    var score = 0

    // Length check
    when {
        password.length >= 12 -> score += 2
        password.length >= 8 -> score += 1
    }

    // Character variety checks
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    // Cap at 4
    score = score.coerceIn(0, 4)

    return when (score) {
        0, 1 -> PasswordStrength(
            level = 1,
            label = "Weak password",
            color = CustomTheme.colors.destructive
        )
        2 -> PasswordStrength(
            level = 2,
            label = "Fair password",
            color = androidx.compose.ui.graphics.Color(0xFFF59E0B) // Orange
        )
        3 -> PasswordStrength(
            level = 3,
            label = "Good password",
            color = androidx.compose.ui.graphics.Color(0xFF10B981) // Green
        )
        else -> PasswordStrength(
            level = 4,
            label = "Strong password",
            color = androidx.compose.ui.graphics.Color(0xFF059669) // Dark Green
        )
    }
}

