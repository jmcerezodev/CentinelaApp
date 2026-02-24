package dev.jmcerezo.centinela.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Utilidades para garantizar la integridad de las evidencias.
 */
object IntegrityUtils {

    /**
     * Genera un hash SHA-256 de un archivo para certificar que no ha sido manipulado.
     * Es una pieza clave para la validez legal de la grabación.
     */
    fun generarHashSHA256(archivo: File): String {
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
        } catch (e: Exception) {
            "Error al generar firma"
        }
    }
}
