package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import java.io.IOException
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val client: OkHttpClient,
    private val gson: Gson,
) : PostRepository {

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val postListType = object : TypeToken<List<Post>>() {}.type
    }

    override fun getAll(): List<Post> =
        dao.getAll().map(PostEntity::toDto)

    override fun getLatest(count: Int): List<Post> {
        val request = Request.Builder()
            .url("${BASE_URL}/api/posts/latest?count=$count")
            .build()

        val posts = executeAndParse(request)
        dao.insert(posts.map(PostEntity::fromDto))
        return dao.getAll().map(PostEntity::toDto)
    }

    override fun getNewer(id: Long): List<Post> {
        val request = Request.Builder()
            .url("${BASE_URL}/api/posts/$id/newer")
            .build()

        val posts = executeAndParse(request)
        if (posts.isNotEmpty()) {
            dao.insert(posts.map(PostEntity::fromDto))
        }
        return dao.getAll().map(PostEntity::toDto)
    }

    override fun getBefore(id: Long, count: Int): List<Post> {
        val request = Request.Builder()
            .url("${BASE_URL}/api/posts/$id/before?count=$count")
            .build()

        val posts = executeAndParse(request)
        if (posts.isNotEmpty()) {
            dao.insert(posts.map(PostEntity::fromDto))
        }
        return dao.getAll().map(PostEntity::toDto)
    }

    override fun likeById(id: Long) {
        dao.likeById(id)
    }

    override fun save(post: Post) {
        val entity = if (post.id == 0L) {
            PostEntity(
                id = 0L,
                author = "Me",
                content = post.content,
                published = "Now",
                likedByMe = false,
                likes = 0,
            )
        } else {
            PostEntity.fromDto(post)
        }
        dao.save(entity)
    }

    override fun removeById(id: Long) {
        dao.removeById(id)
    }

    override fun dbIsEmpty(): Boolean = dao.count() == 0

    private fun executeAndParse(request: Request): List<Post> {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty body")
            return gson.fromJson(body, postListType)
        }
    }
}
