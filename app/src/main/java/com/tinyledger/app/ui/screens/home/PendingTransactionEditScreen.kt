package com.tinyledger.app.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinyledger.app.data.local.dao.PendingTransactionDao
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.PendingTransactionRepository
import com.tinyledger.app.ui.viewmodel.AddTransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 包装界面：从待确认交易加载数据后，打开标准记账编辑界面
 * 保存成功后自动删除待确认记录
 */
@Composable
fun PendingTransactionEditScreen(
    pendingId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    pendingTransactionDao: PendingTransactionDao = hiltViewModel<PendingTransactionDaoProvider>().dao
) {
    var loaded by remember { mutableStateOf(false) }

    val viewModel: AddTransactionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(pendingId) {
        if (!loaded && pendingId > 0) {
            withContext(Dispatchers.IO) {
                val entity = pendingTransactionDao.getById(pendingId)
                entity?.let { e ->
                    val transactionType = TransactionType.fromInt(e.type)
                    val category = Category.fromId(e.category, transactionType)
                    viewModel.setTransactionType(transactionType)
                    viewModel.setAmount(e.amount.toString())
                    viewModel.setNote(e.note ?: "")
                    viewModel.setDate(e.date)
                    viewModel.selectCategory(category)
                }
            }
            loaded = true
        }
    }

    // When save is successful, also delete the pending transaction
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess && loaded) {
            withContext(Dispatchers.IO) {
                pendingTransactionDao.deleteById(pendingId)
            }
            onNavigateBack()
        }
    }

    AddTransactionScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToAccounts = onNavigateToAccounts,
        viewModel = viewModel
    )
}

/**
 * ViewModel provider to inject PendingTransactionDao into composable
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class PendingTransactionDaoProvider @Inject constructor(
    val dao: PendingTransactionDao
) : androidx.lifecycle.ViewModel()
