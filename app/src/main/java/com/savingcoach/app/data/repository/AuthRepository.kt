package com.savingcoach.app.data.repository

import com.google.firebase.auth.AuthResult

interface AuthRepository {
    fun isUserSignedIn(): Boolean
    fun getCurrentUserId(): String?
    suspend fun signInWithGoogle(idToken: String): Result<AuthResult>
    suspend fun signInWithEmailOrUsername(input: String, password: String): Result<AuthResult>
    suspend fun signUpWithEmail(email: String, password: String, username: String): Result<AuthResult>
    suspend fun updateEmail(newEmail: String): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
    suspend fun signOut()
}
