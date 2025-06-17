package com.example.metroinfo.viewmodel

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
import javax.inject.Inject
import timber.log.Timber

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

    fun loadLines() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.getLines()
                if (response.isSuccessful) {
                    val lines = response.body() ?: emptyList()
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
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.getLineStationsArrivalTime(lineId)
                if (response.isSuccessful) {
                    val arrivalInfos = response.body() ?: emptyList()
                    _stations.value = arrivalInfos.map { arrivalInfo ->
                        ArrivalTimeInfo(
                            lineId = arrivalInfo.lineId,
                            stationId = arrivalInfo.stationId,
                            stationName = arrivalInfo.stationName,
                            lineName = arrivalInfo.lineName,
                            directionDesc = arrivalInfo.directionDesc,
                            firstArrivalTime = arrivalInfo.firstArrivalTime,
                            minutesRemaining = arrivalInfo.minutesRemaining
                        )
                    }
                } else {
                    _error.value = "Failed to load stations: ${response.code()}"
                }
            } catch (e: Exception) {
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
                    val arrivalInfos = response.body() ?: emptyList()
                    _stations.value = arrivalInfos.map { arrivalInfo ->
                        ArrivalTimeInfo(
                            lineId = arrivalInfo.lineId,
                            stationId = arrivalInfo.stationId,
                            stationName = arrivalInfo.stationName,
                            lineName = arrivalInfo.lineName,
                            directionDesc = arrivalInfo.directionDesc,
                            firstArrivalTime = arrivalInfo.firstArrivalTime,
                            minutesRemaining = arrivalInfo.minutesRemaining
                        )
                    }
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
                    val arrivalInfos = response.body() ?: emptyList()
                    _stations.value = arrivalInfos.map { arrivalInfo ->
                        ArrivalTimeInfo(
                            lineId = arrivalInfo.lineId,
                            stationId = arrivalInfo.stationId,
                            stationName = arrivalInfo.stationName,
                            lineName = arrivalInfo.lineName,
                            directionDesc = arrivalInfo.directionDesc,
                            firstArrivalTime = arrivalInfo.firstArrivalTime,
                            minutesRemaining = arrivalInfo.minutesRemaining
                        )
                    }
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