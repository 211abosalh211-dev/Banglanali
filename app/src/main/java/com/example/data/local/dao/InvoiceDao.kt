package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.invoice.InvoiceAdjustmentEntity
import com.example.data.local.entity.invoice.InvoiceEntity
import com.example.data.local.entity.invoice.InvoiceItemEntity
import com.example.data.local.entity.invoice.InvoicePaymentEntity
import com.example.data.local.entity.invoice.InvoiceReturnEntity
import com.example.data.local.relation.InvoiceWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE isDeleted = 0 ORDER BY issueDate DESC, id DESC")
    fun getAllActiveInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :number")
    suspend fun getInvoiceByNumber(number: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE customerId = :customerId AND isDeleted = 0")
    fun getInvoicesForCustomer(customerId: Long): Flow<List<InvoiceEntity>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceWithDetails(id: Long): InvoiceWithDetails?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoicePayment(payment: InvoicePaymentEntity): Long

    @Query("SELECT * FROM invoice_payments WHERE invoiceId = :invoiceId")
    suspend fun getPaymentsForInvoice(invoiceId: Long): List<InvoicePaymentEntity>

    @Query("UPDATE invoices SET paidAmountMinor = paidAmountMinor + :amountMinor, balanceDueMinor = balanceDueMinor - :amountMinor WHERE id = :invoiceId")
    suspend fun recordPaymentOnInvoice(invoiceId: Long, amountMinor: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAdjustment(adjustment: InvoiceAdjustmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturn(invoiceReturn: InvoiceReturnEntity): Long

    @Query("UPDATE invoices SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteInvoice(id: Long, timestamp: Long = System.currentTimeMillis())
}
