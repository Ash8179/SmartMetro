package com.example.metroinfo.ui.station

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.model.Station
import com.example.metroinfo.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StationDetailViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val stationId: Int = checkNotNull(savedStateHandle["stationId"])

    private val _station = MutableStateFlow<Station?>(null)
    val station: StateFlow<Station?> = _station

    private val _arrivals = MutableStateFlow<List<ArrivalInfo>>(emptyList())
    val arrivals: StateFlow<List<ArrivalInfo>> = _arrivals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadStationDetails()
    }

    fun loadStationDetails() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Load station details
                val station = stationRepository.getStation(stationId)
                _station.value = station

                // Load arrival times
                val arrivals = stationRepository.getStationArrivalTime(stationId.toString())
                _arrivals.value = arrivals
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadStationDetail(stationId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val arrivals = stationRepository.getStationArrivalTime(stationId.toString())
                _arrivals.value = arrivals
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 