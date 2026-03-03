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
    private val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setup() {
        motor = GrabadoraMotor.getInstance(contexto)
        motor.resetEstadoInterno()
        
        // Otorgamos permisos de forma estable al inicio
        val packageName = contexto.packageName
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.RECORD_AUDIO}")
        uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.ACCESS_FINE_LOCATION}")
        
        // Pausa generosa para que el S23 procese los permisos
        Thread.sleep(1500)
    }

    @Test
    fun testCambioDeEstadoAlPulsarRec() {
        composeTestRule.setContent {
            TarjetaGrabacion(
                gestorAudio = motor, 
                alVerArchivos = {}, 
                onSolicitarConsentimiento = {}
            )
        }

        // 1. Verificar estado inicial
        composeTestRule.onNodeWithText("REC").assertIsDisplayed()

        // 2. Iniciar grabación
        composeTestRule.onNodeWithText("REC").performClick()

        // 3. Esperar a que el motor cambie el estado visual a STOP
        composeTestRule.waitUntil(8000) {
            try {
                composeTestRule.onNodeWithText("STOP").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // Limpieza
        motor.resetEstadoInterno()
    }
}
