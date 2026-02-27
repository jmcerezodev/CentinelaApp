package dev.jmcerezo.centinela.ui.componentes

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.jmcerezo.centinela.core.service.CentinelaService
import dev.jmcerezo.centinela.core.service.ServicioBotones
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import dev.jmcerezo.centinela.ui.componentes.dialogos.StructuredInfoDialog
import dev.jmcerezo.centinela.util.SystemUtils

@Composable
fun TopBarApp(onInfoClick: () -> Unit) {
    val contexto = LocalContext.current
    val prefs = remember { Preferencias(contexto) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var mostrarPanelSeguridad by remember { mutableStateOf(false) }
    var mostrarPanelAjustes by remember { mutableStateOf(false) }
    
    // Estado para el Consentimiento Destacado (Requisito Google Play para permisos sensibles)
    var consentimientoActual by remember { mutableStateOf<PermisoConsentimiento?>(null) }

    // ESTADOS AJUSTES
    var servicioPermanente by remember { mutableStateOf(prefs.servicioPermanente) }
    var modoSilencioso by remember { mutableStateOf(prefs.modoSilencioso) }
    var botonesHabilitados by remember { mutableStateOf(prefs.botonesHabilitados) }
    var seguridadBiometrica by remember { mutableStateOf(prefs.seguridadBiometrica) }

    // INFO DIALOGS (Informativos de Ajustes)
    var mostrarInfoPermanente by remember { mutableStateOf(false) }
    var mostrarInfoAntiSuspension by remember { mutableStateOf(false) }
    var mostrarInfoBotones by remember { mutableStateOf(false) }
    var mostrarInfoBiometria by remember { mutableStateOf(false) }

    // ESTADOS DE PERMISOS
    var accesibilidad by remember { mutableStateOf(false) }
    var superposicion by remember { mutableStateOf(false) }
    var bateria by remember { mutableStateOf(false) }
    var microfono by remember { mutableStateOf(false) }
    var ubicacion by remember { mutableStateOf(false) }
    var notificaciones by remember { mutableStateOf(false) }

    val todosLosPermisosOk = accesibilidad && superposicion && bateria && microfono && ubicacion && 
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificaciones else true)

    // Launchers para solicitud directa de permisos
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val sincronizarServicios = {
        contexto.startService(Intent(contexto, CentinelaService::class.java))
        contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName))
    }

    DisposableEffect(contexto) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                servicioPermanente = prefs.servicioPermanente
                modoSilencioso = prefs.modoSilencioso
                botonesHabilitados = prefs.botonesHabilitados
                seguridadBiometrica = prefs.seguridadBiometrica
            }
        }
        val filter = IntentFilter("dev.jmcerezo.ACTUALIZAR_CONFIGURACION")
        ContextCompat.registerReceiver(contexto, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { contexto.unregisterReceiver(receiver) }
    }

    val actualizarEstados = {
        accesibilidad = SystemUtils.isAccessibilityServiceEnabled(contexto, ServicioBotones::class.java)
        superposicion = Settings.canDrawOverlays(contexto)
        val pm = contexto.getSystemService(Context.POWER_SERVICE) as PowerManager
        bateria = pm.isIgnoringBatteryOptimizations(contexto.packageName)
        
        microfono = ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ubicacion = ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificaciones = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) actualizarEstados()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 20.dp, end = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "CENTINELA", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Text(text = "SISTEMA DE PROTECCIÓN LEGAL", color = Color(0xFF3D5AFE), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        Row {
            IconButton(onClick = { mostrarPanelSeguridad = true }) {
                Icon(
                    imageVector = if (todosLosPermisosOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Estado de Seguridad",
                    tint = if (todosLosPermisosOk) Color(0xFF00C853) else Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            IconButton(onClick = { mostrarPanelAjustes = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuración Avanzada",
                    tint = Color(0xFF3D5AFE),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onInfoClick) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Información", tint = Color(0xFF3D5AFE), modifier = Modifier.size(24.dp))
            }
        }
    }

    // PANEL DE CONFIGURACIÓN AVANZADA
    if (mostrarPanelAjustes) {
        AlertDialog(
            onDismissRequest = { mostrarPanelAjustes = false },
            title = { Text("Configuración Avanzada", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AjusteInterruptorConInfo(
                        titulo = "Protección Huella",
                        subtitulo = "Pedir huella al abrir la app",
                        activo = seguridadBiometrica,
                        onInfo = { mostrarInfoBiometria = true },
                        onToggle = { activado ->
                            seguridadBiometrica = activado
                            prefs.seguridadBiometrica = activado
                        }
                    )

                    AjusteInterruptorConInfo(
                        titulo = "Grabación con Botones",
                        subtitulo = "Usa volumen arriba (x3) para grabar",
                        activo = botonesHabilitados,
                        onInfo = { mostrarInfoBotones = true },
                        onToggle = { activado ->
                            if (activado && !accesibilidad) {
                                consentimientoActual = PermisoConsentimiento.Accesibilidad
                            } else {
                                botonesHabilitados = activado
                                prefs.botonesHabilitados = activado
                                sincronizarServicios()
                            }
                        }
                    )

                    AjusteInterruptorConInfo(
                        titulo = "Servicio Permanente",
                        subtitulo = "Evita el cierre automático",
                        activo = servicioPermanente,
                        onInfo = { mostrarInfoPermanente = true },
                        onToggle = { activado ->
                            servicioPermanente = activado
                            prefs.servicioPermanente = activado
                            sincronizarServicios()
                        }
                    )

                    AjusteInterruptorConInfo(
                        titulo = "Modo Anti-Suspensión",
                        subtitulo = "Escucha con pantalla apagada",
                        activo = modoSilencioso,
                        onInfo = { mostrarInfoAntiSuspension = true },
                        onToggle = { activado ->
                            modoSilencioso = activado
                            prefs.modoSilencioso = activado
                            sincronizarServicios()
                        }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { mostrarPanelAjustes = false }) { Text("CERRAR", color = Color(0xFF3D5AFE)) } },
            containerColor = Color(0xFF1A1D2E),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // PANEL DE ESTADO DE SEGURIDAD (PERMISOS)
    if (mostrarPanelSeguridad) {
        AlertDialog(
            onDismissRequest = { mostrarPanelSeguridad = false },
            title = { Text("Estado de Seguridad", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configura los permisos necesarios para el correcto funcionamiento del sistema.", color = Color.Gray, fontSize = 12.sp)
                    
                    PermisoRenglonAppBar("Micrófono", microfono) {
                        consentimientoActual = PermisoConsentimiento.Microfono
                    }

                    PermisoRenglonAppBar("Ubicación (GPS)", ubicacion) {
                        consentimientoActual = PermisoConsentimiento.Ubicacion
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermisoRenglonAppBar("Notificaciones", notificaciones) {
                            consentimientoActual = PermisoConsentimiento.Notificaciones
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    PermisoRenglonAppBar("Accesibilidad", accesibilidad) {
                        consentimientoActual = PermisoConsentimiento.Accesibilidad
                    }
                    
                    PermisoRenglonAppBar("Aparecer encima", superposicion) {
                        consentimientoActual = PermisoConsentimiento.Superposicion
                    }
                    
                    PermisoRenglonAppBar("Gestión de Batería", bateria) {
                        consentimientoActual = PermisoConsentimiento.Bateria
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarPanelSeguridad = false }) { Text("CERRAR", color = Color(0xFF3D5AFE)) } },
            containerColor = Color(0xFF1A1D2E),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // DIÁLOGO DE CONSENTIMIENTO DESTACADO GENÉRICO (CUMPLE POLÍTICAS GOOGLE PLAY)
    consentimientoActual?.let { consentimiento ->
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
            title = { 
                Text(
                    text = consentimiento.titulo, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = consentimiento.introduccion,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF3D5AFE).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Propósito de la función:",
                                color = Color(0xFF3D5AFE),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = consentimiento.proposito,
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Text(
                        text = "Privacidad: Centinela NO recopila ni comparte sus datos personales con terceros. Esta información se utiliza exclusivamente para la funcionalidad descrita.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "¿Deseas conceder este permiso ahora?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { consentimientoActual = null }) {
                    Text("AHORA NO", color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        val actual = consentimientoActual
                        consentimientoActual = null
                        when (actual) {
                            PermisoConsentimiento.Accesibilidad -> contexto.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            PermisoConsentimiento.Microfono -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            PermisoConsentimiento.Ubicacion -> locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            PermisoConsentimiento.Notificaciones -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            PermisoConsentimiento.Superposicion -> contexto.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${contexto.packageName}")))
                            PermisoConsentimiento.Bateria -> contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}")))
                            else -> {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SÍ, CONTINUAR", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1D2E),
            shape = RoundedCornerShape(28.dp)
        )
    }

    // DIÁLOGOS DE INFORMACIÓN DE AJUSTES
    if (mostrarInfoBiometria) {
        StructuredInfoDialog(
            titulo = "Protección Huella",
            secciones = listOf(
                "Función" to "Exige autenticación mediante huella dactilar o rostro cada vez que se abre la aplicación.",
                "Privacidad" to "Tus evidencias estarán seguras aunque prestes el móvil a otra persona.",
                "Recomendación" to "Mantén esta opción activada para máxima seguridad."
            ),
            onDismiss = { mostrarInfoBiometria = false }
        )
    }

    if (mostrarInfoBotones) {
        StructuredInfoDialog(
            titulo = "Grabación con Botones",
            secciones = listOf(
                "Función" to "Permite iniciar o detener la grabación pulsando 3 veces el botón de volumen arriba.",
                "Recomendación" to "Desactívalo si vas a escuchar música.",
                "Seguridad" to "Aunque esté desactivado, el botón REC de la pantalla siempre funcionará."
            ),
            onDismiss = { mostrarInfoBotones = false }
        )
    }

    if (mostrarInfoPermanente) {
        StructuredInfoDialog(
            titulo = "Servicio Permanente",
            secciones = listOf(
                "Función" to "Mantiene la app en memoria para actuar siempre.",
                "Batería" to "Consumo insignificante (0%)."
            ),
            onDismiss = { mostrarInfoPermanente = false }
        )
    }

    if (mostrarInfoAntiSuspension) {
        StructuredInfoDialog(
            titulo = "Modo Anti-Suspensión",
            secciones = listOf(
                "Función" to "Fuerza la escucha con pantalla apagada.",
                "Caso de Uso" to "Actívalo solo para grabaciones discretas.",
                "Batería" to "Consumo moderado. Desactívalo al terminar."
            ),
            onDismiss = { mostrarInfoAntiSuspension = false }
        )
    }
}

@Composable
fun PermisoRenglonAppBar(nombre: String, activado: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.background(Color(0xFF25293D), RoundedCornerShape(8.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(if (activado) Color(0xFF00C853) else Color(0xFFFF5252), CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Text(nombre, color = Color.White, fontSize = 14.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
    }
}

sealed class PermisoConsentimiento(val titulo: String, val introduccion: String, val proposito: String) {
    object Accesibilidad : PermisoConsentimiento(
        "Servicio de Accesibilidad",
        "Centinela requiere la API de Accesibilidad para funcionar en segundo plano.",
        "Detectar pulsaciones de los botones físicos de volumen para iniciar o detener grabaciones de emergencia sin necesidad de encender la pantalla."
    )
    object Microfono : PermisoConsentimiento(
        "Permiso de Micrófono",
        "El acceso al micrófono es esencial para la funcionalidad principal de la app.",
        "Capturar y grabar audio de alta fidelidad para generar evidencias legales válidas."
    )
    object Ubicacion : PermisoConsentimiento(
        "Permiso de Ubicación",
        "La ubicación añade una capa de validez legal a tus grabaciones.",
        "Certificar el lugar exacto (coordenadas GPS y dirección) donde se realizó la grabación de la evidencia."
    )
    object Notificaciones : PermisoConsentimiento(
        "Permiso de Notificaciones",
        "Necesario para mantener el sistema de protección activo.",
        "Mostrar una notificación permanente que evita que el sistema Android cierre la aplicación y garantiza que la grabación no se interrumpa."
    )
    object Superposicion : PermisoConsentimiento(
        "Aparecer encima",
        "Permite que el proceso de grabación tenga prioridad visual.",
        "Garantizar que la grabación no sea interrumpida por otras aplicaciones o por el bloqueo automático del sistema."
    )
    object Bateria : PermisoConsentimiento(
        "Gestión de Batería",
        "Evita la suspensión automática por ahorro de energía.",
        "Asegurar que Centinela esté siempre listo para actuar, impidiendo que Android cierre el servicio de seguridad para ahorrar batería."
    )
}
