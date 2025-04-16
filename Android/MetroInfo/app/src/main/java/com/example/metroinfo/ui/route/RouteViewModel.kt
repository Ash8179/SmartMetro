package com.example.metroinfo.ui.route

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.RouteInfo
import com.example.metroinfo.model.Station
import com.example.metroinfo.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {

    private val _routeInfo = MutableLiveData<RouteInfo>()
    val routeInfo: LiveData<RouteInfo> = _routeInfo

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _stations = MutableLiveData<List<Station>>()
    val stations: LiveData<List<Station>> = _stations

    private val _bestRoute = MutableLiveData<RouteInfo?>()
    val bestRoute: LiveData<RouteInfo?> = _bestRoute

    private var selectedStartStationId: Int? = null
    private var selectedEndStationId: Int? = null

    init {
        loadStations()
    }

    private fun loadStations() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val stations = repository.getStations()
                _stations.value = stations
            } catch (e: Exception) {
                _error.value = "加载站点列表失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onStartStationSelected(station: Station) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                selectedStartStationId = station.stationId.toInt()
                
                // 如果已经选择了终点站，则查询路线
                selectedEndStationId?.let { endStationId ->
                    queryRoutes(station.stationId.toInt(), endStationId)
                }
            } catch (e: Exception) {
                _error.value = "选择起点站失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onEndStationSelected(station: Station) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                selectedEndStationId = station.stationId.toInt()
                
                // 如果已经选择了起点站，则查询路线
                selectedStartStationId?.let { startStationId ->
                    queryRoutes(startStationId, station.stationId.toInt())
                }
            } catch (e: Exception) {
                _error.value = "选择终点站失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun queryRoutes(startStationId: Int, endStationId: Int) {
        try {
            val response = repository.getBestRoute(startStationId, endStationId)
            _bestRoute.value = response
        } catch (e: Exception) {
            _error.value = "查询路线失败：${e.message}"
        }
    }

    fun findRoute(startStation: String, endStation: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // 从站点列表中查找对应的站点ID
                val stations = _stations.value ?: return@launch
                val startStationId = stations.find { it.nameCn == startStation }?.stationId?.toInt()
                val endStationId = stations.find { it.nameCn == endStation }?.stationId?.toInt()

                if (startStationId == null || endStationId == null) {
                    _error.value = "找不到对应的站点"
                    return@launch
                }

                val response = repository.findRoute(startStationId, endStationId)
                if (response?.success == true && response.data != null) {
                    _routeInfo.value = RouteInfo(
                        segments = response.data.path,
                        totalTime = response.data.totalTime,
                        transferCount = response.data.transferCount
                    )
                } else {
                    _error.value = response?.message ?: "查询路线失败"
                }
            } catch (e: Exception) {
                _error.value = "查询路线失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 