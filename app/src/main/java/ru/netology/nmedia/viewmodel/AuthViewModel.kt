package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException
import javax.inject.Inject
import kotlin.concurrent.thread

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val client: OkHttpClient,
    private val gson: Gson,
) : ViewModel() {
    val authState: LiveData<AuthState> = appAuth.authState

    private val _authError = SingleLiveEvent<String>()
    val authError: LiveData<String> = _authError

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
    }

    val isAuthenticated: Boolean
        get() = appAuth.authState.value?.token != null

    fun login(login: String, pass: String) {
        thread {
            try {
                val json = gson.toJson(mapOf("login" to login, "pass" to pass))
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${BASE_URL}/api/auth/login")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _authError.postValue("Login failed: ${response.code}")
                        return@thread
                    }
                    val body =
                        response.body?.string() ?: throw RuntimeException("Empty response body")
                    val token = gson.fromJson(body, Token::class.java)
                    appAuth.setAuth(token.id, token.token)
                }
            } catch (e: IOException) {
                _authError.postValue("Network error: ${e.message}")
            }
        }
    }

    fun register(login: String, pass: String, name: String) {
        thread {
            try {
                val json = gson.toJson(mapOf("login" to login, "pass" to pass, "name" to name))
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${BASE_URL}/api/auth/register")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _authError.postValue("Registration failed: ${response.code}")
                        return@thread
                    }
                    val body =
                        response.body?.string() ?: throw RuntimeException("Empty response body")
                    val token = gson.fromJson(body, Token::class.java)
                    appAuth.setAuth(token.id, token.token)
                }
            } catch (e: IOException) {
                _authError.postValue("Network error: ${e.message}")
            }
        }
    }

    fun logout() {
        appAuth.removeAuth()
    }
}
