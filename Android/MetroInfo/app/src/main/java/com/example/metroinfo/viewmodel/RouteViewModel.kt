package com.example.metroinfo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.BestRouteResponse
import com.example.metroinfo.model.RouteInfo
import com.example.metroinfo.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {
    private val _bestRoute = MutableLiveData<BestRouteResponse>()
    val bestRoute: LiveData<BestRouteResponse> = _bestRoute

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun findRoute(startStationId: Int, endStationId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.findRoute(startStationId, endStationId)
                if (response != null) {
                    _bestRoute.value = response
                } else {
                    _error.value = "Failed to find route"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 