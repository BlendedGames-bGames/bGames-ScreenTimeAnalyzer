package com.example.screentime

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.Image
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screentime.model.UpdateAttributesRequest
import com.example.screentime.network.RetrofitInstance
import com.example.screentime.ui.theme.ScreenTimeAnalyzerTheme
import com.example.screentime.ui.theme.rexliaFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun RequestPermissionScreen(
    modifier: Modifier = Modifier,
    onPermissionGranted: () -> Unit // 🔥 Callback para notificar que el permiso se otorgó
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasUsageAccessPermission(context)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Para calcular el tiempo de sueño, necesitamos acceso al uso del dispositivo.",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
            }) {
                Text(text = "Otorgar Permiso")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                hasPermission = hasUsageAccessPermission(context)
                if (hasPermission) {
                    onPermissionGranted() // 🔥 Notificar que el permiso fue concedido
                }
            }) {
                Text(text = "Ya otorgué el permiso")
            }
        }
    }
}



fun hasUsageAccessPermission(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
    val mode = appOpsManager.checkOpNoThrow(
        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}


@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
fun calculateScreenUsage(context: Context): Long? {
    if (!hasUsageAccessPermission(context)) {
        Log.e("ScreenUsage", "Permiso de acceso a uso no concedido")
        return null
    }
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val now = System.currentTimeMillis()
    val startOfDay = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)

    Log.d("ScreenUsage", "Cantidad de registros obtenidos: ${usageStats.size}")

    if (usageStats.isNullOrEmpty()) {
        Log.d("ScreenUsage", "No se obtuvieron datos de uso.")
        return null
    }

    val screenOffIntervals = mutableListOf<Pair<Long, Long>>()
    var previousEndTime = startOfDay

    Log.d("ScreenUsage", "=== Intervalos de uso de pantalla ===")
    for (stat in usageStats.sortedBy { it.firstTimeStamp }) {
        Log.d(
            "ScreenUsage",
            "Uso activo: ${formatTimestamp(stat.firstTimeStamp)} - ${formatTimestamp(stat.lastTimeStamp)}"
        )

        if (stat.firstTimeStamp > previousEndTime) {
            screenOffIntervals.add(previousEndTime to stat.firstTimeStamp)
        }
        previousEndTime = maxOf(previousEndTime, stat.lastTimeStamp)
    }

    if (previousEndTime < now) {
        screenOffIntervals.add(previousEndTime to now)
    }

    // Log intervalos de pantalla apagada
    Log.d("ScreenUsage", "=== Intervalos de pantalla apagada ===")
    var totalScreenOffTime = 0L
    screenOffIntervals.forEach { (start, end) ->
        Log.d("ScreenUsage", "Pantalla apagada: ${formatTimestamp(start)} - ${formatTimestamp(end)}")
        totalScreenOffTime += (end - start)  // Sumar la duración de cada intervalo de pantalla apagada
    }

    // Mostrar el tiempo total de pantalla apagada
    Log.d("ScreenUsage", "Tiempo total de pantalla apagada: ${totalScreenOffTime / 1000} segundos")
    return totalScreenOffTime
}

fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}




suspend fun claimPointsForPlayer(
    playerId: Int,
    pointsToClaim: Int,
    onClaim: (Int) -> Unit
) {
    // 🔥 Obtener los atributos actuales del jugador
    val response = RetrofitInstance.getApi().getPlayerAllAttributes(playerId)

    if (response.isSuccessful) {
        val attributes = response.body()
        val sleepAttribute = attributes?.find { it.name == "Afectivo" }  // Asegurar que es el correcto

        if (sleepAttribute != null) {
            val newData = sleepAttribute.data + pointsToClaim

            // 🔥 Crear la petición para actualizar los puntos
            val updateRequest = UpdateAttributesRequest(
                id_player = playerId,
                id_attributes = listOf(sleepAttribute.id_attributes),
                new_data = listOf(newData)
            )

            val updateResponse = RetrofitInstance.postApi().updatePlayerAttributes(updateRequest)

            if (updateResponse.isSuccessful) {
                onClaim(pointsToClaim)
                Log.d("ClaimPoints", "✅ Atributos actualizados con éxito: +$pointsToClaim puntos")
            } else {
                Log.e("ClaimPoints", "❌ Error al actualizar atributos. Código: ${updateResponse.code()}")
            }
        } else {
            Log.e("ClaimPoints", "⚠️ No se encontró el atributo relacionado con el sueño")
        }
    } else {
        Log.e("ClaimPoints", "❌ Error al obtener atributos del jugador. Código: ${response.code()}")
    }
}




