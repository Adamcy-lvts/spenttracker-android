package com.example.spenttracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.work.WorkManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
// Font Awesome Icons for beautiful navigation
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Tags
import compose.icons.fontawesomeicons.solid.ChartLine
import compose.icons.fontawesomeicons.solid.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.spenttracker.presentation.auth.AuthViewModel
import com.example.spenttracker.presentation.auth.LoginScreen
import com.example.spenttracker.presentation.auth.RegisterScreen
import com.example.spenttracker.presentation.dashboard.DashboardScreen
import com.example.spenttracker.presentation.expenses.ExpensesScreen
import com.example.spenttracker.presentation.categories.CategoriesScreen
import com.example.spenttracker.presentation.income.IncomeScreen
import com.example.spenttracker.presentation.splash.AnimatedSplashScreen
import com.example.spenttracker.presentation.theme.SpentTrackerTheme
import com.example.spenttracker.presentation.theme.CustomSpentTrackerTheme
import com.example.spenttracker.presentation.theme.CustomMaterialTheme
import com.example.spenttracker.util.ExpenseNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Navigation Item Data Class
 */
data class NavigationItem(
    val route: String,
    val title: String,
    val iconRes: Int? = null,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

/**
 * MainActivity with Authentication, Splash Screen, Dark Mode Support, and Navigation
 * Like Laravel's web.php with auth middleware
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var notificationManager: ExpenseNotificationManager
    
    @Inject
    lateinit var userContextProvider: com.example.spenttracker.data.auth.UserContextProviderImpl
    
    // Sequential permission launchers - must be registered before STARTED state
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
        proceedToNextPermissionStep()
    }
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            Log.d("MainActivity", "Location permissions granted")
        } else {
            Log.w("MainActivity", "Some location permissions denied")
        }
        proceedToNextPermissionStep()
    }
    
    // Permission flow state
    private var currentPermissionStep = 0
    private val permissionSteps = mutableListOf<() -> Unit>()
    private var onPermissionsCompleted: (() -> Unit)? = null
    
    // Temporarily removed for minimal testing
    // @Inject
    // lateinit var userPreferencesManager: UserPreferencesManager
    
    /**
     * Verify WorkManager is properly initialized with HiltWorkerFactory
     */
    private fun verifyWorkManagerInitialization() {
        try {
            val workManager = WorkManager.getInstance(this)
            Log.d("MainActivity", "✓ WorkManager instance: ${workManager != null}")
            
            // Check configuration
            val config = (application as? SpentTrackerApplication)?.workManagerConfiguration
            Log.d("MainActivity", "✓ WorkManager config: ${config != null}")
            
            // Log worker factory info
            config?.let {
                Log.d("MainActivity", "✓ WorkManager using custom factory: ${it.workerFactory != null}")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ WorkManager verification failed", e)
        }
    }
    
    /**
     * Initialize offline session restoration
     * This enables users to access their local data even without internet
     */
    private fun initializeOfflineSession() {
        Log.d("MainActivity", "=== INITIALIZING OFFLINE ACCESS ===")
        
        // Trigger session restoration from stored preferences
        userContextProvider.restoreStoredSession()
        
        Log.d("MainActivity", "✅ Offline session initialization triggered")
        Log.d("MainActivity", "Users can now access local data without network connection!")
    }
    
    /**
     * Start sequential permission flow
     */
    private fun startSequentialPermissionFlow(onCompleted: () -> Unit) {
        onPermissionsCompleted = onCompleted
        currentPermissionStep = 0
        permissionSteps.clear()
        
        // Add notification permission step (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionSteps.add {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        
        // Add location permissions step
        val hasLocationCoarse = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasLocationFine = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasLocationCoarse || !hasLocationFine) {
            permissionSteps.add {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
        
        // Start the flow
        if (permissionSteps.isNotEmpty()) {
            executeCurrentPermissionStep()
        } else {
            onPermissionsCompleted?.invoke()
        }
    }
    
    private fun executeCurrentPermissionStep() {
        if (currentPermissionStep < permissionSteps.size) {
            permissionSteps[currentPermissionStep].invoke()
        }
    }
    
    private fun proceedToNextPermissionStep() {
        currentPermissionStep++
        if (currentPermissionStep < permissionSteps.size) {
            executeCurrentPermissionStep()
        } else {
            // All permissions handled
            onPermissionsCompleted?.invoke()
        }
    }
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // 🔑 CRITICAL: Initialize offline session restoration FIRST
        initializeOfflineSession()
        
        // Verify WorkManager is properly initialized
        verifyWorkManagerInitialization()
        
        setContent {
            // App state
            var isDarkTheme by remember { mutableStateOf(true) } // Default to dark mode
            var currentScreen by remember { mutableStateOf("dashboard") }
            var showSplash by remember { mutableStateOf(true) }
            var authScreen by remember { mutableStateOf("login") } // "login" or "register"
            var showOverlayPermissionDialog by remember { mutableStateOf(false) }
            var permissionsCompleted by remember { mutableStateOf(false) }
            
            // Show logout options dialog
            var showLogoutDialog by remember { mutableStateOf(false) }
            
            // Start sequential permission flow after splash
            LaunchedEffect(showSplash) {
                if (!showSplash && !permissionsCompleted) {
                    // Small delay to let splash fully complete
                    kotlinx.coroutines.delay(500)
                    startSequentialPermissionFlow {
                        permissionsCompleted = true
                    }
                }
            }
            
            // Check if we should show the overlay permission dialog (now handled sequentially)
            LaunchedEffect(permissionsCompleted) {
                if (permissionsCompleted) {
                    val prefs = getSharedPreferences("app_preferences", MODE_PRIVATE)
                    val hasShownOverlayDialog = prefs.getBoolean("has_shown_overlay_dialog", false)
                    
                    if (!hasShownOverlayDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            showOverlayPermissionDialog = true
                        }
                    }
                }
            }
            
            // Get auth state from ViewModel
            val authViewModel: AuthViewModel = hiltViewModel()
            val authUiState by authViewModel.uiState.collectAsState()
            val isLoggedIn = authUiState.isLoggedIn
            
            when {
                showSplash -> {
                    CustomMaterialTheme {
                        AnimatedSplashScreen(
                            onSplashCompleted = { showSplash = false }
                        )
                    }
                }
                !isLoggedIn -> {
                    // Show auth screens with custom dark theme
                    CustomSpentTrackerTheme {
                        AuthFlow(
                            currentAuthScreen = authScreen,
                            onAuthScreenChange = { authScreen = it },
                            onAuthSuccess = { 
                                // Navigation will happen automatically when isLoggedIn becomes true
                            }
                        )
                    }
                }
                else -> {
                    // Show main app with custom dark theme - like Laravel's auth middleware passed
                    CustomMaterialTheme {
                        NavigationDrawerApp(
                            isDarkTheme = isDarkTheme,
                            onDarkThemeToggle = { isDarkTheme = !isDarkTheme },
                            currentScreen = currentScreen,
                            onScreenSelected = { screen -> currentScreen = screen },
                            onLogoutRequest = { showLogoutDialog = true }
                        )
                    }
                }
            }
            
            // Overlay permission dialog
            if (showOverlayPermissionDialog) {
                CustomMaterialTheme {
                    AlertDialog(
                    onDismissRequest = {
                        // Mark dialog as shown even if dismissed
                        getSharedPreferences("app_preferences", MODE_PRIVATE).edit {
                            putBoolean("has_shown_overlay_dialog", true)
                        }
                        showOverlayPermissionDialog = false
                    },
                    title = {
                        Text(
                            "Enhanced Notifications",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            "Allow SpentTracker to display reminder notifications at the top of your screen for better visibility?\n\n" +
                            "This helps ensure you never miss expense tracking reminders!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Mark dialog as shown
                                getSharedPreferences("app_preferences", MODE_PRIVATE).edit {
                                    putBoolean("has_shown_overlay_dialog", true)
                                }
                                
                                // Open overlay permission settings
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                                showOverlayPermissionDialog = false
                            }
                        ) {
                            Text("Allow Permission")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                // Mark dialog as shown even if dismissed
                                getSharedPreferences("app_preferences", MODE_PRIVATE).edit {
                                    putBoolean("has_shown_overlay_dialog", true)
                                }
                                showOverlayPermissionDialog = false
                            }
                        ) {
                            Text("Maybe Later")
                        }
                    }
                )
                }
            }
            
            // Enhanced logout dialog with offline mode option
            if (showLogoutDialog) {
                CustomMaterialTheme {
                    AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = {
                        Text(
                            "Logout Options",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            "Choose how you want to logout:\n\n" +
                            "• Complete Logout: Clear everything, require login next time\n" +
                            "• Offline Mode: Logout from server but keep local data access",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Complete logout
                            OutlinedButton(
                                onClick = {
                                    authViewModel.logout(preserveOfflineAccess = false)
                                    showLogoutDialog = false
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Complete Logout",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            
                            // Offline mode
                            Button(
                                onClick = {
                                    authViewModel.logout(preserveOfflineAccess = true)
                                    showLogoutDialog = false
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Offline Mode",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
                }
            }
        }
    }
}

/**
 * Auth Flow Component - Like Laravel's auth routes
 * Handles navigation between login and register screens
 */
@Composable
fun AuthFlow(
    currentAuthScreen: String,
    onAuthScreenChange: (String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    when (currentAuthScreen) {
        "login" -> {
            LoginScreen(
                onLoginSuccess = onAuthSuccess,
                onNavigateToRegister = { onAuthScreenChange("register") }
            )
        }
        "register" -> {
            RegisterScreen(
                onRegisterSuccess = onAuthSuccess,
                onNavigateToLogin = { onAuthScreenChange("login") }
            )
        }
    }
}

/**
 * Navigation Drawer App Component
 * Like a Laravel sidebar menu with navigation links
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawerApp(
    isDarkTheme: Boolean,
    onDarkThemeToggle: () -> Unit,
    currentScreen: String,
    onScreenSelected: (String) -> Unit,
    onLogoutRequest: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Navigation items with beautiful Font Awesome icons
    val navigationItems = listOf(
        NavigationItem("dashboard", "Dashboard", icon = FontAwesomeIcons.Solid.ChartLine),
        NavigationItem("expenses", "Expenses", icon = FontAwesomeIcons.Solid.Receipt),
        NavigationItem("income", "Income", icon = Icons.Default.AttachMoney),
        NavigationItem("budgets", "Budgets", icon = Icons.Default.AccountBalance),
        NavigationItem("categories", "Categories", icon = FontAwesomeIcons.Solid.Tags), // Beautiful tag icon!
        NavigationItem("settings", "Settings", icon = Icons.Default.Settings)
    )
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                navigationItems = navigationItems,
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    onScreenSelected(screen)
                    scope.launch {
                        drawerState.close()
                    }
                },
                isDarkTheme = isDarkTheme,
                onDarkThemeToggle = onDarkThemeToggle,
                onLogoutRequest = onLogoutRequest
            )
        }
    ) {
        // Main content with bottom navigation
        Scaffold(
            bottomBar = {
                BottomTabNavigation(
                    currentScreen = currentScreen,
                    onScreenSelected = onScreenSelected,
                    navigationItems = navigationItems
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    "dashboard" -> {
                        DashboardScreen(
                            onNavigateToExpenses = { onScreenSelected("expenses") },
                            darkTheme = isDarkTheme,
                            onDarkThemeToggle = onDarkThemeToggle,
                            onMenuClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                    "expenses" -> {
                        ExpensesScreen(
                            darkTheme = isDarkTheme,
                            onDarkThemeToggle = onDarkThemeToggle,
                            onNavigateToDashboard = { onScreenSelected("dashboard") },
                            onMenuClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                    "income" -> {
                        IncomeScreen(
                            onMenuClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                    "budgets" -> {
                        com.example.spenttracker.presentation.budgets.BudgetScreen(
                            onMenuClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                    "categories" -> {
                        CategoriesScreen(
                            darkTheme = isDarkTheme,
                            onDarkThemeToggle = onDarkThemeToggle,
                            onMenuClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                    "settings" -> {
                        com.example.spenttracker.presentation.settings.SettingsScreen(
                            darkTheme = isDarkTheme,
                            onDarkThemeToggle = onDarkThemeToggle,
                            onMenuClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Navigation Drawer Content - The sliding sidebar menu
 */
@Composable
fun NavigationDrawerContent(
    navigationItems: List<NavigationItem>,
    currentScreen: String,
    onScreenSelected: (String) -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeToggle: () -> Unit,
    onLogoutRequest: () -> Unit
) {
    // Get auth viewModel for logout functionality and user info
    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.uiState.collectAsState()
    val currentUser = authUiState.currentUser
    
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(0.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Header section with gradient-like background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // App header
                    Text(
                        text = "SpentTracker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // User info section
                    currentUser?.let { user ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.first().uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Navigation section
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            
                // Navigation items with modern styling
                navigationItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = {
                            if (item.iconRes != null) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        label = { 
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentScreen == item.route) FontWeight.SemiBold else FontWeight.Normal
                            ) 
                        },
                        selected = currentScreen == item.route,
                        onClick = { onScreenSelected(item.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom section with settings
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Dark mode toggle with better styling
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.Brightness7 else Icons.Default.Brightness2,
                            contentDescription = "Toggle theme",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { 
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { onDarkThemeToggle() },
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    selected = false,
                    onClick = { onDarkThemeToggle() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                
                // Logout button with better styling
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { 
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    selected = false,
                    onClick = onLogoutRequest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.error,
                        unselectedTextColor = MaterialTheme.colorScheme.error,
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

/**
 * Bottom Tab Navigation Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomTabNavigation(
    currentScreen: String,
    onScreenSelected: (String) -> Unit,
    navigationItems: List<NavigationItem>
) {
    // Filter to only show main screens in bottom navigation
    val bottomNavItems = navigationItems.filter {
        it.route in listOf("dashboard", "expenses", "income", "budgets")
    }
    val selectedTabIndex = bottomNavItems.indexOfFirst { it.route == currentScreen }

    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex.coerceAtLeast(0),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomNavItems.forEachIndexed { index, item ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onScreenSelected(item.route) },
                text = {
                    Text(
                        text = item.title,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                icon = {
                    if (item.iconRes != null) {
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.title,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    }
}
