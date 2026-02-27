package dev.jmcerezo.centinela.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import dev.jmcerezo.centinela.util.SystemUtils

/**
 * Actividad principal de la aplicación Centinela.
 * Gestiona la autenticación biométrica y la visualización de avisos destacados de permisos.
 */
class MainActivity : AppCompatActivity() {

    private val permisoWidgetState = mutableStateOf<String?>(null)

    private val solicitudPermisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val microConcedido = resultados[Manifest.permission.RECORD_AUDIO] ?: false
        val gpsConcedido = resultados[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (!microConcedido || !gpsConcedido) {
            Toast.makeText(this, "Se requieren permisos de micro y GPS", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        permisoWidgetState.value = intent.getStringExtra("SOLICITAR_PERMISO")

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
                
                var consentimientoDestacado by remember { mutableStateOf<PermisoConsentimiento?>(null) }
                var mostrarSugerenciaBiometria by remember { 
                    mutableStateOf(!prefs.biometriaPreguntada && BiometricHelper.esBiometriaDisponible(contexto)) 
                }

                // Sincronización con el Widget
                val permisoDelWidget by permisoWidgetState
                LaunchedEffect(permisoDelWidget, estaAutenticado) {
                    if (estaAutenticado && permisoDelWidget != null) {
                        consentimientoDestacado = when (permisoDelWidget) {
                            "MICROFONO" -> PermisoConsentimiento.Microfono
                            "ACCESIBILIDAD" -> PermisoConsentimiento.Accesibilidad
                            "NOTIFICACIONES" -> PermisoConsentimiento.Notificaciones
                            "BATERIA" -> PermisoConsentimiento.Bateria
                            else -> null
                        }
                        permisoWidgetState.value = null
                    }
                }

                // GESTIÓN DE CICLO DE VIDA (Seguridad)
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_START) {
                            if (prefs.seguridadBiometrica && BiometricHelper.esBiometriaDisponible(contexto)) {
                                BiometricHelper.autenticar(
                                    actividad = this@MainActivity,
                                    onExito = { estaAutenticado = true },
                                    onError = { error ->
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

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color(0xFF0F111A))
                            .navigationBarsPadding()
                            .statusBarsPadding()
                    ) {
                        TopBarApp(
                            onInfoClick = { mostrarInfoTecnica = true },
                            onSolicitarConsentimiento = { tipo -> consentimientoDestacado = tipo }
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

                    // --- DIÁLOGOS CENTRALIZADOS ---
                    
                    consentimientoDestacado?.let { consentimiento ->
                        val micL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                        val locL = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
                        val notifL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

                        DialogoConsentimientoDestacado(
                            consentimiento = consentimiento,
                            onConfirm = {
                                consentimientoDestacado = null
                                when (consentimiento) {
                                    PermisoConsentimiento.Accesibilidad -> SystemUtils.abrirAjustesAccesibilidad(contexto)
                                    PermisoConsentimiento.Microfono -> micL.launch(Manifest.permission.RECORD_AUDIO)
                                    PermisoConsentimiento.Ubicacion -> locL.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                                    PermisoConsentimiento.Notificaciones -> if (android.os.Build.VERSION.SDK_INT >= 33) notifL.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    PermisoConsentimiento.Superposicion -> SystemUtils.abrirAjustesSuperposicion(contexto)
                                    PermisoConsentimiento.Bateria -> SystemUtils.abrirAjustesBateria(contexto)
                                    PermisoConsentimiento.Biometria -> {
                                        prefs.seguridadBiometrica = true
                                        prefs.biometriaPreguntada = true
                                        contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName))
                                    }
                                }
                            },
                            onDismiss = { consentimientoDestacado = null }
                        )
                    }

                    if (mostrarSugerenciaBiometria) {
                        DialogoSugerenciaBiometria(
                            onConfirm = {
                                prefs.seguridadBiometrica = true
                                prefs.biometriaPreguntada = true
                                estaAutenticado = true
                                mostrarSugerenciaBiometria = false
                                contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName))
                            },
                            onDismiss = { 
                                prefs.biometriaPreguntada = true
                                mostrarSugerenciaBiometria = false 
                                contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName))
                            }
                        )
                    }

                    if (mostrarInfoTecnica) DialogoInfoTecnica(onDismiss = { mostrarInfoTecnica = false })
                    archivoParaEliminar?.let { grab ->
                        DialogoEliminar(nombreArchivo = grab.nombre, onConfirm = { motor.eliminarGrabacion(grab); archivoParaEliminar = null }, onDismiss = { archivoParaEliminar = null })
                    }
                    archivoParaRenombrar?.let { grab ->
                        DialogoRenombrar(nombreActual = grab.nombre, onConfirm = { nuevo -> motor.renombrarGrabacion(grab, nuevo); archivoParaRenombrar = null }, onDismiss = { archivoParaRenombrar = null })
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0F111A)))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val permiso = intent.getStringExtra("SOLICITAR_PERMISO")
        if (permiso != null) {
            permisoWidgetState.value = permiso
        }
    }
}
