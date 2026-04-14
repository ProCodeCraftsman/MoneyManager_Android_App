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
import com.moneymanager.app.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalance)
    data object Transactions : Screen("transactions", "Transactions", Icons.Default.Receipt)
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
        Screen.Accounts,
        Screen.Transactions,
        Screen.Budgets,
        Screen.Reports,
        Screen.Recurring,
        Screen.Goals,
        Screen.Transfer,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavScreens.forEach { screen ->
                    screen.icon?.let { icon ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
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
                DashboardScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Accounts.route) {
                AccountsScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = hiltViewModel(),
                    accountsViewModel = hiltViewModel()
                )
            }
            composable(Screen.Budgets.route) {
                BudgetsScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Goals.route) {
                GoalsScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Tags.route) {
                TagsScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Categories.route) {
                CategoriesScreen(viewModel = hiltViewModel())
            }
            composable(Screen.Transfer.route) {
                TransferScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Recurring.route) {
                RecurringListScreen(viewModel = hiltViewModel())
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
                TemplatesScreen(viewModel = hiltViewModel())
            }
        }
    }
}