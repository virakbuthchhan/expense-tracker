package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class TransactionWithCategory(
    val id: Int,
    val amount: Double,
    val type: String,
    val categoryId: Int,
    val note: String,
    val date: Long,
    val createdAt: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String
)

data class CategorySpend(
    val categoryId: Int,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val totalAmount: Double,
    val transactionCount: Int
)

@Dao
interface ExpenseDao {

    // --- Transactions ---
    @Query("""
        SELECT t.id, t.amount, t.type, t.categoryId, t.note, t.date, t.createdAt,
               c.name AS categoryName, c.icon AS categoryIcon, c.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        ORDER BY t.date DESC, t.id DESC
    """)
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Query("""
        SELECT t.id, t.amount, t.type, t.categoryId, t.note, t.date, t.createdAt,
               c.name AS categoryName, c.icon AS categoryIcon, c.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.date >= :startDate AND t.date <= :endDate
        ORDER BY t.date DESC, t.id DESC
    """)
    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>

    @Query("""
        SELECT t.id, t.amount, t.type, t.categoryId, t.note, t.date, t.createdAt,
               c.name AS categoryName, c.icon AS categoryIcon, c.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.id = :id
    """)
    suspend fun getTransactionById(id: Int): TransactionWithCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // --- Budgets ---
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Int)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND month = :month")
    suspend fun deleteBudgetForCategory(categoryId: Int, month: String)

    // --- Analytics Aggregations ---
    @Query("""
        SELECT c.id AS categoryId, c.name AS categoryName, c.icon AS categoryIcon, c.colorHex AS categoryColorHex,
               SUM(t.amount) AS totalAmount, COUNT(t.id) AS transactionCount
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'expense' AND t.date >= :startDate AND t.date <= :endDate
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun getCategorySpendingInRange(startDate: Long, endDate: Long): Flow<List<CategorySpend>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
