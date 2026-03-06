package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(): List<Post>
    fun getLatest(count: Int): List<Post>
    fun getNewer(id: Long): List<Post>
    fun getBefore(id: Long, count: Int): List<Post>
    fun likeById(id: Long)
    fun save(post: Post)
    fun removeById(id: Long)
    fun dbIsEmpty(): Boolean
}
