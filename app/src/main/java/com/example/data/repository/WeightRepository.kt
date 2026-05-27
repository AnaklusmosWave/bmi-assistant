package com.example.data.repository

import com.example.data.database.UserProfile
import com.example.data.database.UserProfileDao
import com.example.data.database.WeightDao
import com.example.data.database.WeightLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeightRepository(
    private val weightDao: WeightDao,
    private val userProfileDao: UserProfileDao
) {
    // Expose all weight logs
    val allLogs: Flow<List<WeightLog>> = weightDao.getAllLogs()
    val allLogsAscending: Flow<List<WeightLog>> = weightDao.getAllLogsAscending()

    // Expose user profile with default fallback if database is empty
    val userProfile: Flow<UserProfile> = userProfileDao.getUserProfile().map { profile ->
        profile ?: UserProfile()
    }

    suspend fun getLatestLog(): WeightLog? {
        return weightDao.getLatestLog()
    }

    suspend fun getUserProfileDirect(): UserProfile {
        return userProfileDao.getUserProfileDirect() ?: UserProfile()
    }

    suspend fun insertLog(log: WeightLog) {
        weightDao.insertLog(log)
    }

    suspend fun updateLog(log: WeightLog) {
        weightDao.updateLog(log)
    }

    suspend fun deleteLog(log: WeightLog) {
        weightDao.deleteLog(log)
    }

    suspend fun deleteLogById(id: Int) {
        weightDao.deleteLogById(id)
    }

    suspend fun deleteAllLogs() {
        weightDao.deleteAllLogs()
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        userProfileDao.saveUserProfile(profile)
    }
}
