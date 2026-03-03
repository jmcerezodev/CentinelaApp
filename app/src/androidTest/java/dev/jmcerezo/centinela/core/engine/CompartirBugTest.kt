package dev.jmcerezo.centinela.core.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.jmcerezo.centinela.core.engine.helpers.AudioRecorderManager
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Test de instrumentación para blindar la funcionalidad de compartir.
 * Verifica que el sistema no crashea al intentar compartir un archivo interno.
 */
@RunWith(AndroidJUnit4::class)
class CompartirBugTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val audioRecorderManager = AudioRecorderManager(context)

    @Test
    fun testCompartirNoLanzaExcepcion() {
        // 1. Creamos un archivo de prueba en el almacenamiento interno de la app
        val testFile = File(context.filesDir, "test_compartir.m4a")
        if (!testFile.exists()) {
            testFile.createNewFile()
        }

        val grabacionFake = GrabacionDato(
            id = 999,
            nombre = "Test Blindaje",
            rutaArchivo = testFile.absolutePath,
            fecha = "01/01/2024 12:00",
            hash = "fake_hash_test"
        )

        // 2. Ejecutamos el método. 
        // Si el bug (falta de flags o context) persiste, el test fallará por un crash.
        try {
            audioRecorderManager.compartir(grabacionFake)
        } catch (e: Exception) {
            fail("El método compartir lanzó una excepción: ${e.message}")
        } finally {
            if (testFile.exists()) testFile.delete()
        }
    }
}
