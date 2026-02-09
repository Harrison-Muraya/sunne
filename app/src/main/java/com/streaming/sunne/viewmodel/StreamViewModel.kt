package com.streaming.sunne.viewmodel

import androidx.lifecycle.viewModelScope
import com.streaming.sunne.data.model.Stream
import com.streaming.sunne.data.model.StreamCategory
import com.streaming.sunne.repository.StreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel

/**
 * UI State for streams
 */
sealed class StreamUiState : ViewModel() {
    object Loading : StreamUiState()
    data class Success(val categories: List<StreamCategory>) : StreamUiState()
    data class Error(val message: String) : StreamUiState()
}

/**
 * ViewModel for managing stream data and UI state
 */

class StreamViewModel : ViewModel(){
    private val repository = StreamRepository()

    private val _uiState = MutableStateFlow<StreamUiState>(StreamUiState.Loading)
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private val _selectedStream = MutableStateFlow<Stream?>(null)
    val selectedStream: StateFlow<Stream?> = _selectedStream.asStateFlow()

    init {
        loadStreams()
    }

    /**
     * Load all streams grouped by category
     */
    fun loadStreams() {
//        viewModelScope.launch {
//            _uiState.value = StreamUiState.Loading
//
//            val result = repository.getStreamsByCategory()
//
//            _uiState.value = if (result.isSuccess) {
//                val categories = result.getOrNull() ?: emptyList()
//                if (categories.isEmpty()) {
//                    StreamUiState.Error("No streams available")
//                } else {
//                    StreamUiState.Success(categories)
//                }
//            } else {
//                StreamUiState.Error(
//                    result.exceptionOrNull()?.message ?: "Failed to load streams"
//                )
//            }
//        }
    }

    /**
     * Select a stream to view details or play
     */
    fun selectStream(stream: Stream) {
        _selectedStream.value = stream
    }

    /**
     * Clear selected stream
     */
    fun clearSelectedStream() {
        _selectedStream.value = null
    }

    /**
     * Retry loading streams after an error
     */
    fun retry() {
        loadStreams()
    }
}