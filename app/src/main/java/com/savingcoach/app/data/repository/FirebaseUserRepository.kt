package com.savingcoach.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.savingcoach.app.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            // Document ID is the user's UID
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(uid: String): Result<User?> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                Result.success(document.toObject(User::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEmailByUsername(username: String): Result<String?> {
        return try {
            val querySnapshot = usersCollection
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val user = querySnapshot.documents[0].toObject(User::class.java)
                Result.success(user?.email)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUsernameTaken(username: String): Result<Boolean> {
        return try {
            val querySnapshot = usersCollection
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            
            Result.success(!querySnapshot.isEmpty)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateUsername(uid: String, newUsername: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .set(mapOf("username" to newUsername), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAge(uid: String, age: Int?): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .set(mapOf("age" to age), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateGender(uid: String, gender: String?): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .set(mapOf("gender" to gender), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSalaryRange(uid: String, range: String?): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .set(mapOf("salaryRange" to range), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFieldOfWork(uid: String, field: String?): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .set(mapOf("fieldOfWork" to field), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEmail(uid: String, newEmail: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .set(mapOf("email" to newEmail), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOnboardingCompleted(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .set(mapOf("onboardingCompleted" to true), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUserProfileFlow(uid: String): Flow<User?> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(User::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun updateCurrencyPreference(uid: String, currency: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .set(mapOf("currencyPreference" to currency), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLanguagePreference(uid: String, language: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .set(mapOf("languagePreference" to language), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
