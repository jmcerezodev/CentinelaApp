package dev.jmcerezo.grabadoralegal.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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

        // --- CONFIGURACIÓN BLINDADA PARA COMPILAR SIN ERRORES ---
        val info = serviceInfo ?: AccessibilityServiceInfo()

        info.apply {
            // Usamos la constante directa para evitar errores de referencia
            eventTypes = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC
            notificationTimeout = 100

            // Flags indispensables para el S23 Ultra
            flags = flags or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }

        this.serviceInfo = info
        // -------------------------------------------------------

        Log.d("Centinela", "Servicio de Accesibilidad Conectado y Configurado")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode

        // Escuchamos el botón de Volumen Arriba (KEYCODE_VOLUME_UP)
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
            }

            // Bloqueamos el evento para que no salga la UI de volumen
            return true
        }

        return super.onKeyEvent(event)
    }

    private fun gestionarGrabacion() {
        try {
            if (motor.estaGrabando) {
                motor.detenerGrabacion()
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