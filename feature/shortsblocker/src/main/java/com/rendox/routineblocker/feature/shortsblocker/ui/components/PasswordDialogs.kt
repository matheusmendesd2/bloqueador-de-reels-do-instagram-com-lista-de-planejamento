package com.rendox.routineblocker.feature.shortsblocker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

const val MIN_PASSWORD_LENGTH = 4

/** Dialogo usado para definir a senha pela primeira vez. */
@Composable
fun SetPasswordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val error = when {
        password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH ->
            "Use pelo menos $MIN_PASSWORD_LENGTH caracteres"
        confirmation.isNotEmpty() && password != confirmation -> "As senhas não conferem"
        else -> null
    }

    PasswordDialogScaffold(
        icon = Icons.Default.Lock,
        title = "Proteger com senha",
        description = "A senha será pedida para alterar qualquer regra de bloqueio.",
        error = error,
        confirmLabel = "Salvar",
        confirmEnabled = password.length >= MIN_PASSWORD_LENGTH && password == confirmation,
        onConfirm = { onConfirm(password) },
        onDismiss = onDismiss,
    ) {
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Senha",
            visible = visible,
            onToggleVisibility = { visible = !visible },
        )
        Spacer(modifier = Modifier.height(8.dp))
        PasswordField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = "Repita a senha",
            visible = visible,
            onToggleVisibility = { visible = !visible },
        )
    }
}

/** Dialogo para desligar a protecao, confirmando a senha salva. */
@Composable
fun DisableProtectionDialog(
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    PasswordDialogScaffold(
        icon = Icons.Default.Warning,
        title = "Desativar proteção",
        description = "Digite a senha para desligar a proteção. " +
            "Nenhuma regra será aplicada até você reativar.",
        error = error,
        confirmLabel = "Desativar",
        confirmEnabled = password.isNotEmpty(),
        onConfirm = { onConfirm(password) },
        onDismiss = onDismiss,
    ) {
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Senha",
            visible = visible,
            onToggleVisibility = { visible = !visible },
        )
    }
}

/** Dialogo de desbloqueio das configuracoes. */
@Composable
fun UnlockDialog(
    error: String?,
    unlockDurationMinutes: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    PasswordDialogScaffold(
        icon = Icons.Default.LockOpen,
        title = "Desbloquear configurações",
        description = "As configurações ficam liberadas por $unlockDurationMinutes minutos.",
        error = error,
        confirmLabel = "Desbloquear",
        confirmEnabled = password.isNotEmpty(),
        onConfirm = { onConfirm(password) },
        onDismiss = onDismiss,
    ) {
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Senha",
            visible = visible,
            onToggleVisibility = { visible = !visible },
        )
    }
}

/** Dialogo de troca de senha. */
@Composable
fun ChangePasswordDialog(
    error: String?,
    onConfirm: (current: String, new: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val tooShort = newPassword.isNotEmpty() && newPassword.length < MIN_PASSWORD_LENGTH

    PasswordDialogScaffold(
        icon = Icons.Default.Lock,
        title = "Alterar senha",
        description = "Confirme a senha atual antes de definir a nova.",
        error = error ?: if (tooShort) "Use pelo menos $MIN_PASSWORD_LENGTH caracteres" else null,
        confirmLabel = "Alterar",
        confirmEnabled = current.isNotEmpty() && newPassword.length >= MIN_PASSWORD_LENGTH,
        onConfirm = { onConfirm(current, newPassword) },
        onDismiss = onDismiss,
    ) {
        PasswordField(
            value = current,
            onValueChange = { current = it },
            label = "Senha atual",
            visible = visible,
            onToggleVisibility = { visible = !visible },
        )
        Spacer(modifier = Modifier.height(8.dp))
        PasswordField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "Nova senha",
            visible = visible,
            onToggleVisibility = { visible = !visible },
        )
    }
}

/** Dialogo de remocao da senha. */
@Composable
fun RemovePasswordDialog(
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    PasswordDialogScaffold(
        icon = Icons.Default.LockOpen,
        title = "Remover senha",
        description = "Sem senha, qualquer pessoa poderá alterar as regras de bloqueio.",
        error = error,
        confirmLabel = "Remover",
        confirmEnabled = password.isNotEmpty(),
        onConfirm = { onConfirm(password) },
        onDismiss = onDismiss,
    ) {
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Senha atual",
            visible = visible,
            onToggleVisibility = { visible = !visible },
        )
    }
}

@Composable
private fun PasswordDialogScaffold(
    icon: ImageVector,
    title: String,
    description: String,
    error: String?,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    fields: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                fields()
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = if (visible) "Ocultar senha" else "Mostrar senha",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
