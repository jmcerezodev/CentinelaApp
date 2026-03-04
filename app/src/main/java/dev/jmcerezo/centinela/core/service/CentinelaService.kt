package dev.jmcerezo.centinela.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import dev.jmcerezo.centinela.ui.MainActivity

/**
 * SERVICIO MAESTRO DE NOTIFICACIONES
 * Centraliza toda la visibilidad de la app en una única notificación unificada y discreta.
 * No muestra información explícita sobre la grabación activa en la barra de estado.
 */
class CentinelaService : Service() {

    private lateinit var motor: GrabadoraMotor
    private lateinit var prefs: Preferencias

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "dev.jmcerezo.ACTUALIZAR_CONFIGURACION") {
                actualizarNotificacionUnificada()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        motor = GrabadoraMotor.getInstance(this)
        prefs = Preferencias(this)

        ContextCompat.registerReceiver(this, receiver, IntentFilter("dev.jmcerezo.ACTUALIZAR_CONFIGURACION"), ContextCompat.RECEIVER_NOT_EXPORTED)
        
        actualizarNotificacionUnificada()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        actualizarNotificacionUnificada()
        return START_STICKY
    }

    private fun actualizarNotificacionUnificada() {
        val debeEstarActivo = prefs.servicioPermanente || prefs.modoSilencioso || motor.estaGrabando || prefs.botonesHabilitados
        
        if (debeEstarActivo) {
            val texto = buildString {
                val activos = mutableListOf<String>()
                if (prefs.servicioPermanente) activos.add("Permanente")
                if (prefs.botonesHabilitados) activos.add("Botones")
                if (prefs.modoSilencioso) activos.add("Anti-Suspensión")
                
                if (activos.isEmpty()) append("Sistema de protección activo.")
                else append("Protección activa: ${activos.joinToString(", ")}.")
            }
            
            val notification = crearNotificacion(texto)
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Mantenemos el tipo MICROPHONE si estamos grabando para cumplir con la ley de Android,
                    // pero la notificación visual ya no lo menciona.
                    val type = if (motor.estaGrabando) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE 
                               else ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    startForeground(1001, notification, type)
                } else {
                    startForeground(1001, notification)
                }
            } catch (e: Exception) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(1001, notification)
                    }
                } catch (ex: Exception) {}
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    private fun crearNotificacion(texto: String): Notification {
        val channelId = "centinela_status"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Estado Centinela", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val intentApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intentApp, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // El título es siempre el mismo para mantener la discreción
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sistema Centinela")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Prioridad baja para no molestar
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (e: Exception) {}
        super.onDestroy()
    }
}
