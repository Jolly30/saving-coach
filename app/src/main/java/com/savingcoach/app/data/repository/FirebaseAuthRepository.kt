package com.savingcoach.app.data.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.savingcoach.app.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) : AuthRepository {

    override fun isUserSignedIn(): Boolean = firebaseAuth.currentUser != null

    override fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    override suspend fun signInWithGoogle(idToken: String): Result<AuthResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
            
            if (user != null) {
                // Check if user profile exists
                val profileResult = userRepository.getUserProfile(user.uid)
                if (profileResult.isSuccess && profileResult.getOrNull() == null) {
                    // Create default profile for new Google user
                    val defaultUsername = "user_${UUID.randomUUID().toString().substring(0, 8)}"
                    val newUser = User(
                        uid = user.uid,
                        email = user.email ?: "",
                        username = defaultUsername
                    )
                    userRepository.createUserProfile(newUser)
                }
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmailOrUsername(input: String, password: String): Result<AuthResult> {
        return try {
            val emailToUse = if (input.contains("@")) {
                input
            } else {
                val emailResult = userRepository.getEmailByUsername(input)
                if (emailResult.isFailure) {
                    return Result.failure(emailResult.exceptionOrNull() ?: Exception("Failed to check username"))
                }
                val email = emailResult.getOrNull()
                if (email == null) {
                    return Result.failure(Exception("Username not found"))
                }
                email
            }
            
            val result = firebaseAuth.signInWithEmailAndPassword(emailToUse, password).await()
            val user = result.user
            
            if (user != null) {
                // Check if user profile exists (for older users logging in)
                val profileResult = userRepository.getUserProfile(user.uid)
                val userProfile = profileResult.getOrNull()
                if (profileResult.isSuccess && (userProfile == null || userProfile.username.isBlank())) {
                    // Create default profile for existing user without a username
                    val defaultUsername = "user_${UUID.randomUUID().toString().substring(0, 8)}"
                    val newUser = User(
                        uid = user.uid,
                        email = user.email ?: emailToUse,
                        username = defaultUsername
                    )
                    userRepository.createUserProfile(newUser)
                }
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, username: String): Result<AuthResult> {
        return try {
            // First check if username is taken
            val isTakenResult = userRepository.isUsernameTaken(username)
            if (isTakenResult.isSuccess && isTakenResult.getOrDefault(true)) {
                return Result.failure(Exception("Username is already taken"))
            }
            
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                val newUser = User(
                    uid = user.uid,
                    email = email,
                    username = username
                )
                userRepository.createUserProfile(newUser)
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: throw Exception("User not logged in")
            user.updateEmail(newEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: throw Exception("User not logged in")
            val email = user.email ?: throw Exception("User has no email associated")
            
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
}
