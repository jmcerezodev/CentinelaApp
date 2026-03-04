package dev.jmcerezo.centinela.core.engine

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import dev.jmcerezo.centinela.core.engine.helpers.*
import dev.jmcerezo.centinela.data.local.db.AppDatabase
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import dev.jmcerezo.centinela.util.IntegrityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * MOTOR DE GRABACIÓN CENTINELA (Singleton)
 * Gestiona el ciclo de vida de las evidencias y la interacción con el hardware.
 */
class GrabadoraMotor private constructor(private val contexto: Context) {

    private val recorder = AudioRecorderManager(contexto)
    private val player = AudioPlayerManager()
    private val location = LocationManagerHelper(contexto)
    private val system = SystemInteractionHelper(contexto)
    
    private val db = AppDatabase.getDatabase(contexto)
    private val dao = db.grabacionDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    var onActualizarLista: (() -> Unit)? = null
    @Volatile var estaGrabando: Boolean = false
        private set

    private var contadorPulsaciones = 0
    private var ultimaPulsacion: Long = 0
    private var ultimaAccionExitosa: Long = 0
    private var ultimaPulsacionRecibida: Long = 0

    companion object {
        @Volatile private var INSTANCE: GrabadoraMotor? = null
        fun getInstance(context: Context): GrabadoraMotor {
            return INSTANCE ?: synchronized(this) {
                GrabadoraMotor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun resetEstadoInterno() {
        contadorPulsaciones = 0
        ultimaPulsacion = 0
        ultimaAccionExitosa = 0
        ultimaPulsacionRecibida = 0
        if (estaGrabando) {
            detenerGrabacion()
        }
    }

    fun registrarPulsacion() {
        val ahora = System.currentTimeMillis()
        if (ahora - ultimaPulsacionRecibida < 150) return
        ultimaPulsacionRecibida = ahora
        if (ahora - ultimaAccionExitosa < 2000) return

        if (ahora - ultimaPulsacion < 1000) {
            contadorPulsaciones++
        } else {
            contadorPulsaciones = 1
        }
        ultimaPulsacion = ahora

        if (contadorPulsaciones >= 3) {
            contadorPulsaciones = 0
            ultimaAccionExitosa = ahora
            gestionarEstadoGrabacion()
        }
    }

    private fun gestionarEstadoGrabacion() {
        if (estaGrabando) detenerGrabacion() else iniciarGrabacion()
    }

    fun iniciarGrabacion() {
        if (estaGrabando) return
        
        val tienePermiso = ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!tienePermiso) {
            system.vibrarError()
            return
        }

        try {
            val nuevoArchivo = GeneradorArchivos.prepararArchivo(contexto)
            system.despertarDispositivo()
            system.vibrarConfirmacion()

            if (recorder.iniciar(nuevoArchivo)) {
                estaGrabando = true
                notificarCambioGlobal()
            } else {
                system.vibrarError()
            }
        } catch (e: Exception) {
            Log.e("Centinela", "Error fatal al iniciar grabación: ${e.message}")
            estaGrabando = false
            system.vibrarError()
        }
    }

    fun detenerGrabacion(): String {
        if (!estaGrabando) return ""
        
        estaGrabando = false
        val archivoAudio = recorder.detener()
        system.vibrarConfirmacion()
        system.liberarRecursos()

        if (archivoAudio != null) {
            location.capturarUbicacionActual { ubicacion ->
                val firma = IntegrityUtils.generarHashSHA256(archivoAudio)
                persistirEvidencia(archivoAudio, firma, ubicacion)
            }
            notificarCambioGlobal()
            return IntegrityUtils.generarHashSHA256(archivoAudio) 
        } else {
            notificarCambioGlobal()
            return "Error"
        }
    }

    private fun notificarCambioGlobal() {
        Handler(Looper.getMainLooper()).post { 
            onActualizarLista?.invoke() 
        }
        val intent = Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName)
        contexto.sendBroadcast(intent)
    }

    private fun persistirEvidencia(file: File, hash: String, ubicacion: String) {
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(System.currentTimeMillis())
        scope.launch {
            dao.insert(GrabacionDato(
                nombre = file.nameWithoutExtension,
                rutaArchivo = file.absolutePath,
                fecha = fecha,
                hash = hash,
                ubicacion = ubicacion
            ))
        }
    }

    fun eliminarGrabacion(grabacion: GrabacionDato) {
        scope.launch {
            try {
                val file = File(grabacion.rutaArchivo)
                if (file.exists()) file.delete()
                dao.delete(grabacion)
            } catch (e: Exception) {
                Log.e("Centinela", "Error al eliminar: ${e.message}")
            }
        }
    }

    fun renombrarGrabacion(grabacion: GrabacionDato, nuevoNombre: String) {
        scope.launch {
            try {
                val nombreLimpio = nuevoNombre.trim().replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
                if (nombreLimpio.isEmpty()) return@launch
                val archivoOriginal = File(grabacion.rutaArchivo)
                val extension = archivoOriginal.extension
                val nuevoArchivo = File(archivoOriginal.parent, "$nombreLimpio.$extension")

                if (archivoOriginal.exists() && archivoOriginal.renameTo(nuevoArchivo)) {
                    dao.update(grabacion.copy(nombre = nombreLimpio, rutaArchivo = nuevoArchivo.absolutePath))
                }
            } catch (e: Exception) {
                Log.e("Centinela", "Error al renombrar: ${e.message}")
            }
        }
    }

    /**
     * Guarda la grabación en la carpeta pública de música del dispositivo.
     * Utiliza MediaStore para asegurar compatibilidad con Android 10+.
     */
    fun guardarEnDispositivo(grabacion: GrabacionDato) {
        scope.launch {
            try {
                val file = File(grabacion.rutaArchivo)
                if (!file.exists()) return@launch

                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "${grabacion.nombre}.${file.extension}")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Centinela")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                }

                val resolver = contexto.contentResolver
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let { destinationUri ->
                    resolver.openOutputStream(destinationUri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(destinationUri, contentValues, null, null)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(contexto, "Guardado en Music/Centinela", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Centinela", "Error al guardar en dispositivo: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(contexto, "Error al guardar archivo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun compartirArchivo(grabacion: GrabacionDato) = recorder.compartir(grabacion)
    fun reproducirAudio(grabacion: GrabacionDato) = player.reproducir(File(grabacion.rutaArchivo))
    fun detenerReproduccion() = player.detener()
}
