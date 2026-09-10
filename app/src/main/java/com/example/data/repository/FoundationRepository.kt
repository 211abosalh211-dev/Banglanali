package com.example.data.repository

import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.SystemConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class FoundationRepository(private val database: HotelDatabase) {

    private val configDao = database.systemConfigDao()
    private val auditDao = database.auditLogDao()

    fun getAllConfigs(): Flow<List<SystemConfigEntity>> {
        return configDao.getAllConfigs().flowOn(Dispatchers.IO)
    }

    fun getAllAuditLogs(): Flow<List<AuditLogEntity>> {
        return auditDao.getAllActiveLogs().flowOn(Dispatchers.IO)
    }

    fun getAuditLogCount(): Flow<Int> {
        return auditDao.countActiveLogs().flowOn(Dispatchers.IO)
    }

    fun getActiveAccounts(): Flow<List<com.example.data.local.entity.accounting.AccountEntity>> {
        return database.accountingDao().getAllActiveAccounts().flowOn(Dispatchers.IO)
    }

    fun getActiveCashboxes(): Flow<List<com.example.data.local.entity.cashbox.CashboxEntity>> {
        return database.cashboxDao().getAllActiveCashboxes().flowOn(Dispatchers.IO)
    }

    fun getActiveUnits(): Flow<List<com.example.data.local.entity.hotel.UnitEntity>> {
        return database.hotelUnitDao().getAllUnits().flowOn(Dispatchers.IO)
    }

    suspend fun updateConfig(key: String, value: String, description: String = ""): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val existing = configDao.getValue(key)
                val entity = SystemConfigEntity(
                    configKey = key,
                    configValue = value,
                    description = description,
                    updatedAt = System.currentTimeMillis()
                )
                configDao.insertOrUpdate(entity)

                // Audit log this change
                auditDao.insertLog(
                    AuditLogEntity(
                        operatorName = "مدير النظام",
                        operatorRole = "ADMIN",
                        actionType = "UPDATE",
                        entityType = "CONFIG",
                        entityId = key,
                        details = "تعديل إعداد النظام: $key",
                        previousValue = existing,
                        newValue = value
                    )
                )
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "فشل حفظ إعداد النظام", e)
            }
        }
    }

    suspend fun recordAuditAction(
        actionType: String,
        entityType: String,
        entityId: String,
        details: String,
        previousVal: String? = null,
        newVal: String? = null
    ): Resource<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val log = AuditLogEntity(
                    operatorName = "مشرف الوردية",
                    operatorRole = "SUPERVISOR",
                    actionType = actionType,
                    entityType = entityType,
                    entityId = entityId,
                    details = details,
                    previousValue = previousVal,
                    newValue = newVal
                )
                val id = auditDao.insertLog(log)
                Resource.Success(id)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "فشل تسجيل قيد التدقيق", e)
            }
        }
    }

    suspend fun ensureDefaultConfigsInitialized(): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (configDao.countConfigs() == 0) {
                    database.seedDefaultConfigurations()
                }
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "فشل تهيئة الإعدادات الافتراضية", e)
            }
        }
    }
}
