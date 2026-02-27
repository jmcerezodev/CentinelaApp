package dev.jmcerezo.centinela.data.local.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de preferencias locales de la aplicación.
 * Utiliza SharedPreferences para almacenar configuraciones de usuario persistentes.
 */
class Preferencias(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("centinela_prefs", Context.MODE_PRIVATE)

    /**
     * Determina si el servicio de primer plano (notificación permanente) está activo.
     */
    var servicioPermanente: Boolean
        get() = prefs.getBoolean("servicio_permanente", false)
        set(value) = prefs.edit().putBoolean("servicio_permanente", value).apply()

    /**
     * Determina si el modo de audio silencioso para evitar la suspensión está activo.
     */
    var modoSilencioso: Boolean
        get() = prefs.getBoolean("modo_silencioso", false)
        set(value) = prefs.edit().putBoolean("modo_silencioso", value).apply()

    /**
     * Determina si la grabación mediante los botones de volumen está habilitada.
     * Ajustado: Ahora desactivado por defecto al instalar la aplicación.
     */
    var botonesHabilitados: Boolean
        get() = prefs.getBoolean("botones_habilitados", false)
        set(value) = prefs.edit().putBoolean("botones_habilitados", value).apply()

    /**
     * Determina si la seguridad biométrica está habilitada al abrir la aplicación.
     * Ajustado: Ahora desactivado por defecto.
     */
    var seguridadBiometrica: Boolean
        get() = prefs.getBoolean("seguridad_biometrica", false)
        set(value) = prefs.edit().putBoolean("seguridad_biometrica", value).apply()

    /**
     * Indica si ya se le ha preguntado al usuario sobre la biometría.
     */
    var biometriaPreguntada: Boolean
        get() = prefs.getBoolean("biometria_preguntada", false)
        set(value) = prefs.edit().putBoolean("biometria_preguntada", value).apply()
}
