package com.example.metroinfo.ui.route

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.FragmentRouteBinding
import com.example.metroinfo.model.PathSegment
import com.example.metroinfo.viewmodel.RouteViewModel
import com.example.metroinfo.ui.route.adapter.RouteStepAdapter
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteFragment : Fragment() {
    private var _binding: FragmentRouteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RouteViewModel by viewModels()
    private lateinit var routeStepAdapter: RouteStepAdapter
    
    // 路径规划策略
    enum class RouteStrategy {
        FASTEST, LEAST_TRANSFERS, LEAST_WALKING
    }
    
    private var currentStrategy = RouteStrategy.FASTEST

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
        setupChipGroup()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        routeStepAdapter = RouteStepAdapter()
        binding.rvRouteSegments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = routeStepAdapter
        }
    }
    
    private fun setupChipGroup() {
        binding.routeOptionsChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    binding.chipFastest.id -> {
                        currentStrategy = RouteStrategy.FASTEST
                    }
                    binding.chipLeastTransfers.id -> {
                        currentStrategy = RouteStrategy.LEAST_TRANSFERS
                    }
                    binding.chipLeastWalking.id -> {
                        currentStrategy = RouteStrategy.LEAST_WALKING
                    }
                }
                
                // 如果已经有搜索结果，自动重新搜索
                if (binding.routeSummary.root.visibility == View.VISIBLE) {
                    findRoute()
                }
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // 如果已经有搜索结果，刷新当前的路线
            if (binding.routeSummary.root.visibility == View.VISIBLE) {
                findRoute()
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            findRoute()
        }
        
        binding.btnSwap.setOnClickListener {
            // 交换起点和终点
            val fromStation = binding.etFromStation.text.toString()
            val toStation = binding.etToStation.text.toString()
            
            binding.etFromStation.setText(toStation)
            binding.etToStation.setText(fromStation)
            
            // 如果两个站点都有数据，自动搜索
            if (fromStation.isNotEmpty() && toStation.isNotEmpty()) {
                findRoute()
            }
        }
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
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
            
            // 根据当前选择的策略查找路线
            val strategyParam = when(currentStrategy) {
                RouteStrategy.FASTEST -> "fastest"
                RouteStrategy.LEAST_TRANSFERS -> "least_transfers"
                RouteStrategy.LEAST_WALKING -> "least_walking"
            }
            
            viewModel.findRoute(fromStation, toStation, strategyParam)
        } else {
            Toast.makeText(context, "请输入起点和终点站", Toast.LENGTH_SHORT).show()
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
            
            binding.swipeRefreshLayout.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading && routeStepAdapter.currentList.isEmpty()) View.VISIBLE else View.GONE
            if (isLoading && routeStepAdapter.currentList.isNotEmpty()) {
                binding.swipeRefreshLayout.isRefreshing = true
            }
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
            
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 