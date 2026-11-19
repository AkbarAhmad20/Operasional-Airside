package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ActivityType
import com.example.data.AirportConstants
import com.example.data.AuditLogEntity
import com.example.data.DockingLogEntity
import com.example.data.UserEntity
import com.example.data.UserRole
import com.example.data.VerificationStatus
import com.example.ui.components.ActivityTypeBadge
import com.example.ui.components.OpsTimeFormatters
import com.example.ui.components.RolePillBadge
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.LocalOperationalColors
import com.example.ui.theme.TelemetryClockStyle
import com.example.ui.theme.TelemetryCodeStyle
import java.util.Calendar

private enum class LogFilter(val label: String) {
    ALL("Semua"),
    PENDING("Menunggu"),
    VERIFIED("Terverifikasi"),
    DOCKING("Doking"),
    UNDOCKING("Undocking")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsAndVerificationScreen(
    currentUser: UserEntity,
    allLogs: List<DockingLogEntity>,
    auditLogs: List<AuditLogEntity>,
    onVerifyLog: (Long) -> Unit,
    onUpdateLog: (
        logId: Long,
        aircraftRegistration: String,
        flightNumber: String,
        parkingStand: String,
        activityType: ActivityType,
        activityTimestampMillis: Long,
        operationalNotes: String,
        markVerified: Boolean,
        onSuccess: () -> Unit
    ) -> Unit,
    onRequestExportCsv: (List<DockingLogEntity>) -> Unit
) {
    val opColors = LocalOperationalColors.current
    val role = currentUser.userRole

    // Admin has dual sub-tabs: 0 = Log Operasi Detail, 1 = Jejak Audit Sistem
    var adminSubTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(LogFilter.ALL) }
    var selectedStandFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var editingLog by remember { mutableStateOf<DockingLogEntity?>(null) }

    val baseLogs = remember(allLogs, currentUser) {
        if (role == UserRole.OPERATOR) {
            allLogs.filter { it.operatorId == currentUser.id }
        } else {
            allLogs
        }
    }

