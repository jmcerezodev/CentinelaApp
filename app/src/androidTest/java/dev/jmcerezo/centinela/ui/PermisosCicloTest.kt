package dev.jmcerezo.centinela.ui

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de instrumentación para verificar el flujo de permisos y diálogos de consentimiento.
 * Diseñado para ser robusto ante la presencia o ausencia de permisos en el dispositivo.
 */
@RunWith(AndroidJUnit4::class)
class PermisosCicloTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Limpiamos preferencias para evitar bloqueos por biometría durante el test
        context.getSharedPreferences("centinela_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
            
        // Reset motor
        GrabadoraMotor.getInstance(context).resetEstadoInterno()
        
        Thread.sleep(500)
    }

    @Test
    fun testFlujoBotonRec() {
        // 1. Verificamos que el botón REC existe
        val recButton = composeTestRule.onNodeWithText("REC")
        recButton.assertIsDisplayed()

        // 2. Ejecutamos la acción
        recButton.performClick()

        // 3. Verificamos que la app ha reaccionado.
        // Puede ocurrir dos cosas según el estado del dispositivo:
        // A. Se muestra el diálogo de consentimiento (porque faltan permisos)
        // B. Se inicia la grabación (porque ya tiene permisos)
        
        val esVisibleDialogo = try {
            composeTestRule.onNodeWithText("SÍ, CONTINUAR").assertIsDisplayed()
            true
        } catch (e: AssertionError) {
            false
        }

        val esVisibleGrabando = try {
            composeTestRule.onNodeWithText("STOP").assertIsDisplayed()
            true
        } catch (e: AssertionError) {
            false
        }

        // El test es exitoso si cualquiera de las dos reacciones válidas ocurre
        assert(esVisibleDialogo || esVisibleGrabando) {
            "Al pulsar REC debe aparecer el diálogo de consentimiento o iniciarse la grabación"
        }
    }
}
