package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.purchasing.PurchaseInvoiceEntity
import com.example.data.local.entity.purchasing.PurchaseInvoiceItemEntity
import com.example.data.local.entity.purchasing.PurchasePaymentEntity
import com.example.data.local.entity.purchasing.PurchaseReturnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchasingDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseInvoice(invoice: PurchaseInvoiceEntity): Long

    @Update
    suspend fun updatePurchaseInvoice(invoice: PurchaseInvoiceEntity)

    @Query("SELECT * FROM purchase_invoices WHERE isDeleted = 0 ORDER BY invoiceDate DESC")
    fun getAllActivePurchases(): Flow<List<PurchaseInvoiceEntity>>

    @Query("SELECT * FROM purchase_invoices WHERE id = :id")
    suspend fun getPurchaseById(id: Long): PurchaseInvoiceEntity?

    @Query("SELECT * FROM purchase_invoices WHERE purchaseNumber = :number")
    suspend fun getPurchaseByNumber(number: String): PurchaseInvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseItems(items: List<PurchaseInvoiceItemEntity>)

    @Query("SELECT * FROM purchase_invoice_items WHERE purchaseInvoiceId = :purchaseId")
    suspend fun getItemsForPurchase(purchaseId: Long): List<PurchaseInvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: PurchasePaymentEntity): Long

    @Query("SELECT * FROM purchase_payments WHERE purchaseInvoiceId = :purchaseId")
    fun getPaymentsForPurchase(purchaseId: Long): Flow<List<PurchasePaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturn(purchaseReturn: PurchaseReturnEntity): Long
}
