package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.result.Resource
import com.example.data.local.HotelDatabase
import com.example.data.local.entity.accounting.AccountEntity
import com.example.data.local.entity.accounting.JournalEntryEntity
import com.example.data.local.entity.accounting.JournalEntryLineEntity
import com.example.data.local.relation.JournalEntryWithLines
import kotlinx.coroutines.flow.Flow

class AccountingRepository(private val database: HotelDatabase) {

    private val accountingDao = database.accountingDao()
    private val auditDao = database.auditLogDao()

    fun getAllAccounts(): Flow<List<AccountEntity>> = accountingDao.getAllActiveAccounts()

    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>> = accountingDao.getAllJournalEntries()

    suspend fun getJournalEntryWithLines(id: Long): JournalEntryWithLines? =
        accountingDao.getJournalEntryWithLines(id)

    /**
     * Strict Accounting Integrity:
     * - Every journal entry MUST satisfy: Total Debit == Total Credit.
     * - Executed inside an atomic Room transaction.
     * - Updates account balances and creates audit trail.
     */
    suspend fun postJournalEntry(
        entryNumber: String,
        description: String,
        date: Long = System.currentTimeMillis(),
        referenceType: String? = null,
        referenceId: String? = null,
        createdBy: String = "SYSTEM",
        lines: List<JournalEntryLineEntity>
    ): Resource<Long> {
        if (lines.isEmpty()) {
            return Resource.Error("لا يمكن حفظ قيد محاسبي فارغ بدون بنود")
        }

        // Calculate debit and credit totals
        var totalDebit = 0L
        var totalCredit = 0L

        for (line in lines) {
            if (line.debit < 0 || line.credit < 0) {
                return Resource.Error("المبالغ المحاسبية يجب أن تكون أرقاماً موجبة (المدين أو الدائن)")
            }
            if (line.debit > 0 && line.credit > 0) {
                return Resource.Error("لا يمكن لبند القيد أن يحتوي على مدين ودائن معاً في آن واحد")
            }
            if (line.debit == 0L && line.credit == 0L) {
                return Resource.Error("يجب تحديد مبلغ للمدين أو الدائن في كل بند")
            }
            totalDebit += line.debit
            totalCredit += line.credit
        }

        // Critical Check: Total Debit == Total Credit
        if (totalDebit != totalCredit) {
            val diff = totalDebit - totalCredit
            return Resource.Error(
                "خطأ في توازن القيد المحاسبي: إجمالي المدين ($totalDebit) لا يساوي إجمالي الدائن ($totalCredit). الفارق: $diff"
            )
        }

        return try {
            val entryId = database.withTransaction {
                val entryEntity = JournalEntryEntity(
                    entryNumber = entryNumber,
                    date = date,
                    description = description,
                    referenceType = referenceType,
                    referenceId = referenceId,
                    status = "POSTED",
                    totalDebitMinor = totalDebit,
                    totalCreditMinor = totalCredit,
                    createdBy = createdBy
                )

                val id = accountingDao.insertJournalEntry(entryEntity)

                // Assign entryId to all lines
                val assignedLines = lines.map { it.copy(entryId = id) }
                accountingDao.insertJournalLines(assignedLines)

                // Update ledger account balances
                for (line in assignedLines) {
                    val account = accountingDao.getAccountById(line.accountId)
                        ?: throw IllegalStateException("الحساب المحاسبي رقم #${line.accountId} غير موجود")

                    // Asset & Expense increase with Debit (+), decrease with Credit (-)
                    // Liability, Equity & Revenue increase with Credit (+), decrease with Debit (-)
                    val balanceDiff = when (account.accountType) {
                        "ASSET", "EXPENSE" -> line.debit - line.credit
                        else -> line.credit - line.debit
                    }
                    accountingDao.adjustAccountBalance(account.id, balanceDiff)
                }

                id
            }

            Resource.Success(entryId)
        } catch (e: Exception) {
            Resource.Error("فشلت عملية ترحيل القيد المحاسبي: ${e.localizedMessage}")
        }
    }

    /**
     * Foreign Key & Historical Protection:
     * Prevent deleting accounts with existing journal lines.
     */
    suspend fun deleteAccountSafe(accountId: Long): Resource<Boolean> {
        val linesCount = accountingDao.countJournalLinesForAccount(accountId)
        return if (linesCount > 0) {
            // Refuse hard delete to preserve financial history! Perform soft delete instead.
            accountingDao.softDeleteAccount(accountId)
            Resource.Error("لا يمكن الحذف النهائي للحساب لوجود $linesCount قيود محاسبية مرتبطة به. تم تعطيل الحساب (Soft Delete) لحماية التاريخ المالي.")
        } else {
            accountingDao.hardDeleteAccount(accountId)
            Resource.Success(true)
        }
    }
}
