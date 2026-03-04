package dev.jmcerezo.centinela.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlinx.coroutines.delay

/**
 * Actividad principal de la aplicación Centinela.
 * Gestiona la autenticación biométrica y la visualización de avisos destacados de permisos.
 */
class MainActivity : AppCompatActivity() {

    // Estado reactivo para capturar solicitudes externas del Widget
    private val permisoWidgetState = mutableStateOf<String?>(null)

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Capturamos el permiso solicitado si la app se inicia desde el Widget
        actualizarPeticionWidget(intent)

        setContent {
            CentinelaTheme {
                val contexto = LocalContext.current
                val prefs = remember { Preferencias(contexto) }
                val motor = remember { GrabadoraMotor.getInstance(contexto) }
                val viewModel: GrabacionViewModel = viewModel()
                val lifecycleOwner = LocalLifecycleOwner.current

                // Estado de autenticación
                var estaAutenticado by remember { 
                    mutableStateOf(!prefs.seguridadBiometrica || !BiometricHelper.esBiometriaDisponible(contexto)) 
                }
                
                // Estados de Diálogos
                var consentimientoDestacado by remember { mutableStateOf<PermisoConsentimiento?>(null) }
                var consentimientoParaDesactivar by remember { mutableStateOf<PermisoConsentimiento?>(null) }
                var mostrarSugerenciaBiometria by remember { 
                    mutableStateOf(!prefs.biometriaPreguntada && BiometricHelper.esBiometriaDisponible(contexto)) 
                }

                // Control de flujo secuencial
                var esFlujoSecuencial by remember { mutableStateOf(false) }

                // Lanzadores de permisos
                val micL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
                    if (concedido) {
                        if (esFlujoSecuencial && ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            consentimientoDestacado = PermisoConsentimiento.Ubicacion
                        }
                    } else {
                        Toast.makeText(contexto, "Se requiere permiso de micro", Toast.LENGTH_SHORT).show()
                    }
                    esFlujoSecuencial = false
                }

                val gpsL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
                    if (!concedido) {
                        Toast.makeText(contexto, "Se requiere permiso de GPS para evidencias legales", Toast.LENGTH_SHORT).show()
                    }
                }

                val notifL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

                // --- LÓGICA DE SINCRONIZACIÓN DEL WIDGET (Sincronizada para Cold Start) ---
                val permisoDelWidget by permisoWidgetState
                LaunchedEffect(permisoDelWidget, estaAutenticado) {
                    if (estaAutenticado && permisoDelWidget != null) {
                        // Delay de seguridad para asegurar que la ventana está lista tras el arranque
                        delay(500)
                        
                        // Si venimos del widget, la prioridad es total
                        mostrarSugerenciaBiometria = false
                        
                        if (permisoDelWidget == "MICROFONO") esFlujoSecuencial = true
                        
                        consentimientoDestacado = when (permisoDelWidget) {
                            "MICROFONO" -> PermisoConsentimiento.Microfono
                            "ACCESIBILIDAD" -> PermisoConsentimiento.Accesibilidad
                            "NOTIFICACIONES" -> PermisoConsentimiento.Notificaciones
                            "BATERIA" -> PermisoConsentimiento.Bateria
                            else -> null
                        }
                        // Solo limpiamos el estado del widget una vez que el diálogo ha sido asignado
                        permisoWidgetState.value = null 
                    }
                }

