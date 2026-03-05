package ru.netology.nmedia.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AuthState(val id: Long = 0, val token: String? = null)

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        val id = prefs.getLong("id", 0)
        val token = prefs.getString("token", null)
        if (id != 0L && token != null) {
            _authState.value = AuthState(id, token)
        } else {
            _authState.value = AuthState()
        }
    }

    @Synchronized
    fun setAuth(id: Long, token: String) {
        _authState.postValue(AuthState(id, token))
        prefs.edit()
            .putLong("id", id)
            .putString("token", token)
            .apply()
    }

    @Synchronized
    fun removeAuth() {
        _authState.postValue(AuthState())
        prefs.edit().clear().apply()
    }
}
