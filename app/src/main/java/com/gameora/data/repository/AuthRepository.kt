package com.gameora.data.repository

import com.gameora.data.local.SessionManager
import com.gameora.data.local.TokenStore
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.UserDto
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
     * After Firebase authentication, the Gameora profile
     * is synchronized with the backend.
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

            val fallbackUsername =
                firebaseUser.email
                    ?.substringBefore("@")
                    ?.takeIf { it.isNotBlank() }
                    ?: firebaseUser.uid

            val syncedUser = syncGameoraUser(
                firebaseUser = firebaseUser,
                username = fallbackUsername
            )

            Result.success(
                session.copy(user = syncedUser)
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Creates a Firebase account and then creates/synchronizes
     * the Gameora profile on the backend.
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

            firebaseUser
                .reload()
                .awaitFirebaseTask()

            val refreshedUser =
                firebaseAuth.currentUser ?: firebaseUser

            /*
             * Save the Firebase ID token before calling /users/sync.
             */
            val sessionResult = createSession(refreshedUser)

            if (sessionResult.isFailure) {
                return sessionResult
            }

            val session = sessionResult.getOrThrow()

            /*
             * Create the Gameora profile using the Firebase UID.
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
     * Synchronizes the Firebase account with the Gameora backend.
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

        val serverUserDto = api.syncUser(body)

        val serverUser = serverUserDto.toDomainUser()

        sessionManager.onLoggedIn(serverUser)

        return serverUser
    }

    /**
     * Converts the backend UserDto into the application's domain User.
     */
    private fun UserDto.toDomainUser(): User {
        return User(
            id = id,
            username = username,
            displayName = displayName,
            email = email,
            avatarUrl = avatarUrl,
            rating = rating,
            reviewsCount = reviewsCount,
            verified = verified,
            isSeller = isSeller,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
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
     * Returns the currently authenticated user and synchronizes
     * the Gameora profile with the backend.
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
     * Creates the application's AuthSession from Firebase.
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
                ?: uid

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
     * Converts Firebase Task<T> into a suspending function.
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
