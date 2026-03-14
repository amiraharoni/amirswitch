package com.amirswitch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amirswitch.data.FirebaseRepository
import com.amirswitch.data.models.Schedule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel shared across all screens.
 * Manages device state, online status, and schedules.
 */
class DeviceViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    // ========================================================
    // UI State
    // ========================================================

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deviceState = MutableStateFlow(false)
    val deviceState: StateFlow<Boolean> = _deviceState.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _lastSeen = MutableStateFlow(0L)
    val lastSeen: StateFlow<Long> = _lastSeen.asStateFlow()

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ========================================================
    // Initialization
    // ========================================================

    init {
        viewModelScope.launch {
            try {
                // Sign in anonymously
                repository.signInAnonymously()

                // Start observing Firebase data
                launch {
                    repository.observeDeviceState().collect { state ->
                        _deviceState.value = state
                        _isLoading.value = false
                    }
                }
                launch {
                    repository.observeOnlineStatus().collect { online ->
                        _isOnline.value = online
                    }
                }
                launch {
                    repository.observeLastSeen().collect { ts ->
                        _lastSeen.value = ts
                    }
                }
                launch {
                    repository.observeSchedules().collect { list ->
                        _schedules.value = list
                    }
                }
            } catch (e: Exception) {
                _error.value = "Connection failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // ========================================================
    // Actions
    // ========================================================

    /**
     * Toggle the device relay ON/OFF.
     */
    fun toggleDevice() {
        viewModelScope.launch {
            try {
                repository.setDeviceState(!_deviceState.value)
            } catch (e: Exception) {
                _error.value = "Failed to toggle: ${e.message}"
            }
        }
    }

    /**
     * Set device to a specific state.
     */
    fun setDeviceState(on: Boolean) {
        viewModelScope.launch {
            try {
                repository.setDeviceState(on)
            } catch (e: Exception) {
                _error.value = "Failed to set state: ${e.message}"
            }
        }
    }

    /**
     * Add a new schedule.
     */
    fun addSchedule(schedule: Schedule) {
        viewModelScope.launch {
            try {
                repository.addSchedule(schedule)
            } catch (e: Exception) {
                _error.value = "Failed to add schedule: ${e.message}"
            }
        }
    }

    /**
     * Update an existing schedule.
     */
    fun updateSchedule(schedule: Schedule) {
        viewModelScope.launch {
            try {
                repository.updateSchedule(schedule)
            } catch (e: Exception) {
                _error.value = "Failed to update schedule: ${e.message}"
            }
        }
    }

    /**
     * Delete a schedule.
     */
    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSchedule(scheduleId)
            } catch (e: Exception) {
                _error.value = "Failed to delete schedule: ${e.message}"
            }
        }
    }

    /**
     * Toggle a schedule's enabled/disabled state.
     */
    fun toggleScheduleEnabled(scheduleId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleSchedule(scheduleId, enabled)
            } catch (e: Exception) {
                _error.value = "Failed to toggle schedule: ${e.message}"
            }
        }
    }

    /**
     * Update the device ID (for connecting to a different device).
     */
    fun setDeviceId(id: String) {
        repository.setDeviceId(id)
        // Re-initialize observers would be needed here for a full implementation
    }

    fun getDeviceId(): String = repository.getDeviceId()

    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
}
