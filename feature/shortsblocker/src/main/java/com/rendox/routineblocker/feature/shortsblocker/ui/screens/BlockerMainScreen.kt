package com.rendox.routineblocker.feature.shortsblocker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rendox.routineblocker.feature.shortsblocker.ui.components.TrackedPackagesSection
import kotlin.math.roundToInt

private val DAY_NAMES = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

@Composable
fun BlockerMainScreen(
    state: com.rendox.routineblocker.feature.shortsblocker.ui.viewmodels.BlockerUiState,
    isOnboarding: Boolean,
    showDisclosure: Boolean,
    onCompleteOnboarding: () -> Unit,
    onAcceptDisclosure: () -> Unit,
    onDismissDisclosure: () -> Unit,
    onCheckService: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePackage: (String, Boolean) -> Unit,
    onUpdateAllowedDays: (Set<Int>) -> Unit = {},
    onUpdateDailyQuota: (Int) -> Unit = {},
    onToggleAppBlock: () -> Unit = {},
    onUpdateAppBlockedDays: (Set<Int>) -> Unit = {},
    onSetPassword: (String) -> Unit = {},
    onChangePassword: (String, String) -> Unit = { _: String, _: String -> },
    onUnlock: (String) -> Unit = {},
    onClearPasswordError: () -> Unit = {},
    onToggleBlocker: () -> Unit = {},
    onLock: () -> Unit = {},
    onActivateDeviceAdmin: () -> Unit = {},
    onRefreshAdminStatus: () -> Unit = {},
) {
    if (showDisclosure) {
        ProminentDisclosureScreen(
            onAgree = onAcceptDisclosure,
            onCancel = onDismissDisclosure,
        )
        return
    }

    if (!state.isOnboardingCompleted) {
        OnboardingScreen(
            onComplete = onCompleteOnboarding,
        )
        return
    }

    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Shorts Blocker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (state.isServiceEnabled) {
            ServiceActiveCard()
        } else {
            SetupRequiredCard(onOpenSettings)
        }

        Spacer(modifier = Modifier.height(16.dp))

        BlockerToggleCard(
            enabled = state.blockerEnabled,
            onToggle = onToggleBlocker,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordSection(
            hasPassword = state.hasPassword,
            isUnlocked = state.isUnlocked,
            onSetPasswordClick = { showSetPasswordDialog = true },
            onChangePasswordClick = { showChangePasswordDialog = true },
            onUnlockClick = { showUnlockDialog = true },
            onLockClick = onLock,
        )

        Spacer(modifier = Modifier.height(16.dp))

        DeviceAdminSection(
            isActive = state.isDeviceAdminActive,
            onActivate = onActivateDeviceAdmin,
            onRefresh = onRefreshAdminStatus,
        )

        Spacer(modifier = Modifier.height(24.dp))

        val isLocked = state.hasPassword && !state.isUnlocked

        Box(
            modifier = if (isLocked) {
                Modifier
                    .fillMaxWidth()
                    .alpha(0.5f)
                    .clickable { showUnlockDialog = true }
            } else Modifier.fillMaxWidth()
        ) {
            Column {
                AllowedDaysSection(
                    allowedDays = state.allowedDays,
                    onToggleDay = if (isLocked) {{}} else { day ->
                        val newDays = if (day in state.allowedDays) {
                            state.allowedDays - day
                        } else {
                            state.allowedDays + day
                        }
                        onUpdateAllowedDays(newDays)
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                DailyQuotaSection(
                    quotaMinutes = state.dailyQuotaMinutes,
                    onQuotaChange = if (isLocked) {{}} else onUpdateDailyQuota,
                    usedMinutes = state.usedMinutesToday,
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppBlockSection(
                    appBlockEnabled = state.appBlockEnabled,
                    appBlockedDays = state.appBlockedDays,
                    onToggleAppBlock = if (isLocked) {{}} else onToggleAppBlock,
                    onToggleDay = if (isLocked) {{}} else { day ->
                        val newDays = if (day in state.appBlockedDays) {
                            state.appBlockedDays - day
                        } else {
                            state.appBlockedDays + day
                        }
                        onUpdateAppBlockedDays(newDays)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TrackedPackagesSection(
            packages = state.trackedPackages,
            onPackageToggle = if (isLocked) {{ _,_ -> }} else onTogglePackage,
        )

        Spacer(modifier = Modifier.height(24.dp))

        FooterSection()
    }

    if (showSetPasswordDialog) {
        SetPasswordDialog(
            password = passwordInput,
            confirmPassword = confirmPasswordInput,
            showPassword = showPassword,
            error = if (passwordInput.length in 1..3) "Minimo 4 caracteres" else if (confirmPasswordInput.isNotEmpty() && passwordInput != confirmPasswordInput) "Senhas nao conferem" else null,
            onPasswordChange = { passwordInput = it; onClearPasswordError() },
            onConfirmPasswordChange = { confirmPasswordInput = it },
            onToggleVisibility = { showPassword = !showPassword },
            onConfirm = {
                if (passwordInput.length >= 4 && passwordInput == confirmPasswordInput) {
                    onSetPassword(passwordInput)
                    passwordInput = ""
                    confirmPasswordInput = ""
                    showSetPasswordDialog = false
                }
            },
            onDismiss = {
                showSetPasswordDialog = false
                passwordInput = ""
                confirmPasswordInput = ""
            },
        )
    }

    if (showUnlockDialog) {
        UnlockDialog(
            password = passwordInput,
            showPassword = showPassword,
            error = state.passwordError,
            onPasswordChange = { passwordInput = it; onClearPasswordError() },
            onToggleVisibility = { showPassword = !showPassword },
            onConfirm = {
                onUnlock(passwordInput)
            },
            onDismiss = {
                showUnlockDialog = false
                passwordInput = ""
                onClearPasswordError()
            },
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            currentPassword = passwordInput,
            newPassword = confirmPasswordInput,
            showPassword = showPassword,
            error = state.passwordError,
            onCurrentPasswordChange = { passwordInput = it; onClearPasswordError() },
            onNewPasswordChange = { confirmPasswordInput = it },
            onToggleVisibility = { showPassword = !showPassword },
            onConfirm = { newPw ->
                if (newPw.length >= 4) {
                    onChangePassword(passwordInput, newPw)
                }
            },
            onDismiss = {
                showChangePasswordDialog = false
                passwordInput = ""
                confirmPasswordInput = ""
                onClearPasswordError()
            },
        )
    }

    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) {
            showUnlockDialog = false
            passwordInput = ""
        }
    }

    LaunchedEffect(state.passwordChangedSuccessfully) {
        if (state.passwordChangedSuccessfully) {
            showChangePasswordDialog = false
            passwordInput = ""
            confirmPasswordInput = ""
        }
    }
}

@Composable
private fun AllowedDaysSection(
    allowedDays: Set<Int>,
    onToggleDay: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dias com liberação parcial",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Nestes dias, o bloqueio só age depois dos minutos liberados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (i in 1..7) {
                    FilterChip(
                        selected = i in allowedDays,
                        onClick = { onToggleDay(i) },
                        label = { Text(DAY_NAMES[i - 1], style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBlockSection(
    appBlockEnabled: Boolean,
    appBlockedDays: Set<Int>,
    onToggleAppBlock: () -> Unit,
    onToggleDay: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bloquear abertura do Instagram",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Impede o Instagram de abrir nos dias selecionados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = appBlockEnabled,
                    onCheckedChange = { onToggleAppBlock() },
                )
            }

            if (appBlockEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Dias de bloqueio total",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nestes dias, o Instagram não abre",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (i in 1..7) {
                        FilterChip(
                            selected = i in appBlockedDays,
                            onClick = { onToggleDay(i) },
                            label = { Text(DAY_NAMES[i - 1], style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuotaSection(
    quotaMinutes: Int,
    onQuotaChange: (Int) -> Unit,
    usedMinutes: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cota diária",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Limite de minutos por dia nos dias liberados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$quotaMinutes min",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = quotaMinutes.toFloat(),
                onValueChange = { onQuotaChange(it.roundToInt()) },
                valueRange = 0f..120f,
                steps = 23,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("120 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (quotaMinutes > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val remaining = (quotaMinutes - usedMinutes).coerceAtLeast(0)
                val fraction = if (quotaMinutes > 0) usedMinutes.toFloat() / quotaMinutes else 0f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (remaining > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Usados hoje: $usedMinutes min · Restam: $remaining min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ServiceActiveCard() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Service Active",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Content blocker is running",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SetupRequiredCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Setup Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Enable accessibility service to block short-form content automatically",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "How to enable:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                Text("1. Tap the button below to open settings")
                Text("2. Find the app in the list")
                Text("3. Toggle the switch to enable")
                Text("4. Return to this app")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun BlockerToggleCard(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bloqueio de Reels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (enabled) "Ativo - quota e dias sao aplicados"
                    else "Desativado - Reels liberados sem restricao",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@Composable
private fun PasswordSection(
    hasPassword: Boolean,
    isUnlocked: Boolean,
    onSetPasswordClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onLockClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protecao por senha",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasPassword) "Configuracoes protegidas" else "Nenhuma senha definida",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!hasPassword) {
                    FilledTonalButton(onClick = onSetPasswordClick) {
                        Text("Definir senha")
                    }
                } else if (isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Desbloqueado",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onChangePasswordClick) {
                        Text("Alterar senha")
                    }
                    TextButton(onClick = onLockClick) {
                        Text("Bloquear")
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onUnlockClick) {
                        Text("Desbloquear")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAdminSection(
    isActive: Boolean,
    onActivate: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Administrador do dispositivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isActive) "Ativado - impede desinstalacao sem senha"
                        else "Desativado - app pode ser desinstalado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Admin ativo",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    FilledTonalButton(onClick = onActivate) {
                        Text("Ativar")
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        onRefresh()
    }
}

@Composable
private fun SetPasswordDialog(
    password: String,
    confirmPassword: String,
    showPassword: Boolean,
    error: String?,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Definir senha de protecao") },
        text = {
            Column {
                Text(
                    text = "Esta senha sera exigida para alterar configuracoes.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Senha") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    trailingIcon = {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirmar senha") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    trailingIcon = {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = password.length >= 4 && confirmPassword.isNotEmpty() && password == confirmPassword,
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun UnlockDialog(
    password: String,
    showPassword: Boolean,
    error: String?,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Desbloquear configuracoes") },
        text = {
            Column {
                Text(
                    text = "Digite a senha para alterar as configuracoes.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Senha") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    trailingIcon = {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = password.isNotEmpty(),
            ) {
                Text("Desbloquear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun ChangePasswordDialog(
    currentPassword: String,
    newPassword: String,
    showPassword: Boolean,
    error: String?,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar senha") },
        text = {
            Column {
                Text(
                    text = "Digite a senha atual e a nova senha.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = { Text("Senha atual") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    trailingIcon = {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("Nova senha") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    trailingIcon = {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newPassword) },
                enabled = currentPassword.isNotEmpty() && newPassword.length >= 4,
            ) {
                Text("Alterar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun FooterSection() {
    Text(
        text = "Shorts Blocker",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
