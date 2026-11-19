package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.security.MessageDigest

object PasswordHasher {
    private const val SALT = "SHIA_INJOURNEY_AVIOBRIDGE_OPS_2026"

    fun hash(rawPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$SALT:$rawPassword".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(rawPassword: String, expectedHash: String): Boolean =
        hash(rawPassword) == expectedHash
}

@Database(
    entities = [
        UserEntity::class,
        DockingLogEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun dockingLogDao(): DockingLogDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "garbarata_ops.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
