package com.rendox.routineblocker.feature.shortsblocker

import com.google.common.truth.Truth.assertThat
import com.rendox.routineblocker.feature.shortsblocker.models.AppAccess
import com.rendox.routineblocker.feature.shortsblocker.models.AppSchedule
import com.rendox.routineblocker.feature.shortsblocker.models.BlockReason
import com.rendox.routineblocker.feature.shortsblocker.models.DaySchedule
import com.rendox.routineblocker.feature.shortsblocker.models.ShortsPolicy
import com.rendox.routineblocker.feature.shortsblocker.models.TimeWindow
import org.junit.jupiter.api.Test

private const val INSTAGRAM = "com.instagram.android"
private const val MONDAY = 1
private const val TUESDAY = 2

private fun minutes(hour: Int, minute: Int = 0) = hour * 60 + minute

class BlockRulesTest {

    @Test
    fun `dia liberado nunca bloqueia a abertura`() {
        val schedule = AppSchedule(
            packageName = INSTAGRAM,
            monitored = true,
            days = mapOf(MONDAY to DaySchedule(access = AppAccess.LIBERADO)),
        )

        assertThat(schedule.appAccessReason(MONDAY, minutes(3)))
            .isEqualTo(BlockReason.NENHUM)
        assertThat(schedule.appAccessReason(MONDAY, minutes(23, 59)))
            .isEqualTo(BlockReason.NENHUM)
    }

    @Test
    fun `dia bloqueado bloqueia a abertura a qualquer hora`() {
        val schedule = AppSchedule(
            packageName = INSTAGRAM,
            monitored = true,
            days = mapOf(TUESDAY to DaySchedule(access = AppAccess.BLOQUEADO)),
        )

        assertThat(schedule.appAccessReason(TUESDAY, minutes(12)))
            .isEqualTo(BlockReason.APP_BLOQUEADO_HOJE)
    }

    @Test
    fun `com janelas o app so abre dentro do intervalo`() {
        val schedule = AppSchedule(
            packageName = INSTAGRAM,
            monitored = true,
            days = mapOf(
                MONDAY to DaySchedule(
                    access = AppAccess.JANELAS,
                    windows = listOf(TimeWindow(minutes(18), minutes(20))),
                ),
            ),
        )

        assertThat(schedule.appAccessReason(MONDAY, minutes(17, 59)))
            .isEqualTo(BlockReason.FORA_DA_JANELA)
        assertThat(schedule.appAccessReason(MONDAY, minutes(18)))
            .isEqualTo(BlockReason.NENHUM)
        assertThat(schedule.appAccessReason(MONDAY, minutes(19, 59)))
            .isEqualTo(BlockReason.NENHUM)
        // o fim da janela e exclusivo: as 20:00 em ponto ja esta bloqueado
        assertThat(schedule.appAccessReason(MONDAY, minutes(20)))
            .isEqualTo(BlockReason.FORA_DA_JANELA)
    }

    @Test
    fun `app nao monitorado nunca bloqueia`() {
        val schedule = AppSchedule(
            packageName = INSTAGRAM,
            monitored = false,
            days = mapOf(MONDAY to DaySchedule(access = AppAccess.BLOQUEADO)),
        )

        assertThat(schedule.appAccessReason(MONDAY, minutes(12)))
            .isEqualTo(BlockReason.NENHUM)
    }

    @Test
    fun `cota libera ate o limite e bloqueia depois`() {
        val schedule = AppSchedule(
            packageName = INSTAGRAM,
            monitored = true,
            days = mapOf(
                MONDAY to DaySchedule(
                    shorts = ShortsPolicy.COTA,
                    shortsQuotaMinutes = 20,
                ),
            ),
        )

        assertThat(schedule.shortsReason(MONDAY, usedMinutesToday = 0))
            .isEqualTo(BlockReason.NENHUM)
        assertThat(schedule.shortsReason(MONDAY, usedMinutesToday = 19))
            .isEqualTo(BlockReason.NENHUM)
        assertThat(schedule.shortsReason(MONDAY, usedMinutesToday = 20))
            .isEqualTo(BlockReason.COTA_ESGOTADA)
    }

    @Test
    fun `dia sem configuracao usa o padrao seguro`() {
        val schedule = AppSchedule(packageName = INSTAGRAM, monitored = true)

        // por padrao o app abre, mas os Reels ficam bloqueados
        assertThat(schedule.appAccessReason(MONDAY, minutes(12)))
            .isEqualTo(BlockReason.NENHUM)
        assertThat(schedule.shortsReason(MONDAY, usedMinutesToday = 0))
            .isEqualTo(BlockReason.SHORTS_BLOQUEADO)
    }

    @Test
    fun `janelas encostadas viram uma so`() {
        val day = DaySchedule(access = AppAccess.JANELAS)
            .withWindow(TimeWindow(minutes(18), minutes(20)))
            .withWindow(TimeWindow(minutes(20), minutes(22)))

        assertThat(day.sortedWindows).containsExactly(TimeWindow(minutes(18), minutes(22)))
    }

    @Test
    fun `janelas sobrepostas sao unidas`() {
        val day = DaySchedule(access = AppAccess.JANELAS)
            .withWindow(TimeWindow(minutes(18), minutes(20)))
            .withWindow(TimeWindow(minutes(19), minutes(21)))

        assertThat(day.sortedWindows).containsExactly(TimeWindow(minutes(18), minutes(21)))
    }

    @Test
    fun `janela invalida e ignorada`() {
        val day = DaySchedule().withWindow(TimeWindow(minutes(20), minutes(18)))

        assertThat(day.sortedWindows).isEmpty()
    }

    @Test
    fun `copiar um dia aplica a configuracao nos outros`() {
        val monday = DaySchedule(
            access = AppAccess.JANELAS,
            windows = listOf(TimeWindow(minutes(18), minutes(20))),
            shorts = ShortsPolicy.COTA,
            shortsQuotaMinutes = 30,
        )
        val schedule = AppSchedule(packageName = INSTAGRAM, monitored = true)
            .withDay(MONDAY, monday)
            .copyDay(from = MONDAY, targets = listOf(1, 2, 3, 4, 5))

        assertThat(schedule.day(TUESDAY)).isEqualTo(monday)
        assertThat(schedule.day(5)).isEqualTo(monday)
        // domingo continua com o padrao
        assertThat(schedule.day(7)).isEqualTo(DaySchedule.Default)
    }

    @Test
    fun `proxima janela do dia ignora as que ja passaram`() {
        val schedule = AppSchedule(
            packageName = INSTAGRAM,
            monitored = true,
            days = mapOf(
                MONDAY to DaySchedule(
                    access = AppAccess.JANELAS,
                    windows = listOf(
                        TimeWindow(minutes(8), minutes(9)),
                        TimeWindow(minutes(18), minutes(20)),
                    ),
                ),
            ),
        )

        assertThat(schedule.nextWindowToday(MONDAY, minutes(10)))
            .isEqualTo(TimeWindow(minutes(18), minutes(20)))
        assertThat(schedule.nextWindowToday(MONDAY, minutes(21))).isNull()
        assertThat(schedule.currentWindowEnd(MONDAY, minutes(19))).isEqualTo(minutes(20))
        assertThat(schedule.currentWindowEnd(MONDAY, minutes(21))).isNull()
    }
}
