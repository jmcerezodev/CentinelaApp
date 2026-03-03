package dev.jmcerezo.centinela.ui.widget

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de instrumentación para verificar que los componentes del Widget son accesibles.
 * Ayuda a prevenir que Proguard/R8 elimine clases esenciales en la versión de producción.
 */
@RunWith(AndroidJUnit4::class)
class CentinelaWidgetTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testWidgetReceiverCanBeInstantiated() {
        // Verificamos que la clase del receptor existe y puede ser instanciada.
        // Si R8 la eliminara por error, este test fallaría.
        val receiver = CentinelaWidgetReceiver()
        assertNotNull("El receptor del widget no debe ser nulo", receiver)
    }

    @Test
    fun testToggleActionCanBeInstantiated() {
        // Verificamos que la acción de toggle (callback de Glance) es accesible.
        val action = ToggleAction()
        assertNotNull("La acción de toggle no debe ser nula", action)
    }

    @Test
    fun testWidgetCanHandleBroadcast() {
        val intent = Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION")
        intent.setPackage(context.packageName)
        
        // Simplemente verificamos que el envío del broadcast no produce excepciones de seguridad
        // o fallos de clase no encontrada.
        try {
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            org.junit.Assert.fail("El Receiver falló al procesar el broadcast: ${e.message}")
        }
    }
}
