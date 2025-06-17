package com.example.metroinfo.ui.line

import android.content.Intent
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
            val stationId = station.stationId?.toIntOrNull()
            if (stationId != null) {
                findNavController().navigate(
                    LineDetailFragmentDirections.actionLineDetailToStationDetail(stationId)
                )
            } else {
                Toast.makeText(context, "Invalid station ID", Toast.LENGTH_SHORT).show()
            }
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
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        viewModel.loadLineDetail(args.lineId)
    }

    private fun setupRecyclerView() {
        binding.rvStations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stationAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnRoute.setOnClickListener {
            val startStation = stationAdapter.currentList.firstOrNull()
            val endStation = stationAdapter.currentList.lastOrNull()
            if (startStation != null && endStation != null) {
                val startStationId = startStation.stationId?.toIntOrNull()
                val endStationId = endStation.stationId?.toIntOrNull()
                if (startStationId != null && endStationId != null) {
                    val intent = Intent(requireContext(), RouteActivity::class.java).apply {
                        putExtra(RouteActivity.EXTRA_START_STATION_ID, startStationId)
                        putExtra(RouteActivity.EXTRA_END_STATION_ID, endStationId)
                    }
                    startActivity(intent)
                } else {
                    Snackbar.make(binding.root, "无效的站点ID", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(binding.root, "无法获取起始站和终点站", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnArrivalTime.setOnClickListener {
            val intent = ArrivalTimeActivity.newIntent(requireContext(), args.lineId)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.line?.let { updateUI(it) }
                    state.stations?.let { stationAdapter.submitList(it) }
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    state.error?.let { error ->
                        Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateUI(line: Line) {
        binding.tvLineName.text = line.nameCn
        binding.tvLineNameEn.text = line.nameEn
        binding.tvLineColor.setBackgroundColor(Color.parseColor(line.color))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 