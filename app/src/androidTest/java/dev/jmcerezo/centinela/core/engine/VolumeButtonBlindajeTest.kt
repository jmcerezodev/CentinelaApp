package dev.jmcerezo.centinela.core.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TEST DE BLINDAJE CRÍTICO: Grabación mediante botones físicos.
 * 
 * Verifica que 3 pulsaciones de volumen arriba inicien la grabación
 * y otras 3 pulsaciones la detengan correctamente.
 */
@RunWith(AndroidJUnit4::class)
class VolumeButtonBlindajeTest {

    private lateinit var motor: GrabadoraMotor

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        motor = GrabadoraMotor.getInstance(context)
        
        // Limpiamos el estado para que el test sea determinista
        motor.resetEstadoInterno()
    }

    @Test
    fun testCicloCompletoGrabacionPorBotones() {
        // --- FASE 1: INICIAR GRABACIÓN ---
        assertFalse("El motor debe estar detenido al inicio", motor.estaGrabando)

        // Simulamos 3 pulsaciones con intervalo de 300ms (humano rápido)
        motor.registrarPulsacion()
        Thread.sleep(300)
        motor.registrarPulsacion()
        Thread.sleep(300)
        motor.registrarPulsacion()

        assertTrue("El motor DEBERÍA haber iniciado la grabación tras 3 pulsaciones", motor.estaGrabando)

        // Esperamos un poco para simular una grabación real y evitar el filtro de "acción exitosa" (2000ms)
        Thread.sleep(2500)

        // --- FASE 2: DETENER GRABACIÓN ---
        // Simulamos otras 3 pulsaciones
        motor.registrarPulsacion()
        Thread.sleep(300)
        motor.registrarPulsacion()
        Thread.sleep(300)
        motor.registrarPulsacion()

        assertFalse("El motor DEBERÍA haberse detenido tras otras 3 pulsaciones", motor.estaGrabando)
    }

    @Test
    fun testFiltroReboteEvitaFalsosPositivos() {
        // Simulamos pulsaciones extremadamente rápidas (rebotes de hardware < 150ms)
        motor.registrarPulsacion()
        Thread.sleep(50) 
        motor.registrarPulsacion()
        Thread.sleep(50)
        motor.registrarPulsacion()

        assertFalse("El motor NO debe iniciar si las pulsaciones son demasiado rápidas (filtro de rebote)", motor.estaGrabando)
    }
}
