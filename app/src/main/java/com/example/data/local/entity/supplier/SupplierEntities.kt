package com.example.data.local.entity.supplier

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["supplierCode"], unique = true),
        Index(value = ["phone"]),
        Index(value = ["isDeleted"])
    ]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierCode: String, // e.g. "SUP-0001"
    val companyName: String,
    val contactPerson: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val taxNumber: String? = null,
    val currentBalanceMinor: Long = 0L, // Positive = Payable (مستحق للمورد), Negative = Advance (دفعة مقدمة)
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "supplier_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transactionNumber"], unique = true),
        Index(value = ["supplierId"]),
        Index(value = ["transactionDate"])
    ]
)
data class SupplierTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionNumber: String, // e.g. "SPT-000001"
    val supplierId: Long,
    val transactionType: String, // BILL, PAYMENT, RETURN, ADJUSTMENT
    val amountMinor: Long,
    val balanceAfterMinor: Long,
    val referenceType: String? = null, // PURCHASE_INVOICE, PAYMENT_VOUCHER
    val referenceId: String? = null,
    val notes: String? = null,
    val createdBy: String,
    val transactionDate: Long = System.currentTimeMillis()
)
