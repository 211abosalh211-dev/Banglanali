package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.currency.Money
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.SystemConfigEntity
import com.example.data.repository.FoundationRepository
import com.example.ui.navigation.NavDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoundationUiState(
    val selectedDestination: NavDestination = NavDestination.DASHBOARD,
    val hotelName: String = "فندق البرج الذهبي الملكي",
    val defaultCurrency: String = Money.DEFAULT_CURRENCY,
    val currencyNameAr: String = "ريال يمني",
    val isRtl: Boolean = true,
    val isSystemReady: Boolean = true,
    val systemConfigs: List<SystemConfigEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val auditCount: Int = 0,
    val accountsCount: Int = 0,
    val cashboxesCount: Int = 0,
    val unitsCount: Int = 0,
    val databaseSchemaVersion: Int = 2,
    val showExitConfirmDialog: Boolean = false,
    val snackbarMessage: String? = null,
    val precisionTestResult: Money = Money.ZERO,
    val isCalculating: Boolean = false
)

class FoundationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoundationRepository

    private val _uiState = MutableStateFlow(FoundationUiState())
    val uiState: StateFlow<FoundationUiState>

    init {
        val database = HotelDatabase.getInstance(application)
        repository = FoundationRepository(database)

        // Seed or ensure initial configuration
        viewModelScope.launch {
            repository.ensureDefaultConfigsInitialized()
            // Run initial precision test
            runPrecisionVerification()
        }

        // Combine flows for unified reactive UI state
        val combinedFlow = combine(
            _uiState,
            repository.getAllConfigs(),
            repository.getAllAuditLogs(),
            repository.getAuditLogCount(),
            repository.getActiveAccounts(),
            repository.getActiveCashboxes(),
            repository.getActiveUnits()
        ) { args: Array<Any> ->
            @Suppress("UNCHECKED_CAST")
            val state = args[0] as FoundationUiState
            @Suppress("UNCHECKED_CAST")
            val configs = args[1] as List<SystemConfigEntity>
            @Suppress("UNCHECKED_CAST")
            val logs = args[2] as List<AuditLogEntity>
            val count = args[3] as Int
            @Suppress("UNCHECKED_CAST")
            val accounts = args[4] as List<com.example.data.local.entity.accounting.AccountEntity>
            @Suppress("UNCHECKED_CAST")
            val cashboxes = args[5] as List<com.example.data.local.entity.cashbox.CashboxEntity>
            @Suppress("UNCHECKED_CAST")
            val units = args[6] as List<com.example.data.local.entity.hotel.UnitEntity>

            val hotelConfig = configs.find { it.configKey == "hotel_name" }?.configValue ?: state.hotelName
            val currencyConfig = configs.find { it.configKey == "currency_code" }?.configValue ?: state.defaultCurrency
            val currencyArConfig = configs.find { it.configKey == "currency_name" }?.configValue ?: state.currencyNameAr

            state.copy(
                hotelName = hotelConfig,
                defaultCurrency = currencyConfig,
                currencyNameAr = currencyArConfig,
                systemConfigs = configs,
                auditLogs = logs,
                auditCount = count,
                accountsCount = accounts.size,
                cashboxesCount = cashboxes.size,
                unitsCount = units.size,
                isSystemReady = true
            )
        }

        uiState = combinedFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _uiState.value
        )
    }

    fun selectDestination(destination: NavDestination) {
        _uiState.update { it.copy(selectedDestination = destination) }
    }

    fun toggleLayoutDirection() {
        _uiState.update { it.copy(isRtl = !it.isRtl) }
    }

    fun updateHotelConfig(key: String, value: String, description: String = "") {
        viewModelScope.launch {
            when (val result = repository.updateConfig(key, value, description)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(snackbarMessage = "تم تحديث إعداد النظام بنجاح ($key)") }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(snackbarMessage = "خطأ: ${result.message}") }
                }
                else -> Unit
            }
        }
    }

    fun addManualAuditVerification(details: String = "فحص ميداني وتدقيق للنظام") {
        viewModelScope.launch {
            when (val result = repository.recordAuditAction(
                actionType = "AUDIT_VERIFY",
                entityType = "GOVERNANCE",
                entityId = "GOV-${System.currentTimeMillis() % 10000}",
                details = details,
                previousVal = "PENDING",
                newVal = "VERIFIED"
            )) {
                is Resource.Success -> {
                    _uiState.update { it.copy(snackbarMessage = "تم تسجيل قيد التدقيق بنجاح في قاعدة البيانات #ID:${result.data}") }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(snackbarMessage = "خطأ في تسجيل القيد: ${result.message}") }
                }
                else -> Unit
            }
        }
    }

    fun runPrecisionVerification() {
        // Strict financial precision simulation:
        // Room daily rate: 35,000.75 YER * 3 nights = 105,002.25 YER
        // Minus early check-in discount of 5,000.25 YER = 100,002.00 YER
        // Add exact tax of 5% (5,000.10 YER) = 105,002.10 YER
        // Zero floating point error guaranteed via Money class!
        val rate = Money.fromString("35000.75", Money.DEFAULT_CURRENCY)
        val nights = 3L
        val subtotal = rate * nights
        val discount = Money.fromString("5000.25", Money.DEFAULT_CURRENCY)
        val afterDiscount = subtotal - discount
        val tax = Money.fromString("5000.10", Money.DEFAULT_CURRENCY)
        val grandTotal = afterDiscount + tax

        _uiState.update { it.copy(precisionTestResult = grandTotal) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showExitConfirmation() {
        _uiState.update { it.copy(showExitConfirmDialog = true) }
    }

    fun dismissExitConfirmation() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
    }
}
