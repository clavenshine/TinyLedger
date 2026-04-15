package com.tinyledger.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tinyledger.app.ui.screens.accounts.AccountsScreen
import com.tinyledger.app.ui.screens.autoaccounting.AutoAccountingScreen
import com.tinyledger.app.ui.screens.automation.AutoImportScreen
import com.tinyledger.app.ui.screens.automation.ImportSource
import com.tinyledger.app.ui.screens.bills.BillsScreen
import com.tinyledger.app.ui.screens.bills.SearchScreen
import com.tinyledger.app.ui.screens.budget.BudgetScreen
import com.tinyledger.app.ui.screens.budget.ScreenshotAccountingScreen
import com.tinyledger.app.ui.screens.help.HelpScreen
import com.tinyledger.app.ui.screens.home.AddTransactionScreen
import com.tinyledger.app.ui.screens.home.HomeScreen
import com.tinyledger.app.ui.screens.home.PendingTransactionEditScreen
import com.tinyledger.app.ui.screens.profile.ProfileScreen
import com.tinyledger.app.ui.screens.settings.ImportType
import com.tinyledger.app.ui.screens.settings.SettingsScreen
import com.tinyledger.app.ui.screens.statistics.StatisticsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onEditTransaction = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                },
                onNavigateToAccounts = { tabIndex ->
                    navController.navigate(Screen.Accounts.createRoute(tabIndex))
                },
                onNavigateToAutoAccounting = {
                    navController.navigate(Screen.AutoAccounting.route)
                }
            )
        }

        composable(Screen.Bills.route) {
            BillsScreen(
                onEditTransaction = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditTransaction = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id)) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAutoImport = { importType ->
                    val route = when (importType) {
                        ImportType.SMS -> Screen.AutoImport.createRoute("sms")
                        ImportType.WECHAT -> Screen.AutoImport.createRoute("wechat")
                        ImportType.ALIPAY -> Screen.AutoImport.createRoute("alipay")
                    }
                    navController.navigate(route)
                },
                onNavigateToBudget = {
                    navController.navigate(Screen.Budget.route)
                },
                onNavigateToScreenshotAccounting = {
                    navController.navigate(Screen.ScreenshotAccounting.route)
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.createRoute(0))
                },
                onNavigateToHelp = {
                    navController.navigate(Screen.Help.route)
                },
                onNavigateToAutoAccounting = {
                    navController.navigate(Screen.AutoAccounting.route)
                }
            )
        }

        composable(
            route = Screen.Accounts.route,
            arguments = listOf(navArgument("tabIndex") { type = NavType.IntType; defaultValue = 0 })
        ) { backStackEntry ->
            val tabIndex = backStackEntry.arguments?.getInt("tabIndex") ?: 0
            AccountsScreen(
                initialTab = tabIndex,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRepay = { creditAccount ->
                    navController.navigate(Screen.CreditRepay.createRoute(creditAccount.id))
                }
            )
        }

        composable(
            route = Screen.CreditRepay.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            // Navigate to AddTransactionScreen and trigger credit repayment mode
            AddTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                initialCreditAccountId = if (accountId > 0) accountId else null
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAutoImport = { importType ->
                    val route = when (importType) {
                        ImportType.SMS -> Screen.AutoImport.createRoute("sms")
                        ImportType.WECHAT -> Screen.AutoImport.createRoute("wechat")
                        ImportType.ALIPAY -> Screen.AutoImport.createRoute("alipay")
                    }
                    navController.navigate(route)
                },
                onNavigateToAutoAccounting = {
                    navController.navigate(Screen.AutoAccounting.route)
                }
            )
        }

        composable(
            route = Screen.AutoImport.route,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "sms"
            val importSource = when (source) {
                "wechat" -> ImportSource.WECHAT
                "alipay" -> ImportSource.ALIPAY
                else -> ImportSource.SMS
            }

            AutoImportScreen(
                importSource = importSource,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onImportComplete = {
                    navController.popBackStack(Screen.Profile.route, inclusive = false)
                }
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ScreenshotAccounting.route) {
            ScreenshotAccountingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Help.route) {
            HelpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AutoAccounting.route) {
            AutoAccountingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.createRoute(0))
                }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            AddTransactionScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.createRoute(0))
                }
            )
        }

        composable(
            route = Screen.EditPendingTransaction.route,
            arguments = listOf(
                navArgument("pendingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val pendingId = backStackEntry.arguments?.getLong("pendingId") ?: 0L
            PendingTransactionEditScreen(
                pendingId = pendingId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.createRoute(0))
                }
            )
        }
    }
}
