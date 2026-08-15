package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.IncomeVsExpenseComparisonCard
import com.example.ui.components.SpendingTrendBarChart
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current

    val monthCal = Calendar.getInstance().apply {
        try {
            val parts = selectedMonth.split("-")
            val y = parts[0].toInt()
            val m = parts[1].toInt() - 1
            set(y, m, 1)
        } catch (e: Exception) {
            // Default
        }
    }

    val displayMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)

    fun changeMonth(offset: Int) {
        monthCal.add(Calendar.MONTH, offset)
        val newMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(monthCal.time)
        viewModel.setSelectedMonth(newMonthStr)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = strings.analyticsTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.analyticsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Month Selector with Prev/Next buttons
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { changeMonth(-1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = displayMonth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = { changeMonth(1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Donut Chart: Spending by Category
        item {
            CategoryDonutChart(
                spendingList = metrics.topSpendingCategories,
                currencySymbol = preferences.currencySymbol
            )
        }

        // Income vs Expense Comparison Bar
        item {
            IncomeVsExpenseComparisonCard(
                totalIncome = metrics.monthlyIncome,
                totalExpense = metrics.monthlyExpense,
                currencySymbol = preferences.currencySymbol
            )
        }

        // Spending Trend Over Time (Bar Chart)
        if (metrics.dailySpendPoints.isNotEmpty()) {
            item {
                SpendingTrendBarChart(
                    points = metrics.dailySpendPoints,
                    currencySymbol = preferences.currencySymbol
                )
            }
        }

        // Ranked Category Spending Table
        if (metrics.topSpendingCategories.isNotEmpty()) {
            item {
                val totalSpend = metrics.monthlyExpense.coerceAtLeast(1.0)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = strings.topCategories,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        metrics.topSpendingCategories.forEach { cat ->
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(cat.categoryColorHex))
                            } catch (e: Exception) {
                                Emerald500
                            }
                            val progress = (cat.totalAmount / totalSpend).toFloat()
                            val pct = (progress * 100).toInt()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(catColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getIcon(cat.categoryIcon),
                                                contentDescription = cat.categoryName,
                                                tint = catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = cat.categoryName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${cat.transactionCount} ${strings.transactionsTitle.lowercase()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${preferences.currencySymbol}${String.format(Locale.US, "%,.2f", cat.totalAmount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$pct%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(100.dp)),
                                    color = catColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
