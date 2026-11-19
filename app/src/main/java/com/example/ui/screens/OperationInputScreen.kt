package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.data.ActivityType
import com.example.data.AirportConstants
import com.example.data.OperationFormDraft
import com.example.data.ParkingStandLiveStatus
import com.example.ui.components.LivePulseDot
import com.example.ui.components.OpsTimeFormatters
import com.example.ui.theme.LocalOperationalColors
import com.example.ui.theme.TelemetryCodeStyle
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OperationInputScreen(
    standStatuses: List<ParkingStandLiveStatus>,
    draft: OperationFormDraft,
    onSubmitLog: (
        aircraftRegistration: String,
        flightNumber: String,
        parkingStand: String,
        activityType: ActivityType,
        activityTimestampMillis: Long,
        operationalNotes: String,
        onSuccess: () -> Unit
    ) -> Unit
) {
    val opColors = LocalOperationalColors.current

    var aircraftReg by rememberSaveable { mutableStateOf("") }
    var flightNumber by rememberSaveable { mutableStateOf("") }
    var selectedStand by rememberSaveable { mutableStateOf(AirportConstants.PARKING_STANDS.first()) }
    var selectedActivity by rememberSaveable { mutableStateOf(ActivityType.DOCKING) }
    var operationalNotes by rememberSaveable { mutableStateOf("") }
    var useRealtimeClock by rememberSaveable { mutableStateOf(true) }
    var activityTimestampMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Apply stand quick-action prefill when triggered from Live Monitor
    LaunchedEffect(draft.nonce) {
        if (draft.nonce > 0L) {
            selectedStand = draft.parkingStand
            selectedActivity = draft.activityType
            aircraftReg = draft.aircraftRegistration
            flightNumber = draft.flightNumber
            useRealtimeClock = true
            activityTimestampMillis = System.currentTimeMillis()
            validationError = null
        }
    }

    // Keep timestamp continuously synced when Real-Time mode is ON
    LaunchedEffect(useRealtimeClock) {
        if (useRealtimeClock) {
            while (true) {
                activityTimestampMillis = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }

    val dockedMap = remember(standStatuses) {
        standStatuses.associateBy { it.standCode }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, opColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = opColors.goldContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AirplanemodeActive,
                                contentDescription = null,
                                tint = opColors.goldAccent
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Input Operasi Doking / Undocking",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Catat waktu masuk & keluar pesawat pada garbarata secara akurat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 1: Aktivitas (Doking / Undocking)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, opColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Jenis Aktivitas Garbarata *",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActivityToggleCard(
                            title = "DOKING",
                            subtitle = "Pesawat Masuk / Pasang",
                            icon = Icons.Filled.FlightLand,
                            selected = selectedActivity == ActivityType.DOCKING,
                            activeColor = opColors.docked,
                            activeContainer = opColors.dockedContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("activity_docking_button"),
                            onClick = { selectedActivity = ActivityType.DOCKING }
                        )
                        ActivityToggleCard(
                            title = "UNDOCKING",
                            subtitle = "Pesawat Keluar / Lepas",
                            icon = Icons.Filled.FlightTakeoff,
                            selected = selectedActivity == ActivityType.UNDOCKING,
                            activeColor = opColors.undocking,
                            activeContainer = opColors.undockingContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("activity_undocking_button"),
                            onClick = { selectedActivity = ActivityType.UNDOCKING }
                        )
                    }
                }
            }

            // Section 2: Parking Stand (Exact 12 stands)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, opColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Parking Stand *",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .then(Modifier)
                            )
                            Text(
                                text = "Titik hijau = sedang doking",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 4
                    ) {
                        AirportConstants.PARKING_STANDS.forEach { standCode ->
                            val isSelected = selectedStand == standCode
                            val liveStatus = dockedMap[standCode]
                            val isCurrentlyDocked = liveStatus?.isDocked == true

                            Surface(
                                modifier = Modifier
                                    .width(74.dp)
                                    .height(48.dp)
                                    .testTag("stand_select_$standCode")
                                    .clickable {
                                        selectedStand = standCode
                                        // Helpful auto-suggest: if stand is docked & user chose undocking, prefill reg if blank
                                        if (isCurrentlyDocked && aircraftReg.isBlank() && liveStatus?.activeLog != null) {
                                            selectedActivity = ActivityType.UNDOCKING
                                            aircraftReg = liveStatus.activeLog.aircraftRegistration
                                            flightNumber = liveStatus.activeLog.flightNumber
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = when {
                                        isSelected -> opColors.goldAccent
                                        isCurrentlyDocked -> opColors.docked
                                        else -> opColors.cardBorder
                                    }
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isCurrentlyDocked) {
                                            LivePulseDot(
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else opColors.docked
                                            )
                                        }
                                        Text(
                                            text = standCode,
                                            style = TelemetryCodeStyle,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Identitas Pesawat (Registrasi wajib + Nomor Penerbangan opsional)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, opColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. Identitas Pesawat",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = aircraftReg,
                        onValueChange = {
                            aircraftReg = it.uppercase()
                            validationError = null
                        },
                        label = { Text("Registrasi Pesawat *") },
                        placeholder = { Text("Contoh: PK-GFA / PK-LQP") },
                        leadingIcon = {
                            Icon(Icons.Filled.AirplanemodeActive, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_aircraft_reg")
                    )

                    OutlinedTextField(
                        value = flightNumber,
                        onValueChange = { flightNumber = it.uppercase() },
                        label = { Text("Nomor Penerbangan (Opsional)") },
                        placeholder = { Text("Contoh: GA-604 / JT-781 / ID-6290") },
                        leadingIcon = {
                            Icon(Icons.Filled.ConfirmationNumber, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_flight_number")
                    )
                }
            }

            // Section 4: Tanggal & Waktu Aktivitas
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, opColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. Tanggal & Waktu Aktivitas *",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilterChip(
                            selected = useRealtimeClock,
                            onClick = {
                                useRealtimeClock = true
                                activityTimestampMillis = System.currentTimeMillis()
                            },
                            label = { Text(if (useRealtimeClock) "Live Sekarang" else "Set Sekarang") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("time_live_now_chip")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                useRealtimeClock = false
                                showDatePicker = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pick_date_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = OpsTimeFormatters.formatDate(activityTimestampMillis),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                useRealtimeClock = false
                                showTimePicker = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pick_time_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = OpsTimeFormatters.formatClock(activityTimestampMillis),
                                style = TelemetryCodeStyle
                            )
                        }
                    }
                }
            }

            // Section 5: Catatan Kegiatan Operasional (Opsional)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, opColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "5. Catatan Kegiatan Operasional (Opsional)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AirportConstants.QUICK_OPERATIONAL_NOTES.forEach { preset ->
                            FilterChip(
                                selected = operationalNotes == preset,
                                onClick = { operationalNotes = preset },
                                label = { Text(preset) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = operationalNotes,
                        onValueChange = { operationalNotes = it },
                        label = { Text("Catatan Lapangan") },
                        placeholder = { Text("Kondisi garbarata, kendala teknis, informasi boarding...") },
                        leadingIcon = {
                            Icon(Icons.Filled.EditNote, contentDescription = null)
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_operational_notes")
                    )
                }
            }

            if (!validationError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val trimmedReg = aircraftReg.trim()
                    if (trimmedReg.length < 3) {
                        validationError = "Masukkan Registrasi Pesawat yang valid (minimal 3 karakter, contoh: PK-GFA)."
                        return@Button
                    }
                    validationError = null
                    val finalTimestamp = if (useRealtimeClock) System.currentTimeMillis() else activityTimestampMillis
                    onSubmitLog(
                        trimmedReg,
                        flightNumber.trim(),
                        selectedStand,
                        selectedActivity,
                        finalTimestamp,
                        operationalNotes.trim()
                    ) {
                        aircraftReg = ""
                        flightNumber = ""
                        operationalNotes = ""
                        useRealtimeClock = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_operation_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedActivity == ActivityType.DOCKING) opColors.docked else opColors.undocking,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Simpan ${selectedActivity.label} · Stand $selectedStand",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = activityTimestampMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedUtc = datePickerState.selectedDateMillis
                        if (pickedUtc != null) {
                            val currentCal = Calendar.getInstance().apply { timeInMillis = activityTimestampMillis }
                            val pickedCal = Calendar.getInstance().apply { timeInMillis = pickedUtc }
                            currentCal.set(
                                pickedCal.get(Calendar.YEAR),
                                pickedCal.get(Calendar.MONTH),
                                pickedCal.get(Calendar.DAY_OF_MONTH)
                            )
                            activityTimestampMillis = currentCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = remember(activityTimestampMillis) {
            Calendar.getInstance().apply { timeInMillis = activityTimestampMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pilih Waktu Aktivitas") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedCal = Calendar.getInstance().apply {
                            timeInMillis = activityTimestampMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                        }
                        activityTimestampMillis = updatedCal.timeInMillis
                        showTimePicker = false
                    }
                ) { Text("Terapkan") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun ActivityToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    activeContainer: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val opColors = LocalOperationalColors.current
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) activeContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) activeColor else opColors.cardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Dipilih",
                        tint = activeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) activeColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
