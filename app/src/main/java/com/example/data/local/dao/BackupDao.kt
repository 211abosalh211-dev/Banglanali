package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.backup.BackupMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupRecord(metadata: BackupMetadataEntity): Long

    @Query("SELECT * FROM backup_metadata ORDER BY createdAt DESC")
    fun getAllBackups(): Flow<List<BackupMetadataEntity>>

    @Query("SELECT * FROM backup_metadata WHERE status = 'COMPLETED' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSuccessfulBackup(): BackupMetadataEntity?
}
