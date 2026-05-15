package com.moneymanager.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.moneymanager.app.ui.auth.AppLockScreen
import com.moneymanager.app.ui.accounts.AccountsScreen
import com.moneymanager.app.ui.accounts.AccountsViewModel
import com.moneymanager.app.ui.addtransaction.AddTransactionScreen
import com.moneymanager.app.ui.borrowlend.BorrowLendScreen
import com.moneymanager.app.ui.borrowlend.BorrowLendViewModel
import com.moneymanager.app.ui.budgets.BudgetsScreen
import com.moneymanager.app.ui.budgets.BudgetsViewModel
import com.moneymanager.app.ui.categories.CategoriesScreen
import com.moneymanager.app.ui.categories.CategoriesViewModel
import com.moneymanager.app.ui.goals.GoalsScreen
import com.moneymanager.app.ui.goals.GoalsViewModel
import com.moneymanager.app.ui.peerlist.PeerListScreen
import com.moneymanager.app.ui.peerlist.PeerListViewModel
import com.moneymanager.app.ui.recurring.RecurringFormScreen
import com.moneymanager.app.ui.recurring.RecurringListScreen
import com.moneymanager.app.ui.recurring.RecurringViewModel
import com.moneymanager.app.ui.settings.SettingsScreen
import com.moneymanager.app.ui.settings.SettingsViewModel
import com.moneymanager.app.ui.summary.SummaryScreen
import com.moneymanager.app.ui.tags.TagsScreen
import com.moneymanager.app.ui.tags.TagsViewModel
import com.moneymanager.app.ui.transactions.TransactionsScreen
import com.moneymanager.app.ui.transactions.TransactionsViewModel
import com.moneymanager.app.ui.transfer.TransferScreen
import com.moneymanager.app.ui.transfer.TransferViewModel
import com.moneymanager.app.ui.util.AppLockManager
import com.moneymanager.app.ui.util.AppLockState
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.security.BiometricAuthManager
import com.moneymanager.data.security.SecurityManager
import androidx.compose.material.icons.rounded.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    data object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalance)
    data object Transactions : Screen(
        route = "transactions?type={type}&accountId={accountId}&startDate={startDate}&endDate={endDate}&goalId={goalId}&categoryId={categoryId}&peerId={peerId}",
        title = "Transactions",
        icon = Icons.Rounded.ReceiptLong
    ) {
        fun createRoute(
            type: String? = null,
            accountId: Long? = null,
            startDate: Long? = null,
            endDate: Long? = null,
            goalId: Long? = null,
            categoryId: Long? = null,
            peerId: Long? = null
        ): String {
            val builder = StringBuilder("transactions?")
            type?.let { builder.append("type=$it&") }
            accountId?.let { builder.append("accountId=$it&") }
            startDate?.let { builder.append("startDate=$it&") }
            endDate?.let { builder.append("endDate=$it&") }
            goalId?.let { builder.append("goalId=$it&") }
            categoryId?.let { builder.append("categoryId=$it&") }
            peerId?.let { builder.append("peerId=$it&") }
            return builder.toString().removeSuffix("&").removeSuffix("?")
        }
    }
    data object Budgets : Screen("budgets", "Budgets", Icons.Default.PieChart)
    data object Goals : Screen("goals", "Goals", Icons.Default.Flag)
    data object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
    data object Summary : Screen("summary", "Summary", Icons.Rounded.SpaceDashboard)
    data object Tags : Screen("tags", "Tags", null)
    data object Categories : Screen("categories", "Categories", null)
    data object Transfer : Screen("transfer", "Transfer", Icons.Default.SwapHoriz)
    data object Recurring : Screen("recurring", "Recurring", Icons.Default.Repeat)
    data object RecurringForm : Screen("recurring_form?recurringId={recurringId}", "Recurring Form", null)
    data object Peers : Screen("peers", "Peers", Icons.Default.People)
    data object BorrowLend : Screen("borrow_lend", "Borrow/Lend", null)
    data object AddTransaction : Screen("add_transaction?type={type}", "Add Transaction", null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyManagerNavHost(
    preferencesManager: PreferencesManager,
    securityManager: SecurityManager,
    biometricAuthManager: BiometricAuthManager,
    appLockManager: AppLockManager
) {
    val navController = rememberNavController()
    val lockState by appLockManager.lockState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    if (lockState is AppLockState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (lockState is AppLockState.Locked && activity != null) {
        AppLockScreen(
            preferencesManager = preferencesManager,
            securityManager = securityManager,
            biometricAuthManager = biometricAuthManager,
            activity = activity,
            onUnlocked = { appLockManager.unlock() }
        )
    } else {
        val bottomNavScreens = listOf(
            Screen.Summary,
            Screen.Transactions,
            Screen.Settings
        )

        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(72.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavScreens.forEach { screen ->

                        val route =
                            if (screen is Screen.Transactions) {
                                screen.createRoute()
                            } else {
                                screen.route
                            }

                        screen.icon?.let { icon ->

                            NavigationBarItem(
                                selected = currentDestination?.hierarchy?.any {
                                    it.route == screen.route
                                } == true,

                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },

                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(20.dp).padding(top = 4.dp)
                                    )
                                },

                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },

                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),

                                alwaysShowLabel = true
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Summary.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Accounts.route) {
                    AccountsScreen(
                        viewModel = hiltViewModel(),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    Screen.Transactions.route,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "moneymanager://transactions?type={type}&accountId={accountId}&startDate={startDate}&endDate={endDate}&goalId={goalId}&peerId={peerId}" }
                    )
                ) { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type")
                    val accountId = backStackEntry.arguments?.getString("accountId")?.toLongOrNull()
                    val startDate = backStackEntry.arguments?.getString("startDate")?.toLongOrNull()
                    val endDate = backStackEntry.arguments?.getString("endDate")?.toLongOrNull()
                    val goalId = backStackEntry.arguments?.getString("goalId")?.toLongOrNull()
                    val categoryId = backStackEntry.arguments?.getString("categoryId")?.toLongOrNull()
                    val peerId = backStackEntry.arguments?.getString("peerId")?.toLongOrNull()

                    TransactionsScreen(
                        viewModel = hiltViewModel(),
                        initialType = type,
                        initialAccountId = accountId,
                        initialStartDate = startDate,
                        initialEndDate = endDate,
                        initialGoalId = goalId,
                        initialCategoryId = categoryId,
                        initialPeerId = peerId
                    )
                }
                composable(Screen.Budgets.route) {
                    BudgetsScreen(
                        viewModel = hiltViewModel(),
                        onNavigateBack = { navController.popBackStack() }
                    )
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
                        onNavigateToPeers = { navController.navigate(Screen.Peers.route) },
                        onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                        onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                        onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) }
                    )
                }
                composable(Screen.Summary.route) {
                    SummaryScreen(
                        viewModel = hiltViewModel(),
                        onNavigateToAddTransaction = {
                            navController.navigate(Screen.Transactions.createRoute())
                        },
                        onNavigateToTransactions = { type, accountId, startDate, endDate, goalId, categoryId, peerId ->
                            navController.navigate(
                                Screen.Transactions.createRoute(type, accountId, startDate, endDate, goalId, categoryId, peerId)
                            )
                        },
                        onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                        onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) }
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
                composable(Screen.Peers.route) {
                    PeerListScreen(
                        viewModel = hiltViewModel<PeerListViewModel>(),
                        onNavigateBack = { navController.popBackStack() },
                        onPeerClick = { peerId: Long ->
                            // TODO: Implement peer details or transaction history
                        }
                    )
                }
                composable(
                    Screen.AddTransaction.route,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "moneymanager://add_transaction?type={type}" }
                    )
                ) { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type")
                    AddTransactionScreen(
                        type = type,
                        onDismiss = { navController.popBackStack() }
                    )
                }

            }
        }
    }
}
