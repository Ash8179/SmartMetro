package com.example.metroinfo.ui.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.data.repository.MetroRepository
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.model.ArrivalTimeInfo
import com.example.metroinfo.model.Line
import com.example.metroinfo.model.LineStationsResponse
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

    private val _arrivalTimes = MutableStateFlow<List<ArrivalTimeInfo>>(emptyList())
    val arrivalTimes: StateFlow<List<ArrivalTimeInfo>> = _arrivalTimes.asStateFlow()

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

    fun loadStationArrivalTime(stationId: String) {
        viewModelScope.launch {
            try {
            _isLoading.value = true
                _error.value = null
                
                Timber.d("Loading arrival time for station: $stationId")
                val response = repository.getStationArrivalTime(stationId)
                
                if (response.isSuccessful) {
                    val arrivalTimes = response.body() ?: emptyList()
                    _arrivalTimes.value = arrivalTimes
                    Timber.d("Loaded ${arrivalTimes.size} arrival times")
                } else {
                    throw Exception("获取到站信息失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading arrival times")
                _error.value = e.message ?: "获取到站信息失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadLineStationsArrivalTime(lineId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Timber.d("Loading arrival time for line: $lineId")
                val response = repository.getLineStationsArrivalTime(lineId)
                
                if (response.isSuccessful) {
                    val lineStationsResponse: LineStationsResponse? = response.body()
                    if (lineStationsResponse != null) {
                        // 将 LineStationInfo 转换为 ArrivalTimeInfo
                        val arrivalTimes = lineStationsResponse.stations.map { station ->
                            ArrivalTimeInfo(
                                lineId = station.lineNumber,
                                stationId = station.stationId.toString(),
                                stationName = station.stationName,
                                lineName = "${station.lineNumber}号线",
                                directionDesc = "上行", // 默认方向
                                firstArrivalTime = station.firstTrain,
                                minutesRemaining = station.minutesRemaining,
                                isOperating = station.isOperating,
                                serviceStatus = station.serviceStatus,
                                nextServiceTime = station.nextServiceTime,
                                nextArrival = station.nextArrival
                            )
                        }
                        _arrivalTimes.value = arrivalTimes
                        Timber.d("Loaded ${arrivalTimes.size} arrival times for line $lineId")
                    } else {
                        _error.value = "No data received"
                    }
                } else {
                    throw Exception("获取线路到站信息失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading line arrival times")
                _error.value = e.message ?: "获取线路到站信息失败"
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
                
                Timber.d("Searching stations with query: $query")
                val response = repository.searchStations(query)
                
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()
                    _arrivalTimes.value = results
                    Timber.d("Found ${results.size} stations matching query")
                } else {
                    throw Exception("搜索站点失败: ${response.code()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error searching stations")
                _error.value = e.message ?: "搜索站点失败"
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
                    val arrivalTimeInfoList: List<ArrivalTimeInfo> = response.body() ?: emptyList()
                    _arrivalInfo.value = arrivalTimeInfoList.map { arrivalTimeInfo: ArrivalTimeInfo ->
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