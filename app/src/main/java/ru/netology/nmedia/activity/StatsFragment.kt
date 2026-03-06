package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.netology.nmedia.databinding.FragmentStatsBinding

class StatsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentStatsBinding.inflate(inflater, container, false)
        binding.stats.maxValue = 2000F
        binding.stats.data = listOf(
            500F,
            500F,
            500F,
        )
        return binding.root
    }
}
