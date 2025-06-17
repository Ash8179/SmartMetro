package com.example.metroinfo.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.Station
import com.example.metroinfo.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MetroViewModel @Inject constructor(
    private val stationRepository: StationRepository
) : ViewModel() {
    
    private val _nearestStations = MutableStateFlow<List<Station>>(emptyList())
    val nearestStations: StateFlow<List<Station>> = _nearestStations
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    fun loadNearestStations(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stations = stationRepository.getNearestStations(latitude, longitude)
                _nearestStations.value = stations
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
} 