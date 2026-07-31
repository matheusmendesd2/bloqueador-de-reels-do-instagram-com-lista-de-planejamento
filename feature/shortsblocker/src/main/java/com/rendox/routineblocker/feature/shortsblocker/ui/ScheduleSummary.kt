package com.rendox.routineblocker.feature.shortsblocker.ui

import com.rendox.routineblocker.feature.shortsblocker.models.AppAccess
import com.rendox.routineblocker.feature.shortsblocker.models.AppSchedule
import com.rendox.routineblocker.feature.shortsblocker.models.BlockReason
import com.rendox.routineblocker.feature.shortsblocker.models.DaySchedule
import com.rendox.routineblocker.feature.shortsblocker.models.ShortsPolicy
import com.rendox.routineblocker.feature.shortsblocker.models.formatDuration
import com.rendox.routineblocker.feature.shortsblocker.models.formatMinuteOfDay

/** Como pintar um status na tela. */
enum class StatusTone { OK, WARN, BLOCKED, OFF }

/** Resumo do que esta valendo agora para um app. */
data class TodayStatus(
    val headline: String,
    val detail: String,
    val tone: StatusTone,
)

/** Descreve a regra de abertura de um dia em uma frase. */
fun accessSummary(day: DaySchedule): String = when (day.access) {
    AppAccess.LIBERADO -> "Abertura liberada o dia todo"
    AppAccess.BLOQUEADO -> "App bloqueado o dia todo"
    AppAccess.JANELAS -> {
        val windows = day.sortedWindows
        val listed = windows.joinToString(separator = " e ") { it.format() }
        when {
            windows.isEmpty() -> "Sem horários liberados"
            windows.size <= 2 -> "Abre $listed"
            else -> "Abre em ${windows.size} faixas de horário"
        }
    }
}

/** Descreve a regra de conteudo curto de um dia em uma frase. */
fun shortsSummary(day: DaySchedule, shortFormName: String): String = when (day.shorts) {
    ShortsPolicy.BLOQUEADO -> "$shortFormName bloqueados"
    ShortsPolicy.LIBERADO -> "$shortFormName liberados"
    ShortsPolicy.COTA -> "$shortFormName até ${formatDuration(day.shortsQuotaMinutes)}"
}

/**
 * Monta o status que aparece no cartao do app na tela inicial, considerando o dia,
 * a hora e o consumo ja registrado.
 */
fun todayStatus(
    schedule: AppSchedule,
    shortFormName: String,
    dayOfWeek: Int,
    minuteOfDay: Int,
    usedMinutes: Int,
    protectionActive: Boolean,
): TodayStatus {
    if (!schedule.monitored) {
        return TodayStatus(
            headline = "Não monitorado",
            detail = "Ative para aplicar a agenda deste app",
            tone = StatusTone.OFF,
        )
    }
    if (!protectionActive) {
        return TodayStatus(
            headline = "Proteção pausada",
            detail = "As regras voltam quando a proteção for retomada",
            tone = StatusTone.OFF,
        )
    }

    val day = schedule.day(dayOfWeek)
    val accessReason = schedule.appAccessReason(dayOfWeek, minuteOfDay)

    if (accessReason == BlockReason.APP_BLOQUEADO_HOJE) {
        return TodayStatus(
            headline = "Bloqueado hoje",
            detail = "O app não abre em nenhum horário de hoje",
            tone = StatusTone.BLOCKED,
        )
    }
    if (accessReason == BlockReason.FORA_DA_JANELA) {
        val next = schedule.nextWindowToday(dayOfWeek, minuteOfDay)
        return TodayStatus(
            headline = "Fora do horário",
            detail = if (next != null) {
                "Libera às ${formatMinuteOfDay(next.startMinute)}"
            } else {
                "Sem mais horários liberados hoje"
            },
            tone = StatusTone.BLOCKED,
        )
    }

    val windowEnd = schedule.currentWindowEnd(dayOfWeek, minuteOfDay)
    val accessDetail = if (day.access == AppAccess.JANELAS && windowEnd != null) {
        "Liberado até ${formatMinuteOfDay(windowEnd)}"
    } else {
        "Abertura liberada"
    }

    return when (day.shorts) {
        ShortsPolicy.BLOQUEADO -> TodayStatus(
            headline = accessDetail,
            detail = "$shortFormName bloqueados hoje",
            tone = StatusTone.OK,
        )
        ShortsPolicy.LIBERADO -> TodayStatus(
            headline = accessDetail,
            detail = "$shortFormName sem limite hoje",
            tone = StatusTone.WARN,
        )
        ShortsPolicy.COTA -> {
            val remaining = (day.shortsQuotaMinutes - usedMinutes).coerceAtLeast(0)
            TodayStatus(
                headline = accessDetail,
                detail = if (remaining > 0) {
                    "$shortFormName: restam ${formatDuration(remaining)} de " +
                        formatDuration(day.shortsQuotaMinutes)
                } else {
                    "$shortFormName: cota de hoje esgotada"
                },
                tone = if (remaining > 0) StatusTone.OK else StatusTone.BLOCKED,
            )
        }
    }
}
