package com.example.spenttracker.presentation.dashboard

import com.example.spenttracker.data.sync.SyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncSchedulerEntryPoint {
    fun syncScheduler(): SyncScheduler
}