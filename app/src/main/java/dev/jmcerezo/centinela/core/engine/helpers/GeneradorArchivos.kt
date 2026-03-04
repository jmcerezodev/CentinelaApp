package dev.jmcerezo.centinela.core.engine.helpers

import android.content.Context
import java.io.File

/**
 * Encargado de la gestión de nombres y rutas de archivos de evidencias.
 * Adaptado para soportar el formato WAV de alta fidelidad y filtrado DSP.
 */
object GeneradorArchivos {

    /**
     * Prepara un nuevo archivo con nombre correlativo (Evidencia 01, 02...).
     * Utiliza .wav para grabaciones PCM sin pérdida con supresión de ruido.
     */
    fun prepararArchivo(contexto: Context): File {
        val storageDir = contexto.filesDir
        val extensionActual = "wav"
        
        // Buscamos archivos existentes con cualquier extensión válida previa
        val extensionesSoportadas = listOf("wav", "ogg", "m4a", "opus", "mp4")
        
        val archivosExistentes = storageDir.listFiles { f ->
            f.name.startsWith("Evidencia ") && extensionesSoportadas.contains(f.extension.lowercase())
        } ?: arrayOf()

        val ultimoNumero = archivosExistentes.mapNotNull { archivo ->
            archivo.nameWithoutExtension
                .replace("Evidencia ", "")
                .toIntOrNull()
        }.maxOrNull() ?: 0

        val nuevoNombre = "Evidencia %02d.$extensionActual".format(ultimoNumero + 1)
        return File(storageDir, nuevoNombre)
    }
}
