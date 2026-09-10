package com.example.data.local.entity.payment

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.local.entity.cashbox.CashboxEntity
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.invoice.InvoiceEntity
import com.example.data.local.entity.purchasing.PurchaseInvoiceEntity
import com.example.data.local.entity.supplier.SupplierEntity

@Entity(
    tableName = "payment_methods",
    indices = [Index(value = ["methodCode"], unique = true)]
)
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val methodCode: String, // CASH, CARD_VISA, CARD_MADA, BANK_TRANSFER, CHEQUE
    val nameAr: String,
    val requiresReferenceNumber: Boolean = false,
    val isActive: Boolean = true
)

@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CashboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["cashboxId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["receiptNumber"], unique = true),
        Index(value = ["customerId"]),
        Index(value = ["cashboxId"]),
        Index(value = ["receiptDate"])
    ]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptNumber: String, // e.g. "REC-000001"
    val customerId: Long,
    val cashboxId: Long,
    val amountMinor: Long,
    val paymentMethodCode: String = "CASH",
    val referenceNumber: String? = null,
    val receivedFrom: String,
    val description: String,
    val receiptDate: Long = System.currentTimeMillis(),
    val receivedBy: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "receipt_allocations",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["receiptId"]),
        Index(value = ["invoiceId"])
    ]
)
data class ReceiptAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptId: Long,
    val invoiceId: Long,
    val allocatedAmountMinor: Long
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CashboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["cashboxId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["paymentNumber"], unique = true),
        Index(value = ["supplierId"]),
        Index(value = ["cashboxId"]),
        Index(value = ["paymentDate"])
    ]
)
data class PaymentVoucherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paymentNumber: String, // e.g. "PAY-000001"
    val supplierId: Long? = null,
    val cashboxId: Long,
    val amountMinor: Long,
    val paymentMethodCode: String = "CASH",
    val referenceNumber: String? = null,
    val paidTo: String,
    val description: String,
    val paymentDate: Long = System.currentTimeMillis(),
    val paidBy: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payment_allocations",
    foreignKeys = [
        ForeignKey(
            entity = PaymentVoucherEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PurchaseInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseInvoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["paymentId"]),
        Index(value = ["purchaseInvoiceId"])
    ]
)
data class PaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paymentId: Long,
    val purchaseInvoiceId: Long,
    val allocatedAmountMinor: Long
)
