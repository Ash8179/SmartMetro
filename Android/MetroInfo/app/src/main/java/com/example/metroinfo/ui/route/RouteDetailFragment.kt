package com.example.metroinfo.ui.route

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.FragmentRouteDetailBinding
import com.example.metroinfo.model.PathSegment
import com.example.metroinfo.viewmodel.RouteDetailViewModel
import com.example.metroinfo.ui.route.adapter.RouteStepAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteDetailFragment : Fragment() {
    private var _binding: FragmentRouteDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RouteDetailViewModel by viewModels()
    private val args: RouteDetailFragmentArgs by navArgs()
    private lateinit var routeStepAdapter: RouteStepAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        fetchRouteDetails()
    }

    private fun setupRecyclerView() {
        routeStepAdapter = RouteStepAdapter()
        binding.rvRouteSteps.apply {
            adapter = routeStepAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewModel.routeSteps.observe(viewLifecycleOwner, Observer<List<PathSegment>> { steps ->
            routeStepAdapter.submitList(steps)
        })

        viewModel.routeInfo.observe(viewLifecycleOwner, Observer<String> { info ->
            binding.tvRouteInfo.text = info
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer<Boolean> { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })
    }

    private fun fetchRouteDetails() {
        viewModel.fetchRouteDetails(args.startStation, args.endStation)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 