package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.hotel.UnitEntity
import com.example.data.local.entity.hotel.UnitFeatureEntity
import com.example.data.local.entity.hotel.UnitStatusHistoryEntity
import com.example.data.local.entity.hotel.UnitTypeEntity
import com.example.data.local.entity.hotel.UnitTypeFeatureEntity
import com.example.data.local.relation.UnitTypeWithFeatures
import kotlinx.coroutines.flow.Flow

@Dao
interface HotelUnitDao {

    // Unit Types
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUnitType(unitType: UnitTypeEntity): Long

    @Update
    suspend fun updateUnitType(unitType: UnitTypeEntity)

    @Query("SELECT * FROM unit_types WHERE isDeleted = 0")
    fun getAllUnitTypes(): Flow<List<UnitTypeEntity>>

    @Query("SELECT * FROM unit_types WHERE id = :id")
    suspend fun getUnitTypeById(id: Long): UnitTypeEntity?

    @Transaction
    @Query("SELECT * FROM unit_types WHERE id = :id")
    suspend fun getUnitTypeWithFeatures(id: Long): UnitTypeWithFeatures?

    // Units
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUnit(unit: UnitEntity): Long

    @Update
    suspend fun updateUnit(unit: UnitEntity)

    @Query("SELECT * FROM units WHERE isDeleted = 0 ORDER BY unitNumber ASC")
    fun getAllUnits(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getUnitById(id: Long): UnitEntity?

    @Query("SELECT * FROM units WHERE unitNumber = :unitNumber AND isDeleted = 0")
    suspend fun getUnitByNumber(unitNumber: String): UnitEntity?

    @Query("SELECT * FROM units WHERE status = :status AND isDeleted = 0")
    fun getUnitsByStatus(status: String): Flow<List<UnitEntity>>

    @Query("UPDATE units SET status = :newStatus, updatedAt = :timestamp WHERE id = :unitId")
    suspend fun updateUnitStatus(unitId: Long, newStatus: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE units SET isDeleted = 1, deletedAt = :timestamp, isActive = 0 WHERE id = :id")
    suspend fun softDeleteUnit(id: Long, timestamp: Long = System.currentTimeMillis())

    // Features
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeature(feature: UnitFeatureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignFeatureToType(mapping: UnitTypeFeatureEntity)

    // Status History
    @Insert
    suspend fun insertStatusHistory(history: UnitStatusHistoryEntity): Long

    @Query("SELECT * FROM unit_status_history WHERE unitId = :unitId ORDER BY timestamp DESC")
    fun getUnitStatusHistory(unitId: Long): Flow<List<UnitStatusHistoryEntity>>
}
