package dev.jmcerezo.centinela.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import dev.jmcerezo.centinela.data.local.prefs.Preferencias

/**
 * SERVICIO DE PERSISTENCIA Y DETECCIÓN EN SEGUNDO PLANO
 * 
 * Gestiona la notificación, el silencio digital y la detección de volumen
 * cuando la pantalla está apagada.
 */
class CentinelaService : Service() {

    private lateinit var motor: GrabadoraMotor
    private lateinit var prefs: Preferencias
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    
    private var audioTrackSilencio: AudioTrack? = null
    private var hiloSilencio: Thread? = null
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var contadorPulsaciones = 0
    private var ultimaPulsacion: Long = 0
    private var ultimaPulsacionProcesada: Long = 0
    private var ultimaAccionMotor: Long = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> asegurarMargenVolumen()
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    // Solo procesamos si los botones están habilitados en ajustes
                    if (!prefs.botonesHabilitados) return

                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC) {
                        val nuevoVol = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                        val antiguoVol = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                        
                        if (nuevoVol > antiguoVol) {
                            registrarPulsacion()
                        }
                        
                        if (nuevoVol >= audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) && !powerManager.isInteractive) {
                            asegurarMargenVolumen()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        motor = GrabadoraMotor.getInstance(this)
        prefs = Preferencias(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        actualizarEstado()
        return START_STICKY
    }

    private fun registrarPulsacion() {
        val tiempoActual = System.currentTimeMillis()
        
        if (tiempoActual - ultimaAccionMotor < 2000) return
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
            ultimaAccionMotor = System.currentTimeMillis()
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

    private fun asegurarMargenVolumen() {
        if (!prefs.botonesHabilitados) return
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (current >= max) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max - 1, 0)
        }
    }

    private fun actualizarEstado() {
        if (prefs.servicioPermanente || prefs.modoSilencioso) {
            mostrarNotificacion()
        } else {
            detenerTodo()
            stopSelf()
            return
        }

        if (prefs.modoSilencioso) {
            iniciarModoAntiSuspension()
        } else {
            detenerModoAntiSuspension()
        }
    }

    private fun mostrarNotificacion() {
        val channelId = "centinela_status"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Estado Centinela", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val texto = if (prefs.modoSilencioso) "Modo Anti-Suspensión activo" else "Servicio de seguridad activo"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sistema Centinela")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun iniciarModoAntiSuspension() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .build()
            audioManager.requestAudioFocus(request)
        }

        if (mediaSession == null) {
            mediaSession = MediaSession(this, "CentinelaSilence").apply {
                setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1.0f).build())
                isActive = true
            }
        }

        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Centinela:KeepAlive")
            wakeLock?.acquire(2 * 60 * 60 * 1000L)
        }

        if (audioTrackSilencio == null) {
            hiloSilencio = Thread {
                try {
                    val bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    audioTrackSilencio = AudioTrack.Builder()
                        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(bufferSize).build()
                    val buffer = ShortArray(bufferSize)
                    audioTrackSilencio?.play()
                    while (!Thread.currentThread().isInterrupted) { audioTrackSilencio?.write(buffer, 0, buffer.size) }
                } catch (e: Exception) {}
            }.apply { start() }
        }
    }

    private fun detenerModoAntiSuspension() {
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

    private fun detenerTodo() {
        detenerModoAntiSuspension()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(receiver)
        detenerTodo()
        super.onDestroy()
    }
}
