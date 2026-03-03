package dev.jmcerezo.centinela.core.engine.helpers

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import java.io.File

/**
 * Gestor especializado en el hardware de grabación (MediaRecorder).
 * Optimizado para captura de voz clara en entornos difíciles (bolsillos, bolsos).
 */
class AudioRecorderManager(private val contexto: Context) {

    private var grabador: MediaRecorder? = null
    private var archivoActual: File? = null

    /**
     * Configura e inicia el micrófono con parámetros de alta fidelidad para voz.
     */
    fun iniciar(archivo: File): Boolean {
        archivoActual = archivo
        return try {
            grabador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(contexto)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                // CAMBIO CLAVE: VOICE_RECOGNITION activa los filtros de ruido del hardware
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                
                // AJUSTES DE CALIDAD: Voces nítidas y volumen equilibrado
                setAudioChannels(1) // Mono es mejor para centrar la captura en la voz
                setAudioSamplingRate(44100) // Calidad profesional (44.1 kHz)
                setAudioEncodingBitRate(128000) // 128 kbps para evitar compresión brusca
                
                setOutputFile(archivo.absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            Log.e("Centinela", "Error al iniciar hardware de audio: ${e.message}")
            false
        }
    }

    /**
     * Detiene la grabación y libera el micrófono.
     * @return El archivo generado o null si hubo error.
     */
    fun detener(): File? {
        return try {
            grabador?.apply {
                stop()
                reset()
                release()
            }
            grabador = null
            archivoActual
        } catch (e: Exception) {
            Log.e("Centinela", "Error al detener grabación: ${e.message}")
            null
        }
    }

    /**
     * Lógica de compartición de archivos mediante FileProvider.
     */
    fun compartir(grabacion: GrabacionDato) {
        try {
            val file = File(grabacion.rutaArchivo)
            if (!file.exists()) {
                Log.e("Centinela", "El archivo de audio no existe en la ruta: ${grabacion.rutaArchivo}")
                return
            }
            
            // Usamos el ID del paquete dinámico para mayor compatibilidad
            val authority = "${contexto.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(contexto, authority, file)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                // Es vital otorgar permisos de lectura a la aplicación receptora
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Crear el Chooser asegurando que el Intent resultante sea una Activity nueva si se lanza desde un contexto de aplicación
            val chooserIntent = Intent.createChooser(intent, "Compartir evidencia...")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            contexto.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e("Centinela", "Error fatal al intentar compartir: ${e.message}")
            e.printStackTrace()
        }
    }
}
