package com.example

import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategorySpend
import com.example.data.local.ExpenseDao
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionWithCategory
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeExpenseDao : ExpenseDao {
  override fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>> = flowOf(emptyList())
  override fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>> = flowOf(emptyList())
  override suspend fun getTransactionById(id: Int): TransactionWithCategory? = null
  override suspend fun insertTransaction(transaction: TransactionEntity): Long = 1L
  override suspend fun updateTransaction(transaction: TransactionEntity) {}
  override suspend fun deleteTransaction(transaction: TransactionEntity) {}
  override suspend fun deleteTransactionById(id: Int) {}
  override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
  override suspend fun getCategoryById(id: Int): CategoryEntity? = null
  override suspend fun getCategoryCount(): Int = 0
  override suspend fun insertCategory(category: CategoryEntity): Long = 1L
  override suspend fun insertCategories(categories: List<CategoryEntity>) {}
  override suspend fun updateCategory(category: CategoryEntity) {}
  override suspend fun deleteCategory(category: CategoryEntity) {}
  override fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>> = flowOf(emptyList())
  override fun getAllBudgets(): Flow<List<BudgetEntity>> = flowOf(emptyList())
  override suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long = 1L
  override suspend fun deleteBudgetById(id: Int) {}
  override suspend fun deleteBudgetForCategory(categoryId: Int, month: String) {}
  override fun getCategorySpendingInRange(startDate: Long, endDate: Long): Flow<List<CategorySpend>> = flowOf(emptyList())
  override suspend fun deleteAllTransactions() {}
  override suspend fun deleteAllBudgets() {}
}

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCsvExportFormat() {
    val dao = FakeExpenseDao()
    val repo = ExpenseRepository(dao)
    val sampleTransactions = listOf(
      TransactionWithCategory(
        id = 1,
        amount = 45.50,
        type = "expense",
        categoryId = 1,
        note = "Lunch with team",
        date = 1723650000000L,
        createdAt = 1723650000000L,
        categoryName = "Food & Dining",
        categoryIcon = "restaurant",
        categoryColorHex = "#8F4C38"
      )
    )
    val csv = repo.generateCsvExport(sampleTransactions)
    assertTrue(csv.contains("ID,Date,Type,Category,Amount,Note"))
    assertTrue(csv.contains("Lunch with team"))
    assertTrue(csv.contains("Food & Dining"))
    assertTrue(csv.contains("45.50"))
  }

  @Test
  fun testUserPreferencesDefaults() {
    val prefs = com.example.data.local.UserPreferences()
    assertEquals("USD", prefs.currencyCode)
    assertEquals("$", prefs.currencySymbol)
    assertFalse(prefs.isAppLockEnabled)
    assertFalse(prefs.isBiometricEnabled)
    assertEquals("", prefs.appLockPin)
    assertEquals("en", prefs.language)
  }

  @Test
  fun testBiometricStatusEnum() {
    val status = com.example.data.service.BiometricStatus.AVAILABLE
    assertEquals("AVAILABLE", status.name)
  }
}

