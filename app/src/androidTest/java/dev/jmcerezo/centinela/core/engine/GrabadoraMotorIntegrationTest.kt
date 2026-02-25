package dev.jmcerezo.centinela.core.engine

import android.Manifest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrabadoraMotorIntegrationTest {

    private lateinit var motor: GrabadoraMotor

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        motor = GrabadoraMotor.getInstance(context)
        
        // 1. Conceder permisos necesarios para que el hardware no bloquee el inicio
        val packageName = context.packageName
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.RECORD_AUDIO}")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.ACCESS_FINE_LOCATION}")

        // 2. Limpieza total usando el nuevo método de reset
        motor.resetEstadoInterno()
        
        // 3. Pequeña espera para asegurar que el sistema procesa el reset
        Thread.sleep(500)
    }

    @Test
    fun testTresPulsacionesInicianLaGrabacion() {
        assertFalse("Debería empezar parado", motor.estaGrabando)

        // Pulsaciones rápidas (300ms > 150ms de filtro)
        motor.registrarPulsacion()
        Thread.sleep(300)
        motor.registrarPulsacion()
        Thread.sleep(300)
        motor.registrarPulsacion()

        assertTrue("El motor debería haber iniciado la grabación", motor.estaGrabando)
        
        motor.detenerGrabacion()
    }

    @Test
    fun testPulsacionesLentasNoInicianGrabacion() {
        motor.registrarPulsacion()
        Thread.sleep(1200) // Reset contador por tiempo (>1000ms)
        motor.registrarPulsacion()
        Thread.sleep(300)
        motor.registrarPulsacion()

        assertFalse("No debería haber iniciado", motor.estaGrabando)
    }
}
