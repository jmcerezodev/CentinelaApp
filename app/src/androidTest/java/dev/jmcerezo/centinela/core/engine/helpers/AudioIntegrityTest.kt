package dev.jmcerezo.centinela.core.engine.helpers

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class AudioIntegrityTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var audioManager: AudioRecorderManager

    @Before
    fun setup() {
        audioManager = AudioRecorderManager(context)
        // Limpiamos archivos previos
        context.filesDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun testWavHeaderIntegrity() {
        val testFile = File(context.filesDir, "test_integrity.wav")
        
        // 1. Iniciar grabación
        val iniciado = audioManager.iniciar(testFile)
        assertTrue("El motor de audio debería iniciar correctamente", iniciado)
        
        // 2. Grabar durante 1.5 segundos para asegurar que se escriben datos
        Thread.sleep(1500)
        
        // 3. Detener grabación
        val fileResult = audioManager.detener()
        assertTrue("El archivo resultante no debería ser null", fileResult != null)
        assertTrue("El archivo debería existir físicamente", testFile.exists())
        assertTrue("El archivo debería tener un tamaño mayor a los 44 bytes del header", testFile.length() > 44)

        // 4. Verificar Header WAV (44 bytes)
        val header = ByteArray(44)
        FileInputStream(testFile).use { fis ->
            fis.read(header)
        }

        // Firma RIFF
        assertEquals('R'.code.toByte(), header[0])
        assertEquals('I'.code.toByte(), header[1])
        assertEquals('F'.code.toByte(), header[2])
        assertEquals('F'.code.toByte(), header[3])

        // Firma WAVE
        assertEquals('W'.code.toByte(), header[8])
        assertEquals('A'.code.toByte(), header[9])
        assertEquals('V'.code.toByte(), header[10])
        assertEquals('E'.code.toByte(), header[11])

        // Subchunk 1: fmt
        assertEquals('f'.code.toByte(), header[12])
        assertEquals('m'.code.toByte(), header[13])
        assertEquals('t'.code.toByte(), header[14])
        assertEquals(' '.code.toByte(), header[15])

        // Subchunk 2: data
        assertEquals('d'.code.toByte(), header[36])
        assertEquals('a'.code.toByte(), header[37])
        assertEquals('t'.code.toByte(), header[38])
        assertEquals('a'.code.toByte(), header[39])
    }
}
