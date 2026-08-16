package com.example.cytisine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    private const val ACTION = "com.example.cytisine.REMIND"

    fun scheduleAll(context: Context) {
        cancelAll(context)
        val now = System.currentTimeMillis()
        val doses = CytisineSchedule.all(Prefs.startDate(context), Prefs.firstTime(context))
        val am = context.getSystemService(AlarmManager::class.java)
        doses.filter { it.atMillis > now && !Prefs.isTaken(context, it) }.forEach { dose ->
            val pi = pendingIntent(context, dose)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dose.atMillis, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dose.atMillis, pi)
            }
        }
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
}
