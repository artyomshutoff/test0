package com.example.cytisine

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    companion object {
        const val ACTION_START = "com.example.cytisine.ALARM_START"
        const val ACTION_STOP = "com.example.cytisine.ALARM_STOP"
        const val CHANNEL_ID = "dose_alarms_v2"
        private const val NOTIFICATION_ID = 7001
    }

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val day = intent?.getIntExtra("day", 0) ?: 0
        val number = intent?.getIntExtra("number", 0) ?: 0
        if (day !in 1..25 || number <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(day, number))
        startSoundAndVibration()
        return START_NOT_STICKY
    }

    private fun buildNotification(day: Int, number: Int): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("day", day)
            putExtra("number", number)
        }
        val fullScreenPending = PendingIntent.getActivity(
            this, day * 10 + number, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionPending(action: String, requestCode: Int): PendingIntent {
            val i = Intent(this, AlarmActionReceiver::class.java).apply {
                this.action = action
                putExtra("day", day)
                putExtra("number", number)
            }
            return PendingIntent.getBroadcast(
                this, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Цитизин — будильник")
            .setContentText("День $day, приём $number")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .addAction(0, "Принято", actionPending(AlarmActionReceiver.ACTION_TAKEN, 1000 + day * 10 + number))
            .addAction(0, "Отложить 5 мин", actionPending(AlarmActionReceiver.ACTION_SNOOZE, 2000 + day * 10 + number))
            .build()
    }

    private fun startSoundAndVibration() {
        stopAlarm()
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmService, alarmUri)
            isLooping = true
            prepare()
            start()
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 700, 400, 700, 900)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopAlarm() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Будильники приёма",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Звуковые будильники по схеме приёма"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null) // звук воспроизводит AlarmService, чтобы он звонил до отключения
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
