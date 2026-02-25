package dev.jmcerezo.centinela.core.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
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
 * MOTOR DE GRABACIÓN CENTINELA (Singleton)
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
    var estaGrabando: Boolean = false
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

    /**
     * Procesa una pulsación de volumen con filtros de seguridad.
     */
    fun registrarPulsacion() {
        val ahora = System.currentTimeMillis()

        // 1. FILTRO ANTI-DUPLICADOS (Multi-source): 
        // Ignora si recibimos eventos de diferentes servicios para el mismo clic (ventana de 150ms)
        if (ahora - ultimaPulsacionRecibida < 150) return
        ultimaPulsacionRecibida = ahora

        // 2. BLOQUEO POST-ACCION: 
        // Evita que ráfagas accidentales tras iniciar/parar cambien el estado (ventana de 2 seg)
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

    @SuppressLint("MissingPermission")
    fun iniciarGrabacion() {
        if (estaGrabando) return
        val nuevoArchivo = GeneradorArchivos.prepararArchivo(contexto)
        location.capturarUbicacionActual()
        system.despertarDispositivo()
        system.vibrarConfirmacion()

        if (recorder.iniciar(nuevoArchivo)) {
            estaGrabando = true
            notificarCambioGlobal()
        } else {
            system.vibrarError()
        }
    }

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

        notificarCambioGlobal()
        return hash
    }

    private fun notificarCambioGlobal() {
        Handler(Looper.getMainLooper()).post { onActualizarLista?.invoke() }
        contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName))
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

    fun eliminarGrabacion(grabacion: GrabacionDato) {
        scope.launch {
            val file = File(grabacion.rutaArchivo)
            if (file.exists()) file.delete()
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
            } catch (e: Exception) {
                Log.e("Centinela", "Error al renombrar: ${e.message}")
            }
        }
    }

    fun compartirArchivo(grabacion: GrabacionDato) = recorder.compartir(grabacion)
    fun reproducirAudio(grabacion: GrabacionDato) = player.reproducir(File(grabacion.rutaArchivo))
    fun detenerReproduccion() = player.detener()
}
