package com.rendox.routineblocker.feature.shortsblocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rendox.routineblocker.feature.shortsblocker.models.TimeWindow
import com.rendox.routineblocker.feature.shortsblocker.models.formatDuration
import com.rendox.routineblocker.feature.shortsblocker.models.formatMinuteOfDay

/** Lista das faixas de horario liberadas, com botao de remover em cada uma. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimeWindowList(
    windows: List<TimeWindow>,
    onRemove: (TimeWindow) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (windows.isEmpty()) {
        Text(
            text = "Nenhum horário liberado ainda. O app fica bloqueado o dia inteiro até você " +
                "adicionar uma faixa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        windows.forEach { window ->
            InputChip(
                selected = false,
                enabled = enabled,
                onClick = { if (enabled) onRemove(window) },
                label = { Text("${window.format()}  ·  ${formatDuration(window.durationMinutes)}") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remover faixa ${window.format()}",
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
    }
}

/** Dialogo para montar uma faixa de horario (inicio e fim). */
@Composable
fun TimeWindowDialog(
    onConfirm: (TimeWindow) -> Unit,
    onDismiss: () -> Unit,
    initialStartMinute: Int = 18 * 60,
    initialEndMinute: Int = 20 * 60,
) {
    var startMinute by remember { mutableIntStateOf(initialStartMinute) }
    var endMinute by remember { mutableIntStateOf(initialEndMinute) }
    var editing by remember { mutableStateOf<TimeField?>(null) }

    val isValid = endMinute > startMinute

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
        title = { Text("Horário liberado") },
        text = {
            Column {
                Text(
                    text = "Neste intervalo o app pode ser aberto normalmente. Fora dele, fica " +
                        "bloqueado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TimeField(
                        label = "Início",
                        minuteOfDay = startMinute,
                        onClick = { editing = TimeField.START },
                        modifier = Modifier.weight(1f),
                    )
                    TimeField(
                        label = "Fim",
                        minuteOfDay = endMinute,
                        onClick = { editing = TimeField.END },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!isValid) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "O fim precisa ser depois do início. Para atravessar a meia-noite, " +
                            "crie uma faixa em cada dia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(TimeWindow(startMinute, endMinute)) },
                enabled = isValid,
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )

    editing?.let { field ->
        val current = if (field == TimeField.START) startMinute else endMinute
        ClockDialog(
            title = if (field == TimeField.START) "Horário de início" else "Horário de fim",
            initialMinuteOfDay = current,
            onConfirm = { minuteOfDay ->
                if (field == TimeField.START) startMinute = minuteOfDay else endMinute = minuteOfDay
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

private enum class TimeField { START, END }

@Composable
private fun TimeField(
    label: String,
    minuteOfDay: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatMinuteOfDay(minuteOfDay),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Relogio do Material 3 dentro de um dialogo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockDialog(
    title: String,
    initialMinuteOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = (initialMinuteOfDay / 60).coerceIn(0, 23),
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour * 60 + timePickerState.minute) },
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
