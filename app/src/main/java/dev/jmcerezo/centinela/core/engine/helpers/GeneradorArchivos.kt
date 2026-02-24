package dev.jmcerezo.centinela.core.engine.helpers

import android.content.Context
import java.io.File

/**
 * Encargado de la gestión de nombres y rutas de archivos de evidencias.
 */
object GeneradorArchivos {

    /**
     * Prepara un nuevo archivo .m4a con nombre correlativo (Evidencia 01, 02...).
     */
    fun prepararArchivo(contexto: Context): File {
        val storageDir = contexto.filesDir
        val archivosExistentes = storageDir.listFiles { f ->
            f.name.startsWith("Evidencia ") && f.extension == "m4a"
        } ?: arrayOf()

        val ultimoNumero = archivosExistentes.mapNotNull { archivo ->
            archivo.nameWithoutExtension
                .replace("Evidencia ", "")
                .toIntOrNull()
        }.maxOrNull() ?: 0

        return File(storageDir, "Evidencia %02d.m4a".format(ultimoNumero + 1))
    }
}
