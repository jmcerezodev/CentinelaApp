package dev.jmcerezo.centinela.ui.componentes

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import org.junit.Rule
import org.junit.Test

class ListaEvidenciasTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testOrdenacionFavoritosPrimero() {
        // Datos de prueba con ID ascendente pero el segundo es favorito
        val grabaciones = listOf(
            GrabacionDato(id = 1, nombre = "Normal", rutaArchivo = "ruta1", fecha = "10/10", hash = "123", esFavorito = false),
            GrabacionDato(id = 2, nombre = "Favorito", rutaArchivo = "ruta2", fecha = "11/10", hash = "456", esFavorito = true)
        )

        composeTestRule.setContent {
            ListaEvidencias(
                lista = grabaciones,
                onPlay = {},
                onRename = {},
                onShare = {},
                onSaveToDevice = {},
                onDelete = {},
                onToggleFavorite = {},
                onGeneratePDF = {}
            )
        }

        // Verificamos que ambos elementos están presentes
        composeTestRule.onNodeWithText("Favorito", substring = true).assertExists()
        composeTestRule.onNodeWithText("Normal", substring = true).assertExists()

        // Verificamos el orden visual: Favorito debería aparecer ANTES que Normal
        val nodos = composeTestRule.onAllNodes(hasText("Favorito", substring = true).or(hasText("Normal", substring = true)))
            .fetchSemanticsNodes()
        
        // El primer nodo encontrado por Compose (de arriba a abajo) debería ser el Favorito
        val textoPrimerNodo = nodos[0].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
        assert(textoPrimerNodo?.contains("Favorito") == true) { 
            "Se esperaba 'Favorito' en la primera posición pero se encontró '$textoPrimerNodo'" 
        }
    }

    @Test
    fun testOrdenacionIdDescendenteCuandoNoHayFavoritos() {
        // Dos grabaciones normales, debería ir primero la de mayor ID (más reciente)
        val grabaciones = listOf(
            GrabacionDato(id = 1, nombre = "Antigua", rutaArchivo = "ruta1", fecha = "10/10", hash = "123", esFavorito = false),
            GrabacionDato(id = 2, nombre = "Reciente", rutaArchivo = "ruta2", fecha = "11/10", hash = "456", esFavorito = false)
        )

        composeTestRule.setContent {
            ListaEvidencias(
                lista = grabaciones,
                onPlay = {},
                onRename = {},
                onShare = {},
                onSaveToDevice = {},
                onDelete = {},
                onToggleFavorite = {},
                onGeneratePDF = {}
            )
        }

        // Verificamos que ambos elementos están presentes
        composeTestRule.onNodeWithText("Reciente", substring = true).assertExists()
        composeTestRule.onNodeWithText("Antigua", substring = true).assertExists()

        // Verificamos el orden visual: Reciente (ID 2) debería aparecer ANTES que Antigua (ID 1)
        val nodos = composeTestRule.onAllNodes(hasText("Reciente", substring = true).or(hasText("Antigua", substring = true)))
            .fetchSemanticsNodes()
        
        val textoPrimerNodo = nodos[0].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
        assert(textoPrimerNodo?.contains("Reciente") == true) { 
            "Se esperaba 'Reciente' en la primera posición pero se encontró '$textoPrimerNodo'" 
        }
    }
}
