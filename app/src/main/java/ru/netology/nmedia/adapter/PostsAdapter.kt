package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.databinding.ItemLoadingBinding
import ru.netology.nmedia.databinding.ItemSeparatorBinding
import ru.netology.nmedia.dto.Post

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
}
sealed interface FeedItem {
    val id: Long

    data class PostItem(val post: Post) : FeedItem {
        override val id: Long = post.id
    }

    data class LoadingItem(val type: LoadingType) : FeedItem {
        override val id: Long = type.id
    }

    data class SeparatorItem(val type: SeparatorType) : FeedItem {
        override val id: Long = type.id
    }
}

enum class LoadingType(val id: Long) {
    PREPEND(-1L),
    APPEND(-2L),
}

enum class SeparatorType(val id: Long, val titleRes: Int) {
    TODAY(-3L, R.string.separator_today),
    YESTERDAY(-4L, R.string.separator_yesterday),
    LAST_WEEK(-5L, R.string.separator_last_week),
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : ListAdapter<FeedItem, RecyclerView.ViewHolder>(FeedItemDiffCallback()) {
    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is FeedItem.PostItem -> R.layout.card_post
        is FeedItem.LoadingItem -> R.layout.item_loading
        is FeedItem.SeparatorItem -> R.layout.item_separator
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.card_post -> {
                val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, onInteractionListener)
            }
            R.layout.item_separator -> {
                val binding = ItemSeparatorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SeparatorViewHolder(binding)
            }
            else -> {
                val binding = ItemLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LoadingViewHolder(binding.root)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FeedItem.PostItem -> (holder as PostViewHolder).bind(item.post)
            is FeedItem.LoadingItem -> Unit
            is FeedItem.SeparatorItem -> (holder as SeparatorViewHolder).bind(item.type)
        }
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = post.published
            content.text = post.content
            // в адаптере
            like.isChecked = post.likedByMe
            like.text = "${post.likes}"

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }

            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
        }
    }
}

class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)
class SeparatorViewHolder(
    private val binding: ItemSeparatorBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(type: SeparatorType) {
        binding.text.text = binding.root.context.getString(type.titleRes)
    }
}

class FeedItemDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean =
        oldItem.id == newItem.id && oldItem::class == newItem::class

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean = when {
        oldItem is FeedItem.PostItem && newItem is FeedItem.PostItem -> oldItem.post == newItem.post
        oldItem is FeedItem.LoadingItem && newItem is FeedItem.LoadingItem -> oldItem.type == newItem.type
        oldItem is FeedItem.SeparatorItem && newItem is FeedItem.SeparatorItem -> oldItem.type == newItem.type
        else -> false
    }
}
