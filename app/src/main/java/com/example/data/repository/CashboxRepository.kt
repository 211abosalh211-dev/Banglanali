package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.cashbox.CashboxEntity
import com.example.data.local.entity.cashbox.CashboxTransactionEntity
import com.example.data.local.entity.cashbox.CashboxTransferEntity
import com.example.data.local.entity.cashbox.ShiftClosureEntity
import com.example.data.local.entity.cashbox.ShiftEntity
import com.example.data.local.entity.cashbox.ShiftTransactionEntity
import kotlinx.coroutines.flow.Flow

class CashboxRepository(private val database: HotelDatabase) {

    private val cashboxDao = database.cashboxDao()

    fun getAllActiveCashboxes(): Flow<List<CashboxEntity>> = cashboxDao.getAllActiveCashboxes()

    fun getAllTransactions(): Flow<List<CashboxTransactionEntity>> = cashboxDao.getAllTransactions()

    suspend fun getCashboxById(id: Long): CashboxEntity? = cashboxDao.getCashboxById(id)

    /**
     * Records a cashbox transaction atomically and updates balance.
     */
    suspend fun recordTransaction(
        cashboxId: Long,
        transactionNumber: String,
        type: String,
        amountMinor: Long,
        direction: String, // IN (DEBIT), OUT (CREDIT)
        referenceType: String? = null,
        referenceId: String? = null,
        description: String,
        operatorName: String
    ): Resource<Long> {
        if (amountMinor <= 0L) {
            return Resource.Error("مبلغ الحركة يجب أن يكون أكبر من الصفر")
        }

        return try {
            val txId = database.withTransaction {
                val box = cashboxDao.getCashboxById(cashboxId)
                    ?: throw IllegalStateException("الصندوق غير موجود")

                val balanceDelta = if (direction == "IN") amountMinor else -amountMinor
                val newBalance = box.currentBalanceMinor + balanceDelta

                val tx = CashboxTransactionEntity(
                    transactionNumber = transactionNumber,
                    cashboxId = cashboxId,
                    transactionType = type,
                    amountMinor = amountMinor,
                    direction = direction,
                    balanceAfterMinor = newBalance,
                    referenceType = referenceType,
                    referenceId = referenceId,
                    description = description,
                    operatorName = operatorName
                )

                val id = cashboxDao.insertTransaction(tx)
                cashboxDao.updateBalance(cashboxId, balanceDelta)

                // If an open shift exists for this cashbox, link it
                val openShift = cashboxDao.getOpenShiftForCashbox(cashboxId)
                if (openShift != null) {
                    cashboxDao.linkTransactionToShift(
                        ShiftTransactionEntity(
                            shiftId = openShift.id,
                            transactionId = id
                        )
                    )
                }

                id
            }
            Resource.Success(txId)
        } catch (e: Exception) {
            Resource.Error("فشل في تسجيل حركة الصندوق: ${e.localizedMessage}")
        }
    }

    /**
     * Transfer between cashboxes atomically.
     */
    suspend fun transferBetweenCashboxes(
        transferNumber: String,
        fromCashboxId: Long,
        toCashboxId: Long,
        amountMinor: Long,
        operatorName: String,
        notes: String? = null
    ): Resource<Long> {
        if (fromCashboxId == toCashboxId) {
            return Resource.Error("لا يمكن التحويل لنفس الصندوق")
        }
        if (amountMinor <= 0L) {
            return Resource.Error("المبلغ المحول يجب أن يكون أكبر من الصفر")
        }

        return try {
            val transferId = database.withTransaction {
                val fromBox = cashboxDao.getCashboxById(fromCashboxId)
                    ?: throw IllegalStateException("صندوق المصدر غير موجود")
                val toBox = cashboxDao.getCashboxById(toCashboxId)
                    ?: throw IllegalStateException("صندوق المستلم غير موجود")

                if (fromBox.currentBalanceMinor < amountMinor) {
                    throw IllegalStateException("رصيد صندوق المصدر لا يكفي لإجراء هذا التحويل")
                }

                val transfer = CashboxTransferEntity(
                    transferNumber = transferNumber,
                    fromCashboxId = fromCashboxId,
                    toCashboxId = toCashboxId,
                    amountMinor = amountMinor,
                    status = "COMPLETED",
                    notes = notes,
                    transferredBy = operatorName
                )
                val id = cashboxDao.insertTransfer(transfer)

                // Record outflow in fromBox
                cashboxDao.insertTransaction(
                    CashboxTransactionEntity(
                        transactionNumber = "TX-OUT-$id",
                        cashboxId = fromCashboxId,
                        transactionType = "TRANSFER_OUT",
                        amountMinor = amountMinor,
                        direction = "OUT",
                        balanceAfterMinor = fromBox.currentBalanceMinor - amountMinor,
                        referenceType = "CASHBOX_TRANSFER",
                        referenceId = transferNumber,
                        description = "تحويل إلى ${toBox.nameAr}",
                        operatorName = operatorName
                    )
                )
                cashboxDao.updateBalance(fromCashboxId, -amountMinor)

                // Record inflow in toBox
                cashboxDao.insertTransaction(
                    CashboxTransactionEntity(
                        transactionNumber = "TX-IN-$id",
                        cashboxId = toCashboxId,
                        transactionType = "TRANSFER_IN",
                        amountMinor = amountMinor,
                        direction = "IN",
                        balanceAfterMinor = toBox.currentBalanceMinor + amountMinor,
                        referenceType = "CASHBOX_TRANSFER",
                        referenceId = transferNumber,
                        description = "تحويل وارد من ${fromBox.nameAr}",
                        operatorName = operatorName
                    )
                )
                cashboxDao.updateBalance(toCashboxId, amountMinor)

                id
            }
            Resource.Success(transferId)
        } catch (e: Exception) {
            Resource.Error("فشلت عملية التحويل بين الصناديق: ${e.localizedMessage}")
        }
    }
}
