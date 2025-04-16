package com.example.metroinfo.ui.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StationDetailViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {

    private val _arrivalInfo = MutableStateFlow<List<ArrivalInfo>>(emptyList())
    val arrivalInfo: StateFlow<List<ArrivalInfo>> = _arrivalInfo

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadArrivalInfo(stationId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = repository.getStationArrivalTimeDetails(stationId.toString())
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