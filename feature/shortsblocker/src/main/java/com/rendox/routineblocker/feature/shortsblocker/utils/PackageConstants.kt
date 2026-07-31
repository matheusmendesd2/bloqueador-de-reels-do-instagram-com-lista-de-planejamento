package com.rendox.routineblocker.feature.shortsblocker.utils

import com.rendox.routineblocker.feature.shortsblocker.models.TrackedPackage

object PackageConstants {
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    const val INSTAGRAM_PACKAGE = "com.instagram.android"

    val AVAILABLE_PACKAGES = listOf(
        TrackedPackage(
            packageName = INSTAGRAM_PACKAGE,
            displayName = "Instagram",
            shortFormName = "Reels",
        ),
        TrackedPackage(
            packageName = YOUTUBE_PACKAGE,
            displayName = "YouTube",
            shortFormName = "Shorts",
        ),
    )

    val ALL_PACKAGE_NAMES = AVAILABLE_PACKAGES.map { it.packageName }

    fun displayNameOf(packageName: String): String =
        AVAILABLE_PACKAGES.firstOrNull { it.packageName == packageName }?.displayName
            ?: packageName

    fun shortFormNameOf(packageName: String): String =
        AVAILABLE_PACKAGES.firstOrNull { it.packageName == packageName }?.shortFormName
            ?: "vídeos curtos"
}
