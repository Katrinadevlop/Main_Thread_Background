package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val client: OkHttpClient,
    private val gson: Gson,
) : ViewModel() {
    val authState: LiveData<AuthState> = appAuth.authState

    private val _authError = SingleLiveEvent<String>()
    val authError: LiveData<String> = _authError

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
    }

    val isAuthenticated: Boolean
        get() = appAuth.authState.value?.token != null

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    fun login(login: String, pass: String) {
        scope.launch {
            try {
                val formBody = FormBody.Builder()
                    .add("login", login)
                    .add("pass", pass)
                    .build()

                val request = Request.Builder()
                    .url("${BASE_URL}/api/users/authentication")
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _authError.postValue("Login failed: ${response.code}")
                        return@launch
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
        scope.launch {
            try {
                val formBody = FormBody.Builder()
                    .add("login", login)
                    .add("pass", pass)
                    .add("name", name)
                    .build()

                val request = Request.Builder()
                    .url("${BASE_URL}/api/users/registration")
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _authError.postValue("Registration failed: ${response.code}")
                        return@launch
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
