package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategorySpend
import com.example.data.local.PreferenceRepository
import com.example.data.local.TransactionWithCategory
import com.example.data.local.UserPreferences
import com.example.data.repository.ExpenseRepository
import com.example.ui.components.DailySpendPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BudgetWithStatus(
    val budgetId: Int,
    val categoryId: Int,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val monthlyLimit: Double,
    val spentAmount: Double,
    val month: String
)

data class DashboardMetrics(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val savingsRate: Double = 0.0,
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val topSpendingCategories: List<CategorySpend> = emptyList(),
    val dailySpendPoints: List<DailySpendPoint> = emptyList(),
    val budgetAlerts: List<BudgetWithStatus> = emptyList()
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ExpenseRepository(db.expenseDao())
    private val preferenceRepository = PreferenceRepository(application)

    val userPreferences: StateFlow<UserPreferences> = preferenceRepository.userPreferences

    // Lock screen status
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("all") // "all", "expense", "income"
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<Int?>(null)
    val selectedCategoryFilter: StateFlow<Int?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedMonth = MutableStateFlow(getCurrentMonthString())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val allTransactions: StateFlow<List<TransactionWithCategory>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (!preferenceRepository.hasCleanedSampleData()) {
                repository.resetAllData()
                preferenceRepository.markSampleDataCleaned()
            }
            repository.initializeDefaultDataIfEmpty()
            if (preferenceRepository.userPreferences.value.isAppLockEnabled &&
                preferenceRepository.userPreferences.value.appLockPin.isNotBlank()
            ) {
                _isAppLocked.value = true
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun relockApp() {
        if (preferenceRepository.userPreferences.value.isAppLockEnabled &&
            preferenceRepository.userPreferences.value.appLockPin.isNotBlank()
        ) {
            _isAppLocked.value = true
        }
    }

    // Filtered Transactions
    val filteredTransactions: StateFlow<List<TransactionWithCategory>> = combine(
        allTransactions,
        _searchQuery,
        _selectedTypeFilter,
        _selectedCategoryFilter
    ) { transactions, query, typeFilter, categoryFilter ->
        transactions.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.categoryName.contains(query, ignoreCase = true)

            val matchesType = when (typeFilter) {
                "expense" -> tx.type == "expense"
                "income" -> tx.type == "income"
                else -> true
            }

            val matchesCategory = categoryFilter == null || tx.categoryId == categoryFilter

            matchesQuery && matchesType && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grouped by Date (e.g. "Today", "Yesterday", "August 14, 2026")
    val groupedTransactions: StateFlow<Map<String, List<TransactionWithCategory>>> =
        filteredTransactions.combine(_selectedMonth) { list, _ ->
            val groupMap = LinkedHashMap<String, MutableList<TransactionWithCategory>>()
            val todayCal = Calendar.getInstance()
            val txCal = Calendar.getInstance()

            val dayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

            list.forEach { tx ->
                txCal.timeInMillis = tx.date
                val isSameYear = todayCal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
                val isToday = isSameYear && todayCal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)

                val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val isYesterday = yesterdayCal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                        yesterdayCal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)

                val header = when {
                    isToday -> "Today"
                    isYesterday -> "Yesterday"
                    else -> dayFormat.format(Date(tx.date))
                }

                if (!groupMap.containsKey(header)) {
                    groupMap[header] = mutableListOf()
                }
                groupMap[header]?.add(tx)
            }
            groupMap
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allTransactions,
        allBudgets,
        allCategories,
        _selectedMonth
    ) { transactions, budgets, categories, monthStr ->
        val monthCal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val (monthStart, monthEnd) = getMonthStartAndEndMillis(monthStr)

        var totalBal = 0.0
        var mIncome = 0.0
        var mExpense = 0.0

        val categorySpendMap = mutableMapOf<Int, Double>()
        val daySpendMap = mutableMapOf<Int, Double>() // day of month -> expense

        transactions.forEach { tx ->
            if (tx.type == "income") {
                totalBal += tx.amount
                if (tx.date in monthStart..monthEnd) {
                    mIncome += tx.amount
                }
            } else {
                totalBal -= tx.amount
                if (tx.date in monthStart..monthEnd) {
                    mExpense += tx.amount
                    categorySpendMap[tx.categoryId] = (categorySpendMap[tx.categoryId] ?: 0.0) + tx.amount

                    monthCal.timeInMillis = tx.date
                    val day = monthCal.get(Calendar.DAY_OF_MONTH)
                    daySpendMap[day] = (daySpendMap[day] ?: 0.0) + tx.amount
                }
            }
        }

        val savingsRate = if (mIncome > 0) ((mIncome - mExpense) / mIncome * 100).coerceAtLeast(0.0) else 0.0

        // Top Category Spends
        val catMap = categories.associateBy { it.id }
        val categorySpends = categorySpendMap.mapNotNull { (catId, sum) ->
            val cat = catMap[catId] ?: return@mapNotNull null
            CategorySpend(
                categoryId = catId,
                categoryName = cat.name,
                categoryIcon = cat.icon,
                categoryColorHex = cat.colorHex,
                totalAmount = sum,
                transactionCount = transactions.count { it.categoryId == catId && it.date in monthStart..monthEnd }
            )
        }.sortedByDescending { it.totalAmount }

        // Daily Spend Trend Points (1 to 28/30/31 days)
        val maxDaysInMonth = getDaysInMonth(monthStr)
        val points = (1..maxDaysInMonth).map { day ->
            DailySpendPoint(
                label = "$day",
                expenseAmount = daySpendMap[day] ?: 0.0
            )
        }

        // Budget Alerts
        val monthBudgets = budgets.filter { it.month == monthStr }
        val budgetStatus = monthBudgets.mapNotNull { b ->
            val cat = catMap[b.categoryId] ?: return@mapNotNull null
            val spent = categorySpendMap[b.categoryId] ?: 0.0
            BudgetWithStatus(
                budgetId = b.id,
                categoryId = b.categoryId,
                categoryName = cat.name,
                categoryIcon = cat.icon,
                categoryColorHex = cat.colorHex,
                monthlyLimit = b.monthlyLimit,
                spentAmount = spent,
                month = b.month
            )
        }

        val budgetAlerts = budgetStatus.filter { it.spentAmount >= it.monthlyLimit * 0.8 }

        DashboardMetrics(
            totalBalance = totalBal,
            monthlyIncome = mIncome,
            monthlyExpense = mExpense,
            savingsRate = savingsRate,
            recentTransactions = transactions.take(6),
            topSpendingCategories = categorySpends,
            dailySpendPoints = points,
            budgetAlerts = budgetAlerts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Budgets with Status for Selected Month
    val budgetsWithStatus: StateFlow<List<BudgetWithStatus>> = combine(
        allBudgets,
        allCategories,
        allTransactions,
        _selectedMonth
    ) { budgets, categories, transactions, monthStr ->
        val (monthStart, monthEnd) = getMonthStartAndEndMillis(monthStr)
        val catMap = categories.associateBy { it.id }

        val categorySpendMap = mutableMapOf<Int, Double>()
        transactions.filter { it.type == "expense" && it.date in monthStart..monthEnd }.forEach { tx ->
            categorySpendMap[tx.categoryId] = (categorySpendMap[tx.categoryId] ?: 0.0) + tx.amount
        }

        budgets.filter { it.month == monthStr }.mapNotNull { b ->
            val cat = catMap[b.categoryId] ?: return@mapNotNull null
            BudgetWithStatus(
                budgetId = b.id,
                categoryId = b.categoryId,
                categoryName = cat.name,
                categoryIcon = cat.icon,
                categoryColorHex = cat.colorHex,
                monthlyLimit = b.monthlyLimit,
                spentAmount = categorySpendMap[b.categoryId] ?: 0.0,
                month = b.month
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun setCategoryFilter(categoryId: Int?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    // CRUD Actions
    fun saveTransaction(
        id: Int? = null,
        amount: Double,
        type: String,
        categoryId: Int,
        note: String,
        date: Long,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (id != null && id > 0) {
                repository.updateTransaction(id, amount, type, categoryId, note, date)
            } else {
                repository.addTransaction(amount, type, categoryId, note, date)
            }
            onComplete()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun addCustomCategory(name: String, icon: String, colorHex: String, type: String = "expense", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addCategory(name, icon, colorHex, type)
            onComplete()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun setBudget(categoryId: Int, monthlyLimit: Double, month: String = _selectedMonth.value, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.setBudget(categoryId, monthlyLimit, month)
            onComplete()
        }
    }

    fun deleteBudget(id: Int) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun setCurrency(code: String, symbol: String) {
        preferenceRepository.setCurrency(code, symbol)
    }

    fun setLanguage(languageCode: String) {
        preferenceRepository.setLanguage(languageCode)
    }

    fun setThemePreference(isDark: Boolean, followSystem: Boolean) {
        preferenceRepository.setThemePreference(isDark, followSystem)
    }

    fun setAppLock(enabled: Boolean, pin: String) {
        preferenceRepository.setAppLock(enabled, pin)
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    fun resetAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.resetAllData()
            onComplete()
        }
    }

    fun generateCsvExport(): String {
        return repository.generateCsvExport(allTransactions.value)
    }

    fun generateJsonExport(): String {
        return repository.generateJsonExport(allTransactions.value)
    }

    fun exportPdfToStorageUri(
        context: android.content.Context,
        targetUri: android.net.Uri,
        appStrings: com.example.ui.i18n.AppStrings,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val prefs = userPreferences.value
            val success = com.example.data.service.PdfExportService.exportToStorageUri(
                context = context,
                targetUri = targetUri,
                transactions = allTransactions.value,
                currencyCode = prefs.currencyCode,
                currencySymbol = prefs.currencySymbol,
                appStrings = appStrings
            )
            onResult(success)
        }
    }

    fun preparePdfForSharing(
        context: android.content.Context,
        appStrings: com.example.ui.i18n.AppStrings,
        onReady: (android.net.Uri?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val prefs = userPreferences.value
                val uri = com.example.data.service.PdfExportService.savePdfToCacheFile(
                    context = context,
                    transactions = allTransactions.value,
                    currencyCode = prefs.currencyCode,
                    currencySymbol = prefs.currencySymbol,
                    appStrings = appStrings
                )
                onReady(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                onReady(null)
            }
        }
    }

    companion object {
        fun getCurrentMonthString(): String {
            return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        }

        fun getMonthStartAndEndMillis(monthStr: String): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            val parts = monthStr.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
            val month = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1

            cal.set(year, month, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            return Pair(start, end)
        }

        fun getDaysInMonth(monthStr: String): Int {
            val cal = Calendar.getInstance()
            val parts = monthStr.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
            val month = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
            cal.set(year, month, 1)
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }
}
