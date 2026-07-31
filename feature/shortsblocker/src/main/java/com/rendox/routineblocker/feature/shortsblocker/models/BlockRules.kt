package com.rendox.routineblocker.feature.shortsblocker.models

/**
 * Regras de bloqueio.
 *
 * Cada app monitorado tem uma agenda semanal. Para cada dia da semana o usuario
 * define duas coisas independentes:
 *  - [AppAccess]: se o app pode ser aberto (sempre, nunca, ou apenas em faixas de horario)
 *  - [ShortsPolicy]: o que acontece com Reels/Shorts dentro do app naquele dia
 */

/** Como o app pode ser aberto em um determinado dia. */
enum class AppAccess {
    /** O app abre a qualquer hora. */
    LIBERADO,

    /** O app so abre dentro das faixas de horario configuradas. */
    JANELAS,

    /** O app nao abre em nenhum momento do dia. */
    BLOQUEADO,
}

/** O que acontece com o conteudo curto (Reels/Shorts) em um determinado dia. */
enum class ShortsPolicy {
    /** Reels/Shorts sempre bloqueados. */
    BLOQUEADO,

    /** Reels/Shorts liberados ate atingir a cota de minutos do dia. */
    COTA,

    /** Reels/Shorts liberados sem limite. */
    LIBERADO,
}

/** O que o app faz quando decide bloquear. */
enum class BlockAction {
    /** Volta uma tela (sai do Reels, continua no app). */
    VOLTAR,

    /** Vai direto para a tela inicial do celular. */
    TELA_INICIAL,
}

/** Motivo pelo qual um acesso foi negado. [NENHUM] significa que o acesso e permitido. */
enum class BlockReason {
    NENHUM,
    APP_BLOQUEADO_HOJE,
    FORA_DA_JANELA,
    SHORTS_BLOQUEADO,
    COTA_ESGOTADA,
}

/** Faixa de horario em minutos desde a meia-noite. [end] e exclusivo. */
data class TimeWindow(
    val startMinute: Int,
    val endMinute: Int,
) {
    val isValid: Boolean
        get() = startMinute in 0..MINUTES_IN_DAY && endMinute in 0..MINUTES_IN_DAY &&
            endMinute > startMinute

    fun contains(minuteOfDay: Int): Boolean =
        minuteOfDay >= startMinute && minuteOfDay < endMinute

    val durationMinutes: Int
        get() = endMinute - startMinute

    fun overlaps(other: TimeWindow): Boolean =
        startMinute < other.endMinute && other.startMinute < endMinute

    fun format(): String = "${formatMinuteOfDay(startMinute)} - ${formatMinuteOfDay(endMinute)}"

    companion object {
        const val MINUTES_IN_DAY = 24 * 60
    }
}

fun formatMinuteOfDay(minuteOfDay: Int): String {
    val clamped = minuteOfDay.coerceIn(0, TimeWindow.MINUTES_IN_DAY)
    if (clamped == TimeWindow.MINUTES_IN_DAY) return "24:00"
    return "%02d:%02d".format(clamped / 60, clamped % 60)
}

fun formatDuration(minutes: Int): String = when {
    minutes <= 0 -> "0 min"
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h${"%02d".format(minutes % 60)}"
}

/** Configuracao de um unico dia da semana. */
data class DaySchedule(
    val access: AppAccess = AppAccess.LIBERADO,
    val windows: List<TimeWindow> = emptyList(),
    val shorts: ShortsPolicy = ShortsPolicy.BLOQUEADO,
    val shortsQuotaMinutes: Int = DEFAULT_QUOTA_MINUTES,
) {
    /** Janelas validas, sem sobreposicao e em ordem cronologica. */
    val sortedWindows: List<TimeWindow>
        get() = windows.filter { it.isValid }.sortedBy { it.startMinute }

    fun withWindow(window: TimeWindow): DaySchedule {
        if (!window.isValid) return this
        val merged = (sortedWindows + window).sortedBy { it.startMinute }
            .fold(mutableListOf<TimeWindow>()) { acc, next ->
                val last = acc.lastOrNull()
                if (last != null && next.startMinute <= last.endMinute) {
                    acc[acc.lastIndex] = last.copy(
                        endMinute = maxOf(last.endMinute, next.endMinute),
                    )
                } else {
                    acc.add(next)
                }
                acc
            }
        return copy(windows = merged)
    }

    fun withoutWindow(window: TimeWindow): DaySchedule =
        copy(windows = windows.filterNot { it == window })

    companion object {
        const val DEFAULT_QUOTA_MINUTES = 15
        const val MAX_QUOTA_MINUTES = 240

        /** Dia liberado por padrao, mas sem Reels. */
        val Default = DaySchedule()
    }
}

