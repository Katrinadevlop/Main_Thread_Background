package ru.netology.nmedia.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException
import javax.inject.Inject

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

private const val PAGE_SIZE = 10

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

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val authObserver = Observer<AuthState> {
        loadPosts(forceRemote = true)
    }

    init {
        appAuth.authState.observeForever(authObserver)
    }

    private val _appAuth = appAuth

    override fun onCleared() {
        super.onCleared()
        _appAuth.authState.removeObserver(authObserver)
        job.cancel()
    }

    /**
     * Первичная загрузка:
     * - Если БД пустая — загружаем latest с сервера.
     * - Если в БД есть данные — показываем их.
     */
    fun loadPosts(forceRemote: Boolean = false) {
        scope.launch {
            _data.postValue(FeedModel(loading = true))
            try {
                val posts = when {
                    forceRemote -> repository.getLatest(PAGE_SIZE)
                    repository.dbIsEmpty() -> repository.getLatest(PAGE_SIZE)
                    else -> repository.getAll()
                }
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (e: IOException) {
                _data.postValue(FeedModel(error = true))
            }
        }
    }

    /**
     * REFRESH: swipe-to-refresh — загружает посты новее верхнего в кеше
     * и добавляет их сверху (не затирая кеш).
     */
    fun refreshPosts() {
        scope.launch {
            val current = _data.value ?: FeedModel()
            _data.postValue(current.copy(refreshing = true))
            try {
                val topId = current.posts.firstOrNull()?.id
                val posts = if (topId != null) {
                    repository.getNewer(topId)
                } else {
                    repository.getLatest(PAGE_SIZE)
                }
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (e: IOException) {
                _data.postValue(current.copy(refreshing = false))
            }
        }
    }
    /**
     * PREPEND: подгрузка новых постов при скролле вверх.
     */
    fun prependPosts() {
        val current = _data.value ?: return
        if (current.prependLoading || current.refreshing || current.loading) return
        val topId = current.posts.firstOrNull()?.id ?: return

        scope.launch {
            _data.postValue(current.copy(prependLoading = true))
            try {
                val posts = repository.getNewer(topId)
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (e: IOException) {
                _data.postValue(current.copy(prependLoading = false))
            }
        }
    }

    /**
     * APPEND: подгрузка старых постов при скролле вниз.
     */
    fun appendPosts() {
        val current = _data.value ?: return
        if (current.appendLoading || current.refreshing || current.loading) return // уже грузим
        val bottomId = current.posts.lastOrNull()?.id ?: return

        scope.launch {
            _data.postValue(current.copy(appendLoading = true))
            try {
                val posts = repository.getBefore(bottomId, PAGE_SIZE)
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (e: IOException) {
                _data.postValue(current.copy(appendLoading = false))
            }
        }
    }

    fun save() {
        edited.value?.let {
            scope.launch {
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
        scope.launch {
            try {
                repository.likeById(id)
                val posts = repository.getAll()
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (e: IOException) {
                _data.postValue(FeedModel(error = true))
            }
        }
    }

    fun removeById(id: Long) {
        val current = _data.value ?: return
        val oldPosts = current.posts

        scope.launch {
            _data.postValue(current.copy(posts = current.posts.filter { it.id != id }))
            try {
                repository.removeById(id)
            } catch (e: IOException) {
                _data.postValue(current.copy(posts = oldPosts))
            }
        }
    }
}
