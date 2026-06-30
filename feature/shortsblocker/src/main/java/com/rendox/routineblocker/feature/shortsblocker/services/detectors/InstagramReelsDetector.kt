package com.rendox.routineblocker.feature.shortsblocker.services.detectors

import android.content.res.Resources
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber

class InstagramReelsDetector : ShortFormContentDetector {
    override fun getPackageName(): String = "com.instagram.android"

    override fun isShortFormContent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo,
        resources: Resources,
    ): Boolean {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(rootNode)
        var nodesScanned = 0
        var feedTabCount = 0
        while (stack.isNotEmpty() && nodesScanned < 10) {
            val n = stack.removeFirst()
            nodesScanned++
            val id = n.viewIdResourceName
            if (id != null && "feed_tab" in id) {
                feedTabCount++
            }
            if (id != null && ("clips_tab" in id && n.isSelected)) {
                Timber.i("[Instagram] User is actively watching Reels in Reels tab")
                return true
            }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { stack.add(it) }
            }
        }
        if (feedTabCount == 0) {
            Timber.i("[Instagram] User is actively watching Media in Fullscreen")
            return true
        }
        return false
    }
}
