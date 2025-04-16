package com.example.metroinfo.ui.station

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.FragmentStationDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StationDetailFragment : Fragment() {

    private var _binding: FragmentStationDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StationDetailViewModel by viewModels()
    private lateinit var adapter: ArrivalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        arguments?.getString("stationId")?.let { _ ->
            viewModel.loadStationDetails()
        } ?: run {
            Toast.makeText(context, "无效的站点ID", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = ArrivalAdapter()
        binding.arrivalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@StationDetailFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadStationDetails()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.station.collectLatest { station ->
                    station?.let {
                        binding.apply {
                            toolbar.title = it.nameCn
                            stationNameTextView.text = it.nameCn
                            lineNameTextView.apply {
                                text = it.lineId.toString()
                                setBackgroundColor(android.graphics.Color.parseColor("#FF0000")) // 临时使用红色
                            }
                        }
                    }
                }
            }

            launch {
                viewModel.arrivals.collectLatest { arrivals ->
                    adapter.submitList(arrivals)
                }
            }

            launch {
                viewModel.isLoading.collectLatest { isLoading ->
                    binding.progressBar.isVisible = isLoading && adapter.currentList.isEmpty()
                    binding.swipeRefreshLayout.isRefreshing = isLoading && adapter.currentList.isNotEmpty()
                }
            }

            launch {
                viewModel.error.collectLatest { error ->
                    binding.errorTextView.apply {
                        isVisible = error != null
                        text = error
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 