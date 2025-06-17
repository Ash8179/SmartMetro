package com.example.metroinfo.ui.station

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.R
import com.example.metroinfo.databinding.FragmentStationDetailBinding
import com.example.metroinfo.ui.station.viewmodel.StationDetailViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

// 简单的到达时间数据类
data class SimpleArrivalInfo(
    val direction: String = "",
    val nextArrival: String = "",
    val minutesRemaining: Int = 0,
    val secondArrival: String = "",
    val intervalMinutes: Int = 0,
    val isOperating: Boolean = true,
    val serviceStatus: String? = null,
    val nextServiceTime: String? = null
)

@AndroidEntryPoint
class StationDetailFragment : Fragment() {

    private var _binding: FragmentStationDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StationDetailViewModel by viewModels()
    private lateinit var adapter: SimpleArrivalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 添加调试日志
        Timber.d("StationDetailFragment onViewCreated")
        Timber.d("Arguments: ${arguments}")
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupDirectionButton()
        observeViewModel()
        
        // 获取传递的站点ID，使用默认值-1来检测无效ID
        val stationId = arguments?.getInt("stationId", -1) ?: -1
        Timber.d("获取到的站点ID: $stationId")
        
        if (stationId != -1) {
            Timber.d("开始加载站点详情: $stationId")
            viewModel.loadStationDetail(stationId)
        } else {
            Timber.e("无效的站点ID: $stationId")
            Toast.makeText(context, "无效的站点ID", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = SimpleArrivalAdapter()
        binding.arrivalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@StationDetailFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            val stationId = arguments?.getInt("stationId", -1) ?: -1
            if (stationId != -1) {
                viewModel.loadStationDetail(stationId)
            }
        }
    }
    
