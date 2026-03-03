package dev.jmcerezo.centinela.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import android.util.Log

/**
 * Gestor de autenticación biométrica profesional.
 */
object BiometricHelper {

    /**
     * Comprueba si el dispositivo es capaz de realizar autenticación biométrica.
     */
    fun esBiometriaDisponible(contexto: Context): Boolean {
        val manager = BiometricManager.from(contexto)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Busca la FragmentActivity recorriendo los ContextWrappers.
     * Vital para evitar crashes en Compose al usar BiometricPrompt.
     */
    fun obtenerActividad(contexto: Context): FragmentActivity? {
        var context = contexto
        while (context is ContextWrapper) {
            if (context is FragmentActivity) return context
            context = context.baseContext
        }
        return null
    }

    /**
     * Lanza el diálogo de autenticación de forma segura.
     */
    fun autenticar(
        contexto: Context,
        titulo: String = "Confirmar Identidad",
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        val actividad = obtenerActividad(contexto)
        if (actividad == null) {
            Log.e("BiometricHelper", "No se pudo encontrar la FragmentActivity")
            onError("Error de sistema: Actividad no encontrada")
            return
        }

        val executor = ContextCompat.getMainExecutor(actividad)
        val biometricPrompt = BiometricPrompt(actividad, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d("BiometricHelper", "Error/Cancelado: $errString")
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onExito()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Autenticación fallida")
                }
            })

        val infoPrompt = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle("Se requiere autenticación para continuar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            biometricPrompt.authenticate(infoPrompt)
        } catch (e: Exception) {
            Log.e("BiometricHelper", "Error al iniciar BiometricPrompt: ${e.message}")
            onError("Error al iniciar autenticación")
        }
    }
}
