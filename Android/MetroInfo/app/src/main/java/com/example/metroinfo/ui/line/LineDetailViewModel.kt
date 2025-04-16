package com.example.metroinfo.ui.line

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.Line
import com.example.metroinfo.model.Station
import com.example.metroinfo.repository.LineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LineDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val line: Line? = null,
    val stations: List<Station>? = null
)

@HiltViewModel
class LineDetailViewModel @Inject constructor(
    private val lineRepository: LineRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LineDetailUiState())
    val uiState: StateFlow<LineDetailUiState> = _uiState
    
    fun loadLineDetail(lineId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val stations = lineRepository.getLineStations(lineId)
                val line = lineRepository.getLine(lineId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    line = line,
                    stations = stations
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }
} 