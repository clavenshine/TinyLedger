package com.tinyledger.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.screens.accounts.AccountDetailScreen
import com.tinyledger.app.ui.screens.accounts.AccountsScreen
import com.tinyledger.app.ui.screens.accounts.AddAccountScreen
import com.tinyledger.app.ui.screens.autoaccounting.AutoAccountingScreen
import com.tinyledger.app.ui.screens.automation.AutoImportScreen
import com.tinyledger.app.ui.screens.automation.ImportSource
import com.tinyledger.app.ui.screens.bills.BillsScreen
import com.tinyledger.app.ui.screens.bills.SearchScreen
import com.tinyledger.app.ui.screens.credit.CreditAccountsScreen
import com.tinyledger.app.ui.screens.credit.CreditAccountsScreen
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
import com.tinyledger.app.ui.screens.category.CategoryManageScreen
import com.tinyledger.app.ui.screens.category.AddCategoryScreen
import com.tinyledger.app.ui.screens.category.EditCategoryScreen
import com.tinyledger.app.ui.screens.category.AddSubCategoryScreen
import com.tinyledger.app.domain.model.Category
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.ui.viewmodel.AddTransactionViewModel
import com.tinyledger.app.ui.viewmodel.HomeViewModel
import com.tinyledger.app.ui.viewmodel.BillsViewModel
import com.tinyledger.app.ui.viewmodel.CategoryViewModel
import com.tinyledger.app.ui.viewmodel.ReimbursementViewModel
import com.tinyledger.app.ui.screens.detail.TransactionDetailScreen
import com.tinyledger.app.ui.screens.backup.BackupScreen
import com.tinyledger.app.ui.screens.backup.BackupExportScreen
import com.tinyledger.app.ui.screens.backup.BackupImportScreen
import com.tinyledger.app.ui.screens.reimbursement.ReimbursementDetailScreen
import com.tinyledger.app.ui.screens.reimbursement.ReimbursementScreen

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
                onViewTransactionDetail = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                },
                onNavigateToAccounts = { tabIndex ->
                    navController.navigate(Screen.Accounts.createRoute(tabIndex))
                },
                onNavigateToAutoAccounting = {
                    navController.navigate(Screen.AutoAccounting.route)
                },
                onNavigateToCreditAccounts = {
                    navController.navigate(Screen.CreditAccounts.route)
                },
                onNavigateToReimbursement = {
                    navController.navigate(Screen.Reimbursement.route)
                },
                onNavigateToReimbursementDetail = { id ->
                    navController.navigate(Screen.ReimbursementDetail.createRoute(id))
                }
            )
        }

        composable(Screen.Bills.route) {
            BillsScreen(
                onEditTransaction = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                },
                onViewTransactionDetail = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                },
                onNavigateToReimbursementDetail = { id ->
                    navController.navigate(Screen.ReimbursementDetail.createRoute(id))
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
                },
                onNavigateToDarkModeSettings = {
                    // 使用Intent打开独立的Activity
                    val context = navController.context
                    val intent = android.content.Intent(context, com.tinyledger.app.ui.screens.settings.DarkModeSettingsActivity::class.java)
                    context.startActivity(intent)
                },
                onNavigateToThemeColor = {
                    navController.navigate(Screen.ThemeColor.route)
                },
                onNavigateToBackup = {
                    navController.navigate(Screen.Backup.route)
                },
                onNavigateToReimbursement = {
                    navController.navigate(Screen.Reimbursement.route)
                },
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
                onNavigateToAccountDetail = { accountId ->
                    navController.navigate(Screen.AccountDetail.createRoute(accountId))
                },
                onNavigateToRepay = { creditAccount ->
                    navController.navigate(Screen.CreditRepay.createRoute(creditAccount.id))
                },
                onNavigateToAddAccount = { tabIndex ->
                    navController.navigate(Screen.AddAccount.createRoute(tabIndex))
                }
            )
        }

        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            AccountDetailScreen(
                accountId = accountId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddTransaction = { selectedAccountId, transactionType, fromAccountId ->
                    // 跳转到添加记账页面，传递参数
                    val route = Screen.AddTransaction.createRoute(
                        selectedAccountId = selectedAccountId,
                        transactionType = transactionType?.name,
                        fromAccountId = fromAccountId
                    )
                    navController.navigate(route)
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
                initialCreditAccountId = if (accountId > 0) accountId else null,
                onNavigateToCategoryManage = { type ->
                    navController.navigate(Screen.CategoryManage.createRoute(type.name))
                }
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
                },
                onNavigateToThemeColor = {
                    navController.navigate(Screen.ThemeColor.route)
                }
            )
        }

        composable(Screen.ThemeColor.route) {
            com.tinyledger.app.ui.screens.settings.ThemeColorScreen(
                onNavigateBack = {
                    navController.popBackStack()
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

        composable(Screen.CreditAccounts.route) {
            CreditAccountsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.AddAccount.route,
            arguments = listOf(navArgument("accountType") { type = NavType.IntType; defaultValue = 0 })
        ) { backStackEntry ->
            val accountType = backStackEntry.arguments?.getInt("accountType") ?: 0
            AddAccountScreen(
                initialAccountType = accountType,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "add_transaction?selectedAccountId={selectedAccountId}&transactionType={transactionType}&fromAccountId={fromAccountId}",
            arguments = listOf(
                navArgument("selectedAccountId") { type = NavType.StringType; defaultValue = ""; nullable = true },
                navArgument("transactionType") { type = NavType.StringType; defaultValue = ""; nullable = true },
                navArgument("fromAccountId") { type = NavType.StringType; defaultValue = ""; nullable = true }
            )
        ) { backStackEntry ->
            val selectedAccountIdStr = backStackEntry.arguments?.getString("selectedAccountId")
            val selectedAccountId = selectedAccountIdStr?.toLongOrNull()?.takeIf { it > 0 }
            val transactionTypeStr = backStackEntry.arguments?.getString("transactionType")
            val fromAccountIdStr = backStackEntry.arguments?.getString("fromAccountId")
            val fromAccountId = fromAccountIdStr?.toLongOrNull()?.takeIf { it > 0 }
            
            val transactionType = transactionTypeStr?.let {
                try {
                    TransactionType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            
            AddTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.createRoute(0))
                },
                initialSelectedAccountId = selectedAccountId,
                initialTransactionType = transactionType,
                initialFromAccountId = fromAccountId,
                onNavigateToCategoryManage = { type ->
                    navController.navigate(Screen.CategoryManage.createRoute(type.name))
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
                },
                onNavigateToCategoryManage = { type ->
                    navController.navigate(Screen.CategoryManage.createRoute(type.name))
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

        composable(
            route = Screen.CategoryManage.route,
            arguments = listOf(
                navArgument("transactionType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val transactionTypeStr = backStackEntry.arguments?.getString("transactionType") ?: "EXPENSE"
            val transactionType = try {
                com.tinyledger.app.domain.model.TransactionType.valueOf(transactionTypeStr)
            } catch (e: Exception) {
                com.tinyledger.app.domain.model.TransactionType.EXPENSE
            }
            com.tinyledger.app.ui.screens.category.CategoryManageScreen(
                transactionType = transactionType,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddCategory = { type ->
                    navController.navigate(Screen.AddCategory.createRoute(type.name))
                },
                onNavigateToEditCategory = { categoryId, type ->
                    navController.navigate(Screen.EditCategory.createRoute(categoryId, type.name))
                },
                onNavigateToAddSubCategory = { parentId, type ->
                    navController.navigate(Screen.AddSubCategory.createRoute(parentId, type.name))
                }
            )
        }

        composable(
            route = Screen.AddCategory.route,
            arguments = listOf(
                navArgument("transactionType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val transactionTypeStr = backStackEntry.arguments?.getString("transactionType") ?: "EXPENSE"
            val transactionType = try {
                com.tinyledger.app.domain.model.TransactionType.valueOf(transactionTypeStr)
            } catch (e: Exception) {
                com.tinyledger.app.domain.model.TransactionType.EXPENSE
            }
            val categoryViewModel: CategoryViewModel = hiltViewModel()
            AddCategoryScreen(
                transactionType = transactionType,
                onNavigateBack = { navController.popBackStack() },
                onSaveCategory = { category ->
                    categoryViewModel.saveCategoryToDatabase(category)
                }
            )
        }

        composable(
            route = Screen.EditCategory.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("transactionType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val addTxnViewModel: AddTransactionViewModel = hiltViewModel()
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val transactionTypeStr = backStackEntry.arguments?.getString("transactionType") ?: "EXPENSE"
            val transactionType = try {
                com.tinyledger.app.domain.model.TransactionType.valueOf(transactionTypeStr)
            } catch (e: Exception) {
                com.tinyledger.app.domain.model.TransactionType.EXPENSE
            }
            val categories = Category.getCategoriesByType(transactionType)
            val category = categories.find { it.id == categoryId }
            if (category != null) {
                EditCategoryScreen(
                    category = category,
                    transactionType = transactionType,
                    onNavigateBack = { navController.popBackStack() },
                    onCategoryUpdated = { updatedCategory ->
                        // 分类更新后，ViewModel 会自动刷新，CategoryManageScreen 会重新获取数据
                    },
                    viewModel = addTxnViewModel
                )
            }
        }

        composable(
            route = Screen.AddSubCategory.route,
            arguments = listOf(
                navArgument("parentId") { type = NavType.StringType },
                navArgument("transactionType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val addTxnViewModel: AddTransactionViewModel = hiltViewModel()
            val categoryViewModel: CategoryViewModel = hiltViewModel()
            val scope = rememberCoroutineScope()
            val parentId = backStackEntry.arguments?.getString("parentId") ?: ""
            val transactionTypeStr = backStackEntry.arguments?.getString("transactionType") ?: "EXPENSE"
            val transactionType = try {
                com.tinyledger.app.domain.model.TransactionType.valueOf(transactionTypeStr)
            } catch (e: Exception) {
                com.tinyledger.app.domain.model.TransactionType.EXPENSE
            }
            val categories = Category.getCategoriesByType(transactionType)
            val parentCategory = categories.find { it.id == parentId }
            if (parentCategory != null) {
                AddSubCategoryScreen(
                    parentCategory = parentCategory,
                    transactionType = transactionType,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSubCategory = { subCategory ->
                        categoryViewModel.saveCategoryToDatabase(subCategory)
                    },
                    onAutoMatchTransactions = { newSubCategory ->
                        var matchedCount = 0
                        kotlinx.coroutines.runBlocking {
                            val allSubCategories = Category.getSubCategories(parentCategory.id, transactionType)
                            val firstSubId = allSubCategories.firstOrNull()?.id
                            matchedCount = addTxnViewModel.autoMatchTransactionsToSubCategory(
                                parentCategoryId = parentCategory.id,
                                newSubCategoryId = newSubCategory.id,
                                subCategoryName = newSubCategory.name,
                                firstSubCategoryId = firstSubId
                            )
                        }
                        matchedCount
                    }
                )
            }
        }

        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            )
        ) {
            val detailViewModel: com.tinyledger.app.ui.viewmodel.TransactionDetailViewModel = hiltViewModel()
            TransactionDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditTransaction = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                },
                onDeleteTransaction = { _ ->
                    detailViewModel.deleteTransaction()
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExport = {
                    navController.navigate(Screen.BackupExport.route)
                },
                onNavigateToImport = {
                    navController.navigate(Screen.BackupImport.route)
                }
            )
        }

        composable(Screen.BackupExport.route) {
            BackupExportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onExportComplete = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.BackupImport.route) {
            BackupImportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onImportComplete = {
                    navController.popBackStack(Screen.Profile.route, inclusive = false)
                }
            )
        }

        composable(Screen.Reimbursement.route) {
            ReimbursementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { transactionId ->
                    navController.navigate(Screen.ReimbursementDetail.createRoute(transactionId))
                }
            )
        }

        composable(
            route = Screen.ReimbursementDetail.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            ReimbursementDetailScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
