package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the 'budgets' table to provide offline persistent storage.
 */
@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE month = :month")
    fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Int): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month LIMIT 1")
    suspend fun getBudgetForCategoryAndMonth(categoryId: Int, month: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Int)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND month = :month")
    suspend fun deleteBudgetForCategory(categoryId: Int, month: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
