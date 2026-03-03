package dev.jmcerezo.centinela.ui.componentes

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import dev.jmcerezo.centinela.ui.theme.CentinelaTheme

class AjusteInterruptorConInfoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBotonActivarSeMuestraCuandoEstaDeshabilitado() {
        var toggleLlamado = false
        
        composeTestRule.setContent {
            CentinelaTheme {
                AjusteInterruptorConInfo(
                    titulo = "Test Ajuste",
                    subtitulo = "Requiere permiso",
                    activo = false,
                    habilitado = false,
                    onInfo = {},
                    onToggle = { toggleLlamado = true }
                )
            }
        }

        // Verificar que el texto de aviso está en rojo/visible
        composeTestRule.onNodeWithText("Requiere permiso").assertIsDisplayed()
        
        // Verificar que el botón ACTIVAR existe y responde
        composeTestRule.onNodeWithText("ACTIVAR").assertIsDisplayed().performClick()
        
        // Verificar que al pulsar ACTIVAR se llama al callback (que lanzará el permiso)
        assert(toggleLlamado)
    }

    @Test
    fun testBotonInfoSiempreFunciona() {
        var infoLlamado = false
        
        composeTestRule.setContent {
            CentinelaTheme {
                AjusteInterruptorConInfo(
                    titulo = "Test Info",
                    subtitulo = "Cualquier estado",
                    activo = false,
                    habilitado = false, // Probamos en estado bloqueado
                    onInfo = { infoLlamado = true },
                    onToggle = {}
                )
            }
        }

        // Verificar que el botón de información es clicable incluso deshabilitado
        composeTestRule.onNodeWithContentDescription("Información").performClick()
        assert(infoLlamado)
    }
}
