package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.expense.ExpenseCategoryEntity
import com.example.data.local.entity.expense.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: ExpenseCategoryEntity): Long

    @Query("SELECT * FROM expense_categories WHERE isActive = 1")
    fun getAllCategories(): Flow<List<ExpenseCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY expenseDate DESC")
    fun getAllActiveExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("UPDATE expenses SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteExpense(id: Long, timestamp: Long = System.currentTimeMillis())
}
