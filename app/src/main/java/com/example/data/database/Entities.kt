package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weightKg: Double,
    val heightCm: Double,
    val timestamp: Long,
    val note: String = ""
) {
    // Helper to compute BMI
    val bmi: Double
        get() = if (heightCm > 0) {
            val heightM = heightCm / 100.0
            weightKg / (heightM * heightM)
        } else 0.0
}

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val heightCm: Double = 170.0,
    val targetWeightKg: Double = 60.0,
    val isMetricUnit: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val reminderFrequency: String = "每日", // "每日", "週一至週五", "每週"
    val language: String = "zh-TW", // "zh-TW" or "en-US"
    val age: Int = 25,
    val gender: String = "女", // "男" or "女"
    val isOnboarded: Boolean = false,
    val isDeveloperMode: Boolean = false,
    val weekStartDay: Int = 2, // 1 = Sunday, 2 = Monday
    val dateFormatPattern: String = "DEFAULT" // "DEFAULT", "yyyy/MM/dd", "MM/dd/yyyy", "dd/MM/yyyy"
)
