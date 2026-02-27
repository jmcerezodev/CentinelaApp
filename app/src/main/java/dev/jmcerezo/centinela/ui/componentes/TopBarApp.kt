package dev.jmcerezo.centinela.ui.componentes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.jmcerezo.centinela.core.service.CentinelaService
import dev.jmcerezo.centinela.core.service.ServicioBotones
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import dev.jmcerezo.centinela.ui.componentes.dialogos.DialogoAjustesAvanzados
import dev.jmcerezo.centinela.ui.componentes.dialogos.DialogoEstadoSeguridad
import dev.jmcerezo.centinela.ui.componentes.dialogos.StructuredInfoDialog
import dev.jmcerezo.centinela.util.BiometricHelper
import dev.jmcerezo.centinela.util.SystemUtils

@Composable
fun TopBarApp(
    onInfoClick: () -> Unit,
    permisoWidgetSolicitado: String? = null,
    onPermisoWidgetMostrado: () -> Unit = {},
    onSolicitarConsentimiento: (PermisoConsentimiento) -> Unit,
    onSolicitarDesactivacion: (PermisoConsentimiento) -> Unit
) {
    val contexto = LocalContext.current
    val prefs = remember { Preferencias(contexto) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var mostrarPanelSeguridad by remember { mutableStateOf(false) }
    var mostrarPanelAjustes by remember { mutableStateOf(false) }
    
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

    // EFECTO: Capturar solicitud externa del Widget
    LaunchedEffect(permisoWidgetSolicitado) {
        if (permisoWidgetSolicitado != null) {
            val tipo = when (permisoWidgetSolicitado) {
                "MICROFONO" -> PermisoConsentimiento.Microfono
                "ACCESIBILIDAD" -> PermisoConsentimiento.Accesibilidad
                "NOTIFICACIONES" -> PermisoConsentimiento.Notificaciones
                "BATERIA" -> PermisoConsentimiento.Bateria
                else -> null
            }
            tipo?.let { onSolicitarConsentimiento(it) }
            onPermisoWidgetMostrado()
        }
    }

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
        
        microfono = ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ubicacion = ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificaciones = ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
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
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 20.dp, end = 8.dp, bottom = 4.dp),
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
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Configuración Avanzada", tint = Color(0xFF3D5AFE), modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onInfoClick) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Información", tint = Color(0xFF3D5AFE), modifier = Modifier.size(24.dp))
            }
        }
    }

    if (mostrarPanelAjustes) {
        DialogoAjustesAvanzados(
            seguridadBiometrica = seguridadBiometrica,
            botonesHabilitados = botonesHabilitados,
            servicioPermanente = servicioPermanente,
            modoSilencioso = modoSilencioso,
            onToggleBiometria = { it ->
                if (it) onSolicitarConsentimiento(PermisoConsentimiento.Biometria)
                else {
                    (contexto as? FragmentActivity)?.let { activity ->
                        BiometricHelper.autenticar(activity, { 
                            seguridadBiometrica = false; prefs.seguridadBiometrica = false
                        }, { })
                    }
                }
            },
            onToggleBotones = { if (it) { if (!accesibilidad) onSolicitarConsentimiento(PermisoConsentimiento.Accesibilidad) else { botonesHabilitados = true; prefs.botonesHabilitados = true; sincronizarServicios() } } else { onSolicitarDesactivacion(PermisoConsentimiento.Accesibilidad) } },
            onTogglePermanente = { if (it) { if (Build.VERSION.SDK_INT >= 33 && !notificaciones) onSolicitarConsentimiento(PermisoConsentimiento.Notificaciones) else { servicioPermanente = true; prefs.servicioPermanente = true; sincronizarServicios() } } else { onSolicitarDesactivacion(PermisoConsentimiento.Notificaciones) } },
            onToggleSilencioso = { if (it) { if (!bateria) onSolicitarConsentimiento(PermisoConsentimiento.Bateria) else { modoSilencioso = true; prefs.modoSilencioso = true; sincronizarServicios() } } else { onSolicitarDesactivacion(PermisoConsentimiento.Bateria) } },
            onInfoBiometria = { mostrarInfoBiometria = true },
            onInfoBotones = { mostrarInfoBotones = true },
            onInfoPermanente = { mostrarInfoPermanente = true },
            onInfoAntiSuspension = { mostrarInfoAntiSuspension = true },
            onDismiss = { mostrarPanelAjustes = false }
        )
    }

    if (mostrarPanelSeguridad) {
        DialogoEstadoSeguridad(
            microfonoOk = microfono, ubicacionOk = ubicacion, notificacionesOk = notificaciones,
            accesibilidadOk = accesibilidad, superposicionOk = superposicion, bateriaOk = bateria,
            onClickMicrofono = { if (microfono) onSolicitarDesactivacion(PermisoConsentimiento.Microfono) else onSolicitarConsentimiento(PermisoConsentimiento.Microfono) },
            onClickUbicacion = { if (ubicacion) onSolicitarDesactivacion(PermisoConsentimiento.Ubicacion) else onSolicitarConsentimiento(PermisoConsentimiento.Ubicacion) },
            onClickNotificaciones = { if (notificaciones) onSolicitarDesactivacion(PermisoConsentimiento.Notificaciones) else onSolicitarConsentimiento(PermisoConsentimiento.Notificaciones) },
            onClickAccesibilidad = { if (accesibilidad) onSolicitarDesactivacion(PermisoConsentimiento.Accesibilidad) else onSolicitarConsentimiento(PermisoConsentimiento.Accesibilidad) },
            onClickSuperposicion = { if (superposicion) onSolicitarDesactivacion(PermisoConsentimiento.Superposicion) else onSolicitarConsentimiento(PermisoConsentimiento.Superposicion) },
            onClickBateria = { if (bateria) onSolicitarDesactivacion(PermisoConsentimiento.Bateria) else onSolicitarConsentimiento(PermisoConsentimiento.Bateria) },
            onDismiss = { mostrarPanelSeguridad = false }
        )
    }

    // INFO DIALOGS
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
    object Accesibilidad : PermisoConsentimiento("Servicio de Accesibilidad", "Centinela requiere la API de Accesibilidad para funcionar en segundo plano.", "Detectar pulsaciones de los botones físicos de volumen para iniciar o detener grabaciones de emergencia sin necesidad de encender la pantalla.")
    object Microfono : PermisoConsentimiento("Permiso de Micrófono", "El acceso al micrófono es esencial para la funcionalidad principal de la app.", "Capturar y grabar audio de alta fidelidad para generar evidencias legales válidas.")
    object Ubicacion : PermisoConsentimiento("Permiso de Ubicación", "La ubicación añade una capa de validez legal a tus grabaciones.", "Certificar el lugar exacto (coordenadas GPS y dirección) donde se realizó la grabación de la evidencia.")
    object Notificaciones : PermisoConsentimiento("Permiso de Notificaciones", "Necesario para mantener el sistema de protección activo.", "Mostrar una notificación permanente que evita que el sistema Android cierre la aplicación y garantiza que la grabación no se interrumpa.")
    object Superposicion : PermisoConsentimiento("Aparecer encima", "Permite que el proceso de grabación tenga prioridad visual.", "Garantizar que la grabación no sea interrumpida por otras aplicaciones o por el bloqueo automático del sistema.")
    object Bateria : PermisoConsentimiento("Gestión de Batería", "Evita la suspensión automática por ahorro de energía.", "Asegurar que Centinela esté siempre listo para actuar, impidiendo que Android cierre el servicio de seguridad para ahorrar batería.")
    object Biometria : PermisoConsentimiento("Seguridad Biométrica", "Protege el acceso a tus grabaciones mediante la seguridad de tu dispositivo.", "Solicitar tu huella dactilar o reconocimiento facial cada vez que se abra la aplicación para garantizar que solo tú puedas ver las evidencias. Puedes cambiar esta opción en cualquier momento desde estos ajustes.")
}
