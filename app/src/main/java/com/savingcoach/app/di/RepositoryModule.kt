package com.savingcoach.app.di

import com.savingcoach.app.ai.AiChatRepository
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ChatRepository
import com.savingcoach.app.data.repository.ExpenseCategoryRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.FirebaseAuthRepository
import com.savingcoach.app.data.repository.FirebaseBudgetRepository
import com.savingcoach.app.data.repository.FirebaseExpenseCategoryRepository
import com.savingcoach.app.data.repository.FirebaseExpenseRepository
import com.savingcoach.app.data.repository.FirebaseInvestmentRepository
import com.savingcoach.app.data.repository.FirebaseSavingChallengeRepository
import com.savingcoach.app.data.repository.FirebaseUserRepository
import com.savingcoach.app.data.repository.InvestmentRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import com.savingcoach.app.data.repository.UserRepository
import com.savingcoach.app.data.repository.NotificationRepository
import com.savingcoach.app.data.repository.FirebaseNotificationRepository
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
        impl: FirebaseExpenseRepository
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: FirebaseBudgetRepository
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: AiChatRepository
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSavingChallengeRepository(
        impl: FirebaseSavingChallengeRepository
    ): SavingChallengeRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindExpenseCategoryRepository(
        impl: FirebaseExpenseCategoryRepository
    ): ExpenseCategoryRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: FirebaseUserRepository
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindInvestmentRepository(
        impl: FirebaseInvestmentRepository
    ): InvestmentRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: FirebaseNotificationRepository
    ): NotificationRepository
}
