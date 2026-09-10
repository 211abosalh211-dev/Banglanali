package com.example.data.local.entity.sales

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.inventory.ProductEntity
import com.example.data.local.entity.inventory.WarehouseEntity

@Entity(
    tableName = "sales_invoices",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["saleNumber"], unique = true),
        Index(value = ["customerId"]),
        Index(value = ["warehouseId"]),
        Index(value = ["status"])
    ]
)
data class SalesInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleNumber: String, // e.g. "SAL-000001"
    val customerId: Long,
    val warehouseId: Long,
    val saleDate: Long = System.currentTimeMillis(),
    val subtotalMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalAmountMinor: Long,
    val paidAmountMinor: Long = 0L,
    val status: String = "ISSUED", // DRAFT, ISSUED, PARTIALLY_PAID, PAID, CANCELLED
    val notes: String? = null,
    val createdBy: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales_invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = SalesInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["salesInvoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["salesInvoiceId"]),
        Index(value = ["productId"])
    ]
)
data class SalesInvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val salesInvoiceId: Long,
    val productId: Long,
    val quantity: Double,
    val unitPriceMinor: Long,
    val subtotalMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalMinor: Long
)

@Entity(
    tableName = "sales_payments",
    foreignKeys = [
        ForeignKey(
            entity = SalesInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["salesInvoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["salesInvoiceId"]),
        Index(value = ["paymentDate"])
    ]
)
data class SalesPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val salesInvoiceId: Long,
    val paymentReferenceNumber: String, // e.g. "REC-000001"
    val amountPaidMinor: Long,
    val paymentMethod: String = "CASH",
    val cashboxId: Long? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val receivedBy: String,
    val notes: String? = null
)

@Entity(
    tableName = "sales_returns",
    foreignKeys = [
        ForeignKey(
            entity = SalesInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["salesInvoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["returnNumber"], unique = true),
        Index(value = ["salesInvoiceId"])
    ]
)
data class SalesReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnNumber: String, // e.g. "SRET-000001"
    val salesInvoiceId: Long,
    val returnAmountMinor: Long,
    val reason: String,
    val processedBy: String,
    val returnDate: Long = System.currentTimeMillis()
)
