package dev.jmcerezo.centinela.ui.componentes

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TarjetaGrabacionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var motor: GrabadoraMotor

    @Before
    fun setup() {
        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        motor = GrabadoraMotor.getInstance(contexto)
        
        // 1. Conceder permisos para que el botón REC pueda iniciar el motor sin diálogos del sistema
        val packageName = contexto.packageName
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.RECORD_AUDIO}")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.ACCESS_FINE_LOCATION}")

        // 2. Limpieza total del Singleton
        motor.resetEstadoInterno()
        Thread.sleep(500)
    }

    @Test
    fun testCambioDeEstadoAlPulsarRec() {
        composeTestRule.setContent {
            TarjetaGrabacion(gestorAudio = motor, alVerArchivos = {})
        }

        // Verificar estado inicial
        composeTestRule.onNodeWithText("REC").assertIsDisplayed()

        // Acción
        composeTestRule.onNodeWithText("REC").performClick()

        // Espera asíncrona robusta al cambio de estado visual
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("STOP").assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        motor.resetEstadoInterno()
    }
}
