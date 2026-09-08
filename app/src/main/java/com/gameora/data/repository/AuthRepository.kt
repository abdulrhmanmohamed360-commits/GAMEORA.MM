package com.gameora.data.repository

import com.gameora.data.local.SessionManager
import com.gameora.data.local.TokenStore
import com.gameora.data.remote.api.ApiService
import com.gameora.domain.model.AuthSession
import com.gameora.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager
) {

    private val firebaseAuth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    /**
     * Firebase Email/Password login.
     *
     * Note:
     * The current UI sends an email address, so Firebase authentication
     * is currently email-based. Username login can be added later using
     * the user profile database.
     */
    suspend fun login(
        emailOrUsername: String,
        password: String
    ): Result<AuthSession> {
        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(emailOrUsername.trim(), password)
                .awaitFirebaseTask()

            val firebaseUser = result.user
                ?: throw IllegalStateException("Firebase user is null")

            createSession(firebaseUser)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Creates a Firebase account using email/password.
     *
     * displayName is stored in Firebase Authentication.
     * The unique username will be handled later by the user profile
     * database/server layer.
     */
    suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String?
    ): Result<AuthSession> {
        return try {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email.trim(), password)
                .awaitFirebaseTask()

            val firebaseUser = result.user
                ?: throw IllegalStateException("Firebase user is null")

            val finalDisplayName =
                displayName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: username.trim().takeIf { it.isNotEmpty() }

            if (!finalDisplayName.isNullOrBlank()) {
                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(finalDisplayName)
                    .build()

                firebaseUser
                    .updateProfile(profileUpdate)
                    .awaitFirebaseTask()
            }

            // Reload the Firebase user so the updated display name is available.
            firebaseUser.reload().awaitFirebaseTask()

            val refreshedUser = firebaseAuth.currentUser
                ?: firebaseUser

            createSession(refreshedUser)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Logs out from Firebase and clears the local session/token.
     */
    suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()

            tokenStore.clear()
            sessionManager.onLoggedOut()

            Result.success(Unit)
        } catch (e: Throwable) {
            // Local credentials must still be removed if anything unexpected happens.
            tokenStore.clear()
            sessionManager.onLoggedOut()

            Result.failure(e)
        }
    }

    /**
     * Returns the currently authenticated Firebase user.
     *
     * This replaces the old server-only getCurrentUser flow for the
     * authentication phase.
     */
    suspend fun fetchCurrentUser(): Result<User> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: throw IllegalStateException("No authenticated Firebase user")

            firebaseUser.reload().awaitFirebaseTask()

            val currentUser = firebaseAuth.currentUser
                ?: firebaseUser

            val user = currentUser.toDomainUser()

            sessionManager.onLoggedIn(user)

            Result.success(user)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Firebase Authentication keeps the refresh token internally.
     *
     * We store the current ID token in TokenStore as the access token
     * so the existing networking layer can use the same storage mechanism.
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Creates the application's AuthSession from the Firebase user.
     */
    private suspend fun createSession(
        firebaseUser: FirebaseUser
    ): Result<AuthSession> {
        return try {
            val tokenResult = firebaseUser
                .getIdToken(false)
                .awaitFirebaseTask()

            val idToken = tokenResult.token
                ?: throw IllegalStateException("Firebase ID token is null")

            val user = firebaseUser.toDomainUser()

            tokenStore.saveTokens(
                accessToken = idToken,
                refreshToken = null
            )

            sessionManager.onLoggedIn(user)

            Result.success(
                AuthSession(
                    accessToken = idToken,
                    refreshToken = null,
                    user = user
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Converts FirebaseUser into the existing Gameora User model.
     *
     * Profile fields that Firebase Authentication does not provide
     * are intentionally left at safe defaults for now.
     *
     * Later, Firestore/backend user profiles can supply:
     * username, avatar, rating, reviewsCount, verified, isSeller, etc.
     */
    private fun FirebaseUser.toDomainUser(): User {
        val emailValue = email?.trim()?.takeIf { it.isNotEmpty() }

        val generatedUsername = emailValue
            ?.substringBefore("@")
            ?.takeIf { it.isNotBlank() }

        return User(
            id = uid,
            username = generatedUsername,
            displayName = displayName?.takeIf { it.isNotBlank() },
            email = emailValue,
            avatarUrl = photoUrl?.toString(),
            rating = 0.0,
            reviewsCount = 0,
            verified = isEmailVerified,
            isSeller = false,
            createdAt = null,
            updatedAt = null
        )
    }

    /**
     * Converts Firebase Task<T> into a suspending function without
     * requiring kotlinx-coroutines-play-services.
     */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitFirebaseTask(): T =
        suspendCancellableCoroutine { continuation ->

            addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }

            addOnCanceledListener {
                continuation.cancel()
            }

            continuation.invokeOnCancellation {
                // Firebase Task does not expose a universal cancellation API
                // for every authentication operation, so there is nothing
                // additional to cancel here.
            }
        }
}
