package dev.jmcerezo.centinela.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Utilidades de sistema para gestionar permisos y estados del dispositivo.
 */
object SystemUtils {

    /**
     * Comprueba si un servicio de accesibilidad específico está activado en los ajustes del sistema.
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
     */
    fun abrirAjustesApp(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Abre los ajustes de accesibilidad.
     */
    fun abrirAjustesAccesibilidad(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Abre los ajustes de superposición sobre otras apps.
     */
    fun abrirAjustesSuperposicion(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Abre los ajustes de optimización de batería.
     */
    fun abrirAjustesBateria(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
