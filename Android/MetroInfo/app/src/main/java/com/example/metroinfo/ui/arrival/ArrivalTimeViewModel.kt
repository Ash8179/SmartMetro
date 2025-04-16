package com.example.metroinfo.ui.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.Line
import com.example.metroinfo.model.ArrivalTimeInfo
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

    private val _arrivalInfo = MutableStateFlow<ArrivalTimeInfo?>(null)
    val arrivalInfo: StateFlow<ArrivalTimeInfo?> = _arrivalInfo.asStateFlow()

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
                val lines = repository.getLines()
                _lines.value = lines
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
                val stations = repository.getLineStationsWithArrivalTime(lineId)
                _stations.value = stations
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
                val stations = repository.searchStationsWithArrivalTime(query)
                _stations.value = stations
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
                val info = repository.getStationArrivalTimeDetails(stationId.toString())
                _arrivalInfo.value = info.firstOrNull()
            } catch (e: Exception) {
                Timber.e(e, "获取到站信息失败")
                _error.value = "获取到站信息失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 