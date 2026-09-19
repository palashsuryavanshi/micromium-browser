package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Decrypted login entry. Only ever populated while the vault is unlocked. */
data class VaultLogin(
    val id: Long,
    val site: String,
    val username: String,
    val password: String
)

/**
 * On-device password vault. Everything is encrypted at rest (Tink/Keystore)
 * and gated behind a master password (optionally via biometrics). CSV import
 * understands the export dialects of Chrome, Brave, Opera, Edge (Chromium)
 * and Firefox.
 */
class VaultRepository(private val appContext: Context) {

    private val dao: VaultDao = VaultDatabase.getInstance(appContext).vaultDao()

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _hasPassword = MutableStateFlow(false)
    val hasPassword: StateFlow<Boolean> = _hasPassword.asStateFlow()

    private val _logins = MutableStateFlow<List<VaultLogin>>(emptyList())
    val logins: StateFlow<List<VaultLogin>> = _logins.asStateFlow()

    suspend fun refreshMeta() {
        _hasPassword.value = dao.getMeta(VaultDatabase.META_PASSWORD_VERIFIER) != null
        if (!_unlocked.value) _logins.value = emptyList()
    }

    suspend fun setupPassword(password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(password.length >= 4) { "Use at least 4 characters for the device password." }
            dao.putMeta(
                VaultMetaEntity(
                    VaultDatabase.META_PASSWORD_VERIFIER,
                    VaultCrypto.hashPassword(password)
                )
            )
            _hasPassword.value = true
            _unlocked.value = true
            reloadLogins()
        }
    }

    suspend fun unlock(password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val stored = dao.getMeta(VaultDatabase.META_PASSWORD_VERIFIER)
                ?: throw IllegalStateException("No device password set yet.")
            require(VaultCrypto.verifyPassword(password, stored)) { "Wrong password. Try again." }
            _unlocked.value = true
            reloadLogins()
        }
    }

    /** Called after a successful BiometricPrompt: no password needed. */
    suspend fun unlockWithBiometrics(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(dao.getMeta(VaultDatabase.META_PASSWORD_VERIFIER) != null) {
                "No device password set yet."
            }
            _unlocked.value = true
            reloadLogins()
        }
    }

    suspend fun setBiometricAllowed(allowed: Boolean) {
        withContext(Dispatchers.IO) {
            dao.putMeta(VaultMetaEntity(VaultDatabase.META_BIOMETRIC_ALLOWED, allowed.toString()))
        }
    }

    suspend fun isBiometricAllowed(): Boolean {
        return dao.getMeta(VaultDatabase.META_BIOMETRIC_ALLOWED) == "true"
    }

    fun lock() {
        _unlocked.value = false
        _logins.value = emptyList()
    }

    suspend fun addLogin(site: String, username: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                checkUnlocked()
                require(site.isNotBlank()) { "Site can't be empty." }
                require(password.isNotBlank()) { "Password can't be empty." }
                val trimmedSite = site.trim()
                dao.insert(
                    VaultLoginEntity(
                        site = trimmedSite,
                        host = hostOf(trimmedSite),
                        username = username.trim(),
                        encryptedPassword = VaultCrypto.encryptToBase64(appContext, password)
                    )
                )
                reloadLogins()
            }
        }

    suspend fun deleteLogin(id: Long) {
        withContext(Dispatchers.IO) {
            if (!_unlocked.value) return@withContext
            dao.deleteById(id)
            reloadLogins()
        }
    }

    /**
     * Imports a password CSV exported from Chrome, Brave, Opera, Edge
     * (header: name,url,username,password[,note]) or Firefox
     * (header contains url,username,password,httpRealm,...).
     * Returns the number of newly inserted logins.
     */
    suspend fun importCsv(content: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            checkUnlocked()
            val rows = parseCsv(content)
            require(rows.isNotEmpty()) { "That file doesn't look like a password CSV." }
            val header = rows.first().map { it.trim().lowercase() }
            val urlIndex: Int
            val userIndex: Int
            val passIndex: Int
            val nameIndex: Int
            if ("httprealm" in header) {
                // Firefox dialect.
                urlIndex = header.indexOf("url")
                userIndex = header.indexOf("username")
                passIndex = header.indexOf("password")
                nameIndex = -1
            } else {
                // Chromium dialect (Chrome, Brave, Opera, Edge).
                urlIndex = header.indexOf("url").takeIf { it >= 0 } ?: header.indexOf("origin")
                userIndex = header.indexOf("username")
                passIndex = header.indexOf("password")
                nameIndex = header.indexOf("name")
            }
            require(urlIndex >= 0 && userIndex >= 0 && passIndex >= 0) {
                "Unrecognized CSV header. Export passwords as CSV from your browser and try again."
            }
            var inserted = 0
            for (row in rows.drop(1)) {
                val url = row.getOrNull(urlIndex)?.trim().orEmpty()
                val username = row.getOrNull(userIndex)?.trim().orEmpty()
                val password = row.getOrNull(passIndex) ?: continue
                if (password.isEmpty()) continue
                val site = row.getOrNull(nameIndex)?.trim()?.takeIf { it.isNotEmpty() } ?: url.ifEmpty { continue }
                val host = hostOf(url.ifEmpty { site })
                if (dao.findByHostAndUsername(host, username) != null) continue
                dao.insert(
                    VaultLoginEntity(
                        site = site,
                        host = host,
                        username = username,
                        encryptedPassword = VaultCrypto.encryptToBase64(appContext, password)
                    )
                )
                inserted++
            }
            reloadLogins()
            inserted
        }
    }

    private fun checkUnlocked() {
        check(_unlocked.value) { "Vault is locked." }
    }

    private suspend fun reloadLogins() {
        if (!_unlocked.value) {
            _logins.value = emptyList()
            return
        }
        _logins.value = dao.getAll().mapNotNull { entity ->
            try {
                VaultLogin(
                    id = entity.id,
                    site = entity.site,
                    username = entity.username,
                    password = VaultCrypto.decryptFromBase64(appContext, entity.encryptedPassword)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        fun hostOf(siteOrUrl: String): String {
            val input = siteOrUrl.trim()
            if (input.isEmpty()) return ""
            val withScheme = if (input.contains("://")) input else "https://$input"
            return try {
                val uri = android.net.Uri.parse(withScheme)
                (uri.host ?: input).lowercase().removePrefix("www.")
            } catch (e: Exception) {
                input.lowercase()
            }
        }

        /** Minimal CSV parser: handles quoted fields, escaped quotes, CRLF. */
        fun parseCsv(content: String): List<List<String>> {
            val rows = mutableListOf<List<String>>()
            var current = mutableListOf<String>()
            val field = StringBuilder()
            var inQuotes = false
            var i = 0
            fun endField() {
                current.add(field.toString())
                field.clear()
            }
            while (i < content.length) {
                val c = content[i]
                when {
                    inQuotes && c == '"' -> {
                        if (i + 1 < content.length && content[i + 1] == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    }
                    inQuotes -> field.append(c)
                    c == '"' && field.isEmpty() -> inQuotes = true
                    c == ',' -> endField()
                    c == '\r' -> { /* skip, \n ends the row */ }
                    c == '\n' -> {
                        endField()
                        if (current.any { it.isNotBlank() }) rows.add(current.toList())
                        current = mutableListOf()
                    }
                    else -> field.append(c)
                }
                i++
            }
            endField()
            if (current.any { it.isNotBlank() }) rows.add(current.toList())
            return rows
        }
    }
}
