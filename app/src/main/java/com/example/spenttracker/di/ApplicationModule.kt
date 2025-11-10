package com.example.spenttracker.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-level dependencies module
 * Provides Context and other app-wide dependencies
 * 
 * Kotlin/Hilt Concepts Explained:
 * - @Module = Tells Hilt this class provides dependencies
 * - @InstallIn(SingletonComponent::class) = Available throughout entire app lifetime
 * - @Provides = This function creates/returns a dependency
 * - @ApplicationContext = Specifically the Application context (not Activity context)
 * - @Singleton = Only create one instance, reuse it everywhere
 */
@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    
    /**
     * Provide Application Context for dependencies that need it
     * 
     * @ApplicationContext = Safe context that won't leak
     * Context is needed for:
     * - DataStore (for storing user preferences)
     * - SharedPreferences
     * - Database operations
     * - File operations
     */
    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context
}