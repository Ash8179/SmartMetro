package com.example.metroinfo.viewmodel

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
class ArrivalViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {
    private val _lines = MutableStateFlow<List<Line>>(emptyList())
    val lines: StateFlow<List<Line>> = _lines.asStateFlow()

    private val _stations = MutableStateFlow<List<ArrivalTimeInfo>>(emptyList())
    val stations: StateFlow<List<ArrivalTimeInfo>> = _stations.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedLineId = MutableStateFlow<Int?>(null)
    val selectedLineId: StateFlow<Int?> = _selectedLineId.asStateFlow()

    fun loadLines() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.getLines()
                if (response.isSuccessful) {
                    val lines: List<Line> = response.body() ?: emptyList()
                    _lines.value = lines
                    if (lines.isEmpty()) {
                        _error.value = "No lines available"
                    }
                } else {
                    _error.value = "Failed to load lines: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectLine(lineId: Int) {
        _selectedLineId.value = lineId
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.getLineStationsArrivalTime(lineId)
                if (response.isSuccessful) {
                    val lineStationsResponse: LineStationsResponse? = response.body()
                    if (lineStationsResponse != null) {
                        // 将 LineStationInfo 转换为 ArrivalTimeInfo
                        val arrivalInfos = lineStationsResponse.stations.map { station ->
                            ArrivalTimeInfo(
                                lineId = station.lineNumber,
                                stationId = station.stationId.toString(),
                                stationName = station.stationName,
                                lineName = "${station.lineNumber}号线",
                                directionDesc = station.directionInfo ?: "上行", // 使用实际的方向信息
                                firstArrivalTime = station.firstTrain,
                                minutesRemaining = station.minutesRemaining,
                                isOperating = station.isOperating,
                                serviceStatus = station.serviceStatus,
                                nextServiceTime = station.nextServiceTime,
                                nextArrival = station.nextArrival,
                                upboundFirstTrain = station.upboundFirstTrain,
                                downboundFirstTrain = station.downboundFirstTrain,
                                directionInfo = station.directionInfo
                            )
                        }
                        _stations.value = arrivalInfos
                        Timber.d("Loaded ${arrivalInfos.size} stations for line $lineId")
                    } else {
                        _error.value = "No data received"
                    }
                } else {
                    _error.value = "Failed to load stations: ${response.code()}"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading line stations")
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getStationArrivalInfo(stationId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getStationArrivalTime(stationId.toString())
                if (response.isSuccessful) {
                    val arrivalInfos: List<ArrivalTimeInfo> = response.body() ?: emptyList()
                    _stations.value = arrivalInfos
                } else {
                    _error.value = "Failed to load arrival info: ${response.code()}"
                }
            } catch (e: Exception) {
                Timber.e(e, "获取到站信息失败")
                _error.value = "获取到站信息失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchStations(query: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.searchStations(query)
                if (response.isSuccessful) {
                    val arrivalInfos: List<ArrivalTimeInfo> = response.body() ?: emptyList()
                    _stations.value = arrivalInfos
                    if (arrivalInfos.isEmpty()) {
                        _error.value = "No stations found"
                    }
                } else {
                    _error.value = "Failed to search stations: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 