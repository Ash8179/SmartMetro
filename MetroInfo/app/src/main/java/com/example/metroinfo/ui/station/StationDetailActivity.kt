package com.example.metroinfo.ui.station

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.metroinfo.databinding.ActivityStationDetailBinding
import com.example.metroinfo.ui.station.viewmodel.StationDetailViewModel
import com.example.metroinfo.utils.NavigationHelper
import com.example.metroinfo.utils.LocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class StationDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStationDetailBinding
    private val viewModel: StationDetailViewModel by viewModels()
    private lateinit var navigationHelper: NavigationHelper
    private lateinit var locationHelper: LocationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化工具类
        navigationHelper = NavigationHelper(this)
        locationHelper = LocationHelper(this)

        // 设置工具栏
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 获取传递的站点ID
        val stationId = intent.getIntExtra(EXTRA_STATION_ID, -1)
        Timber.d("收到站点ID: $stationId")
        
        if (stationId == -1) {
            Timber.e("无效的站点ID")
            Toast.makeText(this, "无效的站点ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 加载站点详情
        Timber.d("开始加载站点详情")
        viewModel.loadStationDetail(stationId)

        // 观察数据变化
        lifecycleScope.launch {
            viewModel.stationDetail.collectLatest { station ->
                station?.let {
                    Timber.d("收到站点详情数据: ${it.name}")
                    try {
                        binding.apply {
                            // 更新基本信息
                            stationNameText.text = it.name
                            stationNameEnText.text = it.nameEn
                            lineInfoText.text = "线路：${it.lines.joinToString("、") { line -> "${line}号线" }}"
                            locationText.text = "位置：东经${it.longitude}，北纬${it.latitude}"

                            // 更新运营时间
                            firstTrainText.text = "首班车: ${it.firstTrain}"
                            lastTrainText.text = "末班车: ${it.lastTrain}"
                            exitCountText.text = "出口: ${it.exitCount}个"
                            
                            // 设置出口点击事件
                            exitCountText.setOnClickListener { _ ->
                                openStationEntrances(stationId, it.name)
                            }

                            // 更新设施信息
                            try {
                                facilitiesContainer.removeAllViews()
                                it.facilities?.forEach { facility ->
                                    val chip = com.google.android.material.chip.Chip(this@StationDetailActivity)
                                    chip.text = facility
                                    facilitiesContainer.addView(chip)
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error updating facilities")
                            }

                            // 显示内容
                            contentLayout.visibility = View.VISIBLE
                            progressBar.visibility = View.GONE
                            errorLayout.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error updating UI with station data")
                        binding.errorLayout.visibility = View.VISIBLE
                        binding.errorText.text = "更新界面失败"
                        binding.contentLayout.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        // 观察加载状态
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                Timber.d("加载状态变化: $isLoading")
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
        }

        // 观察错误状态
        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Timber.e("发生错误: $it")
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.errorText.text = it
                    binding.contentLayout.visibility = View.GONE
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        // 设置重试按钮
        binding.retryButton.setOnClickListener {
            Timber.d("点击重试按钮")
            viewModel.loadStationDetail(stationId)
        }

        // 设置查看到站信息按钮
        binding.btnCheckArrival.setOnClickListener {
            // TODO: 实现查看到站信息功能
            Toast.makeText(this, "查看到站信息功能开发中", Toast.LENGTH_SHORT).show()
        }

        // 设置导航按钮
        binding.btnNavigate.setOnClickListener {
            openNavigation()
        }
    }
    
    private fun openNavigation() {
        val currentStation = viewModel.stationDetail.value
        if (currentStation == null) {
            Toast.makeText(this, "站点信息未加载完成", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 检查站点是否有位置信息
        val stationLat = currentStation.latitude
        val stationLng = currentStation.longitude
        if (stationLat == null || stationLng == null) {
            Toast.makeText(this, "该站点没有位置信息", Toast.LENGTH_SHORT).show()
            Timber.w("站点 ${currentStation.name} 没有位置信息")
            return
        }
        
        try {
            // 检查是否有可用的地图应用
            if (!navigationHelper.hasMapApp()) {
                Toast.makeText(this, "未找到可用的地图应用", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 显示已安装的地图应用
            val installedApps = navigationHelper.getInstalledMapApps()
            if (installedApps.isNotEmpty()) {
                val appsText = installedApps.joinToString("、")
                Timber.d("检测到已安装的地图应用: $appsText")
            }
            
            // 尝试获取当前位置用于导航
            lifecycleScope.launch {
                try {
                    val currentLocation = locationHelper.getCurrentLocation(1000) // 1秒超时
                    if (currentLocation != null) {
                        // 有当前位置，进行路线规划
                        Timber.d("使用当前位置进行导航: ${currentLocation.latitude}, ${currentLocation.longitude}")
                        navigationHelper.navigateTo(
                            latitude = stationLat,
                            longitude = stationLng,
                            destinationName = "${currentStation.name}地铁站",
                            currentLatitude = currentLocation.latitude,
                            currentLongitude = currentLocation.longitude
                        )
                    } else {
                        // 没有当前位置，直接导航到目的地
                        Timber.d("无法获取当前位置，直接导航到目的地")
                        navigationHelper.navigateTo(
                            latitude = stationLat,
                            longitude = stationLng,
                            destinationName = "${currentStation.name}地铁站"
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "获取当前位置失败，直接导航到目的地")
                    // 获取位置失败，直接导航到目的地
                    navigationHelper.navigateTo(
                        latitude = stationLat,
                        longitude = stationLng,
                        destinationName = "${currentStation.name}地铁站"
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error opening navigation: ${currentStation.name}")
            Toast.makeText(this, "打开导航失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStationEntrances(stationId: Int, stationName: String) {
        try {
            val intent = StationEntrancesActivity.newIntent(this, stationId, stationName)
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Error opening station entrances: $stationName")
            Toast.makeText(this, "打开出口信息失败", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_STATION_ID = "extra_station_id"
    }
}
 