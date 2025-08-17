package com.example.spenttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.spenttracker.presentation.dashboard.DashboardScreen
import com.example.spenttracker.presentation.expenses.ExpensesScreen
import com.example.spenttracker.presentation.categories.CategoriesScreen
import com.example.spenttracker.presentation.splash.AnimatedSplashScreen
import com.example.spenttracker.presentation.theme.SpentTrackerTheme
import kotlinx.coroutines.launch

/**
 * MainActivity with Company Logo Splash Screen, Dark Mode Support, and Navigation
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        setContent {
            // App state
            var isDarkTheme by remember { mutableStateOf(false) }
            var currentScreen by remember { mutableStateOf("dashboard") }
            var showSplash by remember { mutableStateOf(true) }
            
            SpentTrackerTheme(
                darkTheme = isDarkTheme
            ) {
                if (showSplash) {
                    AnimatedSplashScreen(
                        onSplashCompleted = { showSplash = false }
                    )
                } else {
                    NavigationDrawerApp(
                        isDarkTheme = isDarkTheme,
                        onDarkThemeToggle = { isDarkTheme = !isDarkTheme },
                        currentScreen = currentScreen,
                        onScreenSelected = { screen -> currentScreen = screen }
                    )
                }
            }
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
    onScreenSelected: (String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Navigation items (like Laravel routes)
    val navigationItems = listOf(
        NavigationItem("dashboard", "Dashboard", Icons.Default.DateRange),
        NavigationItem("expenses", "Expenses", Icons.Default.ShoppingCart),
        NavigationItem("categories", "Categories", Icons.Default.Settings)
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
                onDarkThemeToggle = onDarkThemeToggle
            )
        }
    ) {
        // Main content with hamburger menu
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
    onDarkThemeToggle: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App header
            Text(
                text = "SpentTracker",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Navigation items
            navigationItems.forEach { item ->
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title
                        )
                    },
                    label = { Text(item.title) },
                    selected = currentScreen == item.route,
                    onClick = { onScreenSelected(item.route) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Dark mode toggle at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onDarkThemeToggle() }
                )
            }
        }
    }
}

/**
 * Navigation Item Data Class
 */
data class NavigationItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)