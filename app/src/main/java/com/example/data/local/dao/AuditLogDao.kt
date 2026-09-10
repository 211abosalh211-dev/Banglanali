package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllActiveLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE entityType = :entityType AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getLogsByEntity(entityType: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("UPDATE audit_logs SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteLog(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM audit_logs WHERE isDeleted = 0")
    fun countActiveLogs(): Flow<Int>
}
