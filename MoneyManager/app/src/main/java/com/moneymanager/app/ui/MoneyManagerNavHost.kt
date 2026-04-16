package com.moneymanager.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.moneymanager.app.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalance)
    data object Transactions : Screen("transactions?type={type}", "Transactions", Icons.Default.Receipt) {
        fun createRoute(type: String? = null) = if (type != null) "transactions?type=$type" else "transactions"
    }
    data object Budgets : Screen("budgets", "Budgets", Icons.Default.PieChart)
    data object Reports : Screen("reports", "Reports", Icons.Default.BarChart)
    data object Goals : Screen("goals", "Goals", Icons.Default.Flag)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Tags : Screen("tags", "Tags", null)
    data object Categories : Screen("categories", "Categories", null)
    data object Transfer : Screen("transfer", "Transfer", Icons.Default.SwapHoriz)
    data object Recurring : Screen("recurring", "Recurring", Icons.Default.Repeat)
    data object RecurringForm : Screen("recurring_form?recurringId={recurringId}", "Recurring Form", null)
    data object Templates : Screen("templates", "Templates", Icons.Default.Description)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyManagerNavHost() {
    val navController = rememberNavController()
    val bottomNavScreens = listOf(
        Screen.Dashboard,
        Screen.Transactions,
        Screen.Reports,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavScreens.forEach { screen ->
                    val route = if (screen is Screen.Transactions) screen.createRoute() else screen.route
                    screen.icon?.let { icon ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
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
                    viewModel = hiltViewModel(),
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.createRoute()) }
                )
            }
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.Transactions.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "moneymanager://transactions?type={type}" }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type")
                TransactionsScreen(
                    viewModel = hiltViewModel(),
                    accountsViewModel = hiltViewModel(),
                    initialType = type
                )
            }
            composable(Screen.Budgets.route) {
                BudgetsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Goals.route) {
                GoalsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToTags = { navController.navigate(Screen.Tags.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                    onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                    onNavigateToTemplates = { navController.navigate(Screen.Templates.route) }
                )
            }
            composable(Screen.Tags.route) {
                TagsScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.Transfer.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "moneymanager://transfer" }
                )
            ) {
                TransferScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Recurring.route) {
                RecurringListScreen(
                    viewModel = hiltViewModel(),
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RecurringForm.route) { backStackEntry ->
                val recurringId = backStackEntry.arguments?.getString("recurringId")?.toLongOrNull()
                RecurringFormScreen(
                    viewModel = hiltViewModel(),
                    recurringId = recurringId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Templates.route) {
                TemplatesScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
