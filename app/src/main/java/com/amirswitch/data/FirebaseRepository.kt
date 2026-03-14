package com.amirswitch.data

import com.amirswitch.data.models.Schedule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository handling all Firebase Realtime Database operations.
 */
class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // Default device ID — in a multi-device app, this would be dynamic
    private var deviceId = "device_001"

    private val deviceRef: DatabaseReference
        get() = database.getReference("devices/$deviceId")

    // ========================================================
    // Authentication
    // ========================================================

    /**
     * Sign in anonymously. Creates a new anonymous account if none exists.
     */
    suspend fun signInAnonymously() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    // ========================================================
    // Device State (ON/OFF)
    // ========================================================

    /**
     * Observe the relay state (on/off) in real time.
     */
    fun observeDeviceState(): Flow<Boolean> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(Boolean::class.java) ?: false
                trySend(state)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        deviceRef.child("state").addValueEventListener(listener)
        awaitClose { deviceRef.child("state").removeEventListener(listener) }
    }

    /**
     * Set the relay state (on/off).
     */
    suspend fun setDeviceState(on: Boolean) {
        deviceRef.child("state").setValue(on).await()
    }

    // ========================================================
    // Online Status
    // ========================================================

    /**
     * Observe whether the device (ESP32) is online.
     */
    fun observeOnlineStatus(): Flow<Boolean> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val online = snapshot.getValue(Boolean::class.java) ?: false
                trySend(online)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        deviceRef.child("online").addValueEventListener(listener)
        awaitClose { deviceRef.child("online").removeEventListener(listener) }
    }

    /**
     * Observe last seen timestamp.
     */
    fun observeLastSeen(): Flow<Long> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastSeen = snapshot.getValue(Long::class.java) ?: 0L
                trySend(lastSeen)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        deviceRef.child("lastSeen").addValueEventListener(listener)
        awaitClose { deviceRef.child("lastSeen").removeEventListener(listener) }
    }

    // ========================================================
    // Schedules
    // ========================================================

    /**
     * Observe all schedules in real time.
     */
    fun observeSchedules(): Flow<List<Schedule>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val schedules = mutableListOf<Schedule>()
                for (child in snapshot.children) {
                    val map = child.value as? Map<String, Any?> ?: continue
                    schedules.add(Schedule.fromMap(child.key ?: "", map))
                }
                trySend(schedules)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        deviceRef.child("schedules").addValueEventListener(listener)
        awaitClose { deviceRef.child("schedules").removeEventListener(listener) }
    }

    /**
     * Add a new schedule.
     */
    suspend fun addSchedule(schedule: Schedule): String {
        val ref = deviceRef.child("schedules").push()
        ref.setValue(schedule.toMap()).await()
        return ref.key ?: ""
    }

    /**
     * Update an existing schedule.
     */
    suspend fun updateSchedule(schedule: Schedule) {
        deviceRef.child("schedules/${schedule.id}").setValue(schedule.toMap()).await()
    }

    /**
     * Delete a schedule.
     */
    suspend fun deleteSchedule(scheduleId: String) {
        deviceRef.child("schedules/$scheduleId").removeValue().await()
    }

    /**
     * Toggle a schedule's enabled state.
     */
    suspend fun toggleSchedule(scheduleId: String, enabled: Boolean) {
        deviceRef.child("schedules/$scheduleId/enabled").setValue(enabled).await()
    }

    // ========================================================
    // Device Configuration
    // ========================================================

    fun setDeviceId(id: String) {
        deviceId = id
    }

    fun getDeviceId(): String = deviceId
}
