package com.example.cytisine

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class Dose(val day: Int, val number: Int, val atMillis: Long)

object CytisineSchedule {
    fun dosesForDay(day: Int, date: LocalDate, firstTime: LocalTime, zone: ZoneId = ZoneId.systemDefault()): List<Dose> {
        val offsetsMinutes = when (day) {
            in 1..3 -> listOf(0, 120, 240, 360, 480, 600)
            in 4..12 -> listOf(0, 150, 300, 450, 600)
            in 13..16 -> listOf(0, 180, 360, 540)
            in 17..20 -> listOf(0, 300, 600)
            in 21..25 -> listOf(0, 720)
            else -> emptyList()
        }
        return offsetsMinutes.mapIndexed { index, offset ->
            val dt = LocalDateTime.of(date, firstTime).plusMinutes(offset.toLong())
            Dose(day, index + 1, dt.atZone(zone).toInstant().toEpochMilli())
        }
    }

    fun all(startDate: LocalDate, firstTime: LocalTime, zone: ZoneId = ZoneId.systemDefault()): List<Dose> =
        (1..25).flatMap { day -> dosesForDay(day, startDate.plusDays((day - 1).toLong()), firstTime, zone) }
}
