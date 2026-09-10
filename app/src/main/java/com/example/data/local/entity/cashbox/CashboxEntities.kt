package com.example.data.local.entity.cashbox

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.local.entity.accounting.AccountEntity

@Entity(
    tableName = "cashboxes",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["glAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["cashboxCode"], unique = true),
        Index(value = ["glAccountId"])
    ]
)
data class CashboxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cashboxCode: String, // e.g. "CB-MAIN", "CB-RECEPTION-01"
    val nameAr: String,
    val glAccountId: Long, // Linked General Ledger Account
    val currencyCode: String = "YER",
    val currentBalanceMinor: Long = 0L,
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cashbox_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CashboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["cashboxId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transactionNumber"], unique = true),
        Index(value = ["cashboxId"]),
        Index(value = ["transactionDate"]),
        Index(value = ["referenceType", "referenceId"])
    ]
)
data class CashboxTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionNumber: String, // e.g. "CBT-000001"
    val cashboxId: Long,
    val transactionType: String, // RECEIPT, PAYMENT, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT
    val amountMinor: Long,
    val direction: String, // IN (DEBIT to cash), OUT (CREDIT from cash)
    val balanceAfterMinor: Long,
    val referenceType: String? = null, // INVOICE, RECEIPT, EXPENSE, SHIFT_OPENING
    val referenceId: String? = null,
    val description: String,
    val operatorName: String,
    val transactionDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cashbox_transfers",
    foreignKeys = [
        ForeignKey(
            entity = CashboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromCashboxId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CashboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["toCashboxId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transferNumber"], unique = true),
        Index(value = ["fromCashboxId"]),
        Index(value = ["toCashboxId"])
    ]
)
data class CashboxTransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transferNumber: String, // e.g. "TRF-000001"
    val fromCashboxId: Long,
    val toCashboxId: Long,
    val amountMinor: Long,
    val status: String = "COMPLETED", // PENDING, COMPLETED, CANCELLED
    val notes: String? = null,
    val transferredBy: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shifts",
    foreignKeys = [
        ForeignKey(
            entity = CashboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["cashboxId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shiftNumber"], unique = true),
        Index(value = ["cashboxId"]),
        Index(value = ["status"])
    ]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shiftNumber: String, // e.g. "SHF-000001"
    val cashboxId: Long,
    val operatorName: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val openingBalanceMinor: Long,
    val closingBalanceMinor: Long? = null,
    val systemExpectedBalanceMinor: Long? = null,
    val differenceMinor: Long? = null,
    val status: String = "OPEN" // OPEN, CLOSED, RECONCILED
)

@Entity(
    tableName = "shift_transactions",
    primaryKeys = ["shiftId", "transactionId"],
    foreignKeys = [
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CashboxTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shiftId"]),
        Index(value = ["transactionId"])
    ]
)
data class ShiftTransactionEntity(
    val shiftId: Long,
    val transactionId: Long
)

@Entity(
    tableName = "shift_closures",
    foreignKeys = [
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shiftId"], unique = true)]
)
data class ShiftClosureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shiftId: Long,
    val actualCashCountMinor: Long,
    val systemCalculatedMinor: Long,
    val varianceMinor: Long, // Actual - System (negative means shortage)
    val closureNotes: String? = null,
    val closedBy: String,
    val closureTimestamp: Long = System.currentTimeMillis()
)
