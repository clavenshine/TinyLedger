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

enum class LendingSubType {
    BORROW_IN,   // 借入
    BORROW_OUT,  // 借出
    REPAY,       // 还款
    COLLECT      // 收款
}

// Credit repayment sub-type for transfer mode
enum class CreditRepaySubType {
    NORMAL,      // Normal transfer
    CREDIT_REPAY // Credit account repayment
}

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
    val selectedAccount: Account? = null,
    // 转账/借贷相关
    val selectedFromAccount: Account? = null,
    val selectedToAccount: Account? = null,
    val lendingSubType: LendingSubType = LendingSubType.BORROW_IN,
    // 信用还款相关
    val creditRepaySubType: CreditRepaySubType = CreditRepaySubType.NORMAL,
    val creditAccountToRepay: Account? = null // 要还款的信用账户
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
        loadCustomCategories()
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

    private fun loadCustomCategories() {
        viewModelScope.launch {
            val customCategories = preferencesRepository.getCustomCategories()
            Category.loadCustomCategories(customCategories)
            // 刷新当前分类列表
            val type = _uiState.value.transactionType
            _uiState.update { it.copy(categories = Category.getCategoriesByType(type)) }
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
                
                // 检查是否是双记录交易（转账/借贷）
                if (tx.type == TransactionType.TRANSFER || tx.type == TransactionType.LENDING) {
                    // 查找关联的另一笔交易
                    val relatedTransaction = tx.relatedTransactionId?.let {
                        transactionRepository.getTransactionById(it)
                    }
                    
                    // 如果当前交易没有relatedTransactionId，尝试通过DAO查找指向它的交易
                    val resolvedRelated = relatedTransaction ?: run {
                        // 通过relatedTransactionId反向查找
                        transactionRepository.getTransactionByRelatedId(transactionId)
                    }
                    
                    // 根据金额正负判断哪笔是from，哪笔是to
                    val (fromTx, toTx) = if (tx.amount < 0) {
                        Pair(tx, resolvedRelated)
                    } else {
                        Pair(resolvedRelated, tx)
                    }
                    
                    // 直接从数据库加载账户，避免时序问题（accounts列表可能还未加载）
                    val fromAccount = fromTx?.accountId?.let { accountId ->
                        _uiState.value.accounts.find { it.id == accountId }
                            ?: accountRepository.getAccountById(accountId)
                    }
                    val toAccount = toTx?.accountId?.let { accountId ->
                        _uiState.value.accounts.find { it.id == accountId }
                            ?: accountRepository.getAccountById(accountId)
                    }
                    
                    _uiState.update { state ->
                        state.copy(
                            transactionType = tx.type,
                            categories = categories,
                            selectedCategory = tx.category,
                            amount = kotlin.math.abs(tx.amount).toString(), // 显示绝对值
                            note = tx.note ?: "",
                            date = tx.date,
                            isEditing = true,
                            selectedFromAccount = fromAccount,
                            selectedToAccount = toAccount
                        )
                    }
                } else {
                    // 普通收支交易 - 直接从数据库加载账户
                    val selectedAccount = tx.accountId?.let { accountId ->
                        _uiState.value.accounts.find { it.id == accountId }
                            ?: accountRepository.getAccountById(accountId)
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
    }

    fun setTransactionType(type: TransactionType) {
        val categories = Category.getCategoriesByType(type)
        _uiState.update {
            it.copy(
                transactionType = type,
                categories = categories,
                selectedCategory = null,
                selectedFromAccount = null,
                selectedToAccount = null,
                lendingSubType = LendingSubType.BORROW_IN,
                creditRepaySubType = CreditRepaySubType.NORMAL,
                creditAccountToRepay = null
            )
        }
    }
    
    // Set credit repayment mode
    fun setCreditRepayMode(creditAccount: Account) {
        _uiState.update {
            it.copy(
                transactionType = TransactionType.TRANSFER,
                categories = Category.defaultTransferCategories,
                selectedCategory = Category("credit_repay", "信用还款", "credit_card_repay", TransactionType.TRANSFER),
                selectedFromAccount = null, // Will select cash account
                selectedToAccount = creditAccount, // Credit account to repay
                creditRepaySubType = CreditRepaySubType.CREDIT_REPAY,
                creditAccountToRepay = creditAccount,
                amount = kotlin.math.abs(creditAccount.currentBalance).toString().takeIf { creditAccount.currentBalance < 0 } ?: ""
            )
        }
    }
    
    // Set credit repayment mode by account ID (for navigation)
    fun setCreditRepayModeById(accountId: Long) {
        viewModelScope.launch {
            val account = accountRepository.getAccountById(accountId)
            if (account != null && account.attribute == com.tinyledger.app.domain.model.AccountAttribute.CREDIT) {
                setCreditRepayMode(account)
            }
        }
    }

    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectAccount(account: Account?) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun selectFromAccount(account: Account?) {
        _uiState.update { it.copy(selectedFromAccount = account) }
    }

    fun selectToAccount(account: Account?) {
        _uiState.update { it.copy(selectedToAccount = account) }
    }

    fun setLendingSubType(subType: LendingSubType) {
        _uiState.update { it.copy(lendingSubType = subType, selectedFromAccount = null, selectedToAccount = null) }
    }

    fun addCategory(name: String) {
        val type = _uiState.value.transactionType
        // Check for duplicate name
        val existingNames = _uiState.value.categories.map { it.name }
        if (name.trim() in existingNames) return
        
        val newCategory = Category.addCustomCategory(name, type)

        // 持久化保存自定义分类
        viewModelScope.launch {
            preferencesRepository.saveCustomCategory(newCategory)
        }

        // 重新加载分类列表并选中新增的分类
        val categories = Category.getCategoriesByType(type)
        _uiState.update {
            it.copy(
                categories = categories,
                selectedCategory = newCategory
            )
        }
    }

    fun deleteCategory(category: Category) {
        // Cannot delete default categories
        if (category.isDefault) return
        
        val removed = Category.removeCustomCategory(category)
        if (!removed) return
        
        viewModelScope.launch {
            preferencesRepository.deleteCustomCategory(category.id)
        }
        
        // Refresh category list
        val type = _uiState.value.transactionType
        val categories = Category.getCategoriesByType(type)
        val currentSelected = _uiState.value.selectedCategory
        _uiState.update {
            it.copy(
                categories = categories,
                selectedCategory = if (currentSelected?.id == category.id) null else currentSelected
            )
        }
    }

    fun renameCategory(category: Category, newName: String) {
        // Validate 4-char limit
        if (newName.length > 4) return
        // Cannot rename default categories
        if (category.isDefault) return
        
        val renamed = Category.renameCustomCategory(category, newName) ?: return
        
        viewModelScope.launch {
            // Persist: delete old, save new
            preferencesRepository.deleteCustomCategory(category.id)
            preferencesRepository.saveCustomCategory(renamed)
        }
        
        // Refresh category list and update selected if it was renamed
        val type = _uiState.value.transactionType
        val categories = Category.getCategoriesByType(type)
        val currentSelected = _uiState.value.selectedCategory
        _uiState.update {
            it.copy(
                categories = categories,
                selectedCategory = if (currentSelected?.id == category.id) renamed else currentSelected
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

        // Transfer/Lending validation
        when (state.transactionType) {
            TransactionType.TRANSFER -> {
                val fromMissing = state.selectedFromAccount == null
                val toMissing = state.selectedToAccount == null
                if (fromMissing && toMissing) {
                    _uiState.update { it.copy(errorMessage = "你的转出账户、转入账户未选择") }
                    return
                }
                if (fromMissing) {
                    _uiState.update { it.copy(errorMessage = "你的转出账户未选择") }
                    return
                }
                if (toMissing) {
                    _uiState.update { it.copy(errorMessage = "你的转入账户未选择") }
                    return
                }
            }
            TransactionType.LENDING -> {
                val fromMissing = state.selectedFromAccount == null
                val toMissing = state.selectedToAccount == null
                val fromLabel = getLendingFromLabel(state.lendingSubType)
                val toLabel = getLendingToLabel(state.lendingSubType)
                if (fromMissing && toMissing) {
                    _uiState.update { it.copy(errorMessage = "你的${fromLabel}、${toLabel}未选择") }
                    return
                }
                if (fromMissing) {
                    _uiState.update { it.copy(errorMessage = "你的${fromLabel}未选择") }
                    return
                }
                if (toMissing) {
                    _uiState.update { it.copy(errorMessage = "你的${toLabel}未选择") }
                    return
                }
            }
            else -> {
                if (state.selectedCategory == null) {
                    _uiState.update { it.copy(errorMessage = "请选择分类") }
                    return
                }
                if (state.accounts.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "账户未建立，请先建立账户") }
                    return
                }
                if (state.selectedAccount == null) {
                    _uiState.update { it.copy(errorMessage = "请选择账户") }
                    return
                }
            }
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // For TRANSFER/LENDING, create a synthetic category
                val category = when (state.transactionType) {
                    TransactionType.TRANSFER -> Category("transfer", "转账", "account_transfer", TransactionType.TRANSFER)
                    TransactionType.LENDING -> {
                        val subName = when (state.lendingSubType) {
                            LendingSubType.BORROW_IN -> "借入"
                            LendingSubType.BORROW_OUT -> "借出"
                            LendingSubType.REPAY -> "还款"
                            LendingSubType.COLLECT -> "收款"
                        }
                        val subId = when (state.lendingSubType) {
                            LendingSubType.BORROW_IN -> "borrow_in"
                            LendingSubType.BORROW_OUT -> "borrow_out"
                            LendingSubType.REPAY -> "repay"
                            LendingSubType.COLLECT -> "collect"
                        }
                        Category(subId, subName, "lend", TransactionType.LENDING)
                    }
                    else -> state.selectedCategory!!
                }

                // For TRANSFER/LENDING, create dual records
                if (state.transactionType == TransactionType.TRANSFER || state.transactionType == TransactionType.LENDING) {
                    val fromAccount = state.selectedFromAccount!!
                    val toAccount = state.selectedToAccount!!
                    
                    // 解析双记录的ID和关联ID
                    val (fromId, toId) = if (state.isEditing && editingTransactionId != null) {
                        val existingTx = transactionRepository.getTransactionById(editingTransactionId!!)
                        // editingTransactionId 可能是 from 或 to 记录，需要判断
                        if (existingTx != null && existingTx.amount < 0) {
                            // 当前编辑的是 from（出账）记录
                            val resolvedToId = existingTx.relatedTransactionId
                                ?: transactionRepository.getTransactionByRelatedId(editingTransactionId!!)?.id
                                ?: 0L
                            Pair(editingTransactionId!!, resolvedToId)
                        } else {
                            // 当前编辑的是 to（入账）记录
                            val resolvedFromId = existingTx?.relatedTransactionId
                                ?: transactionRepository.getTransactionByRelatedId(editingTransactionId!!)?.id
                                ?: 0L
                            Pair(resolvedFromId, editingTransactionId!!.toLong())
                        }
                    } else {
                        Pair(0L, 0L)
                    }
                    
                    // 出账账户：负数金额
                    val fromTransaction = Transaction(
                        id = fromId,
                        type = state.transactionType,
                        category = category,
                        amount = -amount, // 负数表示出账
                        note = state.note.ifBlank { null },
                        date = state.date,
                        accountId = fromAccount.id,
                        relatedTransactionId = toId // 保留关联链接
                    )
                    
                    // 入账账户：正数金额
                    val toTransaction = Transaction(
                        id = toId,
                        type = state.transactionType,
                        category = category,
                        amount = amount, // 正数表示入账
                        note = state.note.ifBlank { null },
                        date = state.date,
                        accountId = toAccount.id,
                        relatedTransactionId = fromId // 保留关联链接
                    )
                    
                    if (state.isEditing && editingTransactionId != null) {
                        transactionRepository.updateDualTransaction(fromTransaction, toTransaction)
                    } else {
                        transactionRepository.insertDualTransaction(fromTransaction, toTransaction)
                    }
                } else {
                    // 普通收支：单条记录
                    val accountId = state.selectedAccount?.id
                    
                    val transaction = Transaction(
                        id = editingTransactionId ?: 0,
                        type = state.transactionType,
                        category = category,
                        amount = amount,
                        note = state.note.ifBlank { null },
                        date = state.date,
                        accountId = accountId
                    )

                    if (state.isEditing && editingTransactionId != null) {
                        transactionRepository.updateTransaction(transaction)
                    } else {
                        transactionRepository.insertTransaction(transaction)
                    }
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

    private fun getLendingFromLabel(subType: LendingSubType): String {
        return when (subType) {
            LendingSubType.BORROW_IN -> "负债账户"
            LendingSubType.BORROW_OUT -> "出账账户"
            LendingSubType.REPAY -> "出账账户"
            LendingSubType.COLLECT -> "债权账户"
        }
    }

    private fun getLendingToLabel(subType: LendingSubType): String {
        return when (subType) {
            LendingSubType.BORROW_IN -> "入账账户"
            LendingSubType.BORROW_OUT -> "债权账户"
            LendingSubType.REPAY -> "负债账户"
            LendingSubType.COLLECT -> "入账账户"
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
