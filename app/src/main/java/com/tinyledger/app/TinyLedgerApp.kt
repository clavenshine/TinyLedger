package com.tinyledger.app

import android.app.Application
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.repository.PreferencesRepository
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 在应用启动时加载自定义分类，确保所有页面都能正确显示自定义分类名称
        applicationScope.launch {
            val customCategories = preferencesRepository.getCustomCategories()
            Category.loadCustomCategories(customCategories)
        }
    }
}
