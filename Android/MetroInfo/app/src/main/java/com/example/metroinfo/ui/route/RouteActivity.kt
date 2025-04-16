package com.example.metroinfo.ui.route

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.ActivityRouteBinding
import com.example.metroinfo.model.BestRouteResponse
import com.example.metroinfo.ui.route.adapter.RouteAdapter
import com.example.metroinfo.viewmodel.RouteViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRouteBinding
    private val viewModel: RouteViewModel by viewModels()
    private lateinit var routeAdapter: RouteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
        
        // Get station IDs from intent
        val startStationId = intent.getIntExtra("start_station_id", -1)
        val endStationId = intent.getIntExtra("end_station_id", -1)
        
        if (startStationId != -1 && endStationId != -1) {
            findRoute(startStationId, endStationId)
        } else {
            showError("Invalid station IDs")
        }
    }

    private fun setupUI() {
        routeAdapter = RouteAdapter()
        binding.routeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@RouteActivity)
            adapter = routeAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.bestRoute.observe(this) { response ->
            response?.let {
                if (it.success && it.data != null) {
                    showRouteDetails(it)
                    hideError()
                } else {
                    showError(it.message ?: "Failed to find route")
                }
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let { showError(it) }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun findRoute(startStationId: Int, endStationId: Int) {
        viewModel.findRoute(startStationId, endStationId)
    }

    private fun showRouteDetails(response: BestRouteResponse) {
        response.data?.let { routeData ->
            routeAdapter.submitList(routeData.path)
            
            val summary = buildString {
                append("Total time: ${routeData.totalTime} minutes\n")
                append("From: ${routeData.fromStation.nameCn}\n")
                append("To: ${routeData.toStation.nameCn}\n")
                append("Transfer count: ${routeData.transferCount}")
            }
            binding.routeSummaryTextView.text = summary
            binding.routeSummaryTextView.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        binding.errorTextView.text = message
        binding.errorTextView.visibility = View.VISIBLE
        binding.routeSummaryTextView.visibility = View.GONE
        binding.routeRecyclerView.visibility = View.GONE
    }

    private fun hideError() {
        binding.errorTextView.visibility = View.GONE
        binding.routeRecyclerView.visibility = View.VISIBLE
    }
} 