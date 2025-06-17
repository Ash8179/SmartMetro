package com.example.metroinfo.ui.station

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.ActivityStationEntrancesBinding
import com.example.metroinfo.ui.station.adapter.StationEntrancesAdapter
import com.example.metroinfo.ui.station.viewmodel.StationEntrancesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class StationEntrancesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStationEntrancesBinding
    private val viewModel: StationEntrancesViewModel by viewModels()
    private lateinit var adapter: StationEntrancesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStationEntrancesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置工具栏
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 获取传递的站点ID
        val stationId = intent.getIntExtra(EXTRA_STATION_ID, -1)
        val stationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: "未知站点"
        
        if (stationId == -1) {
            Toast.makeText(this, "无效的站点ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 设置标题
        supportActionBar?.title = "${stationName}出口信息"

        // 设置RecyclerView
        setupRecyclerView()

        // 观察数据变化
        observeViewModel()

        // 加载出口数据
        viewModel.loadStationEntrances(stationId)
    }

    private fun setupRecyclerView() {
        adapter = StationEntrancesAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@StationEntrancesActivity)
            adapter = this@StationEntrancesActivity.adapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.entrancesData.collectLatest { data ->
                data?.let {
                    Timber.d("收到出口数据: ${it.entrances.size}个出口")
                    adapter.submitList(it.entrances)
                    
                    // 更新统计信息
                    binding.totalEntrancesText.text = "共${it.totalEntrances}个出口"
                    
                    // 显示内容
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                    binding.errorLayout.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (isLoading) {
                    binding.contentLayout.visibility = View.GONE
                    binding.errorLayout.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Timber.e("加载出口信息出错: $it")
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.errorText.text = it
                    binding.contentLayout.visibility = View.GONE
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        // 设置重试按钮
        binding.retryButton.setOnClickListener {
            val stationId = intent.getIntExtra(EXTRA_STATION_ID, -1)
            if (stationId != -1) {
                viewModel.loadStationEntrances(stationId)
            }
        }
    }

    companion object {
        const val EXTRA_STATION_ID = "extra_station_id"
        const val EXTRA_STATION_NAME = "extra_station_name"

        fun newIntent(context: Context, stationId: Int, stationName: String): Intent {
            return Intent(context, StationEntrancesActivity::class.java).apply {
                putExtra(EXTRA_STATION_ID, stationId)
                putExtra(EXTRA_STATION_NAME, stationName)
            }
        }
    }
} 