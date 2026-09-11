package com.gameora.data.repository

import com.gameora.data.local.SessionManager
import com.gameora.data.local.TokenStore
import com.gameora.data.remote.api.ApiService
import com.gameora.domain.model.AuthSession
import com.gameora.domain.model.User
import com.gameora.util.UiState
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
     * After Firebase authentication, the Gameora profile is synchronized
     * with the backend using the Firebase UID.
     */
    suspend fun login(
        emailOrUsername: String,
        password: String
    ): Result<AuthSession> {
        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(
                    emailOrUsername.trim(),
                    password
                )
                .awaitFirebaseTask()

            val firebaseUser = result.user
                ?: throw IllegalStateException("Firebase user is null")

            val sessionResult = createSession(firebaseUser)

            if (sessionResult.isFailure) {
                return sessionResult
            }

            val session = sessionResult.getOrThrow()

            val syncedUser = syncGameoraUser(
                firebaseUser = firebaseUser,
                username = session.user.username
                    ?: firebaseUser.email
                        ?.substringBefore("@")
                        ?.takeIf { it.isNotBlank() }
                    ?: firebaseUser.uid
            )

            Result.success(
                session.copy(user = syncedUser)
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Creates a Firebase account using email/password,
     * then creates/synchronizes the Gameora profile on the backend.
     */
    suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String?
    ): Result<AuthSession> {
        return try {
            val cleanUsername = username.trim()
            val cleanEmail = email.trim()

            if (cleanUsername.isBlank()) {
                throw IllegalArgumentException("Username is required")
            }

            if (cleanEmail.isBlank()) {
                throw IllegalArgumentException("Email is required")
            }

            val result = firebaseAuth
                .createUserWithEmailAndPassword(
                    cleanEmail,
                    password
                )
                .awaitFirebaseTask()

            val firebaseUser = result.user
                ?: throw IllegalStateException("Firebase user is null")

            val finalDisplayName =
                displayName
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: cleanUsername

            if (finalDisplayName.isNotBlank()) {
                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(finalDisplayName)
                    .build()

                firebaseUser
                    .updateProfile(profileUpdate)
                    .awaitFirebaseTask()
            }

            // Reload so Firebase contains the latest display name.
            firebaseUser
                .reload()
                .awaitFirebaseTask()

            val refreshedUser =
                firebaseAuth.currentUser ?: firebaseUser

            /*
             * createSession() saves the Firebase ID token first.
             * This is important because /users/sync requires authentication.
             */
            val sessionResult = createSession(refreshedUser)

            if (sessionResult.isFailure) {
                return sessionResult
            }

            val session = sessionResult.getOrThrow()

            /*
             * Now synchronize the Firebase account with Gameora backend.
             *
             * Backend creates:
             * users/{Firebase UID}
             * wallets/{Firebase UID}
             */
            val syncedUser = syncGameoraUser(
                firebaseUser = refreshedUser,
                username = cleanUsername
            )

            Result.success(
                session.copy(user = syncedUser)
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Synchronizes the authenticated Firebase account
     * with the Gameora backend.
     */
    private suspend fun syncGameoraUser(
        firebaseUser: FirebaseUser,
        username: String
    ): User {

        val emailValue =
            firebaseUser.email
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException(
                    "Firebase user email is missing"
                )

        val displayNameValue =
            firebaseUser.displayName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: username

        val avatarUrlValue =
            firebaseUser.photoUrl
                ?.toString()
                ?.takeIf { it.isNotBlank() }

        val body = mapOf(
            "username" to username.trim(),
            "email" to emailValue,
            "displayName" to displayNameValue,
            "avatarUrl" to avatarUrlValue
        )

        val serverUser = api.syncUser(body)

        sessionManager.onLoggedIn(serverUser)

        return serverUser
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
            tokenStore.clear()
            sessionManager.onLoggedOut()

            Result.failure(e)
        }
    }

    /**
     * Returns the currently authenticated Firebase user.
     *
     * The Firebase account is also synchronized with the backend
     * so the Gameora profile remains available.
     */
    suspend fun fetchCurrentUser(): Result<User> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: throw IllegalStateException(
                    "No authenticated Firebase user"
                )

            firebaseUser
                .reload()
                .awaitFirebaseTask()

            val currentUser =
                firebaseAuth.currentUser ?: firebaseUser

            val emailValue =
                currentUser.email
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val generatedUsername =
                emailValue
                    ?.substringBefore("@")
                    ?.takeIf { it.isNotBlank() }
                    ?: currentUser.uid

            val user = syncGameoraUser(
                firebaseUser = currentUser,
                username = generatedUsername
            )

            Result.success(user)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Checks whether a Firebase account is currently authenticated.
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Creates the application's AuthSession from the Firebase user.
     *
     * The Firebase ID token is saved locally so the Retrofit
     * authentication interceptor can send it to the backend.
     */
    private suspend fun createSession(
        firebaseUser: FirebaseUser
    ): Result<AuthSession> {
        return try {
            val tokenResult = firebaseUser
                .getIdToken(false)
                .awaitFirebaseTask()

            val idToken = tokenResult.token
                ?: throw IllegalStateException(
                    "Firebase ID token is null"
                )

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
     * Backend profile information will replace these defaults
     * after synchronization.
     */
    private fun FirebaseUser.toDomainUser(): User {
        val emailValue =
            email
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        val generatedUsername =
            emailValue
                ?.substringBefore("@")
                ?.takeIf { it.isNotBlank() }

        return User(
            id = uid,
            username = generatedUsername,
            displayName = displayName
                ?.takeIf { it.isNotBlank() },
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
     * Converts Firebase Task<T> into a suspending function
     * without requiring kotlinx-coroutines-play-services.
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
                // Firebase Task does not expose a universal
                // cancellation API for every authentication operation.
            }
        }
}
