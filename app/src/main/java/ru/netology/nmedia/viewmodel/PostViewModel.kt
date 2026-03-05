package ru.netology.nmedia.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException
import kotlin.concurrent.thread
import javax.inject.Inject

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    appAuth: AppAuth,
) : ViewModel() {
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val authObserver = Observer<AuthState> {
        loadPosts()
    }

    init {
        appAuth.authState.observeForever(authObserver)
    }

    private val _appAuth = appAuth

    override fun onCleared() {
        super.onCleared()
        _appAuth.authState.removeObserver(authObserver)
    }

    fun loadPosts() {
        thread {
            // Начинаем загрузку (первичная загрузка)
            _data.postValue(FeedModel(loading = true))
            try {
                // Данные успешно получены
                val posts = repository.getAll()
                FeedModel(posts = posts, empty = posts.isEmpty())
            } catch (e: IOException) {
                // Получена ошибка
                FeedModel(error = true)
            }.also(_data::postValue)
        }
    }

    fun refreshPosts() {
        thread {
            // Обновление по свайпу вниз — используем флаг refreshing
            _data.postValue(_data.value?.copy(refreshing = true) ?: FeedModel(refreshing = true))
            try {
                val posts = repository.getAll()
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (e: IOException) {
                _data.postValue(FeedModel(error = true))
            }
        }
    }

    fun save() {
        edited.value?.let {
            thread {
                repository.save(it)
                _postCreated.postValue(Unit)
            }
        }
        edited.value = empty
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long) {
        thread {
            try {
                repository.likeById(id)
                // После успешного лайка/анлайка запрашиваем актуальный список постов
                val posts = repository.getAll()
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (e: IOException) {
                // В случае ошибки просто помечаем состояние как ошибочное
                _data.postValue(FeedModel(error = true))
            }
        }
    }

    fun removeById(id: Long) {
        thread {
            // Оптимистичная модель
            val old = _data.value?.posts.orEmpty()
            _data.postValue(
                _data.value?.copy(posts = _data.value?.posts.orEmpty()
                    .filter { it.id != id }
                )
            )
            try {
                repository.removeById(id)
            } catch (e: IOException) {
                _data.postValue(_data.value?.copy(posts = old))
            }
        }
    }
}
