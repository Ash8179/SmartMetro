package com.example.metroinfo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.Station
import com.example.metroinfo.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyStationsViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {

    private val _nearbyStations = MutableLiveData<List<Station>>()
    val nearbyStations: LiveData<List<Station>> = _nearbyStations

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun getNearbyStations(latitude: Double, longitude: Double, radius: Int = 1000) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val stations = repository.getNearestStations(latitude, longitude, radius)
                _nearbyStations.value = stations
            } catch (e: Exception) {
                _error.value = e.message ?: "获取附近站点失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 