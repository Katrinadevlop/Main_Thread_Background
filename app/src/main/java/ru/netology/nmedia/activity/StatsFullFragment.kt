package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.netology.nmedia.databinding.FragmentStatsFullBinding

class StatsFullFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentStatsFullBinding.inflate(inflater, container, false)
        binding.stats.data = listOf(
            0.25F,
            0.25F,
            0.25F,
            0.25F,
        )
        return binding.root
    }
}
