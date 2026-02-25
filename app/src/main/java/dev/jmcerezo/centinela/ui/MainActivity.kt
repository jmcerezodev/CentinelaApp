package dev.jmcerezo.centinela.ui

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import dev.jmcerezo.centinela.util.BiometricHelper
import dev.jmcerezo.centinela.util.PdfReportGenerator

/**
 * Actividad principal de la aplicación Centinela.
 * Gestiona la autenticación biométrica y la solicitud de permisos críticos.
 */
class MainActivity : AppCompatActivity() {

    // Launcher para solicitar los permisos necesarios para la captura legal
    private val solicitudPermisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val microConcedido = resultados[Manifest.permission.RECORD_AUDIO] ?: false
        val gpsConcedido = resultados[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (!microConcedido || !gpsConcedido) {
            Toast.makeText(this@MainActivity, "Se requieren permisos de micro y GPS", Toast.LENGTH_LONG).show()
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

                // Estado de autenticación para proteger la UI
                var estaAutenticado by remember { mutableStateOf(!BiometricHelper.esBiometriaDisponible(this@MainActivity)) }

                // Disparamos la autenticación al iniciar si está disponible
                LaunchedEffect(Unit) {
                    if (BiometricHelper.esBiometriaDisponible(this@MainActivity)) {
                        BiometricHelper.autenticar(
                            actividad = this@MainActivity,
                            onExito = { estaAutenticado = true },
                            onError = { error ->
                                Toast.makeText(this@MainActivity, "Acceso denegado: $error", Toast.LENGTH_LONG).show()
                                if (error.contains("cancel", true)) finish()
                            }
                        )
                    }
                }

                if (estaAutenticado) {
                    val listaGrabaciones by viewModel.todasLasGrabaciones.collectAsState()
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
                        TopBarApp(onInfoClick = { mostrarInfoTecnica = true })
                        Box(modifier = Modifier.wrapContentHeight()) {
                            TarjetaGrabacion(gestorAudio = motor, alVerArchivos = { })
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TopBarHistorial()
                        Box(modifier = Modifier.weight(1f)) {
                            ListaEvidencias(
                                lista = listaGrabaciones,
                                onPlay = { grabacion -> motor.reproducirAudio(grabacion) },
                                onRename = { grabacion -> archivoParaRenombrar = grabacion },
                                onShare = { grabacion -> motor.compartirArchivo(grabacion) },
                                onDelete = { grabacion -> archivoParaEliminar = grabacion },
                                onToggleFavorite = { grabacion -> viewModel.actualizar(grabacion.copy(esFavorito = !grabacion.esFavorito)) },
                                onGeneratePDF = { grabacion -> PdfReportGenerator.generarYCompartir(contexto, grabacion) }
                            )
                        }
                        FooterApp()
                    }

                    if (mostrarInfoTecnica) DialogoInfoTecnica(onDismiss = { mostrarInfoTecnica = false })
                    archivoParaEliminar?.let { grabacion ->
                        DialogoEliminar(
                            nombreArchivo = grabacion.nombre,
                            onConfirm = { motor.eliminarGrabacion(grabacion); archivoParaEliminar = null },
                            onDismiss = { archivoParaEliminar = null }
                        )
                    }
                    archivoParaRenombrar?.let { grabacion ->
                        DialogoRenombrar(
                            nombreActual = grabacion.nombre,
                            onConfirm = { nuevoNombre -> motor.renombrarGrabacion(grabacion, nuevoNombre); archivoParaRenombrar = null },
                            onDismiss = { archivoParaRenombrar = null }
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0F111A)))
                }
            }
        }
    }
}
