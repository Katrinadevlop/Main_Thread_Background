package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.FeedItem
import ru.netology.nmedia.adapter.LoadingType
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.adapter.SeparatorType
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
                val authenticated = authViewModel.isAuthenticated
                menu.findItem(R.id.signin).isVisible = !authenticated
                menu.findItem(R.id.signup).isVisible = !authenticated
                menu.findItem(R.id.signout).isVisible = authenticated
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.signin -> {
                        findNavController().navigate(R.id.action_feedFragment_to_loginFragment)
                        true
                    }
                    R.id.signup -> {
                        findNavController().navigate(R.id.action_feedFragment_to_registerFragment)
                        true
                    }
                    R.id.signout -> {
                        authViewModel.logout()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        authViewModel.authState.observe(viewLifecycleOwner) {
            requireActivity().invalidateOptionsMenu()
        }

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
            }

            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }
        })
        binding.list.adapter = adapter

        // PREPEND/APPEND: подгрузка при скролле вверх/вниз
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (dy < 0) {
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    if (firstVisible <= 2) {
                        viewModel.prependPosts()
                    }
                } else if (dy > 0) {
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    val total = adapter.itemCount
                    if (lastVisible >= total - 3) {
                        viewModel.appendPosts()
                    }
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        viewModel.data.observe(viewLifecycleOwner) { state ->
            val items = mutableListOf<FeedItem>()
            if (state.prependLoading) items += FeedItem.LoadingItem(LoadingType.PREPEND)
            items += withSeparators(state.posts)
            if (state.appendLoading) items += FeedItem.LoadingItem(LoadingType.APPEND)
            adapter.submitList(items)
            binding.progress.isVisible = state.loading && !state.refreshing
            binding.errorGroup.isVisible = state.error
            binding.emptyText.isVisible = state.empty
            binding.swipeRefresh.isRefreshing = state.refreshing
        }

        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        return binding.root
    }

    private fun withSeparators(posts: List<Post>): List<FeedItem> {
        if (posts.isEmpty()) return emptyList()
        val now = Instant.now()
        val result = ArrayList<FeedItem>(posts.size + 3)
        var lastSeparator: SeparatorType? = null
        for (post in posts) {
            val separator = resolveSeparatorType(post.published, now)
            if (separator != null && separator != lastSeparator) {
                result += FeedItem.SeparatorItem(separator)
                lastSeparator = separator
            }
            result += FeedItem.PostItem(post)
        }
        return result
    }

    private fun resolveSeparatorType(published: String, now: Instant): SeparatorType? {
        val instant = parsePublishedInstant(published, now) ?: return SeparatorType.LAST_WEEK
        val hours = Duration.between(instant, now).toHours()
        return when {
            hours < 24 -> SeparatorType.TODAY
            hours < 48 -> SeparatorType.YESTERDAY
            else -> SeparatorType.LAST_WEEK
        }
    }

    private fun parsePublishedInstant(value: String, now: Instant): Instant? {
        val text = value.trim()
        if (text.equals("now", true) || text.equals("сейчас", true)) return now

        runCatching { Instant.parse(text) }.getOrNull()?.let { return it }
        runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()?.let { return it }
        runCatching {
            LocalDateTime.parse(text).atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()?.let { return it }
        runCatching {
            LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()?.let { return it }
        runCatching {
            val numeric = text.toLong()
            if (numeric > 100_000_000_000L) Instant.ofEpochMilli(numeric) else Instant.ofEpochSecond(numeric)
        }.getOrNull()?.let { return it }
        return null
    }
}
