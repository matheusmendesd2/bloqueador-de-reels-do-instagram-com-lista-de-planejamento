package com.rendox.routineblocker.feature.shortsblocker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rendox.routineblocker.feature.shortsblocker.models.AppSchedule
import com.rendox.routineblocker.feature.shortsblocker.models.BlockerSettings
import com.rendox.routineblocker.feature.shortsblocker.models.ShortsPolicy
import com.rendox.routineblocker.feature.shortsblocker.models.dayLabelLong
import com.rendox.routineblocker.feature.shortsblocker.models.formatDuration
import com.rendox.routineblocker.feature.shortsblocker.ui.BlockerActions
import com.rendox.routineblocker.feature.shortsblocker.ui.StatusTone
import com.rendox.routineblocker.feature.shortsblocker.ui.components.CardHeader
import com.rendox.routineblocker.feature.shortsblocker.ui.components.HintBanner
import com.rendox.routineblocker.feature.shortsblocker.ui.components.IconBadge
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SectionCard
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SectionLabel
import com.rendox.routineblocker.feature.shortsblocker.ui.todayStatus
import com.rendox.routineblocker.feature.shortsblocker.ui.viewmodels.BlockerUiState
import com.rendox.routineblocker.feature.shortsblocker.utils.PackageConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockerHomeScreen(
    state: BlockerUiState,
    actions: BlockerActions,
    onOpenAppSchedule: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onUnlockRequest: () -> Unit,
    onUnlockProtectionRequest: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bloqueio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = dayLabelLong(state.today),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (state.hasPassword) {
                        IconButton(
                            onClick = { if (state.isLocked) onUnlockRequest() else actions.lock() },
                        ) {
                            Icon(
                                imageVector = if (state.isLocked) {
                                    Icons.Default.Lock
                                } else {
                                    Icons.Default.LockOpen
                                },
                                contentDescription = if (state.isLocked) {
                                    "Desbloquear configurações"
                                } else {
                                    "Bloquear configurações"
                                },
                                tint = if (state.isLocked) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Tune, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.isServiceEnabled) {
                ServiceSetupCard(onOpenSettings = actions.openAccessibilitySettings)
            }

            ProtectionCard(
                state = state,
                onToggleProtection = actions.setProtectionEnabled,
                onUnlockProtectionRequest = onUnlockProtectionRequest,
                onEmergencyUnlock = actions.useEmergencyUnlock,
                onCancelPause = actions.cancelPause,
            )

            SectionLabel(text = "Apps")

            PackageConstants.AVAILABLE_PACKAGES.forEach { app ->
                AppSummaryCard(
                    schedule = state.scheduleFor(app.packageName),
                    displayName = app.displayName,
                    shortFormName = app.shortFormName,
                    usedMinutes = state.usedMinutes(app.packageName),
                    dayOfWeek = state.today,
                    minuteOfDay = state.minuteOfDay,
                    protectionActive = state.isProtectionActiveNow,
                    canEdit = state.canEdit,
                    onToggleMonitored = { monitored ->
                        actions.setMonitored(app.packageName, monitored)
                    },
                    onClick = { onOpenAppSchedule(app.packageName) },
                )
            }

            if (state.monitoredSchedules.isEmpty()) {
                HintBanner(
                    text = "Nenhum app está sendo monitorado. Ative o Instagram ou o YouTube " +
                        "acima para começar.",
                    icon = Icons.Default.Warning,
                )
            }

            val hasQuotaToday = state.monitoredSchedules.any {
                it.day(state.today).shorts == ShortsPolicy.COTA
            }
            if (hasQuotaToday) {
                SectionLabel(text = "Consumo de hoje")
                UsageCard(state = state, onReset = actions.resetTodayUsage)
            }

            if (state.isLocked) {
                HintBanner(
                    text = "As configurações estão protegidas por senha. Toque no cadeado para " +
                        "desbloquear.",
                    icon = Icons.Default.Lock,
                )
            }
        }
    }
}

