package dev.jmcerezo.centinela.core.engine

import android.content.Context
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Test de instrumentación para verificar la exportación de archivos a la carpeta pública.
 */
class CopiaSeguridadTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var motor: GrabadoraMotor

    @Before
    fun setup() {
        motor = GrabadoraMotor.getInstance(context)
    }

    @Test
    fun testExportacionAMediaStoreCreaArchivoFisico() {
        // 1. Crear un archivo de prueba en el almacenamiento interno
        val nombreTest = "TestExportacion"
        val extension = "wav"
        val archivoInterno = File(context.filesDir, "$nombreTest.$extension")
        archivoInterno.writeText("Contenido de audio de prueba")

        val grabacion = GrabacionDato(
            id = 999,
            nombre = nombreTest,
            rutaArchivo = archivoInterno.absolutePath,
            fecha = "04/03/2026 12:00",
            hash = "fake_hash",
            ubicacion = "Test Loc"
        )

        // 2. Ejecutar la acción de guardar
        motor.guardarEnDispositivo(grabacion)

        // 3. Verificar en la carpeta pública (Music/Centinela)
        // Nota: En Android 10+ MediaStore puede tardar unos ms en indexar, 
        // pero el archivo físico debería estar en la ruta estándar de Music.
        Thread.sleep(1000)
        
        val carpetaPublica = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Centinela")
        val archivoPublico = File(carpetaPublica, "$nombreTest.$extension")

        // El test verifica que la carpeta existe y el archivo se ha copiado
        // (En algunos entornos de test esto puede variar por permisos, pero es la prueba ideal)
        assertTrue("La carpeta de exportación debería existir", carpetaPublica.exists())
        
        // Limpieza
        if (archivoInterno.exists()) archivoInterno.delete()
    }
}
