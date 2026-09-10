package com.example.data.local.entity.accounting

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "account_groups",
    indices = [Index(value = ["groupCode"], unique = true)]
)
data class AccountGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupCode: String, // 1000 = ASSETS, 2000 = LIABILITIES, 3000 = EQUITY, 4000 = REVENUES, 5000 = EXPENSES
    val groupNameAr: String,
    val groupNameEn: String,
    val normalBalance: String // DEBIT, CREDIT
)

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = AccountGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountGroupId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountCode"], unique = true),
        Index(value = ["accountGroupId"]),
        Index(value = ["parentAccountId"]),
        Index(value = ["isDeleted"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountCode: String, // e.g. "10101", "10201", "40101"
    val accountNameAr: String,
    val accountNameEn: String? = null,
    val accountGroupId: Long,
    val parentAccountId: Long? = null,
    val accountType: String, // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    val isHeader: Boolean = false, // True for group/parent accounts that don't take direct entries
    val currentBalanceMinor: Long = 0L,
    val currencyCode: String = "YER",
    val isSystemAccount: Boolean = false,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fiscal_periods",
    indices = [
        Index(value = ["periodCode"], unique = true),
        Index(value = ["startDate", "endDate"])
    ]
)
data class FiscalPeriodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val periodCode: String, // e.g. "2026-01", "FY2026"
    val periodNameAr: String,
    val startDate: Long,
    val endDate: Long,
    val isClosed: Boolean = false,
    val closedAt: Long? = null,
    val closedBy: String? = null
)

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["entryNumber"], unique = true),
        Index(value = ["date"]),
        Index(value = ["status"]),
        Index(value = ["referenceType", "referenceId"])
    ]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryNumber: String, // e.g. "JV-000001"
    val date: Long, // timestamp
    val description: String,
    val referenceType: String? = null, // INVOICE, PAYMENT, RECEIPT, EXPENSE, ADJUSTMENT
    val referenceId: String? = null, // Document number or ID
    val status: String = "POSTED", // DRAFT, POSTED, REVERSED
    val totalDebitMinor: Long,
    val totalCreditMinor: Long,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_entry_lines",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["accountId"])
    ]
)
data class JournalEntryLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
    val accountId: Long,
    val debit: Long, // debit in minor units
    val credit: Long, // credit in minor units
    val description: String
)
