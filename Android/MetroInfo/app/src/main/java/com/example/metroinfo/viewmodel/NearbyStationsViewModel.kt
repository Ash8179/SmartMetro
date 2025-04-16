package com.example.metroinfo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyStationsViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {

    private val _nearbyStations = MutableStateFlow<List<NearbyStation>>(emptyList())
    val nearbyStations: StateFlow<List<NearbyStation>> = _nearbyStations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun updateLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val stations = repository.getNearestStations(latitude, longitude)
                _nearbyStations.value = stations
            } catch (e: Exception) {
                _error.value = e.message ?: "获取附近站点失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 