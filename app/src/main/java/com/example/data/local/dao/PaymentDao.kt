package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.payment.PaymentAllocationEntity
import com.example.data.local.entity.payment.PaymentMethodEntity
import com.example.data.local.entity.payment.PaymentVoucherEntity
import com.example.data.local.entity.payment.ReceiptAllocationEntity
import com.example.data.local.entity.payment.ReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    // Payment Methods
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPaymentMethod(method: PaymentMethodEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPaymentMethods(methods: List<PaymentMethodEntity>)

    @Query("SELECT * FROM payment_methods WHERE isActive = 1")
    fun getAllPaymentMethods(): Flow<List<PaymentMethodEntity>>

    // Receipts
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts WHERE isDeleted = 0 ORDER BY receiptDate DESC")
    fun getAllActiveReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: Long): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE receiptNumber = :number")
    suspend fun getReceiptByNumber(number: String): ReceiptEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceiptAllocation(allocation: ReceiptAllocationEntity): Long

    @Query("SELECT * FROM receipt_allocations WHERE receiptId = :receiptId")
    suspend fun getAllocationsForReceipt(receiptId: Long): List<ReceiptAllocationEntity>

    // Payments
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: PaymentVoucherEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentVoucherEntity)

    @Query("SELECT * FROM payments WHERE isDeleted = 0 ORDER BY paymentDate DESC")
    fun getAllActivePayments(): Flow<List<PaymentVoucherEntity>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): PaymentVoucherEntity?

    @Query("SELECT * FROM payments WHERE paymentNumber = :number")
    suspend fun getPaymentByNumber(number: String): PaymentVoucherEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPaymentAllocation(allocation: PaymentAllocationEntity): Long

    @Query("SELECT * FROM payment_allocations WHERE paymentId = :paymentId")
    suspend fun getAllocationsForPayment(paymentId: Long): List<PaymentAllocationEntity>
}
