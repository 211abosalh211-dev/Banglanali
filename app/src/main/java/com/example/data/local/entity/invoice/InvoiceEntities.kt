package com.example.data.local.entity.invoice

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.reservation.StayEntity

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = StayEntity::class,
            parentColumns = ["id"],
            childColumns = ["stayId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["customerId"]),
        Index(value = ["stayId"]),
        Index(value = ["status"]),
        Index(value = ["issueDate"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String, // e.g. "INV-000001"
    val customerId: Long,
    val stayId: Long? = null,
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis(),
    val currencyCode: String = "YER",
    val subtotalMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalAmountMinor: Long,
    val paidAmountMinor: Long = 0L,
    val balanceDueMinor: Long,
    val status: String = "ISSUED", // DRAFT, ISSUED, PARTIALLY_PAID, PAID, CANCELLED, REFUNDED
    val notes: String? = null,
    val createdBy: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val itemType: String, // ROOM_STAY, SERVICE, PRODUCT, MINI_BAR, LAUNDRY, EXTRA
    val description: String,
    val quantity: Int = 1,
    val unitPriceMinor: Long,
    val subtotalMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalMinor: Long
)

@Entity(
    tableName = "invoice_payments",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["invoiceId"]),
        Index(value = ["paymentDate"])
    ]
)
data class InvoicePaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val paymentReferenceNumber: String, // e.g. "REC-000001"
    val amountPaidMinor: Long,
    val paymentMethod: String, // CASH, CARD, BANK_TRANSFER, CHEQUE
    val cashboxId: Long? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val receivedBy: String,
    val notes: String? = null
)

@Entity(
    tableName = "invoice_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["adjustmentNumber"], unique = true),
        Index(value = ["invoiceId"])
    ]
)
data class InvoiceAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val adjustmentNumber: String, // e.g. "ADJ-000001"
    val invoiceId: Long,
    val adjustmentType: String, // CREDIT_NOTE, DEBIT_NOTE, DISCOUNT, CORRECTION
    val amountMinor: Long,
    val reason: String,
    val approvedBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_returns",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["returnNumber"], unique = true),
        Index(value = ["invoiceId"])
    ]
)
data class InvoiceReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnNumber: String, // e.g. "RET-000001"
    val invoiceId: Long,
    val returnAmountMinor: Long,
    val reason: String,
    val processedBy: String,
    val returnDate: Long = System.currentTimeMillis()
)
