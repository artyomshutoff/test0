package com.example.cytisine

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CytisineApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CytisineApp() {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf(Prefs.startDate(context)) }
    var firstTime by remember { mutableStateOf(Prefs.firstTime(context)) }
    var refresh by remember { mutableIntStateOf(0) }
    var showInfo by remember { mutableStateOf(true) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val today = LocalDate.now()
    val currentDay = (Duration.between(startDate.atStartOfDay(), today.atStartOfDay()).toDays() + 1).toInt().coerceIn(1, 25)
    val todayDoses = CytisineSchedule.dosesForDay(currentDay, startDate.plusDays((currentDay - 1).toLong()), firstTime)

    Scaffold(topBar = { TopAppBar(title = { Text("Цитизин") }) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (showInfo) item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Важно", fontWeight = FontWeight.Bold)
                        Text("Это приложение только напоминает о приёме и не заменяет инструкцию к препарату или назначение врача. Перед стартом сверяйте дозировку и противопоказания с упаковкой вашего препарата.")
                        TextButton(onClick = { showInfo = false }) { Text("Понятно") }
                    }
                }
            }

            item {
                Text("Настройка курса", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d -> startDate = LocalDate.of(y, m + 1, d) }, startDate.year, startDate.monthValue - 1, startDate.dayOfMonth).show()
                }) { Text("Дата начала: ${startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, h, m -> firstTime = LocalTime.of(h, m) }, firstTime.hour, firstTime.minute, true).show()
                }) { Text("Первый приём: ${firstTime.format(DateTimeFormatter.ofPattern("HH:mm"))}") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    Prefs.saveSchedule(context, startDate, firstTime)
                    Prefs.clearTaken(context)
                    ReminderScheduler.scheduleAll(context)
                    refresh++
                }) { Text("Сохранить и включить напоминания") }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val am = context.getSystemService(AlarmManager::class.java)
                    if (!am.canScheduleExactAlarms()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Точные будильники не разрешены: Android может немного сдвигать уведомления.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                            context.startActivity(i)
                        }) { Text("Разрешить точные напоминания") }
                    }
                }
            }

            item {
                HorizontalDivider()
                Text("Сегодня — день $currentDay", style = MaterialTheme.typography.titleLarge)
                Text(scheduleText(currentDay), style = MaterialTheme.typography.bodyMedium)
            }

            items(todayDoses, key = { "${it.day}-${it.number}-$refresh" }) { dose ->
                val taken = Prefs.isTaken(context, dose)
                val time = Instant.ofEpochMilli(dose.atMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                Card {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("$time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Приём ${dose.number}")
                        }
                        Button(enabled = !taken, onClick = { Prefs.markTaken(context, dose); refresh++ }) {
                            Text(if (taken) "Принято" else "Отметить")
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Text("Схема 25 дней", style = MaterialTheme.typography.titleMedium)
                Text("1–3 дни: каждые 2 ч, максимум 6/сутки\n4–12: каждые 2,5 ч, максимум 5/сутки\n13–16: каждые 3 ч, максимум 4/сутки\n17–20: каждые 5 ч, максимум 3/сутки\n21–25: 1–2 раза/сутки")
                Text("По стандартной схеме курения прекращают не позднее 5-го дня. Сверьте это с инструкцией именно вашего препарата.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun scheduleText(day: Int) = when (day) {
    in 1..3 -> "До 6 приёмов, интервал 2 часа"
    in 4..12 -> "До 5 приёмов, интервал 2,5 часа"
    in 13..16 -> "До 4 приёмов, интервал 3 часа"
    in 17..20 -> "До 3 приёмов, интервал 5 часов"
    in 21..25 -> "1–2 приёма в сутки"
    else -> "Курс завершён"
}
