package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.inventory.ProductEntity
import com.example.data.local.entity.inventory.StockBalanceEntity
import com.example.data.local.entity.inventory.StockTransactionEntity
import com.example.data.local.relation.ProductWithDetails
import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val database: HotelDatabase) {

    private val inventoryDao = database.inventoryDao()

    fun getAllActiveProducts(): Flow<List<ProductEntity>> = inventoryDao.getAllActiveProducts()

    suspend fun getProductWithDetails(id: Long): ProductWithDetails? =
        inventoryDao.getProductWithDetails(id)

    suspend fun createProduct(product: ProductEntity): Resource<Long> {
        return try {
            val id = inventoryDao.insertProduct(product)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error("فشل في إضافة المنتج: ${e.localizedMessage}")
        }
    }

    /**
     * Stock Movement Integrity:
     * Never update stock balance directly without an audit StockTransaction!
     */
    suspend fun recordStockMovement(
        warehouseId: Long,
        productId: Long,
        transactionNumber: String,
        type: String, // PURCHASE_IN, SALE_OUT, ADJUSTMENT
        quantityDelta: Double, // positive for in, negative for out
        unitCostMinor: Long,
        referenceType: String? = null,
        referenceId: String? = null,
        operatorName: String,
        notes: String? = null
    ): Resource<Long> {
        return try {
            val txId = database.withTransaction {
                val currentBalance = inventoryDao.getBalance(warehouseId, productId)
                val prevQty = currentBalance?.currentQuantity ?: 0.0
                val newQty = prevQty + quantityDelta

                if (newQty < 0.0) {
                    throw IllegalStateException("الرصيد المتبقي في المستودع غير كافٍ لإتمام الصرف")
                }

                val totalCostMinor = (Math.abs(quantityDelta) * unitCostMinor).toLong()

                val tx = StockTransactionEntity(
                    transactionNumber = transactionNumber,
                    warehouseId = warehouseId,
                    productId = productId,
                    transactionType = type,
                    quantity = quantityDelta,
                    unitCostMinor = unitCostMinor,
                    totalCostMinor = totalCostMinor,
                    balanceAfterQuantity = newQty,
                    referenceType = referenceType,
                    referenceId = referenceId,
                    notes = notes,
                    operatorName = operatorName
                )

                val id = inventoryDao.insertStockTransaction(tx)

                inventoryDao.upsertStockBalance(
                    StockBalanceEntity(
                        warehouseId = warehouseId,
                        productId = productId,
                        currentQuantity = newQty,
                        averageCostMinor = unitCostMinor,
                        lastUpdated = System.currentTimeMillis()
                    )
                )

                id
            }
            Resource.Success(txId)
        } catch (e: Exception) {
            Resource.Error("فشل في تسجيل حركة المخزون: ${e.localizedMessage}")
        }
    }

    /**
     * Historical Integrity:
     * If product has stock history, REJECT hard delete and soft-delete instead.
     */
    suspend fun safeDeleteProduct(productId: Long): Resource<Boolean> {
        val txCount = inventoryDao.countTransactionsForProduct(productId)
        return if (txCount > 0) {
            inventoryDao.softDeleteProduct(productId)
            Resource.Error("لا يمكن الحذف النهائي للمنتج لوجود $txCount حركات مخزنية سابقة. تم نقل المنتج إلى الأرشيف (Soft Delete) لحماية السجلات.")
        } else {
            try {
                inventoryDao.hardDeleteProduct(productId)
                Resource.Success(true)
            } catch (e: Exception) {
                inventoryDao.softDeleteProduct(productId)
                Resource.Error("تم رفض الحذف: ${e.localizedMessage}")
            }
        }
    }
}
