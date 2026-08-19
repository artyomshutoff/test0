package com.example.cytisine

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime

object Prefs {
    private const val FILE = "cytisine_prefs"
    private fun p(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun startDate(context: Context): LocalDate =
        LocalDate.parse(p(context).getString("start_date", LocalDate.now().toString()))

    fun firstTime(context: Context): LocalTime =
        LocalTime.parse(p(context).getString("first_time", "08:00"))

    fun alarmsEnabled(context: Context): Boolean {
        val prefs = p(context)
        return prefs.getBoolean("alarms_enabled", prefs.contains("start_date"))
    }

    fun saveSchedule(context: Context, date: LocalDate, time: LocalTime) {
        p(context).edit()
            .putString("start_date", date.toString())
            .putString("first_time", time.toString())
            .putBoolean("alarms_enabled", true)
            .apply()
    }

    fun markTaken(context: Context, dose: Dose) {
        p(context).edit().putBoolean("taken_${dose.day}_${dose.number}", true).apply()
    }

    fun isTaken(context: Context, dose: Dose): Boolean =
        p(context).getBoolean("taken_${dose.day}_${dose.number}", false)

    fun clearTaken(context: Context) {
        val prefs = p(context)
        val e = prefs.edit()
        prefs.all.keys.filter { it.startsWith("taken_") }.forEach { e.remove(it) }
        e.apply()
    }

    fun setNeedsExactPermissionReschedule(context: Context, value: Boolean) {
        p(context).edit().putBoolean("needs_exact_reschedule", value).apply()
    }

    fun needsExactPermissionReschedule(context: Context): Boolean =
        p(context).getBoolean("needs_exact_reschedule", false)

    fun recordAlarmDelivery(
        context: Context,
        day: Int,
        number: Int,
        expectedAt: Long,
        receivedAt: Long,
        isSnooze: Boolean
    ) {
        p(context).edit()
            .putInt("last_alarm_day", day)
            .putInt("last_alarm_number", number)
            .putLong("last_alarm_expected", expectedAt)
            .putLong("last_alarm_received", receivedAt)
            .putBoolean("last_alarm_snooze", isSnooze)
            .apply()
    }

    data class AlarmDelivery(
        val day: Int,
        val number: Int,
        val expectedAt: Long,
        val receivedAt: Long,
        val isSnooze: Boolean
    )

    fun lastAlarmDelivery(context: Context): AlarmDelivery? {
        val prefs = p(context)
        val received = prefs.getLong("last_alarm_received", 0L)
        if (received == 0L) return null
        return AlarmDelivery(
            day = prefs.getInt("last_alarm_day", 0),
            number = prefs.getInt("last_alarm_number", 0),
            expectedAt = prefs.getLong("last_alarm_expected", 0L),
            receivedAt = received,
            isSnooze = prefs.getBoolean("last_alarm_snooze", false)
        )
    }
}
