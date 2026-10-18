package com.yokaihub.agent.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class IpDetectorClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val ipEndpoints = listOf(
        "https://api.ipify.org",
        "https://ipv4.icanhazip.com",
        "https://checkip.amazonaws.com"
    )

    suspend fun fetchOutboundIpv4(): Result<String> = withContext(Dispatchers.IO) {
        for (url in ipEndpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "text/plain")
                    .header("User-Agent", "YokaiHub-Agent/1.0")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val ip = response.body?.string()?.trim().orEmpty()
                        if (ip.isNotEmpty() && ip.count { it == '.' } == 3) {
                            return@withContext Result.success(ip)
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to next IPv4 provider
            }
        }
        Result.failure(IllegalStateException("Unable to detect outbound IPv4. Check internet connection."))
    }
}
