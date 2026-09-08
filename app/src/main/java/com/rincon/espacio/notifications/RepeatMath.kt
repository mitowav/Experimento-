package com.rincon.espacio.notifications

import com.rincon.espacio.domain.model.RepeatRule
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Cálculo de la siguiente ocurrencia de un aviso repetido.
 *
 * Se aísla del scheduler para poder probarlo sin Android: es la parte con
 * reglas de calendario (fines de semana, meses de distinta longitud, cambios de
 * horario) y por tanto la que más merece tests.
 */
object RepeatMath {

    private const val MAX_STEPS = 4000

    fun next(
        triggerAtMillis: Long,
        repeat: RepeatRule,
        afterMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (repeat == RepeatRule.Once) return null
        var moment = ZonedDateTime.ofInstant(Instant.ofEpochMilli(triggerAtMillis), zone)
        val limit = ZonedDateTime.ofInstant(Instant.ofEpochMilli(afterMillis), zone)
        var guard = 0
        while (!moment.isAfter(limit) && guard < MAX_STEPS) {
            moment = when (repeat) {
                RepeatRule.Daily -> moment.plusDays(1)
                RepeatRule.Weekdays -> {
                    var candidate = moment.plusDays(1)
                    while (candidate.dayOfWeek == DayOfWeek.SATURDAY ||
                        candidate.dayOfWeek == DayOfWeek.SUNDAY
                    ) {
                        candidate = candidate.plusDays(1)
                    }
                    candidate
                }
                RepeatRule.Weekly -> moment.plusWeeks(1)
                RepeatRule.Monthly -> moment.plusMonths(1)
                RepeatRule.Once -> return null
            }
            guard++
        }
        return if (moment.isAfter(limit)) moment.toInstant().toEpochMilli() else null
    }
}
