package com.example.cytisine

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class AlarmActivity : ComponentActivity() {
    private val day by lazy { intent.getIntExtra("day", 0) }
    private val number by lazy { intent.getIntExtra("number", 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme {
                AlarmScreen(day, number, onTaken = {
                    sendAction(AlarmActionReceiver.ACTION_TAKEN)
                    finishAndRemoveTask()
                }, onSnooze = {
                    sendAction(AlarmActionReceiver.ACTION_SNOOZE)
                    finishAndRemoveTask()
                })
            }
        }
    }

    private fun sendAction(action: String) {
        sendBroadcast(Intent(this, AlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra("day", day)
            putExtra("number", number)
        })
    }
}

@Composable
private fun AlarmScreen(day: Int, number: Int, onTaken: () -> Unit, onSnooze: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Цитизин", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text("Время приёма", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text("День $day • приём $number", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(36.dp))
            Button(onClick = onTaken, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                Text("Принято")
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Отложить на 5 минут")
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Сверяйтесь с назначенной вам схемой и инструкцией к препарату.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
