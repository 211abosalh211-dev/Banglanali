package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.SystemConfigEntity
import com.example.data.local.entity.accounting.*
import com.example.data.local.entity.backup.BackupMetadataEntity
import com.example.data.local.entity.cashbox.*
import com.example.data.local.entity.customer.*
import com.example.data.local.entity.expense.*
import com.example.data.local.entity.hotel.*
import com.example.data.local.entity.inventory.*
import com.example.data.local.entity.invoice.*
import com.example.data.local.entity.payment.*
import com.example.data.local.entity.purchasing.*
import com.example.data.local.entity.reservation.*
import com.example.data.local.entity.sales.*
import com.example.data.local.entity.service.*
import com.example.data.local.entity.supplier.*
import com.example.data.local.entity.system.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        // System & Auth (Phase 01 & 02)
        SystemConfigEntity::class,
        AuditLogEntity::class,
        UserEntity::class,
        RoleEntity::class,
        PermissionEntity::class,
        RolePermissionEntity::class,
        UserRoleEntity::class,

        // Customers
        CustomerEntity::class,
        CustomerNoteEntity::class,
        CustomerDocumentEntity::class,

        // Hotel Units
        UnitTypeEntity::class,
        UnitEntity::class,
        UnitFeatureEntity::class,
        UnitTypeFeatureEntity::class,
        UnitStatusHistoryEntity::class,

        // Reservations & Stays
        ReservationEntity::class,
        ReservationItemEntity::class,
        GuestEntity::class,
        StayEntity::class,
        StayGuestEntity::class,
        UnitAssignmentEntity::class,
        CheckInRecordEntity::class,
        CheckOutRecordEntity::class,

        // Services
        ServiceCategoryEntity::class,
        ServiceEntity::class,
        ServiceTransactionEntity::class,

        // Accounting
        AccountGroupEntity::class,
        AccountEntity::class,
        FiscalPeriodEntity::class,
        JournalEntryEntity::class,
        JournalEntryLineEntity::class,

        // Cashboxes & Shifts
        CashboxEntity::class,
        CashboxTransactionEntity::class,
        CashboxTransferEntity::class,
        ShiftEntity::class,
        ShiftTransactionEntity::class,
        ShiftClosureEntity::class,

        // Invoicing
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        InvoicePaymentEntity::class,
        InvoiceAdjustmentEntity::class,
        InvoiceReturnEntity::class,

        // Suppliers
        SupplierEntity::class,
        SupplierTransactionEntity::class,

        // Inventory
        WarehouseEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        UnitOfMeasureEntity::class,
        ProductUnitEntity::class,
        StockTransactionEntity::class,
        StockBalanceEntity::class,
        StockAdjustmentEntity::class,
        StockTransferEntity::class,
        InventoryCountEntity::class,

        // Purchasing
        PurchaseInvoiceEntity::class,
        PurchaseInvoiceItemEntity::class,
        PurchasePaymentEntity::class,
        PurchaseReturnEntity::class,

        // Sales
        SalesInvoiceEntity::class,
        SalesInvoiceItemEntity::class,
        SalesPaymentEntity::class,
        SalesReturnEntity::class,

        // Expenses
        ExpenseCategoryEntity::class,
        ExpenseEntity::class,

        // Payments & Receipts
        PaymentMethodEntity::class,
        ReceiptEntity::class,
        ReceiptAllocationEntity::class,
        PaymentVoucherEntity::class,
        PaymentAllocationEntity::class,

        // Backup
        BackupMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HotelDatabase : RoomDatabase() {

    abstract fun systemConfigDao(): SystemConfigDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun systemSecurityDao(): SystemSecurityDao
    abstract fun customerDao(): CustomerDao
    abstract fun hotelUnitDao(): HotelUnitDao
    abstract fun reservationDao(): ReservationDao
    abstract fun serviceDao(): ServiceDao
    abstract fun accountingDao(): AccountingDao
    abstract fun cashboxDao(): CashboxDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun supplierDao(): SupplierDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun purchasingDao(): PurchasingDao
    abstract fun salesDao(): SalesDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentDao(): PaymentDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: HotelDatabase? = null

        fun getInstance(context: Context): HotelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HotelDatabase::class.java,
                    "hotel_erp_pro.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default system settings & accounts in background
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).seedDefaultConfigurations()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedDefaultConfigurations() {
        val configDao = systemConfigDao()
        val auditDao = auditLogDao()
        val accountingDao = accountingDao()
        val cashboxDao = cashboxDao()
        val hotelUnitDao = hotelUnitDao()
        val paymentDao = paymentDao()
        val inventoryDao = inventoryDao()

        // 1. System Configurations
        val initialConfigs = listOf(
            SystemConfigEntity("hotel_name", "فندق البرج الذهبي الملكي", "اسم المنشأة الفندقية"),
            SystemConfigEntity("currency_code", "YER", "العملة الافتراضية للنظام"),
            SystemConfigEntity("currency_name", "ريال يمني", "اسم العملة بالعربية"),
            SystemConfigEntity("tax_percentage", "0", "نسبة الضريبة العامة %"),
            SystemConfigEntity("check_in_time", "14:00", "وقت تسجيل الوصول القياسي"),
            SystemConfigEntity("check_out_time", "12:00", "وقت تسجيل المغادرة القياسي"),
            SystemConfigEntity("system_status", "ACTIVE", "حالة النظام التشغيلية"),
            SystemConfigEntity("audit_logging", "ENABLED", "تفعيل سجل الرقابة والتدقيق"),
            SystemConfigEntity("db_schema_version", "2", "إصدار هيكل قاعدة البيانات")
        )
        configDao.insertAll(initialConfigs)

        // 2. Accounting Groups
        val accountGroups = listOf(
            AccountGroupEntity(id = 1, groupCode = "1000", groupNameAr = "الأصول", groupNameEn = "Assets", normalBalance = "DEBIT"),
            AccountGroupEntity(id = 2, groupCode = "2000", groupNameAr = "الخصوم (الالتزامات)", groupNameEn = "Liabilities", normalBalance = "CREDIT"),
            AccountGroupEntity(id = 3, groupCode = "3000", groupNameAr = "حقوق الملكية", groupNameEn = "Equity", normalBalance = "CREDIT"),
            AccountGroupEntity(id = 4, groupCode = "4000", groupNameAr = "الإيرادات", groupNameEn = "Revenues", normalBalance = "CREDIT"),
            AccountGroupEntity(id = 5, groupCode = "5000", groupNameAr = "المصروفات", groupNameEn = "Expenses", normalBalance = "DEBIT")
        )
        accountingDao.insertAccountGroups(accountGroups)

        // 3. Chart of Accounts (COA)
        val defaultAccounts = listOf(
            AccountEntity(id = 1, accountCode = "10101", accountNameAr = "الصندوق الرئيسي", accountGroupId = 1, accountType = "ASSET", isSystemAccount = true),
            AccountEntity(id = 2, accountCode = "10102", accountNameAr = "صندوق الاستقبال 1", accountGroupId = 1, accountType = "ASSET", isSystemAccount = true),
            AccountEntity(id = 3, accountCode = "10201", accountNameAr = "الحساب البنكي الرئيسي", accountGroupId = 1, accountType = "ASSET", isSystemAccount = true),
            AccountEntity(id = 4, accountCode = "10301", accountNameAr = "العملاء (مدينون)", accountGroupId = 1, accountType = "ASSET", isSystemAccount = true),
            AccountEntity(id = 5, accountCode = "10401", accountNameAr = "مخزون البضائع", accountGroupId = 1, accountType = "ASSET", isSystemAccount = true),
            AccountEntity(id = 6, accountCode = "20101", accountNameAr = "الموردون (دائنون)", accountGroupId = 2, accountType = "LIABILITY", isSystemAccount = true),
            AccountEntity(id = 7, accountCode = "30101", accountNameAr = "رأس المال", accountGroupId = 3, accountType = "EQUITY", isSystemAccount = true),
            AccountEntity(id = 8, accountCode = "40101", accountNameAr = "إيرادات إقامة الغرف والوحدات", accountGroupId = 4, accountType = "REVENUE", isSystemAccount = true),
            AccountEntity(id = 9, accountCode = "40201", accountNameAr = "إيرادات خدمات فندقية ووجبات", accountGroupId = 4, accountType = "REVENUE", isSystemAccount = true),
            AccountEntity(id = 10, accountCode = "50101", accountNameAr = "مصروفات الصيانة والنظافة", accountGroupId = 5, accountType = "EXPENSE", isSystemAccount = true),
            AccountEntity(id = 11, accountCode = "50201", accountNameAr = "مصروفات الكهرباء والمياه", accountGroupId = 5, accountType = "EXPENSE", isSystemAccount = true)
        )
        accountingDao.insertAccounts(defaultAccounts)

        // 4. Default Cashbox
        cashboxDao.insertCashbox(
            CashboxEntity(
                id = 1,
                cashboxCode = "CB-MAIN",
                nameAr = "صندوق الاستقبال الرئيسي",
                glAccountId = 1,
                isPrimary = true
            )
        )

        // 5. Payment Methods
        val paymentMethods = listOf(
            PaymentMethodEntity(methodCode = "CASH", nameAr = "نقداً (كاش)", requiresReferenceNumber = false),
            PaymentMethodEntity(methodCode = "CARD_MADA", nameAr = "بطاقة مدى / دفع إلكتروني", requiresReferenceNumber = true),
            PaymentMethodEntity(methodCode = "BANK_TRANSFER", nameAr = "تحويل بنكي / إشعار سداد", requiresReferenceNumber = true)
        )
        paymentDao.insertPaymentMethods(paymentMethods)

        // 6. Unit Types
        val unitTypes = listOf(
            UnitTypeEntity(id = 1, typeCode = "SINGLE_STD", nameAr = "غرفة فردية قياسية", nameEn = "Standard Single Room", basePriceMinor = 1500000L, defaultMaxGuests = 1),
            UnitTypeEntity(id = 2, typeCode = "DOUBLE_DLX", nameAr = "غرفة مزدوجة ديلوكس", nameEn = "Deluxe Double Room", basePriceMinor = 2500000L, defaultMaxGuests = 2),
            UnitTypeEntity(id = 3, typeCode = "SUITE_ROYAL", nameAr = "جناح ملكي فاخر", nameEn = "Royal Luxury Suite", basePriceMinor = 5500000L, defaultMaxGuests = 4)
        )
        for (ut in unitTypes) {
            hotelUnitDao.insertUnitType(ut)
        }

        // 7. Inventory Warehouse, Categories & Units of Measure
        inventoryDao.insertWarehouse(
            WarehouseEntity(
                id = 1,
                warehouseCode = "WH-MAIN",
                warehouseNameAr = "المستودع الرئيسي للفندق",
                isPrimary = true
            )
        )
        inventoryDao.insertCategory(CategoryEntity(id = 1, categoryCode = "BEV", nameAr = "مشروبات ومياه"))
        inventoryDao.insertCategory(CategoryEntity(id = 2, categoryCode = "FOOD", nameAr = "أغذية ومأكولات"))
        inventoryDao.insertCategory(CategoryEntity(id = 3, categoryCode = "AMENITIES", nameAr = "مستلزمات النزلاء والغرف"))
        inventoryDao.insertUom(UnitOfMeasureEntity(uomCode = "PCS", nameAr = "حبة / قطعة", symbolAr = "قطعة"))
        inventoryDao.insertUom(UnitOfMeasureEntity(uomCode = "KG", nameAr = "كيلوجرام", symbolAr = "كجم"))

        // 8. Audit Log
        val initAudit = AuditLogEntity(
            operatorName = "النظام (مدير النظام)",
            operatorRole = "ADMIN",
            actionType = "MIGRATE",
            entityType = "SYSTEM",
            entityId = "SYS-PHASE-02",
            details = "تمت ترقية محرك البيانات وربط كافة جداول ERP المحاسبية والفندقية بنجاح (المرحلة 02)",
            previousValue = "Schema v1",
            newValue = "Schema v2"
        )
        auditDao.insertLog(initAudit)
    }
}
