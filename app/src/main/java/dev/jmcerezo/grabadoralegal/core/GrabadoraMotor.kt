package dev.jmcerezo.grabadoralegal.core

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import androidx.core.content.FileProvider
import dev.jmcerezo.grabadoralegal.model.GrabacionDato
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

class GrabadoraMotor(private val contexto: Context) {

    private var grabador: MediaRecorder? = null
    private var reproductor: MediaPlayer? = null
    private var archivoAudio: File? = null
    private var wakeLock: PowerManager.WakeLock? = null

    var estaGrabando: Boolean = false
        private set

    private val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = contexto.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        contexto.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun iniciarGrabacion() {
        if (estaGrabando) return

        // 1. FORZAR ENCENDIDO DE PANTALLA (El "chispazo" para despertar al sistema)
        try {
            val pm = contexto.getSystemService(Context.POWER_SERVICE) as PowerManager
            liberarWakeLock()

            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "Grabadora:Despertar"
            )
            // Encendemos 3 segundos para garantizar que el hardware responda
            wakeLock?.acquire(3000L)
        } catch (e: Exception) {
            Log.e("Centinela", "Error al despertar: ${e.message}")
        }

        // 2. VIBRACIÓN DE CONFIRMACIÓN
        vibrar(longArrayOf(0, 300))

        try {
            val storageDir = contexto.filesDir
            archivoAudio = File(storageDir, "REC_${System.currentTimeMillis()}.m4a")

            grabador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(contexto)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                // Al estar la pantalla encendida, MIC es ahora seguro y estable
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(archivoAudio!!.absolutePath)
                prepare()
                start()
            }

            estaGrabando = true
            Log.d("Centinela", "Grabación iniciada con pantalla despierta")

        } catch (e: Exception) {
            estaGrabando = false
            liberarWakeLock()
            Log.e("Centinela", "Fallo al iniciar: ${e.message}")
            vibrar(longArrayOf(0, 100, 50, 100))
        }
    }

    fun detenerGrabacion(): String {
        if (!estaGrabando) return ""
        estaGrabando = false

        return try {
            // Despertar también al detener para asegurar el guardado del archivo
            val pm = contexto.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val stopLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "Grabadora:Detener")
            stopLock.acquire(2000L)

            grabador?.apply {
                stop()
                reset()
                release()
            }
            grabador = null

            vibrar(longArrayOf(0, 100, 50, 100))
            liberarWakeLock()

            archivoAudio?.let { generarHashSHA256(it) } ?: "Error"
        } catch (e: Exception) {
            liberarWakeLock()
            "Error"
        }
    }

    // --- NUEVA FUNCIÓN PARA RENOMBRAR ARCHIVOS ---
    fun renombrarGrabacion(archivoOriginal: File, nuevoNombre: String): Boolean {
        return try {
            // Limpiamos el nombre para evitar caracteres prohibidos en el sistema de archivos
            val nombreLimpio = nuevoNombre.trim().replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
            if (nombreLimpio.isEmpty()) return false

            val nuevoArchivo = File(archivoOriginal.parent, "$nombreLimpio.m4a")

            if (archivoOriginal.exists()) {
                archivoOriginal.renameTo(nuevoArchivo)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("Centinela", "Error al renombrar: ${e.message}")
            false
        }
    }

    private fun liberarWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun vibrar(patron: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                vibrador.vibrate(VibrationEffect.createWaveform(patron, -1), attrs)
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(patron, -1)
            }
        } catch (e: Exception) {
            Log.e("Centinela", "Error vibración")
        }
    }

    // --- MÉTODOS DE GESTIÓN (Manteniendo toda tu funcionalidad original) ---

    fun obtenerGrabaciones(): List<GrabacionDato> {
        val archivos = contexto.filesDir.listFiles { _, nombre -> nombre.endsWith(".m4a") } ?: arrayOf()
        return archivos.map { file ->
            GrabacionDato(
                nombre = file.name,
                archivo = file,
                fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(file.lastModified()),
                hash = generarHashSHA256(file)
            )
        }.sortedByDescending { it.archivo.lastModified() }
    }

    fun eliminarGrabacion(archivo: File): Boolean = try { if (archivo.exists()) archivo.delete() else false } catch (e: Exception) { false }

    fun compartirArchivo(grabacion: GrabacionDato) {
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", grabacion.archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contexto.startActivity(Intent.createChooser(intent, "Compartir..."))
    }

    fun reproducirAudio(archivo: File) {
        detenerReproduccion()
        reproductor = MediaPlayer().apply {
            setDataSource(archivo.absolutePath)
            prepare()
            start()
        }
    }

    fun detenerReproduccion() {
        reproductor?.release()
        reproductor = null
    }

    private fun generarHashSHA256(archivo: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val fis = FileInputStream(archivo)
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
            fis.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { "Error" }
    }
}