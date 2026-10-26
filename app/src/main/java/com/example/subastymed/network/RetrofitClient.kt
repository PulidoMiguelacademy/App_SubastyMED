package com.example.subastymed.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.content.Context
import com.google.gson.Gson

object SessionManager {
    private const val PREFS_NAME = "subastymed_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_JSON = "user_json"

    var authToken: String? = null
    var currentUser: UserDto? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        authToken = prefs.getString(KEY_TOKEN, null)
        val userJson = prefs.getString(KEY_USER_JSON, null)
        if (!userJson.isNullOrEmpty()) {
            try {
                currentUser = Gson().fromJson(userJson, UserDto::class.java)
            } catch (e: Exception) {
                Log.e("SessionManager", "Error parsing saved user: ${e.message}")
            }
        }
    }

    fun saveSession(context: Context, token: String, user: UserDto) {
        authToken = token
        currentUser = user
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_JSON, Gson().toJson(user))
            .apply()
    }

    fun clearSession(context: Context) {
        authToken = null
        currentUser = null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return !authToken.isNullOrEmpty() && currentUser != null
    }
}

object RetrofitClient {

    /**
     * URL BASE PRINCIPAL:
     * - Por defecto: 127.0.0.1:8000 (funciona en celular físico con cable USB mediante 'adb reverse')
     * - Si falla, el interceptor inteligente intenta automáticamente con la IP Wi-Fi de tu PC (192.168.80.10:8000)
     *   y con el emulador (10.0.2.2:8000).
     */
    var BASE_URL: String = "http://127.0.0.1:8000/"
        private set

    fun updateBaseUrl(newUrl: String) {
        BASE_URL = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        rebuildRetrofit()
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        SessionManager.authToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    // Interceptor inteligente que prueba hosts alternativos si uno no responde
    private val smartFallbackInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            chain.proceed(request)
        } catch (e: IOException) {
            val originalHost = request.url.host
            val alternateHosts = listOf("127.0.0.1", "192.168.80.10", "10.0.2.2")
                .filter { it != originalHost }

            var lastException: IOException = e
            for (altHost in alternateHosts) {
                try {
                    Log.d("RetrofitClient", "Reintentando conexión con host alternativo: $altHost")
                    val newUrl = request.url.newBuilder().host(altHost).build()
                    val newRequest = request.newBuilder().url(newUrl).build()
                    return@Interceptor chain.proceed(newRequest)
                } catch (retryEx: IOException) {
                    lastException = retryEx
                }
            }
            throw lastException
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(smartFallbackInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var retrofit: Retrofit = buildRetrofit()

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun rebuildRetrofit() {
        retrofit = buildRetrofit()
        apiService = retrofit.create(ApiService::class.java)
    }

    var apiService: ApiService = retrofit.create(ApiService::class.java)
        private set
}
