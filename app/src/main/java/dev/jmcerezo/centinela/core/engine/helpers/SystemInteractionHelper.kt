package dev.jmcerezo.centinela.core.engine.helpers

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Gestor especializado en la interacción con el sistema físico (Vibración y WakeLock).
 */
class SystemInteractionHelper(private val contexto: Context) {

    private var wakeLock: PowerManager.WakeLock? = null
    private val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = contexto.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        contexto.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Activa el WakeLock para despertar la pantalla y la CPU durante un breve periodo.
     */
    fun despertarDispositivo() {
        try {
            val pm = contexto.getSystemService(Context.POWER_SERVICE) as PowerManager
            liberarWakeLock()
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Centinela:Alerta"
            )
            wakeLock?.acquire(3000L)
        } catch (e: Exception) {}
    }

    /**
     * Ejecuta un patrón de vibración corto para confirmar una acción exitosa.
     */
    fun vibrarConfirmacion() {
        vibrar(longArrayOf(0, 300))
    }

    /**
     * Ejecuta un patrón de vibración entrecortado para avisar de un error.
     */
    fun vibrarError() {
        vibrar(longArrayOf(0, 100, 50, 100))
    }

    /**
     * Libera el WakeLock si está activo.
     */
    fun liberarRecursos() {
        liberarWakeLock()
    }

    private fun liberarWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun vibrar(patron: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(patron, -1)
            }
        } catch (e: Exception) {}
    }
}
