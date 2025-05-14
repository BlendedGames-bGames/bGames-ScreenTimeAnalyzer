package com.example.screentime

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun hasClaimedPointsToday(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    val lastClaimTime = sharedPreferences.getLong("last_claim_date", 0)
    val now = System.currentTimeMillis()

    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    Log.d("ClaimCheck", "📆 Última fecha de canjeo guardada: $lastClaimTime")
    Log.d("ClaimCheck", "📆 Inicio del día actual: $todayStart")

    if (lastClaimTime == 0L) {
        Log.d("ClaimCheck", "🔄 No hay un último canje registrado. Permitiendo canjeo.")
        return false  // 🔥 Si el último canje está en 0, permitir canjeo
    }

    return lastClaimTime >= todayStart
}





fun saveClaimDate(context: Context) {
    val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putLong("last_claim_date", System.currentTimeMillis())  // 🔥 Guardar fecha exacta
        apply()
    }
    Log.d("ClaimSave", "✅ Fecha de canjeo guardada correctamente: ${System.currentTimeMillis()}")
}




