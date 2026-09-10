package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.supplier.SupplierEntity
import com.example.data.local.entity.supplier.SupplierTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("SELECT * FROM suppliers WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllActiveSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE supplierCode = :code")
    suspend fun getSupplierByCode(code: String): SupplierEntity?

    @Query("SELECT COUNT(*) FROM purchase_invoices WHERE supplierId = :supplierId AND isDeleted = 0")
    suspend fun countSupplierPurchases(supplierId: Long): Int

    @Query("UPDATE suppliers SET currentBalanceMinor = currentBalanceMinor + :diffMinor WHERE id = :supplierId")
    suspend fun updateBalance(supplierId: Long, diffMinor: Long)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun hardDeleteSupplier(id: Long)

    @Query("UPDATE suppliers SET isDeleted = 1, deletedAt = :timestamp, isActive = 0 WHERE id = :id")
    suspend fun softDeleteSupplier(id: Long, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: SupplierTransactionEntity): Long

    @Query("SELECT * FROM supplier_transactions WHERE supplierId = :supplierId ORDER BY transactionDate DESC")
    fun getTransactionsForSupplier(supplierId: Long): Flow<List<SupplierTransactionEntity>>
}
