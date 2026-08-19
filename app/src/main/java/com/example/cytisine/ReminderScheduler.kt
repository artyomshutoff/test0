package com.example.cytisine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    private const val ACTION = "com.example.cytisine.REMIND"

    /**
     * Schedules only true, user-visible alarm-clock alarms.
     * Returns false if Android has not granted exact-alarm access.
     */
    fun scheduleAll(context: Context): Boolean {
        cancelAll(context)
        if (!Prefs.alarmsEnabled(context)) return true

        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Do NOT silently fall back to an inexact alarm: for a medicine alarm,
            // a delayed fallback is worse than clearly asking for the required access.
            return false
        }

        val now = System.currentTimeMillis()
        val doses = CytisineSchedule.all(Prefs.startDate(context), Prefs.firstTime(context))
        doses.filter { it.atMillis > now && !Prefs.isTaken(context, it) }.forEach { dose ->
            val operation = pendingIntent(context, dose)
            val showIntent = showAlarmIntent(context, dose)
            val info = AlarmManager.AlarmClockInfo(dose.atMillis, showIntent)
            am.setAlarmClock(info, operation)
        }
        return true
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        (1..25).forEach { day ->
            (1..6).forEach { number ->
                val fake = Dose(day, number, 0)
                am.cancel(pendingIntent(context, fake))
            }
        }
    }

    private fun pendingIntent(context: Context, dose: Dose): PendingIntent {
        val requestCode = dose.day * 10 + dose.number
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION
            putExtra("day", dose.day)
            putExtra("number", dose.number)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showAlarmIntent(context: Context, dose: Dose): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            30_000 + dose.day * 10 + dose.number,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
