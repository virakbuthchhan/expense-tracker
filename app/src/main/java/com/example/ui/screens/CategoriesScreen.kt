package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CategoryEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CategoryColors
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel

@Composable
fun CategoriesScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val expenseCategories = allCategories.filter { it.type == "expense" || it.type == "both" }
    val incomeCategories = allCategories.filter { it.type == "income" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("categories_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = strings.categoriesTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.categoriesSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expense Categories Section
            item {
                Text(
                    text = "${strings.expenseCategories} (${expenseCategories.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(expenseCategories) { cat ->
                CategoryRowItem(
                    category = cat,
                    defaultLabel = strings.defaultCategoryBadge,
                    customLabel = strings.customCategoryBadge,
                    deleteContentDesc = strings.delete,
                    onDelete = { viewModel.deleteCategory(cat) }
                )
            }

            // Income Categories Section
            item {
                Text(
                    text = "${strings.incomeCategories} (${incomeCategories.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            items(incomeCategories) { cat ->
                CategoryRowItem(
                    category = cat,
                    defaultLabel = strings.defaultCategoryBadge,
                    customLabel = strings.customCategoryBadge,
                    deleteContentDesc = strings.delete,
                    onDelete = { viewModel.deleteCategory(cat) }
                )
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddCategoryDialog = true },
            containerColor = Emerald500,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .testTag("add_category_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = strings.addNewCategory,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, icon, colorHex, type ->
                viewModel.addCustomCategory(name, icon, colorHex, type)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
fun CategoryRowItem(
    category: CategoryEntity,
    defaultLabel: String = "Default",
    customLabel: String = "Custom",
    deleteContentDesc: String = "Delete",
    onDelete: () -> Unit
) {
    val catColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        Emerald500
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(catColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconHelper.getIcon(category.icon),
                        contentDescription = category.name,
                        tint = catColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (category.isDefault) defaultLabel else customLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!category.isDefault) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = deleteContentDesc,
                        tint = ExpenseRed
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(CategoryIconHelper.availableIcons.first().first) }
    var selectedColor by remember { mutableStateOf(CategoryColors.first()) }
    var type by remember { mutableStateOf("expense") }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun colorToHex(c: Color): String {
        val argb = android.graphics.Color.argb(
            (c.alpha * 255).toInt(),
            (c.red * 255).toInt(),
            (c.green * 255).toInt(),
            (c.blue * 255).toInt()
        )
        return String.format("#%06X", 0xFFFFFF and argb)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = strings.addNewCategory,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    listOf("expense" to strings.filterExpense, "income" to strings.filterIncome).forEach { (t, label) ->
                        val isSel = type == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { type = t }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    placeholder = { Text(strings.categoryNamePlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = strings.icon,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconHelper.availableIcons.take(12).forEach { (iconKey, vector) ->
                        val isSel = selectedIcon == iconKey
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) selectedColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    if (isSel) 2.dp else 0.dp,
                                    if (isSel) selectedColor else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIcon = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = iconKey,
                                tint = if (isSel) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = strings.color,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryColors.take(8).forEach { color ->
                        val isSel = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (isSel) 3.dp else 0.dp,
                                    if (isSel) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = {
                            if (name.trim().isBlank()) {
                                errorText = strings.categoryNamePlaceholder
                            } else {
                                onSave(name.trim(), selectedIcon, colorToHex(selectedColor), type)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.saveCategory, color = Color.White)
                    }
                }
            }
        }
    }
}
