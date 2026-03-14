package com.amirswitch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amirswitch.data.MqttRepository
import com.amirswitch.data.models.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceViewModel : ViewModel() {

    private val repository = MqttRepository()

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

    private val _schedules =
        MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> =
        _schedules.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ========================================================
    // Initialization
    // ========================================================

    init {
        repository.connect()
        _isLoading.value = false

        viewModelScope.launch {
            launch {
                repository.observeDeviceState().collect {
                    _deviceState.value = it
                }
            }
            launch {
                repository.observeOnlineStatus().collect {
                    _isOnline.value = it
                }
            }
            launch {
                repository.observeLastSeen().collect {
                    _lastSeen.value = it
                }
            }
            launch {
                repository.observeSchedules().collect {
                    _schedules.value = it
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }

    // ========================================================
    // Actions
    // ========================================================

    fun toggleDevice() {
        repository.setDeviceState(!_deviceState.value)
    }

    fun setDeviceState(on: Boolean) {
        repository.setDeviceState(on)
    }

    fun addSchedule(schedule: Schedule) {
        repository.addSchedule(schedule)
    }

    fun updateSchedule(schedule: Schedule) {
        repository.updateSchedule(schedule)
    }

    fun deleteSchedule(scheduleId: String) {
        repository.deleteSchedule(scheduleId)
    }

    fun toggleScheduleEnabled(
        scheduleId: String, enabled: Boolean
    ) {
        repository.toggleSchedule(scheduleId, enabled)
    }

    fun setDeviceId(id: String) {
        repository.setDeviceId(id)
    }

    fun getDeviceId(): String = repository.getDeviceId()

    fun clearError() {
        _error.value = null
    }
}
