package com.rendox.routineblocker.feature.shortsblocker.models

/** Um app que o bloqueador sabe monitorar. */
data class TrackedPackage(
    val packageName: String,
    val displayName: String,
    /** Como o conteudo curto se chama dentro deste app ("Reels", "Shorts"). */
    val shortFormName: String,
)