@Composable
private fun ProtectionCard(
    state: BlockerUiState,
    onToggleProtection: (Boolean) -> Unit,
    onUnlockProtectionRequest: () -> Unit,
    onEmergencyUnlock: () -> Unit,
    onCancelPause: () -> Unit,
) {
    val active = state.isProtectionActiveNow
    val container = when {
        active -> MaterialTheme.colorScheme.primaryContainer
        state.isPaused -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val onContainer = when {
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        state.isPaused -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    SectionCard(containerColor = container) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = if (active) Icons.Default.Shield else Icons.Default.PauseCircle,
                tint = onContainer,
                size = 48.dp,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        active -> "Proteção ativa"
                        state.isPaused -> "Pausada"
                        else -> "Proteção desligada"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onContainer,
                )
                Text(
                    text = when {
                        state.isPaused ->
                            "Volta em ${formatDuration(state.pauseRemainingMinutes)}"
                        active -> "Sua agenda está sendo aplicada"
                        else -> "Nenhuma regra está sendo aplicada"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer,
                )
            }
            Switch(
                checked = state.settings.protectionEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled && state.isLocked) {
                        onUnlockProtectionRequest()
                    } else {
                        onToggleProtection(enabled)
                    }
                },
                enabled = state.settings.protectionEnabled || state.canRelaxProtection,
            )
        }

        if (!state.canRelaxProtection) {
            Spacer(modifier = Modifier.height(14.dp))
            HintBanner(
                text = "Modo rígido: não dá para desligar a proteção enquanto um bloqueio de " +
                    "horário está valendo. A liberação de emergência segue disponível 1x por dia.",
                icon = Icons.Default.Lock,
                container = MaterialTheme.colorScheme.errorContainer,
                onContainer = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        AnimatedVisibility(visible = state.isPaused) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCancelPause,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retomar agora")
                }
            }
        }

        AnimatedVisibility(
            visible = state.settings.protectionEnabled && !state.isPaused,
        ) {
            Column {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Emergência",
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainer,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onEmergencyUnlock,
                    enabled = state.canUseEmergencyUnlock,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Liberar por ${formatDuration(BlockerSettings.EMERGENCY_UNLOCK_MINUTES)}",
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (state.emergencyUnlockUsedToday) {
                        "Já usado hoje. Volta amanhã."
                    } else {
                        "Disponível uma vez por dia, sem senha."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer,
                )
            }
        }
    }
}

@Composable
private fun AppSummaryCard(
    schedule: AppSchedule,
    displayName: String,
    shortFormName: String,
    usedMinutes: Int,
    dayOfWeek: Int,
    minuteOfDay: Int,
    protectionActive: Boolean,
    canEdit: Boolean,
    onToggleMonitored: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val status = todayStatus(
        schedule = schedule,
        shortFormName = shortFormName,
        dayOfWeek = dayOfWeek,
        minuteOfDay = minuteOfDay,
        usedMinutes = usedMinutes,
        protectionActive = protectionActive,
    )
    val toneColor = when (status.tone) {
        StatusTone.OK -> MaterialTheme.colorScheme.primary
        StatusTone.WARN -> MaterialTheme.colorScheme.tertiary
        StatusTone.BLOCKED -> MaterialTheme.colorScheme.error
        StatusTone.OFF -> MaterialTheme.colorScheme.outline
    }

    SectionCard(
        modifier = Modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color = toneColor)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = status.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = toneColor,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = status.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = schedule.monitored,
                onCheckedChange = onToggleMonitored,
                enabled = canEdit,
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir agenda de $displayName",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Surface(
        modifier = Modifier.size(10.dp),
        shape = CircleShape,
        color = color,
        content = {},
    )
}

@Composable
private fun UsageCard(
    state: BlockerUiState,
    onReset: () -> Unit,
) {
    SectionCard {
        state.monitoredSchedules.forEachIndexed { index, schedule ->
            val day = schedule.day(state.today)
            if (day.shorts != ShortsPolicy.COTA) return@forEachIndexed

            val used = state.usedMinutes(schedule.packageName)
            val quota = day.shortsQuotaMinutes.coerceAtLeast(1)
            val fraction = (used.toFloat() / quota).coerceIn(0f, 1f)
            val exhausted = used >= day.shortsQuotaMinutes

            if (index > 0) Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = PackageConstants.displayNameOf(schedule.packageName),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatDuration(used)} de ${formatDuration(day.shortsQuotaMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exhausted) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (exhausted) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onReset) {
            Text("Reiniciar contagem de hoje")
        }
    }
}

@Composable
private fun ServiceSetupCard(onOpenSettings: () -> Unit) {
    SectionCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
        CardHeader(
            title = "Ative o serviço de acessibilidade",
            description = "Sem ele o app não consegue detectar nem bloquear nada.",
            icon = Icons.Default.Warning,
            iconTint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "1. Abra as configurações de acessibilidade\n" +
                "2. Encontre este app na lista\n" +
                "3. Ative a permissão e volte",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir configurações")
        }
    }
}
