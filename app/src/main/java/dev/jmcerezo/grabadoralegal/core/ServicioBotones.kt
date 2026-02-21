package dev.jmcerezo.grabadoralegal.core

import android.accessibilityservice.AccessibilityService
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
        Log.d("Centinela", "Servicio de Accesibilidad Conectado")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode

        // Capturamos el evento de bajar el dedo sobre el botón
        if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val tiempoActual = System.currentTimeMillis()

            // Aumentamos a 1500ms (1.5 segundos) para que sea humano el ritmo de pulsación
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
        }

        // Importante: Devolvemos super para que el sistema no crea que hemos bloqueado el evento
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