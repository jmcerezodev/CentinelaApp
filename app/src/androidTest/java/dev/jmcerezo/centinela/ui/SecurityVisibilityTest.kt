package dev.jmcerezo.centinela.ui

import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de instrumentación para blindar la protección de visibilidad.
 * Verifica que el flag SECURE se gestione correctamente según los ajustes.
 */
@RunWith(AndroidJUnit4::class)
class SecurityVisibilityTest {

    private lateinit var prefs: Preferencias

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = Preferencias(context)
        // Forzamos un estado limpio de preferencias
        context.getSharedPreferences("centinela_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testFlagSecureSeAplicaAlInicioSiBiometriaEstaActiva() {
        // 1. Configuramos biometría activa
        prefs.seguridadBiometrica = true

        // 2. Lanzamos la actividad
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // 3. Verificamos que el contenido está bloqueado (porque aún no se ha autenticado)
                val flags = activity.window.attributes.flags
                assertTrue(
                    "La ventana debe tener FLAG_SECURE al inicio si la biometría está activa",
                    (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                )
            }
        }
    }

    @Test
    fun testFlagSecureNoSeAplicaSiBiometriaEstaDesactivada() {
        // 1. Configuramos biometría inactiva
        prefs.seguridadBiometrica = false

        // 2. Lanzamos la actividad
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // 3. Verificamos que el contenido es visible
                val flags = activity.window.attributes.flags
                assertFalse(
                    "La ventana NO debe tener FLAG_SECURE si la biometría está desactivada",
                    (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                )
            }
        }
    }
}
