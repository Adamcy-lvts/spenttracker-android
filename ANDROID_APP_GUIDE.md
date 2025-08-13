# Complete Android Expense Tracker App Development Guide

## Table of Contents
1. [Project Overview](#project-overview)
2. [Android Development Fundamentals](#android-development-fundamentals)
3. [Phase 1: Project Setup](#phase-1-project-setup)
4. [Phase 2: API Integration Layer](#phase-2-api-integration-layer)
5. [Phase 3: Data Models and Local Database](#phase-3-data-models-and-local-database)
6. [Phase 4: User Interface with Jetpack Compose](#phase-4-user-interface-with-jetpack-compose)
7. [Phase 5: Core Functionality](#phase-5-core-functionality)
8. [Phase 6: Offline Support](#phase-6-offline-support)
9. [Phase 7: Testing and Deployment](#phase-7-testing-and-deployment)
10. [Additional Features](#additional-features)

## Project Overview

We're building an Android expense tracker app that connects to your existing Laravel API. The app will:
- Display a list of expenses
- Allow adding new expenses
- Edit and delete existing expenses
- Work offline and sync when online
- Match the functionality of your Vue.js web app

### Your Current Laravel API Endpoints:
- `GET /expense` - List expenses
- `POST /expense` - Create expense
- `PUT /expense/{id}` - Update expense
- `DELETE /expense/{id}` - Delete expense
- `DELETE /expenses/bulk` - Bulk delete

### Expense Model Structure:
```json
{
  "id": 1,
  "description": "Coffee",
  "amount": "5.50",
  "date": "2025-08-10",
  "user_id": 1,
  "created_at": "2025-08-10T10:00:00Z",
  "updated_at": "2025-08-10T10:00:00Z"
}
```

## Android Development Fundamentals

### Key Concepts You'll Learn:

1. **Activities and Fragments**: Building blocks of Android apps
2. **Jetpack Compose**: Modern UI toolkit for building native Android UI
3. **ViewModels**: Manage UI-related data in lifecycle-conscious way
4. **Repository Pattern**: Abstract data sources (API + local database)
5. **Room Database**: Local SQLite database for offline storage
6. **Retrofit**: HTTP client for API calls
7. **Hilt/Dagger**: Dependency injection framework
8. **Coroutines**: Asynchronous programming in Kotlin

### Architecture Pattern: MVVM (Model-View-ViewModel)
```
[UI (Compose)] → [ViewModel] → [Repository] → [API/Database]
```

## Phase 1: Project Setup

### 1.1 Prerequisites
- Install Android Studio (latest version)
- Set up an Android Virtual Device (AVD) for testing
- Basic understanding of Kotlin programming language

### 1.2 Create New Project
1. Open Android Studio
2. Create new project with "Empty Activity"
3. Choose:
   - Name: "SpentTracker"
   - Package: "com.spentracker.app"
   - Language: Kotlin
   - Minimum SDK: API 24 (Android 7.0)

### 1.3 Project Structure Overview
```
app/
├── src/main/
│   ├── java/com/spentracker/app/
│   │   ├── data/          # Data layer (API, Database, Repository)
│   │   ├── domain/        # Business logic and models
│   │   ├── presentation/  # UI layer (Compose screens, ViewModels)
│   │   └── di/           # Dependency injection modules
│   ├── res/              # Resources (layouts, strings, etc.)
│   └── AndroidManifest.xml
├── build.gradle.kts      # App-level build configuration
└── proguard-rules.pro    # Code obfuscation rules
```

### 1.4 Dependencies Setup

Create `gradle/libs.versions.toml` file:
```toml
[versions]
agp = "8.7.2"
kotlin = "2.0.21"
coreKtx = "1.15.0"
lifecycleRuntimeKtx = "2.8.6"
activityCompose = "1.9.2"
composeBom = "2024.10.00"
navigationCompose = "2.8.2"
hilt = "2.52"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
gson = "2.11.0"
kotlinxSerialization = "1.7.3"
material3Datetime = "1.3.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Dependency Injection
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Database
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }

# Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

# Date Picker
androidx-material3-datetime = { group = "io.github.vanpra.compose-material-dialogs", name = "datetime", version.ref = "material3Datetime" }

# Testing
junit = { group = "junit", name = "junit", version = "4.13.2" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version = "1.2.1" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version = "3.6.1" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### 1.5 App-level build.gradle.kts:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    kotlin("kapt")
}

android {
    namespace = "com.spentracker.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spentracker.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://your-production-url.com\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    
    // Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
    
    // Local Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Date Picker
    implementation(libs.androidx.material3.datetime)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}
```

## Phase 2: API Integration Layer

### 2.1 Network Models (Data Transfer Objects)

Create `data/remote/dto/ExpenseDto.kt`:
```kotlin
package com.spentracker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExpenseDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("description")
    val description: String,
    @SerializedName("amount")
    val amount: String, // Laravel returns decimals as strings
    @SerializedName("date")
    val date: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class CreateExpenseRequest(
    @SerializedName("description")
    val description: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("date")
    val date: String
)

data class UpdateExpenseRequest(
    @SerializedName("description")
    val description: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("date")
    val date: String
)

data class BulkDeleteRequest(
    @SerializedName("expense_ids")
    val expenseIds: List<Int>
)

data class ApiResponse<T>(
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("message")
    val message: String? = null
)
```

### 2.2 API Service Interface

Create `data/remote/ExpenseApiService.kt`:
```kotlin
package com.spentracker.app.data.remote

import com.spentracker.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ExpenseApiService {
    
    @GET("expense")
    suspend fun getExpenses(): Response<List<ExpenseDto>>
    
    @POST("expense")
    suspend fun createExpense(
        @Body request: CreateExpenseRequest
    ): Response<ExpenseDto>
    
    @PUT("expense/{id}")
    suspend fun updateExpense(
        @Path("id") id: Int,
        @Body request: UpdateExpenseRequest
    ): Response<ExpenseDto>
    
    @DELETE("expense/{id}")
    suspend fun deleteExpense(
        @Path("id") id: Int
    ): Response<ApiResponse<Any>>
    
    @DELETE("expenses/bulk")
    suspend fun bulkDeleteExpenses(
        @Body request: BulkDeleteRequest
    ): Response<ApiResponse<Any>>
}
```

### 2.3 Network Configuration

Create `data/remote/NetworkModule.kt`:
```kotlin
package com.spentracker.app.di

import com.spentracker.app.BuildConfig
import com.spentracker.app.data.remote.ExpenseApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideExpenseApiService(retrofit: Retrofit): ExpenseApiService {
        return retrofit.create(ExpenseApiService::class.java)
    }
}
```

## Phase 3: Data Models and Local Database

### 3.1 Domain Models

Create `domain/model/Expense.kt`:
```kotlin
package com.spentracker.app.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Expense(
    val id: Int = 0,
    val description: String,
    val amount: Double,
    val date: LocalDate,
    val userId: Int = 0,
    val isOffline: Boolean = false,
    val localId: String? = null
) {
    fun getFormattedAmount(): String {
        return "₦${String.format("%.2f", amount)}"
    }
    
    fun getFormattedDate(): String {
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
}
```

### 3.2 Database Entities

Create `data/local/entity/ExpenseEntity.kt`:
```kotlin
package com.spentracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: Int = 0,
    val description: String,
    val amount: Double,
    val date: String, // Store as ISO string
    val userId: Int,
    val isOffline: Boolean = false,
    val localId: String? = null,
    val createdAt: String,
    val updatedAt: String
)
```

### 3.3 Room Database Setup

Create `data/local/ExpenseDao.kt`:
```kotlin
package com.spentracker.app.data.local

import androidx.room.*
import com.spentracker.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    
    @Query("SELECT * FROM expenses ORDER BY date DESC, updatedAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Int): ExpenseEntity?
    
    @Query("SELECT * FROM expenses WHERE isOffline = 1")
    suspend fun getOfflineExpenses(): List<ExpenseEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)
    
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)
    
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
    
    @Query("DELETE FROM expenses WHERE id IN (:ids)")
    suspend fun deleteExpensesByIds(ids: List<Int>)
    
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
    
    @Query("UPDATE expenses SET isOffline = 0 WHERE localId = :localId")
    suspend fun markAsSynced(localId: String)
}
```

Create `data/local/ExpenseDatabase.kt`:
```kotlin
package com.spentracker.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.spentracker.app.data.local.entity.ExpenseEntity

@Database(
    entities = [ExpenseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}
```

### 3.4 Data Mappers

Create `data/mapper/ExpenseMapper.kt`:
```kotlin
package com.spentracker.app.data.mapper

import com.spentracker.app.data.local.entity.ExpenseEntity
import com.spentracker.app.data.remote.dto.ExpenseDto
import com.spentracker.app.domain.model.Expense
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// DTO to Domain
fun ExpenseDto.toDomain(): Expense {
    return Expense(
        id = id,
        description = description,
        amount = amount.toDoubleOrNull() ?: 0.0,
        date = LocalDate.parse(date),
        userId = userId,
        isOffline = false
    )
}

// Domain to Entity
fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        description = description,
        amount = amount,
        date = date.toString(),
        userId = userId,
        isOffline = isOffline,
        localId = localId,
        createdAt = "",
        updatedAt = ""
    )
}

// Entity to Domain
fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        description = description,
        amount = amount,
        date = LocalDate.parse(date),
        userId = userId,
        isOffline = isOffline,
        localId = localId
    )
}

// Domain to DTO (for API requests)
fun Expense.toCreateRequest(): com.spentracker.app.data.remote.dto.CreateExpenseRequest {
    return com.spentracker.app.data.remote.dto.CreateExpenseRequest(
        description = description,
        amount = amount.toString(),
        date = date.toString()
    )
}

fun Expense.toUpdateRequest(): com.spentracker.app.data.remote.dto.UpdateExpenseRequest {
    return com.spentracker.app.data.remote.dto.UpdateExpenseRequest(
        description = description,
        amount = amount.toString(),
        date = date.toString()
    )
}
```

## Phase 4: User Interface with Jetpack Compose

### 4.1 UI Theme Setup

Create `presentation/theme/Color.kt`:
```kotlin
package com.spentracker.app.presentation.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Custom colors for expense app
val Green500 = Color(0xFF4CAF50)
val Red500 = Color(0xFFF44336)
val Orange500 = Color(0xFFFF9800)
```

Create `presentation/theme/Type.kt`:
```kotlin
package com.spentracker.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
```

Create `presentation/theme/Theme.kt`:
```kotlin
package com.spentracker.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun SpentTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### 4.2 Main Navigation

Create `presentation/navigation/NavGraph.kt`:
```kotlin
package com.spentracker.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.spentracker.app.presentation.expenses.ExpensesScreen
import com.spentracker.app.presentation.add_edit_expense.AddEditExpenseScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.ExpenseList.route
    ) {
        composable(route = Screen.ExpenseList.route) {
            ExpensesScreen(
                onNavigateToAddExpense = {
                    navController.navigate(Screen.AddEditExpense.route)
                },
                onNavigateToEditExpense = { expenseId ->
                    navController.navigate(
                        Screen.AddEditExpense.route + "?expenseId=$expenseId"
                    )
                }
            )
        }
        
        composable(
            route = Screen.AddEditExpense.route + "?expenseId={expenseId}",
            arguments = listOf(
                navArgument("expenseId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            AddEditExpenseScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object ExpenseList : Screen("expense_list")
    object AddEditExpense : Screen("add_edit_expense")
}
```

### 4.3 Expense List Screen UI

Create `presentation/expenses/ExpensesScreen.kt`:
```kotlin
package com.spentracker.app.presentation.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spentracker.app.presentation.expenses.components.ExpenseItem
import com.spentracker.app.presentation.expenses.components.ExpenseListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditExpense: (Int) -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Expenses",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExpense
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state) {
                is ExpenseListState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is ExpenseListState.Success -> {
                    if (state.expenses.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No expenses yet",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap the + button to add your first expense",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.expenses,
                                key = { expense -> expense.id }
                            ) { expense ->
                                ExpenseItem(
                                    expense = expense,
                                    onEditClick = { onNavigateToEditExpense(expense.id) },
                                    onDeleteClick = { viewModel.deleteExpense(expense.id) }
                                )
                            }
                        }
                    }
                }
                
                is ExpenseListState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading expenses",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadExpenses() }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
    
    // Handle one-time events
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ExpensesViewModel.UiEvent.ShowSnackbar -> {
                    // Handle snackbar display
                }
            }
        }
    }
}
```

## Phase 5: Core Functionality

### 5.1 Repository Pattern Implementation

Create `data/repository/ExpenseRepositoryImpl.kt`:
```kotlin
package com.spentracker.app.data.repository

import com.spentracker.app.data.local.ExpenseDao
import com.spentracker.app.data.mapper.*
import com.spentracker.app.data.remote.ExpenseApiService
import com.spentracker.app.data.remote.dto.BulkDeleteRequest
import com.spentracker.app.domain.model.Expense
import com.spentracker.app.domain.repository.ExpenseRepository
import com.spentracker.app.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val api: ExpenseApiService,
    private val dao: ExpenseDao
) : ExpenseRepository {
    
    override fun getExpenses(): Flow<List<Expense>> {
        return dao.getAllExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun refreshExpenses(): Resource<Unit> {
        return try {
            val response = api.getExpenses()
            if (response.isSuccessful && response.body() != null) {
                val expenses = response.body()!!.map { it.toDomain().toEntity() }
                dao.insertExpenses(expenses)
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to fetch expenses: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }
    
    override suspend fun getExpenseById(id: Int): Expense? {
        return dao.getExpenseById(id)?.toDomain()
    }
    
    override suspend fun createExpense(expense: Expense): Resource<Unit> {
        return try {
            val response = api.createExpense(expense.toCreateRequest())
            if (response.isSuccessful && response.body() != null) {
                val createdExpense = response.body()!!.toDomain()
                dao.insertExpense(createdExpense.toEntity())
                Resource.Success(Unit)
            } else {
                // Save offline if API fails
                val offlineExpense = expense.copy(
                    localId = UUID.randomUUID().toString(),
                    isOffline = true
                )
                dao.insertExpense(offlineExpense.toEntity())
                Resource.Success(Unit, "Saved offline, will sync when online")
            }
        } catch (e: Exception) {
            // Save offline on network error
            val offlineExpense = expense.copy(
                localId = UUID.randomUUID().toString(),
                isOffline = true
            )
            dao.insertExpense(offlineExpense.toEntity())
            Resource.Success(Unit, "Saved offline, will sync when online")
        }
    }
    
    override suspend fun updateExpense(expense: Expense): Resource<Unit> {
        return try {
            val response = api.updateExpense(expense.id, expense.toUpdateRequest())
            if (response.isSuccessful && response.body() != null) {
                val updatedExpense = response.body()!!.toDomain()
                dao.updateExpense(updatedExpense.toEntity())
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update expense: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }
    
    override suspend fun deleteExpense(id: Int): Resource<Unit> {
        return try {
            val response = api.deleteExpense(id)
            if (response.isSuccessful) {
                val expense = dao.getExpenseById(id)
                if (expense != null) {
                    dao.deleteExpense(expense)
                }
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to delete expense: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }
    
    override suspend fun bulkDeleteExpenses(ids: List<Int>): Resource<Unit> {
        return try {
            val response = api.bulkDeleteExpenses(BulkDeleteRequest(ids))
            if (response.isSuccessful) {
                dao.deleteExpensesByIds(ids)
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to delete expenses: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }
    
    override suspend fun syncOfflineExpenses(): Resource<Unit> {
        return try {
            val offlineExpenses = dao.getOfflineExpenses()
            for (expenseEntity in offlineExpenses) {
                val expense = expenseEntity.toDomain()
                val response = api.createExpense(expense.toCreateRequest())
                if (response.isSuccessful) {
                    expense.localId?.let { dao.markAsSynced(it) }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }
}
```

### 5.2 ViewModels with Business Logic

Create `presentation/expenses/ExpensesViewModel.kt`:
```kotlin
package com.spentracker.app.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spentracker.app.domain.repository.ExpenseRepository
import com.spentracker.app.domain.util.Resource
import com.spentracker.app.presentation.expenses.components.ExpenseListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow<ExpenseListState>(ExpenseListState.Loading)
    val state: StateFlow<ExpenseListState> = _state.asStateFlow()
    
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()
    
    init {
        loadExpenses()
        observeExpenses()
    }
    
    private fun observeExpenses() {
        repository.getExpenses()
            .onStart { _state.value = ExpenseListState.Loading }
            .catch { exception ->
                _state.value = ExpenseListState.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
            .onEach { expenses ->
                _state.value = ExpenseListState.Success(expenses)
            }
            .launchIn(viewModelScope)
    }
    
    fun loadExpenses() {
        viewModelScope.launch {
            when (val result = repository.refreshExpenses()) {
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Unknown error"))
                }
                is Resource.Success -> {
                    result.message?.let { message ->
                        _eventFlow.emit(UiEvent.ShowSnackbar(message))
                    }
                }
            }
        }
    }
    
    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            when (val result = repository.deleteExpense(id)) {
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Failed to delete"))
                }
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Expense deleted"))
                }
            }
        }
    }
    
    fun syncOfflineData() {
        viewModelScope.launch {
            when (val result = repository.syncOfflineExpenses()) {
                is Resource.Error -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar(result.message ?: "Sync failed"))
                }
                is Resource.Success -> {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Sync completed"))
                }
            }
        }
    }
    
    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}
```

## Phase 6: Offline Support

### 6.1 Network Connectivity Monitor

Create `data/connectivity/ConnectivityObserver.kt`:
```kotlin
package com.spentracker.app.data.connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    fun observe(): Flow<Status>
    
    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}
```

Create `data/connectivity/NetworkConnectivityObserver.kt`:
```kotlin
package com.spentracker.app.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class NetworkConnectivityObserver(
    context: Context
) : ConnectivityObserver {
    
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    override fun observe(): Flow<ConnectivityObserver.Status> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(ConnectivityObserver.Status.Available) }
                }
                
                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    launch { send(ConnectivityObserver.Status.Losing) }
                }
                
                override fun onLost(network: Network) {
                    super.onLost(network)
                    launch { send(ConnectivityObserver.Status.Lost) }
                }
                
                override fun onUnavailable() {
                    super.onUnavailable()
                    launch { send(ConnectivityObserver.Status.Unavailable) }
                }
            }
            
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
                
            connectivityManager.registerNetworkCallback(request, callback)
            
            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
    }
}
```

### 6.2 Offline Sync Strategy

Create `domain/use_case/SyncOfflineDataUseCase.kt`:
```kotlin
package com.spentracker.app.domain.use_case

import com.spentracker.app.data.connectivity.ConnectivityObserver
import com.spentracker.app.domain.repository.ExpenseRepository
import com.spentracker.app.domain.util.Resource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncOfflineDataUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    suspend operator fun invoke(): Resource<Unit> {
        val connectionStatus = connectivityObserver.observe().first()
        
        return if (connectionStatus == ConnectivityObserver.Status.Available) {
            repository.syncOfflineExpenses()
        } else {
            Resource.Error("No internet connection")
        }
    }
}
```

## Phase 7: Testing and Deployment

### 7.1 Unit Testing Setup

Create `test/ExpenseRepositoryTest.kt`:
```kotlin
package com.spentracker.app.data.repository

import com.spentracker.app.data.local.ExpenseDao
import com.spentracker.app.data.remote.ExpenseApiService
import com.spentracker.app.domain.model.Expense
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ExpenseRepositoryTest {
    
    private lateinit var repository: ExpenseRepositoryImpl
    private val mockApi = mockk<ExpenseApiService>()
    private val mockDao = mockk<ExpenseDao>(relaxed = true)
    
    @Before
    fun setup() {
        repository = ExpenseRepositoryImpl(mockApi, mockDao)
    }
    
    @Test
    fun `createExpense saves offline when API fails`() = runTest {
        // Test implementation
        val expense = Expense(
            description = "Test",
            amount = 10.0,
            date = LocalDate.now()
        )
        
        // Mock API failure and verify offline save
        // Add your test assertions here
    }
}
```

### 7.2 Build and Run Instructions

1. **Connect Android Device or Start Emulator**
   - Enable Developer Options and USB Debugging on physical device
   - Or create AVD in Android Studio

2. **Configure Laravel Backend**
   - Ensure Laravel app is running on `http://localhost:8000`
   - For physical device, replace `10.0.2.2` with your computer's IP address in `build.gradle.kts`

3. **Build and Install**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Additional Features

### 8.1 Search and Filter
- Add search functionality to filter expenses by description
- Date range filtering
- Amount range filtering

### 8.2 Export Features
- Export expenses to CSV
- Share functionality
- Email reports

### 8.3 Authentication
- Integrate with Laravel Sanctum for API authentication
- Login/logout screens
- Token management

### 8.4 Push Notifications
- Expense reminders
- Sync completion notifications
- Budget alerts

## Summary

This guide provides a complete roadmap for building your Android expense tracker app. The architecture follows modern Android development best practices:

- **MVVM Architecture** for clean separation of concerns
- **Repository Pattern** for data abstraction
- **Room Database** for offline storage
- **Retrofit** for API communication
- **Jetpack Compose** for modern UI
- **Hilt** for dependency injection
- **Coroutines & Flow** for asynchronous operations

Start with Phase 1 and work through each phase systematically. Each phase builds upon the previous one, so it's important to complete them in order.

The app will have full offline capability, matching the functionality of your Vue.js web application while providing a native Android experience.