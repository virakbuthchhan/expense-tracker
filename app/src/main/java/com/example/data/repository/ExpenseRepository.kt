package com.example.data.repository

import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategorySpend
import com.example.data.local.ExpenseDao
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseRepository(
    private val expenseDao: ExpenseDao
) {
    val allTransactions: Flow<List<TransactionWithCategory>> =
        expenseDao.getAllTransactionsWithCategory()

    val allCategories: Flow<List<CategoryEntity>> =
        expenseDao.getAllCategories()

    val allBudgets: Flow<List<BudgetEntity>> =
        expenseDao.getAllBudgets()

    fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>> =
        expenseDao.getBudgetsForMonth(month)

    fun getCategorySpending(startDate: Long, endDate: Long): Flow<List<CategorySpend>> =
        expenseDao.getCategorySpendingInRange(startDate, endDate)

    suspend fun addTransaction(
        amount: Double,
        type: String,
        categoryId: Int,
        note: String,
        date: Long
    ): Long = withContext(Dispatchers.IO) {
        expenseDao.insertTransaction(
            TransactionEntity(
                amount = amount,
                type = type,
                categoryId = categoryId,
                note = note,
                date = date
            )
        )
    }

    suspend fun updateTransaction(
        id: Int,
        amount: Double,
        type: String,
        categoryId: Int,
        note: String,
        date: Long
    ) = withContext(Dispatchers.IO) {
        expenseDao.updateTransaction(
            TransactionEntity(
                id = id,
                amount = amount,
                type = type,
                categoryId = categoryId,
                note = note,
                date = date
            )
        )
    }

    suspend fun deleteTransaction(id: Int) = withContext(Dispatchers.IO) {
        expenseDao.deleteTransactionById(id)
    }

    suspend fun addCategory(
        name: String,
        icon: String,
        colorHex: String,
        type: String = "expense"
    ): Long = withContext(Dispatchers.IO) {
        expenseDao.insertCategory(
            CategoryEntity(
                name = name,
                icon = icon,
                colorHex = colorHex,
                isDefault = false,
                type = type
            )
        )
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        expenseDao.deleteCategory(category)
    }

    suspend fun setBudget(categoryId: Int, monthlyLimit: Double, month: String) = withContext(Dispatchers.IO) {
        expenseDao.insertOrUpdateBudget(
            BudgetEntity(
                categoryId = categoryId,
                monthlyLimit = monthlyLimit,
                month = month
            )
        )
    }

    suspend fun deleteBudget(id: Int) = withContext(Dispatchers.IO) {
        expenseDao.deleteBudgetById(id)
    }

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = expenseDao.getCategoryCount()
        if (count == 0) {
            val defaultCategories = listOf(
                CategoryEntity(id = 1, name = "Food & Dining", icon = "restaurant", colorHex = "#8F4C38", isDefault = true, type = "expense"),
                CategoryEntity(id = 2, name = "Transportation", icon = "directions_car", colorHex = "#0284C7", isDefault = true, type = "expense"),
                CategoryEntity(id = 3, name = "Housing & Bills", icon = "home", colorHex = "#7C3AED", isDefault = true, type = "expense"),
                CategoryEntity(id = 4, name = "Shopping", icon = "shopping_bag", colorHex = "#C026D3", isDefault = true, type = "expense"),
                CategoryEntity(id = 5, name = "Groceries", icon = "shopping_cart", colorHex = "#D97706", isDefault = true, type = "expense"),
                CategoryEntity(id = 6, name = "Health & Fitness", icon = "fitness_center", colorHex = "#0D9488", isDefault = true, type = "expense"),
                CategoryEntity(id = 7, name = "Entertainment", icon = "movie", colorHex = "#4F46E5", isDefault = true, type = "expense"),
                CategoryEntity(id = 8, name = "Travel", icon = "flight", colorHex = "#EA580C", isDefault = true, type = "expense"),
                CategoryEntity(id = 9, name = "Education", icon = "school", colorHex = "#0284C7", isDefault = true, type = "expense"),
                CategoryEntity(id = 10, name = "Other Expense", icon = "more_horiz", colorHex = "#78716C", isDefault = true, type = "expense"),
                CategoryEntity(id = 11, name = "Salary", icon = "payments", colorHex = "#2E7D32", isDefault = true, type = "income"),
                CategoryEntity(id = 12, name = "Freelance", icon = "laptop", colorHex = "#0D9488", isDefault = true, type = "income"),
                CategoryEntity(id = 13, name = "Investments", icon = "trending_up", colorHex = "#7C3AED", isDefault = true, type = "income"),
                CategoryEntity(id = 14, name = "Gifts & Other", icon = "card_giftcard", colorHex = "#D97706", isDefault = true, type = "income")
            )
            expenseDao.insertCategories(defaultCategories)
        }
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        expenseDao.deleteAllTransactions()
        expenseDao.deleteAllBudgets()
    }

    fun generateCsvExport(transactions: List<TransactionWithCategory>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("ID,Date,Type,Category,Amount,Note\n")
        transactions.forEach { t ->
            val dateStr = dateFormat.format(Date(t.date))
            val safeNote = "\"" + t.note.replace("\"", "\"\"") + "\""
            val safeCategory = "\"" + t.categoryName.replace("\"", "\"\"") + "\""
            sb.append("${t.id},$dateStr,${t.type},$safeCategory,${String.format(Locale.US, "%.2f", t.amount)},$safeNote\n")
        }
        return sb.toString()
    }

    fun generateJsonExport(transactions: List<TransactionWithCategory>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("[\n")
        transactions.forEachIndexed { index, t ->
            val dateStr = dateFormat.format(Date(t.date))
            val escapedNote = t.note.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
            val escapedCat = t.categoryName.replace("\\", "\\\\").replace("\"", "\\\"")
            sb.append("  {\n")
            sb.append("    \"id\": ${t.id},\n")
            sb.append("    \"date\": \"$dateStr\",\n")
            sb.append("    \"type\": \"${t.type}\",\n")
            sb.append("    \"categoryId\": ${t.categoryId},\n")
            sb.append("    \"category\": \"$escapedCat\",\n")
            sb.append("    \"amount\": ${String.format(Locale.US, "%.2f", t.amount)},\n")
            sb.append("    \"note\": \"$escapedNote\"\n")
            sb.append("  }")
            if (index < transactions.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]\n")
        return sb.toString()
    }
}
