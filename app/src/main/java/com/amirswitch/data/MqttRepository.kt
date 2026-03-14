package com.amirswitch.data

import com.amirswitch.data.models.Schedule
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MqttRepository(
    private val host: String = "YOUR_CLUSTER.s1.eu.hivemq.cloud",
    private val port: Int = 8883,
    private val username: String = "YOUR_MQTT_USER",
    private val password: String = "YOUR_MQTT_PASS",
    private val timezone: String = "Asia/Jerusalem"
) {
    private var deviceId = "device_001"
    private var client: Mqtt5AsyncClient? = null

    private val _deviceState = MutableStateFlow(false)
    private val _isOnline = MutableStateFlow(false)
    private val _lastSeen = MutableStateFlow(0L)
    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    private var scheduleVersion = 0

    fun observeDeviceState(): StateFlow<Boolean> =
        _deviceState.asStateFlow()

    fun observeOnlineStatus(): StateFlow<Boolean> =
        _isOnline.asStateFlow()

    fun observeLastSeen(): StateFlow<Long> =
        _lastSeen.asStateFlow()

    fun observeSchedules(): StateFlow<List<Schedule>> =
        _schedules.asStateFlow()

    fun connect() {
        val id = "amirswitch-android-${UUID.randomUUID()
            .toString().take(8)}"

        val mqttClient = Mqtt5Client.builder()
            .serverHost(host)
            .serverPort(port)
            .sslWithDefaultConfig()
            .automaticReconnectWithDefaultConfig()
            .identifier(id)
            .addConnectedListener { subscribeAll() }
            .buildAsync()

        client = mqttClient

        mqttClient.connectWith()
            .simpleAuth()
            .username(username)
            .password(password.toByteArray())
            .applySimpleAuth()
            .willPublish()
            .topic("amirswitch/$deviceId/online")
            .qos(MqttQos.AT_LEAST_ONCE)
            .payload("false".toByteArray())
            .retain(true)
            .applyWillPublish()
            .send()
    }

    fun disconnect() {
        client?.disconnect()
        client = null
    }

    private fun subscribeAll() {
        val c = client ?: return
        val prefix = "amirswitch/$deviceId"

        c.subscribeWith()
            .topicFilter("$prefix/state")
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { pub ->
                val value = String(pub.payloadAsBytes)
                _deviceState.value = value == "true"
            }
            .send()

        c.subscribeWith()
            .topicFilter("$prefix/online")
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { pub ->
                val value = String(pub.payloadAsBytes)
                _isOnline.value = value == "true"
            }
            .send()

        c.subscribeWith()
            .topicFilter("$prefix/last_seen")
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { pub ->
                val value = String(pub.payloadAsBytes)
                value.toLongOrNull()?.let { _lastSeen.value = it }
            }
            .send()

        c.subscribeWith()
            .topicFilter("$prefix/schedules")
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { pub ->
                val payload = String(pub.payloadAsBytes)
                if (payload.isNotEmpty()) {
                    try {
                        val (version, schedules) =
                            Schedule.fromDocument(payload)
                        scheduleVersion = version
                        _schedules.value = schedules
                    } catch (_: Exception) {
                        // Malformed schedule document
                    }
                }
            }
            .send()
    }

    fun setDeviceState(on: Boolean) {
        client?.publishWith()
            ?.topic("amirswitch/$deviceId/state/set")
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.payload(on.toString().toByteArray())
            ?.send()
    }

    fun addSchedule(schedule: Schedule) {
        val withId = if (schedule.id.isEmpty()) {
            schedule.copy(id = UUID.randomUUID().toString())
        } else {
            schedule
        }
        publishSchedules(_schedules.value + withId)
    }

    fun updateSchedule(schedule: Schedule) {
        val updated = _schedules.value.map {
            if (it.id == schedule.id) schedule else it
        }
        publishSchedules(updated)
    }

    fun deleteSchedule(id: String) {
        publishSchedules(_schedules.value.filter { it.id != id })
    }

    fun toggleSchedule(id: String, enabled: Boolean) {
        val updated = _schedules.value.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        publishSchedules(updated)
    }

    private fun publishSchedules(schedules: List<Schedule>) {
        scheduleVersion++
        _schedules.value = schedules
        val doc = Schedule.toDocument(
            schedules, scheduleVersion, timezone
        )
        client?.publishWith()
            ?.topic("amirswitch/$deviceId/schedules")
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.payload(doc.toByteArray())
            ?.retain(true)
            ?.send()
    }

    fun setDeviceId(id: String) {
        disconnect()
        deviceId = id
        connect()
    }

    fun getDeviceId(): String = deviceId
}
