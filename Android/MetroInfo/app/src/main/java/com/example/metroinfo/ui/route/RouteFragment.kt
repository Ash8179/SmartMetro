package com.example.metroinfo.ui.route

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.FragmentRouteBinding
import com.example.metroinfo.model.PathSegment
import com.example.metroinfo.viewmodel.RouteViewModel
import com.example.metroinfo.ui.route.adapter.RouteStepAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteFragment : Fragment() {
    private var _binding: FragmentRouteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RouteViewModel by viewModels()
    private lateinit var routeStepAdapter: RouteStepAdapter

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
        routeStepAdapter = RouteStepAdapter()
        binding.rvRouteSegments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = routeStepAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            findRoute()
        }
    }

    private fun findRoute() {
        val fromStation = binding.etFromStation.text.toString()
        val toStation = binding.etToStation.text.toString()
        
        if (fromStation.isNotEmpty() && toStation.isNotEmpty()) {
            // 清除之前的数据
            binding.routeSummary.root.visibility = View.GONE
            binding.tvError.visibility = View.GONE
            routeStepAdapter.submitList(emptyList())
            
            viewModel.findRoute(fromStation, toStation)
        }
    }

    private fun observeViewModel() {
        viewModel.routeData.observe(viewLifecycleOwner) { routeData ->
            routeData?.let {
                with(binding.routeSummary) {
                    tvDepartureStation.text = it.path.firstOrNull()?.from?.nameCn ?: "未知"
                    tvArrivalStation.text = it.path.lastOrNull()?.to?.nameCn ?: "未知"
                    tvTotalTime.text = "${it.totalTime}分钟"
                    tvTransferCount.text = "${it.transferCount}次"
                    root.visibility = View.VISIBLE
                }
                routeStepAdapter.submitList(it.path)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.tvError.apply {
                text = error
                visibility = if (error != null) View.VISIBLE else View.GONE
            }
            if (error != null) {
                binding.routeSummary.root.visibility = View.GONE
                routeStepAdapter.submitList(emptyList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 