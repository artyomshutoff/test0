package com.example.cytisine

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val day = intent.getIntExtra("day", 0)
        val number = intent.getIntExtra("number", 0)
        if (day !in 1..25 || number <= 0) return

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("doses", "Напоминания о приёме", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Напоминания по выбранной схеме приёма"
            }
        )

        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, "doses")
            .setSmallIcon(com.example.cytisine.R.drawable.ic_launcher_foreground)
            .setContentTitle("Цитизин — время приёма")
            .setContentText("День $day, приём $number. Сверяйтесь с назначенной вам схемой.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        if (android.os.Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(day * 10 + number, notification)
        }
    }
}
