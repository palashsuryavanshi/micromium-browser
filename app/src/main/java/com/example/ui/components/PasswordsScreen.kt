package com.example.ui.components

import android.app.KeyguardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.VaultLogin

/**
 * On-device password manager. Everything is stored encrypted locally (Tink +
 * Android Keystore) behind a device password, optionally via biometrics.
 * Nothing here uses the network.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordsScreen(
    hasPassword: Boolean,
    unlocked: Boolean,
    logins: List<VaultLogin>,
    busy: Boolean,
    error: String?,
    lastImportCount: Int?,
    biometricEnrolled: Boolean,
    biometricAllowed: Boolean,
    onSetupPassword: (password: String, enableBiometric: Boolean) -> Unit,
    onUnlock: (password: String) -> Unit,
    onBiometricUnlock: () -> Unit,
    onSetBiometricAllowed: (Boolean) -> Unit,
    onLock: () -> Unit,
    onAdd: (site: String, username: String, password: String) -> Unit,
    onDelete: (id: Long) -> Unit,
    onImportCsv: (content: String) -> Unit,
    onClearError: () -> Unit,
    onClearImportCount: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag("passwords_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Passwords") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("passwords_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unlocked) {
                        IconButton(
                            onClick = onLock,
                            modifier = Modifier.testTag("passwords_lock")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock vault")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (unlocked) {
                var showAdd by remember { mutableStateOf(false) }
                FloatingActionButton(
                    onClick = { showAdd = true },
                    modifier = Modifier.testTag("passwords_add")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add login")
                }
                if (showAdd) {
                    AddLoginDialog(
                        onDismiss = { showAdd = false },
                        onSave = { site, username, password ->
                            onAdd(site, username, password)
                            showAdd = false
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasPassword) {
                VaultSetupGate(
                    enabled = !busy,
                    biometricEnrolled = biometricEnrolled,
                    onSetup = onSetupPassword
                )
            } else if (!unlocked) {
                VaultUnlockGate(
                    enabled = !busy,
                    biometricEnrolled = biometricEnrolled,
                    biometricAllowed = biometricAllowed,
                    onUnlock = { onClearError(); onUnlock(it) },
                    onBiometricUnlock = onBiometricUnlock,
                    onSetBiometricAllowed = onSetBiometricAllowed
                )
            } else {
                VaultLoginList(
                    logins = logins,
                    onDelete = onDelete,
                    onImportCsv = onImportCsv,
                    lastImportCount = lastImportCount,
                    onClearImportCount = onClearImportCount
                )
            }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("passwords_error")
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.testTag("passwords_busy"))
            }
        }
    }
}

@Composable
private fun VaultSetupGate(
    enabled: Boolean,
    biometricEnrolled: Boolean,
    onSetup: (password: String, enableBiometric: Boolean) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(false) }
    val match = password.length >= 4 && password == confirm

    Text(
        text = "Set a device password",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
    Text(
        text = "It unlocks your saved passwords on this device only. Nothing is uploaded anywhere.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    PasswordField(value = password, onValueChange = { password = it }, label = "Device password", enabled = enabled, testTag = "passwords_setup_password")
    PasswordField(value = confirm, onValueChange = { confirm = it }, label = "Confirm password", enabled = enabled, testTag = "passwords_setup_confirm")
    if (biometricEnrolled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = enableBiometric,
                onCheckedChange = { enableBiometric = it },
                enabled = enabled,
                modifier = Modifier.testTag("passwords_setup_biometric")
            )
            Text("Also allow biometric unlock", style = MaterialTheme.typography.bodyMedium)
        }
    }
    Button(
        onClick = { onSetup(password, enableBiometric) },
        enabled = enabled && match,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("passwords_setup_submit")
    ) {
        Text("Create password")
    }
}

@Composable
private fun VaultUnlockGate(
    enabled: Boolean,
    biometricEnrolled: Boolean,
    biometricAllowed: Boolean,
    onUnlock: (password: String) -> Unit,
    onBiometricUnlock: () -> Unit,
    onSetBiometricAllowed: (Boolean) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var deviceLockNotice by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyguardManager = remember(context) {
        context.getSystemService(KeyguardManager::class.java)
    }
    // System lock-screen auth (PIN / pattern / biometrics, whatever the device
    // offers). Works from any activity — no Fragment dependency.
    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            deviceLockNotice = false
            onBiometricUnlock()
        }
    }

    fun launchDeviceCredential() {
        val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
            "Unlock passwords",
            "Confirm your screen lock to unlock this device's vault"
        )
        if (intent != null) {
            deviceLockNotice = false
            deviceCredentialLauncher.launch(intent)
        } else {
            deviceLockNotice = true
        }
    }

    Text(
        text = "Vault is locked",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
    PasswordField(value = password, onValueChange = { password = it }, label = "Device password", enabled = enabled, testTag = "passwords_unlock_password")
    Button(
        onClick = { onUnlock(password) },
        enabled = enabled && password.isNotEmpty(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("passwords_unlock_submit")
    ) {
        Text("Unlock")
    }
    if (biometricEnrolled) {
        OutlinedButton(
            onClick = {
                if (!biometricAllowed) {
                    onSetBiometricAllowed(true)
                }
                launchDeviceCredential()
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("passwords_unlock_biometric")
        ) {
            Text("Unlock with biometrics")
        }
        if (deviceLockNotice) {
            Text(
                text = "No screen lock set on this device. Set a PIN, pattern, or biometrics in system Settings first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun VaultLoginList(
    logins: List<VaultLogin>,
    onDelete: (id: Long) -> Unit,
    onImportCsv: (content: String) -> Unit,
    lastImportCount: Int?,
    onClearImportCount: () -> Unit
) {
    val context = LocalContext.current
    val csvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader(Charsets.UTF_8).readText()
                        .removePrefix("\uFEFF")
                    onClearImportCount()
                    onImportCsv(text)
                }
            } catch (e: Exception) {
                onImportCsv("")
            }
        }
    }

    OutlinedButton(
        onClick = { csvPicker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values")) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("passwords_import_csv")
    ) {
        Icon(Icons.Default.UploadFile, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Import passwords CSV (Chrome, Brave, Opera, Edge, Firefox)")
    }

    if (lastImportCount != null) {
        Text(
            text = if (lastImportCount > 0) "Imported $lastImportCount login(s)." else "Nothing new to import.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("passwords_import_result")
        )
    }

    if (logins.isEmpty()) {
        Text(
            text = "No saved passwords yet. Add one with + or import a CSV.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logins, key = { it.id }) { login ->
                VaultLoginRow(login = login, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun VaultLoginRow(login: VaultLogin, onDelete: (id: Long) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("passwords_login_${login.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = login.site,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = login.username.ifEmpty { "(no username)" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (revealed) login.password else "••••••••",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { revealed = !revealed },
                modifier = Modifier.testTag("passwords_reveal_${login.id}")
            ) {
                Icon(
                    imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (revealed) "Hide password" else "Show password"
                )
            }
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(login.password))
                },
                modifier = Modifier.testTag("passwords_copy_${login.id}")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy password")
            }
            IconButton(
                onClick = { onDelete(login.id) },
                modifier = Modifier.testTag("passwords_delete_${login.id}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete login")
            }
        }
    }
}

@Composable
private fun AddLoginDialog(onDismiss: () -> Unit, onSave: (site: String, username: String, password: String) -> Unit) {
    var site by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val valid = site.isNotBlank() && password.isNotBlank()

    androidx.compose.material3.    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add login") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = site,
                    onValueChange = { site = it },
                    label = { Text("Site or URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PasswordField(value = password, onValueChange = { password = it }, label = "Password", enabled = true, testTag = "passwords_add_password")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(site.trim(), username.trim(), password) },
                enabled = valid,
                modifier = Modifier.testTag("passwords_add_save")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    testTag: String
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide" else "Show"
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

/** Whether this device has a secure screen lock (PIN / pattern / biometrics). */
fun isBiometricEnrolled(context: android.content.Context): Boolean {
    val manager = context.getSystemService(KeyguardManager::class.java)
    return manager?.isDeviceSecure == true
}
