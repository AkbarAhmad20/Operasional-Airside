package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CsvSpreadsheetExporter
import com.example.data.DockingLogEntity
import com.example.data.UserEntity
import com.example.data.UserRole
import com.example.ui.components.ExportSpreadsheetSheet
import com.example.ui.components.RolePillBadge
import com.example.ui.components.ThemeSwitcherPill
import com.example.ui.screens.AdminUsersScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.LogsAndVerificationScreen
import com.example.ui.screens.OperationInputScreen
import com.example.ui.screens.StandMonitorScreen
import com.example.ui.theme.AdaptiveThemeProvider
import com.example.ui.theme.LocalOperationalColors
import com.example.viewmodel.GarbarataViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: GarbarataViewModel = viewModel(
                factory = GarbarataViewModel.Factory(applicationContext)
            )
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()

            AdaptiveThemeProvider(
                selectedMode = themeMode,
                onSelectMode = { vm.setThemeMode(it) }
            ) {
                GarbarataOpsRoot(viewModel = vm)
            }
        }
    }
}

private enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    STAND_MONITOR("Stand Live", Icons.Filled.GridView, Icons.Outlined.GridView),
    OPERATION_INPUT("Input Operasi", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    SUPERVISOR_LOGS("Verifikasi", Icons.Filled.FactCheck, Icons.Outlined.FactCheck),
    OPERATOR_HISTORY("Riwayat Saya", Icons.Filled.History, Icons.Outlined.History),
    ADMIN_LOGS("Log & Audit", Icons.Filled.Shield, Icons.Outlined.Shield),
    ADMIN_USERS("Kelola User", Icons.Filled.Group, Icons.Outlined.Group)
}

private fun tabsForRole(role: UserRole): List<AppTab> = when (role) {
    UserRole.ADMIN -> listOf(AppTab.STAND_MONITOR, AppTab.ADMIN_LOGS, AppTab.ADMIN_USERS)
    UserRole.SUPERVISOR -> listOf(AppTab.STAND_MONITOR, AppTab.OPERATION_INPUT, AppTab.SUPERVISOR_LOGS)
    UserRole.OPERATOR -> listOf(AppTab.OPERATION_INPUT, AppTab.STAND_MONITOR, AppTab.OPERATOR_HISTORY)
}

