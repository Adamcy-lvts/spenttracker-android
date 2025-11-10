package com.example.spenttracker.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
// Font Awesome Icons for beautiful dashboard stats
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.DollarSign
import compose.icons.fontawesomeicons.solid.CalendarDay
import compose.icons.fontawesomeicons.solid.CalendarWeek
import compose.icons.fontawesomeicons.solid.ArrowUp
import compose.icons.fontawesomeicons.solid.ArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.spenttracker.data.local.ExpenseDatabase
import com.example.spenttracker.data.mapper.toDomainList
import com.example.spenttracker.data.repository.CategoryRepositoryImpl
import com.example.spenttracker.domain.repository.ExpenseRepository
import com.example.spenttracker.domain.model.Expense
import com.example.spenttracker.domain.model.Category
import com.example.spenttracker.presentation.auth.AuthViewModel
import com.example.spenttracker.presentation.theme.ShadcnButton
import com.example.spenttracker.presentation.theme.ShadcnButtonVariant
import com.example.spenttracker.presentation.theme.ShadcnButtonSize
import com.example.spenttracker.data.sync.SyncScheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.util.*
import kotlinx.coroutines.flow.map

/**
 * Dashboard Screen - Matching the Vue.js design with statistics cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToExpenses: () -> Unit = {},
    darkTheme: Boolean = false,
    onDarkThemeToggle: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    // Get user context from AuthViewModel
    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.uiState.collectAsState()
    val currentUser = authUiState.currentUser
    
    // Get sync scheduler for manual sync
    val context = LocalContext.current
    val syncScheduler = remember { 
        val app = context.applicationContext as com.example.spenttracker.SpentTrackerApplication
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            app,
            SyncSchedulerEntryPoint::class.java
        ).syncScheduler()
    }
    
    // Get repository for user-filtered expense data through ExpensesViewModel
    val expensesViewModel: com.example.spenttracker.presentation.expenses.ExpensesViewModel = hiltViewModel()
    
    // Get database access for category data
    val database = ExpenseDatabase.getDatabase(context)
    val categoryRepository = CategoryRepositoryImpl(database.categoryDao())
    
    // Month navigation state
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }

    // Get ALL expenses from ExpensesViewModel (not paginated) for statistics calculation
    val allExpenses by expensesViewModel.allExpenses.collectAsState()
    val expensesState by expensesViewModel.state.collectAsState()
    val isLoading = expensesState is com.example.spenttracker.presentation.expenses.ExpenseListState.Loading
    val errorMessage = (expensesState as? com.example.spenttracker.presentation.expenses.ExpenseListState.Error)?.message

    // Calculate statistics from ALL expenses based on selected month
    val statistics = remember(allExpenses, selectedMonth) {
        calculateStatistics(allExpenses, selectedMonth)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onMenuClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Month Navigation Header
            item {
                MonthNavigationHeader(
                    selectedMonth = selectedMonth,
                    onPreviousMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                    onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
                    onResetToCurrentMonth = { selectedMonth = YearMonth.now() }
                )
            }

            // Statistics Cards Row
            item {
                // Beautiful stats cards with Font Awesome icons
                val statsCards = listOf(
                    StatCardData(
                        title = "Total Expenses",
                        value = formatCurrency(statistics.totalExpenses),
                        subtitle = "All time spending",
                        icon = FontAwesomeIcons.Solid.DollarSign, // 💰 Beautiful dollar icon!
                        iconColor = Color(0xFF16A34A) // Green for money
                    ),
                    StatCardData(
                        title = "This Month",
                        value = formatCurrency(statistics.thisMonth),
                        subtitle = "${statistics.monthlyChangePercent}% from last month",
                        icon = FontAwesomeIcons.Solid.CalendarDay, // 📅 Perfect calendar icon!
                        iconColor = if (statistics.monthlyChangePercent >= 0) Color(0xFFEF4444) else Color(0xFF10B981),
                        trending = if (statistics.monthlyChangePercent >= 0) FontAwesomeIcons.Solid.ArrowUp else FontAwesomeIcons.Solid.ArrowDown
                    ),
                    StatCardData(
                        title = "This Week",
                        value = formatCurrency(statistics.thisWeek),
                        subtitle = "Current week spending",
                        icon = FontAwesomeIcons.Solid.CalendarWeek, // 🗓️ Perfect week calendar icon!
                        iconColor = Color(0xFF3B82F6) // Blue for weekly stats
                    )
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    statsCards.forEach { cardData ->
                        StatisticsCard(
                            data = cardData,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Category Breakdown Section
            item {
                CategoryBreakdownCard(
                    expenses = allExpenses,
                    categoryRepository = categoryRepository,
                    selectedMonth = selectedMonth
                )
            }
            
            // Recent Expenses & Monthly Trend Section
            item {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    else -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Recent Expenses Card - Show 5 most recent from ALL expenses
                            RecentExpensesCard(
                                expenses = allExpenses.sortedByDescending { it.date }.take(5),
                                onViewAllExpenses = onNavigateToExpenses
                            )
                            
                            // Monthly Trend Card
                            MonthlyTrendCard(
                                monthlyTrend = statistics.monthlyTrend
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Month Navigation Header Component
 */
