package com.rendox.routineblocker.feature.shortsblocker.ui

import com.rendox.routineblocker.feature.shortsblocker.models.BlockAction
import com.rendox.routineblocker.feature.shortsblocker.models.DaySchedule
import com.rendox.routineblocker.feature.shortsblocker.models.TimeWindow

/**
 * Todas as acoes que as telas do bloqueador podem disparar.
 *
 * Agrupadas em um unico objeto para as telas nao precisarem de vinte parametros de lambda.
 */
data class BlockerActions(
    val setProtectionEnabled: (Boolean) -> Unit = {},
    val pauseFor: (Int) -> Unit = {},
    val cancelPause: () -> Unit = {},
    val setBlockAction: (BlockAction) -> Unit = {},
    val setShowBlockWarning: (Boolean) -> Unit = {},
    val setBlockMessage: (String) -> Unit = {},
    val setStrictMode: (Boolean) -> Unit = {},
    val setUnlockDurationMinutes: (Int) -> Unit = {},
    val resetTodayUsage: () -> Unit = {},

    val setMonitored: (packageName: String, monitored: Boolean) -> Unit = { _, _ -> },
    val updateDay: (packageName: String, dayOfWeek: Int, day: DaySchedule) -> Unit = { _, _, _ -> },
    val addWindow: (packageName: String, dayOfWeek: Int, window: TimeWindow) -> Unit =
        { _, _, _ -> },
    val removeWindow: (packageName: String, dayOfWeek: Int, window: TimeWindow) -> Unit =
        { _, _, _ -> },
    val copyDayTo: (packageName: String, from: Int, targets: List<Int>) -> Unit = { _, _, _ -> },
    val resetSchedule: (packageName: String) -> Unit = {},

    val setPassword: (String) -> Unit = {},
    val changePassword: (current: String, new: String) -> Unit = { _, _ -> },
    val removePassword: (String) -> Unit = {},
    val unlock: (String) -> Unit = {},
    val lock: () -> Unit = {},
    val clearPasswordError: () -> Unit = {},
    val consumePasswordChanged: () -> Unit = {},

    val openAccessibilitySettings: () -> Unit = {},
    val activateDeviceAdmin: () -> Unit = {},
    val deactivateDeviceAdmin: () -> Unit = {},
)
