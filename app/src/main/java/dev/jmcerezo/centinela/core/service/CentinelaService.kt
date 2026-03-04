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
import dev.jmcerezo.centinela.ui.MainActivity

/**
 * SERVICIO DE PERSISTENCIA Y DETECCIÓN EN SEGUNDO PLANO
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

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "dev.jmcerezo.ACTUALIZAR_CONFIGURACION" -> actualizarEstado()
                Intent.ACTION_SCREEN_OFF -> asegurarMargenVolumen()
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    if (!prefs.botonesHabilitados) return
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC) {
                        val nuevoVol = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                        val antiguoVol = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                        if (nuevoVol > antiguoVol) motor.registrarPulsacion()
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
            addAction("dev.jmcerezo.ACTUALIZAR_CONFIGURACION")
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        actualizarEstado()
        return START_STICKY
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
        if (prefs.servicioPermanente || prefs.modoSilencioso || motor.estaGrabando) {
            mostrarNotificacion(grabando = motor.estaGrabando)
        } else {
            detenerTodo()
            stopSelf()
            return
        }

        if (prefs.modoSilencioso || motor.estaGrabando) {
            iniciarModoAntiSuspension()
        } else {
            detenerModoAntiSuspension()
        }
    }

    private fun mostrarNotificacion(grabando: Boolean) {
        val channelId = "centinela_status"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Estado Centinela", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // Crear Intent para abrir la aplicación
        val intentApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intentApp, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val titulo = if (grabando) "CENTINELA: CAPTURANDO EVIDENCIA" else "Sistema Centinela"
        
        // LÓGICA DE AGRUPACIÓN: Construcción de texto dinámico combinado
        val texto = when {
            grabando -> "La grabación está activa y protegida."
            prefs.servicioPermanente && prefs.modoSilencioso -> "Servicio Permanente y Anti-Suspensión activos"
            prefs.modoSilencioso -> "Modo Anti-Suspensión activo"
            else -> "Servicio Permanente activo"
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent) // Al pulsar, abre la app
            .setPriority(if (grabando) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
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
            adquirirWakeLockForzado()
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

    private fun adquirirWakeLockForzado() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Centinela:Recording")
        wakeLock?.acquire(1 * 60 * 60 * 1000L)
    }

    private fun detenerModoAntiSuspension() {
        hiloSilencio?.interrupt()
        hiloSilencio = null
        try { audioTrackSilencio?.stop(); audioTrackSilencio?.release() } catch (e: Exception) {}
        audioTrackSilencio = null
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        if (!motor.estaGrabando && wakeLock?.isHeld == true) {
            wakeLock?.release()
            wakeLock = null
        }
    }

    private fun detenerTodo() {
        detenerModoAntiSuspension()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (e: Exception) {}
        detenerTodo()
        super.onDestroy()
    }
}
