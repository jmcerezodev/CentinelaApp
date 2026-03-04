package dev.jmcerezo.centinela.ui

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

@RunWith(AndroidJUnit4::class)
class PermisosCicloTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("centinela_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
            
        val motor = GrabadoraMotor.getInstance(context)
        if (motor.estaGrabando) motor.detenerGrabacion()
        motor.resetEstadoInterno()
        
        Thread.sleep(1000)
    }

    @Test
    fun testFlujoBotonRec() {
        // Aseguramos que el botón REC sea visible antes de interactuar
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("REC").assertIsDisplayed()
                true
            } catch (e: Throwable) {
                false
            }
        }

        composeTestRule.onNodeWithText("REC").performClick()

        // Esperamos a que aparezca cualquiera de las dos respuestas válidas
        composeTestRule.waitUntil(10000) {
            val esVisibleDialogo = try {
                composeTestRule.onNodeWithText("SÍ, CONTINUAR").assertIsDisplayed()
                true
            } catch (e: Throwable) {
                false
            }

            val esVisibleGrabando = try {
                composeTestRule.onNodeWithText("STOP").assertIsDisplayed()
                true
            } catch (e: Throwable) {
                false
            }

            esVisibleDialogo || esVisibleGrabando
        }
    }
}
