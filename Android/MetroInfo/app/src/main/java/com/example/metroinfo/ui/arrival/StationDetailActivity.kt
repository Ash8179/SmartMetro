package com.example.metroinfo.ui.arrival

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.ActivityStationDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStationDetailBinding
    private val viewModel: StationDetailViewModel by viewModels()
    private lateinit var adapter: StationDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadData()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = StationDetailAdapter()
        binding.stationList.apply {
            layoutManager = LinearLayoutManager(this@StationDetailActivity)
            adapter = this@StationDetailActivity.adapter
        }
    }

    private fun loadData() {
        val stationId = intent.getIntExtra("stationId", -1)
        if (stationId != -1) {
            viewModel.loadArrivalInfo(stationId)
        } else {
            Toast.makeText(this, "无效的站点ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.arrivalInfo.collectLatest { arrivalInfo ->
                adapter.submitList(arrivalInfo)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Toast.makeText(this@StationDetailActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 