package com.rendox.routineblocker.feature.shortsblocker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rendox.routineblocker.feature.shortsblocker.models.BlockAction
import com.rendox.routineblocker.feature.shortsblocker.models.BlockerSettings
import com.rendox.routineblocker.feature.shortsblocker.ui.BlockerActions
import com.rendox.routineblocker.feature.shortsblocker.ui.components.CardHeader
import com.rendox.routineblocker.feature.shortsblocker.ui.components.HintBanner
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SectionCard
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SectionLabel
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SegmentedChoice
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SwitchRow
import com.rendox.routineblocker.feature.shortsblocker.ui.viewmodels.BlockerUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockerSettingsScreen(
    state: BlockerUiState,
    actions: BlockerActions,
    onBack: () -> Unit,
    onSetPasswordRequest: () -> Unit,
    onChangePasswordRequest: () -> Unit,
    onRemovePasswordRequest: () -> Unit,
    onUnlockRequest: () -> Unit,
) {
    val canEdit = state.canEdit

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!canEdit) {
                HintBanner(
                    text = "Desbloqueie com a senha para alterar os ajustes.",
                    icon = Icons.Default.Lock,
                )
                Button(onClick = onUnlockRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("Desbloquear")
                }
            }

            SectionLabel(text = "Como bloquear")
            SectionCard {
                CardHeader(
                    title = "Ação ao bloquear",
                    description = "O que acontece quando uma regra é violada.",
                    icon = Icons.Default.Bolt,
                )
                Spacer(modifier = Modifier.height(14.dp))
                SegmentedChoice(
                    options = listOf(BlockAction.VOLTAR, BlockAction.TELA_INICIAL),
                    selected = state.settings.blockAction,
                    onSelect = actions.setBlockAction,
                    label = { action ->
                        when (action) {
                            BlockAction.VOLTAR -> "Voltar"
                            BlockAction.TELA_INICIAL -> "Tela inicial"
                        }
                    },
                    enabled = canEdit,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (state.settings.blockAction) {
                        BlockAction.VOLTAR ->
                            "Sai do Reels/Shorts e mantém você dentro do app."
                        BlockAction.TELA_INICIAL ->
                            "Fecha o app e leva direto para a tela inicial do celular."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard {
                SwitchRow(
                    title = "Mostrar aviso ao bloquear",
                    description = "Exibe uma mensagem rápida explicando o motivo.",
                    checked = state.settings.showBlockWarning,
                    onCheckedChange = actions.setShowBlockWarning,
                    enabled = canEdit,
                    icon = Icons.Default.Campaign,
                )
                AnimatedVisibility(visible = state.settings.showBlockWarning) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        BlockMessageField(
                            message = state.settings.blockMessage,
                            enabled = canEdit,
                            onSave = actions.setBlockMessage,
                        )
                    }
                }
            }

            SectionLabel(text = "Segurança")
            SectionCard {
                CardHeader(
                    title = "Senha de proteção",
                    description = if (state.hasPassword) {
                        "As regras só mudam com a senha."
                    } else {
                        "Sem senha, qualquer um pode desligar o bloqueio."
                    },
                    icon = Icons.Default.Lock,
                    iconTint = if (state.hasPassword) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (!state.hasPassword) {
                    Button(onClick = onSetPasswordRequest, modifier = Modifier.fillMaxWidth()) {
                        Text("Definir senha")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onChangePasswordRequest,
                            enabled = canEdit,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Alterar")
                        }
                        OutlinedButton(
                            onClick = onRemovePasswordRequest,
                            enabled = canEdit,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Remover")
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Tempo desbloqueado",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Depois desse tempo sem uso, as configurações travam de novo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SegmentedChoice(
                        options = BlockerSettings.UNLOCK_DURATION_OPTIONS,
                        selected = state.settings.unlockDurationMinutes,
                        onSelect = actions.setUnlockDurationMinutes,
                        label = { "$it min" },
                        enabled = canEdit,
                    )
                }
            }

            SectionCard {
                SwitchRow(
                    title = "Modo rígido",
                    description = "Impede desligar ou pausar a proteção enquanto um bloqueio de " +
                        "horário está valendo.",
                    checked = state.settings.strictMode,
                    onCheckedChange = actions.setStrictMode,
                    enabled = canEdit &&
                        (!state.settings.strictMode || state.canRelaxProtection),
                    icon = Icons.Default.VerifiedUser,
                )
            }

            SectionCard {
                CardHeader(
                    title = "Administrador do dispositivo",
                    description = if (state.isDeviceAdminActive) {
                        "Ativo. A desinstalação exige desativar o administrador antes."
                    } else {
                        "Inativo. O app pode ser desinstalado a qualquer momento."
                    },
                    icon = Icons.Default.AdminPanelSettings,
                    iconTint = if (state.isDeviceAdminActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (state.isDeviceAdminActive) {
                    OutlinedButton(
                        onClick = actions.deactivateDeviceAdmin,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Desativar administrador")
                    }
                } else {
                    FilledTonalButton(
                        onClick = actions.activateDeviceAdmin,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ativar administrador")
                    }
                }
            }

            SectionLabel(text = "Serviço")
            SectionCard {
                CardHeader(
                    title = "Serviço de acessibilidade",
                    description = if (state.isServiceEnabled) {
                        "Ativo e monitorando os apps selecionados."
                    } else {
                        "Desativado. Nenhum bloqueio está sendo aplicado."
                    },
                    icon = Icons.Default.VerifiedUser,
                    iconTint = if (state.isServiceEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = actions.openAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.isServiceEnabled) {
                            "Abrir configurações do sistema"
                        } else {
                            "Ativar serviço"
                        },
                    )
                }
            }

            Text(
                text = "Todo o processamento acontece no seu aparelho. Nada é enviado para " +
                    "servidores.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun BlockMessageField(
    message: String,
    enabled: Boolean,
    onSave: (String) -> Unit,
) {
    var draft by remember(message) { mutableStateOf(message) }

    // Salva sozinho pouco depois que o usuario para de digitar.
    LaunchedEffect(draft) {
        if (draft != message && draft.isNotBlank()) {
            delay(600)
            onSave(draft)
        }
    }

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it.take(80) },
        label = { Text("Mensagem do aviso") },
        enabled = enabled,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        supportingText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Aparece junto com o motivo do bloqueio")
                Text("${draft.length}/80")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(4.dp))
    TextButton(
        onClick = { onSave(BlockerSettings.DEFAULT_BLOCK_MESSAGE) },
        enabled = enabled && message != BlockerSettings.DEFAULT_BLOCK_MESSAGE,
    ) {
        Text("Voltar à mensagem padrão")
    }
}
