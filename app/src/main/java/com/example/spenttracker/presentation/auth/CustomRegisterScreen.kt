package com.example.spenttracker.presentation.auth

import androidx.compose.foundation.layout.*
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
 * Custom register screen built from scratch without Material3
 * Based on markdown specifications for pixel-perfect design matching
 */
@Composable
fun CustomRegisterScreen(
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
        var confirmPassword by remember { mutableStateOf("") }
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
        
        // Request location permission on first launch (but don't block registration)
        LaunchedEffect(Unit) {
            if (!locationPermissionHandler.hasLocationPermission()) {
                locationPermissionHandler.requestLocationPermission()
            }
        }

        CustomAuthScaffold(
            title = "Create an account",
            description = "Enter your details below to create your account"
        ) {
            CustomRegisterForm(
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
                onPasswordChange = { password = it },
                passwordConfirmation = confirmPassword,
                onPasswordConfirmationChange = { confirmPassword = it },
                isLoading = uiState.isLoading,
                emailError = emailError,
                phoneNumberError = phoneNumberError,
                generalError = generalError,
                errorMessage = uiState.errorMessage,
                onRegisterClick = { 
                    if (email.isBlank() && phoneNumber.isBlank()) {
                        generalError = "Please provide either an email address or phone number"
                        return@CustomRegisterForm
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
private fun CustomRegisterForm(
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
    generalError: String?,
    errorMessage: String?,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CustomTheme.dimensions.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // General validation error (email or phone required)
        if (generalError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CustomText(
                    text = generalError,
                    color = CustomTheme.colors.destructive,
                    style = CustomTheme.typography.bodyMedium
                )
            }
        }

        // Error Message
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CustomText(
                    text = errorMessage,
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
            CustomText(
                text = "💡 You can provide either email, phone, or both for account access",
                style = CustomTheme.typography.labelMedium,
                color = CustomTheme.colors.mutedForeground,
                modifier = Modifier.padding(start = CustomTheme.dimensions.spaceSm)
            )

            // Password Field
            CustomTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                placeholder = "Password",
                isPassword = true,
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password
            )

            // Password Confirmation Field
            CustomTextField(
                value = passwordConfirmation,
                onValueChange = onPasswordConfirmationChange,
                label = "Confirm password",
                placeholder = "Confirm password",
                isPassword = true,
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password
            )

            // Register Button
            CustomButton(
                text = "Create account",
                onClick = onRegisterClick,
                loading = isLoading,
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

