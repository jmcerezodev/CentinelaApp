package dev.jmcerezo.centinela.core

import android.content.Context
import android.content.SharedPreferences

class Preferencias(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("centinela_prefs", Context.MODE_PRIVATE)

    var servicioPermanente: Boolean
        get() = prefs.getBoolean("servicio_permanente", false)
        set(value) = prefs.edit().putBoolean("servicio_permanente", value).apply()

    var modoSilencioso: Boolean
        get() = prefs.getBoolean("modo_silencioso", false)
        set(value) = prefs.edit().putBoolean("modo_silencioso", value).apply()
}
