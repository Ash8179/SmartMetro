package com.example.metroinfo.ui.nearby

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.R
import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.databinding.FragmentNearbyStationsBinding
import com.example.metroinfo.ui.adapter.NearbyStationsAdapter
import com.example.metroinfo.ui.station.StationDetailActivity
import com.example.metroinfo.ui.nearby.NearbyStationsViewModel
import com.example.metroinfo.utils.LocationHelper
import com.example.metroinfo.utils.NavigationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber


@AndroidEntryPoint
class NearbyStationsFragment : Fragment() {

    private var _binding: FragmentNearbyStationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NearbyStationsViewModel by viewModels()
    private lateinit var adapter: NearbyStationsAdapter
    private lateinit var locationHelper: LocationHelper
    private lateinit var navigationHelper: NavigationHelper
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Timber.d("权限请求结果: $isGranted")
        if (isGranted) {
            Timber.d("用户授予了定位权限")
            getCurrentLocationAndLoadStations()
        } else {
            // 权限被拒绝，使用测试坐标
            Timber.w("定位权限被拒绝，使用测试坐标")
            Toast.makeText(context, "权限被拒绝，使用同济大学测试坐标", Toast.LENGTH_SHORT).show()
            // 使用同济大学四平路校区的测试坐标
            viewModel.loadNearbyStationsWithLocation(31.2804, 121.5029)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNearbyStationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        locationHelper = LocationHelper(requireContext())
        navigationHelper = NavigationHelper(requireContext())
        
        setupRecyclerView()
        observeViewModel()
        loadNearbyStationsWithLocation()
    }

