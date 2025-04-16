package com.example.metroinfo.ui.line

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.R
import com.example.metroinfo.databinding.FragmentLineDetailBinding
import com.example.metroinfo.model.Line
import com.example.metroinfo.model.Station
import com.example.metroinfo.ui.arrival.ArrivalTimeActivity
import com.example.metroinfo.ui.route.RouteActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LineDetailFragment : Fragment() {

    private var _binding: FragmentLineDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LineDetailViewModel by viewModels()
    private val args: LineDetailFragmentArgs by navArgs()
    private val stationAdapter = StationAdapter { station ->
        try {
            val stationId = station.stationId.toInt()
            findNavController().navigate(
                LineDetailFragmentDirections.actionLineDetailToStationDetail(stationId)
            )
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid station ID", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLineDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        viewModel.loadLineDetail(args.lineId)
    }

    private fun setupViews() {
        binding.apply {
            stationsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = stationAdapter
            }
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.loadLineDetail(args.lineId)
            }
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefreshLayout.isRefreshing = state.isLoading
                    state.error?.let { error ->
                        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    }
                    state.line?.let { line ->
                        binding.toolbar.title = line.nameCn
                        try {
                            binding.lineColorView.setBackgroundColor(Color.parseColor(line.color))
                        } catch (e: IllegalArgumentException) {
                            binding.lineColorView.setBackgroundColor(Color.GRAY)
                        }
                    }
                    state.stations?.let { stations ->
                        stationAdapter.submitList(stations)
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