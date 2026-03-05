package ru.netology.nmedia.repository

import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
) : PostRepository {
    override fun getAll(): List<Post> =
        dao.getAll().map(PostEntity::toDto)

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
}
