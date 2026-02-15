package com.streaming.sunne.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * OkHttp interceptor that:
 *  1. Attaches "Authorization: Bearer <token>" to every request if a token exists.
 *  2. On 401, attempts a token refresh and retries the original request once.
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenManager.accessToken
        val request = chain.request().withBearerToken(token)
        val response = chain.proceed(request)

        // Token expired — try to refresh
        if (response.code == 401 && TokenManager.refreshToken != null) {
            response.close()

            val refreshed = tryRefreshToken()
            return if (refreshed) {
                // Retry original request with new token
                chain.proceed(chain.request().withBearerToken(TokenManager.accessToken))
            } else {
                // Refresh failed — clear tokens and return original 401
                TokenManager.clear()
                chain.proceed(chain.request())
            }
        }

        return response
    }

    /** Synchronously calls the refresh endpoint */
    private fun tryRefreshToken(): Boolean {
        return try {
            val refreshToken = TokenManager.refreshToken ?: return false

            // Build a minimal client (no interceptor to avoid recursion)
            val client = OkHttpClient.Builder().build()
            val url = "${RetrofitClient.BASE_URL}api/v1/auth/refresh?refresh_token=$refreshToken"
            val req = Request.Builder().url(url).post(okhttp3.RequestBody.create(null, ByteArray(0))).build()
            val resp = client.newCall(req).execute()

            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return false
                val token = com.google.gson.Gson().fromJson(body, com.streaming.sunne.data.model.Token::class.java)
                TokenManager.saveTokens(token.accessToken, token.refreshToken)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun Request.withBearerToken(token: String?): Request {
        return if (token != null) {
            newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else this
    }
}

/**
 * Singleton Retrofit client.
 * Update [BASE_URL] to point to your FastAPI server.
 */
object RetrofitClient {

    // ── ✏️ Change this to your server URL ─────────────────────────────────────
    // Android emulator  → "http://10.0.2.2:8000/"
    // Physical device   → "http://192.168.1.XXX:8000/"
    // Production        → "https://api.sunetv.com/"
    const val BASE_URL = "http://192.168.100.8:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())        // JWT auth
            .addInterceptor(loggingInterceptor)       // debug logs
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)        // longer for streaming
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
