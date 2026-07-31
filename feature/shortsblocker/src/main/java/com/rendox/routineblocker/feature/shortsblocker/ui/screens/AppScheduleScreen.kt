package com.rendox.routineblocker.feature.shortsblocker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rendox.routineblocker.feature.shortsblocker.models.ALL_DAYS
import com.rendox.routineblocker.feature.shortsblocker.models.AppAccess
import com.rendox.routineblocker.feature.shortsblocker.models.AppSchedule
import com.rendox.routineblocker.feature.shortsblocker.models.DaySchedule
import com.rendox.routineblocker.feature.shortsblocker.models.ShortsPolicy
import com.rendox.routineblocker.feature.shortsblocker.models.WEEKDAYS
import com.rendox.routineblocker.feature.shortsblocker.models.WEEKEND
import com.rendox.routineblocker.feature.shortsblocker.models.dayLabelLong
import com.rendox.routineblocker.feature.shortsblocker.models.dayLabelShort
import com.rendox.routineblocker.feature.shortsblocker.models.formatDuration
import com.rendox.routineblocker.feature.shortsblocker.ui.BlockerActions
import com.rendox.routineblocker.feature.shortsblocker.ui.accessSummary
import com.rendox.routineblocker.feature.shortsblocker.ui.components.CardHeader
import com.rendox.routineblocker.feature.shortsblocker.ui.components.HintBanner
import com.rendox.routineblocker.feature.shortsblocker.ui.components.LabeledSlider
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SectionCard
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SectionLabel
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SegmentedChoice
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SwitchRow
import com.rendox.routineblocker.feature.shortsblocker.ui.components.TimeWindowDialog
import com.rendox.routineblocker.feature.shortsblocker.ui.components.TimeWindowList
import com.rendox.routineblocker.feature.shortsblocker.ui.shortsSummary
import com.rendox.routineblocker.feature.shortsblocker.ui.viewmodels.BlockerUiState
import com.rendox.routineblocker.feature.shortsblocker.utils.PackageConstants
import kotlin.math.roundToInt