@Composable
fun MonthNavigationHeader(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: () -> Unit
) {
    val isCurrentMonth = selectedMonth == YearMonth.now()
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Month Button
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Month Display with Reset Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selectedMonth.format(monthFormatter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (!isCurrentMonth) {
                    TextButton(
                        onClick = onResetToCurrentMonth,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Current Month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Next Month Button
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Statistics Card Component - Matches Vue.js Card design
 */
@Composable
fun StatisticsCard(
    data: StatCardData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with title and icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Value
            Text(
                text = data.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Subtitle with optional trending icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                data.trending?.let { trendIcon ->
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        tint = data.iconColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = data.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (data.trending != null) data.iconColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Data class for statistics cards
 */
data class StatCardData(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val trending: ImageVector? = null
)

/**
 * Recent Expenses Card Component
 */
@Composable
fun RecentExpensesCard(
    expenses: List<Expense>,
    onViewAllExpenses: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recent Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (expenses.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No expenses yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Start by adding your first expense!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Recent expenses list
                expenses.forEach { expense ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = expense.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = expense.getFormattedDate(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = expense.getFormattedAmount(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
                
                // View all expenses button
                ShadcnButton(
                    onClick = onViewAllExpenses,
                    variant = ShadcnButtonVariant.Outline,
                    size = ShadcnButtonSize.Small,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("View all expenses")
                }
            }
        }
    }
}

/**
 * Monthly Trend Card Component
 */
@Composable
fun MonthlyTrendCard(
    monthlyTrend: List<MonthlyTrend>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Monthly Spending Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Monthly trend bars - Fix division by zero
            val maxAmount = monthlyTrend.maxOfOrNull { it.amount } ?: 0.0
            
            monthlyTrend.forEach { trend ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Month label
                    Text(
                        text = trend.month,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp)
                    )
                    
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                    ) {
                        // Calculate progress safely to avoid NaN
                        val progress = if (maxAmount > 0.0) {
                            ((trend.amount / maxAmount).toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                    
                    // Amount
                    Text(
                        text = formatCurrency(trend.amount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        }
    }
}

/**
 * Data classes for statistics
 */
data class ExpenseStatistics(
    val totalExpenses: Double = 0.0,
    val thisMonth: Double = 0.0,
    val lastMonth: Double = 0.0,
    val thisWeek: Double = 0.0,
    val monthlyChangePercent: Int = 0,
    val monthlyTrend: List<MonthlyTrend> = emptyList()
)

data class MonthlyTrend(
    val month: String,
    val amount: Double
)

data class CategoryBreakdown(
    val name: String,
    val amount: Double,
    val count: Int,
    val percentage: Double,
    val color: String
)

/**
 * Calculate statistics from expenses based on selected month
 */
fun calculateStatistics(expenses: List<Expense>, selectedMonth: YearMonth = YearMonth.now()): ExpenseStatistics {
    val now = LocalDate.now()
    val lastMonth = selectedMonth.minusMonths(1)

    // Calculate week boundaries for selected month
    val firstDayOfMonth = selectedMonth.atDay(1)
    val isCurrentMonth = selectedMonth == YearMonth.now()
    val weekStart = if (isCurrentMonth) {
        now.minusDays(now.dayOfWeek.value.toLong() - 1)
    } else {
        firstDayOfMonth
    }

    val totalExpenses = expenses.sumOf { it.amount }

    val thisMonthExpenses = expenses
        .filter { YearMonth.from(it.date) == selectedMonth }
        .sumOf { it.amount }

    val lastMonthExpenses = expenses
        .filter { YearMonth.from(it.date) == lastMonth }
        .sumOf { it.amount }

    val thisWeekExpenses = if (isCurrentMonth) {
        expenses
            .filter { it.date >= weekStart }
            .sumOf { it.amount }
    } else {
        // For past/future months, show first week expenses
        val weekEnd = weekStart.plusDays(6)
        expenses
            .filter { it.date >= weekStart && it.date <= weekEnd }
            .sumOf { it.amount }
    }

    val monthlyChangePercent = if (lastMonthExpenses == 0.0) {
        if (thisMonthExpenses > 0) 100 else 0
    } else {
        (((thisMonthExpenses - lastMonthExpenses) / lastMonthExpenses) * 100).toInt()
    }

    // Generate monthly trend - 6 months centered around selected month
    val monthlyTrend = (5 downTo 0).map { monthsBack ->
        val month = selectedMonth.minusMonths(monthsBack.toLong())
        val monthExpenses = expenses
            .filter { YearMonth.from(it.date) == month }
            .sumOf { it.amount }
        MonthlyTrend(
            month = month.format(DateTimeFormatter.ofPattern("MMM")),
            amount = monthExpenses
        )
    }

    return ExpenseStatistics(
        totalExpenses = totalExpenses,
        thisMonth = thisMonthExpenses,
        lastMonth = lastMonthExpenses,
        thisWeek = thisWeekExpenses,
        monthlyChangePercent = monthlyChangePercent,
        monthlyTrend = monthlyTrend
    )
}

/**
 * Category Breakdown Card Component
 */
@Composable
fun CategoryBreakdownCard(
    expenses: List<Expense>,
    categoryRepository: CategoryRepositoryImpl,
    selectedMonth: YearMonth = YearMonth.now()
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    // Load categories
    LaunchedEffect(Unit) {
        categoryRepository.getActiveCategories().collect { categoryList ->
            categories = categoryList
        }
    }

    // Calculate category breakdown for selected month
    val selectedMonthExpenses = expenses.filter {
        YearMonth.from(it.date) == selectedMonth
    }

    val categoryBreakdown = remember(selectedMonthExpenses, categories) {
        calculateCategoryBreakdown(selectedMonthExpenses, categories)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "This Month by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (categoryBreakdown.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No expenses this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Category breakdown list
                categoryBreakdown.take(6).forEach { category ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color indicator
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            Color(android.graphics.Color.parseColor(category.color)),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(${category.count} expense${if (category.count == 1) "" else "s"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = formatCurrency(category.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format("%.1f%%", category.percentage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((category.percentage / 100).toFloat())
                                    .background(
                                        Color(android.graphics.Color.parseColor(category.color)),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
                
                // Show More Link if there are more than 6 categories
                if (categoryBreakdown.size > 6) {
                    ShadcnButton(
                        onClick = { /* Navigate to categories screen */ },
                        variant = ShadcnButtonVariant.Ghost,
                        size = ShadcnButtonSize.Small,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("View all categories (${categoryBreakdown.size - 6} more)")
                    }
                }
            }
        }
    }
}

/**
 * Calculate category breakdown from expenses
 */
fun calculateCategoryBreakdown(expenses: List<Expense>, categories: List<Category>): List<CategoryBreakdown> {
    val totalAmount = expenses.sumOf { it.amount }
    if (totalAmount == 0.0) return emptyList()
    
    // Group expenses by category
    val categoryExpenses = expenses.groupBy { expense ->
        expense.categoryId ?: -1 // Use -1 for uncategorized
    }
    
    // Create breakdown for each category that has expenses
    val breakdown = mutableListOf<CategoryBreakdown>()
    
    var runningPercentage = 0.0
    val sortedCategoryExpenses = categoryExpenses.toList().sortedByDescending { it.second.sumOf { expense -> expense.amount } }
    
    sortedCategoryExpenses.forEachIndexed { index, (categoryId, expenseList) ->
        val amount = expenseList.sumOf { it.amount }
        val count = expenseList.size
        
        // For the last item, use remaining percentage to avoid rounding errors
        val percentage = if (index == sortedCategoryExpenses.size - 1) {
            100.0 - runningPercentage
        } else {
            ((amount / totalAmount) * 100).let { calculated ->
                // Properly round to 1 decimal place
                kotlin.math.round(calculated * 10.0) / 10.0
            }
        }
        
        runningPercentage += percentage
        
        if (categoryId == -1) {
            // Uncategorized expenses
            breakdown.add(
                CategoryBreakdown(
                    name = "Uncategorized",
                    amount = amount,
                    count = count,
                    percentage = percentage,
                    color = "#6B7280" // Gray color for uncategorized
                )
            )
        } else {
            // Find category details
            val category = categories.find { it.id == categoryId }
            category?.let {
                breakdown.add(
                    CategoryBreakdown(
                        name = it.name,
                        amount = amount,
                        count = count,
                        percentage = percentage,
                        color = it.color
                    )
                )
            }
        }
    }
    
    return breakdown
}

/**
 * Format currency to Nigerian Naira
 */
fun formatCurrency(amount: Double): String {
    return "₦${String.format(Locale.getDefault(), "%,.2f", amount)}"
}