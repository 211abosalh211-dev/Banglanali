package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.customer.CustomerDocumentEntity
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.customer.CustomerNoteEntity
import com.example.data.local.relation.CustomerWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllActiveCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id AND isDeleted = 0")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE customerCode = :code AND isDeleted = 0")
    suspend fun getCustomerByCode(code: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone AND isDeleted = 0 LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): CustomerEntity?

    @Query("SELECT COUNT(*) FROM invoices WHERE customerId = :customerId AND isDeleted = 0")
    suspend fun countCustomerInvoices(customerId: Long): Int

    @Query("SELECT COUNT(*) FROM reservations WHERE customerId = :customerId AND isDeleted = 0")
    suspend fun countCustomerReservations(customerId: Long): Int

    @Query("UPDATE customers SET isDeleted = 1, deletedAt = :timestamp, isActive = 0 WHERE id = :id")
    suspend fun softDeleteCustomer(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun hardDeleteCustomer(id: Long)

    @Transaction
    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerWithDetails(id: Long): CustomerWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CustomerNoteEntity): Long

    @Query("SELECT * FROM customer_notes WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getNotesForCustomer(customerId: Long): Flow<List<CustomerNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: CustomerDocumentEntity): Long

    @Query("SELECT * FROM customer_documents WHERE customerId = :customerId")
    fun getDocumentsForCustomer(customerId: Long): Flow<List<CustomerDocumentEntity>>
}