private const val QUOTA_STEP_MINUTES = 5
private const val MIN_QUOTA_MINUTES = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScheduleScreen(
    state: BlockerUiState,
    packageName: String,
    actions: BlockerActions,
    onBack: () -> Unit,
    onUnlockRequest: () -> Unit,
) {
    val schedule = state.scheduleFor(packageName)
    val displayName = PackageConstants.displayNameOf(packageName)
    val shortFormName = PackageConstants.shortFormNameOf(packageName)
    val canEdit = state.canEdit

    var selectedDay by remember { mutableIntStateOf(state.today) }
    var showWindowDialog by remember { mutableStateOf(false) }

    val day = schedule.day(selectedDay)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName,
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
                    text = "Desbloqueie com a senha para alterar a agenda.",
                    icon = Icons.Default.Lock,
                    modifier = Modifier.clickable(onClick = onUnlockRequest),
                )
            }

            SectionCard {
                SwitchRow(
                    title = "Monitorar $displayName",
                    description = "Quando desligado, nenhuma regra abaixo é aplicada.",
                    checked = schedule.monitored,
                    onCheckedChange = { actions.setMonitored(packageName, it) },
                    enabled = canEdit,
                )
            }

            SectionLabel(text = "Escolha o dia")
            DaySelector(
                selectedDay = selectedDay,
                onSelectDay = { selectedDay = it },
                today = state.today,
            )

            SectionCard {
                CardHeader(
                    title = dayLabelLong(selectedDay),
                    description = if (selectedDay == state.today) "Hoje" else null,
                )
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Abertura do app",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Controla se o $displayName pode ser aberto neste dia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedChoice(
                    options = listOf(AppAccess.LIBERADO, AppAccess.JANELAS, AppAccess.BLOQUEADO),
                    selected = day.access,
                    onSelect = { access ->
                        actions.updateDay(packageName, selectedDay, day.copy(access = access))
                    },
                    label = { access ->
                        when (access) {
                            AppAccess.LIBERADO -> "Liberado"
                            AppAccess.JANELAS -> "Horários"
                            AppAccess.BLOQUEADO -> "Bloqueado"
                        }
                    },
                    enabled = canEdit,
                )

                AnimatedVisibility(visible = day.access == AppAccess.JANELAS) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        TimeWindowList(
                            windows = day.sortedWindows,
                            onRemove = { window ->
                                actions.removeWindow(packageName, selectedDay, window)
                            },
                            enabled = canEdit,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showWindowDialog = true },
                            enabled = canEdit,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adicionar horário")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toque em uma faixa para removê-la.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = shortFormName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Vale quando o app está liberado. O resto do $displayName continua " +
                        "funcionando normalmente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedChoice(
                    options = listOf(
                        ShortsPolicy.BLOQUEADO,
                        ShortsPolicy.COTA,
                        ShortsPolicy.LIBERADO,
                    ),
                    selected = day.shorts,
                    onSelect = { policy ->
                        val quota = day.shortsQuotaMinutes.coerceAtLeast(MIN_QUOTA_MINUTES)
                        actions.updateDay(
                            packageName,
                            selectedDay,
                            day.copy(shorts = policy, shortsQuotaMinutes = quota),
                        )
                    },
                    label = { policy ->
                        when (policy) {
                            ShortsPolicy.BLOQUEADO -> "Bloqueado"
                            ShortsPolicy.COTA -> "Com cota"
                            ShortsPolicy.LIBERADO -> "Liberado"
                        }
                    },
                    enabled = canEdit,
                )

                AnimatedVisibility(visible = day.shorts == ShortsPolicy.COTA) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        LabeledSlider(
                            title = "Cota do dia",
                            valueLabel = formatDuration(day.shortsQuotaMinutes),
                            value = day.shortsQuotaMinutes.toFloat(),
                            onValueChange = { value ->
                                val rounded = (value / QUOTA_STEP_MINUTES).roundToInt() *
                                    QUOTA_STEP_MINUTES
                                actions.updateDay(
                                    packageName,
                                    selectedDay,
                                    day.copy(shortsQuotaMinutes = rounded),
                                )
                            },
                            valueRange = MIN_QUOTA_MINUTES.toFloat()..
                                DaySchedule.MAX_QUOTA_MINUTES.toFloat(),
                            steps = (DaySchedule.MAX_QUOTA_MINUTES - MIN_QUOTA_MINUTES) /
                                QUOTA_STEP_MINUTES - 1,
                            enabled = canEdit,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                CopyDayRow(
                    enabled = canEdit,
                    onCopy = { targets -> actions.copyDayTo(packageName, selectedDay, targets) },
                )
            }

            SectionLabel(text = "Semana inteira")
            WeekOverview(
                schedule = schedule,
                shortFormName = shortFormName,
                today = state.today,
                selectedDay = selectedDay,
                onSelectDay = { selectedDay = it },
            )

            TextButton(
                onClick = { actions.resetSchedule(packageName) },
                enabled = canEdit,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Restaurar agenda padrão")
            }
        }
    }

    if (showWindowDialog) {
        TimeWindowDialog(
            onConfirm = { window ->
                actions.addWindow(packageName, selectedDay, window)
                showWindowDialog = false
            },
            onDismiss = { showWindowDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySelector(
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    today: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ALL_DAYS.forEach { dayOfWeek ->
            FilterChip(
                selected = dayOfWeek == selectedDay,
                onClick = { onSelectDay(dayOfWeek) },
                label = {
                    Text(
                        text = dayLabelShort(dayOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (dayOfWeek == today) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyDayRow(
    enabled: Boolean,
    onCopy: (List<Int>) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Aplicar esta configuração em",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { onCopy(ALL_DAYS) },
                enabled = enabled,
                label = { Text("Todos") },
            )
            AssistChip(
                onClick = { onCopy(WEEKDAYS) },
                enabled = enabled,
                label = { Text("Seg a Sex") },
            )
            AssistChip(
                onClick = { onCopy(WEEKEND) },
                enabled = enabled,
                label = { Text("Fim de semana") },
            )
        }
    }
}

@Composable
private fun WeekOverview(
    schedule: AppSchedule,
    shortFormName: String,
    today: Int,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
) {
    SectionCard {
        ALL_DAYS.forEach { dayOfWeek ->
            val day = schedule.day(dayOfWeek)
            val isSelected = dayOfWeek == selectedDay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectDay(dayOfWeek) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dayLabelShort(dayOfWeek),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        dayOfWeek == today -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.width(40.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = accessSummary(day),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = shortsSummary(day, shortFormName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (dayOfWeek != ALL_DAYS.last()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}
