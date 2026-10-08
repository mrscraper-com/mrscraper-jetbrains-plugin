package com.mrscraper.mcp

import com.intellij.util.io.HttpRequests
import java.io.IOException

/** Checks an API key with the MrScraper API before it is saved. */
internal object ApiKeyCheck {
    sealed interface Result {
        data object Valid : Result
        data object Rejected : Result
        data class Failed(val reason: String) : Result
    }

    fun check(apiKey: String): Result = try {
        val status = HttpRequests.request(MrScraperMcp.VERIFY_URL)
            .accept("application/json")
            .productNameAsUserAgent()
            .connectTimeout(15_000)
            .readTimeout(30_000)
            .throwStatusCodeException(false)
            .tuner { connection ->
                connection.setRequestProperty("x-api-token", apiKey)
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            .tryConnect()
        when (status) {
            in 200..299 -> Result.Valid
            401, 403 -> Result.Rejected
            else -> Result.Failed("MrScraper returned HTTP $status.")
        }
    } catch (e: IOException) {
        Result.Failed(e.message ?: e.javaClass.simpleName)
    }
}
