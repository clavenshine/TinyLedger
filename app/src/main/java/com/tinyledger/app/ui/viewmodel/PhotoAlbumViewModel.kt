package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.TransactionRepository
import com.tinyledger.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * 月份选择模式: MONTH 表示按月筛选, YEAR 表示按年筛选
 */
enum class DateFilterMode { MONTH, YEAR }

data class DateFilter(
    val mode: DateFilterMode = DateFilterMode.MONTH,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
) {
    /** 获取显示文本, 如 "2026年4月" 或 "2026年" */
    fun displayText(): String = if (mode == DateFilterMode.MONTH) {
        "${year}年${month}月"
    } else {
        "${year}年"
    }

    /** 筛选交易是否在范围内 */
    fun matches(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return when (mode) {
            DateFilterMode.MONTH -> cal.get(Calendar.YEAR) == year && (cal.get(Calendar.MONTH) + 1) == month
            DateFilterMode.YEAR -> cal.get(Calendar.YEAR) == year
        }
    }
}

data class PhotoAlbumUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val dateFilter: DateFilter = DateFilter(),
    val selectedTypes: Set<TransactionType> = emptySet(),
    val groupedByMonth: Map<String, List<Transaction>> = emptyMap(),
    val isLoading: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PhotoAlbumViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _dateFilter = MutableStateFlow(DateFilter())
    private val _selectedTypes = MutableStateFlow<Set<TransactionType>>(emptySet())

    private val _uiState = MutableStateFlow(PhotoAlbumUiState(isLoading = true))
    val uiState: StateFlow<PhotoAlbumUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                _dateFilter,
                _selectedTypes
            ) { filter, types ->
                Pair(filter, types)
            }.flatMapLatest { (filter, types) ->
                transactionRepository.getAllTransactions()
                    .map { transactions ->
                        // 过滤有图片的交易
                        val withImages = transactions.filter {
                            !it.imagePath.isNullOrBlank()
                        }

                        // 按日期筛选(月/年)
                        val dateFiltered = withImages.filter { filter.matches(it.date) }

                        // 按类型过滤（多选）
                        val typeFiltered = if (types.isNotEmpty()) {
                            dateFiltered.filter { it.type in types }
                        } else {
                            dateFiltered
                        }

                        // 如果按月筛选, 不再按月分组, 直接展示; 按年筛选则按月分组
                        val grouped = if (filter.mode == DateFilterMode.YEAR) {
                            typeFiltered.groupBy { tx ->
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = tx.date
                                val y = cal.get(Calendar.YEAR)
                                val m = cal.get(Calendar.MONTH) + 1
                                "${y}年${m.toString().padStart(2, '0')}月"
                            }.mapValues { (_, txs) ->
                                txs.sortedByDescending { it.date }
                            }.toSortedMap(compareByDescending { it })
                        } else {
                            // 按月筛选时, 用单月 key 包裹所有结果
                            val label = "${filter.year}年${filter.month.toString().padStart(2, '0')}月"
                            if (typeFiltered.isEmpty()) emptyMap() else mapOf(label to typeFiltered.sortedByDescending { it.date })
                        }

                        PhotoAlbumUiState(
                            transactions = typeFiltered,
                            filteredTransactions = typeFiltered,
                            dateFilter = filter,
                            selectedTypes = types,
                            groupedByMonth = grouped,
                            isLoading = false
                        )
                    }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun setTypes(types: Set<TransactionType>) {
        _selectedTypes.value = types
    }
}