    val filteredLogs = remember(baseLogs, searchQuery, selectedFilter, selectedStandFilter) {
        val q = searchQuery.trim().lowercase()
        baseLogs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                LogFilter.ALL -> true
                LogFilter.PENDING -> log.parsedVerificationStatus == VerificationStatus.PENDING
                LogFilter.VERIFIED -> log.parsedVerificationStatus == VerificationStatus.VERIFIED
                LogFilter.DOCKING -> log.parsedActivityType == ActivityType.DOCKING
                LogFilter.UNDOCKING -> log.parsedActivityType == ActivityType.UNDOCKING
            }
            val matchesStand = selectedStandFilter == null || log.parkingStand == selectedStandFilter
            val matchesSearch = q.isEmpty() ||
                log.aircraftRegistration.lowercase().contains(q) ||
                log.flightNumber.lowercase().contains(q) ||
                log.parkingStand.lowercase().contains(q) ||
                log.operatorName.lowercase().contains(q) ||
                log.operationalNotes.lowercase().contains(q)
            matchesFilter && matchesStand && matchesSearch
        }
    }

    val filteredAuditLogs = remember(auditLogs, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) auditLogs else auditLogs.filter { entry ->
            entry.actorName.lowercase().contains(q) ||
                entry.action.lowercase().contains(q) ||
                entry.targetRef.lowercase().contains(q) ||
                entry.details.lowercase().contains(q)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
        ) {
            // Top Controls Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (role) {
                                UserRole.ADMIN -> "Monitoring Log & Audit Detail"
                                UserRole.SUPERVISOR -> "Verifikasi & Edit Log Operasi"
                                UserRole.OPERATOR -> "Riwayat Input Saya"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = when (role) {
                                UserRole.ADMIN -> "${allLogs.size} log operasi · ${auditLogs.size} jejak audit"
                                UserRole.SUPERVISOR -> "${baseLogs.count { it.parsedVerificationStatus == VerificationStatus.PENDING }} menunggu verifikasi"
                                UserRole.OPERATOR -> "${baseLogs.size} catatan operasi Anda"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (role.canExportSpreadsheet) {
                        Button(
                            onClick = { onRequestExportCsv(filteredLogs) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("open_export_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TableChart,
                                contentDescription = stringResource(R.string.cd_export_csv),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Sheets CSV")
                        }
                    }
                }

                if (role == UserRole.ADMIN) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = adminSubTab == 0,
                            onClick = { adminSubTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("admin_subtab_ops_logs")
                        ) {
                            Text("Log Operasi (${allLogs.size})")
                        }
                        SegmentedButton(
                            selected = adminSubTab == 1,
                            onClick = { adminSubTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("admin_subtab_audit_logs")
                        ) {
                            Text("Audit Detail (${auditLogs.size})")
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (role == UserRole.ADMIN && adminSubTab == 1) {
                                "Cari aktor, aksi, stand, atau rincian audit..."
                            } else {
                                "Cari registrasi (PK-...), no. flight, stand, petugas..."
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Bersihkan pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logs_search_input")
                )

                if (!(role == UserRole.ADMIN && adminSubTab == 1)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LogFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStandFilter == null,
                            onClick = { selectedStandFilter = null },
                            label = { Text("Semua Stand") }
                        )
                        AirportConstants.PARKING_STANDS.forEach { stand ->
                            FilterChip(
                                selected = selectedStandFilter == stand,
                                onClick = {
                                    selectedStandFilter = if (selectedStandFilter == stand) null else stand
                                },
                                label = { Text(stand) }
                            )
                        }
                    }
                }
            }

            // Content List
            if (role == UserRole.ADMIN && adminSubTab == 1) {
                if (filteredAuditLogs.isEmpty()) {
                    EmptyLogsPlaceholder(
                        title = "Belum Ada Catatan Audit",
                        subtitle = "Setiap aktivitas login, input doking/undoking, verifikasi, edit, dan manajemen user akan tercatat otomatis di sini."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAuditLogs, key = { it.id }) { entry ->
                            AuditLogCard(entry = entry)
                        }
                    }
                }
            } else {
                if (filteredLogs.isEmpty()) {
                    EmptyLogsPlaceholder(
                        title = "Belum Ada Catatan Operasi",
                        subtitle = if (role == UserRole.OPERATOR) {
                            "Mulai catat aktivitas doking atau undoking pesawat melalui tab Input Operasi."
                        } else {
                            "Tidak ada catatan yang cocok dengan filter aktif atau belum ada input operasi hari ini."
                        }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            OperationLogCard(
                                log = log,
                                canVerify = role.canVerifyLogs,
                                canEdit = role.canEditLogs,
                                onVerify = { onVerifyLog(log.id) },
                                onEdit = { editingLog = log }
                            )
                        }
                    }
                }
            }
        }
    }

    val logToEdit = editingLog
    if (logToEdit != null) {
        EditLogBottomSheet(
            log = logToEdit,
            onDismiss = { editingLog = null },
            onSave = { reg, flight, stand, act, ts, notes, markVerified ->
                onUpdateLog(
                    logToEdit.id,
                    reg,
                    flight,
                    stand,
                    act,
                    ts,
                    notes,
                    markVerified
                ) {
                    editingLog = null
                }
            }
        )
    }
}

