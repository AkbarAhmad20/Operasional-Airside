package com.example.data

enum class UserRole(
    val displayLabel: String,
    val badgeShort: String,
    val description: String
) {
    ADMIN(
        displayLabel = "Admin",
        badgeShort = "ADM",
        description = "Kelola user & pantau seluruh log aktivitas serta audit detail"
    ),
    SUPERVISOR(
        displayLabel = "Supervisor",
        badgeShort = "SPV",
        description = "Input operasi, verifikasi & edit log, serta ekspor Spreadsheet"
    ),
    OPERATOR(
        displayLabel = "Operator",
        badgeShort = "OPS",
        description = "Input operasi doking/undoking & pantau status stand lapangan"
    );

    val canManageUsers: Boolean get() = this == ADMIN
    val canViewAllAudit: Boolean get() = this == ADMIN
    val canVerifyLogs: Boolean get() = this == SUPERVISOR || this == ADMIN
    val canEditLogs: Boolean get() = this == SUPERVISOR || this == ADMIN
    val canInputOperations: Boolean get() = this == OPERATOR || this == SUPERVISOR
    val canExportSpreadsheet: Boolean get() = this == SUPERVISOR || this == ADMIN
    val usesPinLogin: Boolean get() = this == OPERATOR || this == SUPERVISOR

    companion object {
        fun fromStored(raw: String): UserRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OPERATOR
    }
}

enum class LoginMode(val label: String) {
    STAFF_PIN("Petugas (PIN)"),
    ADMIN_PASSWORD("Admin (Sandi)")
}

enum class ActivityType(val label: String) {
    DOCKING("Doking"),
    UNDOCKING("Undocking");

    companion object {
        fun fromStored(raw: String): ActivityType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DOCKING
    }
}

enum class VerificationStatus(val label: String) {
    PENDING("Menunggu Verifikasi"),
    VERIFIED("Terverifikasi");

    companion object {
        fun fromStored(raw: String): VerificationStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

object AirportConstants {
    val PARKING_STANDS: List<String> = listOf(
        "B1", "B2", "B3",
        "B4R", "B5R", "B6R",
        "B07", "B08", "B09",
        "B10", "B11", "B12"
    )

    val QUICK_OPERATIONAL_NOTES: List<String> = listOf(
        "Operasional normal & aman",
        "Keterlambatan jadwal pesawat",
        "Menunggu kesiapan marshaller / chock",
        "Pengecekan teknis garbarata",
        "Kondisi cuaca hujan / angin kencang"
    )
}

data class ParkingStandLiveStatus(
    val standCode: String,
    val isDocked: Boolean,
    val activeLog: DockingLogEntity?,
    val lastLog: DockingLogEntity?
)

data class OperationFormDraft(
    val parkingStand: String = "B1",
    val activityType: ActivityType = ActivityType.DOCKING,
    val aircraftRegistration: String = "",
    val flightNumber: String = "",
    val nonce: Long = 0L
)
