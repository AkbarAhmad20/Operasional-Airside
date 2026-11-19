package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LoginMode
import com.example.data.UserRole
import com.example.ui.components.ThemeSwitcherPill
import com.example.ui.theme.LocalOperationalColors
import com.example.ui.theme.LocalThemeController
import com.example.ui.theme.TelemetryClockStyle
import com.example.ui.theme.TelemetryCodeStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    loginError: String?,
    onLogin: (username: String, secret: String, mode: LoginMode) -> Unit,
    onClearError: () -> Unit
) {
    val opColors = LocalOperationalColors.current
    val themeController = LocalThemeController.current
    var loginMode by rememberSaveable { mutableStateOf(LoginMode.STAFF_PIN) }
    var username by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var secretVisible by rememberSaveable { mutableStateOf(false) }

    val isStaffPinMode = loginMode == LoginMode.STAFF_PIN

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar with adaptive lighting badge + switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UPG · MAKASSAR",
                            style = TelemetryClockStyle,
                            color = opColors.goldAccent
                        )
                        Text(
                            text = themeController.activeReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ThemeSwitcherPill()
                }

                Spacer(Modifier.height(16.dp))

                // Branded Airside Header Card featuring Sultan Hasanuddin + Airside Operation emblem
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, opColors.goldAccent.copy(alpha = 0.45f)),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        opColors.heroBannerStart,
                                        opColors.heroBannerEnd
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF152B32),
                                border = BorderStroke(2.dp, opColors.goldAccent),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.airside_emblem),
                                    contentDescription = stringResource(R.string.cd_airside_emblem),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Image(
                                painter = painterResource(id = R.drawable.logo_sultan_hasanuddin),
                                contentDescription = stringResource(R.string.cd_sultan_hasanuddin_logo),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth(0.86f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = opColors.goldAccent.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, opColors.goldAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "GARBARATA OPS · DOKING & UNDOKING REAL-TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFE082),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Login Form Card with 2-Tab Mode Switcher: Petugas (PIN) vs Admin (Sandi)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, opColors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isStaffPinMode) "Login Petugas Lapangan" else "Login Administrator",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (isStaffPinMode) {
                                    "Operator & Supervisor masuk dengan Username dan PIN 6 digit dari Admin."
                                } else {
                                    "Admin masuk dengan Username dan Kata Sandi untuk kelola user & audit."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Segmented 2-Tab Login Mode Switcher
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_mode_tabs")
                        ) {
                            LoginMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = loginMode == mode,
                                    onClick = {
                                        if (loginMode != mode) {
                                            loginMode = mode
                                            secret = ""
                                            secretVisible = false
                                            onClearError()
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = LoginMode.entries.size
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = if (mode == LoginMode.STAFF_PIN) {
                                                Icons.Filled.Dialpad
                                            } else {
                                                Icons.Filled.AdminPanelSettings
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("login_tab_${mode.name.lowercase()}")
                                ) {
                                    Text(mode.label, maxLines = 1)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it.replace(" ", "")
                                if (loginError != null) onClearError()
                            },
                            label = {
                                Text(if (isStaffPinMode) "Username Petugas" else "Username Admin")
                            },
                            placeholder = {
                                Text(if (isStaffPinMode) "supervisor / operator" else "admin")
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Ascii
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input")
                        )

                        if (isStaffPinMode) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = secret,
                                    onValueChange = { input ->
                                        secret = input.filter { it.isDigit() }.take(6)
                                        if (loginError != null) onClearError()
                                    },
                                    label = { Text("PIN 6 Digit") },
                                    placeholder = { Text("Masukkan 6 digit angka") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Dialpad, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { secretVisible = !secretVisible },
                                            modifier = Modifier.testTag("toggle_password_visibility")
                                        ) {
                                            Icon(
                                                imageVector = if (secretVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (secretVisible) "Sembunyikan PIN" else "Tampilkan PIN"
                                            )
                                        }
                                    },
                                    supportingText = {
                                        Text(
                                            text = if (secret.length == 6) {
                                                "6/6 digit PIN siap"
                                            } else {
                                                "${secret.length}/6 digit angka"
                                            },
                                            color = if (secret.length == 6) opColors.docked else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    visualTransformation = if (secretVisible) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done,
                                        keyboardType = KeyboardType.NumberPassword
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { onLogin(username, secret, loginMode) }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_pin_input")
                                )

                                // 6-box visual PIN digit indicator
                                PinSixDigitIndicator(
                                    pinValue = secret,
                                    showDigits = secretVisible
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = secret,
                                onValueChange = {
                                    secret = it
                                    if (loginError != null) onClearError()
                                },
                                label = { Text("Kata Sandi Admin") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Lock, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { secretVisible = !secretVisible },
                                        modifier = Modifier.testTag("toggle_password_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (secretVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (secretVisible) "Sembunyikan sandi" else "Tampilkan sandi"
                                        )
                                    }
                                },
                                visualTransformation = if (secretVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done,
                                    keyboardType = KeyboardType.Password
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { onLogin(username, secret, loginMode) }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input")
                            )
                        }

                        if (!loginError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = loginError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { onLogin(username, secret, loginMode) },
                            enabled = username.isNotBlank() && if (isStaffPinMode) secret.length == 6 else secret.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isStaffPinMode) "Masuk dengan PIN" else "Masuk sebagai Admin",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        HorizontalDivider(color = opColors.cardBorder)

                        // Contextual Quick Fill for active LoginMode
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isStaffPinMode) {
                                    "AKUN DEFAULT PETUGAS (KETUK UNTUK ISI)"
                                } else {
                                    "AKUN DEFAULT ADMIN (KETUK UNTUK ISI)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isStaffPinMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickRoleChip(
                                        role = UserRole.SUPERVISOR,
                                        icon = Icons.Filled.SupervisorAccount,
                                        subtitle = "supervisor · 112233",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            username = "supervisor"
                                            secret = "112233"
                                            onClearError()
                                        }
                                    )
                                    QuickRoleChip(
                                        role = UserRole.OPERATOR,
                                        icon = Icons.Filled.Engineering,
                                        subtitle = "operator · 123456",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            username = "operator"
                                            secret = "123456"
                                            onClearError()
                                        }
                                    )
                                }
                            } else {
                                QuickRoleChip(
                                    role = UserRole.ADMIN,
                                    icon = Icons.Filled.AdminPanelSettings,
                                    subtitle = "admin · sandi default",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        username = "admin"
                                        secret = "admin123"
                                        onClearError()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Footer InJourney Airports brand mark
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, opColors.cardBorder.copy(alpha = 0.6f))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_injourney_airports),
                        contentDescription = stringResource(R.string.cd_injourney_logo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(176.dp)
                            .height(48.dp)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PinSixDigitIndicator(
    pinValue: String,
    showDigits: Boolean
) {
    val opColors = LocalOperationalColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(6) { index ->
            val filled = index < pinValue.length
            val isNext = index == pinValue.length
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (filled) {
                    opColors.goldContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = BorderStroke(
                    width = if (isNext || filled) 1.5.dp else 1.dp,
                    color = when {
                        filled -> opColors.goldAccent
                        isNext -> MaterialTheme.colorScheme.primary
                        else -> opColors.cardBorder
                    }
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (filled) {
                        if (showDigits) {
                            Text(
                                text = pinValue[index].toString(),
                                style = TelemetryCodeStyle,
                                color = opColors.onGoldContainer
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(opColors.goldAccent)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickRoleChip(
    role: UserRole,
    icon: ImageVector,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val opColors = LocalOperationalColors.current
    Surface(
        modifier = modifier
            .testTag("quick_role_${role.name.lowercase()}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, opColors.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = role.displayLabel,
                tint = opColors.goldAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = role.displayLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
