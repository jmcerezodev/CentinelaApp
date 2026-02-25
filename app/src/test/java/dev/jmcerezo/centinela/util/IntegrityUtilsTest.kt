package dev.jmcerezo.centinela.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class IntegrityUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testGenerarHashSHA256Correcto() {
        val file = tempFolder.newFile("test.txt")
        file.writeText("Contenido de prueba para Centinela")
        
        val expectedHash = IntegrityUtils.generarHashSHA256(file)
        
        assertEquals(64, expectedHash.length)
        
        val sameHash = IntegrityUtils.generarHashSHA256(file)
        assertEquals(expectedHash, sameHash)
    }

    @Test
    fun testGenerarHashSHA256CambiaConContenido() {
        val file = tempFolder.newFile("test_cambio.txt")
        file.writeText("Original")
        val hash1 = IntegrityUtils.generarHashSHA256(file)
        
        file.writeText("Modificado")
        val hash2 = IntegrityUtils.generarHashSHA256(file)
        
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testGenerarHashSHA256ErrorSiNoExiste() {
        val nonExistentFile = File("archivo_fantasma.m4a")
        val result = IntegrityUtils.generarHashSHA256(nonExistentFile)
        
        // Corregido: Coincidir con el string exacto de IntegrityUtils.kt
        assertEquals("Error al generar firma", result)
    }
}
