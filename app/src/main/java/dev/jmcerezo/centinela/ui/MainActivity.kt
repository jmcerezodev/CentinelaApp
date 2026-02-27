package dev.jmcerezo.centinela.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import dev.jmcerezo.centinela.ui.componentes.*
import dev.jmcerezo.centinela.ui.componentes.dialogos.*
import dev.jmcerezo.centinela.ui.theme.CentinelaTheme
import dev.jmcerezo.centinela.util.BiometricHelper
import dev.jmcerezo.centinela.util.PdfReportGenerator

/**
 * Actividad principal de la aplicación Centinela.
 * Gestiona la autenticación biométrica obligatoria y las solicitudes de permisos desde el Widget.
 */
class MainActivity : AppCompatActivity() {

    // Estado para capturar solicitudes de permisos externas (Widget)
    private var permisoPendientePorWidget: String? = null

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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Capturamos si venimos del widget solicitando un permiso
        permisoPendientePorWidget = intent.getStringExtra("SOLICITAR_PERMISO")

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
                val prefs = remember { Preferencias(contexto) }
                val motor = remember { GrabadoraMotor.getInstance(contexto) }
                val viewModel: GrabacionViewModel = viewModel()
                val lifecycleOwner = LocalLifecycleOwner.current

                var estaAutenticado by remember { 
                    mutableStateOf(!prefs.seguridadBiometrica || !BiometricHelper.esBiometriaDisponible(contexto)) 
                }
                
                var mostrarSugerenciaBiometria by remember { 
                    mutableStateOf(!prefs.biometriaPreguntada && BiometricHelper.esBiometriaDisponible(contexto)) 
                }

                // NUEVO: Estado para mostrar el diálogo de permiso solicitado desde el Widget
                var permisoWidgetActual by remember { mutableStateOf(permisoPendientePorWidget) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_START) {
                            if (prefs.seguridadBiometrica && BiometricHelper.esBiometriaDisponible(contexto)) {
                                BiometricHelper.autenticar(
                                    actividad = this@MainActivity,
                                    onExito = { estaAutenticado = true },
                                    onError = { error ->
                                        Toast.makeText(this@MainActivity, "Acceso denegado", Toast.LENGTH_LONG).show()
                                        if (error.contains("cancel", true) || error.contains("atrás", true)) finish()
                                    }
                                )
                            } else {
                                estaAutenticado = true
                            }
                        } else if (event == Lifecycle.Event.ON_STOP) {
                            if (prefs.seguridadBiometrica) estaAutenticado = false
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (estaAutenticado) {
                    val listaGrabaciones by viewModel.todasLasGrabaciones.collectAsState()
                    var mostrarInfoTecnica by remember { mutableStateOf(false) }
                    var archivoParaEliminar by remember { mutableStateOf<GrabacionDato?>(null) }
                    var archivoParaRenombrar by remember { mutableStateOf<GrabacionDato?>(null) }

                    // Si venimos del widget, forzamos la apertura del panel de seguridad o ajustes
                    LaunchedEffect(permisoWidgetActual) {
                        if (permisoWidgetActual != null) {
                            // Al detectar que venimos del widget, mostramos el panel de seguridad
                            // TopBarApp ya gestionará el diálogo destacado según el estado de los permisos.
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color(0xFF0F111A))
                            .navigationBarsPadding()
                            .statusBarsPadding()
                    ) {
                        TopBarApp(
                            onInfoClick = { mostrarInfoTecnica = true },
                            permisoWidgetSolicitado = permisoWidgetActual,
                            onPermisoWidgetMostrado = { permisoWidgetActual = null }
                        )
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

                    if (mostrarSugerenciaBiometria) {
                        AlertDialog(
                            onDismissRequest = { },
                            properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
                            title = { Text("Reforzar Seguridad", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                            text = { 
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Para proteger tus evidencias legales, Centinela puede solicitar tu huella dactilar o reconocimiento facial cada vez que abras la aplicación.", color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp)
                                    Box(modifier = Modifier.background(androidx.compose.ui.graphics.Color(0xFF3D5AFE).copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                                        Text("Esto garantiza que solo tú puedas acceder a los archivos grabados, incluso si alguien más utiliza tu dispositivo.", color = androidx.compose.ui.graphics.Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
                                    }
                                    Text("Nota: Puedes cambiar esta configuración en cualquier momento desde los Ajustes Avanzados de la aplicación.", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    prefs.seguridadBiometrica = true
                                    prefs.biometriaPreguntada = true
                                    estaAutenticado = true
                                    mostrarSugerenciaBiometria = false
                                    contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName))
                                    Toast.makeText(contexto, "Seguridad activada", Toast.LENGTH_SHORT).show()
                                }, colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF3D5AFE)), shape = RoundedCornerShape(12.dp)) {
                                    Text("ACTIVAR PROTECCIÓN", color = androidx.compose.ui.graphics.Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { 
                                    prefs.biometriaPreguntada = true
                                    mostrarSugerenciaBiometria = false 
                                    contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName))
                                }) {
                                    Text("MANTENER DESACTIVADA", color = androidx.compose.ui.graphics.Color.Gray)
                                }
                            },
                            containerColor = androidx.compose.ui.graphics.Color(0xFF1A1D2E),
                            shape = RoundedCornerShape(28.dp)
                        )
                    }

                    if (mostrarInfoTecnica) DialogoInfoTecnica(onDismiss = { mostrarInfoTecnica = false })
                    archivoParaEliminar?.let { grabacion ->
                        DialogoEliminar(nombreArchivo = grabacion.nombre, onConfirm = { motor.eliminarGrabacion(grabacion); archivoParaEliminar = null }, onDismiss = { archivoParaEliminar = null })
                    }
                    archivoParaRenombrar?.let { grabacion ->
                        DialogoRenombrar(nombreActual = grabacion.nombre, onConfirm = { nuevoNombre -> motor.renombrarGrabacion(grabacion, nuevoNombre); archivoParaRenombrar = null }, onDismiss = { archivoParaRenombrar = null })
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0F111A)))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // También capturamos si la app ya estaba abierta y se pulsa el widget
        val permiso = intent.getStringExtra("SOLICITAR_PERMISO")
        if (permiso != null) {
            permisoPendientePorWidget = permiso
            // Esto disparará la recomposición en setContent si usamos un estado observable
        }
    }
}
