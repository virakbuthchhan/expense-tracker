package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.TransactionWithCategory
import com.example.ui.components.AppLockOverlay
import com.example.ui.screens.AddCategoryDialog
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.viewmodel.ExpenseViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "History", Icons.Default.ReceiptLong)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.PieChart)
    object Budgets : Screen("budgets", "Budgets", Icons.Default.Savings)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Categories : Screen("categories", "Categories", Icons.Default.Category)
    object Export : Screen("export", "Export", Icons.Default.FileDownload)
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Analytics,
    Screen.Budgets,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()

    var activeTransactionToEdit by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var showNewCategoryDialogFromSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val isTopLevelScreen = bottomNavScreens.any { it.route == currentRoute }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (!isTopLevelScreen) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentRoute) {
                                    Screen.Categories.route -> "Categories"
                                    Screen.Export.route -> "Data Export"
                                    else -> ""
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (isTopLevelScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp)
                            .testTag("main_bottom_nav_bar")
                    ) {
                        bottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTransactions = {
                            navController.navigate(Screen.Transactions.route)
                        },
                        onNavigateToAnalytics = {
                            navController.navigate(Screen.Analytics.route)
                        },
                        onNavigateToBudgets = {
                            navController.navigate(Screen.Budgets.route)
                        },
                        onAddTransactionClick = {
                            activeTransactionToEdit = null
                            showAddTransactionSheet = true
                        },
                        onEditTransactionClick = { tx ->
                            activeTransactionToEdit = tx
                            showAddTransactionSheet = true
                        }
                    )
                }

                composable(Screen.Transactions.route) {
                    TransactionsScreen(
                        viewModel = viewModel,
                        onAddTransactionClick = {
                            activeTransactionToEdit = null
                            showAddTransactionSheet = true
                        },
                        onEditTransactionClick = { tx ->
                            activeTransactionToEdit = tx
                            showAddTransactionSheet = true
                        }
                    )
                }

                composable(Screen.Analytics.route) {
                    AnalyticsScreen(viewModel = viewModel)
                }

                composable(Screen.Budgets.route) {
                    BudgetsScreen(viewModel = viewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToCategories = {
                            navController.navigate(Screen.Categories.route)
                        },
                        onNavigateToExport = {
                            navController.navigate(Screen.Export.route)
                        }
                    )
                }

                composable(Screen.Categories.route) {
                    CategoriesScreen(viewModel = viewModel)
                }

                composable(Screen.Export.route) {
                    ExportScreen(viewModel = viewModel)
                }
            }
        }

        // Add / Edit Transaction Sheet Dialog
        if (showAddTransactionSheet) {
            AddEditTransactionSheet(
                viewModel = viewModel,
                transactionToEdit = activeTransactionToEdit,
                onDismiss = {
                    showAddTransactionSheet = false
                    activeTransactionToEdit = null
                },
                onOpenCreateCategory = {
                    showNewCategoryDialogFromSheet = true
                }
            )
        }

        // Quick Category Create Dialog from Sheet
        if (showNewCategoryDialogFromSheet) {
            AddCategoryDialog(
                onDismiss = { showNewCategoryDialogFromSheet = false },
                onSave = { name, icon, colorHex, type ->
                    viewModel.addCustomCategory(name, icon, colorHex, type)
                    showNewCategoryDialogFromSheet = false
                }
            )
        }

        // App Lock Overlay
        AnimatedVisibility(
            visible = isAppLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AppLockOverlay(
                correctPin = preferences.appLockPin,
                onUnlocked = { viewModel.unlockApp() }
            )
        }
    }
}
