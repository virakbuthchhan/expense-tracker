package com.example.ui.navigation

import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material.icons.filled.Tour
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
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.AddCategoryDialog
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.ImportScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.viewmodel.ExpenseViewModel

sealed class Screen(val route: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", Icons.Default.ReceiptLong)
    object Analytics : Screen("analytics", Icons.Default.PieChart)
    object Budgets : Screen("budgets", Icons.Default.Savings)
    object Settings : Screen("settings", Icons.Default.Settings)
    object Categories : Screen("categories", Icons.Default.Category)
    object Export : Screen("export", Icons.Default.FileDownload)
    object Import : Screen("import", Icons.Default.FileDownload)
    object Onboarding : Screen("onboarding", Icons.Default.Tour)
}

fun Screen.getTitle(strings: AppStrings): String {
    return when (this) {
        Screen.Dashboard -> strings.navHome
        Screen.Transactions -> strings.navHistory
        Screen.Analytics -> strings.navAnalytics
        Screen.Budgets -> strings.navBudgets
        Screen.Settings -> strings.navSettings
        Screen.Categories -> strings.navCategories
        Screen.Export -> strings.navExport
        Screen.Import -> strings.importTitle
        Screen.Onboarding -> "Welcome"
    }
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Analytics,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: ExpenseViewModel,
    initialDestination: String? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current

    LaunchedEffect(initialDestination) {
        if (initialDestination == Screen.Export.route) {
            navController.navigate(Screen.Export.route) {
                launchSingleTop = true
            }
        }
    }

    var activeTransactionToEdit by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var showNewCategoryDialogFromSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: if (preferences.isOnboardingCompleted) Screen.Dashboard.route else Screen.Onboarding.route

    val isTopLevelScreen = bottomNavScreens.any { it.route == currentRoute }

    val startDest = if (preferences.isOnboardingCompleted) Screen.Dashboard.route else Screen.Onboarding.route

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (!isTopLevelScreen && currentRoute != Screen.Import.route && currentRoute != Screen.Onboarding.route) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentRoute) {
                                    Screen.Categories.route -> strings.categoriesTitle
                                    Screen.Export.route -> strings.exportTitle
                                    Screen.Budgets.route -> strings.navBudgets
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
                if (isTopLevelScreen && currentRoute != Screen.Onboarding.route) {
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
                            val title = screen.getTitle(strings)
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = title,
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
                startDestination = startDest,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinish = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Onboarding.route) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }

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
                        },
                        onNavigateToImport = {
                            navController.navigate(Screen.Import.route)
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
                        onNavigateToBudgets = {
                            navController.navigate(Screen.Budgets.route)
                        },
                        onNavigateToExport = {
                            navController.navigate(Screen.Export.route)
                        },
                        onNavigateToImport = {
                            navController.navigate(Screen.Import.route)
                        },
                        onNavigateToOnboarding = {
                            navController.navigate(Screen.Onboarding.route)
                        }
                    )
                }

                composable(Screen.Categories.route) {
                    CategoriesScreen(viewModel = viewModel)
                }

                composable(Screen.Export.route) {
                    ExportScreen(
                        viewModel = viewModel,
                        onNavigateToImport = {
                            navController.navigate(Screen.Import.route)
                        }
                    )
                }

                composable(Screen.Import.route) {
                    ImportScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTransactions = {
                            navController.navigate(Screen.Transactions.route) {
                                popUpTo(Screen.Dashboard.route)
                            }
                        },
                        onNavigateToDashboard = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    )
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
                isBiometricEnabled = preferences.isBiometricEnabled,
                onUnlocked = { viewModel.unlockApp() }
            )
        }
    }
}
