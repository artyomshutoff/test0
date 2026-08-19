package com.example.cytisine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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
            ACTION_TAKEN -> {
                Prefs.markTaken(context, Dose(day, number, 0))
                ReminderScheduler.cancelDose(context, day, number)
            }
            ACTION_SNOOZE -> {
                val at = System.currentTimeMillis() + 5 * 60_000L
                ReminderScheduler.scheduleSnooze(context, day, number, at)
            }
        }
        stopAlarm(context)
    }

    private fun stopAlarm(context: Context) {
        context.stopService(Intent(context, AlarmService::class.java))
    }
}
