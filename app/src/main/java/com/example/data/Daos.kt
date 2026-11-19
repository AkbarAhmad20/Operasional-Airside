package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY CASE role WHEN 'ADMIN' THEN 0 WHEN 'SUPERVISOR' THEN 1 ELSE 2 END, fullName ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface DockingLogDao {
    @Query("SELECT * FROM docking_logs ORDER BY activityTimestampMillis DESC, id DESC")
    fun observeAllLogs(): Flow<List<DockingLogEntity>>

    @Query("SELECT * FROM docking_logs WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): DockingLogEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: DockingLogEntity): Long

    @Update
    suspend fun updateLog(log: DockingLogEntity)

    @Query("DELETE FROM docking_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestampMillis DESC, id DESC")
    fun observeAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(entry: AuditLogEntity): Long
}
