package com.example.metroinfo.ui.nearby

import android.Manifest
import android.content.pm.PackageManager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.metroinfo.databinding.FragmentNearbyMetroBinding
import com.example.metroinfo.utils.LocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.metroinfo.data.model.NearbyStation

@AndroidEntryPoint
class NearbyMetroFragment : Fragment() {
    private var _binding: FragmentNearbyMetroBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NearbyStationsViewModel by viewModels()
    private lateinit var locationHelper: LocationHelper
    private val adapter = NearbyMetroAdapter()
    
    // 定义权限请求启动器
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getLocation()
            } else {
                // 即使没有位置权限，也加载默认的附近站点
                viewModel.loadNearbyStations()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNearbyMetroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationHelper = LocationHelper(requireContext())

        binding.nearbyMetroList.layoutManager = LinearLayoutManager(context)
        binding.nearbyMetroList.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // 观察 ViewModel 状态
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.nearbyStations.collectLatest { stations ->
                adapter.submitList(stations)
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    // 可以在这里显示错误信息
                }
            }
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 使用新的权限请求API
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLocation()
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            lifecycleScope.launch {
                try {
                    val locationResult = locationHelper.getCurrentLocation(1500)
                    if (locationResult != null) {
                        // 获取到位置，可以根据位置信息加载附近站点
                        viewModel.loadNearbyStationsWithLocation(locationResult.latitude, locationResult.longitude)
                    } else {
                        // 获取位置失败，加载默认的附近站点
                        viewModel.loadNearbyStations()
                    }
                } catch (e: Exception) {
                    // 定位异常，加载默认的附近站点
                    viewModel.loadNearbyStations()
                }
            }
        } else {
            // 没有权限，加载默认的附近站点
            viewModel.loadNearbyStations()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 