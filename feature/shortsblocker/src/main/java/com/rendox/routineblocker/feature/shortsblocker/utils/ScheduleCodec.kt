package com.rendox.routineblocker.feature.shortsblocker.utils

import com.rendox.routineblocker.feature.shortsblocker.models.AppAccess
import com.rendox.routineblocker.feature.shortsblocker.models.AppSchedule
import com.rendox.routineblocker.feature.shortsblocker.models.DaySchedule
import com.rendox.routineblocker.feature.shortsblocker.models.ShortsPolicy
import com.rendox.routineblocker.feature.shortsblocker.models.TimeWindow

/**
 * Serializa uma [AppSchedule] em uma unica string para o DataStore.
 *
 * Formato: `v1|<monitorado 0/1>|<dia>;<dia>;...`
 * Cada dia: `<1-7>,<acesso>,<janelas>,<politica de shorts>,<cota>`
 * Janelas: `inicio-fim+inicio-fim` (em minutos desde a meia-noite), vazio se nao houver.
 *
 * Exemplo: `v1|1|1,J,1080-1200+1290-1350,C,20;2,B,,B,0`
 *
 * Formato proprio em vez de JSON para nao adicionar dependencia de serializacao ao modulo.
 */
object ScheduleCodec {
    private const val VERSION = "v1"
    private const val FIELD_SEPARATOR = "|"
    private const val DAY_SEPARATOR = ";"
    private const val PART_SEPARATOR = ","
    private const val WINDOW_SEPARATOR = "+"
    private const val RANGE_SEPARATOR = "-"

    private fun encodeAccess(access: AppAccess): String = when (access) {
        AppAccess.LIBERADO -> "L"
        AppAccess.JANELAS -> "J"
        AppAccess.BLOQUEADO -> "B"
    }

    private fun decodeAccess(raw: String?): AppAccess = when (raw) {
        "J" -> AppAccess.JANELAS
        "B" -> AppAccess.BLOQUEADO
        else -> AppAccess.LIBERADO
    }

    private fun encodeShorts(policy: ShortsPolicy): String = when (policy) {
        ShortsPolicy.BLOQUEADO -> "B"
        ShortsPolicy.COTA -> "C"
        ShortsPolicy.LIBERADO -> "L"
    }

    private fun decodeShorts(raw: String?): ShortsPolicy = when (raw) {
        "C" -> ShortsPolicy.COTA
        "L" -> ShortsPolicy.LIBERADO
        else -> ShortsPolicy.BLOQUEADO
    }

    fun encode(schedule: AppSchedule): String {
        val days = schedule.days.entries
            .sortedBy { it.key }
            .joinToString(DAY_SEPARATOR) { (dayOfWeek, day) ->
                val windows = day.sortedWindows.joinToString(WINDOW_SEPARATOR) {
                    "${it.startMinute}$RANGE_SEPARATOR${it.endMinute}"
                }
                listOf(
                    dayOfWeek.toString(),
                    encodeAccess(day.access),
                    windows,
                    encodeShorts(day.shorts),
                    day.shortsQuotaMinutes.toString(),
                ).joinToString(PART_SEPARATOR)
            }
        val monitored = if (schedule.monitored) "1" else "0"
        return listOf(VERSION, monitored, days).joinToString(FIELD_SEPARATOR)
    }

    /** Decodifica de forma tolerante: qualquer campo invalido cai no padrao. */
    fun decode(packageName: String, raw: String?): AppSchedule {
        if (raw.isNullOrBlank()) return AppSchedule(packageName = packageName)
        val fields = raw.split(FIELD_SEPARATOR)
        if (fields.size < 3 || fields[0] != VERSION) {
            return AppSchedule(packageName = packageName)
        }
        val monitored = fields[1] == "1"
        val days = fields[2]
            .split(DAY_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull { decodeDay(it) }
            .toMap()
        return AppSchedule(packageName = packageName, monitored = monitored, days = days)
    }

    private fun decodeDay(raw: String): Pair<Int, DaySchedule>? {
        val parts = raw.split(PART_SEPARATOR)
        if (parts.size < 5) return null
        val dayOfWeek = parts[0].toIntOrNull()?.takeIf { it in 1..7 } ?: return null
        val windows = parts[2]
            .split(WINDOW_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull { decodeWindow(it) }
        val quota = parts[4].toIntOrNull()
            ?.coerceIn(0, DaySchedule.MAX_QUOTA_MINUTES)
            ?: DaySchedule.DEFAULT_QUOTA_MINUTES
        val day = DaySchedule(
            access = decodeAccess(parts[1]),
            windows = windows,
            shorts = decodeShorts(parts[3]),
            shortsQuotaMinutes = quota,
        )
        return dayOfWeek to day
    }

    private fun decodeWindow(raw: String): TimeWindow? {
        val bounds = raw.split(RANGE_SEPARATOR)
        if (bounds.size != 2) return null
        val start = bounds[0].toIntOrNull() ?: return null
        val end = bounds[1].toIntOrNull() ?: return null
        val window = TimeWindow(start, end)
        return window.takeIf { it.isValid }
    }
}
