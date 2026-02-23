package dev.jmcerezo.centinela.core.engine

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
import dev.jmcerezo.centinela.data.local.db.AppDatabase
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Motor principal encargado de la gestión del hardware de audio, GPS y sistema de archivos.
 */
class GrabadoraMotor(private val contexto: Context) {

    private var grabador: MediaRecorder? = null
    private var reproductor: MediaPlayer? = null
    private var archivoAudio: File? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val db = AppDatabase.getDatabase(contexto)
    private val dao = db.grabacionDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    var onActualizarLista: (() -> Unit)? = null

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
        val archivosExistentes = storageDir.listFiles { f ->
            f.name.startsWith("Evidencia ") && f.extension == "m4a"
        } ?: arrayOf()
        val ultimoNumero = archivosExistentes.mapNotNull { it.nameWithoutExtension.replace("Evidencia ", "").toIntOrNull() }.maxOrNull() ?: 0
        val nuevoNombre = "Evidencia %02d".format(ultimoNumero + 1)
        archivoAudio = File(storageDir, "$nuevoNombre.m4a")
        ubicacionActual = "Ubicación no disponible"

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(contexto, Locale.getDefault())
                        val direcciones = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val coordenadas = "${location.latitude}, ${location.longitude}"
                        ubicacionActual = if (!direcciones.isNullOrEmpty()) {
                            "${direcciones[0].getAddressLine(0)} | GPS: $coordenadas"
                        } else {
                            "GPS: $coordenadas"
                        }
                    } catch (e: Exception) {
                        ubicacionActual = "GPS: ${location.latitude}, ${location.longitude}"
                    }
                }
            }

        despertarDispositivo()
        vibrar(longArrayOf(0, 300))

        try {
            grabador = crearGrabador().apply { prepare(); start() }
            estaGrabando = true
        } catch (e: Exception) {
            estaGrabando = false
            vibrar(longArrayOf(0, 100, 50, 100))
        }
    }

    fun detenerGrabacion(): String {
        if (!estaGrabando) return ""
        estaGrabando = false
        val ubicacionParaGuardar = ubicacionActual
        val hash = try {
            grabador?.apply { stop(); reset(); release() }
            grabador = null
            vibrar(longArrayOf(0, 100, 50, 100))
            liberarWakeLock()
            val generado = archivoAudio?.let { generarHashSHA256(it) } ?: "Error"
            archivoAudio?.let { file ->
                val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(file.lastModified())
                scope.launch {
                    dao.insert(GrabacionDato(
                        nombre = file.nameWithoutExtension,
                        rutaArchivo = file.absolutePath,
                        fecha = fecha,
                        hash = generado,
                        ubicacion = ubicacionParaGuardar
                    ))
                }
            }
            generado
        } catch (e: Exception) {
            liberarWakeLock()
            "Error"
        }
        Handler(Looper.getMainLooper()).post { onActualizarLista?.invoke() }
        return hash
    }

    fun eliminarGrabacion(grabacion: GrabacionDato) {
        scope.launch {
            val archivo = File(grabacion.rutaArchivo)
            if (archivo.exists()) archivo.delete()
            dao.delete(grabacion)
        }
    }

    fun renombrarGrabacion(grabacion: GrabacionDato, nuevoNombre: String) {
        scope.launch {
            try {
                val nombreLimpio = nuevoNombre.trim().replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
                if (nombreLimpio.isEmpty()) return@launch
                val archivoOriginal = File(grabacion.rutaArchivo)
                val nuevoArchivo = File(archivoOriginal.parent, "$nombreLimpio.m4a")
                if (archivoOriginal.exists() && archivoOriginal.renameTo(nuevoArchivo)) {
                    dao.update(grabacion.copy(nombre = nombreLimpio, rutaArchivo = nuevoArchivo.absolutePath))
                }
            } catch (e: Exception) { }
        }
    }

    fun compartirArchivo(grabacion: GrabacionDato) {
        val archivo = File(grabacion.rutaArchivo)
        if (!archivo.exists()) return
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contexto.startActivity(Intent.createChooser(intent, "Compartir evidencia..."))
    }

    fun reproducirAudio(grabacion: GrabacionDato) {
        val archivo = File(grabacion.rutaArchivo)
        if (!archivo.exists()) return
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

    private fun crearGrabador(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(contexto)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(archivoAudio!!.absolutePath)
        }
    }

    private fun despertarDispositivo() {
        try {
            val pm = contexto.getSystemService(Context.POWER_SERVICE) as PowerManager
            liberarWakeLock()
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "Centinela:Alerta")
            wakeLock?.acquire(3000L)
        } catch (e: Exception) { }
    }

    private fun liberarWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun vibrar(patron: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()
                vibrador.vibrate(VibrationEffect.createWaveform(patron, -1), attrs)
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(patron, -1)
            }
        } catch (e: Exception) { }
    }

    private fun generarHashSHA256(archivo: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val fis = FileInputStream(archivo)
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) { digest.update(buffer, 0, read) }
            fis.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { "Error" }
    }
}
