package dev.jmcerezo.centinela.core.engine.helpers

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class GeneradorArchivosTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        // Limpiamos el directorio de archivos internos antes de cada test
        context.filesDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun testGeneracionNombreCorrelativoConMultiplesExtensiones() {
        // 1. Simulamos que ya existen archivos con diferentes extensiones (antiguas y nuevas)
        File(context.filesDir, "Evidencia 01.m4a").createNewFile()
        File(context.filesDir, "Evidencia 02.ogg").createNewFile()
        File(context.filesDir, "Evidencia 03.mp4").createNewFile()

        // 2. Ejecutamos la lógica real del generador
        val siguienteArchivo = GeneradorArchivos.prepararArchivo(context)

        // 3. Verificamos que detecta el número 03 y propone el 04 con la nueva extensión .wav
        assertEquals("Evidencia 04.wav", siguienteArchivo.name)
    }

    @Test
    fun testGeneracionPrimerArchivo() {
        // Directorio vacío
        val siguienteArchivo = GeneradorArchivos.prepararArchivo(context)
        assertEquals("Evidencia 01.wav", siguienteArchivo.name)
    }
}
