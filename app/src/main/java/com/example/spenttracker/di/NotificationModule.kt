package com.example.spenttracker.di

import android.content.Context
import com.example.spenttracker.util.ExpenseNotificationManager
import com.example.spenttracker.util.NotificationScheduler
import com.example.spenttracker.util.AlarmNotificationScheduler
import com.example.spenttracker.data.preferences.SettingsManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection Module for Notification System
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    
    @Provides
    @Singleton
    fun provideExpenseNotificationManager(
        @ApplicationContext context: Context
    ): ExpenseNotificationManager {
        return ExpenseNotificationManager(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationScheduler(
        @ApplicationContext context: Context
    ): NotificationScheduler {
        return NotificationScheduler(context)
    }
    
    @Provides
    @Singleton
    fun provideAlarmNotificationScheduler(
        @ApplicationContext context: Context
    ): AlarmNotificationScheduler {
        return AlarmNotificationScheduler(context)
    }
    
    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context
    ): SettingsManager {
        return SettingsManager(context)
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}