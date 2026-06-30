package com.rendox.routineblocker.feature.shortsblocker.models

data class TrackedPackage(
    val packageName: String,
    val displayName: String,
    val description: String,
    val isEnabled: Boolean = true,
)
