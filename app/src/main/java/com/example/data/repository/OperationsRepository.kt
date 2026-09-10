package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.currency.Money
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.accounting.JournalEntryLineEntity
import com.example.data.local.entity.inventory.ProductEntity
import com.example.data.local.entity.inventory.StockAdjustmentEntity
import com.example.data.local.entity.inventory.StockBalanceEntity
import com.example.data.local.entity.inventory.StockTransactionEntity
import com.example.data.local.entity.inventory.StockTransferEntity
import com.example.data.local.entity.inventory.WarehouseEntity
import com.example.data.local.entity.purchasing.PurchaseInvoiceEntity
import com.example.data.local.entity.purchasing.PurchaseInvoiceItemEntity
import com.example.data.local.entity.purchasing.PurchasePaymentEntity
import com.example.data.local.entity.sales.SalesInvoiceEntity
import com.example.data.local.entity.sales.SalesInvoiceItemEntity
import com.example.data.local.entity.sales.SalesPaymentEntity
import com.example.data.local.entity.sales.SalesReturnEntity
import com.example.data.local.entity.supplier.SupplierEntity
import com.example.data.local.entity.supplier.SupplierTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class OperationsRepository(
    private val database: HotelDatabase,
    private val accountingEngine: AccountingEngine
) {
    private val inventoryDao = database.inventoryDao()
    private val supplierDao = database.supplierDao()
    private val purchasingDao = database.purchasingDao()
    private val salesDao = database.salesDao()

    // 1. Inventory Products & Warehouses
    fun getAllProducts(): Flow<List<ProductEntity>> = inventoryDao.getAllActiveProducts()
    fun getAllWarehouses(): Flow<List<WarehouseEntity>> = inventoryDao.getAllActiveWarehouses()

    suspend fun createProduct(
        nameAr: String,
        categoryId: Long,
        purchasePriceMinor: Long,
        sellingPriceMinor: Long,
        initialStock: Double = 0.0,
        warehouseId: Long = 1L
    ): Resource<Long> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val code = "PRD-${System.currentTimeMillis() % 100000}"
                val product = ProductEntity(
                    productCode = code,
                    nameAr = nameAr.trim(),
                    categoryId = categoryId,
                    defaultCostPriceMinor = purchasePriceMinor,
                    defaultSalePriceMinor = sellingPriceMinor
                )
                val prodId = inventoryDao.insertProduct(product)

                if (initialStock > 0.0) {
                    inventoryDao.upsertStockBalance(
                        StockBalanceEntity(
                            warehouseId = warehouseId,
                            productId = prodId,
                            currentQuantity = initialStock,
                            averageCostMinor = purchasePriceMinor
                        )
                    )
                    inventoryDao.insertStockTransaction(
                        StockTransactionEntity(
                            transactionNumber = "TX-INIT-$code",
                            warehouseId = warehouseId,
                            productId = prodId,
                            transactionType = "PURCHASE_IN",
                            quantity = initialStock,
                            unitCostMinor = purchasePriceMinor,
                            totalCostMinor = (purchasePriceMinor * initialStock).toLong(),
                            balanceAfterQuantity = initialStock,
                            referenceType = "INITIAL_BALANCE",
                            referenceId = prodId.toString(),
                            operatorName = "SYSTEM"
                        )
                    )
                }
                prodId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل إنشاء الصنف: ${e.message}")
        }
    }

    // 2. Stock Transfer (نقل مخزني بين المستودعات)
    suspend fun transferStock(
        fromWarehouseId: Long,
        toWarehouseId: Long,
        productId: Long,
        quantity: Double
    ): Resource<Long> = withContext(Dispatchers.IO) {
        if (fromWarehouseId == toWarehouseId) return@withContext Resource.Error("لا يمكن التحويل إلى نفس المستودع")
        try {
            database.withTransaction {
                val sourceBalance = inventoryDao.getBalance(fromWarehouseId, productId)
                if (sourceBalance == null || sourceBalance.currentQuantity < quantity) {
                    throw IllegalStateException("الكمية المتوفرة في المستودع المصدر غير كافية")
                }
                val destBalance = inventoryDao.getBalance(toWarehouseId, productId)?.currentQuantity ?: 0.0

                inventoryDao.upsertStockBalance(sourceBalance.copy(currentQuantity = sourceBalance.currentQuantity - quantity))
                inventoryDao.upsertStockBalance(
                    StockBalanceEntity(warehouseId = toWarehouseId, productId = productId, currentQuantity = destBalance + quantity)
                )

                val transferNumber = "TRF-${System.currentTimeMillis() % 100000}"
                val transfer = StockTransferEntity(
                    transferNumber = transferNumber,
                    fromWarehouseId = fromWarehouseId,
                    toWarehouseId = toWarehouseId,
                    status = "COMPLETED",
                    transferredBy = "ADMIN"
                )
                val trfId = inventoryDao.insertTransfer(transfer)

                inventoryDao.insertStockTransaction(
                    StockTransactionEntity(
                        transactionNumber = "TX-OUT-$transferNumber",
                        warehouseId = fromWarehouseId,
                        productId = productId,
                        transactionType = "TRANSFER_OUT",
                        quantity = -quantity,
                        unitCostMinor = sourceBalance.averageCostMinor,
                        totalCostMinor = (sourceBalance.averageCostMinor * quantity).toLong(),
                        balanceAfterQuantity = sourceBalance.currentQuantity - quantity,
                        referenceType = "STOCK_TRANSFER",
                        referenceId = trfId.toString(),
                        operatorName = "ADMIN"
                    )
                )
                inventoryDao.insertStockTransaction(
                    StockTransactionEntity(
                        transactionNumber = "TX-IN-$transferNumber",
                        warehouseId = toWarehouseId,
                        productId = productId,
                        transactionType = "TRANSFER_IN",
                        quantity = quantity,
                        unitCostMinor = sourceBalance.averageCostMinor,
                        totalCostMinor = (sourceBalance.averageCostMinor * quantity).toLong(),
                        balanceAfterQuantity = destBalance + quantity,
                        referenceType = "STOCK_TRANSFER",
                        referenceId = trfId.toString(),
                        operatorName = "ADMIN"
                    )
                )
                trfId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل تحويل المخزون: ${e.message}")
        }
    }

    // 3. Suppliers & Purchasing (الموردين والمشتريات)
    fun getAllSuppliers(): Flow<List<SupplierEntity>> = supplierDao.getAllActiveSuppliers()

    suspend fun createSupplier(nameAr: String, phone: String, taxNumber: String = ""): Resource<Long> = withContext(Dispatchers.IO) {
        try {
            val code = "SUP-${System.currentTimeMillis() % 100000}"
            val supplier = SupplierEntity(
                supplierCode = code,
                companyName = nameAr.trim(),
                phone = phone.trim(),
                taxNumber = taxNumber.trim()
            )
            val id = supplierDao.insertSupplier(supplier)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error("فشل إنشاء المورد: ${e.message}")
        }
    }

    suspend fun recordPurchaseInvoice(
        supplierId: Long,
        warehouseId: Long,
        cashboxId: Long,
        productId: Long,
        quantity: Double,
        unitPriceMinor: Long,
        isPaidCash: Boolean
    ): Resource<Long> = withContext(Dispatchers.IO) {
        val totalAmountMinor = (quantity * unitPriceMinor).toLong()
        try {
            database.withTransaction {
                val supplier = supplierDao.getSupplierById(supplierId)
                    ?: throw IllegalStateException("المورد غير موجود")
                val product = inventoryDao.getProductById(productId)
                    ?: throw IllegalStateException("الصنف غير موجود")

                val invoiceNumber = "PUR-${System.currentTimeMillis() % 100000}"
                val invoice = PurchaseInvoiceEntity(
                    purchaseNumber = invoiceNumber,
                    supplierId = supplierId,
                    warehouseId = warehouseId,
                    subtotalMinor = totalAmountMinor,
                    totalAmountMinor = totalAmountMinor,
                    paidAmountMinor = if (isPaidCash) totalAmountMinor else 0L,
                    status = "RECEIVED",
                    createdBy = "ADMIN"
                )
                val purId = purchasingDao.insertPurchaseInvoice(invoice)

                purchasingDao.insertPurchaseItems(
                    listOf(
                        PurchaseInvoiceItemEntity(
                            purchaseInvoiceId = purId,
                            productId = productId,
                            quantity = quantity,
                            unitCostMinor = unitPriceMinor,
                            subtotalMinor = totalAmountMinor,
                            totalMinor = totalAmountMinor
                        )
                    )
                )

                // Update Stock
                val currentBal = inventoryDao.getBalance(warehouseId, productId)?.currentQuantity ?: 0.0
                inventoryDao.upsertStockBalance(
                    StockBalanceEntity(
                        warehouseId = warehouseId,
                        productId = productId,
                        currentQuantity = currentBal + quantity,
                        averageCostMinor = unitPriceMinor
                    )
                )

                inventoryDao.insertStockTransaction(
                    StockTransactionEntity(
                        transactionNumber = "TX-PUR-$invoiceNumber",
                        warehouseId = warehouseId,
                        productId = productId,
                        transactionType = "PURCHASE_IN",
                        quantity = quantity,
                        unitCostMinor = unitPriceMinor,
                        totalCostMinor = totalAmountMinor,
                        balanceAfterQuantity = currentBal + quantity,
                        referenceType = "PURCHASE_INVOICE",
                        referenceId = invoiceNumber,
                        operatorName = "ADMIN"
                    )
                )

                // Accounting Effect
                if (isPaidCash) {
                    accountingEngine.createPaymentVoucher(
                        paymentNumber = "PAY-$invoiceNumber",
                        supplierId = supplierId,
                        paidTo = supplier.companyName,
                        amountMinor = totalAmountMinor,
                        paymentMethodCode = "CASH",
                        cashboxId = cashboxId,
                        debitAccountId = 5L, // 10401 مخزون البضائع
                        referenceNo = invoiceNumber,
                        notes = "شراء أصناف نقداً فاتورة $invoiceNumber"
                    )
                } else {
                    // Credit purchase: Debit Inventory, Credit Supplier (20101)
                    supplierDao.updateBalance(supplierId, totalAmountMinor)
                    supplierDao.insertTransaction(
                        SupplierTransactionEntity(
                            transactionNumber = "SUP-TX-$invoiceNumber",
                            supplierId = supplierId,
                            transactionType = "BILL",
                            amountMinor = totalAmountMinor,
                            balanceAfterMinor = supplier.currentBalanceMinor + totalAmountMinor,
                            referenceType = "PURCHASE_INVOICE",
                            referenceId = invoiceNumber,
                            createdBy = "ADMIN"
                        )
                    )
                    val lines = listOf(
                        JournalEntryLineEntity(
                            entryId = 0,
                            accountId = 5L, // 10401 مخزون
                            debit = totalAmountMinor,
                            credit = 0L,
                            description = "مشتريات بضاعة $invoiceNumber"
                        ),
                        JournalEntryLineEntity(
                            entryId = 0,
                            accountId = 6L, // 20101 الموردون
                            debit = 0L,
                            credit = totalAmountMinor,
                            description = "استحقاق مورد ${supplier.companyName}"
                        )
                    )
                    accountingEngine.postJournalEntry(
                        entryNumber = "JV-PUR-$invoiceNumber",
                        description = "فاتورة مشتريات آجل رقم $invoiceNumber",
                        referenceType = "PURCHASE",
                        referenceId = invoiceNumber,
                        lines = lines
                    )
                }

                purId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل تسجيل فاتورة الشراء: ${e.message}")
        }
    }

    // 4. POS & Direct Sales (نقطة البيع والمبيعات المباشرة)
    fun getAllSales(): Flow<List<SalesInvoiceEntity>> = salesDao.getAllActiveSales()

    suspend fun performPosSale(
        cashboxId: Long,
        warehouseId: Long,
        productId: Long,
        quantity: Double,
        customerName: String = "عميل نقدي مباشر",
        customerId: Long = 1L
    ): Resource<Long> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val product = inventoryDao.getProductById(productId)
                    ?: throw IllegalStateException("المنتج غير موجود")
                val balance = inventoryDao.getBalance(warehouseId, productId)?.currentQuantity ?: 0.0
                if (balance < quantity) {
                    throw IllegalStateException("الرصيد المتبقي في المستودع (${balance}) غير كافٍ لعملية البيع")
                }

                val totalMinor = (quantity * product.defaultSalePriceMinor).toLong()
                val saleNumber = "POS-${System.currentTimeMillis() % 100000}"

                val sale = SalesInvoiceEntity(
                    saleNumber = saleNumber,
                    customerId = customerId,
                    warehouseId = warehouseId,
                    subtotalMinor = totalMinor,
                    totalAmountMinor = totalMinor,
                    paidAmountMinor = totalMinor,
                    status = "PAID",
                    createdBy = "CASHIER"
                )
                val saleId = salesDao.insertSalesInvoice(sale)

                salesDao.insertSalesItems(
                    listOf(
                        SalesInvoiceItemEntity(
                            salesInvoiceId = saleId,
                            productId = productId,
                            quantity = quantity,
                            unitPriceMinor = product.defaultSalePriceMinor,
                            subtotalMinor = totalMinor,
                            totalMinor = totalMinor
                        )
                    )
                )

                // Stock deduction
                inventoryDao.upsertStockBalance(
                    StockBalanceEntity(
                        warehouseId = warehouseId,
                        productId = productId,
                        currentQuantity = balance - quantity,
                        averageCostMinor = product.defaultCostPriceMinor
                    )
                )

                inventoryDao.insertStockTransaction(
                    StockTransactionEntity(
                        transactionNumber = "TX-SALE-$saleNumber",
                        warehouseId = warehouseId,
                        productId = productId,
                        transactionType = "SALE_OUT",
                        quantity = -quantity,
                        unitCostMinor = product.defaultCostPriceMinor,
                        totalCostMinor = (quantity * product.defaultCostPriceMinor).toLong(),
                        balanceAfterQuantity = balance - quantity,
                        referenceType = "SALES_INVOICE",
                        referenceId = saleNumber,
                        operatorName = "CASHIER"
                    )
                )

                // Cash receipt & Central Accounting Effect
                accountingEngine.createReceiptVoucher(
                    receiptNumber = "REC-$saleNumber",
                    customerId = customerId,
                    receivedFrom = customerName,
                    amountMinor = totalMinor,
                    paymentMethodCode = "CASH",
                    cashboxId = cashboxId,
                    creditAccountId = 9L, // 40201 إيرادات خدمات ومبيعات
                    referenceNo = saleNumber,
                    notes = "مبيعات نقطة البيع POS - فاتورة $saleNumber"
                )

                saleId
            }.let { Resource.Success(it) }
        } catch (e: Exception) {
            Resource.Error("فشل إتمام عملية البيع: ${e.message}")
        }
    }
}
