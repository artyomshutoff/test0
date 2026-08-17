package com.example.cytisine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val day = intent.getIntExtra("day", 0)
        val number = intent.getIntExtra("number", 0)
        if (day !in 1..25 || number <= 0) return

        val service = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra("day", day)
            putExtra("number", number)
        }
        context.startForegroundService(service)
    }
}
