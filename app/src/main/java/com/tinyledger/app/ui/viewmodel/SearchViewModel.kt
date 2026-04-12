package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.PreferencesRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import com.tinyledger.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class SearchFilterType {
    ALL, EXPENSE, INCOME, TRANSFER, LENDING
}

data class SearchUiState(
    val query: String = "",
    val results: List<Transaction> = emptyList(),
    val filterType: SearchFilterType = SearchFilterType.ALL,
    val currencySymbol: String = "¥",
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filterType = MutableStateFlow(SearchFilterType.ALL)
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadSearchResults()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.getSettings().collect { settings ->
                _uiState.update { it.copy(currencySymbol = settings.currencySymbol) }
            }
        }
    }

    private fun loadSearchResults() {
        viewModelScope.launch {
            combine(
                _query,
                _filterType,
                _startDate,
                _endDate,
                transactionRepository.getAllTransactions()
            ) { query, filterType, startDate, endDate, allTransactions ->
                SearchParams(query, filterType, startDate, endDate, allTransactions)
            }.map { params ->
                if (params.query.isBlank()) {
                    emptyList()
                } else {
                    val keyword = params.query.trim().lowercase()
                    params.allTransactions
                        .filter { transaction ->
                            // Match by category name
                            transaction.category.name.lowercase().contains(keyword) ||
                            // Match by note
                            transaction.note?.lowercase()?.contains(keyword) == true ||
                            // Match by amount (exact or partial)
                            transaction.amount.toString().contains(keyword) ||
                            // Match by category id
                            transaction.category.id.lowercase().contains(keyword)
                        }
                        .filter { transaction ->
                            // Filter by type
                            when (params.filterType) {
                                SearchFilterType.ALL -> true
                                SearchFilterType.EXPENSE -> transaction.type == TransactionType.EXPENSE
                                SearchFilterType.INCOME -> transaction.type == TransactionType.INCOME
                                SearchFilterType.TRANSFER -> transaction.type == TransactionType.TRANSFER
                                SearchFilterType.LENDING -> transaction.type == TransactionType.LENDING
                            }
                        }
                        .filter { transaction ->
                            // Filter by date range
                            if (params.startDate != null && params.endDate != null) {
                                transaction.date in params.startDate..params.endDate
                            } else {
                                true
                            }
                        }
                        .sortedByDescending { it.date }
                }
            }.collect { results ->
                _uiState.update { it.copy(results = results, isLoading = false) }
            }
        }
    }

    fun setQuery(query: String) {
        _query.value = query
        _uiState.update { it.copy(query = query, hasSearched = true, isLoading = query.isNotBlank()) }
    }

    fun setFilterType(filterType: SearchFilterType) {
        _filterType.value = filterType
        _uiState.update { it.copy(filterType = filterType) }
    }

    fun setDateRange(startDate: Long?, endDate: Long?) {
        _startDate.value = startDate
        _endDate.value = endDate
        _uiState.update { it.copy(startDate = startDate, endDate = endDate) }
    }

    fun clearSearch() {
        _query.value = ""
        _uiState.update { it.copy(query = "", results = emptyList(), hasSearched = false, startDate = null, endDate = null) }
        _startDate.value = null
        _endDate.value = null
    }

    private data class SearchParams(
        val query: String,
        val filterType: SearchFilterType,
        val startDate: Long?,
        val endDate: Long?,
        val allTransactions: List<Transaction>
    )
}
