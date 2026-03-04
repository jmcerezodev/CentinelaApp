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
        // Aseguramos que el motor esté detenido antes de empezar
        if (motor.estaGrabando) {
            motor.detenerGrabacion()
        }
        motor.resetEstadoInterno()
        
        val packageName = contexto.packageName
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.RECORD_AUDIO}")
        uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.ACCESS_FINE_LOCATION}")
        
        Thread.sleep(1000)
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

        // 1. Verificar estado inicial: Debe mostrar REC
        composeTestRule.onNodeWithText("REC").assertIsDisplayed()

        // 2. Iniciar grabación
        composeTestRule.onNodeWithText("REC").performClick()

        // 3. Esperar con timeout extendido a que el estado cambie a STOP
        // Aumentamos a 10 segundos por lentitud del emulador/dispositivo en tests
        composeTestRule.waitUntil(10000) {
            try {
                composeTestRule.onNodeWithText("STOP").assertIsDisplayed()
                true
            } catch (e: Throwable) {
                false
            }
        }
        
        // Limpieza final
        if (motor.estaGrabando) {
            motor.detenerGrabacion()
        }
    }
}
