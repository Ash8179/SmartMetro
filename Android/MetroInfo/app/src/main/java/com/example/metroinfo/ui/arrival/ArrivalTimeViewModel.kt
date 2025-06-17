package com.example.metroinfo.ui.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.model.ArrivalTimeInfo
import com.example.metroinfo.model.Line
import com.example.metroinfo.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ArrivalTimeViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {

    private val _lines = MutableStateFlow<List<Line>>(emptyList())
    val lines: StateFlow<List<Line>> = _lines.asStateFlow()

    private val _stations = MutableStateFlow<List<ArrivalTimeInfo>>(emptyList())
    val stations: StateFlow<List<ArrivalTimeInfo>> = _stations.asStateFlow()

    private val _arrivalInfo = MutableStateFlow<List<ArrivalInfo>>(emptyList())
    val arrivalInfo: StateFlow<List<ArrivalInfo>> = _arrivalInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadLines()
    }

    fun loadLines() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getLines()
                if (response.isSuccessful) {
                    _lines.value = response.body() ?: emptyList()
                } else {
                    _error.value = "加载线路失败"
                }
            } catch (e: Exception) {
                Timber.e(e, "加载线路失败")
                _error.value = "加载线路失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectLine(lineId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getLineStationsArrivalTime(lineId)
                if (response.isSuccessful) {
                    _stations.value = response.body() ?: emptyList()
                } else {
                    _error.value = "加载站点失败"
                }
            } catch (e: Exception) {
                Timber.e(e, "加载站点失败")
                _error.value = "加载站点失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchStations(query: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = repository.searchStations(query)
                if (response.isSuccessful) {
                    _stations.value = response.body() ?: emptyList()
                } else {
                    _error.value = "搜索站点失败"
                }
            } catch (e: Exception) {
                Timber.e(e, "搜索站点失败")
                _error.value = "搜索站点失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getStationArrivalInfo(stationId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = repository.getStationArrivalTimeDetails(stationId.toString())
                if (response.isSuccessful) {
                    val arrivalTimeInfoList = response.body() ?: emptyList()
                    _arrivalInfo.value = arrivalTimeInfoList.map { arrivalTimeInfo ->
                        ArrivalInfo(
                            lineId = arrivalTimeInfo.lineId,
                            stationId = arrivalTimeInfo.stationId,
                            stationName = arrivalTimeInfo.stationName,
                            directionDesc = arrivalTimeInfo.directionDesc,
                            firstArrivalTime = arrivalTimeInfo.firstArrivalTime,
                            nextArrivalTime = "",
                            minutesRemaining = arrivalTimeInfo.minutesRemaining,
                            lineName = arrivalTimeInfo.lineName,
                            arrivalTime = arrivalTimeInfo.firstArrivalTime
                        )
                    }
                } else {
                    _error.value = "获取站点到站信息失败"
                }
            } catch (e: Exception) {
                Timber.e(e, "获取站点到站信息失败")
                _error.value = "获取站点到站信息失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 