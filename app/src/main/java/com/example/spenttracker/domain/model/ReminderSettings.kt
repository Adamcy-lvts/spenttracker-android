package com.example.spenttracker.domain.model

import java.time.LocalTime

/**
 * Domain model for expense reminder settings
 */
data class ReminderSettings(
    val id: String,
    val name: String,
    val time: LocalTime,
    val isEnabled: Boolean = true,
    val message: String = "💰 Time to log your expenses! Don't forget to track your spending for today.",
    val createdAt: String = "",
    val updatedAt: String = ""
)

/**
 * App settings data model
 */
data class AppSettings(
    val reminders: List<ReminderSettings> = getDefaultReminders(),
    val notificationsEnabled: Boolean = true,
    val darkMode: Boolean = false,
    val autoSync: Boolean = true,
    val syncFrequency: SyncFrequency = SyncFrequency.HOURLY
)

/**
 * Sync frequency options
 */
enum class SyncFrequency(val displayName: String, val minutes: Long) {
    EVERY_15_MINUTES("Every 15 minutes", 15),
    EVERY_30_MINUTES("Every 30 minutes", 30),
    HOURLY("Every hour", 60),
    EVERY_2_HOURS("Every 2 hours", 120),
    EVERY_6_HOURS("Every 6 hours", 360),
    DAILY("Once daily", 1440)
}

/**
 * Get default reminder settings
 */
fun getDefaultReminders(): List<ReminderSettings> {
    return listOf(
        ReminderSettings(
            id = "evening_reminder",
            name = "Evening Reminder",
            time = LocalTime.of(22, 0), // 10:00 PM
            isEnabled = true,
            message = "💰 Time to log your expenses! Don't forget to track your spending for today."
        )
    )
}

/**
 * Preset reminder times for quick selection
 */
fun getPresetReminderTimes(): List<Pair<String, LocalTime>> {
    return listOf(
        "Morning (8:00 AM)" to LocalTime.of(8, 0),
        "Lunch Break (12:00 PM)" to LocalTime.of(12, 0),
        "Afternoon (3:00 PM)" to LocalTime.of(15, 0),
        "After Work (6:00 PM)" to LocalTime.of(18, 0),
        "Evening (8:00 PM)" to LocalTime.of(20, 0),
        "Night (10:00 PM)" to LocalTime.of(22, 0),
        "Late Night (11:00 PM)" to LocalTime.of(23, 0)
    )
}

/**
 * Preset reminder messages
 */
fun getPresetReminderMessages(): List<String> {
    return listOf(
        "💰 Time to log your expenses! Don't forget to track your spending for today.",
        "📊 Quick reminder: Have you logged your expenses today?",
        "💳 Don't let expenses slip by! Log them now to stay on track.",
        "🎯 Keep your financial goals on track - log today's expenses!",
        "📝 A quick expense check-in to keep your budget organized.",
        "💡 Remember: Small expenses add up! Log them while you remember.",
        "🔔 Daily expense reminder: Every penny counts towards your goals!"
    )
}