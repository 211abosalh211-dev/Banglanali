package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.currency.Money
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.accounting.AccountEntity
import com.example.data.local.entity.accounting.JournalEntryEntity
import com.example.data.local.entity.cashbox.CashboxEntity
import com.example.data.local.entity.customer.CustomerEntity
import com.example.data.local.entity.hotel.UnitEntity
import com.example.data.local.entity.hotel.UnitTypeEntity
import com.example.data.local.entity.inventory.ProductEntity
import com.example.data.local.entity.inventory.WarehouseEntity
import com.example.data.local.entity.reservation.ReservationEntity
import com.example.data.local.entity.reservation.StayEntity
import com.example.data.local.entity.sales.SalesInvoiceEntity
import com.example.data.local.entity.supplier.SupplierEntity
import com.example.data.local.entity.system.UserEntity
import com.example.data.local.entity.backup.BackupMetadataEntity
import com.example.data.repository.AccountingEngine
import com.example.data.repository.AuthRepository
import com.example.data.repository.BackupRestoreManager
import com.example.data.repository.HotelRepository
import com.example.data.repository.OperationsRepository
import com.example.data.repository.UserSession
import com.example.core.pdf.PdfReportGenerator
import android.content.Context
import android.net.Uri
import com.example.ui.navigation.NavDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ErpUiState(
    val session: UserSession? = null,
    val selectedDestination: NavDestination = NavDestination.DASHBOARD,
    val hotelName: String = "فندق البرج الذهبي الملكي",
    val defaultCurrency: String = Money.DEFAULT_CURRENCY,

    // Data lists
    val units: List<UnitEntity> = emptyList(),
    val unitTypes: List<UnitTypeEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val reservations: List<ReservationEntity> = emptyList(),
    val activeStays: List<StayEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val journalEntries: List<JournalEntryEntity> = emptyList(),
    val cashboxes: List<CashboxEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val warehouses: List<WarehouseEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val sales: List<SalesInvoiceEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val backups: List<BackupMetadataEntity> = emptyList(),
    val isBackupInProgress: Boolean = false,

    // UI feedback
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,
    val showExitConfirmDialog: Boolean = false
)

class ErpMasterViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HotelDatabase.getInstance(application)
    val authRepository = AuthRepository(database)
    val accountingEngine = AccountingEngine(database)
    val hotelRepository = HotelRepository(database, accountingEngine)
    val operationsRepository = OperationsRepository(database, accountingEngine)
    val backupRestoreManager = BackupRestoreManager(application, database)
    private val auditDao = database.auditLogDao()
    private val cashboxDao = database.cashboxDao()
    private val reservationDao = database.reservationDao()

    private val _uiState = MutableStateFlow(ErpUiState())
    val uiState: StateFlow<ErpUiState>

    init {
        // Initialize Security and Defaults
        viewModelScope.launch {
            authRepository.initializeSecurityDefaults()
            // Auto login default admin to make app immediately ready and testable
            authRepository.login("admin", "admin123")
            seedInitialHotelUnitsIfEmpty()
        }

        // Combine reactive database flows into unified ERP state
        uiState = combine(
            _uiState,
            authRepository.currentSession,
            hotelRepository.getAllUnits(),
            hotelRepository.getAllUnitTypes(),
            hotelRepository.getAllCustomers(),
            hotelRepository.getAllReservations(),
            accountingEngine.getAllAccounts(),
            cashboxDao.getAllActiveCashboxes(),
            operationsRepository.getAllProducts(),
            operationsRepository.getAllWarehouses(),
            operationsRepository.getAllSuppliers(),
            operationsRepository.getAllSales(),
            authRepository.getAllUsers(),
            auditDao.getAllActiveLogs(),
            backupRestoreManager.getAllBackups()
        ) { values ->
            val baseState = values[0] as ErpUiState
            val session = values[1] as? UserSession
            @Suppress("UNCHECKED_CAST")
            baseState.copy(
                session = session,
                units = values[2] as List<UnitEntity>,
                unitTypes = values[3] as List<UnitTypeEntity>,
                customers = values[4] as List<CustomerEntity>,
                reservations = values[5] as List<ReservationEntity>,
                accounts = values[6] as List<AccountEntity>,
                cashboxes = values[7] as List<CashboxEntity>,
                products = values[8] as List<ProductEntity>,
                warehouses = values[9] as List<WarehouseEntity>,
                suppliers = values[10] as List<SupplierEntity>,
                sales = values[11] as List<SalesInvoiceEntity>,
                users = values[12] as List<UserEntity>,
                auditLogs = values[13] as List<AuditLogEntity>,
                backups = values[14] as List<BackupMetadataEntity>
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ErpUiState()
        )
    }

    private suspend fun seedInitialHotelUnitsIfEmpty() {
        val existingUnits = database.openHelper.readableDatabase.let {
            val cursor = it.query("SELECT COUNT(*) FROM units")
            cursor.moveToFirst()
            cursor.getInt(0)
        }
        if (existingUnits == 0) {
            hotelRepository.createUnit("101", 1, 1, 1500000L, "غرفة فردية مطلة على الحديقة")
            hotelRepository.createUnit("102", 1, 2, 2500000L, "غرفة مزدوجة فاخرة")
            hotelRepository.createUnit("201", 2, 3, 5500000L, "جناح ملكي مع إطلالة بانورامية")
            hotelRepository.createCustomer("عبدالله مسعد اليافعي", "771234567", "0101010101", "يمني")
            hotelRepository.createCustomer("فهد عبدالعزيز الشمري", "778899112", "0202020202", "سعودي")
            operationsRepository.createProduct("مياه معدنية طبيعية 500 مل", 1, 20000L, 50000L, 200.0)
            operationsRepository.createProduct("عصير برتقال طبيعي طازج", 1, 50000L, 100000L, 50.0)
            operationsRepository.createProduct("شاي أحمر فاخر مع نعناع", 1, 15000L, 35000L, 100.0)
            operationsRepository.createSupplier("شركة الجزيرة للمرطبات والتموين", "770112233", "3001234567")
        }
    }

    fun selectDestination(destination: NavDestination) {
        _uiState.update { it.copy(selectedDestination = destination) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun setExitConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showExitConfirmDialog = show) }
    }

    // AUTH ACTIONS
    fun login(user: String, pass: String) = viewModelScope.launch {
        when (val res = authRepository.login(user, pass)) {
            is Resource.Success -> showSnackbar("أهلاً بك، تم تسجيل الدخول بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun logout() = viewModelScope.launch {
        authRepository.logout()
        showSnackbar("تم تسجيل الخروج بنجاح")
    }

    // HOTEL ACTIONS
    fun addUnit(unitNumber: String, floor: Int, typeId: Long, price: Long, desc: String) = viewModelScope.launch {
        when (val res = hotelRepository.createUnit(unitNumber, floor, typeId, price, desc)) {
            is Resource.Success -> showSnackbar("تم إضافة الوحدة $unitNumber بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun setUnitStatus(unitId: Long, status: String) = viewModelScope.launch {
        when (val res = hotelRepository.updateUnitStatus(unitId, status)) {
            is Resource.Success -> showSnackbar("تم تحديث حالة الوحدة إلى $status")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    // CUSTOMER ACTIONS
    fun addCustomer(name: String, phone: String, nationalId: String, nationality: String) = viewModelScope.launch {
        when (val res = hotelRepository.createCustomer(name, phone, nationalId, nationality)) {
            is Resource.Success -> showSnackbar("تم إضافة النزيل بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun deleteCustomer(id: Long) = viewModelScope.launch {
        when (val res = hotelRepository.deleteCustomerSafely(id)) {
            is Resource.Success -> showSnackbar("تم حذف أو أرشفة العميل بنجاح وأمان")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    // RESERVATIONS & CHECK-IN
    fun createReservation(customerId: Long, unitId: Long, checkIn: Long, checkOut: Long, total: Long) = viewModelScope.launch {
        when (val res = hotelRepository.createReservation(customerId, unitId, checkIn, checkOut, total)) {
            is Resource.Success -> showSnackbar("تم تأكيد الحجز بنجاح ومنع أي تضارب")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun checkInGuest(reservationId: Long?, customerId: Long, unitId: Long, checkOut: Long, rate: Long) = viewModelScope.launch {
        val op = uiState.value.session?.user?.fullName ?: "الموظف"
        when (val res = hotelRepository.performCheckIn(reservationId, customerId, unitId, checkOut, rate, op)) {
            is Resource.Success -> showSnackbar("تم تسجيل وصول النزيل وتسكين الغرفة بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun checkOutGuest(stayId: Long, cashboxId: Long, paymentMethod: String) = viewModelScope.launch {
        val op = uiState.value.session?.user?.fullName ?: "الموظف"
        when (val res = hotelRepository.performCheckOut(stayId, cashboxId, paymentMethod, op)) {
            is Resource.Success -> showSnackbar("تمت المغادرة وتسوية الفاتورة وإيداع الإيراد محاسبياً")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    // ACCOUNTING ACTIONS
    fun createReceipt(from: String, amountMinor: Long, cashboxId: Long, creditAccId: Long, notes: String) = viewModelScope.launch {
        val recNo = "REC-${System.currentTimeMillis() % 100000}"
        when (val res = accountingEngine.createReceiptVoucher(
            receiptNumber = recNo,
            customerId = 1L,
            receivedFrom = from,
            amountMinor = amountMinor,
            paymentMethodCode = "CASH",
            cashboxId = cashboxId,
            creditAccountId = creditAccId,
            referenceNo = null,
            notes = notes
        )) {
            is Resource.Success<*> -> showSnackbar("تم إصدار سند القبض وترحيل القيد المحاسبي المتوازن")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun createPayment(to: String, amountMinor: Long, cashboxId: Long, debitAccId: Long, notes: String) = viewModelScope.launch {
        val payNo = "PAY-${System.currentTimeMillis() % 100000}"
        when (val res = accountingEngine.createPaymentVoucher(
            paymentNumber = payNo,
            supplierId = null,
            paidTo = to,
            amountMinor = amountMinor,
            paymentMethodCode = "CASH",
            cashboxId = cashboxId,
            debitAccountId = debitAccId,
            referenceNo = null,
            notes = notes
        )) {
            is Resource.Success<*> -> showSnackbar("تم إصدار سند الصرف وترحيل القيد المحاسبي المتوازن")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun transferCash(fromId: Long, toId: Long, amountMinor: Long, notes: String) = viewModelScope.launch {
        when (val res = accountingEngine.transferBetweenCashboxes(fromId, toId, amountMinor, notes)) {
            is Resource.Success<*> -> showSnackbar("تم تحويل النقدية بين الصندوقين وترحيل القيد المالي")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun openShift(cashboxId: Long, openingBalanceMinor: Long) = viewModelScope.launch {
        val user = uiState.value.session?.user
        val name = user?.fullName ?: "الكاشير"
        val code = "SHF-${System.currentTimeMillis() % 100000}"
        when (val res = accountingEngine.openShift(code, cashboxId, name, openingBalanceMinor)) {
            is Resource.Success<*> -> showSnackbar("تم فتح الوردية بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun closeShift(shiftId: Long, actualCashCountMinor: Long, notes: String) = viewModelScope.launch {
        val name = uiState.value.session?.user?.fullName ?: "الكاشير"
        when (val res = accountingEngine.closeShift(shiftId, name, actualCashCountMinor, notes)) {
            is Resource.Success<*> -> showSnackbar("تم إغلاق الوردية والمطابقة المالية بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    // OPERATIONS & POS ACTIONS
    fun addProduct(name: String, catId: Long, buyPrice: Long, sellPrice: Long, qty: Double) = viewModelScope.launch {
        when (val res = operationsRepository.createProduct(name, catId, buyPrice, sellPrice, qty)) {
            is Resource.Success -> showSnackbar("تم إضافة الصنف للمستودع بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun addSupplier(name: String, phone: String, tax: String) = viewModelScope.launch {
        when (val res = operationsRepository.createSupplier(name, phone, tax)) {
            is Resource.Success -> showSnackbar("تم إضافة المورد بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun recordPurchase(supplierId: Long, warehouseId: Long, cashboxId: Long, productId: Long, qty: Double, price: Long, isCash: Boolean) = viewModelScope.launch {
        when (val res = operationsRepository.recordPurchaseInvoice(supplierId, warehouseId, cashboxId, productId, qty, price, isCash)) {
            is Resource.Success -> showSnackbar("تم تسجيل فاتورة الشراء وتحديث الأرصدة والمخزون بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun makePosSale(cashboxId: Long, warehouseId: Long, productId: Long, qty: Double, customer: String) = viewModelScope.launch {
        when (val res = operationsRepository.performPosSale(cashboxId, warehouseId, productId, qty, customer)) {
            is Resource.Success -> showSnackbar("تمت عملية البيع في نقطة البيع POS وقبض المبلغ محاسبياً")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    // SECURITY & USER ACTIONS
    fun createUser(user: String, name: String, pass: String, roleId: Long) = viewModelScope.launch {
        when (val res = authRepository.createUser(user, name, pass, roleId)) {
            is Resource.Success -> showSnackbar("تم إنشاء المستخدم بنجاح")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    fun toggleUser(userId: Long) = viewModelScope.launch {
        when (val res = authRepository.toggleUserActivation(userId)) {
            is Resource.Success -> showSnackbar("تم تعديل حالة تفعيل الحساب")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
    }

    // BACKUP & RESTORE ACTIONS (PHASE 11)
    fun createBackup(note: String = "نسخة احتياطية يدوية كاملة") = viewModelScope.launch {
        _uiState.update { it.copy(isBackupInProgress = true) }
        val operator = uiState.value.session?.user?.fullName ?: "ADMIN"
        when (val res = backupRestoreManager.createBackup(note, operator)) {
            is Resource.Success -> showSnackbar("تم إنشاء وحفظ النسخة الاحتياطية بنجاح: ${res.data.backupFileName}")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
        _uiState.update { it.copy(isBackupInProgress = false) }
    }

    fun restoreBackup(backupFileName: String) = viewModelScope.launch {
        _uiState.update { it.copy(isBackupInProgress = true) }
        val operator = uiState.value.session?.user?.fullName ?: "ADMIN"
        when (val res = backupRestoreManager.restoreBackup(backupFileName, operator, createAutoPreBackup = true)) {
            is Resource.Success -> showSnackbar("تمت استعادة البيانات بنجاح وفحص سلامة الهيكل المحاسبي والتشغيلي")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
        _uiState.update { it.copy(isBackupInProgress = false) }
    }

    fun exportBackup(context: Context, backupFileName: String) {
        val file = backupRestoreManager.getBackupFile(backupFileName)
        if (file != null && file.exists()) {
            PdfReportGenerator.sharePdf(context, file, "مشاركة وتصدير النسخة الاحتياطية")
        } else {
            showSnackbar("الملف غير موجود على مساحة التخزين")
        }
    }

    fun importBackup(sourceUri: Uri) = viewModelScope.launch {
        _uiState.update { it.copy(isBackupInProgress = true) }
        when (val res = backupRestoreManager.importBackupFromUri(sourceUri)) {
            is Resource.Success -> showSnackbar("تم استيراد النسخة الاحتياطية وفحص سلامتها بنجاح: ${res.data.name}")
            is Resource.Error -> showSnackbar(res.message)
            else -> {}
        }
        _uiState.update { it.copy(isBackupInProgress = false) }
    }

    // PDF & PRINTING ACTIONS (PHASE 16)
    fun printFinancialReport(context: Context) {
        val st = uiState.value
        val totalAssets = st.accounts.filter { it.accountGroupId == 1L }.sumOf { it.currentBalanceMinor }
        val totalLiabilities = st.accounts.filter { it.accountGroupId == 2L }.sumOf { it.currentBalanceMinor }
        val totalEquity = st.accounts.filter { it.accountGroupId == 3L }.sumOf { it.currentBalanceMinor }
        val totalRevenues = st.accounts.filter { it.accountGroupId == 4L }.sumOf { it.currentBalanceMinor }
        val totalExpenses = st.accounts.filter { it.accountGroupId == 5L }.sumOf { it.currentBalanceMinor }
        val netProfit = totalRevenues - totalExpenses

        val pdfFile = PdfReportGenerator.generateFinancialReportPdf(
            context = context,
            hotelName = st.hotelName,
            totalRevenues = totalRevenues,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            totalEquity = totalEquity
        )
        PdfReportGenerator.sharePdf(context, pdfFile, "طباعة ومشاركة التقرير المالي الشامل")
    }

    fun printInvoice(
        context: Context,
        invoiceNumber: String,
        customerName: String,
        unitNumber: String,
        stayNights: Int,
        totalAmountMinor: Long,
        paidAmountMinor: Long
    ) {
        val pdfFile = PdfReportGenerator.generateInvoicePdf(
            context = context,
            invoiceNumber = invoiceNumber,
            customerName = customerName,
            unitNumber = unitNumber,
            stayNights = stayNights,
            totalAmountMinor = totalAmountMinor,
            paidAmountMinor = paidAmountMinor,
            hotelName = uiState.value.hotelName
        )
        PdfReportGenerator.sharePdf(context, pdfFile, "طباعة ومشاركة فاتورة الإقامة")
    }

    fun printReceipt(
        context: Context,
        receiptNumber: String,
        receivedFrom: String,
        amountMinor: Long,
        notes: String
    ) {
        val pdfFile = PdfReportGenerator.generateReceiptPdf(
            context = context,
            receiptNumber = receiptNumber,
            receivedFrom = receivedFrom,
            amountMinor = amountMinor,
            notes = notes,
            hotelName = uiState.value.hotelName
        )
        PdfReportGenerator.sharePdf(context, pdfFile, "طباعة ومشاركة سند القبض")
    }

    fun printThermalReceipt(
        context: Context,
        receiptNumber: String,
        customerName: String,
        description: String,
        amountMinor: Long
    ) {
        val operator = uiState.value.session?.user?.fullName ?: "المحاسب المعتمد"
        val pdfFile = PdfReportGenerator.generateThermalReceiptPdf(
            context = context,
            receiptNumber = receiptNumber,
            customerName = customerName,
            description = description,
            amountMinor = amountMinor,
            hotelName = uiState.value.hotelName,
            operatorName = operator
        )
        PdfReportGenerator.sharePdf(context, pdfFile, "طباعة إيصال طابعة حرارية 80mm")
    }
}
