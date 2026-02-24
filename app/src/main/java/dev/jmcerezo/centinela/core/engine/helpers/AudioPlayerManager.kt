package dev.jmcerezo.centinela.core.engine.helpers

import android.media.MediaPlayer
import java.io.File

/**
 * Gestor especializado en la reproducción de archivos de audio.
 */
class AudioPlayerManager {
    private var reproductor: MediaPlayer? = null

    /**
     * Inicia la reproducción de un archivo de audio. Detiene cualquier reproducción previa.
     */
    fun reproducir(archivo: File) {
        detener()
        try {
            reproductor = MediaPlayer().apply {
                setDataSource(archivo.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Detiene la reproducción y libera los recursos del hardware.
     */
    fun detener() {
        reproductor?.release()
        reproductor = null
    }
}