    private fun setupDirectionButton() {
        binding.directionButton.setOnClickListener {
            // TODO: 实现方向切换功能
            Toast.makeText(context, "方向切换功能开发中", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupTransferLines(transferLines: List<Int>) {
        try {
            binding.transferLinesContainer.isVisible = transferLines.isNotEmpty()
            binding.transferLinesChipGroup.removeAllViews()
            
            transferLines.forEach { lineId ->
                try {
                    val chip = Chip(requireContext()).apply {
                        text = "${lineId}号线"
                        chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), getLineColor(lineId))
                        setTextColor(Color.WHITE)
                        isClickable = true
                        setOnClickListener {
                            Toast.makeText(context, "${text}换乘", Toast.LENGTH_SHORT).show()
                            // 这里可以导航到对应线路的详情页面
                        }
                    }
                    binding.transferLinesChipGroup.addView(chip)
                } catch (e: Exception) {
                    // 如果创建单个chip失败，跳过这个线路（在forEach中不能使用continue，直接返回）
                    return@forEach
                }
            }
        } catch (e: Exception) {
            // 如果设置换乘线路失败，隐藏容器
            binding.transferLinesContainer.isVisible = false
        }
    }

    private fun observeViewModel() {
        Timber.d("开始观察ViewModel")
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.stationDetail.collectLatest { station ->
                    Timber.d("收到站点详情数据: $station")
                    station?.let {
                        Timber.d("站点名称: ${it.name}")
                        try {
                            binding.apply {
                                toolbar.title = it.name
                                stationNameTextView.text = it.name
                                stationDescriptionTextView.text = it.nameEn ?: ""
                                lineNameTextView.apply {
                                    text = "${it.lines.firstOrNull() ?: 1}号线"
                                    
                                    // 设置线路标识的背景色
                                    val lineId = it.lines.firstOrNull() ?: 1
                                    val lineColor = getLineColor(lineId)
                                    try {
                                        setBackgroundColor(ContextCompat.getColor(requireContext(), lineColor))
                                    } catch (e: Exception) {
                                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                                    }
                                }
                                
                                // 设置换乘线路
                                try {
                                    if (it.lines.size > 1) {
                                        setupTransferLines(it.lines.drop(1)) // 除了主线路外的其他线路
                                    } else {
                                        binding.transferLinesContainer.isVisible = false
                                    }
                                } catch (e: Exception) {
                                    binding.transferLinesContainer.isVisible = false
                                }
                                
                                // 显示拥挤度信息
                                it.crowdingInfo?.let { crowdingInfo ->
                                    Timber.d("更新拥挤度信息: $crowdingInfo")
                                    updateCrowdingInfo(crowdingInfo)
                                }
                                
                                // 显示到达时间信息
                                it.arrivalTimes?.let { arrivalTimes ->
                                    Timber.d("更新到达时间信息: ${arrivalTimes.size}条记录")
                                    updateArrivalTimes(arrivalTimes)
                                }
                            }
                            Timber.d("UI更新完成")
                        } catch (e: Exception) {
                            Timber.e(e, "更新UI时出错")
                            // 如果更新UI失败，显示错误信息
                            binding.errorTextView.apply {
                                isVisible = true
                                text = "加载站点详情时出现错误"
                            }
                        }
                    }
                }
            }

            launch {
                viewModel.isLoading.collectLatest { isLoading ->
                    Timber.d("加载状态变化: $isLoading")
                    try {
                        binding.progressBar.isVisible = isLoading
                        binding.swipeRefreshLayout.isRefreshing = isLoading
                    } catch (e: Exception) {
                        // 忽略加载状态更新错误
                    }
                }
            }

            launch {
                viewModel.error.collectLatest { error ->
                    Timber.d("错误状态变化: $error")
                    try {
                        binding.errorTextView.apply {
                            isVisible = error != null
                            text = error ?: ""
                        }
                    } catch (e: Exception) {
                        // 忽略错误信息显示错误
                    }
                }
            }
        }
    }
    
    // 更新拥挤度信息显示
    private fun updateCrowdingInfo(crowdingInfo: com.example.metroinfo.data.model.CrowdingInfo) {
        try {
            binding.apply {
                // 根据人数计算拥挤度百分比
                val crowdingPercentage = when {
                    crowdingInfo.personCount <= 34 -> (crowdingInfo.personCount * 100) / 80 // 最大80人
                    crowdingInfo.personCount <= 59 -> 40 + ((crowdingInfo.personCount - 34) * 30) / 25
                    else -> 70 + ((crowdingInfo.personCount - 59) * 30) / 21
                }
                
                crowdingIndicator.progress = crowdingPercentage
                crowdingTextView.text = "${crowdingInfo.crowdStatus} (${crowdingInfo.personCount}人)"
                
                // 根据拥挤度级别设置颜色
                val indicatorColorRes = when (crowdingInfo.crowdLevel) {
                    0 -> R.color.success_green
                    1 -> R.color.warning_orange
                    2 -> R.color.error_red
                    else -> R.color.primary_blue
                }
                
                try {
                    crowdingIndicator.setIndicatorColor(ContextCompat.getColor(requireContext(), indicatorColorRes))
                } catch (e: Exception) {
                    // 如果设置颜色失败，使用默认颜色
                    crowdingIndicator.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                }
            }
        } catch (e: Exception) {
            // 如果更新拥挤度信息失败，显示默认信息
            binding.apply {
                crowdingIndicator.progress = 50
                crowdingTextView.text = "拥挤度信息暂不可用"
            }
        }
    }
    
    // 更新到达时间信息显示
    private fun updateArrivalTimes(arrivalTimes: List<com.example.metroinfo.data.model.ArrivalTime>) {
        try {
            // 创建适配器数据
            val arrivalInfoList = arrivalTimes.map { arrivalTime ->
                SimpleArrivalInfo(
                    direction = arrivalTime.direction,
                    nextArrival = arrivalTime.nextArrival,
                    minutesRemaining = arrivalTime.minutesRemaining,
                    secondArrival = arrivalTime.secondArrival,
                    intervalMinutes = arrivalTime.intervalMinutes,
                    isOperating = arrivalTime.isOperating ?: true,
                    serviceStatus = arrivalTime.serviceStatus,
                    nextServiceTime = arrivalTime.nextServiceTime
                )
            }
            adapter.submitList(arrivalInfoList)
        } catch (e: Exception) {
            // 如果更新到达时间失败，显示空列表
            adapter.submitList(emptyList())
        }
    }

    // 获取线路对应的颜色资源ID
    private fun getLineColor(lineId: Int): Int {
        return when (lineId) {
            1 -> R.color.line_1
            2 -> R.color.line_2
            3 -> R.color.line_3
            4 -> R.color.line_4
            5 -> R.color.line_5
            6 -> R.color.line_6
            7 -> R.color.line_7
            8 -> R.color.line_8
            9 -> R.color.line_9
            10 -> R.color.line_10
            11 -> R.color.line_11
            12 -> R.color.line_12
            13 -> R.color.line_13
            14 -> R.color.line_14
            15 -> R.color.line_15
            16 -> R.color.line_16
            17 -> R.color.line_17
            18 -> R.color.line_18
            else -> R.color.primary_blue
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 