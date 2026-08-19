package com.example.cytisine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val day = intent.getIntExtra("day", 0)
        val number = intent.getIntExtra("number", 0)
        if (day !in 1..25 || number <= 0) return

        // Bridge the short interval between AlarmManager delivering the broadcast
        // and the foreground alarm service starting audio on a sleeping device.
        val powerManager = context.getSystemService(PowerManager::class.java)
        powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "cytisine:alarm-start"
        ).apply {
            setReferenceCounted(false)
            acquire(30_000L) // timeout releases it automatically
        }

        val service = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra("day", day)
            putExtra("number", number)
        }
        context.startForegroundService(service)
    }
}
