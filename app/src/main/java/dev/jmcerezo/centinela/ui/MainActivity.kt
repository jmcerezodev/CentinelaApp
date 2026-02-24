package dev.jmcerezo.centinela.ui

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import dev.jmcerezo.centinela.ui.componentes.*
import dev.jmcerezo.centinela.ui.componentes.dialogos.*
import dev.jmcerezo.centinela.ui.theme.CentinelaTheme

/**
 * Actividad principal de la aplicación Centinela.
 * Gestiona la solicitud de permisos críticos y sirve como contenedor para la interfaz de Compose.
 */
class MainActivity : ComponentActivity() {

    // Launcher para solicitar los permisos necesarios para la captura legal
    private val solicitudPermisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val microConcedido = resultados[Manifest.permission.RECORD_AUDIO] ?: false
        val gpsConcedido = resultados[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (!microConcedido || !gpsConcedido) {
            Toast.makeText(this, "Se requieren permisos de micro y GPS para la validez legal", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Configuración de pantalla completa y barras transparentes
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Lanzamos la solicitud de permisos al arrancar
        solicitudPermisosLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            CentinelaTheme {
                val contexto = LocalContext.current
                val motor = remember { GrabadoraMotor.getInstance(contexto) }
                val viewModel: GrabacionViewModel = viewModel()

                // Observamos la lista de grabaciones desde Room
                val listaGrabaciones by viewModel.todasLasGrabaciones.collectAsState()
                
                // Estados para la gestión de diálogos
                var mostrarInfoTecnica by remember { mutableStateOf(false) }
                var archivoParaEliminar by remember { mutableStateOf<GrabacionDato?>(null) }
                var archivoParaRenombrar by remember { mutableStateOf<GrabacionDato?>(null) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color(0xFF0F111A))
                        .navigationBarsPadding()
                        .statusBarsPadding()
                ) {
                    // Cabecera con estado de permisos e info técnica
                    TopBarApp(onInfoClick = { mostrarInfoTecnica = true })

                    // Control de grabación y ajustes avanzados
                    Box(modifier = Modifier.wrapContentHeight()) {
                        TarjetaGrabacion(
                            gestorAudio = motor,
                            alVerArchivos = { /* Room refresca automáticamente vía Flow */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Título de la sección de historial
                    TopBarHistorial()

                    // Lista reactiva de evidencias
                    Box(modifier = Modifier.weight(1f)) {
                        ListaEvidencias(
                            lista = listaGrabaciones,
                            onPlay = { grabacion ->
                                motor.reproducirAudio(grabacion)
                                Toast.makeText(contexto, "Reproduciendo...", Toast.LENGTH_SHORT).show()
                            },
                            onRename = { grabacion -> archivoParaRenombrar = grabacion },
                            onShare = { grabacion -> motor.compartirArchivo(grabacion) },
                            onDelete = { grabacion -> archivoParaEliminar = grabacion },
                            onToggleFavorite = { grabacion ->
                                viewModel.actualizar(grabacion.copy(esFavorito = !grabacion.esFavorito))
                            }
                        )
                    }

                    // Pie de página con créditos
                    FooterApp()
                }

                // GESTIÓN DINÁMICA DE DIÁLOGOS
                if (mostrarInfoTecnica) {
                    DialogoInfoTecnica(onDismiss = { mostrarInfoTecnica = false })
                }

                archivoParaEliminar?.let { grabacion ->
                    DialogoEliminar(
                        nombreArchivo = grabacion.nombre,
                        onConfirm = {
                            motor.eliminarGrabacion(grabacion)
                            archivoParaEliminar = null
                        },
                        onDismiss = { archivoParaEliminar = null }
                    )
                }

                archivoParaRenombrar?.let { grabacion ->
                    DialogoRenombrar(
                        nombreActual = grabacion.nombre,
                        onConfirm = { nuevoNombre ->
                            motor.renombrarGrabacion(grabacion, nuevoNombre)
                            archivoParaRenombrar = null
                        },
                        onDismiss = { archivoParaRenombrar = null }
                    )
                }
            }
        }
    }
}
