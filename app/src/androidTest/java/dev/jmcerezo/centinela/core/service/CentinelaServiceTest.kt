package dev.jmcerezo.centinela.core.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de instrumentación para blindar la lógica de notificaciones de CentinelaService.
 */
@RunWith(AndroidJUnit4::class)
class CentinelaServiceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var prefs: Preferencias
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        prefs = Preferencias(context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Limpiar estado
        context.getSharedPreferences("centinela_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testNotificacionUnificadaEnDiferentesEstados() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        // Caso 1: Solo Servicio Permanente
        prefs.servicioPermanente = true
        prefs.modoSilencioso = false
        
        val intent = Intent(context, CentinelaService::class.java)
        context.startService(intent)
        
        // Esperar a que el servicio se inicie y publique la notificación
        Thread.sleep(1000)
        
        var activeNotifications = notificationManager.activeNotifications
        var centinelaNotif = activeNotifications.find { it.id == 1001 }
        
        assertTrue("La notificación 1001 debe estar activa", centinelaNotif != null)
        assertEquals("Servicio Permanente activo", centinelaNotif?.notification?.extras?.getCharSequence("android.text"))

        // Caso 2: Ambos activos
        prefs.modoSilencioso = true
        context.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(context.packageName))
        
        Thread.sleep(1000)
        
        activeNotifications = notificationManager.activeNotifications
        centinelaNotif = activeNotifications.find { it.id == 1001 }
        
        assertEquals("Servicio Permanente y Anti-Suspensión activos", centinelaNotif?.notification?.extras?.getCharSequence("android.text"))

        // Limpieza
        context.stopService(intent)
    }

    @Test
    fun testNotificacionTienePendingIntent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        prefs.servicioPermanente = true
        context.startService(Intent(context, CentinelaService::class.java))
        
        Thread.sleep(1000)
        
        val centinelaNotif = notificationManager.activeNotifications.find { it.id == 1001 }
        
        assertTrue("La notificación debe tener un contentIntent para abrir la app", centinelaNotif?.notification?.contentIntent != null)
        
        context.stopService(Intent(context, CentinelaService::class.java))
    }
}
