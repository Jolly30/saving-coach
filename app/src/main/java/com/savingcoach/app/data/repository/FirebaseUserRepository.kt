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
    private val usernamesCollection = firestore.collection("usernames")

    override suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            val batch = firestore.batch()
            batch.set(usersCollection.document(user.uid), user)
            val exactUsername = user.username.trim()
            if (exactUsername.isNotBlank()) {
                batch.set(
                    usernamesCollection.document(exactUsername),
                    mapOf(
                        "email" to user.email,
                        "uid" to user.uid,
                        "username" to exactUsername
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(uid: String): Result<User?> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEmailByUsername(username: String): Result<String?> {
        val exactUsername = username.trim()
        return try {
            val doc = usernamesCollection.document(exactUsername).get().await()
            if (doc.exists()) {
                val storedUsername = doc.getString("username")
                // Strict character-by-character case-sensitive comparison
                if (storedUsername == exactUsername) {
                    val email = doc.getString("email")
                    if (!email.isNullOrBlank()) {
                        return Result.success(email)
                    }
                }
            }
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUsernameTaken(username: String): Result<Boolean> {
        val exactUsername = username.trim()
        return try {
            val doc = usernamesCollection.document(exactUsername).get().await()
            if (doc.exists() && doc.getString("username") == exactUsername) {
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    override suspend fun updateUsername(uid: String, newUsername: String): Result<Unit> {
        val exactNew = newUsername.trim()
        return try {
            val oldDoc = usersCollection.document(uid).get().await()
            val oldUsername = oldDoc.getString("username")
            val email = oldDoc.getString("email")?.ifBlank { null }
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""

            // Update primary user profile in users collection
            usersCollection.document(uid)
                .set(mapOf(
                    "username" to exactNew,
                    "email" to email
                ), com.google.firebase.firestore.SetOptions.merge())
                .await()

            // Sync to public usernames collection and purge all old ones
            try {
                if (exactNew.isNotBlank() && email.isNotBlank()) {
                    usernamesCollection.document(exactNew)
                        .set(mapOf(
                            "email" to email,
                            "uid" to uid,
                            "username" to exactNew
                        ), com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }

                val oldEntries = usernamesCollection.whereEqualTo("uid", uid).get().await()
                for (doc in oldEntries.documents) {
                    if (doc.id != exactNew) {
                        doc.reference.delete().await()
                    }
                }
            } catch (_: Exception) {}

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
            val doc = usersCollection.document(uid).get().await()
            val username = doc.getString("username")
            if (!username.isNullOrBlank()) {
                try {
                    usernamesCollection.document(username.lowercase().trim())
                        .set(mapOf("email" to newEmail), com.google.firebase.firestore.SetOptions.merge())
                        .await()
                } catch (_: Exception) {}
            }

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

    override suspend fun deleteUserProfile(uid: String): Result<Unit> {
        return try {
            val oldDoc = usersCollection.document(uid).get().await()
            val username = oldDoc.getString("username")?.trim()
            if (!username.isNullOrBlank()) {
                try {
                    usernamesCollection.document(username).delete().await()
                } catch (_: Exception) {}
            }
            try {
                val oldEntries = usernamesCollection.whereEqualTo("uid", uid).get().await()
                for (doc in oldEntries.documents) {
                    doc.reference.delete().await()
                }
            } catch (_: Exception) {}
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
