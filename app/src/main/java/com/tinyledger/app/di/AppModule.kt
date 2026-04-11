package com.tinyledger.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.tinyledger.app.data.repository.AccountRepositoryImpl
import com.tinyledger.app.data.repository.PreferencesRepositoryImpl
import com.tinyledger.app.data.repository.TransactionRepositoryImpl
import com.tinyledger.app.data.repository.UpdateCheckRepositoryImpl
import com.tinyledger.app.domain.repository.AccountRepository
import com.tinyledger.app.domain.repository.PreferencesRepository
import com.tinyledger.app.domain.repository.TransactionRepository
import com.tinyledger.app.domain.repository.UpdateCheckRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: com.tinyledger.app.data.local.dao.TransactionDao,
        accountDao: com.tinyledger.app.data.local.dao.AccountDao
    ): TransactionRepository {
        return TransactionRepositoryImpl(transactionDao, accountDao)
    }

    @Provides
    @Singleton
    fun provideAccountRepository(
        accountDao: com.tinyledger.app.data.local.dao.AccountDao,
        transactionDao: com.tinyledger.app.data.local.dao.TransactionDao
    ): AccountRepository {
        return AccountRepositoryImpl(accountDao, transactionDao)
    }

    @Provides
    @Singleton
    fun providePreferencesRepository(
        dataStore: DataStore<Preferences>
    ): PreferencesRepository {
        return PreferencesRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideUpdateCheckRepository(): UpdateCheckRepository {
        return UpdateCheckRepositoryImpl()
    }
}
