package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TransactionWithCategory
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.TransactionItemCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (TransactionWithCategory) -> Unit,
    onNavigateToImport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    val groupedTransactions by viewModel.groupedTransactions.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val dateFilterType by viewModel.dateFilterType.collectAsStateWithLifecycle()
    val customFilterDate by viewModel.customFilterDate.collectAsStateWithLifecycle()
    val customFilterMonth by viewModel.customFilterMonth.collectAsStateWithLifecycle()

    // Export Dialog State
    var showExportDialog by remember { mutableStateOf(false) }

    // Custom Date Picker Dialog State
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = customFilterDate ?: System.currentTimeMillis()
    )

    // Custom Month Picker Dialog State
    var showMonthPickerDialog by remember { mutableStateOf(false) }

    // Export storage document launcher state
    var pendingExportFormat by remember { mutableStateOf("csv") } // csv, tsv, json, pdf
    var pendingExportList by remember { mutableStateOf<List<TransactionWithCategory>>(emptyList()) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            when (pendingExportFormat) {
                "csv" -> "text/csv"
                "tsv" -> "text/tab-separated-values"
                "json" -> "application/json"
                "pdf" -> "application/pdf"
                else -> "text/plain"
            }
        )
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    when (pendingExportFormat) {
                        "csv" -> {
                            val content = viewModel.generateCsvExport(pendingExportList)
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openOutputStream(uri)?.use { os ->
                                    OutputStreamWriter(os).use { writer -> writer.write(content) }
                                }
                            }
                            Toast.makeText(context, strings.exportSuccessToast, Toast.LENGTH_SHORT).show()
                            viewModel.recordExportDone()
                        }
                        "tsv" -> {
                            val content = viewModel.generateTsvExport(pendingExportList)
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openOutputStream(uri)?.use { os ->
                                    OutputStreamWriter(os).use { writer -> writer.write(content) }
                                }
                            }
                            Toast.makeText(context, strings.exportSuccessToast, Toast.LENGTH_SHORT).show()
                            viewModel.recordExportDone()
                        }
                        "json" -> {
                            val content = viewModel.generateJsonExport(pendingExportList)
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openOutputStream(uri)?.use { os ->
                                    OutputStreamWriter(os).use { writer -> writer.write(content) }
                                }
                            }
                            Toast.makeText(context, strings.exportSuccessToast, Toast.LENGTH_SHORT).show()
                            viewModel.recordExportDone()
                        }
                        "pdf" -> {
                            viewModel.exportPdfToStorageUri(
                                context = context,
                                targetUri = uri,
                                appStrings = strings,
                                transactions = pendingExportList
                            ) { success ->
                                if (success) {
                                    Toast.makeText(context, strings.exportSuccessToast, Toast.LENGTH_SHORT).show()
                                    viewModel.recordExportDone()
                                } else {
                                    Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Header Bar: Title & Action Icons (Export, Import)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.transactionsTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${filteredTransactions.size} ${strings.totalCountLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Export Icon Button
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_transactions_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = strings.exportHistoryAction,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Import Icon Button
                    IconButton(
                        onClick = onNavigateToImport,
                        modifier = Modifier.testTag("import_transactions_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = strings.importTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(strings.searchPlaceholder, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("search_transactions_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Type Filter Selector (All, Expenses, Income)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val filters = listOf(
                    "all" to strings.filterAll,
                    "expense" to strings.filterExpense,
                    "income" to strings.filterIncome
                )
                filters.forEach { (key, label) ->
                    val isSelected = selectedTypeFilter == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable { viewModel.setTypeFilter(key) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Month Filter Horizontal Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                // All Time
                item {
                    val isSelected = dateFilterType == DateFilterType.ALL_TIME
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDateFilterType(DateFilterType.ALL_TIME) },
                        label = { Text(strings.filterAllTime, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Today
                item {
                    val isSelected = dateFilterType == DateFilterType.TODAY
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDateFilterType(DateFilterType.TODAY) },
                        label = { Text(strings.filterToday, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // This Week
                item {
                    val isSelected = dateFilterType == DateFilterType.THIS_WEEK
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDateFilterType(DateFilterType.THIS_WEEK) },
                        label = { Text(strings.filterThisWeek, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // This Month
                item {
                    val isSelected = dateFilterType == DateFilterType.THIS_MONTH
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDateFilterType(DateFilterType.THIS_MONTH) },
                        label = { Text(strings.filterThisMonth, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Last Month
                item {
                    val isSelected = dateFilterType == DateFilterType.LAST_MONTH
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDateFilterType(DateFilterType.LAST_MONTH) },
                        label = { Text(strings.filterLastMonth, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Custom Date Chip / Picker
                item {
                    val isCustomDate = dateFilterType == DateFilterType.CUSTOM_DATE && customFilterDate != null
                    val dateLabel = if (isCustomDate && customFilterDate != null) {
                        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(customFilterDate!!))
                    } else {
                        strings.filterCustomDate
                    }

                    FilterChip(
                        selected = isCustomDate,
                        onClick = { showDatePickerDialog = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        trailingIcon = if (isCustomDate) {
                            {
                                IconButton(
                                    onClick = { viewModel.clearDateFilter() },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        } else null,
                        label = { Text(dateLabel, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Custom Month Chip / Picker
                item {
                    val isCustomMonth = dateFilterType == DateFilterType.CUSTOM_MONTH && !customFilterMonth.isNullOrBlank()
                    val monthLabel = if (isCustomMonth && customFilterMonth != null) {
                        try {
                            val inFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                            val outFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                            val d = inFormat.parse(customFilterMonth!!)
                            if (d != null) outFormat.format(d) else customFilterMonth!!
                        } catch (e: Exception) {
                            customFilterMonth!!
                        }
                    } else {
                        strings.filterCustomMonth
                    }

                    FilterChip(
                        selected = isCustomMonth,
                        onClick = { showMonthPickerDialog = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        trailingIcon = if (isCustomMonth) {
                            {
                                IconButton(
                                    onClick = { viewModel.clearDateFilter() },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        } else null,
                        label = { Text(monthLabel, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Category Filter Horizontal Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                item {
                    val isAll = selectedCategoryFilter == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (isAll) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                            .clickable { viewModel.setCategoryFilter(null) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = strings.filterAll,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAll) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(allCategories) { category ->
                    val isSelected = selectedCategoryFilter == category.id
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(category.colorHex))
                    } catch (e: Exception) {
                        Emerald500
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (isSelected) catColor
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                            .clickable {
                                viewModel.setCategoryFilter(if (isSelected) null else category.id)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(category.icon),
                                contentDescription = category.name,
                                tint = if (isSelected) Color.White else catColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Grouped Transactions List
            if (groupedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = strings.noTransactionsFound,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.tryDifferentFilter,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (dateFilterType != DateFilterType.ALL_TIME || selectedTypeFilter != "all" || selectedCategoryFilter != null || searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearDateFilter()
                                    viewModel.setTypeFilter("all")
                                    viewModel.setCategoryFilter(null)
                                    viewModel.setSearchQuery("")
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset All Filters", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedTransactions.forEach { (dateHeader, transactions) ->
                        val dailyExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
                        val dailyIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }

                        item(key = "header_$dateHeader") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Text(
                                    text = if (dailyExpense > 0) "-${preferences.currencySymbol}${String.format(Locale.US, "%,.2f", dailyExpense)}"
                                    else "+${preferences.currencySymbol}${String.format(Locale.US, "%,.2f", dailyIncome)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (dailyExpense > 0) ExpenseRed else IncomeGreen
                                )
                            }
                        }

                        items(
                            items = transactions,
                            key = { it.id }
                        ) { tx ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissVal ->
                                    if (dismissVal == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteTransaction(tx.id)
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(ExpenseRed)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White
                                        )
                                    }
                                }
                            ) {
                                TransactionItemCard(
                                    transaction = tx,
                                    currencySymbol = preferences.currencySymbol,
                                    onClick = { onEditTransactionClick(tx) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddTransactionClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("add_transaction_fab_tx_screen")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Transaction",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Export Dialog (CSV, JSON, TSV, PDF)
    if (showExportDialog) {
        var selectedFormat by remember { mutableStateOf("csv") } // csv, tsv, json, pdf
        var exportScopeFiltered by remember { mutableStateOf(true) } // true: filtered, false: all

        val targetList = if (exportScopeFiltered) filteredTransactions else allTransactions

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = strings.exportHistoryAction,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose Format",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Format Selection Grid
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val formats = listOf(
                            Triple("csv", strings.exportFormatCsv, Icons.Default.TableChart),
                            Triple("tsv", strings.exportFormatTsv, Icons.Default.TableChart),
                            Triple("json", strings.exportFormatJson, Icons.Default.ReceiptLong),
                            Triple("pdf", strings.exportFormatPdf, Icons.Default.PictureAsPdf)
                        )

                        formats.forEach { (formatKey, label, icon) ->
                            val isSelected = selectedFormat == formatKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedFormat = formatKey }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedFormat = formatKey }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Export Range",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Scope options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = exportScopeFiltered,
                            onClick = { exportScopeFiltered = true },
                            label = {
                                Text(
                                    String.format(Locale.getDefault(), strings.exportScopeFiltered, filteredTransactions.size),
                                    fontSize = 11.5.sp
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = !exportScopeFiltered,
                            onClick = { exportScopeFiltered = false },
                            label = {
                                Text(
                                    String.format(Locale.getDefault(), strings.exportScopeAll, allTransactions.size),
                                    fontSize = 11.5.sp
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetList.isEmpty()) {
                            Toast.makeText(context, "No transactions to export.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        pendingExportFormat = selectedFormat
                        pendingExportList = targetList
                        showExportDialog = false

                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val defaultFileName = "transactions_$timeStamp.$selectedFormat"
                        createDocumentLauncher.launch(defaultFileName)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save File")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        if (targetList.isEmpty()) {
                            Toast.makeText(context, "No transactions to share.", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        showExportDialog = false

                        scope.launch {
                            try {
                                when (selectedFormat) {
                                    "csv" -> {
                                        val content = viewModel.generateCsvExport(targetList)
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, content)
                                            putExtra(Intent.EXTRA_TITLE, "Transactions Export (CSV)")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share CSV"))
                                        viewModel.recordExportDone()
                                    }
                                    "tsv" -> {
                                        val content = viewModel.generateTsvExport(targetList)
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, content)
                                            putExtra(Intent.EXTRA_TITLE, "Transactions Export (TSV)")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share TSV"))
                                        viewModel.recordExportDone()
                                    }
                                    "json" -> {
                                        val content = viewModel.generateJsonExport(targetList)
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, content)
                                            putExtra(Intent.EXTRA_TITLE, "Transactions Export (JSON)")
                                            type = "application/json"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share JSON"))
                                        viewModel.recordExportDone()
                                    }
                                    "pdf" -> {
                                        viewModel.preparePdfForSharing(
                                            context = context,
                                            appStrings = strings,
                                            transactions = targetList
                                        ) { uri ->
                                            if (uri != null) {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    type = "application/pdf"
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Share Statement PDF"))
                                                viewModel.recordExportDone()
                                            } else {
                                                Toast.makeText(context, "Could not generate PDF", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        )
    }

    // Material 3 Date Picker Dialog
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            viewModel.setCustomFilterDate(selectedMillis)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Custom Month Selector Dialog
    if (showMonthPickerDialog) {
        val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
        var pickerYear by remember { mutableIntStateOf(currentYear) }

        val months = listOf(
            "01" to "Jan", "02" to "Feb", "03" to "Mar", "04" to "Apr",
            "05" to "May", "06" to "Jun", "07" to "Jul", "08" to "Aug",
            "09" to "Sep", "10" to "Oct", "11" to "Nov", "12" to "Dec"
        )

        AlertDialog(
            onDismissRequest = { showMonthPickerDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pickerYear-- }) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Year")
                    }
                    Text(
                        text = "$pickerYear",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { pickerYear++ }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Year")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val monthIdx = row * 3 + col
                                val (monthNum, monthName) = months[monthIdx]
                                val monthKey = "$pickerYear-$monthNum"
                                val isSelected = customFilterMonth == monthKey

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.setCustomFilterMonth(monthKey)
                                            showMonthPickerDialog = false
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = monthName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMonthPickerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
