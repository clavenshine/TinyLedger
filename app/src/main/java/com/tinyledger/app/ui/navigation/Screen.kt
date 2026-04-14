package com.tinyledger.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Bills : Screen("bills")
    data object Statistics : Screen("statistics")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object Accounts : Screen("accounts/{tabIndex}") {
        fun createRoute(tabIndex: Int = 0) = "accounts/$tabIndex"
    }
    data object AddTransaction : Screen("add_transaction")
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
    data object EditPendingTransaction : Screen("edit_pending_transaction/{pendingId}") {
        fun createRoute(pendingId: Long) = "edit_pending_transaction/$pendingId"
    }
}
