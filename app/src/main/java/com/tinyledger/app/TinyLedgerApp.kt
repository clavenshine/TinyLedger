package com.tinyledger.app

import android.app.Application
import android.util.Log
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.repository.PreferencesRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TinyLedgerApp : Application() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 在应用启动时加载自定义分类，确保所有页面都能正确显示自定义分类名称
        applicationScope.launch {
            val customCategories = preferencesRepository.getCustomCategories()
            Category.loadCustomCategories(customCategories)

            // 检测自定义分类与默认分类名称重复，优先使用默认分类，并同步更新记账明细
            val duplicates = Category.findDuplicateCustomCategories()
            if (duplicates.isNotEmpty()) {
                Log.d("TinyLedgerApp", "Found ${duplicates.size} duplicate custom categories, migrating...")
                
                // 1. 更新数据库中使用了旧自定义分类ID的交易记录
                for ((oldCategoryId, newCategoryId) in duplicates) {
                    transactionRepository.updateCategoryForTransactions(oldCategoryId, newCategoryId)
                    Log.d("TinyLedgerApp", "Migrated category: $oldCategoryId -> $newCategoryId")
                }
                
                // 2. 从内存中移除重复的自定义分类
                val removedCategories = Category.removeDuplicateCustomCategories(duplicates.keys)
                
                // 3. 从持久化存储中删除重复的自定义分类
                for (category in removedCategories) {
                    preferencesRepository.deleteCustomCategory(category.id)
                }
                
                Log.d("TinyLedgerApp", "Migration complete: removed ${removedCategories.size} duplicate custom categories")
            }
        }
    }
}
