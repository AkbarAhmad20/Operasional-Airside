package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.UserEntity
import com.example.data.UserRole
import com.example.ui.components.OpsTimeFormatters
import com.example.ui.components.RolePillBadge
import com.example.ui.theme.LocalOperationalColors
import com.example.ui.theme.TelemetryClockStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    currentUser: UserEntity,
    allUsers: List<UserEntity>,
    onAddUser: (
        username: String,
        fullName: String,
        employeeId: String,
        secret: String,
        role: UserRole,
        onSuccess: () -> Unit
    ) -> Unit,
    onUpdateRole: (userId: Long, newRole: UserRole) -> Unit,
    onToggleActive: (userId: Long) -> Unit,
    onResetPassword: (userId: Long, newSecret: String, onSuccess: () -> Unit) -> Unit
) {
    val opColors = LocalOperationalColors.current
    var roleFilter by rememberSaveable { mutableStateOf<UserRole?>(null) }
    var showAddUserSheet by remember { mutableStateOf(false) }
    var managingUser by remember { mutableStateOf<UserEntity?>(null) }
    var resetSecretTarget by remember { mutableStateOf<UserEntity?>(null) }

    val filteredUsers = remember(allUsers, roleFilter) {
        if (roleFilter == null) allUsers else allUsers.filter { it.userRole == roleFilter }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Manajemen Akun Petugas",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${allUsers.size} akun terdaftar · ${allUsers.count { it.active }} aktif",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showAddUserSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_user_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = stringResource(R.string.cd_add_user),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Tambah User")
                    }
                }

                // Role filter strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = roleFilter == null,
                        onClick = { roleFilter = null },
                        label = { Text("Semua (${allUsers.size})") }
                    )
                    UserRole.entries.forEach { role ->
                        val count = allUsers.count { it.userRole == role }
                        FilterChip(
                            selected = roleFilter == role,
                            onClick = { roleFilter = if (roleFilter == role) null else role },
                            label = { Text("${role.displayLabel} ($count)") }
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredUsers, key = { it.id }) { user ->
                    val isSelf = user.id == currentUser.id
                    val usesPin = user.userRole.usesPinLogin

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (user.active) opColors.cardBorder else MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_card_${user.username}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = user.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelf) {
                                            Text(
                                                text = "(Anda)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = opColors.goldAccent
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@${user.username} · NIP ${user.employeeId}",
                                        style = TelemetryClockStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RolePillBadge(role = user.userRole)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (usesPin) {
                                        "Login: Username + PIN 6 Digit · Dibuat ${OpsTimeFormatters.formatDate(user.createdAtMillis)}"
                                    } else {
                                        "Login: Username + Kata Sandi · Dibuat ${OpsTimeFormatters.formatDate(user.createdAtMillis)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (user.active) "AKTIF" else "NONAKTIF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (user.active) opColors.docked else MaterialTheme.colorScheme.error
                                )
                            }

                            HorizontalDivider(color = opColors.cardBorder.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { managingUser = user },
                                        enabled = !isSelf,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("manage_role_button_${user.username}")
                                    ) {
                                        Icon(Icons.Filled.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("Ubah Peran")
                                    }
                                    OutlinedButton(
                                        onClick = { resetSecretTarget = user },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("reset_pwd_button_${user.username}")
                                    ) {
                                        Icon(
                                            imageVector = if (usesPin) Icons.Filled.Dialpad else Icons.Filled.Key,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(if (usesPin) "Reset PIN" else "Reset Sandi")
                                    }
                                }

                                Switch(
                                    checked = user.active,
                                    onCheckedChange = { onToggleActive(user.id) },
                                    enabled = !isSelf,
                                    modifier = Modifier.testTag("toggle_active_${user.username}")
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddUserSheet = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_user_fab")
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = stringResource(R.string.cd_add_user)
            )
        }
    }

    if (showAddUserSheet) {
        AddUserBottomSheet(
            onDismiss = { showAddUserSheet = false },
            onSubmit = { username, fullName, nip, secret, role ->
                onAddUser(username, fullName, nip, secret, role) {
                    showAddUserSheet = false
                }
            }
        )
    }

    val targetRoleUser = managingUser
    if (targetRoleUser != null) {
        var pickedRole by remember(targetRoleUser) { mutableStateOf(targetRoleUser.userRole) }
        val authModeChanged = pickedRole.usesPinLogin != targetRoleUser.userRole.usesPinLogin

        AlertDialog(
            onDismissRequest = { managingUser = null },
            title = { Text("Ubah Peran: ${targetRoleUser.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pilih peran akses baru untuk @${targetRoleUser.username}:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        UserRole.entries.forEachIndexed { idx, r ->
                            SegmentedButton(
                                selected = pickedRole == r,
                                onClick = { pickedRole = r },
                                shape = SegmentedButtonDefaults.itemShape(idx, UserRole.entries.size)
                            ) {
                                Text(r.displayLabel)
                            }
                        }
                    }
                    Text(
                        text = pickedRole.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (authModeChanged) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = opColors.pendingContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (pickedRole.usesPinLogin) {
                                    "Catatan: Setelah peran diubah ke ${pickedRole.displayLabel}, lakukan Reset PIN (6 digit angka) agar user bisa login di tab Petugas."
                                } else {
                                    "Catatan: Setelah peran diubah ke Admin, lakukan Reset Sandi agar user bisa login di tab Admin."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = opColors.pending,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateRole(targetRoleUser.id, pickedRole)
                        managingUser = null
                    }
                ) { Text("Simpan Peran") }
            },
            dismissButton = {
                TextButton(onClick = { managingUser = null }) { Text("Batal") }
            }
        )
    }

    val targetResetUser = resetSecretTarget
    if (targetResetUser != null) {
        val isPinAccount = targetResetUser.userRole.usesPinLogin
        var newSecret by remember(targetResetUser) { mutableStateOf("") }
        var showSecret by remember(targetResetUser) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { resetSecretTarget = null },
            title = {
                Text(
                    if (isPinAccount) "Reset PIN 6 Digit: ${targetResetUser.fullName}"
                    else "Reset Sandi Admin: ${targetResetUser.fullName}"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isPinAccount) {
                            "Ketik PIN baru sebanyak tepat 6 digit angka untuk @${targetResetUser.username} (${targetResetUser.userRole.displayLabel})."
                        } else {
                            "Masukkan kata sandi baru minimal 6 karakter untuk Admin @${targetResetUser.username}."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newSecret,
                        onValueChange = { input ->
                            newSecret = if (isPinAccount) {
                                input.filter { it.isDigit() }.take(6)
                            } else {
                                input
                            }
                        },
                        label = {
                            Text(if (isPinAccount) "PIN Baru (6 Digit Angka)" else "Kata Sandi Baru")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isPinAccount) Icons.Filled.Dialpad else Icons.Filled.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showSecret = !showSecret }) {
                                Icon(
                                    imageVector = if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showSecret) "Sembunyikan" else "Tampilkan"
                                )
                            }
                        },
                        supportingText = if (isPinAccount) {
                            {
                                Text(
                                    text = "${newSecret.length}/6 digit angka",
                                    color = if (newSecret.length == 6) opColors.docked else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isPinAccount) KeyboardType.NumberPassword else KeyboardType.Password
                        ),
                        visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_secret_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = if (isPinAccount) newSecret.length == 6 else newSecret.length >= 6,
                    onClick = {
                        onResetPassword(targetResetUser.id, newSecret) {
                            resetSecretTarget = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_reset_secret_button")
                ) {
                    Text(if (isPinAccount) "Simpan PIN" else "Simpan Sandi")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetSecretTarget = null }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUserBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (
        username: String,
        fullName: String,
        employeeId: String,
        secret: String,
        role: UserRole
    ) -> Unit
) {
    val opColors = LocalOperationalColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.OPERATOR) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val isPinRole = selectedRole.usesPinLogin

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tambah User Operasional Baru",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Operator & Supervisor menggunakan Username + PIN 6 digit angka. Admin menggunakan Kata Sandi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Peran Akses *",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                UserRole.entries.forEachIndexed { idx, role ->
                    SegmentedButton(
                        selected = selectedRole == role,
                        onClick = {
                            if (selectedRole != role) {
                                val switchingType = selectedRole.usesPinLogin != role.usesPinLogin
                                selectedRole = role
                                if (switchingType) secret = ""
                                errorMsg = null
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, UserRole.entries.size),
                        modifier = Modifier.testTag("add_user_role_${role.name.lowercase()}")
                    ) {
                        Text(role.displayLabel)
                    }
                }
            }
            Text(
                text = selectedRole.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMsg = null
                },
                label = { Text("Nama Lengkap *") },
                placeholder = { Text("Contoh: Budi Santoso") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_user_fullname_input")
            )

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().replace(" ", "")
                    errorMsg = null
                },
                label = { Text("Username Login *") },
                placeholder = { Text("Contoh: budi.ops") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_user_username_input")
            )

            OutlinedTextField(
                value = employeeId,
                onValueChange = { employeeId = it.uppercase() },
                label = { Text("NIP / ID Petugas (Opsional)") },
                placeholder = { Text("Contoh: INJ-OPS-204") },
                leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_user_nip_input")
            )

            OutlinedTextField(
                value = secret,
                onValueChange = { input ->
                    secret = if (isPinRole) {
                        input.filter { it.isDigit() }.take(6)
                    } else {
                        input
                    }
                    errorMsg = null
                },
                label = {
                    Text(
                        if (isPinRole) "PIN 6 Digit * (Ketik Angka)"
                        else "Kata Sandi Admin * (min. 6 karakter)"
                    )
                },
                placeholder = {
                    Text(if (isPinRole) "Contoh: 458210" else "Minimal 6 karakter")
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPinRole) Icons.Filled.Dialpad else Icons.Filled.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            imageVector = if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showSecret) "Sembunyikan" else "Tampilkan"
                        )
                    }
                },
                supportingText = if (isPinRole) {
                    {
                        Text(
                            text = if (secret.length == 6) {
                                "6/6 digit PIN valid"
                            } else {
                                "${secret.length}/6 digit angka (ketik manual oleh Admin)"
                            },
                            color = if (secret.length == 6) opColors.docked else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPinRole) KeyboardType.NumberPassword else KeyboardType.Password
                ),
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_user_password_input")
            )

            if (!errorMsg.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    when {
                        fullName.trim().length < 2 -> errorMsg = "Nama lengkap wajib diisi."
                        username.trim().length < 3 -> errorMsg = "Username minimal 3 karakter."
                        isPinRole && secret.length != 6 -> errorMsg = "PIN ${selectedRole.displayLabel} harus tepat 6 digit angka."
                        !isPinRole && secret.length < 6 -> errorMsg = "Kata sandi Admin minimal 6 karakter."
                        else -> onSubmit(username, fullName, employeeId, secret, selectedRole)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_user_confirm_button")
            ) {
                Text(
                    if (isPinRole) "Simpan Akun ${selectedRole.displayLabel} (PIN)"
                    else "Simpan Akun Admin"
                )
            }
        }
    }
}
