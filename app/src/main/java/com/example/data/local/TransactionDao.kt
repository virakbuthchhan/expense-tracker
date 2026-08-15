package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the 'transactions' table to provide offline persistent storage.
 */
@Dao
interface TransactionDao {

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

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategoryId(categoryId: Int): Flow<List<TransactionEntity>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
