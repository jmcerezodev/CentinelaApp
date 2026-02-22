package dev.jmcerezo.grabadoralegal.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class ServicioBotones : AccessibilityService() {

    private lateinit var motor: GrabadoraMotor
    private var contadorPulsaciones = 0
    private var ultimaPulsacion: Long = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        motor = GrabadoraMotor(this)

        // --- CONFIGURACIÓN MINIMISTA PARA EVITAR BLOQUEOS DE SISTEMA ---
        val info = serviceInfo ?: AccessibilityServiceInfo()

        info.apply {
            eventTypes = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC
            notificationTimeout = 100

            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        this.serviceInfo = info
        Log.d("Centinela", "Servicio de Accesibilidad Conectado y Configurado")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode

        if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val tiempoActual = System.currentTimeMillis()

            if (tiempoActual - ultimaPulsacion < 1500) {
                contadorPulsaciones++
            } else {
                contadorPulsaciones = 1
            }

            ultimaPulsacion = tiempoActual
            Log.d("Centinela", "Pulsación detectada: $contadorPulsaciones")

            if (contadorPulsaciones == 3) {
                Log.d("Centinela", "¡Disparando grabación!")
                gestionarGrabacion()
                contadorPulsaciones = 0
                return true
            }
        }
        return false
    }

    private fun gestionarGrabacion() {
        try {
            if (motor.estaGrabando) {
                motor.detenerGrabacion()

                // --- CAMBIO PARA BLINDAR EL REFRESCO ---
                // Usamos el nombre de acción único y especificamos el paquete
                val intent = Intent("dev.jmcerezo.ACTUALIZAR_LISTA")
                intent.setPackage(packageName)
                sendBroadcast(intent)
                Log.d("Centinela", "Aviso de refresco enviado: dev.jmcerezo.ACTUALIZAR_LISTA")
                // ---------------------------------------

            } else {
                motor.iniciarGrabacion()
            }
        } catch (e: Exception) {
            Log.e("Centinela", "Error al gestionar grabación: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}