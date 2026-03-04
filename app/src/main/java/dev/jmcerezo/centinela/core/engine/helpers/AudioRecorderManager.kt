package dev.jmcerezo.centinela.core.engine.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.concurrent.thread

/**
 * Gestor avanzado de audio usando AudioRecord.
 * Implementa Supresión de Ruido y Control de Ganancia por hardware para 
 * capturar voces claras sin cortes en entornos difíciles.
 */
class AudioRecorderManager(private val contexto: Context) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var archivoActual: File? = null
    
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    @SuppressLint("MissingPermission")
    fun iniciar(archivo: File): Boolean {
        if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        archivoActual = archivo
        return try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return false
            }

            // Activar efectos de hardware si están disponibles
            val sessionId = audioRecord!!.audioSessionId
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
                Log.d("Centinela", "Supresor de ruido de hardware ACTIVADO")
            }
            if (AutomaticGainControl.isAvailable()) {
                gainControl = AutomaticGainControl.create(sessionId)?.apply { enabled = true }
                Log.d("Centinela", "Control de ganancia de hardware ACTIVADO")
            }

            audioRecord?.startRecording()
            isRecording = true

            // Hilo de escritura a disco
            thread(start = true, name = "CentinelaAudioThread") {
                escribirAudioAFile(archivo)
            }

            true
        } catch (e: Exception) {
            Log.e("Centinela", "Error al iniciar AudioRecord: ${e.message}")
            false
        }
    }

    private fun escribirAudioAFile(archivo: File) {
        val data = ByteArray(bufferSize)
        FileOutputStream(archivo).use { fos ->
            // Dejamos espacio para el header de WAV (44 bytes)
            fos.write(ByteArray(44))
            
            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                if (read > 0) {
                    fos.write(data, 0, read)
                }
            }
        }
        // Una vez terminada la grabación, insertamos el header WAV correcto
        actualizarWavHeader(archivo)
    }

    private fun actualizarWavHeader(file: File) {
        val totalAudioLen = file.length() - 44
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate.toLong() and 0xff).toByte()
        header[25] = (sampleRate.toLong() shr 8 and 0xff).toByte()
        header[26] = (sampleRate.toLong() shr 16 and 0xff).toByte()
        header[27] = (sampleRate.toLong() shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (2 * channels / 8).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.write(header)
        }
    }

    fun detener(): File? {
        isRecording = false
        try {
            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED && recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
            noiseSuppressor?.release()
            gainControl?.release()
        } catch (e: Exception) {
            Log.e("Centinela", "Error al liberar AudioRecord: ${e.message}")
        }
        audioRecord = null
        noiseSuppressor = null
        gainControl = null
        return archivoActual
    }

    fun compartir(grabacion: GrabacionDato) {
        try {
            val file = File(grabacion.rutaArchivo)
            if (!file.exists()) return
            
            val authority = "${contexto.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(contexto, authority, file)
            
            val mimeType = when(file.extension.lowercase()) {
                "wav" -> "audio/wav"
                "ogg" -> "audio/ogg"
                else -> "audio/mp4"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(intent, "Compartir evidencia...")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            contexto.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e("Centinela", "Error al compartir: ${e.message}")
        }
    }
}
