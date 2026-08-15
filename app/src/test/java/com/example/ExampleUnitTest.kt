package com.example

import com.example.data.local.ExpenseDao
import com.example.data.local.TransactionWithCategory
import com.example.data.repository.ExpenseRepository
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCsvExportFormat() {
    val dao = mock(ExpenseDao::class.java)
    val repo = ExpenseRepository(dao)
    val sampleTransactions = listOf(
      TransactionWithCategory(
        id = 1,
        amount = 45.50,
        type = "expense",
        categoryId = 1,
        note = "Lunch with team",
        date = 1723650000000L,
        categoryName = "Food & Dining",
        categoryIcon = "restaurant",
        categoryColorHex = "#8F4C38",
        categoryType = "expense"
      )
    )
    val csv = repo.generateCsvExport(sampleTransactions)
    assertTrue(csv.contains("ID,Date,Type,Category,Amount,Note"))
    assertTrue(csv.contains("Lunch with team"))
    assertTrue(csv.contains("Food & Dining"))
    assertTrue(csv.contains("45.50"))
  }
}

