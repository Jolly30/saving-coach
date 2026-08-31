package com.savingcoach.app.data.repository

import com.savingcoach.app.data.model.User

interface UserRepository {
    suspend fun createUserProfile(user: User): Result<Unit>
    suspend fun getUserProfile(uid: String): Result<User?>
    suspend fun getEmailByUsername(username: String): Result<String?>
    suspend fun isUsernameTaken(username: String): Result<Boolean>
    suspend fun updateUsername(uid: String, newUsername: String): Result<Unit>
    suspend fun updateAge(uid: String, age: Int?): Result<Unit>
    suspend fun updateGender(uid: String, gender: String?): Result<Unit>
    suspend fun updateSalaryRange(uid: String, range: String?): Result<Unit>
    suspend fun updateFieldOfWork(uid: String, field: String?): Result<Unit>
    suspend fun updateEmail(uid: String, newEmail: String): Result<Unit>
    suspend fun updateOnboardingCompleted(uid: String): Result<Unit>
    fun getUserProfileFlow(uid: String): kotlinx.coroutines.flow.Flow<User?>
    suspend fun updateCurrencyPreference(uid: String, currency: String): Result<Unit>
    suspend fun updateLanguagePreference(uid: String, language: String): Result<Unit>
}

