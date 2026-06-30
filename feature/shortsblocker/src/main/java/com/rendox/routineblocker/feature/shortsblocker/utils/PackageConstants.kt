package com.rendox.routineblocker.feature.shortsblocker.utils

import com.rendox.routineblocker.feature.shortsblocker.models.TrackedPackage

object PackageConstants {
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    val AVAILABLE_PACKAGES = listOf(
        TrackedPackage(
            packageName = YOUTUBE_PACKAGE,
            displayName = "YouTube",
            description = "Block YouTube Shorts",
            isEnabled = false,
        ),
        TrackedPackage(
            packageName = INSTAGRAM_PACKAGE,
            displayName = "Instagram",
            description = "Block Instagram Reels",
            isEnabled = false,
        ),
    )
    val DEFAULT_ENABLED_PACKAGES = AVAILABLE_PACKAGES
        .filter { it.isEnabled }
        .map { it.packageName }
}
