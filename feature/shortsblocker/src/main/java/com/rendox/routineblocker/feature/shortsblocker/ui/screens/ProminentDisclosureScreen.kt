package com.rendox.routineblocker.feature.shortsblocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rendox.routineblocker.feature.shortsblocker.ui.components.IconBadge
import com.rendox.routineblocker.feature.shortsblocker.ui.components.SectionCard

@Composable
fun ProminentDisclosureScreen(
    onAgree: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(icon = Icons.Default.Accessibility, size = 72.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Permissão de acessibilidade",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "É essa permissão que permite ao app perceber quando um Reels ou Short " +
                "aparece na tela.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        DisclosureItem(
            icon = Icons.Default.Accessibility,
            title = "O que o app lê",
            description = "Apenas a estrutura da tela dos apps que você escolheu monitorar, para " +
                "identificar telas de vídeo curto.",
        )
        Spacer(modifier = Modifier.height(10.dp))
        DisclosureItem(
            icon = Icons.Default.Block,
            title = "O que o app faz com isso",
            description = "Aplica as regras que você configurou: sai da tela de Reels ou impede " +
                "a abertura do app fora do horário.",
        )
        Spacer(modifier = Modifier.height(10.dp))
        DisclosureItem(
            icon = Icons.Default.PhonelinkLock,
            title = "Privacidade",
            description = "Nada é coletado, guardado ou enviado para fora. Todo o processamento " +
                "acontece no seu aparelho.",
        )

        Spacer(modifier = Modifier.height(28.dp))
        Button(onClick = onAgree, modifier = Modifier.fillMaxWidth()) {
            Text("Entendi, ativar permissão")
        }
        TextButton(onClick = onCancel) {
            Text("Agora não")
        }
    }
}

@Composable
private fun DisclosureItem(
    icon: ImageVector,
    title: String,
    description: String,
) {
    SectionCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
