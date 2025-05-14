package com.example.screentime

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PointClaimReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("PointClaimReceiver", "📌 Dispositivo reiniciado, reprogramando canjeo.")
            schedulePointClaim(context)  // 🔥 Siempre reprogramar la alarma tras reinicio
        } else {
            Log.d("PointClaimReceiver", "📢 Se activó el BroadcastReceiver para el canjeo automático.")

            CoroutineScope(Dispatchers.IO).launch {
                val userId = getUserId(context)
                Log.d("PointClaimReceiver", "🆔 ID del jugador: $userId")

                if (hasClaimedPointsToday(context)) {
                    Log.d("PointClaimReceiver", "⚠️ Ya se reclamaron puntos hoy.")
                } else {
                    val screenUsageTime = calculateScreenUsage(context) ?: 0L
                    val hours = (screenUsageTime / TimeUnit.HOURS.toMillis(1)).coerceAtMost(10)
                    val points = (hours * 20).toInt()

                    claimPointsForPlayer(
                        playerId = userId,
                        pointsToClaim = points,
                        onClaim = { claimedPoints ->
                            Log.d("PointClaimReceiver", "✅ Puntos canjeados exitosamente: $claimedPoints")
                            saveClaimDate(context)  // 🔥 Guardar fecha de canjeo

                            // 🔥 Mostrar la notificación después del canjeo
                            showNotification(context, claimedPoints)
                        }
                    )
                }

                schedulePointClaim(context) // 🔥 Siempre reprogramar la alarma
                Log.d("PointClaimReceiver", "📆 Canjeo reprogramado para la siguiente ejecución.")
            }
        }
    }

    private fun showNotification(context: Context, points: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "sleep_time_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones uso de pantalla",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("¡Has recibido $points puntos!")
            .setContentText("Tu tiempo sin pantalla te ha generado recompensas. 🎉")
            .setSmallIcon(R.drawable.ic_icon)  // 🔥 Asegúrate de que este ícono exista
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        Log.d("NotificationReceiver", "📩 Enviando notificación de $points puntos...")

        notificationManager.notify(1, notification)
    }
}
