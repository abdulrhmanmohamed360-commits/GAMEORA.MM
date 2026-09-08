package com.gameora.data.repository

import com.gameora.data.remote.dto.ApiErrorDto
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/** A server-side failure with the HTTP status code and a parsed message. */
class ApiException(message: String, val code: Int, cause: Throwable) : Exception(message, cause)

/** A connectivity / transport failure (no response received). */
class NetworkException(cause: Throwable) : Exception("network_error", cause)

/**
 * Wraps a suspend API call in [Result]. On HTTP errors it parses the server's error
 * body into a message; on transport errors it returns [NetworkException]. The UI maps
 * any failure to an Error state instead of showing placeholder content.
 */
suspend fun <T> safeApi(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    Result.failure(ApiException(parseErrorMessage(e), e.code(), e))
} catch (e: IOException) {
    Result.failure(NetworkException(e))
} catch (e: Throwable) {
    Result.failure(e)
}

private fun parseErrorMessage(e: HttpException): String {
    return try {
        val raw = e.response()?.errorBody()?.string()
        val dto = Gson().fromJson(raw, ApiErrorDto::class.java)
        dto?.message?.takeIf { it.isNotBlank() }
            ?: dto?.error?.takeIf { it.isNotBlank() }
            ?: "error_${e.code()}"
    } catch (_: Exception) {
        "error_${e.code()}"
    }
}
