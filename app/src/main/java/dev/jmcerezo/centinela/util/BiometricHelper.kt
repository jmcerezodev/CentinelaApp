package dev.jmcerezo.centinela.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

/**
 * Gestor de autenticación biométrica (Huella, Rostro, PIN).
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
     * Lanza el diálogo de autenticación del sistema.
     */
    fun autenticar(
        actividad: FragmentActivity,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(actividad)
        
        val biometricPrompt = BiometricPrompt(actividad, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
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
            .setTitle("Acceso a Centinela")
            .setSubtitle("Autentícate para ver tus evidencias")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(infoPrompt)
    }
}
