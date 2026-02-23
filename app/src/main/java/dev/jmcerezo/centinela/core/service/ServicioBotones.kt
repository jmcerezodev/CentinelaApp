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
 * Responsabilidad: Detectar las pulsaciones de los botones físicos de volumen.
 * Este servicio implementa múltiples estrategias para evitar que Android suspenda 
 * la escucha cuando la pantalla se apaga.
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
    
    private var contadorPulsaciones = 0
    private var ultimaPulsacion: Long = 0
    private var ultimaPulsacionProcesada: Long = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "dev.jmcerezo.ACTUALIZAR_CONFIGURACION" -> actualizarEstadoServicio()
                Intent.ACTION_SCREEN_OFF -> asegurarMargenVolumen()
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC) {
                        val nuevoVol = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                        val antiguoVol = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                        if (nuevoVol > antiguoVol) registrarPulsacion()
                        
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
        motor = GrabadoraMotor(this)
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

        actualizarEstadoServicio()
    }

    private fun asegurarMargenVolumen() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (current >= max) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max - 1, 0)
        }
    }

    fun actualizarEstadoServicio() {
        if (prefs.servicioPermanente) {
            mostrarNotificacionPermanente()
        } else {
            detenerWakeLock()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }

        if (prefs.modoSilencioso) {
            solicitarFocoAudio()
            activarMediaSession()
            iniciarSilencioDigital()
            adquirirWakeLock()
        } else {
            detenerSilencioDigital()
            desactivarMediaSession()
            detenerWakeLock()
            abandonarFocoAudio()
        }
    }

    private fun solicitarFocoAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .build()
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun abandonarFocoAudio() {
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(null)
    }

    private fun mostrarNotificacionPermanente() {
        val channelId = "centinela_servicio"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Protección Centinela"
            val mChannel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(mChannel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sistema Centinela")
            .setContentText("Servicio de seguridad activo")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun activarMediaSession() {
        if (mediaSession != null) return
        mediaSession = MediaSession(this, "CentinelaSilentPlayer").apply {
            val state = PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
                .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                .build()
            setPlaybackState(state)
            isActive = true
        }
    }

    private fun desactivarMediaSession() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
    }

    private fun adquirirWakeLock() {
        if (wakeLock != null) return
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Centinela:CPU")
        wakeLock?.acquire(2 * 60 * 60 * 1000L) 
    }

    private fun detenerWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun iniciarSilencioDigital() {
        if (audioTrackSilencio != null) return
        hiloSilencio = Thread {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                audioTrackSilencio = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                val silenceBuffer = ShortArray(bufferSize)
                audioTrackSilencio?.play()
                while (!Thread.currentThread().isInterrupted && audioTrackSilencio != null) {
                    audioTrackSilencio?.write(silenceBuffer, 0, silenceBuffer.size)
                }
            } catch (e: Exception) { }
        }
        hiloSilencio?.start()
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

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                registrarPulsacion()
            }
            return false 
        }
        return false
    }

    private fun registrarPulsacion() {
        val tiempoActual = System.currentTimeMillis()
        if (tiempoActual - ultimaPulsacionProcesada < 100) return
        ultimaPulsacionProcesada = tiempoActual

        if (tiempoActual - ultimaPulsacion < 1000) {
            contadorPulsaciones++
        } else {
            contadorPulsaciones = 1
        }
        ultimaPulsacion = tiempoActual

        if (contadorPulsaciones == 3) {
            gestionarGrabacion()
            contadorPulsaciones = 0
        }
    }

    private fun gestionarGrabacion() {
        if (motor.estaGrabando) {
            motor.detenerGrabacion()
            sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_LISTA").setPackage(packageName))
        } else {
            motor.iniciarGrabacion()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (e: Exception) {}
        detenerSilencioDigital()
        desactivarMediaSession()
        detenerWakeLock()
        abandonarFocoAudio()
        super.onDestroy()
    }
}
