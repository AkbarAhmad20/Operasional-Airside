package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.ui.theme.garbarataPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GarbarataRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val userDao = db.userDao()
    private val dockingLogDao = db.dockingLogDao()
    private val auditLogDao = db.auditLogDao()

    private val sessionUserIdKey = longPreferencesKey("active_session_user_id")

    val allUsersFlow: Flow<List<UserEntity>> = userDao.observeAllUsers()
    val allLogsFlow: Flow<List<DockingLogEntity>> = dockingLogDao.observeAllLogs()
    val allAuditLogsFlow: Flow<List<AuditLogEntity>> = auditLogDao.observeAllAuditLogs()

    val savedSessionUserIdFlow: Flow<Long?> = context.garbarataPreferencesDataStore.data.map { prefs ->
        prefs[sessionUserIdKey]?.takeIf { it > 0L }
    }

    companion object {
        val PIN_6_DIGITS_REGEX = Regex("^\\d{6}$")
    }

    suspend fun ensureDefaultAccountsSeeded() {
        val existingCount = userDao.countUsers()
        if (existingCount > 0) {
            // Seamlessly upgrade legacy default Supervisor/Operator passwords to 6-digit PINs
            userDao.findByUsername("supervisor")?.let { spv ->
                if (PasswordHasher.verify("spv123", spv.passwordHash)) {
                    userDao.updateUser(spv.copy(passwordHash = PasswordHasher.hash("112233")))
                }
            }
            userDao.findByUsername("operator")?.let { ops ->
                if (PasswordHasher.verify("ops123", ops.passwordHash)) {
                    userDao.updateUser(ops.copy(passwordHash = PasswordHasher.hash("123456")))
                }
            }
            return
        }
        val now = System.currentTimeMillis()
        val defaults = listOf(
            UserEntity(
                username = "admin",
                fullName = "Administrator Airside",
                employeeId = "INJ-ADM-001",
                passwordHash = PasswordHasher.hash("admin123"),
                role = UserRole.ADMIN.name,
                active = true,
                createdBy = "SYSTEM",
                createdAtMillis = now
            ),
            UserEntity(
                username = "supervisor",
                fullName = "Supervisor Shift Apron",
                employeeId = "INJ-SPV-014",
                passwordHash = PasswordHasher.hash("112233"),
                role = UserRole.SUPERVISOR.name,
                active = true,
                createdBy = "SYSTEM",
                createdAtMillis = now
            ),
            UserEntity(
                username = "operator",
                fullName = "Operator Garbarata",
                employeeId = "INJ-OPS-108",
                passwordHash = PasswordHasher.hash("123456"),
                role = UserRole.OPERATOR.name,
                active = true,
                createdBy = "SYSTEM",
                createdAtMillis = now
            )
        )
        defaults.forEach { user ->
            runCatching { userDao.insertUser(user) }
        }
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "SYSTEM_INIT",
                actorName = "Sistem Garbarata Ops",
                actorRole = UserRole.ADMIN.name,
                targetRef = "UPG-SHIA",
                details = "Inisialisasi akun awal operasional (Admin dengan Sandi, Supervisor & Operator dengan PIN 6 digit).",
                timestampMillis = now
            )
        )
    }

    suspend fun getUserById(id: Long): UserEntity? = userDao.findById(id)

    suspend fun login(username: String, secret: String, mode: LoginMode): Result<UserEntity> {
        val cleanUsername = username.trim()
        if (cleanUsername.isBlank()) {
            return Result.failure(IllegalArgumentException("Username wajib diisi."))
        }
        if (mode == LoginMode.STAFF_PIN) {
            if (!PIN_6_DIGITS_REGEX.matches(secret)) {
                return Result.failure(IllegalArgumentException("PIN petugas harus tepat 6 digit angka."))
            }
        } else if (secret.isBlank()) {
            return Result.failure(IllegalArgumentException("Kata sandi Admin wajib diisi."))
        }

        val user = userDao.findByUsername(cleanUsername)
            ?: return Result.failure(IllegalArgumentException("Akun '$cleanUsername' tidak ditemukan."))
        if (!user.active) {
            return Result.failure(IllegalStateException("Akun ini sedang dinonaktifkan oleh Admin."))
        }

        if (mode == LoginMode.STAFF_PIN && !user.userRole.usesPinLogin) {
            return Result.failure(IllegalArgumentException("Akun Admin harus masuk melalui tab Admin (Sandi)."))
        }
        if (mode == LoginMode.ADMIN_PASSWORD && user.userRole != UserRole.ADMIN) {
            return Result.failure(IllegalArgumentException("Akun ${user.userRole.displayLabel} masuk melalui tab Petugas (PIN 6 digit)."))
        }

        if (!PasswordHasher.verify(secret, user.passwordHash)) {
            val errMsg = if (mode == LoginMode.STAFF_PIN) {
                "PIN 6 digit tidak sesuai."
            } else {
                "Kata sandi Admin tidak sesuai."
            }
            return Result.failure(IllegalArgumentException(errMsg))
        }
        context.garbarataPreferencesDataStore.edit { prefs ->
            prefs[sessionUserIdKey] = user.id
        }
        val methodDesc = if (mode == LoginMode.STAFF_PIN) "Username & PIN 6 digit" else "Username & Kata Sandi"
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "LOGIN",
                actorName = user.fullName,
                actorRole = user.role,
                targetRef = user.username,
                details = "Masuk sebagai ${user.userRole.displayLabel} (NIP: ${user.employeeId}) menggunakan $methodDesc."
            )
        )
        return Result.success(user)
    }

    suspend fun logout(actor: UserEntity?) {
        context.garbarataPreferencesDataStore.edit { prefs ->
            prefs.remove(sessionUserIdKey)
        }
        if (actor != null) {
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    action = "LOGOUT",
                    actorName = actor.fullName,
                    actorRole = actor.role,
                    targetRef = actor.username,
                    details = "Keluar dari sesi operasional."
                )
            )
        }
    }

    suspend fun createDockingLog(
        actor: UserEntity,
        aircraftRegistration: String,
        flightNumber: String,
        parkingStand: String,
        activityType: ActivityType,
        activityTimestampMillis: Long,
        operationalNotes: String
    ): Result<Long> {
        val cleanReg = aircraftRegistration.trim().uppercase()
        val cleanFlight = flightNumber.trim().uppercase()
        val cleanNotes = operationalNotes.trim()

        if (cleanReg.length < 3) {
            return Result.failure(IllegalArgumentException("Registrasi pesawat minimal 3 karakter (contoh: PK-GFA)."))
        }
        if (parkingStand !in AirportConstants.PARKING_STANDS) {
            return Result.failure(IllegalArgumentException("Parking stand tidak valid."))
        }

        val now = System.currentTimeMillis()
        // If Supervisor inputs directly, it can still start as VERIFIED or PENDING; keeping Supervisor self-verified is realistic,
        // while Operator inputs await Supervisor verification.
        val initialVerification = if (actor.userRole == UserRole.SUPERVISOR) {
            VerificationStatus.VERIFIED
        } else {
            VerificationStatus.PENDING
        }

        val entity = DockingLogEntity(
            aircraftRegistration = cleanReg,
            flightNumber = cleanFlight,
            parkingStand = parkingStand,
            activityType = activityType.name,
            activityTimestampMillis = activityTimestampMillis,
            operationalNotes = cleanNotes,
            operatorId = actor.id,
            operatorName = actor.fullName,
            operatorEmployeeId = actor.employeeId,
            verificationStatus = initialVerification.name,
            verifiedByName = if (initialVerification == VerificationStatus.VERIFIED) actor.fullName else null,
            verifiedAtMillis = if (initialVerification == VerificationStatus.VERIFIED) now else null,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        val newId = dockingLogDao.insertLog(entity)
        val flightPart = if (cleanFlight.isNotBlank()) " ($cleanFlight)" else ""
        val notePart = if (cleanNotes.isNotBlank()) " · Catatan: $cleanNotes" else ""
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "CREATE_LOG",
                actorName = actor.fullName,
                actorRole = actor.role,
                targetRef = "Stand $parkingStand · $cleanReg",
                details = "Mencatat ${activityType.label} pesawat $cleanReg$flightPart di Parking Stand $parkingStand$notePart."
            )
        )
        return Result.success(newId)
    }

    suspend fun verifyDockingLog(actor: UserEntity, logId: Long): Result<Unit> {
        if (!actor.userRole.canVerifyLogs) {
            return Result.failure(SecurityException("Hanya Supervisor atau Admin yang dapat memverifikasi log."))
        }
        val existing = dockingLogDao.findById(logId)
            ?: return Result.failure(IllegalArgumentException("Data log tidak ditemukan."))
        val now = System.currentTimeMillis()
        dockingLogDao.updateLog(
            existing.copy(
                verificationStatus = VerificationStatus.VERIFIED.name,
                verifiedByName = actor.fullName,
                verifiedAtMillis = now,
                updatedAtMillis = now
            )
        )
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "VERIFY_LOG",
                actorName = actor.fullName,
                actorRole = actor.role,
                targetRef = "Log #$logId · ${existing.parkingStand}",
                details = "Memverifikasi log ${existing.parsedActivityType.label} pesawat ${existing.aircraftRegistration} di Stand ${existing.parkingStand}."
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateDockingLog(
        actor: UserEntity,
        logId: Long,
        aircraftRegistration: String,
        flightNumber: String,
        parkingStand: String,
        activityType: ActivityType,
        activityTimestampMillis: Long,
        operationalNotes: String,
        markVerified: Boolean
    ): Result<Unit> {
        if (!actor.userRole.canEditLogs) {
            return Result.failure(SecurityException("Anda tidak memiliki izin mengedit catatan operasi."))
        }
        val existing = dockingLogDao.findById(logId)
            ?: return Result.failure(IllegalArgumentException("Catatan operasi tidak ditemukan."))

        val cleanReg = aircraftRegistration.trim().uppercase()
        val cleanFlight = flightNumber.trim().uppercase()
        val cleanNotes = operationalNotes.trim()

        if (cleanReg.length < 3) {
            return Result.failure(IllegalArgumentException("Registrasi pesawat minimal 3 karakter."))
        }
        if (parkingStand !in AirportConstants.PARKING_STANDS) {
            return Result.failure(IllegalArgumentException("Parking stand tidak valid."))
        }

        val changes = buildList {
            if (existing.aircraftRegistration != cleanReg) add("Reg: ${existing.aircraftRegistration} -> $cleanReg")
            if (existing.flightNumber != cleanFlight) add("Flight: '${existing.flightNumber}' -> '$cleanFlight'")
            if (existing.parkingStand != parkingStand) add("Stand: ${existing.parkingStand} -> $parkingStand")
            if (existing.activityType != activityType.name) add("Aktivitas: ${existing.parsedActivityType.label} -> ${activityType.label}")
            if (existing.activityTimestampMillis != activityTimestampMillis) add("Waktu aktivitas diperbarui")
            if (existing.operationalNotes != cleanNotes) add("Catatan diperbarui")
        }

        val now = System.currentTimeMillis()
        val newStatus = if (markVerified) VerificationStatus.VERIFIED.name else existing.verificationStatus
        val verifiedBy = if (markVerified) actor.fullName else existing.verifiedByName
        val verifiedAt = if (markVerified) now else existing.verifiedAtMillis

        dockingLogDao.updateLog(
            existing.copy(
                aircraftRegistration = cleanReg,
                flightNumber = cleanFlight,
                parkingStand = parkingStand,
                activityType = activityType.name,
                activityTimestampMillis = activityTimestampMillis,
                operationalNotes = cleanNotes,
                verificationStatus = newStatus,
                verifiedByName = verifiedBy,
                verifiedAtMillis = verifiedAt,
                lastEditedByName = actor.fullName,
                updatedAtMillis = now
            )
        )

        val diffText = if (changes.isEmpty()) "Konfirmasi ulang data log." else changes.joinToString("; ")
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "EDIT_LOG",
                actorName = actor.fullName,
                actorRole = actor.role,
                targetRef = "Log #$logId · $parkingStand",
                details = "Mengedit log $cleanReg: $diffText"
            )
        )
        return Result.success(Unit)
    }

    suspend fun addUser(
        actor: UserEntity,
        username: String,
        fullName: String,
        employeeId: String,
        password: String,
        role: UserRole
    ): Result<Long> {
        if (!actor.userRole.canManageUsers) {
            return Result.failure(SecurityException("Hanya Admin yang dapat menambahkan user baru."))
        }
        val cleanUser = username.trim().lowercase()
        val cleanName = fullName.trim()
        val cleanNip = employeeId.trim().uppercase()

        if (cleanUser.length < 3 || cleanUser.contains(" ")) {
            return Result.failure(IllegalArgumentException("Username minimal 3 karakter tanpa spasi."))
        }
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Nama lengkap wajib diisi."))
        }
        if (role.usesPinLogin) {
            if (!PIN_6_DIGITS_REGEX.matches(password)) {
                return Result.failure(IllegalArgumentException("PIN ${role.displayLabel} harus tepat 6 digit angka."))
            }
        } else if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Kata sandi Admin minimal 6 karakter."))
        }
        if (userDao.findByUsername(cleanUser) != null) {
            return Result.failure(IllegalArgumentException("Username '$cleanUser' sudah terdaftar."))
        }

        val newId = userDao.insertUser(
            UserEntity(
                username = cleanUser,
                fullName = cleanName,
                employeeId = cleanNip.ifBlank { "INJ-${role.badgeShort}-${(100..999).random()}" },
                passwordHash = PasswordHasher.hash(password),
                role = role.name,
                active = true,
                createdBy = actor.fullName
            )
        )
        val credLabel = if (role.usesPinLogin) "dengan PIN 6 digit" else "dengan Kata Sandi Admin"
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "ADD_USER",
                actorName = actor.fullName,
                actorRole = actor.role,
                targetRef = cleanUser,
                details = "Menambahkan user baru '$cleanName' ($cleanUser) sebagai ${role.displayLabel} $credLabel."
            )
        )
        return Result.success(newId)
    }

    suspend fun updateUserRole(actor: UserEntity, targetUserId: Long, newRole: UserRole): Result<Unit> {
        if (!actor.userRole.canManageUsers) {
            return Result.failure(SecurityException("Hanya Admin yang dapat mengubah peran user."))
        }
        val target = userDao.findById(targetUserId)
            ?: return Result.failure(IllegalArgumentException("User tidak ditemukan."))
        if (target.id == actor.id && newRole != UserRole.ADMIN) {
            return Result.failure(IllegalArgumentException("Admin tidak dapat menurunkan perannya sendiri."))
        }
        userDao.updateUser(target.copy(role = newRole.name))
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "UPDATE_USER",
                actorName = actor.fullName,
                actorRole = actor.role,
                targetRef = target.username,
                details = "Mengubah peran ${target.fullName} dari ${target.userRole.displayLabel} menjadi ${newRole.displayLabel}."
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleUserActive(actor: UserEntity, targetUserId: Long): Result<Unit> {
        if (!actor.userRole.canManageUsers) {
            return Result.failure(SecurityException("Hanya Admin yang dapat mengubah status akun."))
        }
        val target = userDao.findById(targetUserId)
            ?: return Result.failure(IllegalArgumentException("User tidak ditemukan."))
        if (target.id == actor.id) {
            return Result.failure(IllegalArgumentException("Anda tidak dapat menonaktifkan akun yang sedang aktif."))
        }
        val nextActive = !target.active
        userDao.updateUser(target.copy(active = nextActive))
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "UPDATE_USER",
                actorName = actor.fullName,
                actorRole = actor.role,
                targetRef = target.username,
                details = if (nextActive) "Mengaktifkan kembali akun ${target.fullName}." else "Menonaktifkan akun ${target.fullName}."
            )
        )
        return Result.success(Unit)
    }

    suspend fun resetUserPassword(actor: UserEntity, targetUserId: Long, newPassword: String): Result<Unit> {
        if (!actor.userRole.canManageUsers) {
            return Result.failure(SecurityException("Hanya Admin yang dapat mereset PIN atau kata sandi."))
        }
        val target = userDao.findById(targetUserId)
            ?: return Result.failure(IllegalArgumentException("User tidak ditemukan."))
        if (target.userRole.usesPinLogin) {
            if (!PIN_6_DIGITS_REGEX.matches(newPassword)) {
                return Result.failure(IllegalArgumentException("PIN baru untuk ${target.userRole.displayLabel} harus tepat 6 digit angka."))
            }
        } else if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Kata sandi baru Admin minimal 6 karakter."))
        }
        userDao.updateUser(target.copy(passwordHash = PasswordHasher.hash(newPassword)))
        val resetDetail = if (target.userRole.usesPinLogin) {
            "Mereset PIN 6 digit akun ${target.fullName} (${target.userRole.displayLabel})."
        } else {
            "Mereset kata sandi akun Admin ${target.fullName}."
        }
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "UPDATE_USER",
                actorName = actor.fullName,
                actorRole = actor.role,
                targetRef = target.username,
                details = resetDetail
            )
        )
        return Result.success(Unit)
    }

    suspend fun recordSpreadsheetExport(actor: UserEntity, rowCount: Int, methodLabel: String) {
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "EXPORT_CSV",
                actorName = actor.fullName,
                actorRole = actor.role,
                targetRef = "$rowCount baris",
                details = "Mengekspor $rowCount catatan operasi garbarata ke Spreadsheet ($methodLabel)."
            )
        )
    }
}
