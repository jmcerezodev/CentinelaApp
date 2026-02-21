package dev.jmcerezo.grabadoralegal

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background // ESTA ES LA IMPORTACIÓN QUE FALTABA
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        // Forzamos transparencia total y estilo oscuro para iconos blancos
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
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
            var refreshTrigger by remember { mutableIntStateOf(0) }

            // Aplicamos el color de fondo aquí para que la barra transparente lo deje ver
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F111A)) // Corregido: Ahora se reconoce gracias al import
                    .navigationBarsPadding()
            ) {
                Box(modifier = Modifier.wrapContentHeight()) {
                    PantallaGrabacion(
                        gestorAudio = motor,
                        alVerArchivos = {
                            refreshTrigger++
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                key(refreshTrigger) {
                    Box(modifier = Modifier.weight(1f)) {
                        PantallaListaArchivos(
                            gestorAudio = motor,
                            alVolver = { }
                        )
                    }
                }
            }
        }
    }
}