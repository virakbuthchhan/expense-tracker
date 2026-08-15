package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategorySpend
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Locale
import kotlin.math.atan2

@Composable
fun CategoryDonutChart(
    spendingList: List<CategorySpend>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val totalSpending = remember(spendingList) { spendingList.sumOf { it.totalAmount } }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(spendingList) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_donut_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.categoryBreakdown,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (spendingList.isEmpty() || totalSpending <= 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noAnalyticsData,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(200.dp)
                            .pointerInput(spendingList) {
                                detectTapGestures { tapOffset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = tapOffset.x - center.x
                                    val dy = tapOffset.y - center.y
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    // Rotate so 0 is at top (-90 degrees)
                                    val adjustedAngle = (angle + 90f) % 360f

                                    var currentAngle = 0f
                                    var clicked: Int? = null
                                    for (i in spendingList.indices) {
                                        val sweep = ((spendingList[i].totalAmount / totalSpending) * 360f).toFloat()
                                        if (adjustedAngle >= currentAngle && adjustedAngle < currentAngle + sweep) {
                                            clicked = i
                                            break
                                        }
                                        currentAngle += sweep
                                    }
                                    selectedIndex = if (selectedIndex == clicked) null else clicked
                                }
                            }
                    ) {
                        val strokeWidth = 32.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        val arcSize = Size(diameter, diameter)

                        var startAngle = -90f

                        spendingList.forEachIndexed { index, item ->
                            val sliceFraction = (item.totalAmount / totalSpending).toFloat()
                            val sweepAngle = sliceFraction * 360f * animatedProgress.value
                            val isSelected = selectedIndex == index
                            val itemColor = try {
                                Color(android.graphics.Color.parseColor(item.categoryColorHex))
                            } catch (e: Exception) {
                                Emerald500
                            }

                            drawArc(
                                color = itemColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle - 2f, // subtle gap
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Center Info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val activeItem = selectedIndex?.let { spendingList.getOrNull(it) }
                        if (activeItem != null) {
                            Text(
                                text = activeItem.categoryName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.0f", activeItem.totalAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val pct = (activeItem.totalAmount / totalSpending * 100).toInt()
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = try {
                                    Color(android.graphics.Color.parseColor(activeItem.categoryColorHex))
                                } catch (e: Exception) {
                                    Emerald500
                                },
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = strings.totalSpent,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.0f", totalSpending)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Category List Breakdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    spendingList.take(6).forEachIndexed { index, item ->
                        val itemColor = try {
                            Color(android.graphics.Color.parseColor(item.categoryColorHex))
                        } catch (e: Exception) {
                            Emerald500
                        }
                        val pct = if (totalSpending > 0) (item.totalAmount / totalSpending * 100).toInt() else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selectedIndex == index) itemColor.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    selectedIndex = if (selectedIndex == index) null else index
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(itemColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", item.totalAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DailySpendPoint(
    val label: String, // e.g. "Aug 1", "Mon", "Day 5"
    val expenseAmount: Double,
    val incomeAmount: Double = 0.0
)

@Composable
fun SpendingTrendBarChart(
    points: List<DailySpendPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val maxSpend = remember(points) {
        (points.maxOfOrNull { it.expenseAmount } ?: 100.0).coerceAtLeast(50.0)
    }
    var activeIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_trend_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.dailySpendingTrend,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.spendingTrend,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (activeIndex != null) {
                    val pt = points.getOrNull(activeIndex!!)
                    if (pt != null) {
                        Text(
                            text = "${pt.label}: $currencySymbol${String.format(Locale.US, "%,.2f", pt.expenseAmount)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(strings.noAnalyticsData, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .pointerInput(points) {
                                detectTapGestures { tapOffset ->
                                    val barWidthWithGap = size.width / points.size
                                    val index = (tapOffset.x / barWidthWithGap).toInt().coerceIn(0, points.size - 1)
                                    activeIndex = if (activeIndex == index) null else index
                                }
                            }
                    ) {
                        val canvasHeight = size.height
                        val totalBars = points.size
                        val barSlotWidth = size.width / totalBars
                        val barWidth = (barSlotWidth * 0.55f).coerceAtLeast(8.dp.toPx())

                        // Baseline
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(0f, canvasHeight),
                            end = Offset(size.width, canvasHeight),
                            strokeWidth = 1.dp.toPx()
                        )

                        points.forEachIndexed { index, point ->
                            val heightFraction = (point.expenseAmount / maxSpend).toFloat().coerceIn(0.04f, 1f)
                            val barHeight = canvasHeight * heightFraction * animatedProgress.value
                            val x = index * barSlotWidth + (barSlotWidth - barWidth) / 2
                            val y = canvasHeight - barHeight
                            val isSelected = activeIndex == index

                            val brush = if (isSelected) {
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFDAD6), ExpenseRed)
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFB5A0), Color(0xFF8F4C38))
                                )
                            }

                            drawRoundRect(
                                brush = brush,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                            )
                        }
                    }

                    // Labels below canvas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val stride = (points.size / 6).coerceAtLeast(1)
                        points.forEachIndexed { index, point ->
                            if (index % stride == 0 || index == points.size - 1) {
                                Text(
                                    text = point.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeVsExpenseComparisonCard(
    totalIncome: Double,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val total = (totalIncome + totalExpense).coerceAtLeast(1.0)
    val incomePct = (totalIncome / total * 100).toInt()
    val expensePct = (totalExpense / total * 100).toInt()
    val netSavings = totalIncome - totalExpense

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("income_vs_expense_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = strings.cashFlowSummary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Comparative Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (totalIncome > 0) {
                    Box(
                        modifier = Modifier
                            .weight((totalIncome / total).toFloat().coerceAtLeast(0.01f))
                            .fillMaxWidth()
                            .background(IncomeGreen)
                    )
                }
                if (totalExpense > 0) {
                    Box(
                        modifier = Modifier
                            .weight((totalExpense / total).toFloat().coerceAtLeast(0.01f))
                            .fillMaxWidth()
                            .background(ExpenseRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(IncomeGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${strings.monthlyIncome} ($incomePct%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+$currencySymbol${String.format(Locale.US, "%,.2f", totalIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ExpenseRed))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${strings.monthlyExpense} ($expensePct%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-$currencySymbol${String.format(Locale.US, "%,.2f", totalExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Net Difference
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.netSavings,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${if (netSavings >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.2f", netSavings)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (netSavings >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }
        }
    }
}
