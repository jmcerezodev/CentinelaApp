package dev.jmcerezo.centinela.core.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.*
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import dev.jmcerezo.centinela.data.local.prefs.Preferencias

/**
 * SERVICIO DE ACCESIBILIDAD: CENTINELA
 * 
 * Gestiona la detección de botones. Utiliza tipo SPECIAL_USE para mayor estabilidad.
 */
@SuppressLint("AccessibilityService")
class ServicioBotones : AccessibilityService() {

    private lateinit var motor: GrabadoraMotor
    private lateinit var prefs: Preferencias
    private var audioTrackSilencio: AudioTrack? = null
    private var hiloSilencio: Thread? = null
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "dev.jmcerezo.ACTUALIZAR_CONFIGURACION") {
                actualizarEstadoServicio()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        motor = GrabadoraMotor.getInstance(this)
        prefs = Preferencias(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(POWER_SERVICE) as PowerManager

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC
            notificationTimeout = 100
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        this.serviceInfo = info
        
        ContextCompat.registerReceiver(this, receiver, IntentFilter("dev.jmcerezo.ACTUALIZAR_CONFIGURACION"), ContextCompat.RECEIVER_NOT_EXPORTED)
        actualizarEstadoServicio()
    }

    private fun asegurarMargenVolumen() {
        try {
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (current >= max) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max - 1, 0)
            }
        } catch (e: Exception) {}
    }

    fun actualizarEstadoServicio() {
        if (prefs.servicioPermanente || prefs.modoSilencioso) {
            mostrarNotificacionActiva()
        } else {
            detenerTodo()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }

        if (prefs.modoSilencioso) {
            iniciarSilencioDigital()
        } else {
            detenerSilencioDigital()
        }
    }

    private fun mostrarNotificacionActiva() {
        val channelId = "centinela_servicio"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Servicio Centinela", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val subtitulo = if (prefs.modoSilencioso) "Modo Anti-Suspensión activo" else "Servicio de seguridad activo"
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sistema Centinela")
            .setContentText(subtitulo)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Usamos SPECIAL_USE para evitar cierres por falta de permiso de micro
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {}
    }

    private fun iniciarSilencioDigital() {
        if (audioTrackSilencio != null) return
        hiloSilencio = Thread {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                audioTrackSilencio = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(bufferSize).build()
                val silenceBuffer = ShortArray(bufferSize)
                audioTrackSilencio?.play()
                while (!Thread.currentThread().isInterrupted && audioTrackSilencio != null) {
                    audioTrackSilencio?.write(silenceBuffer, 0, silenceBuffer.size)
                }
            } catch (e: Exception) {}
        }.apply { start() }
    }

    private fun detenerSilencioDigital() {
        hiloSilencio?.interrupt()
        hiloSilencio = null
        try {
            audioTrackSilencio?.stop()
            audioTrackSilencio?.release()
        } catch (e: Exception) {}
        audioTrackSilencio = null
    }

    private fun detenerTodo() {
        detenerSilencioDigital()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!prefs.botonesHabilitados) return false
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            motor.registrarPulsacion()
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (e: Exception) {}
        detenerTodo()
        super.onDestroy()
    }
}