                // GESTIÓN DE SEGURIDAD (Ciclo de Vida)
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> {
                                if (prefs.seguridadBiometrica && BiometricHelper.esBiometriaDisponible(contexto)) {
                                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                    BiometricHelper.autenticar(
                                        contexto = contexto,
                                        onExito = { 
                                            estaAutenticado = true
                                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                        },
                                        onError = { error ->
                                            if (error.contains("cancel", true) || error.contains("atrás", true)) finish()
                                        }
                                    )
                                } else {
                                    estaAutenticado = true
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                }
                            }
                            Lifecycle.Event.ON_PAUSE -> {
                                if (prefs.seguridadBiometrica) {
                                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                }
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                if (estaAutenticado) {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                }
                            }
                            Lifecycle.Event.ON_STOP -> {
                                if (prefs.seguridadBiometrica) estaAutenticado = false
                            }
                            else -> {}
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
                            onSolicitarConsentimiento = { tipo -> 
                                esFlujoSecuencial = false
                                consentimientoDestacado = tipo 
                            }
                        )
                        
                        BotonEstadoSeguridad(
                            onSolicitarConsentimiento = { tipo -> 
                                esFlujoSecuencial = false
                                consentimientoDestacado = tipo 
                            },
                            onSolicitarDesactivacion = { tipo -> consentimientoParaDesactivar = tipo }
                        )

                        Box(modifier = Modifier.wrapContentHeight()) {
                            TarjetaGrabacion(
                                gestorAudio = motor, 
                                alVerArchivos = { },
                                onSolicitarConsentimiento = { tipo -> 
                                    esFlujoSecuencial = true
                                    consentimientoDestacado = tipo 
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TopBarHistorial()
                        Box(modifier = Modifier.weight(1f)) {
                            ListaEvidencias(
                                lista = listaGrabaciones,
                                onPlay = { grab -> motor.reproducirAudio(grab) },
                                onRename = { grab -> archivoParaRenombrar = grab },
                                onShare = { grab -> motor.compartirArchivo(grab) },
                                onSaveToDevice = { grab -> motor.guardarEnDispositivo(grab) },
                                onDelete = { grab -> archivoParaEliminar = grab },
                                onToggleFavorite = { grab -> viewModel.actualizar(grab.copy(esFavorito = !grab.esFavorito)) },
                                onGeneratePDF = { grab -> PdfReportGenerator.generarYCompartir(contexto, grab) }
                            )
                        }
                        FooterApp()
                    }

                    // --- DIÁLOGOS CENTRALIZADOS ---
                    consentimientoDestacado?.let { consentimiento ->
                        DialogoConsentimientoDestacado(
                            consentimiento = consentimiento,
                            onConfirm = {
                                consentimientoDestacado = null
                                when (consentimiento) {
                                    PermisoConsentimiento.Accesibilidad -> SystemUtils.abrirAjustesAccesibilidad(contexto)
                                    PermisoConsentimiento.Microfono -> micL.launch(Manifest.permission.RECORD_AUDIO)
                                    PermisoConsentimiento.Ubicacion -> gpsL.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                            onDismiss = { 
                                consentimientoDestacado = null 
                                esFlujoSecuencial = false
                            }
                        )
                    }

                    consentimientoParaDesactivar?.let { consentimiento ->
                        DialogoDesactivarPermiso(
                            consentimiento = consentimiento,
                            onConfirm = {
                                consentimientoParaDesactivar = null
                                when (consentimiento) {
                                    PermisoConsentimiento.Accesibilidad -> SystemUtils.abrirAjustesAccesibilidad(contexto)
                                    PermisoConsentimiento.Superposicion -> SystemUtils.abrirAjustesSuperposicion(contexto)
                                    PermisoConsentimiento.Bateria -> SystemUtils.abrirAjustesBateria(contexto)
                                    PermisoConsentimiento.Notificaciones -> SystemUtils.abrirAjustesNotificaciones(contexto)
                                    PermisoConsentimiento.Ubicacion, PermisoConsentimiento.Microfono -> SystemUtils.abrirPermisosApp(contexto)
                                    else -> SystemUtils.abrirAjustesApp(contexto)
                                }
                            },
                            onDismiss = { consentimientoParaDesactivar = null }
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
                        DialogoRenombrar(nombreActual = grab.nombre, onConfirm = { nuevoNombre -> motor.renombrarGrabacion(grab, nuevoNombre); archivoParaRenombrar = null }, onDismiss = { archivoParaRenombrar = null })
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
        actualizarPeticionWidget(intent)
    }

    private fun actualizarPeticionWidget(intent: Intent) {
        val permiso = intent.getStringExtra("SOLICITAR_PERMISO")
        if (permiso != null) {
            permisoWidgetState.value = permiso
        }
    }
}
