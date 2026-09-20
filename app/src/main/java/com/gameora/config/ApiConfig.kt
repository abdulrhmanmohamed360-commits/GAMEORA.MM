package com.gameora.config

import com.gameora.BuildConfig

/**
 * Single source of truth for the backend location. The values come from
 * BuildConfig fields defined once in app/build.gradle. Change the URL there
 * (or via a product flavour / CI variable) and the whole app follows.
 *
 * No other file in the project hard-codes a host.
 */
object ApiConfig {
    const val API_BASE_URL: String = BuildConfig.API_BASE_URL
    const val IMAGE_BASE_URL: String = BuildConfig.IMAGE_BASE_URL

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    /** Default page size for paginated endpoints. */
    const val DEFAULT_PAGE_LIMIT = 20
}
