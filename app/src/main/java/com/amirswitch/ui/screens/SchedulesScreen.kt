package com.amirswitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amirswitch.data.models.Schedule

/**
 * Schedules list screen.
 * Shows all schedules with enable/disable toggle, add/edit/delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    schedules: List<Schedule>,
    onToggleSchedule: (String, Boolean) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onAddSchedule: (Schedule) -> Unit,
    onUpdateSchedule: (Schedule) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedules") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
            }
        }
    ) { padding ->
        if (schedules.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No schedules yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap + to add your first schedule",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onToggle = { enabled ->
                            onToggleSchedule(schedule.id, enabled)
                        },
                        onEdit = { editingSchedule = schedule },
                        onDelete = { onDeleteSchedule(schedule.id) }
                    )
                }
            }
        }

        // Add schedule dialog
        if (showAddDialog) {
            ScheduleEditDialog(
                schedule = null,
                onDismiss = { showAddDialog = false },
                onSave = { schedule ->
                    onAddSchedule(schedule)
                    showAddDialog = false
                }
            )
        }

        // Edit schedule dialog
        editingSchedule?.let { schedule ->
            ScheduleEditDialog(
                schedule = schedule,
                onDismiss = { editingSchedule = null },
                onSave = { updated ->
                    onUpdateSchedule(updated)
                    editingSchedule = null
                }
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onEdit
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = schedule.name.ifEmpty { "Unnamed Schedule" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time display
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ON: ${schedule.onTime}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "OFF: ${schedule.offTime}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Schedule.DAY_NAMES.forEachIndexed { index, name ->
                        val dayNum = index + 1
                        val isActive = dayNum in schedule.days
                        FilterChip(
                            selected = isActive,
                            onClick = { },
                            label = {
                                Text(
                                    text = name.take(2),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Schedule") },
            text = { Text("Are you sure you want to delete \"${schedule.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================================================
// Schedule Edit / Add Dialog
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditDialog(
    schedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    val isEditing = schedule != null
    var name by remember { mutableStateOf(schedule?.name ?: "") }
    var onTime by remember { mutableStateOf(schedule?.onTime ?: "08:00") }
    var offTime by remember { mutableStateOf(schedule?.offTime ?: "18:00") }
    var selectedDays by remember { mutableStateOf(schedule?.days ?: listOf(1, 2, 3, 4, 5)) }
    var showOnTimePicker by remember { mutableStateOf(false) }
    var showOffTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Schedule" else "New Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Schedule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ON time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ON Time:", modifier = Modifier.width(80.dp))
                    OutlinedButton(onClick = { showOnTimePicker = true }) {
                        Text(onTime)
                    }
                }

                // OFF time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OFF Time:", modifier = Modifier.width(80.dp))
                    OutlinedButton(onClick = { showOffTimePicker = true }) {
                        Text(offTime)
                    }
                }

                // Day selector
                Text("Active Days:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Schedule.DAY_NAMES.forEachIndexed { index, dayName ->
                        val dayNum = index + 1
                        val isSelected = dayNum in selectedDays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    selectedDays - dayNum
                                } else {
                                    selectedDays + dayNum
                                }
                            },
                            label = {
                                Text(
                                    text = dayName.take(2),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = Schedule(
                        id = schedule?.id ?: "",
                        name = name.ifBlank { "Schedule" },
                        onTime = onTime,
                        offTime = offTime,
                        days = selectedDays.sorted(),
                        enabled = schedule?.enabled ?: true
                    )
                    onSave(result)
                }
            ) {
                Text(if (isEditing) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // ON time picker
    if (showOnTimePicker) {
        TimePickerDialog(
            initialTime = onTime,
            onDismiss = { showOnTimePicker = false },
            onConfirm = { time ->
                onTime = time
                showOnTimePicker = false
            }
        )
    }

    // OFF time picker
    if (showOffTimePicker) {
        TimePickerDialog(
            initialTime = offTime,
            onDismiss = { showOffTimePicker = false },
            onConfirm = { time ->
                offTime = time
                showOffTimePicker = false
            }
        )
    }
}

// ============================================================
// Time Picker Dialog
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = String.format("%02d", timePickerState.hour)
                    val m = String.format("%02d", timePickerState.minute)
                    onConfirm("$h:$m")
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
