package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.currency.Money
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.MIGRATION_1_2
import com.example.data.local.entity.accounting.AccountEntity
import com.example.data.local.entity.accounting.JournalEntryLineEntity
import com.example.data.local.entity.cashbox.CashboxEntity
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.hotel.UnitEntity
import com.example.data.local.entity.hotel.UnitTypeEntity
import com.example.data.local.entity.inventory.ProductEntity
import com.example.data.local.entity.inventory.StockTransactionEntity
import com.example.data.local.entity.invoice.InvoiceEntity
import com.example.data.local.entity.invoice.InvoiceItemEntity
import com.example.data.local.entity.reservation.ReservationEntity
import com.example.data.local.entity.reservation.ReservationItemEntity
import com.example.data.repository.AccountingRepository
import com.example.data.repository.CashboxRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.InventoryRepository
import com.example.data.repository.InvoiceRepository
import com.example.data.repository.ReservationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase02DataIntegrityTest {

    private lateinit var database: HotelDatabase
    private lateinit var context: Context

    private lateinit var accountingRepo: AccountingRepository
    private lateinit var customerRepo: CustomerRepository
    private lateinit var reservationRepo: ReservationRepository
    private lateinit var cashboxRepo: CashboxRepository
    private lateinit var inventoryRepo: InventoryRepository
    private lateinit var invoiceRepo: InvoiceRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HotelDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        accountingRepo = AccountingRepository(database)
        customerRepo = CustomerRepository(database)
        reservationRepo = ReservationRepository(database)
        cashboxRepo = CashboxRepository(database)
        inventoryRepo = InventoryRepository(database)
        invoiceRepo = InvoiceRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test database seeding sets up default chart of accounts and cashbox`() = runTest {
        database.seedDefaultConfigurations()

        val accounts = database.accountingDao().getAllActiveAccounts().first()
        assertTrue("Accounts should be populated", accounts.isNotEmpty())
        assertEquals(11, accounts.size)

        val cashbox = database.cashboxDao().getCashboxByCode("CB-MAIN")
        assertNotNull("Main cashbox should be created", cashbox)
        assertEquals("صندوق الاستقبال الرئيسي", cashbox!!.nameAr)

        val unitTypes = database.hotelUnitDao().getAllUnitTypes().first()
        assertEquals(3, unitTypes.size)
    }

    @Test
    fun `test accounting integrity rejects unbalanced journal entry`() = runTest {
        database.seedDefaultConfigurations()

        // Attempt to post unbalanced entry: Debit 50,000 vs Credit 40,000
        val unbalancedLines = listOf(
            JournalEntryLineEntity(
                entryId = 0,
                accountId = 1, // Main Cashbox
                debit = 5000000L,
                credit = 0L,
                description = "استلام نقدية"
            ),
            JournalEntryLineEntity(
                entryId = 0,
                accountId = 8, // Room Revenue
                debit = 0L,
                credit = 4000000L, // 10,000 diff!
                description = "إيراد إقامة"
            )
        )

        val result = accountingRepo.postJournalEntry(
            entryNumber = "JV-TEST-01",
            description = "قيد تجريبي غير متوازن",
            lines = unbalancedLines
        )

        assertTrue("Unbalanced entry MUST be rejected", result is Resource.Error)
        val errorMessage = (result as Resource.Error).message
        assertTrue(errorMessage.contains("خطأ في توازن القيد المحاسبي"))

        // Verify that no lines or entries were saved
        val entries = database.accountingDao().getAllJournalEntries().first()
        assertTrue("No journal entries should exist", entries.isEmpty())
    }

    @Test
    fun `test accounting integrity posts balanced entry and updates balances atomically`() = runTest {
        database.seedDefaultConfigurations()

        val initialCashAccount = database.accountingDao().getAccountById(1)!!
        val initialRevenueAccount = database.accountingDao().getAccountById(8)!!
        assertEquals(0L, initialCashAccount.currentBalanceMinor)
        assertEquals(0L, initialRevenueAccount.currentBalanceMinor)

        // Balanced entry: Debit 30,000 == Credit 30,000
        val amount = 3000000L // 30,000.00 YER
        val balancedLines = listOf(
            JournalEntryLineEntity(
                entryId = 0,
                accountId = 1, // Cash (Asset increases with Debit)
                debit = amount,
                credit = 0L,
                description = "قبض إيراد حجز نقداً"
            ),
            JournalEntryLineEntity(
                entryId = 0,
                accountId = 8, // Revenue (Revenue increases with Credit)
                debit = 0L,
                credit = amount,
                description = "إيراد إقامة الغرفة"
            )
        )

        val result = accountingRepo.postJournalEntry(
            entryNumber = "JV-BALANCED-01",
            description = "قيد متوازن سليم",
            lines = balancedLines
        )

        assertTrue("Balanced entry must succeed", result is Resource.Success)

        // Verify account balance updates
        val updatedCash = database.accountingDao().getAccountById(1)!!
        val updatedRevenue = database.accountingDao().getAccountById(8)!!
        assertEquals(amount, updatedCash.currentBalanceMinor)
        assertEquals(amount, updatedRevenue.currentBalanceMinor)
    }

    @Test
    fun `test financial protection prevents hard deleting account with journal history`() = runTest {
        database.seedDefaultConfigurations()

        val amount = 1000000L
        val lines = listOf(
            JournalEntryLineEntity(entryId = 0, accountId = 1, debit = amount, credit = 0L, description = "debit"),
            JournalEntryLineEntity(entryId = 0, accountId = 8, debit = 0L, credit = amount, description = "credit")
        )
        accountingRepo.postJournalEntry("JV-HIST-01", "حركة مالية سابقة", lines = lines)

        // Attempting to delete account 1 (Cashbox account)
        val deleteResult = accountingRepo.deleteAccountSafe(1)

        // Must reject hard delete
        assertTrue("Hard delete must be rejected", deleteResult is Resource.Error)
        val deleteError = deleteResult as Resource.Error
        assertTrue(deleteError.message.contains("لا يمكن الحذف النهائي للحساب"))

        // Account must still exist in DB (Soft deleted)
        val account = database.accountingDao().getAccountById(1)
        assertNotNull(account)
        assertTrue(account!!.isDeleted)
        assertFalse(account.isActive)
    }

    @Test
    fun `test customer soft delete protection when invoices exist`() = runTest {
        database.seedDefaultConfigurations()

        // 1. Create customer
        val custId = database.customerDao().insertCustomer(
            CustomerEntity(
                customerCode = "CUST-001",
                fullName = "محمد عبدالله صالح",
                phone = "777123456"
            )
        )

        // 2. Issue an invoice for this customer
        database.invoiceDao().insertInvoice(
            InvoiceEntity(
                invoiceNumber = "INV-001",
                customerId = custId,
                subtotalMinor = 5000000L,
                totalAmountMinor = 5000000L,
                balanceDueMinor = 5000000L,
                createdBy = "ADMIN"
            )
        )

        // 3. Attempt to delete customer
        val deleteResult = customerRepo.safeDeleteCustomer(custId)

        // Must reject hard delete
        assertTrue("Hard delete must be rejected", deleteResult is Resource.Error)
        val customerError = deleteResult as Resource.Error
        assertTrue(customerError.message.contains("تم رفض الحذف النهائي للعميل لوجود 1 فواتير"))

        // Verify customer record is preserved as soft deleted
        val customer = database.customerDao().getCustomerById(custId)
        assertNull("Customer should not be returned by active query", customer)

        // Check details query
        val details = database.customerDao().getCustomerWithDetails(custId)
        assertNotNull("Customer still exists in database", details)
        assertTrue(details!!.customer.isDeleted)
    }

    @Test
    fun `test reservation double booking prevention`() = runTest {
        database.seedDefaultConfigurations()

        val custId = database.customerDao().insertCustomer(
            CustomerEntity(customerCode = "CUST-002", fullName = "علي يحيى", phone = "777000111")
        )

        val unitId = database.hotelUnitDao().insertUnit(
            UnitEntity(
                unitNumber = "101",
                unitTypeId = 1,
                floorNumber = 1
            )
        )

        val checkIn = 1750000000000L
        val checkOut = 1750500000000L

        // First reservation
        val res1Result = reservationRepo.createReservation(
            reservation = ReservationEntity(
                reservationNumber = "RES-001",
                customerId = custId,
                expectedCheckIn = checkIn,
                expectedCheckOut = checkOut,
                createdBy = "RECEPTION"
            ),
            items = listOf(
                ReservationItemEntity(
                    reservationId = 0,
                    unitTypeId = 1,
                    assignedUnitId = unitId,
                    checkInDate = checkIn,
                    checkOutDate = checkOut,
                    ratePerNightMinor = 1500000L,
                    nightsCount = 5,
                    subtotalMinor = 7500000L,
                    totalMinor = 7500000L
                )
            )
        )
        assertTrue("First reservation should succeed", res1Result is Resource.Success)

        // Second reservation with overlapping dates for the same unit
        val res2Result = reservationRepo.createReservation(
            reservation = ReservationEntity(
                reservationNumber = "RES-002",
                customerId = custId,
                expectedCheckIn = checkIn + 100000L, // Overlap!
                expectedCheckOut = checkOut + 100000L,
                createdBy = "RECEPTION"
            ),
            items = listOf(
                ReservationItemEntity(
                    reservationId = 0,
                    unitTypeId = 1,
                    assignedUnitId = unitId,
                    checkInDate = checkIn + 100000L,
                    checkOutDate = checkOut + 100000L,
                    ratePerNightMinor = 1500000L,
                    nightsCount = 5,
                    subtotalMinor = 7500000L,
                    totalMinor = 7500000L
                )
            )
        )

        assertTrue("Overlapping reservation MUST be rejected", res2Result is Resource.Error)
        val res2Error = res2Result as Resource.Error
        assertTrue(res2Error.message.contains("محجوزة أو مشغولة بالفعل"))
    }

    @Test
    fun `test composite invoice payment and cashbox transaction atomicity`() = runTest {
        database.seedDefaultConfigurations()

        val custId = database.customerDao().insertCustomer(
            CustomerEntity(customerCode = "CUST-003", fullName = "جمال سعيد", phone = "771234999")
        )

        val paidAmount = 4500000L // 45,000.00 YER

        val result = invoiceRepo.issueInvoiceAtomic(
            invoice = InvoiceEntity(
                invoiceNumber = "INV-ATOMIC-01",
                customerId = custId,
                subtotalMinor = paidAmount,
                totalAmountMinor = paidAmount,
                balanceDueMinor = 0L,
                createdBy = "ADMIN"
            ),
            items = listOf(
                InvoiceItemEntity(
                    invoiceId = 0,
                    itemType = "ROOM_STAY",
                    description = "إقامة جناح 3 ليالي",
                    quantity = 3,
                    unitPriceMinor = 1500000L,
                    subtotalMinor = paidAmount,
                    totalMinor = paidAmount
                )
            ),
            paidAmountMinor = paidAmount,
            cashboxId = 1L,
            cashboxGlAccountId = 1L,
            revenueGlAccountId = 8L
        )

        assertTrue("Composite atomic issuance should succeed", result is Resource.Success)
        val invoiceId = (result as Resource.Success).data!!

        // Verify Invoice & Payments
        val invoiceWithDetails = invoiceRepo.getInvoiceWithDetails(invoiceId)
        assertNotNull(invoiceWithDetails)
        assertEquals(1, invoiceWithDetails!!.items.size)
        assertEquals(1, invoiceWithDetails.payments.size)
        assertEquals(paidAmount, invoiceWithDetails.invoice.paidAmountMinor)

        // Verify Cashbox balance updated
        val cashbox = cashboxRepo.getCashboxById(1L)!!
        assertEquals(paidAmount, cashbox.currentBalanceMinor)

        // Verify Cashbox transaction created
        val txs = cashboxRepo.getAllTransactions().first()
        assertEquals(1, txs.size)
        assertEquals(paidAmount, txs.first().amountMinor)

        // Verify Balanced Journal Entry created
        val jv = database.accountingDao().getAllJournalEntries().first()
        assertEquals(1, jv.size)
        assertEquals(paidAmount, jv.first().totalDebitMinor)
        assertEquals(paidAmount, jv.first().totalCreditMinor)
    }

    @Test
    fun `test product soft delete protection when stock transactions exist`() = runTest {
        database.seedDefaultConfigurations()

        val prodId = inventoryRepo.createProduct(
            ProductEntity(
                productCode = "PRD-WATER",
                nameAr = "مياه معدنية 500 مل",
                categoryId = 1,
                defaultCostPriceMinor = 15000L,
                defaultSalePriceMinor = 25000L
            )
        ).let { (it as Resource.Success).data!! }

        // Record stock movement (Purchase In)
        val moveResult = inventoryRepo.recordStockMovement(
            warehouseId = 1L,
            productId = prodId,
            transactionNumber = "STK-IN-001",
            type = "PURCHASE_IN",
            quantityDelta = 100.0,
            unitCostMinor = 15000L,
            operatorName = "STORE_KEEPER"
        )
        assertTrue(moveResult is Resource.Success)

        // Verify stock balance
        val balance = database.inventoryDao().getBalance(1L, prodId)
        assertNotNull(balance)
        assertEquals(100.0, balance!!.currentQuantity, 0.001)

        // Attempt to delete product
        val deleteResult = inventoryRepo.safeDeleteProduct(prodId)
        assertTrue("Hard delete must be rejected", deleteResult is Resource.Error)
        val prodError = deleteResult as Resource.Error
        assertTrue(prodError.message.contains("لا يمكن الحذف النهائي للمنتج"))

        // Product is soft deleted
        val prod = database.inventoryDao().getProductById(prodId)
        assertNotNull(prod)
        assertTrue(prod!!.isDeleted)
    }

    @Test
    fun `test foreign key constraint rejects product with invalid category`() = runTest {
        database.seedDefaultConfigurations()

        // Category 99999 does not exist
        val invalidProductResult = inventoryRepo.createProduct(
            ProductEntity(
                productCode = "PRD-INVALID",
                nameAr = "منتج برقم تصنيف غير موجود",
                categoryId = 99999,
                defaultCostPriceMinor = 1000L,
                defaultSalePriceMinor = 2000L
            )
        )

        assertTrue("Product with invalid foreign key MUST be rejected", invalidProductResult is Resource.Error)
        val error = invalidProductResult as Resource.Error
        assertTrue(error.message.contains("فشل في إضافة المنتج"))
    }

    @Test
    fun `test cashbox transfer updates balances of both cashboxes atomically`() = runTest {
        database.seedDefaultConfigurations()

        // Create second cashbox
        val cashbox2Id = database.cashboxDao().insertCashbox(
            CashboxEntity(
                cashboxCode = "CB-AUX",
                nameAr = "صندوق الاستقبال 2",
                glAccountId = 2,
                currentBalanceMinor = 1000000L // 10,000 initial
            )
        )

        // Give initial balance to main cashbox: 50,000
        database.cashboxDao().updateBalance(1L, 5000000L)

        // Transfer 20,000 from main cashbox (1) to aux cashbox (cashbox2Id)
        val transferResult = cashboxRepo.transferBetweenCashboxes(
            transferNumber = "TRF-TEST-01",
            fromCashboxId = 1L,
            toCashboxId = cashbox2Id,
            amountMinor = 2000000L,
            operatorName = "SUPERVISOR"
        )

        assertTrue("Transfer must succeed", transferResult is Resource.Success)

        // Main Cashbox should be 50,000 - 20,000 = 30,000 (3,000,000 minor)
        val box1 = database.cashboxDao().getCashboxById(1L)!!
        assertEquals(3000000L, box1.currentBalanceMinor)

        // Aux Cashbox should be 10,000 + 20,000 = 30,000 (3,000,000 minor)
        val box2 = database.cashboxDao().getCashboxById(cashbox2Id)!!
        assertEquals(3000000L, box2.currentBalanceMinor)
    }

    @Test
    fun `test migration from v1 to v2 preserves existing phase 01 data`() {
        // Create an SQLite database conforming to Version 1 schema
        val dbName = "test_migration.db"
        context.deleteDatabase(dbName)

        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `system_config` (
                            `configKey` TEXT NOT NULL,
                            `configValue` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`configKey`)
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `audit_logs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `operatorName` TEXT NOT NULL,
                            `operatorRole` TEXT NOT NULL,
                            `actionType` TEXT NOT NULL,
                            `entityType` TEXT NOT NULL,
                            `entityId` TEXT NOT NULL,
                            `details` TEXT NOT NULL,
                            `previousValue` TEXT,
                            `newValue` TEXT,
                            `isDeleted` INTEGER NOT NULL,
                            `deletedAt` INTEGER,
                            `timestamp` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    // Insert Phase 01 seed data
                    db.execSQL("INSERT INTO `system_config` VALUES ('hotel_name', 'فندق المرحلة الأولى', 'اسم الفندق', 123456789)")
                    db.execSQL("INSERT INTO `audit_logs` VALUES (1, 'مدير المرحلة الأولى', 'ADMIN', 'CREATE', 'SYS', '1', 'تهيئة المرحلة 01', NULL, 'OK', 0, NULL, 123456789)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = helperFactory.create(config)
        val dbV1 = openHelper.writableDatabase

        // Execute MIGRATION_1_2
        MIGRATION_1_2.migrate(dbV1)

        // Verify Phase 01 data survived
        val cursorConfig = dbV1.query("SELECT configValue FROM system_config WHERE configKey = 'hotel_name'")
        assertTrue(cursorConfig.moveToFirst())
        assertEquals("فندق المرحلة الأولى", cursorConfig.getString(0))
        cursorConfig.close()

        val cursorAudit = dbV1.query("SELECT operatorName FROM audit_logs WHERE id = 1")
        assertTrue(cursorAudit.moveToFirst())
        assertEquals("مدير المرحلة الأولى", cursorAudit.getString(0))
        cursorAudit.close()

        // Verify new Phase 02 tables were created by migration
        val cursorCheckTable = dbV1.query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='customers'")
        assertTrue(cursorCheckTable.moveToFirst())
        assertEquals(1, cursorCheckTable.getInt(0))
        cursorCheckTable.close()

        val cursorCheckAccounts = dbV1.query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='journal_entries'")
        assertTrue(cursorCheckAccounts.moveToFirst())
        assertEquals(1, cursorCheckAccounts.getInt(0))
        cursorCheckAccounts.close()

        dbV1.close()
        context.deleteDatabase(dbName)
    }
}
