package com.tinyledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinyledger.app.data.local.dao.CategoryDao
import com.tinyledger.app.data.local.entity.CategoryEntity
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // 独立于 viewModelScope 的保存作用域，确保保存操作不会因页面返回而被取消
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun saveCategoryToDatabase(category: Category) {
        val entity = CategoryEntity(
            id = category.id,
            name = category.name,
            icon = category.icon,
            type = category.type.ordinal,
            isDefault = category.isDefault,
            parentId = category.parentId,
            sortOrder = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        // 使用 saveScope 而非 viewModelScope，确保页面返回后保存仍能完成
        saveScope.launch {
            // 保存到 Room 数据库
            categoryDao.insertCategory(entity)
            // 同时保存到 DataStore（兼容现有加载逻辑）
            preferencesRepository.saveCustomCategory(category)
        }
    }

    fun deleteCategoryFromDatabase(categoryId: String) {
        viewModelScope.launch {
            categoryDao.deleteCategoryById(categoryId)
        }
    }

    suspend fun isCategoryNameExists(type: TransactionType, name: String): Boolean {
        return categoryDao.countCategoryByName(type.ordinal, name) > 0
    }
}
