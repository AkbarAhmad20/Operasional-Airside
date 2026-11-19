package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DockingLogEntity
import com.example.data.ParkingStandLiveStatus
import com.example.data.UserRole
import com.example.data.VerificationStatus
import com.example.ui.components.ActivityTypeBadge
import com.example.ui.components.LivePulseDot
import com.example.ui.components.OpsTimeFormatters
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.LocalOperationalColors
import com.example.ui.theme.StandCodeDisplayStyle
import com.example.ui.theme.TelemetryClockStyle
import com.example.ui.theme.TelemetryCodeStyle
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandMonitorScreen(
    userRole: UserRole,
    standStatuses: List<ParkingStandLiveStatus>,
    allLogs: List<DockingLogEntity>,
    onQuickActionStand: (ParkingStandLiveStatus) -> Unit
) {
    val opColors = LocalOperationalColors.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var inspectedStand by remember { mutableStateOf<ParkingStandLiveStatus?>(null) }

    // 1-second real-time ticker for apron clock & live docked duration timers
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val dockedCount = remember(standStatuses) { standStatuses.count { it.isDocked } }
    val freeCount = standStatuses.size - dockedCount
    val startOfTodayMillis = remember(nowMillis / 60_000L) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayOpsCount = remember(allLogs, startOfTodayMillis) {
        allLogs.count { it.activityTimestampMillis >= startOfTodayMillis }
    }
    val pendingVerifyCount = remember(allLogs) {
        allLogs.count { it.parsedVerificationStatus == VerificationStatus.PENDING }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gridColumns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 540.dp -> 3
            else -> 2
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Live Apron Clock & Summary Banner
            item(span = { GridItemSpan(gridColumns) }) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LivePulseDot(color = opColors.docked)
                                Column {
                                    Text(
                                        text = "PELACAKAN WAKTU NYATA · APRON B",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = opColors.goldAccent
                                    )
                                    Text(
                                        text = "12 Parking Stand Garbarata (B1–B12)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = OpsTimeFormatters.formatClock(nowMillis),
                                        style = TelemetryCodeStyle,
                                        color = opColors.goldAccent
                                    )
                                    Text(
                                        text = OpsTimeFormatters.formatDate(nowMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KpiTile(
                                label = "Terisi / Doking",
                                value = "$dockedCount",
                                accent = opColors.docked,
                                container = opColors.dockedContainer,
                                modifier = Modifier.weight(1f)
                            )
                            KpiTile(
                                label = "Stand Tersedia",
                                value = "$freeCount",
                                accent = MaterialTheme.colorScheme.onSurface,
                                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            )
                            KpiTile(
                                label = "Log Hari Ini",
                                value = "$todayOpsCount",
                                accent = opColors.goldAccent,
                                container = opColors.goldContainer,
                                modifier = Modifier.weight(1f)
                            )
                            KpiTile(
                                label = "Pending Verif",
                                value = "$pendingVerifyCount",
                                accent = opColors.pending,
                                container = opColors.pendingContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Grid of 12 Parking Stands (B1, B2, B3, B4R, B5R, B6R, B07..B12)
            items(standStatuses, key = { it.standCode }) { stand ->
                ParkingStandCard(
                    status = stand,
                    nowMillis = nowMillis,
                    onClick = { inspectedStand = stand }
                )
            }
        }
    }

    val currentInspected = inspectedStand
    if (currentInspected != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { inspectedStand = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PARKING STAND ${currentInspected.standCode}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentInspected.isDocked) {
                                "Garbarata terpasang (Pesawat sedang Doking)"
                            } else {
                                "Stand kosong & siap menerima pesawat"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (currentInspected.isDocked) opColors.dockedContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = if (currentInspected.isDocked) "DOCKED" else "TERSEDIA",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentInspected.isDocked) opColors.docked else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                val latest = currentInspected.lastLog
                if (latest != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, opColors.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = latest.aircraftRegistration + if (latest.flightNumber.isNotBlank()) " · ${latest.flightNumber}" else "",
                                    style = TelemetryCodeStyle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                ActivityTypeBadge(latest.parsedActivityType)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Waktu: ${OpsTimeFormatters.formatDateTime(latest.activityTimestampMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                VerificationBadge(latest.parsedVerificationStatus)
                            }
                            if (currentInspected.isDocked) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = opColors.docked,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Durasi doking berjalan: ${OpsTimeFormatters.formatElapsedDuration(latest.activityTimestampMillis, nowMillis)}",
                                        style = TelemetryClockStyle,
                                        color = opColors.docked
                                    )
                                }
                            }
                            Text(
                                text = "Petugas: ${latest.operatorName} (${latest.operatorEmployeeId})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (latest.operationalNotes.isNotBlank()) {
                                Text(
                                    text = "Catatan: ${latest.operationalNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada riwayat aktivitas pada Parking Stand ${currentInspected.standCode}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (userRole.canInputOperations) {
                    Button(
                        onClick = {
                            val selected = currentInspected
                            inspectedStand = null
                            onQuickActionStand(selected)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("stand_quick_action_button")
                    ) {
                        Icon(
                            imageVector = if (currentInspected.isDocked) Icons.Filled.FlightTakeoff else Icons.Filled.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (currentInspected.isDocked) {
                                "Catat Undocking Stand ${currentInspected.standCode}"
                            } else {
                                "Catat Doking di Stand ${currentInspected.standCode}"
                            }
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = opColors.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Mode pantau Admin aktif. Input operasi dilakukan oleh Supervisor atau Operator.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = container
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ParkingStandCard(
    status: ParkingStandLiveStatus,
    nowMillis: Long,
    onClick: () -> Unit
) {
    val opColors = LocalOperationalColors.current
    val active = status.activeLog
    val isDocked = status.isDocked && active != null

    val borderColor = if (isDocked) opColors.docked else opColors.cardBorder
    val containerColor = if (isDocked) {
        opColors.dockedContainer.copy(alpha = 0.48f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stand_card_${status.standCode}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(if (isDocked) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.standCode,
                    style = StandCodeDisplayStyle,
                    color = if (isDocked) opColors.docked else MaterialTheme.colorScheme.onSurface
                )
                if (isDocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        LivePulseDot(color = opColors.docked)
                        Text(
                            text = "DOKING",
                            style = MaterialTheme.typography.labelSmall,
                            color = opColors.docked
                        )
                    }
                } else {
                    Text(
                        text = "SIAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isDocked && active != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FlightLand,
                            contentDescription = null,
                            tint = opColors.goldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = active.aircraftRegistration,
                            style = TelemetryCodeStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = active.flightNumber.ifBlank { "No. Flight -" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = OpsTimeFormatters.formatShortTime(active.activityTimestampMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = OpsTimeFormatters.formatElapsedDuration(active.activityTimestampMillis, nowMillis),
                            style = TelemetryClockStyle,
                            color = opColors.docked
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val last = status.lastLog
                    Text(
                        text = if (last != null) {
                            "Terakhir: ${last.aircraftRegistration}\nUndocking ${OpsTimeFormatters.formatShortTime(last.activityTimestampMillis)}"
                        } else {
                            "Garbarata standby\nKetuk untuk catat doking"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
