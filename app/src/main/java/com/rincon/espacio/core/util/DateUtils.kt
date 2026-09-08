package com.rincon.espacio.core.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val SpanishLocale: Locale = Locale.forLanguageTag("es-ES")

object Dates {

    fun today(): LocalDate = LocalDate.now()

    fun epochDay(date: LocalDate): Long = date.toEpochDay()

    fun fromEpochDay(day: Long): LocalDate = LocalDate.ofEpochDay(day)

    fun secondOfDay(time: LocalTime): Int = time.toSecondOfDay()

    fun fromSecondOfDay(second: Int): LocalTime = LocalTime.ofSecondOfDay(second.toLong())

    fun millisOf(date: LocalDate, time: LocalTime, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    /** "Martes, 8 de septiembre" — con inicial mayúscula. */
    fun longDate(date: LocalDate): String {
        val day = date.dayOfWeek.getDisplayName(TextStyle.FULL, SpanishLocale)
        val month = date.month.getDisplayName(TextStyle.FULL, SpanishLocale)
        return "${day.replaceFirstChar { it.uppercase(SpanishLocale) }}, ${date.dayOfMonth} de $month"
    }

    fun shortDate(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.SHORT, SpanishLocale).trimEnd('.')
        return "${date.dayOfMonth} ${month.replaceFirstChar { it.uppercase(SpanishLocale) }}"
    }

    fun monthTitle(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.FULL, SpanishLocale)
        return "${month.replaceFirstChar { it.uppercase(SpanishLocale) }} ${date.year}"
    }

    fun weekdayInitial(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.NARROW, SpanishLocale).uppercase(SpanishLocale)

    fun weekdayShort(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, SpanishLocale)
            .trimEnd('.')
            .replaceFirstChar { it.uppercase(SpanishLocale) }

    fun time(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

    /** Lunes de la semana que contiene [date]. */
    fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

    /** "Hoy", "Mañana", "En 3 días", "Hace 2 días"... */
    fun relative(date: LocalDate, from: LocalDate = today()): String {
        val days = ChronoUnit.DAYS.between(from, date)
        return when {
            days == 0L -> "Hoy"
            days == 1L -> "Mañana"
            days == -1L -> "Ayer"
            days in 2..6 -> weekdayShort(date)
            days in -6..-2 -> "Hace ${-days} días"
            else -> shortDate(date)
        }
    }

    fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
        in 5..12 -> "Buenos días"
        in 13..20 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    fun greetingIcon(now: LocalTime = LocalTime.now()): String = when (now.hour) {
        in 6..18 -> "sun"
        else -> "moon"
    }
}
