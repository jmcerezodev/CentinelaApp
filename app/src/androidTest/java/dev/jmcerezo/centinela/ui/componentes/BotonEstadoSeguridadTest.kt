package dev.jmcerezo.centinela.ui.componentes

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BotonEstadoSeguridadTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        // Aseguramos que los permisos estén en un estado conocido si es posible
        // Pero para este test, simplemente verificaremos que el componente se renderiza 
        // y muestra uno de los dos estados válidos.
    }

    @Test
    fun testBotonMuestraEstadoDePermisos() {
        composeTestRule.setContent {
            BotonEstadoSeguridad(
                onSolicitarConsentimiento = {},
                onSolicitarDesactivacion = {}
            )
        }

        // Verificamos que al menos uno de los dos textos posibles aparece
        val nodoCompletos = composeTestRule.onNodeWithText("Estado de Permisos: Completos")
        val nodoIncompletos = composeTestRule.onNodeWithText("Estado de Permisos: Incompletos")

        // El test es exitoso si el componente es capaz de determinar su estado
        try {
            nodoCompletos.assertIsDisplayed()
        } catch (e: AssertionError) {
            nodoIncompletos.assertIsDisplayed()
        }
    }
}
