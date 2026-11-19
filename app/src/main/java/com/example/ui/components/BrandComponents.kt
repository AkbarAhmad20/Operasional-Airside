package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ActivityType
import com.example.data.UserRole
import com.example.data.VerificationStatus
import com.example.ui.theme.LocalOperationalColors
import com.example.ui.theme.TelemetryClockStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OpsTimeFormatters {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val shortTimeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val fullDateTimeFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("id", "ID"))

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))
    fun formatClock(millis: Long): String = timeFormat.format(Date(millis))
    fun formatShortTime(millis: Long): String = shortTimeFormat.format(Date(millis))
    fun formatDateTime(millis: Long): String = fullDateTimeFormat.format(Date(millis))

    fun formatElapsedDuration(startMillis: Long, nowMillis: Long): String {
        val diffSec = ((nowMillis - startMillis).coerceAtLeast(0L)) / 1000L
        val hours = diffSec / 3600L
        val minutes = (diffSec % 3600L) / 60L
        val seconds = diffSec % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}

@Composable
fun LivePulseDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    Box(
        modifier = modifier
            .size(9.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun RolePillBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val opColors = LocalOperationalColors.current
    val (bg, fg) = when (role) {
        UserRole.ADMIN -> opColors.goldContainer to opColors.onGoldContainer
        UserRole.SUPERVISOR -> opColors.verifiedContainer to opColors.verified
        UserRole.OPERATOR -> opColors.dockedContainer to opColors.docked
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Text(
            text = role.displayLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ActivityTypeBadge(
    activityType: ActivityType,
    modifier: Modifier = Modifier
) {
    val opColors = LocalOperationalColors.current
    val (bg, fg) = when (activityType) {
        ActivityType.DOCKING -> opColors.dockedContainer to opColors.docked
        ActivityType.UNDOCKING -> opColors.undockingContainer to opColors.undocking
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
    ) {
        Text(
            text = activityType.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VerificationBadge(
    status: VerificationStatus,
    modifier: Modifier = Modifier
) {
    val opColors = LocalOperationalColors.current
    val isVerified = status == VerificationStatus.VERIFIED
    val bg = if (isVerified) opColors.verifiedContainer else opColors.pendingContainer
    val fg = if (isVerified) opColors.verified else opColors.pending
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isVerified) Icons.Filled.Verified else Icons.Outlined.Schedule,
                contentDescription = status.label,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isVerified) "Terverifikasi" else "Menunggu",
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSpreadsheetSheet(
    rowCount: Int,
    onDismiss: () -> Unit,
    onShareToSheets: () -> Unit,
    onSaveCsvFile: () -> Unit,
    onCopyTsv: () -> Unit
) {
    val opColors = LocalOperationalColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = opColors.dockedContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.TableChart,
                            contentDescription = "Spreadsheet",
                            tint = opColors.docked,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Ekspor Data ke Google Sheets",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$rowCount baris catatan operasi siap diekspor (UTF-8 CSV)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, opColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "KOLOM TERSTRUKTUR OTOMATIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = opColors.goldAccent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ID Log · Registrasi Pesawat · No. Penerbangan · Parking Stand · Aktivitas · Tanggal · Waktu · Catatan Operasional · Operator · Status Verifikasi · Verifikator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    onDismiss()
                    onShareToSheets()
                },
                enabled = rowCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_share_sheets_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bagikan / Buka di Google Sheets (.csv)")
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onSaveCsvFile()
                },
                enabled = rowCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_save_csv_button")
            ) {
                Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Simpan File CSV ke Perangkat / Drive")
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onCopyTsv()
                },
                enabled = rowCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_copy_tsv_button")
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Salin Tabel (Tempel Langsung di Sheets)")
            }

            if (rowCount == 0) {
                Text(
                    text = "Belum ada catatan operasi untuk diekspor.",
                    style = TelemetryClockStyle,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
