package com.example.metroinfo.ui.arrival

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.R
import com.example.metroinfo.databinding.FragmentArrivalTimeBinding
import com.example.metroinfo.model.ArrivalTimeInfo
import com.example.metroinfo.ui.arrival.adapter.LineAdapter
import com.example.metroinfo.ui.arrival.adapter.StationCardAdapter
import com.example.metroinfo.viewmodel.ArrivalViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ArrivalTimeFragment : Fragment() {
    private var _binding: FragmentArrivalTimeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ArrivalViewModel by viewModels()
    private lateinit var lineAdapter: LineAdapter
    private lateinit var stationAdapter: StationCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArrivalTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerViews()
        setupSearch()
        setupObservers()
        loadInitialData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.title = getString(R.string.arrival_time)
    }

    private fun setupRecyclerViews() {
        lineAdapter = LineAdapter { line ->
            viewModel.selectLine(line.lineId)
        }
        stationAdapter = StationCardAdapter { station ->
            station.stationId.toIntOrNull()?.let { stationId ->
                findNavController().navigate(
                    ArrivalTimeFragmentDirections.actionArrivalTimeToStationDetail(stationId)
                )
            }
        }

        binding.lineList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lineAdapter
        }

        binding.stationList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stationAdapter
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchStations(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.lines.collectLatest { lines ->
                lineAdapter.submitList(lines)
            }
        }

        lifecycleScope.launch {
            viewModel.stations.collectLatest { stations ->
                stationAdapter.submitList(stations)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let { errorMessage ->
                    binding.errorText.text = errorMessage
                    binding.errorText.visibility = View.VISIBLE
                } ?: run {
                    binding.errorText.visibility = View.GONE
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModel.loadLines()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 