/** Agenda semanal de um app. As chaves de [days] sao 1 (segunda) a 7 (domingo). */
data class AppSchedule(
    val packageName: String,
    val monitored: Boolean = false,
    val days: Map<Int, DaySchedule> = emptyMap(),
) {
    fun day(dayOfWeek: Int): DaySchedule = days[dayOfWeek] ?: DaySchedule.Default

    fun withDay(dayOfWeek: Int, schedule: DaySchedule): AppSchedule =
        copy(days = days + (dayOfWeek to schedule))

    /** Aplica a configuracao de [from] em todos os dias de [targets]. */
    fun copyDay(from: Int, targets: Collection<Int>): AppSchedule {
        val source = day(from)
        return copy(days = days + targets.filter { it != from }.associateWith { source })
    }

    /**
     * Verifica se o app pode ser aberto agora. Nao considera a chave geral nem a pausa
     * temporaria - isso e responsabilidade de quem chama.
     */
    fun appAccessReason(dayOfWeek: Int, minuteOfDay: Int): BlockReason {
        if (!monitored) return BlockReason.NENHUM
        val day = day(dayOfWeek)
        return when (day.access) {
            AppAccess.LIBERADO -> BlockReason.NENHUM
            AppAccess.BLOQUEADO -> BlockReason.APP_BLOQUEADO_HOJE
            AppAccess.JANELAS -> {
                val inside = day.sortedWindows.any { it.contains(minuteOfDay) }
                if (inside) BlockReason.NENHUM else BlockReason.FORA_DA_JANELA
            }
        }
    }

    /** Verifica se o conteudo curto pode ser exibido agora, dado o consumo do dia. */
    fun shortsReason(dayOfWeek: Int, usedMinutesToday: Int): BlockReason {
        if (!monitored) return BlockReason.NENHUM
        val day = day(dayOfWeek)
        return when (day.shorts) {
            ShortsPolicy.LIBERADO -> BlockReason.NENHUM
            ShortsPolicy.BLOQUEADO -> BlockReason.SHORTS_BLOQUEADO
            ShortsPolicy.COTA ->
                if (usedMinutesToday < day.shortsQuotaMinutes) {
                    BlockReason.NENHUM
                } else {
                    BlockReason.COTA_ESGOTADA
                }
        }
    }

    /** Fim da janela em que [minuteOfDay] se encontra, ou null se estiver fora de qualquer janela. */
    fun currentWindowEnd(dayOfWeek: Int, minuteOfDay: Int): Int? =
        day(dayOfWeek).sortedWindows.firstOrNull { it.contains(minuteOfDay) }?.endMinute

    /** Proxima janela que ainda vai comecar hoje, ou null se nao houver. */
    fun nextWindowToday(dayOfWeek: Int, minuteOfDay: Int): TimeWindow? =
        day(dayOfWeek).sortedWindows.firstOrNull { it.startMinute > minuteOfDay }
}

/** Configuracoes globais do bloqueador. */
data class BlockerSettings(
    val protectionEnabled: Boolean = true,
    val blockAction: BlockAction = BlockAction.VOLTAR,
    val showBlockWarning: Boolean = true,
    val blockMessage: String = DEFAULT_BLOCK_MESSAGE,
    val strictMode: Boolean = false,
    val unlockDurationMinutes: Int = 5,
    val pausedUntilEpochMillis: Long = 0L,
) {
    fun isPaused(nowEpochMillis: Long): Boolean = pausedUntilEpochMillis > nowEpochMillis

    fun pauseRemainingMinutes(nowEpochMillis: Long): Int {
        val remaining = pausedUntilEpochMillis - nowEpochMillis
        if (remaining <= 0) return 0
        return ((remaining + 59_999L) / 60_000L).toInt()
    }

    /** A protecao esta valendo neste instante? */
    fun isActive(nowEpochMillis: Long): Boolean = protectionEnabled && !isPaused(nowEpochMillis)

    companion object {
        const val DEFAULT_BLOCK_MESSAGE = "Bloqueado pelo seu plano de foco."
        val UNLOCK_DURATION_OPTIONS = listOf(1, 5, 15, 30)
        val PAUSE_DURATION_OPTIONS = listOf(15, 30, 60, 120)
    }
}

val DAY_LABELS_SHORT = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
val DAY_LABELS_LONG = listOf(
    "Segunda-feira",
    "Terça-feira",
    "Quarta-feira",
    "Quinta-feira",
    "Sexta-feira",
    "Sábado",
    "Domingo",
)

val WEEKDAYS = listOf(1, 2, 3, 4, 5)
val WEEKEND = listOf(6, 7)
val ALL_DAYS = listOf(1, 2, 3, 4, 5, 6, 7)

fun dayLabelShort(dayOfWeek: Int): String = DAY_LABELS_SHORT[(dayOfWeek - 1).coerceIn(0, 6)]
fun dayLabelLong(dayOfWeek: Int): String = DAY_LABELS_LONG[(dayOfWeek - 1).coerceIn(0, 6)]
