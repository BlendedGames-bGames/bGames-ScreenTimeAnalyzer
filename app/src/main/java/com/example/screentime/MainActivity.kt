package com.example.screentime

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.screentime.notifications.NotificationScheduler
import com.example.screentime.ui.theme.ScreenTimeAnalyzerTheme
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // 🔥 Notificación de prueba tras otorgar el permiso
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "test_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Test", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val testNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🔔 Notificación de prueba")
            .setContentText("Si ves esto, las notificaciones estan activadas y funcionan correctamente.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(99, testNotification)

        super.onCreate(savedInstanceState)
        setContent {
            ScreenTimeAnalyzerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
        NotificationScheduler.scheduleDailyNotification(this)
        requestExactAlarmPermission(this)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun requestNotificationPermission(activity: ComponentActivity) {
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                NotificationScheduler.scheduleDailyNotification(activity)
            } else {
                Log.e("NotificationScheduler", "Permiso de notificaciones DENEGADO")
            }
        }.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        NotificationScheduler.scheduleDailyNotification(activity)
    }
}

fun requestExactAlarmPermission(activity: ComponentActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            activity.startActivity(intent)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun AppContent(modifier: Modifier = Modifier) {
    // Estado para manejar la pantalla actual y el email del usuario
    var currentScreen by remember { mutableStateOf("login") }
    var userEmail by remember { mutableStateOf("") }

    // Manejo de pantallas
    when (currentScreen) {
        "login" -> LoginScreen(
            onLoginSuccess = { email ->
                userEmail = email
                currentScreen = "home"
            },
            modifier = modifier
        )
        "home" -> HomeScreen(
            modifier = modifier
        )
    }
}

