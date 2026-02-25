package dev.jmcerezo.centinela.data.local.prefs

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferenciasTest {

    private lateinit var prefs: Preferencias

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Limpiamos las preferencias antes de cada test para que sean independientes
        context.getSharedPreferences("centinela_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        prefs = Preferencias(context)
    }

    @Test
    fun testValoresPorDefectoSonCorrectos() {
        assertFalse(prefs.servicioPermanente)
        assertFalse(prefs.modoSilencioso)
        assertTrue(prefs.botonesHabilitados)
        assertTrue(prefs.seguridadBiometrica)
    }

    @Test
    fun testGuardarYRecuperarServicioPermanente() {
        prefs.servicioPermanente = true
        assertTrue(prefs.servicioPermanente)
        
        prefs.servicioPermanente = false
        assertFalse(prefs.servicioPermanente)
    }

    @Test
    fun testGuardarYRecuperarModoSilencioso() {
        prefs.modoSilencioso = true
        assertTrue(prefs.modoSilencioso)
    }

    @Test
    fun testGuardarYRecuperarBotonesHabilitados() {
        prefs.botonesHabilitados = false
        assertFalse(prefs.botonesHabilitados)
    }
}
