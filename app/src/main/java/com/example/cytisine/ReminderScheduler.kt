package com.example.cytisine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    private const val MAIN_BASE = 10_000
    private const val SNOOZE_BASE = 20_000
    private const val SHOW_BASE = 30_000
    private const val RESCHEDULE_GRACE_MS = 2 * 60_000L

    /**
     * Schedules real alarm-clock alarms. Returns false when Android has not
     * granted the user-controlled "Alarms & reminders" special access.
     */
    fun scheduleAll(context: Context): Boolean {
        if (!Prefs.alarmsEnabled(context)) {
            cancelAll(context)
            return true
        }

        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            Prefs.setNeedsExactPermissionReschedule(context, true)
            return false
        }

        // Rescheduling is destructive, so it is done only after an explicit
        // schedule change or a system event (boot/time/permission change),
        // never on every Activity resume.
        cancelAll(context)

        val now = System.currentTimeMillis()
        val doses = CytisineSchedule.all(Prefs.startDate(context), Prefs.firstTime(context))
        doses.filter {
            !Prefs.isTaken(context, it) && it.atMillis >= now - RESCHEDULE_GRACE_MS
        }.forEach { dose ->
            scheduleDoseInternal(context, am, dose, dose.atMillis, isSnooze = false)
        }
        Prefs.setNeedsExactPermissionReschedule(context, false)
        return true
    }

    fun scheduleSnooze(context: Context, day: Int, number: Int, atMillis: Long): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            return false
        }
        cancelSnooze(context, day, number)
        scheduleDoseInternal(context, am, Dose(day, number, atMillis), atMillis, isSnooze = true)
        return true
    }

    fun cancelDose(context: Context, day: Int, number: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        servicePendingIntent(context, day, number, false, 0L, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it) }
        servicePendingIntent(context, day, number, true, 0L, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it) }
    }

    fun cancelSnooze(context: Context, day: Int, number: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        servicePendingIntent(context, day, number, true, 0L, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it) }
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        (1..25).forEach { day ->
            (1..6).forEach { number ->
                servicePendingIntent(context, day, number, false, 0L, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it) }
                servicePendingIntent(context, day, number, true, 0L, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it) }
            }
        }
    }

    private fun scheduleDoseInternal(
        context: Context,
        am: AlarmManager,
        dose: Dose,
        atMillis: Long,
        isSnooze: Boolean
    ) {
        val operation = servicePendingIntent(
            context,
            dose.day,
            dose.number,
            isSnooze,
            atMillis,
            PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return
        val showIntent = showAlarmIntent(context, dose.day, dose.number, isSnooze)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showIntent), operation)
    }

    private fun servicePendingIntent(
        context: Context,
        day: Int,
        number: Int,
        isSnooze: Boolean,
        expectedAt: Long,
        baseFlag: Int
    ): PendingIntent? {
        val requestCode = (if (isSnooze) SNOOZE_BASE else MAIN_BASE) + day * 10 + number
        val intent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra("day", day)
            putExtra("number", number)
            putExtra("expected_at", expectedAt)
            putExtra("is_snooze", isSnooze)
        }
        val flags = baseFlag or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            @Suppress("DEPRECATION")
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }

    private fun showAlarmIntent(context: Context, day: Int, number: Int, isSnooze: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val requestCode = SHOW_BASE + (if (isSnooze) 5_000 else 0) + day * 10 + number
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
