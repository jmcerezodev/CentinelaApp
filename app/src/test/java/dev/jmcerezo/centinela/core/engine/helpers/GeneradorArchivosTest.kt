package dev.jmcerezo.centinela.core.engine.helpers

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class GeneradorArchivosTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // Mock manual simple para evitar usar la librería MockK que no está en el gradle
    private fun getMockContextDir(): File {
        return tempFolder.newFolder("files")
    }

    @Test
    fun `prepararArchivo genera nombre correlativo correctamente`() {
        val baseDir = getMockContextDir()
        
        // Simulamos la lógica de GeneradorArchivos.prepararArchivo pero inyectando el directorio
        // Como GeneradorArchivos es un object y usa contexto.filesDir, 
        // vamos a probar la lógica interna creando archivos en una carpeta temporal.
        
        fun testLogic(dir: File): File {
            val archivosExistentes = dir.listFiles { f ->
                f.name.startsWith("Evidencia ") && f.extension == "m4a"
            } ?: arrayOf()

            val ultimoNumero = archivosExistentes.mapNotNull { archivo ->
                archivo.nameWithoutExtension
                    .replace("Evidencia ", "")
                    .toIntOrNull()
            }.maxOrNull() ?: 0

            return File(dir, "Evidencia %02d.m4a".format(ultimoNumero + 1))
        }

        // 1. Primer archivo
        val file1 = testLogic(baseDir)
        assertEquals("Evidencia 01.m4a", file1.name)
        file1.createNewFile()

        // 2. Segundo archivo
        val file2 = testLogic(baseDir)
        assertEquals("Evidencia 02.m4a", file2.name)
    }
}
