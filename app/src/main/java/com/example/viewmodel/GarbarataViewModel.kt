package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityType
import com.example.data.AirportConstants
import com.example.data.AppDatabase
import com.example.data.AuditLogEntity
import com.example.data.DockingLogEntity
import com.example.data.GarbarataRepository
import com.example.data.OperationFormDraft
import com.example.data.ParkingStandLiveStatus
import com.example.data.UserEntity
import com.example.data.UserRole
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GarbarataViewModel(
    private val repository: GarbarataRepository,
    private val themePreferences: ThemePreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.AUTO_APRON
        )

    private val _isBootstrapping = MutableStateFlow(true)
    val isBootstrapping: StateFlow<Boolean> = _isBootstrapping.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _formDraft = MutableStateFlow(OperationFormDraft())
    val formDraft: StateFlow<OperationFormDraft> = _formDraft.asStateFlow()

    val allLogs: StateFlow<List<DockingLogEntity>> = repository.allLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val standStatuses: StateFlow<List<ParkingStandLiveStatus>> = allLogs
        .map { logs ->
            val latestByStand = logs
                .groupBy { it.parkingStand }
                .mapValues { (_, standLogs) ->
                    standLogs.maxWithOrNull(
                        compareBy<DockingLogEntity> { it.activityTimestampMillis }.thenBy { it.id }
                    )
                }
            AirportConstants.PARKING_STANDS.map { stand ->
                val latest = latestByStand[stand]
                val docked = latest?.parsedActivityType == ActivityType.DOCKING
                ParkingStandLiveStatus(
                    standCode = stand,
                    isDocked = docked,
                    activeLog = if (docked) latest else null,
                    lastLog = latest
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AirportConstants.PARKING_STANDS.map {
                ParkingStandLiveStatus(it, isDocked = false, activeLog = null, lastLog = null)
            }
        )

    init {
        viewModelScope.launch {
            repository.ensureDefaultAccountsSeeded()
            val savedId = repository.savedSessionUserIdFlow.first()
            if (savedId != null) {
                val restored = repository.getUserById(savedId)
                if (restored != null && restored.active) {
                    _currentUser.value = restored
                }
            }
            _isBootstrapping.value = false
        }

        // Keep active session user synchronized if Admin edits their role or deactivates them
        viewModelScope.launch {
            allUsers.collect { users ->
                val active = _currentUser.value ?: return@collect
                val updated = users.firstOrNull { it.id == active.id }
                if (updated == null || !updated.active) {
                    _currentUser.value = null
                    repository.logout(null)
                } else if (updated != active) {
                    _currentUser.value = updated
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun login(username: String, secret: String, mode: com.example.data.LoginMode) {
        viewModelScope.launch {
            _loginError.value = null
            repository.login(username, secret, mode)
                .onSuccess { user ->
                    _currentUser.value = user
                    _snackbarMessage.value = "Selamat bertugas, ${user.fullName} (${user.userRole.displayLabel})"
                }
                .onFailure { err ->
                    _loginError.value = err.message ?: "Gagal masuk. Periksa kembali kredensial Anda."
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val actor = _currentUser.value
            _currentUser.value = null
            repository.logout(actor)
            _snackbarMessage.value = "Sesi diakhiri dengan aman."
        }
    }

    fun prefillFormFromStand(status: ParkingStandLiveStatus) {
        val nextActivity = if (status.isDocked) ActivityType.UNDOCKING else ActivityType.DOCKING
        _formDraft.value = OperationFormDraft(
            parkingStand = status.standCode,
            activityType = nextActivity,
            aircraftRegistration = if (status.isDocked) status.activeLog?.aircraftRegistration.orEmpty() else "",
            flightNumber = if (status.isDocked) status.activeLog?.flightNumber.orEmpty() else "",
            nonce = System.currentTimeMillis()
        )
    }

    fun submitDockingLog(
        aircraftRegistration: String,
        flightNumber: String,
        parkingStand: String,
        activityType: ActivityType,
        activityTimestampMillis: Long,
        operationalNotes: String,
        onSuccess: () -> Unit
    ) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createDockingLog(
                actor = actor,
                aircraftRegistration = aircraftRegistration,
                flightNumber = flightNumber,
                parkingStand = parkingStand,
                activityType = activityType,
                activityTimestampMillis = activityTimestampMillis,
                operationalNotes = operationalNotes
            ).onSuccess {
                _snackbarMessage.value = "${activityType.label} ${aircraftRegistration.trim().uppercase()} di Stand $parkingStand tersimpan."
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Gagal menyimpan catatan operasi."
            }
        }
    }

    fun verifyLog(logId: Long) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            repository.verifyDockingLog(actor, logId)
                .onSuccess {
                    _snackbarMessage.value = "Log #$logId berhasil diverifikasi."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Gagal memverifikasi log."
                }
        }
    }

    fun updateLog(
        logId: Long,
        aircraftRegistration: String,
        flightNumber: String,
        parkingStand: String,
        activityType: ActivityType,
        activityTimestampMillis: Long,
        operationalNotes: String,
        markVerified: Boolean,
        onSuccess: () -> Unit
    ) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateDockingLog(
                actor = actor,
                logId = logId,
                aircraftRegistration = aircraftRegistration,
                flightNumber = flightNumber,
                parkingStand = parkingStand,
                activityType = activityType,
                activityTimestampMillis = activityTimestampMillis,
                operationalNotes = operationalNotes,
                markVerified = markVerified
            ).onSuccess {
                _snackbarMessage.value = "Perubahan log #$logId disimpan & dicatat di audit."
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Gagal memperbarui log."
            }
        }
    }

    fun addUser(
        username: String,
        fullName: String,
        employeeId: String,
        password: String,
        role: UserRole,
        onSuccess: () -> Unit
    ) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            repository.addUser(
                actor = actor,
                username = username,
                fullName = fullName,
                employeeId = employeeId,
                password = password,
                role = role
            ).onSuccess {
                _snackbarMessage.value = "User ${fullName.trim()} (${role.displayLabel}) berhasil ditambahkan."
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.message ?: "Gagal menambahkan user."
            }
        }
    }

    fun updateUserRole(targetUserId: Long, newRole: UserRole) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserRole(actor, targetUserId, newRole)
                .onSuccess {
                    _snackbarMessage.value = "Peran petugas diperbarui ke ${newRole.displayLabel}."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Gagal mengubah peran."
                }
        }
    }

    fun toggleUserActive(targetUserId: Long) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleUserActive(actor, targetUserId)
                .onSuccess {
                    _snackbarMessage.value = "Status aktif akun petugas diperbarui."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Gagal mengubah status akun."
                }
        }
    }

    fun resetUserPassword(targetUserId: Long, newPassword: String, onSuccess: () -> Unit) {
        val actor = _currentUser.value ?: return
        val target = allUsers.value.firstOrNull { it.id == targetUserId }
        val credLabel = if (target?.userRole?.usesPinLogin == true) "PIN 6 digit" else "Kata sandi"
        viewModelScope.launch {
            repository.resetUserPassword(actor, targetUserId, newPassword)
                .onSuccess {
                    _snackbarMessage.value = "$credLabel petugas berhasil diperbarui."
                    onSuccess()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: "Gagal memperbarui kredensial."
                }
        }
    }

    fun recordExportAudit(rowCount: Int, methodLabel: String) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            repository.recordSpreadsheetExport(actor, rowCount, methodLabel)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            val db = AppDatabase.getInstance(appContext)
            val repository = GarbarataRepository(appContext, db)
            val themePreferences = ThemePreferencesRepository(appContext)
            return GarbarataViewModel(repository, themePreferences) as T
        }
    }
}
