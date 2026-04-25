package com.moneymanager.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.moneymanager.app.ui.auth.LockScreen
import com.moneymanager.app.ui.auth.PinLockScreen
import com.moneymanager.app.ui.screens.AccountsScreen
import com.moneymanager.app.ui.screens.AccountsViewModel
import com.moneymanager.app.ui.screens.BudgetsScreen
import com.moneymanager.app.ui.screens.BorrowLendScreen
import com.moneymanager.app.ui.screens.BudgetsViewModel
import com.moneymanager.app.ui.screens.CategoriesScreen
import com.moneymanager.app.ui.screens.CategoriesViewModel
import com.moneymanager.app.ui.screens.DashboardScreen
import com.moneymanager.app.ui.screens.DashboardViewModel
import com.moneymanager.app.ui.screens.GoalsScreen
import com.moneymanager.app.ui.screens.GoalsViewModel
import com.moneymanager.app.ui.screens.PeerListScreen
import com.moneymanager.app.ui.screens.PeerListViewModel
import com.moneymanager.app.ui.screens.RecurringFormScreen
import com.moneymanager.app.ui.screens.RecurringListScreen
import com.moneymanager.app.ui.screens.RecurringViewModel
import com.moneymanager.app.ui.screens.ReportsScreen
import com.moneymanager.app.ui.screens.ReportsViewModel
import com.moneymanager.app.ui.screens.SettingsScreen
import com.moneymanager.app.ui.screens.SettingsViewModel
import com.moneymanager.app.ui.screens.TagsScreen
import com.moneymanager.app.ui.screens.TagsViewModel
import com.moneymanager.app.ui.screens.TemplatesScreen
import com.moneymanager.app.ui.screens.TemplatesViewModel
import com.moneymanager.app.ui.screens.TransactionsScreen
import com.moneymanager.app.ui.screens.TransactionsViewModel
import com.moneymanager.app.ui.screens.TransferScreen
import com.moneymanager.app.ui.screens.TransferViewModel
import com.moneymanager.app.ui.util.AppLockManager
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.security.BiometricAuthManager
import com.moneymanager.data.security.SecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalance)
    data object Transactions : Screen(
        route = "transactions?type={type}&accountId={accountId}&startDate={startDate}&endDate={endDate}&goalId={goalId}&categoryId={categoryId}&peerId={peerId}",
        title = "Transactions",
        icon = Icons.Default.Receipt
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
    data object Reports : Screen("reports", "Reports", Icons.Default.BarChart)
    data object Goals : Screen("goals", "Goals", Icons.Default.Flag)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Tags : Screen("tags", "Tags", null)
    data object Categories : Screen("categories", "Categories", null)
    data object Transfer : Screen("transfer", "Transfer", Icons.Default.SwapHoriz)
    data object Recurring : Screen("recurring", "Recurring", Icons.Default.Repeat)
    data object RecurringForm : Screen("recurring_form?recurringId={recurringId}", "Recurring Form", null)
    data object Templates : Screen("templates", "Templates", Icons.Default.Description)
    data object Peers : Screen("peers", "Peers", Icons.Default.People)
    data object BorrowLend : Screen("borrow_lend", "Borrow/Lend", null)
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
    val isLocked by appLockManager.isLocked.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    if (isLocked && activity != null) {
        var showSetup by remember { mutableStateOf(false) }

        if (showSetup) {
            var enteredPin by remember { mutableStateOf("") }
            var isConfirming by remember { mutableStateOf(false) }
            var firstPin by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isConfirming) "Confirm PIN" else "Create PIN",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isConfirming) "Re-enter your PIN"
                           else "Create a 4-digit PIN to secure your app",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 4) enteredPin = it },
                    placeholder = { Text("----") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        appLockManager.unlock()
                    }) {
                        Text("Skip")
                    }
                    Button(
                        onClick = {
                            if (enteredPin.length != 4) {
                                error = "PIN must be 4 digits"
                            } else if (!isConfirming) {
                                firstPin = enteredPin
                                enteredPin = ""
                                isConfirming = true
                                error = null
                            } else {
                                if (enteredPin == firstPin) {
                                    // Set up PIN and unlock
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val (hash, salt) = securityManager.hashPin(enteredPin)
                                        preferencesManager.setPinHash(hash)
                                        preferencesManager.setPinSalt(salt)
                                        preferencesManager.setPinEnabled(true)
                                        appLockManager.unlock()
                                    }
                                } else {
                                    error = "PINs don't match. Try again."
                                    isConfirming = false
                                    enteredPin = ""
                                    firstPin = ""
                                }
                            }
                        }
                    ) {
                        Text(if (isConfirming) "Confirm" else "Next")
                    }
                }
            }
        } else {
            LockScreen(
                preferencesManager = preferencesManager,
                securityManager = securityManager,
                biometricAuthManager = biometricAuthManager,
                activity = activity,
                onUnlocked = {
                    appLockManager.unlock()
                },
                onSetupRequired = {
                    showSetup = true
                }
            )
        }
    } else {
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
                        onNavigateToTransactions = { type, accountId, startDate, endDate, goalId, categoryId, peerId ->
                            navController.navigate(
                                Screen.Transactions.createRoute(type, accountId, startDate, endDate, goalId, categoryId, peerId)
                            )
                        },
                        onNavigateToBorrowLend = { navController.navigate(Screen.BorrowLend.route) }
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
                        onNavigateToPeers = { navController.navigate(Screen.Peers.route) },
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
                composable(Screen.Peers.route) {
                    PeerListScreen(
                        viewModel = hiltViewModel(),
                        onNavigateBack = { navController.popBackStack() },
                        onPeerClick = { peerId ->
                            // TODO: Implement peer details or transaction history
                        }
                    )
                }
                composable(Screen.BorrowLend.route) {
                    BorrowLendScreen(
                        viewModel = hiltViewModel(),
                        onNavigateBack = { navController.popBackStack() },
                        onSuccess = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
