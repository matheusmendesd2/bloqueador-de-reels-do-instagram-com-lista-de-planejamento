package com.rendox.routineblocker.feature.shortsblocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ProminentDisclosureScreen(
    onAgree: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Accessibility Service",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Shorts Blocker needs accessibility access to detect and block short-form video content.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))

        DisclosureCard(
            icon = Icons.Default.Visibility,
            title = "What This App Uses",
            description = "This app uses AccessibilityService to detect when YouTube Shorts or Instagram Reels are being displayed."
        )
        Spacer(modifier = Modifier.height(12.dp))
        DisclosureCard(
            icon = Icons.Default.Info,
            title = "Why We Need This",
            description = "The service allows us to identify short-form content and help you stay focused by returning to the main feed."
        )
        Spacer(modifier = Modifier.height(12.dp))
        DisclosureCard(
            icon = Icons.Default.Lock,
            title = "Your Privacy",
            description = "No data is collected, stored, or transmitted. All processing happens locally on your device."
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onAgree, modifier = Modifier.fillMaxWidth()) {
            Text("I Understand and Agree")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
private fun DisclosureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
