package dev.jmcerezo.grabadoralegal

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.jmcerezo.grabadoralegal.core.GrabadoraMotor
import dev.jmcerezo.grabadoralegal.ui.PantallaGrabacion
import dev.jmcerezo.grabadoralegal.ui.PantallaListaArchivos

class MainActivity : ComponentActivity() {

    private val solicitudPermisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val microConcedido = resultados[Manifest.permission.RECORD_AUDIO] ?: false
        if (!microConcedido) {
            Toast.makeText(this, "Se necesita el micrófono para grabar", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        solicitudPermisosLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            val contexto = LocalContext.current
            val motor = remember { GrabadoraMotor(contexto) }

            // Estado para forzar el refresco de la lista
            var refreshTrigger by remember { mutableIntStateOf(0) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // 1. ZONA SUPERIOR: Panel de Control (Grabadora)
                Box(modifier = Modifier.wrapContentHeight()) {
                    PantallaGrabacion(
                        gestorAudio = motor,
                        alVerArchivos = {
                            // Incrementamos el disparador para avisar a la lista
                            refreshTrigger++
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. ZONA INFERIOR: Lista de Historial
                // Usamos key(refreshTrigger) para forzar la recomposición cuando cambie
                key(refreshTrigger) {
                    Box(modifier = Modifier.weight(1f)) {
                        PantallaListaArchivos(
                            gestorAudio = motor,
                            alVolver = { /* Ya estamos en la misma pantalla */ }
                        )
                    }
                }
            }
        }
    }
}