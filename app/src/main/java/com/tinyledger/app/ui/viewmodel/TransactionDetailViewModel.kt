package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val accountName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val transactionId: Long = savedStateHandle["transactionId"] ?: 0L

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        if (transactionId <= 0) {
            _uiState.value = TransactionDetailUiState(
                isLoading = false,
                error = "无效的交易ID"
            )
            return
        }
        viewModelScope.launch {
            try {
                val transaction = transactionRepository.getTransactionById(transactionId)
                val account = transaction?.accountId?.let { accountRepository.getAccountById(it) }
                val accountDisplayName = account?.let {
                    if (!it.cardNumber.isNullOrBlank() && it.cardNumber.length >= 4)
                        "${it.name} (${it.cardNumber.takeLast(4)})"
                    else it.name
                }
                _uiState.value = TransactionDetailUiState(
                    transaction = transaction,
                    accountName = accountDisplayName,
                    isLoading = false,
                    error = if (transaction == null) "交易记录不存在" else null
                )
            } catch (e: Exception) {
                _uiState.value = TransactionDetailUiState(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transactionId)
            } catch (e: Exception) {
                // 删除失败时忽略，因为用户已经确认删除
            }
        }
    }
}
