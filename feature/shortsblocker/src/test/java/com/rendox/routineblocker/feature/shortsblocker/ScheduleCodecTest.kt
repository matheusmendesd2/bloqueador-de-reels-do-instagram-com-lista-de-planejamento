package com.rendox.routineblocker.feature.shortsblocker

import com.google.common.truth.Truth.assertThat
import com.rendox.routineblocker.feature.shortsblocker.models.AppAccess
import com.rendox.routineblocker.feature.shortsblocker.models.AppSchedule
import com.rendox.routineblocker.feature.shortsblocker.models.DaySchedule
import com.rendox.routineblocker.feature.shortsblocker.models.ShortsPolicy
import com.rendox.routineblocker.feature.shortsblocker.models.TimeWindow
import com.rendox.routineblocker.feature.shortsblocker.utils.ScheduleCodec
import org.junit.jupiter.api.Test

private const val INSTAGRAM = "com.instagram.android"

class ScheduleCodecTest {

    @Test
    fun `codifica e decodifica sem perder nada`() {
        val original = AppSchedule(
            packageName = INSTAGRAM,
            monitored = true,
            days = mapOf(
                1 to DaySchedule(
                    access = AppAccess.JANELAS,
                    windows = listOf(TimeWindow(1080, 1200), TimeWindow(1290, 1350)),
                    shorts = ShortsPolicy.COTA,
                    shortsQuotaMinutes = 20,
                ),
                2 to DaySchedule(access = AppAccess.BLOQUEADO, shorts = ShortsPolicy.BLOQUEADO),
                7 to DaySchedule(access = AppAccess.LIBERADO, shorts = ShortsPolicy.LIBERADO),
            ),
        )

        val decoded = ScheduleCodec.decode(INSTAGRAM, ScheduleCodec.encode(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `string vazia vira agenda padrao`() {
        assertThat(ScheduleCodec.decode(INSTAGRAM, null))
            .isEqualTo(AppSchedule(packageName = INSTAGRAM))
        assertThat(ScheduleCodec.decode(INSTAGRAM, ""))
            .isEqualTo(AppSchedule(packageName = INSTAGRAM))
    }

    @Test
    fun `dados corrompidos nao derrubam a decodificacao`() {
        assertThat(ScheduleCodec.decode(INSTAGRAM, "lixo aleatorio"))
            .isEqualTo(AppSchedule(packageName = INSTAGRAM))

        val comDiaInvalido = ScheduleCodec.decode(INSTAGRAM, "v1|1|9,J,1080-1200,C,20;1,B,,B,0")
        assertThat(comDiaInvalido.days.keys).containsExactly(1)
        assertThat(comDiaInvalido.day(1).access).isEqualTo(AppAccess.BLOQUEADO)
    }

    @Test
    fun `janela invertida e descartada na decodificacao`() {
        val decoded = ScheduleCodec.decode(INSTAGRAM, "v1|1|1,J,1200-1080+600-660,B,0")

        assertThat(decoded.day(1).sortedWindows).containsExactly(TimeWindow(600, 660))
    }

    @Test
    fun `cota fora do limite e ajustada`() {
        val decoded = ScheduleCodec.decode(INSTAGRAM, "v1|1|1,L,,C,9999")

        assertThat(decoded.day(1).shortsQuotaMinutes).isEqualTo(DaySchedule.MAX_QUOTA_MINUTES)
    }
}
