package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.cashbox.CashboxEntity
import com.example.data.local.entity.cashbox.CashboxTransactionEntity
import com.example.data.local.entity.cashbox.CashboxTransferEntity
import com.example.data.local.entity.cashbox.ShiftClosureEntity
import com.example.data.local.entity.cashbox.ShiftEntity
import com.example.data.local.entity.cashbox.ShiftTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashboxDao {

    // Cashboxes
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCashbox(cashbox: CashboxEntity): Long

    @Update
    suspend fun updateCashbox(cashbox: CashboxEntity)

    @Query("SELECT * FROM cashboxes WHERE isDeleted = 0")
    fun getAllActiveCashboxes(): Flow<List<CashboxEntity>>

    @Query("SELECT * FROM cashboxes WHERE id = :id")
    suspend fun getCashboxById(id: Long): CashboxEntity?

    @Query("SELECT * FROM cashboxes WHERE cashboxCode = :code")
    suspend fun getCashboxByCode(code: String): CashboxEntity?

    @Query("UPDATE cashboxes SET currentBalanceMinor = currentBalanceMinor + :diffMinor WHERE id = :cashboxId")
    suspend fun updateBalance(cashboxId: Long, diffMinor: Long)

    @Query("SELECT COUNT(*) FROM cashbox_transactions WHERE cashboxId = :cashboxId")
    suspend fun countTransactionsForCashbox(cashboxId: Long): Int

    @Query("UPDATE cashboxes SET isDeleted = 1, deletedAt = :timestamp, isActive = 0 WHERE id = :id")
    suspend fun softDeleteCashbox(id: Long, timestamp: Long = System.currentTimeMillis())

    // Cashbox Transactions
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: CashboxTransactionEntity): Long

    @Query("SELECT * FROM cashbox_transactions WHERE cashboxId = :cashboxId ORDER BY transactionDate DESC")
    fun getTransactionsForCashbox(cashboxId: Long): Flow<List<CashboxTransactionEntity>>

    @Query("SELECT * FROM cashbox_transactions ORDER BY transactionDate DESC")
    fun getAllTransactions(): Flow<List<CashboxTransactionEntity>>

    // Transfers
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransfer(transfer: CashboxTransferEntity): Long

    @Query("SELECT * FROM cashbox_transfers ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<CashboxTransferEntity>>

    // Shifts
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' AND cashboxId = :cashboxId LIMIT 1")
    suspend fun getOpenShiftForCashbox(cashboxId: Long): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun linkTransactionToShift(shiftTx: ShiftTransactionEntity)

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getShiftById(id: Long): ShiftEntity?

    @Query("UPDATE shifts SET status = :status, endTime = :endTime, closingBalanceMinor = :closingBalance, differenceMinor = :difference WHERE id = :shiftId")
    suspend fun updateShiftStatus(shiftId: Long, status: String, endTime: Long, closingBalance: Long, difference: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShiftClosure(closure: ShiftClosureEntity): Long
}
