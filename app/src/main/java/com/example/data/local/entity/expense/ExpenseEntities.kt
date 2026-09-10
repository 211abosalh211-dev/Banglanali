package com.example.data.local.entity.expense

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.local.entity.accounting.AccountEntity
import com.example.data.local.entity.cashbox.CashboxEntity

@Entity(
    tableName = "expense_categories",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["glAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryCode"], unique = true),
        Index(value = ["glAccountId"])
    ]
)
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryCode: String, // e.g. "EXP-UTIL", "EXP-MAINT", "EXP-SALARY"
    val nameAr: String,
    val glAccountId: Long,
    val isActive: Boolean = true
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CashboxEntity::class,
            parentColumns = ["id"],
            childColumns = ["cashboxId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["expenseNumber"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["cashboxId"]),
        Index(value = ["expenseDate"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expenseNumber: String, // e.g. "EXP-000001"
    val categoryId: Long,
    val cashboxId: Long,
    val amountMinor: Long,
    val expenseDate: Long = System.currentTimeMillis(),
    val paidTo: String,
    val description: String,
    val referenceInvoiceNo: String? = null,
    val paymentMethod: String = "CASH",
    val approvedBy: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
