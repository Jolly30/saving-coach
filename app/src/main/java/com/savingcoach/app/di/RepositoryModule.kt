package com.savingcoach.app.di

import com.savingcoach.app.data.repository.FirebaseAuthRepository
import com.savingcoach.app.data.mock.MockBudgetRepository
import com.savingcoach.app.data.mock.MockChatRepository
import com.savingcoach.app.data.mock.MockExpenseRepository
import com.savingcoach.app.data.mock.MockSavingChallengeRepository
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ChatRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        impl: MockExpenseRepository
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: MockBudgetRepository
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: MockChatRepository
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSavingChallengeRepository(
        impl: MockSavingChallengeRepository
    ): SavingChallengeRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepository
    ): AuthRepository
}
