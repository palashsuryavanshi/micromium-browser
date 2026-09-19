package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

/**
 * On-device password vault. Passwords are stored encrypted with Tink
 * (AES-256-GCM, key in Android Keystore); the master password only gates
 * access inside the app and is itself stored as a salted PBKDF2 verifier.
 * Nothing here ever leaves the device.
 */
@Entity(tableName = "vault_logins")
data class VaultLoginEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val site: String,
    val host: String,
    val username: String,
    val encryptedPassword: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_meta")
data class VaultMetaEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_logins ORDER BY site COLLATE NOCASE ASC, username COLLATE NOCASE ASC")
    suspend fun getAll(): List<VaultLoginEntity>

    @Query("SELECT * FROM vault_logins WHERE host = :host AND username = :username LIMIT 1")
    suspend fun findByHostAndUsername(host: String, username: String): VaultLoginEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(login: VaultLoginEntity): Long

    @Query("DELETE FROM vault_logins WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT value FROM vault_meta WHERE `key` = :key LIMIT 1")
    suspend fun getMeta(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(meta: VaultMetaEntity)
}

@Database(
    entities = [VaultLoginEntity::class, VaultMetaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        const val META_PASSWORD_VERIFIER = "master_password_verifier"
        const val META_BIOMETRIC_ALLOWED = "biometric_allowed"

        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
