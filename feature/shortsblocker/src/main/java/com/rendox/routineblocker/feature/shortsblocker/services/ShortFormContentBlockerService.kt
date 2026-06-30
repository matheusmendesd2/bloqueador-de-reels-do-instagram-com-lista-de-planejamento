package com.rendox.routineblocker.feature.shortsblocker.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.rendox.routineblocker.feature.shortsblocker.services.detectors.InstagramReelsDetector
import com.rendox.routineblocker.feature.shortsblocker.services.detectors.ShortFormContentDetector
import com.rendox.routineblocker.feature.shortsblocker.services.detectors.YouTubeShortsDetector
import com.rendox.routineblocker.feature.shortsblocker.utils.UserPreferencesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("AccessibilityPolicy")
class ShortFormContentBlockerService : AccessibilityService() {
    private val lastActionTimestamps = ConcurrentHashMap<String, Long>()
    private val actionCooldownMillis = 1500L
    private val userPreferencesProvider by lazy { UserPreferencesProvider(applicationContext) }
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)
    private val detectors: Map<String, ShortFormContentDetector> by lazy {
        mapOf(
            "com.google.android.youtube" to YouTubeShortsDetector(),
            "com.instagram.android" to InstagramReelsDetector(),
        )
    }

    private var allowedDays: Set<Int> = emptySet()
    private var dailyQuotaMinutes: Int = 0
    private var usedMinutesToday: Int = 0
    private var reelsActive = false
    private var lastDetectionTime: Long = 0L
    private var appBlockEnabled = false
    private var appBlockedDays: Set<Int> = emptySet()
    private var blockerEnabled = true

    override fun onServiceConnected() {
        userPreferencesProvider.getTrackedPackages()
            .onEach { packages ->
                val info = AccessibilityServiceInfo().apply {
                    eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                    flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                    packageNames = packages.toTypedArray()
                    notificationTimeout = 100
                }
                serviceInfo = info
            }
            .catch { error ->
                Timber.e(error, "Error fetching tracked packages")
            }
            .launchIn(coroutineScope)

        userPreferencesProvider.getAllowedDays()
            .onEach { allowedDays = it }
            .launchIn(coroutineScope)

        userPreferencesProvider.getDailyQuotaMinutes()
            .onEach { dailyQuotaMinutes = it }
            .launchIn(coroutineScope)

        userPreferencesProvider.getUsedMinutesToday()
            .onEach { usedMinutesToday = it }
            .launchIn(coroutineScope)

        userPreferencesProvider.getAppBlockEnabled()
            .onEach { appBlockEnabled = it }
            .launchIn(coroutineScope)

        userPreferencesProvider.getAppBlockedDays()
            .onEach { appBlockedDays = it }
            .launchIn(coroutineScope)

        userPreferencesProvider.getBlockerEnabled()
            .onEach { blockerEnabled = it }
            .launchIn(coroutineScope)

        startUsageHeartbeat()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            !(event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0)
        ) return

        val packageName = event.packageName?.toString()

        if (packageName != null && appBlockEnabled) {
            val today = LocalDate.now().dayOfWeek.value
            val appBlockKey = "${packageName}_app_block"
            val trackedForBlock = detectors.containsKey(packageName)
            if (trackedForBlock && today in appBlockedDays && shouldPerformAction(appBlockKey)) {
                Timber.i("[$packageName] Blocking app launch (blocked day $today)")
                performGlobalAction(GLOBAL_ACTION_BACK)
                return
            }
        }

        val detector = packageName?.let { detectors[it] }

        if (detector == null) {
            reelsActive = false
            return
        }

        if (!blockerEnabled) {
            reelsActive = false
            return
        }

        val windows = windows
        for (win in windows) {
            if (!win.isFocused || !win.isActive) continue
            val root = win.root ?: continue

            if (detector.isShortFormContent(event, root, resources)) {
                val key = "${packageName}_content_detected"
                if (!shouldPerformAction(key)) return

                if (shouldBlock()) {
                    Timber.i("[$packageName] Blocking short-form content!")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    reelsActive = false
                } else {
                    reelsActive = true
                    lastDetectionTime = SystemClock.uptimeMillis()
                    Timber.i("[$packageName] Allowing (quota remaining)")
                }
                break
            }
        }
    }

    override fun onInterrupt() {
        Timber.w("ShortFormContentBlockerService interrupted")
    }

    private fun startUsageHeartbeat() {
        coroutineScope.launch {
            userPreferencesProvider.resetUsedMinutesIfNewDay()
            while (isActive) {
                delay(60_000L)
                if (reelsActive) {
                    val elapsedSinceLastDetection = SystemClock.uptimeMillis() - lastDetectionTime
                    if (elapsedSinceLastDetection >= 120_000L) {
                        Timber.d("No Reels detection for 2min, ending session")
                        reelsActive = false
                    } else {
                        Timber.d("Adding 1 min to daily usage")
                        userPreferencesProvider.incrementUsedMinutes(1)
                    }
                }
            }
        }
    }

    private fun shouldBlock(): Boolean {
        val today = LocalDate.now().dayOfWeek.value
        if (today !in allowedDays) return true
        if (dailyQuotaMinutes <= 0) return true
        return usedMinutesToday >= dailyQuotaMinutes
    }

    private fun shouldPerformAction(key: String): Boolean {
        val now = SystemClock.uptimeMillis()
        val last = lastActionTimestamps[key] ?: 0L
        if (now - last < actionCooldownMillis) return false
        lastActionTimestamps[key] = now
        return true
    }
}
