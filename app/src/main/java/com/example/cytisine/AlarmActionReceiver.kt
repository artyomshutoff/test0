package com.example.cytisine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TAKEN = "com.example.cytisine.ALARM_TAKEN"
        const val ACTION_SNOOZE = "com.example.cytisine.ALARM_SNOOZE"
        const val ACTION_DISMISS = "com.example.cytisine.ALARM_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val day = intent.getIntExtra("day", 0)
        val number = intent.getIntExtra("number", 0)
        if (day !in 1..25 || number <= 0) return

        when (intent.action) {
            ACTION_TAKEN -> Prefs.markTaken(context, Dose(day, number, 0))
            ACTION_SNOOZE -> snooze(context, day, number)
        }
        stopAlarm(context)
    }

    private fun snooze(context: Context, day: Int, number: Int) {
        val alarmIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.cytisine.REMIND_SNOOZE"
            putExtra("day", day)
            putExtra("number", number)
        }
        val pi = PendingIntent.getBroadcast(
            context, 5000 + day * 10 + number, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + 5 * 60_000L

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            val showIntent = PendingIntent.getActivity(
                context,
                35_000 + day * 10 + number,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent), pi)
        }
    }

    private fun stopAlarm(context: Context) {
        context.stopService(Intent(context, AlarmService::class.java))
    }
}
