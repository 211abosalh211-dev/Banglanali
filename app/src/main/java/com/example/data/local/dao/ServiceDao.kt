package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.service.ServiceCategoryEntity
import com.example.data.local.entity.service.ServiceEntity
import com.example.data.local.entity.service.ServiceTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: ServiceCategoryEntity): Long

    @Query("SELECT * FROM service_categories WHERE isActive = 1")
    fun getAllCategories(): Flow<List<ServiceCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertService(service: ServiceEntity): Long

    @Update
    suspend fun updateService(service: ServiceEntity)

    @Query("SELECT * FROM services WHERE isDeleted = 0")
    fun getAllActiveServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun getServiceById(id: Long): ServiceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertServiceTransaction(transaction: ServiceTransactionEntity): Long

    @Query("SELECT * FROM service_transactions WHERE stayId = :stayId ORDER BY transactionTimestamp DESC")
    fun getTransactionsForStay(stayId: Long): Flow<List<ServiceTransactionEntity>>
}
