package com.tinyledger.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Bills : Screen("bills")
    data object Statistics : Screen("statistics")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object ThemeColor : Screen("theme_color")
    data object Accounts : Screen("accounts/{tabIndex}") {
        fun createRoute(tabIndex: Int = 0) = "accounts/$tabIndex"
    }
    data object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: Long) = "account_detail/$accountId"
    }
    data object AddTransaction : Screen("add_transaction") {
        fun createRoute(selectedAccountId: Long? = null, transactionType: String? = null, fromAccountId: Long? = null): String {
            return buildString {
                append("add_transaction")
                if (selectedAccountId != null || transactionType != null || fromAccountId != null) {
                    append("?")
                    if (selectedAccountId != null) append("selectedAccountId=$selectedAccountId")
                    if (transactionType != null) {
                        if (selectedAccountId != null) append("&")
                        append("transactionType=$transactionType")
                    }
                    if (fromAccountId != null) {
                        if (selectedAccountId != null || transactionType != null) append("&")
                        append("fromAccountId=$fromAccountId")
                    }
                }
            }
        }
    }
    data object CreditRepay : Screen("credit_repay/{accountId}") {
        fun createRoute(accountId: Long) = "credit_repay/$accountId"
    }
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    data object AutoImport : Screen("auto_import/{source}") {
        fun createRoute(source: String) = "auto_import/$source"
    }
    data object Budget : Screen("budget")
    data object ScreenshotAccounting : Screen("screenshot_accounting")
    data object Search : Screen("search")
    data object Help : Screen("help")
    data object AutoAccounting : Screen("auto_accounting")
    data object CreditAccounts : Screen("credit_accounts")
    data object EditPendingTransaction : Screen("edit_pending_transaction/{pendingId}") {
        fun createRoute(pendingId: Long) = "edit_pending_transaction/$pendingId"
    }
    data object CategoryManage : Screen("category_manage/{transactionType}") {
        fun createRoute(transactionType: String = "EXPENSE") = "category_manage/$transactionType"
    }
    data object AddCategory : Screen("add_category/{transactionType}") {
        fun createRoute(transactionType: String = "EXPENSE") = "add_category/$transactionType"
    }
    data object AddAccount : Screen("add_account/{accountType}") {
        fun createRoute(accountType: Int = 0) = "add_account/$accountType"
    }
    data object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }
}
