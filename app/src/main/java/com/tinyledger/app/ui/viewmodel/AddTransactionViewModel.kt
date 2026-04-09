package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.Account
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.PreferencesRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import com.tinyledger.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTransactionUiState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = Category.defaultExpenseCategories,
    val selectedCategory: Category? = null,
    val amount: String = "",
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val currencySymbol: String = "¥",
    // 账户相关
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private var editingTransactionId: Long? = null

    init {
        loadSettings()
        loadAccounts()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.getSettings().collect { settings ->
                _uiState.update { it.copy(currencySymbol = settings.currencySymbol) }
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun refreshAccounts() {
        loadAccounts()
    }

    fun loadTransaction(transactionId: Long) {
        if (transactionId <= 0) return

        editingTransactionId = transactionId
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId)
            transaction?.let { tx ->
                val categories = Category.getCategoriesByType(tx.type)
                // 如果有accountId，找到对应的账户
                val selectedAccount = tx.accountId?.let { accountId ->
                    _uiState.value.accounts.find { it.id == accountId }
                }
                _uiState.update { state ->
                    state.copy(
                        transactionType = tx.type,
                        categories = categories,
                        selectedCategory = tx.category,
                        amount = tx.amount.toString(),
                        note = tx.note ?: "",
                        date = tx.date,
                        isEditing = true,
                        selectedAccount = selectedAccount
                    )
                }
            }
        }
    }

    fun setTransactionType(type: TransactionType) {
        val categories = Category.getCategoriesByType(type)
        _uiState.update {
            it.copy(
                transactionType = type,
                categories = categories,
                selectedCategory = null
            )
        }
    }

    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectAccount(account: Account?) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun addCategory(name: String) {
        val type = _uiState.value.transactionType
        val newCategory = Category.addCustomCategory(name, type)
        
        // 重新加载分类列表并选中新增的分类
        val categories = Category.getCategoriesByType(type)
        _uiState.update { 
            it.copy(
                categories = categories,
                selectedCategory = newCategory
            )
        }
    }

    fun setAmount(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun setDate(date: Long) {
        _uiState.update { it.copy(date = date) }
    }

    fun saveTransaction() {
        val state = _uiState.value

        // Validation
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "请输入有效金额") }
            return
        }

        if (state.selectedCategory == null) {
            _uiState.update { it.copy(errorMessage = "请选择分类") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    id = editingTransactionId ?: 0,
                    type = state.transactionType,
                    category = state.selectedCategory,
                    amount = amount,
                    note = state.note.ifBlank { null },
                    date = state.date,
                    accountId = state.selectedAccount?.id
                )

                if (state.isEditing && editingTransactionId != null) {
                    transactionRepository.updateTransaction(transaction)
                } else {
                    transactionRepository.insertTransaction(transaction)
                }

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
