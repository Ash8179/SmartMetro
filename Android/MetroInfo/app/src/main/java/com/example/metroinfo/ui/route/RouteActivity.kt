package com.example.metroinfo.ui.route

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.ActivityRouteBinding
import com.example.metroinfo.model.RouteData
import com.example.metroinfo.ui.route.adapter.RouteStepAdapter
import com.example.metroinfo.viewmodel.RouteViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRouteBinding
    private val viewModel: RouteViewModel by viewModels()
    private lateinit var routeStepAdapter: RouteStepAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
        
        // Get station IDs from intent
        val startStationId = intent.getIntExtra(EXTRA_START_STATION_ID, -1)
        val endStationId = intent.getIntExtra(EXTRA_END_STATION_ID, -1)
        
        if (startStationId != -1 && endStationId != -1) {
            findRoute(startStationId.toString(), endStationId.toString())
        } else {
            showError("Invalid station IDs")
        }
    }

    private fun setupUI() {
        routeStepAdapter = RouteStepAdapter()
        binding.rvRoute.apply {
            layoutManager = LinearLayoutManager(this@RouteActivity)
            adapter = routeStepAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.routeData.observe(this) { routeData ->
            routeData?.let {
                showRouteDetails(it)
                hideError()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let { showError(it) }
        }
    }

    private fun findRoute(startStationId: String, endStationId: String) {
        viewModel.findRoute(startStationId, endStationId)
    }

    private fun showRouteDetails(routeData: RouteData) {
        routeStepAdapter.submitList(routeData.path)
        
        val summary = buildString {
            append("总时间: ${routeData.totalTime}分钟\n")
            append("从: ${routeData.path.first().from?.nameCn ?: "未知"}\n")
            append("到: ${routeData.path.last().to?.nameCn ?: "未知"}\n")
            append("换乘次数: ${routeData.transferCount}")
        }
        binding.tvRouteSummary.text = summary
        binding.tvRouteSummary.visibility = View.VISIBLE
        binding.rvRoute.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        binding.tvRouteSummary.visibility = View.GONE
        binding.rvRoute.visibility = View.GONE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    companion object {
        const val EXTRA_START_STATION_ID = "start_station_id"
        const val EXTRA_END_STATION_ID = "end_station_id"

        @JvmStatic
        fun newIntent(context: Context, startStationId: Int, endStationId: Int): Intent {
            return Intent(context, RouteActivity::class.java).apply {
                putExtra(EXTRA_START_STATION_ID, startStationId)
                putExtra(EXTRA_END_STATION_ID, endStationId)
            }
        }
    }
} 