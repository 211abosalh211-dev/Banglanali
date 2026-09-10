package com.example.data.local.entity.purchasing

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.local.entity.inventory.ProductEntity
import com.example.data.local.entity.inventory.WarehouseEntity
import com.example.data.local.entity.supplier.SupplierEntity

@Entity(
    tableName = "purchase_invoices",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
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
        Index(value = ["purchaseNumber"], unique = true),
        Index(value = ["supplierId"]),
        Index(value = ["warehouseId"]),
        Index(value = ["status"])
    ]
)
data class PurchaseInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseNumber: String, // e.g. "PUR-000001"
    val supplierId: Long,
    val warehouseId: Long,
    val supplierInvoiceRef: String? = null,
    val invoiceDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis(),
    val subtotalMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalAmountMinor: Long,
    val paidAmountMinor: Long = 0L,
    val status: String = "RECEIVED", // DRAFT, RECEIVED, PARTIALLY_PAID, PAID, CANCELLED
    val notes: String? = null,
    val createdBy: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseInvoiceId"],
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
        Index(value = ["purchaseInvoiceId"]),
        Index(value = ["productId"])
    ]
)
data class PurchaseInvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseInvoiceId: Long,
    val productId: Long,
    val quantity: Double,
    val unitCostMinor: Long,
    val subtotalMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalMinor: Long
)

@Entity(
    tableName = "purchase_payments",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseInvoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["purchaseInvoiceId"]),
        Index(value = ["paymentDate"])
    ]
)
data class PurchasePaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseInvoiceId: Long,
    val paymentReferenceNumber: String, // e.g. "PAY-000001"
    val amountPaidMinor: Long,
    val paymentMethod: String = "CASH",
    val cashboxId: Long? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val paidBy: String,
    val notes: String? = null
)

@Entity(
    tableName = "purchase_returns",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseInvoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["returnNumber"], unique = true),
        Index(value = ["purchaseInvoiceId"])
    ]
)
data class PurchaseReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnNumber: String, // e.g. "PRET-000001"
    val purchaseInvoiceId: Long,
    val returnAmountMinor: Long,
    val reason: String,
    val processedBy: String,
    val returnDate: Long = System.currentTimeMillis()
)
