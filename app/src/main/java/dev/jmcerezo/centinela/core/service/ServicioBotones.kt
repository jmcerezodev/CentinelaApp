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
 * Es el guardián de la persistencia del sistema. Gestiona la detección de botones 
 * y utiliza estrategias agresivas para evitar que Android suspenda la app 
 * cuando la pantalla se apaga.
 */
@SuppressLint("AccessibilityService")
class ServicioBotones : AccessibilityService() {

    private lateinit var motor: GrabadoraMotor
    private lateinit var prefs: Preferencias
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    
    private var audioTrackSilencio: AudioTrack? = null
    private var hiloSilencio: Thread? = null
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "dev.jmcerezo.ACTUALIZAR_CONFIGURACION" -> actualizarEstadoServicio()
                Intent.ACTION_SCREEN_OFF -> asegurarMargenVolumen()
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    if (!prefs.botonesHabilitados) return
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC) {
                        val nuevoVol = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                        val antiguoVol = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                        
                        // Si el volumen sube, registramos pulsación (método redundante de seguridad)
                        if (nuevoVol > antiguoVol) motor.registrarPulsacion()

                        // Rearme: si llegamos al máximo con pantalla apagada, bajamos un punto
                        if (nuevoVol >= audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) && !powerManager.isInteractive) {
                            asegurarMargenVolumen()
                        }
                    }
                }
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
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or 
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        this.serviceInfo = info
        
        val filter = IntentFilter().apply {
            addAction("dev.jmcerezo.ACTUALIZAR_CONFIGURACION")
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        
        // Llamada inmediata para evitar ForegroundServiceDidNotStartInTimeException
        forzarInicioForeground()
        actualizarEstadoServicio()
    }

    private fun forzarInicioForeground() {
        val channelId = "centinela_botones_persist"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Servicio de Detección", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sistema Centinela")
            .setContentText("Detección de botones activa")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(2001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(2001, notification)
            }
        } catch (e: Exception) {}
    }

    private fun actualizarEstadoServicio() {
        if (prefs.botonesHabilitados || prefs.modoSilencioso || motor.estaGrabando) {
            activarModoInmortal()
        } else {
            desactivarModoInmortal()
        }
    }

    private fun activarModoInmortal() {
        // 1. WakeLock: Impide que el procesador entre en modo Deep Sleep
        if (wakeLock == null || !wakeLock!!.isHeld) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Centinela:AccessibilityCPU")
            wakeLock?.acquire(60 * 60 * 1000L)
        }

        // 2. MediaSession: Prioridad multimedia para que Android no cierre el proceso
        if (mediaSession == null) {
            mediaSession = MediaSession(this, "CentinelaPersistentSession").apply {
                setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1.0f).build())
                isActive = true
            }
        }

        // 3. Audio Silencioso (Anti-Doze): Vital para capturar audio con pantalla apagada
        if (audioTrackSilencio == null) {
            hiloSilencio = Thread {
                try {
                    val bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    audioTrackSilencio = AudioTrack.Builder()
                        .setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        .setAudioFormat(AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(44100)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(bufferSize).build()
                    val silenceBuffer = ShortArray(bufferSize)
                    audioTrackSilencio?.play()
                    while (!Thread.currentThread().isInterrupted && audioTrackSilencio != null) {
                        audioTrackSilencio?.write(silenceBuffer, 0, silenceBuffer.size)
                    }
                } catch (e: Exception) {}
            }.apply { start() }
        }
    }

    private fun desactivarModoInmortal() {
        hiloSilencio?.interrupt()
        hiloSilencio = null
        try { audioTrackSilencio?.stop(); audioTrackSilencio?.release() } catch (e: Exception) {}
        audioTrackSilencio = null
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
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

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!prefs.botonesHabilitados) return false
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            if (!powerManager.isInteractive) asegurarMargenVolumen()
            motor.registrarPulsacion()
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (e: Exception) {}
        desactivarModoInmortal()
        super.onDestroy()
    }
}
