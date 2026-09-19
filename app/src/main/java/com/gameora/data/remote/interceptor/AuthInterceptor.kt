package com.gameora.data.remote.interceptor

import com.gameora.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Bearer <token>` to every request when a token is present.
 * The token is read from the encrypted [TokenStore], never hard-coded.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenStore.accessToken
        val authed = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        return chain.proceed(authed)
    }
}
