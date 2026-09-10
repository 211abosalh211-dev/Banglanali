package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.accounting.AccountEntity
import com.example.data.local.entity.accounting.AccountGroupEntity
import com.example.data.local.entity.accounting.FiscalPeriodEntity
import com.example.data.local.entity.accounting.JournalEntryEntity
import com.example.data.local.entity.accounting.JournalEntryLineEntity
import com.example.data.local.relation.JournalEntryWithLines
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {

    // Account Groups
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccountGroup(group: AccountGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccountGroups(groups: List<AccountGroupEntity>)

    @Query("SELECT * FROM account_groups ORDER BY groupCode ASC")
    fun getAllAccountGroups(): Flow<List<AccountGroupEntity>>

    // Chart of Accounts
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY accountCode ASC")
    fun getAllActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE accountCode = :code")
    suspend fun getAccountByCode(code: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM journal_entry_lines WHERE accountId = :accountId")
    suspend fun countJournalLinesForAccount(accountId: Long): Int

    @Query("UPDATE accounts SET currentBalanceMinor = currentBalanceMinor + :diffMinor WHERE id = :accountId")
    suspend fun adjustAccountBalance(accountId: Long, diffMinor: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun hardDeleteAccount(id: Long)

    @Query("UPDATE accounts SET isDeleted = 1, deletedAt = :timestamp, isActive = 0 WHERE id = :id")
    suspend fun softDeleteAccount(id: Long, timestamp: Long = System.currentTimeMillis())

    // Fiscal Periods
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFiscalPeriod(period: FiscalPeriodEntity): Long

    @Query("SELECT * FROM fiscal_periods ORDER BY startDate DESC")
    fun getAllFiscalPeriods(): Flow<List<FiscalPeriodEntity>>

    // Journal Entries
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalLines(lines: List<JournalEntryLineEntity>)

    @Update
    suspend fun updateJournalEntry(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries ORDER BY date DESC, id DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getJournalEntryById(id: Long): JournalEntryEntity?

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getJournalEntryWithLines(id: Long): JournalEntryWithLines?

    @Query("SELECT * FROM journal_entry_lines WHERE entryId = :entryId")
    suspend fun getLinesForEntry(entryId: Long): List<JournalEntryLineEntity>
}
