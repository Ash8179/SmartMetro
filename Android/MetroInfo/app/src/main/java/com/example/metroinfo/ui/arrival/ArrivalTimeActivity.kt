package com.example.metroinfo.ui.arrival

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.R
import com.example.metroinfo.databinding.ActivityArrivalTimeBinding
import com.example.metroinfo.model.ArrivalTimeInfo
import com.example.metroinfo.ui.arrival.adapter.LineAdapter
import com.example.metroinfo.ui.arrival.adapter.StationCardAdapter
import com.example.metroinfo.viewmodel.ArrivalViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ArrivalTimeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArrivalTimeBinding
    private val viewModel: ArrivalViewModel by viewModels()
    private lateinit var lineAdapter: LineAdapter
    private lateinit var stationAdapter: StationCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArrivalTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        setupSearch()
        setupObservers()
        loadInitialData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.arrival_time)
        }
    }

    private fun setupRecyclerViews() {
        lineAdapter = LineAdapter { line ->
            viewModel.selectLine(line.lineId)
        }
        stationAdapter = StationCardAdapter { station ->
            station.stationId.toIntOrNull()?.let { viewModel.getStationArrivalInfo(it) }
        }

        binding.lineList.apply {
            layoutManager = LinearLayoutManager(this@ArrivalTimeActivity)
            adapter = lineAdapter
        }

        binding.stationList.apply {
            layoutManager = LinearLayoutManager(this@ArrivalTimeActivity)
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
                binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let { errorMessage ->
                    binding.errorText.text = errorMessage
                    binding.errorText.visibility = android.view.View.VISIBLE
                } ?: run {
                    binding.errorText.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModel.loadLines()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_LINE_ID = "line_id"

        fun newIntent(context: Context, lineId: Int): Intent {
            return Intent(context, ArrivalTimeActivity::class.java).apply {
                putExtra(EXTRA_LINE_ID, lineId)
            }
        }
    }
} 