package com.example.screentime.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.screentime.MainActivity
import com.example.screentime.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("NotificationReceiver", "📢 Se activó el BroadcastReceiver para mostrar la notificación.")
        showNotification(context)
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación (Android 8.0+)
        val channelId = "sleep_time_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("¡Puntos cobrados!")
            .setContentText("Acabas de cobrar tus puntos por no uso del celular.")
            .setSmallIcon(R.drawable.ic_icon)  // 🔥 Asegúrate de que este ícono exista
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        Log.d("NotificationReceiver", "📩 Enviando notificación...")

        notificationManager.notify(1, notification)
    }
}
