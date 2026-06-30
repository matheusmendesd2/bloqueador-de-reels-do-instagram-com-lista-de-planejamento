package com.rendox.routineblocker.feature.shortsblocker.services.detectors

import android.content.res.Resources
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

interface ShortFormContentDetector {
    fun isShortFormContent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo,
        resources: Resources,
    ): Boolean
    fun getPackageName(): String
}
