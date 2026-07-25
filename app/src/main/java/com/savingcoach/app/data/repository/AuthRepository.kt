package com.savingcoach.app.data.repository

import com.google.firebase.auth.AuthResult

interface AuthRepository {
    fun isUserSignedIn(): Boolean
    fun getCurrentUserId(): String?
    suspend fun signInWithGoogle(idToken: String): Result<AuthResult>
    suspend fun signInWithEmail(email: String, password: String): Result<AuthResult>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthResult>
    suspend fun signOut()
}
