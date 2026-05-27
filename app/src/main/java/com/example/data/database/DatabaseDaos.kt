package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp ASC")
    fun getAllLogsAscending(): Flow<List<WeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WeightLog)

    @Update
    suspend fun updateLog(log: WeightLog)

    @Delete
    suspend fun deleteLog(log: WeightLog)

    @Query("DELETE FROM weight_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Int)

    @Query("DELETE FROM weight_logs")
    suspend fun deleteAllLogs()

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLog(): WeightLog?
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)
}
