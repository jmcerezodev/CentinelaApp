package dev.jmcerezo.centinela.core.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import dev.jmcerezo.centinela.core.engine.helpers.*
import dev.jmcerezo.centinela.data.local.db.AppDatabase
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import dev.jmcerezo.centinela.util.IntegrityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * MOTOR DE GRABACIÓN CENTINELA (Director de Orquesta)
 * 
 * Orquesta la lógica entre el hardware de audio, GPS y la base de datos.
 * Utiliza ayudantes especializados para cada tarea técnica.
 */
class GrabadoraMotor private constructor(private val contexto: Context) {

    // Especialistas modulares
    private val recorder = AudioRecorderManager(contexto)
    private val player = AudioPlayerManager()
    private val location = LocationManagerHelper(contexto)
    private val system = SystemInteractionHelper(contexto)
    
    // Acceso a Datos
    private val db = AppDatabase.getDatabase(contexto)
    private val dao = db.grabacionDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    var onActualizarLista: (() -> Unit)? = null
    var estaGrabando: Boolean = false
        private set

    companion object {
        @Volatile private var INSTANCE: GrabadoraMotor? = null
        fun getInstance(context: Context): GrabadoraMotor {
            return INSTANCE ?: synchronized(this) {
                GrabadoraMotor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Inicia la orquestación de una nueva evidencia legal.
     */
    fun iniciarGrabacion() {
        if (estaGrabando) return

        // 1. Preparamos el archivo y la ubicación
        val nuevoArchivo = GeneradorArchivos.prepararArchivo(contexto)
        location.capturarUbicacionActual()

        // 2. Alertamos al sistema y al usuario
        system.despertarDispositivo()
        system.vibrarConfirmacion()

        // 3. Iniciamos el hardware de audio
        if (recorder.iniciar(nuevoArchivo)) {
            estaGrabando = true
        } else {
            system.vibrarError()
        }
    }

    /**
     * Detiene la orquestación y guarda el resultado legal.
     */
    fun detenerGrabacion(): String {
        if (!estaGrabando) return ""
        estaGrabando = false

        val archivoAudio = recorder.detener()
        system.vibrarConfirmacion()
        system.liberarRecursos()

        val hash = if (archivoAudio != null) {
            val firma = IntegrityUtils.generarHashSHA256(archivoAudio)
            persistirEvidencia(archivoAudio, firma, location.obtenerUbicacionCapturada())
            firma
        } else "Error"

        notifyUI()
        return hash
    }

    private fun persistirEvidencia(file: File, hash: String, ubicacion: String) {
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(file.lastModified())
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

    private fun notifyUI() {
        Handler(Looper.getMainLooper()).post { onActualizarLista?.invoke() }
    }

    // Delegación de funciones a especialistas
    fun eliminarGrabacion(grabacion: GrabacionDato) {
        scope.launch {
            val file = File(grabacion.rutaArchivo)
            if (file.exists()) file.delete()
            dao.delete(grabacion)
        }
    }

    /**
     * Cambia el nombre de la evidencia tanto en el sistema de archivos como en la base de datos.
     */
    fun renombrarGrabacion(grabacion: GrabacionDato, nuevoNombre: String) {
        scope.launch {
            try {
                val nombreLimpio = nuevoNombre.trim().replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
                if (nombreLimpio.isEmpty()) return@launch
                
                val archivoOriginal = File(grabacion.rutaArchivo)
                val nuevoArchivo = File(archivoOriginal.parent, "$nombreLimpio.m4a")
                
                if (archivoOriginal.exists() && archivoOriginal.renameTo(nuevoArchivo)) {
                    dao.update(grabacion.copy(
                        nombre = nombreLimpio,
                        rutaArchivo = nuevoArchivo.absolutePath
                    ))
                }
            } catch (e: Exception) {
                // Silenciamos el error o podríamos notificar a la UI si fuera necesario
            }
        }
    }

    fun compartirArchivo(grabacion: GrabacionDato) = recorder.compartir(grabacion)
    fun reproducirAudio(grabacion: GrabacionDato) = player.reproducir(File(grabacion.rutaArchivo))
    fun detenerReproduccion() = player.detener()
}
