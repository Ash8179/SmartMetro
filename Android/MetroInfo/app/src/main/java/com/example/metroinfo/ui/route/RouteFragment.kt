package com.example.metroinfo.ui.route

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.FragmentRouteBinding
import com.example.metroinfo.ui.adapter.RouteAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteFragment : Fragment() {

    private var _binding: FragmentRouteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RouteViewModel by viewModels()
    private lateinit var routeAdapter: RouteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        routeAdapter = RouteAdapter()
        binding.routeRecyclerView.apply {
            adapter = routeAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupClickListeners() {
        binding.searchButton.setOnClickListener {
            val startStation = binding.startStationInput.text.toString()
            val endStation = binding.endStationInput.text.toString()
            if (startStation.isNotEmpty() && endStation.isNotEmpty()) {
                viewModel.findRoute(startStation, endStation)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.routeInfo.observe(viewLifecycleOwner) { routeInfo ->
            routeInfo?.let {
                binding.routeSummaryCard.visibility = View.VISIBLE
                binding.totalTimeText.text = "总时间: ${it.totalTime}分钟"
                binding.transferCountText.text = "换乘: ${it.transferCount}次"
                routeAdapter.submitList(it.segments)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.searchButton.isEnabled = !isLoading
            // TODO: Add loading indicator
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.routeSummaryCard.visibility = View.GONE
                // TODO: Show error message
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 