    private fun setupRecyclerView() {
        adapter = NearbyStationsAdapter(
            onStationClick = { station ->
                openStationDetail(station)
            },
            onRouteClick = { station ->
                openRoutePlanning(station)
            },
            onNavigateClick = { station ->
                openNavigation(station)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NearbyStationsFragment.adapter
        }
        
        // 设置下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadNearbyStationsWithLocation()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.swipeRefreshLayout.isRefreshing = isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.nearbyStations.collectLatest { stations ->
                Timber.d("次级页面收到附近站点数据: ${stations.size}个站点")
                adapter.submitList(stations)
                updateEmptyView(stations.isEmpty() && !viewModel.isLoading.value)
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Timber.e("次级页面加载附近站点出错: $it")
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    updateEmptyView(true)
                }
            }
        }
    }
    
    private fun loadNearbyStationsWithLocation() {
        Timber.d("次级页面开始加载附近站点（带定位）")
        checkLocationPermissionAndLoad()
    }
    
    private fun checkLocationPermissionAndLoad() {
        Timber.d("检查定位权限...")
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Timber.d("定位权限已授予，获取位置")
                getCurrentLocationAndLoadStations()
            }
            else -> {
                Timber.d("定位权限未授予，请求权限")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
    
    private fun getCurrentLocationAndLoadStations() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 权限未授予，使用同济大学测试坐标
            Toast.makeText(context, "需要定位权限，使用测试坐标", Toast.LENGTH_SHORT).show()
            viewModel.loadNearbyStationsWithLocation(31.2804, 121.5029)
            return
        }
        
        Timber.d("开始获取当前位置...")
        Toast.makeText(context, "正在获取您的位置...", Toast.LENGTH_SHORT).show()
        
        // 使用LocationHelper获取位置
        lifecycleScope.launch {
            try {
                val locationResult = locationHelper.getCurrentLocation(1500) // 1.5秒超时
                if (locationResult != null) {
                    Timber.d("=== 获取到位置信息 ===")
                    Timber.d("纬度: ${locationResult.latitude}")
                    Timber.d("经度: ${locationResult.longitude}")
                    locationResult.accuracy?.let { accuracy ->
                        Timber.d("精度: ${accuracy}米")
                    }
                    Toast.makeText(context, "获取到位置信息", Toast.LENGTH_SHORT).show()
                    viewModel.loadNearbyStationsWithLocation(locationResult.latitude, locationResult.longitude)
                } else {
                    Timber.w("定位失败，使用测试坐标")
                    Toast.makeText(context, "定位失败，使用测试坐标", Toast.LENGTH_SHORT).show()
                    viewModel.loadNearbyStationsWithLocation(31.2804, 121.5029)
                }
            } catch (e: Exception) {
                Timber.e(e, "定位过程中发生异常")
                Toast.makeText(context, "定位异常，使用测试坐标", Toast.LENGTH_SHORT).show()
                viewModel.loadNearbyStationsWithLocation(31.2804, 121.5029)
            }
        }
    }


    private fun openStationDetail(station: NearbyStation) {
        try {
            val intent = Intent(context, StationDetailActivity::class.java).apply {
                putExtra(StationDetailActivity.EXTRA_STATION_ID, station.stationId)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Error opening station detail: ${station.stationName}")
            Toast.makeText(context, R.string.error_opening_station_detail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openRoutePlanning(station: NearbyStation) {
        try {
            // 获取当前位置附近的站点作为起点
            lifecycleScope.launch {
                try {
                    val currentLocation = locationHelper.getCurrentLocation(1000)
                    if (currentLocation != null) {
                        // 找到最近的地铁站作为起点
                        val nearbyStations = viewModel.nearbyStations.value
                        if (nearbyStations.isNotEmpty()) {
                            // 使用最近的站点作为起点（通常是列表中的第一个）
                            val startStation = nearbyStations.first()
                            
                            // 如果起点和终点是同一个站点，提示用户
                            if (startStation.stationId == station.stationId) {
                                Toast.makeText(context, "您已经在${station.stationName}附近了", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            // 启动路线规划Activity，使用站点名称而不是ID
                            val intent = Intent(context, com.example.metroinfo.ui.route.RouteActivity::class.java).apply {
                                putExtra("EXTRA_START_STATION_NAME", startStation.stationName)
                                putExtra("EXTRA_END_STATION_NAME", station.stationName)
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(context, "无法获取附近站点信息", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 无法获取位置，让用户手动选择起点
                        Toast.makeText(context, "无法获取位置信息，请在路线规划页面手动选择起点", Toast.LENGTH_SHORT).show()
                        // 可以跳转到路线规划页面让用户手动选择
                        findNavController().navigate(R.id.action_nearbyStationsFragment_to_routeFragment)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "获取位置信息失败")
                    Toast.makeText(context, "获取位置信息失败，请在路线规划页面手动选择起点", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_nearbyStationsFragment_to_routeFragment)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error opening route planning: ${station.stationName}")
            Toast.makeText(context, "打开路线规划失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNavigation(station: NearbyStation) {
        try {
            station.latitude?.let { lat ->
                station.longitude?.let { lng ->
                    // 检查是否有可用的地图应用
                    if (!navigationHelper.hasMapApp()) {
                        Toast.makeText(context, "未找到可用的地图应用", Toast.LENGTH_SHORT).show()
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
                                    latitude = lat,
                                    longitude = lng,
                                    destinationName = "${station.stationName}地铁站",
                                    currentLatitude = currentLocation.latitude,
                                    currentLongitude = currentLocation.longitude
                                )
                            } else {
                                // 没有当前位置，直接导航到目的地
                                Timber.d("无法获取当前位置，直接导航到目的地")
                                navigationHelper.navigateTo(
                                    latitude = lat,
                                    longitude = lng,
                                    destinationName = "${station.stationName}地铁站"
                                )
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "获取当前位置失败，直接导航到目的地")
                            // 获取位置失败，直接导航到目的地
                            navigationHelper.navigateTo(
                                latitude = lat,
                                longitude = lng,
                                destinationName = "${station.stationName}地铁站"
                            )
                        }
                    }
                }
            } ?: run {
                Toast.makeText(context, "该站点没有位置信息", Toast.LENGTH_SHORT).show()
                Timber.w("站点 ${station.stationName} 没有位置信息")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error opening navigation: ${station.stationName}")
            Toast.makeText(context, "打开导航失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 