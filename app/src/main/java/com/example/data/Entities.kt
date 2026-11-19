package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val fullName: String,
    val employeeId: String,
    val passwordHash: String,
    val role: String, // UserRole.name
    val active: Boolean = true,
    val createdBy: String = "SYSTEM",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val userRole: UserRole get() = UserRole.fromStored(role)
}

@Entity(
    tableName = "docking_logs",
    indices = [
        Index(value = ["parkingStand"]),
        Index(value = ["activityTimestampMillis"]),
        Index(value = ["operatorId"])
    ]
)
data class DockingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val aircraftRegistration: String,
    val flightNumber: String = "",
    val parkingStand: String,
    val activityType: String, // ActivityType.name
    val activityTimestampMillis: Long,
    val operationalNotes: String = "",
    val operatorId: Long,
    val operatorName: String,
    val operatorEmployeeId: String,
    val verificationStatus: String = VerificationStatus.PENDING.name,
    val verifiedByName: String? = null,
    val verifiedAtMillis: Long? = null,
    val lastEditedByName: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val parsedActivityType: ActivityType get() = ActivityType.fromStored(activityType)
    val parsedVerificationStatus: VerificationStatus get() = VerificationStatus.fromStored(verificationStatus)
}

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["timestampMillis"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val action: String, // LOGIN, CREATE_LOG, EDIT_LOG, VERIFY_LOG, ADD_USER, UPDATE_USER, EXPORT_CSV
    val actorName: String,
    val actorRole: String,
    val targetRef: String,
    val details: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