@Composable
private fun GarbarataOpsRoot(viewModel: GarbarataViewModel) {
    val isBootstrapping by viewModel.isBootstrapping.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            isBootstrapping -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            currentUser == null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LoginScreen(
                        loginError = loginError,
                        onLogin = { u, secret, mode -> viewModel.login(u, secret, mode) },
                        onClearError = { viewModel.clearLoginError() }
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
            else -> {
                AuthenticatedWorkspace(
                    user = currentUser!!,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedWorkspace(
    user: UserEntity,
    viewModel: GarbarataViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val opColors = LocalOperationalColors.current

    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val standStatuses by viewModel.standStatuses.collectAsStateWithLifecycle()
    val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val formDraft by viewModel.formDraft.collectAsStateWithLifecycle()

    val roleTabs = remember(user.role) { tabsForRole(user.userRole) }
    var selectedTab by rememberSaveable(user.id, user.role) { mutableStateOf(roleTabs.first()) }
    if (selectedTab !in roleTabs) {
        selectedTab = roleTabs.first()
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var pendingExportLogs by remember { mutableStateOf<List<DockingLogEntity>?>(null) }
    var logsQueuedForSafSave by remember { mutableStateOf<List<DockingLogEntity>>(emptyList()) }

    // Back button returns to the primary role tab before exiting
    BackHandler(enabled = selectedTab != roleTabs.first()) {
        selectedTab = roleTabs.first()
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.let { out ->
                    CsvSpreadsheetExporter.writeCsvToStream(out, logsQueuedForSafSave)
                }
            }.onSuccess {
                viewModel.recordExportAudit(logsQueuedForSafSave.size, "Simpan File CSV")
                viewModel.showMessage("File CSV (${logsQueuedForSafSave.size} data) tersimpan & siap dibuka di Google Sheets.")
            }.onFailure {
                viewModel.showMessage("Gagal menyimpan file CSV: ${it.localizedMessage}")
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        navigationIcon = {
                            Surface(
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, opColors.goldAccent),
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 6.dp)
                                    .size(38.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.airside_emblem),
                                    contentDescription = stringResource(R.string.cd_airside_emblem),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                        },
                        title = {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Garbarata Ops",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    RolePillBadge(role = user.userRole)
                                }
                                Text(
                                    text = "${user.fullName} · SHIA UPG",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            ThemeSwitcherPill(compact = !useNavigationRail)
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier.testTag("logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = stringResource(R.string.cd_logout),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    HorizontalDivider(color = opColors.cardBorder)
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        roleTabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        roleTabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedTab) {
                        AppTab.STAND_MONITOR -> {
                            StandMonitorScreen(
                                userRole = user.userRole,
                                standStatuses = standStatuses,
                                allLogs = allLogs,
                                onQuickActionStand = { status ->
                                    viewModel.prefillFormFromStand(status)
                                    if (AppTab.OPERATION_INPUT in roleTabs) {
                                        selectedTab = AppTab.OPERATION_INPUT
                                    }
                                }
                            )
                        }
                        AppTab.OPERATION_INPUT -> {
                            OperationInputScreen(
                                standStatuses = standStatuses,
                                draft = formDraft,
                                onSubmitLog = { reg, flight, stand, act, ts, notes, onSuccess ->
                                    viewModel.submitDockingLog(
                                        aircraftRegistration = reg,
                                        flightNumber = flight,
                                        parkingStand = stand,
                                        activityType = act,
                                        activityTimestampMillis = ts,
                                        operationalNotes = notes,
                                        onSuccess = onSuccess
                                    )
                                }
                            )
                        }
                        AppTab.SUPERVISOR_LOGS, AppTab.OPERATOR_HISTORY, AppTab.ADMIN_LOGS -> {
                            LogsAndVerificationScreen(
                                currentUser = user,
                                allLogs = allLogs,
                                auditLogs = auditLogs,
                                onVerifyLog = { id -> viewModel.verifyLog(id) },
                                onUpdateLog = { id, reg, flight, stand, act, ts, notes, verify, onSuccess ->
                                    viewModel.updateLog(id, reg, flight, stand, act, ts, notes, verify, onSuccess)
                                },
                                onRequestExportCsv = { logsToExport ->
                                    pendingExportLogs = logsToExport
                                }
                            )
                        }
                        AppTab.ADMIN_USERS -> {
                            AdminUsersScreen(
                                currentUser = user,
                                allUsers = allUsers,
                                onAddUser = { u, full, nip, pwd, role, onSuccess ->
                                    viewModel.addUser(u, full, nip, pwd, role, onSuccess)
                                },
                                onUpdateRole = { uid, newRole ->
                                    viewModel.updateUserRole(uid, newRole)
                                },
                                onToggleActive = { uid ->
                                    viewModel.toggleUserActive(uid)
                                },
                                onResetPassword = { uid, pwd, onSuccess ->
                                    viewModel.resetUserPassword(uid, pwd, onSuccess)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val exportBatch = pendingExportLogs
    if (exportBatch != null) {
        ExportSpreadsheetSheet(
            rowCount = exportBatch.size,
            onDismiss = { pendingExportLogs = null },
            onShareToSheets = {
                runCatching {
                    val (_, uri) = CsvSpreadsheetExporter.createShareableCsvFile(context, exportBatch)
                    val chooser = CsvSpreadsheetExporter.createShareToSheetsIntent(context, uri, exportBatch.size)
                    context.startActivity(chooser)
                }.onSuccess {
                    viewModel.recordExportAudit(exportBatch.size, "Bagikan ke Google Sheets (CSV)")
                }.onFailure {
                    viewModel.showMessage("Gagal menyiapkan file ekspor: ${it.localizedMessage}")
                }
            },
            onSaveCsvFile = {
                logsQueuedForSafSave = exportBatch
                saveCsvLauncher.launch(CsvSpreadsheetExporter.suggestedFileName())
            },
            onCopyTsv = {
                val tsv = CsvSpreadsheetExporter.buildTsvForSheetsClipboard(exportBatch)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Garbarata Ops Spreadsheet", tsv))
                viewModel.recordExportAudit(exportBatch.size, "Salin TSV ke Clipboard")
                viewModel.showMessage("${exportBatch.size} baris disalin — tempel langsung ke Google Sheets.")
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar dari Akun?") },
            text = {
                Text("Anda akan keluar dari sesi ${user.fullName} (${user.userRole.displayLabel}). Data operasi tetap tersimpan aman di perangkat.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    modifier = Modifier.testTag("confirm_logout_button")
                ) { Text("Keluar") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") }
            }
        )
    }
}
