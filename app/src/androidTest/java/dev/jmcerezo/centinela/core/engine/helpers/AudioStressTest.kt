package dev.jmcerezo.centinela.core.engine.helpers

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Test de estrés para el motor de audio.
 * Verifica que el sistema puede manejar ciclos rápidos de inicio/detención 
 * sin bloquear el hardware ni agotar recursos.
 */
class AudioStressTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var audioManager: AudioRecorderManager

    @Before
    fun setup() {
        audioManager = AudioRecorderManager(context)
        // Limpiamos el directorio de pruebas
        context.filesDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun testCiclosRapidosGrabacion() {
        val ciclos = 10
        for (i in 1..ciclos) {
            val testFile = File(context.filesDir, "stress_test_$i.wav")
            
            // Iniciar
            val iniciado = audioManager.iniciar(testFile)
            assertTrue("Fallo al iniciar en ciclo $i", iniciado)
            
            // Espera mínima para simular actividad real del buffer
            Thread.sleep(300)
            
            // Detener
            val resultado = audioManager.detener()
            assertNotNull("El resultado no debe ser null en ciclo $i", resultado)
            assertTrue("El archivo debería existir en ciclo $i", testFile.exists())
            
            // Verificamos que el archivo tenga al menos el header (44 bytes)
            assertTrue("El archivo en ciclo $i es demasiado pequeño", testFile.length() >= 44)
        }
    }
}
