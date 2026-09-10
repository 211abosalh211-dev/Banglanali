package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.sales.SalesInvoiceEntity
import com.example.data.local.entity.sales.SalesInvoiceItemEntity
import com.example.data.local.entity.sales.SalesPaymentEntity
import com.example.data.local.entity.sales.SalesReturnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSalesInvoice(invoice: SalesInvoiceEntity): Long

    @Update
    suspend fun updateSalesInvoice(invoice: SalesInvoiceEntity)

    @Query("SELECT * FROM sales_invoices WHERE isDeleted = 0 ORDER BY saleDate DESC")
    fun getAllActiveSales(): Flow<List<SalesInvoiceEntity>>

    @Query("SELECT * FROM sales_invoices WHERE id = :id")
    suspend fun getSaleById(id: Long): SalesInvoiceEntity?

    @Query("SELECT * FROM sales_invoices WHERE saleNumber = :number")
    suspend fun getSaleByNumber(number: String): SalesInvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSalesItems(items: List<SalesInvoiceItemEntity>)

    @Query("SELECT * FROM sales_invoice_items WHERE salesInvoiceId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SalesInvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: SalesPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturn(salesReturn: SalesReturnEntity): Long
}
