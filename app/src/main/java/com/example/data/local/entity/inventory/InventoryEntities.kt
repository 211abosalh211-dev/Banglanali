package com.example.data.local.entity.inventory

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "warehouses",
    indices = [Index(value = ["warehouseCode"], unique = true)]
)
data class WarehouseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val warehouseCode: String, // e.g. "WH-MAIN", "WH-KITCHEN", "WH-LINEN"
    val warehouseNameAr: String,
    val locationDescription: String? = null,
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["categoryCode"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryCode: String, // e.g. "BEV", "FOOD", "AMENITIES", "CLEANING"
    val nameAr: String,
    val parentCategoryId: Long? = null,
    val isActive: Boolean = true
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["productCode"], unique = true),
        Index(value = ["barcode"], unique = false),
        Index(value = ["categoryId"]),
        Index(value = ["isDeleted"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productCode: String, // e.g. "PRD-001"
    val barcode: String? = null,
    val nameAr: String,
    val nameEn: String? = null,
    val categoryId: Long,
    val defaultCostPriceMinor: Long = 0L,
    val defaultSalePriceMinor: Long = 0L,
    val minReorderLevel: Double = 5.0,
    val trackInventory: Boolean = true,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "units_of_measure",
    indices = [Index(value = ["uomCode"], unique = true)]
)
data class UnitOfMeasureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uomCode: String, // e.g. "PCS", "KG", "BOX", "BOTTLE"
    val nameAr: String,
    val symbolAr: String
)

@Entity(
    tableName = "product_units",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UnitOfMeasureEntity::class,
            parentColumns = ["id"],
            childColumns = ["uomId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["uomId"])
    ]
)
data class ProductUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val uomId: Long,
    val conversionFactor: Double = 1.0, // Quantity of base unit in this unit
    val isBaseUnit: Boolean = true,
    val barcode: String? = null
)

@Entity(
    tableName = "stock_transactions",
    foreignKeys = [
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transactionNumber"], unique = true),
        Index(value = ["warehouseId"]),
        Index(value = ["productId"]),
        Index(value = ["transactionDate"]),
        Index(value = ["referenceType", "referenceId"])
    ]
)
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionNumber: String, // e.g. "STK-000001"
    val warehouseId: Long,
    val productId: Long,
    val transactionType: String, // PURCHASE_IN, SALE_OUT, RETURN_IN, RETURN_OUT, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT
    val quantity: Double, // positive for incoming, negative for outgoing
    val unitCostMinor: Long,
    val totalCostMinor: Long,
    val balanceAfterQuantity: Double,
    val referenceType: String? = null, // PURCHASE_INVOICE, SALES_INVOICE, STOCK_ADJUSTMENT
    val referenceId: String? = null,
    val notes: String? = null,
    val operatorName: String,
    val transactionDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_balances",
    primaryKeys = ["warehouseId", "productId"],
    foreignKeys = [
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["warehouseId"]),
        Index(value = ["productId"])
    ]
)
data class StockBalanceEntity(
    val warehouseId: Long,
    val productId: Long,
    val currentQuantity: Double = 0.0,
    val reservedQuantity: Double = 0.0,
    val averageCostMinor: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["adjustmentNumber"], unique = true),
        Index(value = ["warehouseId"])
    ]
)
data class StockAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val adjustmentNumber: String, // e.g. "SADJ-000001"
    val warehouseId: Long,
    val adjustmentDate: Long = System.currentTimeMillis(),
    val reason: String,
    val approvedBy: String,
    val totalValueMinor: Long = 0L
)

@Entity(
    tableName = "stock_transfers",
    foreignKeys = [
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromWarehouseId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["toWarehouseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transferNumber"], unique = true),
        Index(value = ["fromWarehouseId"]),
        Index(value = ["toWarehouseId"])
    ]
)
data class StockTransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transferNumber: String, // e.g. "STRF-000001"
    val fromWarehouseId: Long,
    val toWarehouseId: Long,
    val status: String = "COMPLETED", // PENDING, COMPLETED, CANCELLED
    val transferDate: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val transferredBy: String
)

@Entity(
    tableName = "inventory_counts",
    foreignKeys = [
        ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["countNumber"], unique = true),
        Index(value = ["warehouseId"])
    ]
)
data class InventoryCountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val countNumber: String, // e.g. "CNT-000001"
    val warehouseId: Long,
    val countDate: Long = System.currentTimeMillis(),
    val status: String = "DRAFT", // DRAFT, POSTED, CANCELLED
    val countedBy: String,
    val notes: String? = null
)
