package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SystemConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemConfigDao {
    @Query("SELECT * FROM system_config WHERE configKey = :key LIMIT 1")
    fun getConfig(key: String): Flow<SystemConfigEntity?>

    @Query("SELECT configValue FROM system_config WHERE configKey = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM system_config ORDER BY configKey ASC")
    fun getAllConfigs(): Flow<List<SystemConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: SystemConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<SystemConfigEntity>)

    @Query("SELECT COUNT(*) FROM system_config")
    suspend fun countConfigs(): Int
}
