package com.example.metroinfo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.model.Line
import com.example.metroinfo.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class ArrivalViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {
    private val _lines = MutableLiveData<List<Line>>()
    val lines: LiveData<List<Line>> = _lines

    private val _arrivalInfo = MutableLiveData<List<ArrivalInfo>>()
    val arrivalInfo: LiveData<List<ArrivalInfo>> = _arrivalInfo

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadLines() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lines = repository.getLines()
                _lines.value = lines
                if (lines.isEmpty()) {
                    _error.value = "No lines available"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getStationArrivalTime(stationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getStationArrivalTime(stationId)
                _arrivalInfo.value = response
            } catch (e: Exception) {
                Timber.e(e, "获取到站信息失败")
                _error.value = "获取到站信息失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getLineStationsWithArrivalTime(lineId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val arrivalTimeInfo = repository.getLineStationsArrivalTime(lineId)
                _arrivalInfo.value = arrivalTimeInfo
            } catch (e: Exception) {
                _error.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchStations(query: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val stations = repository.searchStations(query)
                _arrivalInfo.value = stations
                if (stations.isEmpty()) {
                    _error.value = "No stations found"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getStationArrivalTimeDetails(stationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getStationArrivalTimeDetails(stationId)
                _arrivalInfo.value = response.map { arrivalTimeInfo ->
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
            } catch (e: Exception) {
                Timber.e(e, "获取站点详情失败")
                _error.value = "获取站点详情失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 