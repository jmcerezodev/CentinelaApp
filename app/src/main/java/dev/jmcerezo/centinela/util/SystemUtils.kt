package dev.jmcerezo.centinela.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Utilidades de sistema para gestionar permisos y estados del dispositivo.
 */
object SystemUtils {

    /**
     * Comprueba si un servicio de accesibilidad específico está activado en los ajustes del sistema.
     * 
     * @param context Contexto de la aplicación.
     * @param service Clase del servicio que se desea comprobar.
     * @return True si el servicio está habilitado y activo.
     */
    fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { 
            it.resolveInfo.serviceInfo.packageName == context.packageName && 
            it.resolveInfo.serviceInfo.name == service.name 
        }
    }

    /**
     * Abre la pantalla de información de la aplicación en los ajustes del sistema.
     * Útil para que el usuario gestione permisos de forma manual.
     */
    fun abrirAjustesApp(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
