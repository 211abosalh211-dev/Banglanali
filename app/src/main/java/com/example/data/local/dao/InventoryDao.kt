package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.inventory.CategoryEntity
import com.example.data.local.entity.inventory.InventoryCountEntity
import com.example.data.local.entity.inventory.ProductEntity
import com.example.data.local.entity.inventory.ProductUnitEntity
import com.example.data.local.entity.inventory.StockAdjustmentEntity
import com.example.data.local.entity.inventory.StockBalanceEntity
import com.example.data.local.entity.inventory.StockTransactionEntity
import com.example.data.local.entity.inventory.StockTransferEntity
import com.example.data.local.entity.inventory.UnitOfMeasureEntity
import com.example.data.local.entity.inventory.WarehouseEntity
import com.example.data.local.relation.ProductWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // Warehouses
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWarehouse(warehouse: WarehouseEntity): Long

    @Query("SELECT * FROM warehouses WHERE isDeleted = 0")
    fun getAllActiveWarehouses(): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses WHERE id = :id")
    suspend fun getWarehouseById(id: Long): WarehouseEntity?

    // Categories
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM categories WHERE isActive = 1")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    // Units of Measure
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUom(uom: UnitOfMeasureEntity): Long

    @Query("SELECT * FROM units_of_measure")
    fun getAllUoms(): Flow<List<UnitOfMeasureEntity>>

    // Products
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY nameAr ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE productCode = :code")
    suspend fun getProductByCode(code: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM stock_transactions WHERE productId = :productId")
    suspend fun countTransactionsForProduct(productId: Long): Int

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun hardDeleteProduct(id: Long)

    @Query("UPDATE products SET isDeleted = 1, deletedAt = :timestamp, isActive = 0 WHERE id = :id")
    suspend fun softDeleteProduct(id: Long, timestamp: Long = System.currentTimeMillis())

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductWithDetails(id: Long): ProductWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductUnit(productUnit: ProductUnitEntity)

    // Stock Transactions
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockTransaction(tx: StockTransactionEntity): Long

    @Query("SELECT * FROM stock_transactions WHERE productId = :productId ORDER BY transactionDate DESC")
    fun getTransactionsForProduct(productId: Long): Flow<List<StockTransactionEntity>>

    // Stock Balances
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStockBalance(balance: StockBalanceEntity)

    @Query("SELECT * FROM stock_balances WHERE warehouseId = :warehouseId AND productId = :productId")
    suspend fun getBalance(warehouseId: Long, productId: Long): StockBalanceEntity?

    @Query("SELECT * FROM stock_balances WHERE productId = :productId")
    fun getBalancesForProduct(productId: Long): Flow<List<StockBalanceEntity>>

    // Adjustments, Transfers, Counts
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAdjustment(adj: StockAdjustmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransfer(transfer: StockTransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInventoryCount(count: InventoryCountEntity): Long
}
