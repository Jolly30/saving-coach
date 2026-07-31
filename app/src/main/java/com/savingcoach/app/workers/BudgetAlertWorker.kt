package com.savingcoach.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.savingcoach.app.core.notification.NotificationHelper
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

            val budget = budgetRepository.getBudget(userId, currentMonth).first()
            val expenses = expenseRepository.getExpensesForMonth(userId, currentMonth).first()

            if (budget != null) {
                val totalSpent = expenses.sumOf { it.amount }
                val percentage = ((totalSpent / budget.limit) * 100).toInt()

                when {
                    percentage >= 100 -> notificationHelper.showBudgetAlert(percentage, totalSpent - budget.limit)
                    percentage >= 90 -> notificationHelper.showBudgetAlert(percentage)
                    percentage >= 75 -> notificationHelper.showBudgetAlert(percentage)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
