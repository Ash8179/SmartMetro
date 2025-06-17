package com.example.metroinfo.ui.arrival

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.R
import com.example.metroinfo.databinding.ActivityArrivalTimeBinding
import com.example.metroinfo.model.ArrivalTimeInfo
import com.example.metroinfo.ui.arrival.adapter.LineAdapter
import com.example.metroinfo.ui.arrival.adapter.StationCardAdapter
import com.example.metroinfo.util.AnimationUtils
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
        setupSwipeRefresh()
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

        // 计算适合当前屏幕的网格列数
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val columnCount = when {
            screenWidthDp >= 840 -> 3  // 大屏设备显示3列
            screenWidthDp >= 600 -> 2  // 中等屏幕显示2列
            else -> 1                  // 小屏幕显示1列
        }

        binding.stationList.apply {
            layoutManager = GridLayoutManager(this@ArrivalTimeActivity, columnCount)
            adapter = stationAdapter
            
            // 添加卡片间距装饰器
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val spacing = resources.getDimensionPixelSize(R.dimen.card_spacing)
                    val position = parent.getChildAdapterPosition(view)
                    val column = position % columnCount
                    
                    // 设置左右间距
                    outRect.left = spacing - column * spacing / columnCount
                    outRect.right = (column + 1) * spacing / columnCount
                    
                    // 如果不是第一行，设置顶部间距
                    if (position >= columnCount) {
                        outRect.top = spacing
                    }
                }
            })
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchStations(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrBlank() && newText.length >= 2) {
                    viewModel.searchStations(newText)
                }
                return true
            }
        })
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary_blue,
            R.color.primary_blue_dark,
            R.color.primary_blue_light
        )
        
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }

    private fun refreshData() {
        val selectedLineId = viewModel.selectedLineId.value
        if (selectedLineId != null) {
            viewModel.selectLine(selectedLineId)
        } else {
            viewModel.loadLines()
        }
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
                binding.swipeRefresh.isRefreshing = false
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading && !binding.swipeRefresh.isRefreshing) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                binding.swipeRefresh.isRefreshing = false
                error?.let { errorMessage ->
                    binding.errorText.text = errorMessage
                    binding.errorText.visibility = android.view.View.VISIBLE
                    Toast.makeText(this@ArrivalTimeActivity, errorMessage, Toast.LENGTH_SHORT).show()
                } ?: run {
                    binding.errorText.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun loadInitialData() {
        // 如果有传入的线路ID，直接加载该线路的站点
        intent.getIntExtra(EXTRA_LINE_ID, -1).takeIf { it != -1 }?.let { lineId ->
            viewModel.selectLine(lineId)
        } ?: run {
            viewModel.loadLines()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                AnimationUtils.applyActivityTransition(this, AnimationUtils.AnimationType.SLIDE_LEFT)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        AnimationUtils.applyActivityTransition(this, AnimationUtils.AnimationType.SLIDE_LEFT)
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