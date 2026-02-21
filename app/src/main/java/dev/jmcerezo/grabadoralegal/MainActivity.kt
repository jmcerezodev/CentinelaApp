package dev.jmcerezo.grabadoralegal

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import dev.jmcerezo.grabadoralegal.core.GrabadoraMotor
import dev.jmcerezo.grabadoralegal.ui.PantallaGrabacion
import dev.jmcerezo.grabadoralegal.ui.PantallaListaArchivos

class MainActivity : ComponentActivity() {

    // 1. Definimos el lanzador de permisos fuera del onCreate
    private val solicitudPermisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val microConcedido = resultados[Manifest.permission.RECORD_AUDIO] ?: false
        val ubicacionConcedida = resultados[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (!microConcedido) {
            Toast.makeText(this, "Se necesita el micrófono para grabar", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Pedimos los permisos nada más arrancar la app
        solicitudPermisosLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            var pantallaActual by remember { mutableStateOf("grabacion") }
            val contexto = LocalContext.current

            // Instanciamos el motor de grabación
            val motor = remember { GrabadoraMotor(contexto) }

            if (pantallaActual == "grabacion") {
                PantallaGrabacion(
                    gestorAudio = motor,
                    alVerArchivos = { pantallaActual = "lista" }
                )
            } else {
                PantallaListaArchivos(
                    gestorAudio = motor,
                    alVolver = { pantallaActual = "grabacion" }
                )
            }
        }
    }
}