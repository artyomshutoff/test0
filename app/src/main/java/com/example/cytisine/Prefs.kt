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
        // Migration from previous versions: if a schedule was already saved,
        // keep alarms enabled after updating the APK.
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
}
