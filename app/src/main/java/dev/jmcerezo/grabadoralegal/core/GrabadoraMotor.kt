package dev.jmcerezo.grabadoralegal.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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

    // GPS: Cliente de servicios de ubicación
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(contexto)
    private var ubicacionActual: String = "Ubicación no disponible"

    var estaGrabando: Boolean = false
        private set

    private val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = contexto.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        contexto.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    @SuppressLint("MissingPermission")
    fun iniciarGrabacion() {
        if (estaGrabando) return

        val storageDir = contexto.filesDir

        // --- NUEVA LÓGICA DE NOMBRE CORRELATIVO ---
        val archivosExistentes = storageDir.listFiles { f ->
            f.name.startsWith("Evidencia ") && f.extension == "m4a"
        } ?: arrayOf()

        val ultimoNumero = archivosExistentes.mapNotNull { archivo ->
            archivo.nameWithoutExtension
                .replace("Evidencia ", "")
                .toIntOrNull()
        }.maxOrNull() ?: 0

        val nuevoNombre = "Evidencia %02d".format(ultimoNumero + 1)
        // ------------------------------------------

        archivoAudio = File(storageDir, "$nuevoNombre.m4a")
        val archivoLoc = File(storageDir, "$nuevoNombre.loc")

        // Captura de ubicación asíncrona con dirección + coordenadas exactas
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(contexto, Locale.getDefault())
                        val direcciones = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val coordenadasPuras = "${location.latitude}, ${location.longitude}"

                        ubicacionActual = if (!direcciones.isNullOrEmpty()) {
                            // Combinamos dirección aproximada con coordenadas exactas
                            "${direcciones[0].getAddressLine(0)} | GPS: $coordenadasPuras"
                        } else {
                            "GPS: $coordenadasPuras"
                        }
                    } catch (e: Exception) {
                        ubicacionActual = "GPS: ${location.latitude}, ${location.longitude}"
                    }
                    // Actualización del archivo de metadatos con el dato completo
                    if (archivoLoc.exists()) {
                        archivoLoc.writeText(ubicacionActual)
                    }
                }
            }

        // 1. FORZAR ENCENDIDO DE PANTALLA
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
            wakeLock?.acquire(3000L)
        } catch (e: Exception) {
            Log.e("Centinela", "Error al despertar: ${e.message}")
        }

        // 2. VIBRACIÓN DE CONFIRMACIÓN
        vibrar(longArrayOf(0, 300))

        try {
            grabador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(contexto)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(archivoAudio!!.absolutePath)
                prepare()
                start()
            }

            estaGrabando = true

            // Creamos el archivo inicial
            archivoLoc.writeText(ubicacionActual)

        } catch (e: Exception) {
            estaGrabando = false
            liberarWakeLock()
            vibrar(longArrayOf(0, 100, 50, 100))
        }
    }

    fun detenerGrabacion(): String {
        if (!estaGrabando) return ""
        estaGrabando = false

        return try {
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

            val hash = archivoAudio?.let { generarHashSHA256(it) } ?: "Error"
            ubicacionActual = "Ubicación no disponible"
            hash
        } catch (e: Exception) {
            liberarWakeLock()
            "Error"
        }
    }

    fun renombrarGrabacion(archivoOriginal: File, nuevoNombre: String): Boolean {
        return try {
            val nombreLimpio = nuevoNombre.trim().replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
            if (nombreLimpio.isEmpty()) return false

            val nuevoArchivo = File(archivoOriginal.parent, "$nombreLimpio.m4a")
            val nuevoArchivoLoc = File(archivoOriginal.parent, "$nombreLimpio.loc")

            val archivoLocOriginal = File(archivoOriginal.absolutePath.replace(".m4a", ".loc"))
            if (archivoLocOriginal.exists()) archivoLocOriginal.renameTo(nuevoArchivoLoc)

            if (archivoOriginal.exists()) {
                archivoOriginal.renameTo(nuevoArchivo)
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun obtenerGrabaciones(): List<GrabacionDato> {
        val archivos = contexto.filesDir.listFiles { _, nombre -> nombre.endsWith(".m4a") } ?: arrayOf()
        return archivos.map { file ->
            val archivoLoc = File(file.absolutePath.replace(".m4a", ".loc"))
            val locData = if (archivoLoc.exists()) archivoLoc.readText() else "Ubicación no disponible"

            GrabacionDato(
                nombre = file.name,
                archivo = file,
                fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(file.lastModified()),
                hash = generarHashSHA256(file),
                ubicacion = locData
            )
        }.sortedByDescending { it.archivo.lastModified() }
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
        } catch (e: Exception) { }
    }

    fun eliminarGrabacion(archivo: File): Boolean = try {
        val archivoLoc = File(archivo.absolutePath.replace(".m4a", ".loc"))
        if (archivoLoc.exists()) archivoLoc.delete()
        if (archivo.exists()) archivo.delete() else false
    } catch (e: Exception) { false }

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