@Composable
private fun OperationLogCard(
    log: DockingLogEntity,
    canVerify: Boolean,
    canEdit: Boolean,
    onVerify: () -> Unit,
    onEdit: () -> Unit
) {
    val opColors = LocalOperationalColors.current
    val isPending = log.parsedVerificationStatus == VerificationStatus.PENDING

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isPending) 1.2.dp else 1.dp,
            color = if (isPending) opColors.pending.copy(alpha = 0.55f) else opColors.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("operation_log_card_${log.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Stand + Registration + Activity badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = opColors.goldContainer,
                        border = BorderStroke(1.dp, opColors.goldAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = log.parkingStand,
                            style = TelemetryCodeStyle,
                            color = opColors.onGoldContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Column {
                        Text(
                            text = log.aircraftRegistration,
                            style = TelemetryCodeStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (log.flightNumber.isNotBlank()) {
                                "Flight ${log.flightNumber}"
                            } else {
                                "Tanpa nomor penerbangan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ActivityTypeBadge(log.parsedActivityType)
            }

            // Row 2: Exact Activity DateTime & Verification Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${OpsTimeFormatters.formatDate(log.activityTimestampMillis)} · ${OpsTimeFormatters.formatClock(log.activityTimestampMillis)}",
                        style = TelemetryClockStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                VerificationBadge(log.parsedVerificationStatus)
            }

            if (log.operationalNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Catatan: ${log.operationalNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            HorizontalDivider(color = opColors.cardBorder.copy(alpha = 0.6f))

            // Footer Metadata: Operator, Verifier, Last Editor
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Input oleh: ${log.operatorName} (${log.operatorEmployeeId})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (log.verifiedByName != null && log.verifiedAtMillis != null) {
                    Text(
                        text = "Diverifikasi: ${log.verifiedByName} · ${OpsTimeFormatters.formatDateTime(log.verifiedAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = opColors.verified
                    )
                }
                if (log.lastEditedByName != null) {
                    Text(
                        text = "Terakhir diedit: ${log.lastEditedByName} · ${OpsTimeFormatters.formatDateTime(log.updatedAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = opColors.goldAccent
                    )
                }
            }

            if (canVerify || canEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canVerify && isPending) {
                        Button(
                            onClick = onVerify,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("verify_log_button_${log.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = stringResource(R.string.cd_verify_log),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Verifikasi")
                        }
                    }
                    if (canEdit) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_log_button_${log.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.cd_edit_log),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Log")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogCard(entry: AuditLogEntity) {
    val opColors = LocalOperationalColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, opColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = entry.action,
                            style = MaterialTheme.typography.labelSmall,
                            color = opColors.goldAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = entry.targetRef,
                        style = TelemetryClockStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                RolePillBadge(role = UserRole.fromStored(entry.actorRole))
            }

            Text(
                text = entry.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktor: ${entry.actorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${OpsTimeFormatters.formatDate(entry.timestampMillis)} · ${OpsTimeFormatters.formatClock(entry.timestampMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyLogsPlaceholder(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditLogBottomSheet(
    log: DockingLogEntity,
    onDismiss: () -> Unit,
    onSave: (
        aircraftRegistration: String,
        flightNumber: String,
        parkingStand: String,
        activityType: ActivityType,
        activityTimestampMillis: Long,
        operationalNotes: String,
        markVerified: Boolean
    ) -> Unit
) {
    val opColors = LocalOperationalColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var reg by remember { mutableStateOf(log.aircraftRegistration) }
    var flight by remember { mutableStateOf(log.flightNumber) }
    var stand by remember { mutableStateOf(log.parkingStand) }
    var activity by remember { mutableStateOf(log.parsedActivityType) }
    var timestamp by remember { mutableLongStateOf(log.activityTimestampMillis) }
    var notes by remember { mutableStateOf(log.operationalNotes) }
    var markVerified by remember { mutableStateOf(true) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Edit Catatan Operasi #${log.id}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = reg,
                onValueChange = { reg = it.uppercase() },
                label = { Text("Registrasi Pesawat *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = flight,
                onValueChange = { flight = it.uppercase() },
                label = { Text("Nomor Penerbangan (Opsional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Aktivitas",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ActivityType.entries.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = activity == type,
                        onClick = { activity = type },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = ActivityType.entries.size)
                    ) {
                        Text(type.label)
                    }
                }
            }

            Text(
                text = "Parking Stand",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AirportConstants.PARKING_STANDS.forEach { code ->
                    FilterChip(
                        selected = stand == code,
                        onClick = { stand = code },
                        label = { Text(code) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(OpsTimeFormatters.formatDate(timestamp))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(OpsTimeFormatters.formatClock(timestamp))
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Operasional (Opsional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { markVerified = !markVerified }
            ) {
                Checkbox(
                    checked = markVerified,
                    onCheckedChange = { markVerified = it }
                )
                Text(
                    text = "Tandai otomatis sebagai Terverifikasi saat disimpan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = opColors.verified
                )
            }

            Button(
                onClick = {
                    onSave(reg, flight, stand, activity, timestamp, notes, markVerified)
                },
                enabled = reg.trim().length >= 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_edit_log_button")
            ) {
                Text("Simpan Perubahan Log")
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { picked ->
                            val cur = Calendar.getInstance().apply { timeInMillis = timestamp }
                            val p = Calendar.getInstance().apply { timeInMillis = picked }
                            cur.set(p.get(Calendar.YEAR), p.get(Calendar.MONTH), p.get(Calendar.DAY_OF_MONTH))
                            timestamp = cur.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val cal = remember(timestamp) { Calendar.getInstance().apply { timeInMillis = timestamp } }
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Ubah Jam Aktivitas") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = timestamp
                            set(Calendar.HOUR_OF_DAY, state.hour)
                            set(Calendar.MINUTE, state.minute)
                        }
                        timestamp = updated.timeInMillis
                        showTimePicker = false
                    }
                ) { Text("Terapkan") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Batal") } }
        )
    }
}