fun getUserId(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    return sharedPreferences.getInt("user_id", -1)  // -1 es el valor por defecto si no existe
}

fun getUserName(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("user_name", "Usuario") ?: "Usuario"  // 🔥 Si no hay nombre, usa "Usuario"
}


@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..5 -> "Buenas noches"
        in 6..11 -> "Buenos días"
        in 12..20 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    val context = LocalContext.current
    var screenUsageTime by remember { mutableStateOf(0L) }
    var hasPermission by remember { mutableStateOf(hasUsageAccessPermission(context)) }
    val userName = remember { mutableStateOf(getUserName(context)) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            schedulePointClaim(context)
            screenUsageTime = calculateScreenUsage(context) ?: 0L
        }
    }

    if (!hasPermission) {
        RequestPermissionScreen {
            hasPermission = hasUsageAccessPermission(context)
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF121212), // Gris oscuro
                            Color(0xFF1A237E)  // Azul oscuro
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 🔥 Logo y título
                Image(
                    painter = painterResource(id = R.drawable.ic_icon), // Usa el mismo logo del login
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "Screen Time Analyzer",
                    fontFamily = rexliaFontFamily,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    style = TextStyle(
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(0f, 10f),
                            blurRadius = 30f
                        )
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 🔥 Saludo con nombre
                Text(
                    text = "$greeting, ${userName.value} 👋",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 🔥 Mostrar tiempo sin pantalla
                val screenOffHours = TimeUnit.MILLISECONDS.toHours(screenUsageTime)
                val screenOffMinutes = TimeUnit.MILLISECONDS.toMinutes(screenUsageTime) % 60
                Text(
                    text = "Tiempo sin pantalla: $screenOffHours horas y $screenOffMinutes minutos",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(
                    onClick = {
                        resetLastClaimDate(context)  // 🔥 Resetea la fecha de último canjeo
                        Toast.makeText(context, "Último canjeo reiniciado", Toast.LENGTH_SHORT).show()

                        // 🔥 Ejecutar el canjeo manualmente después de resetear la fecha
                        val intent = Intent(context, PointClaimReceiver::class.java)
                        PointClaimReceiver().onReceive(context, intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Reiniciar Canjeo y Forzar")
                }


            }
        }
    }
}

fun resetLastClaimDate(context: Context) {
    val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putLong("last_claim_date", 0)  // 🔥 En lugar de borrar, ponerlo en 0
        apply()
    }
    Log.d("ClaimReset", "🔄 Última fecha de canjeo reiniciada a 0.")

    // 🔥 Verificar si se guardó correctamente
    val newLastClaim = sharedPreferences.getLong("last_claim_date", -1)
    Log.d("ClaimReset", "📆 Nuevo valor en SharedPreferences: $newLastClaim")
}






@RequiresApi(Build.VERSION_CODES.M)
fun schedulePointClaim(context: Context) {
    if (!hasUsageAccessPermission(context)) {
        Log.e("AlarmManager", "❌ No se puede programar el canjeo sin permisos de uso.")
        return
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, PointClaimReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /*
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)  // 🔥 Hora correcta para el canjeo diario
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1) // Asegurar que sea el próximo día
        }
    }
    */
    val calendar = Calendar.getInstance().apply {
        add(Calendar.SECOND, 30) // La notificación se activará en 10 segundos
    }

    Log.d("AlarmManager", "📆 Canjeo programado para: ${calendar.time}")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            Log.e("AlarmManager", "❌ No se pueden programar alarmas exactas. Requiere permisos.")
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}






@RequiresApi(Build.VERSION_CODES.M)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ScreenTimeAnalyzerTheme {
        HomeScreen()
    }
}