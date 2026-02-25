package dev.jmcerezo.centinela.ui.componentes

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.jmcerezo.centinela.core.service.CentinelaService
import dev.jmcerezo.centinela.core.service.ServicioBotones
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import dev.jmcerezo.centinela.ui.componentes.dialogos.StructuredInfoDialog

@Composable
fun TopBarApp(onInfoClick: () -> Unit) {
    val contexto = LocalContext.current
    val prefs = remember { Preferencias(contexto) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var mostrarPanelSeguridad by remember { mutableStateOf(false) }
    var mostrarPanelAjustes by remember { mutableStateOf(false) }
    var infoPermiso by remember { mutableStateOf<PermisoDetalle?>(null) }

    // ESTADOS AJUSTES
    var servicioPermanente by remember { mutableStateOf(prefs.servicioPermanente) }
    var modoSilencioso by remember { mutableStateOf(prefs.modoSilencioso) }
    var botonesHabilitados by remember { mutableStateOf(prefs.botonesHabilitados) }
    var seguridadBiometrica by remember { mutableStateOf(prefs.seguridadBiometrica) }

    // INFO DIALOGS
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

    val sincronizarServicios = {
        contexto.startService(Intent(contexto, CentinelaService::class.java))
        contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName))
    }

    // RECEPTOR PARA SINCRONIZAR CON EL WIDGET
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
        accesibilidad = isAccessibilityServiceEnabledLocal(contexto, ServicioBotones::class.java)
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
                            botonesHabilitados = activado
                            prefs.botonesHabilitados = activado
                            sincronizarServicios()
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
                        infoPermiso = PermisoDetalle(
                            "Permiso de Micrófono",
                            "Es la base del sistema. Permite capturar el audio de las evidencias con alta fidelidad.",
                            "Poder registrar lo que sucede a tu alrededor cuando activas la grabación.",
                            listOf("Se abrirá la configuración de la aplicación.", "Entra en el apartado 'Permisos'.", "Asegúrate de que 'Micrófono' esté en 'Permitir'."),
                            { abrirAjustesAppLocal(contexto) }
                        )
                    }

                    PermisoRenglonAppBar("Ubicación (GPS)", ubicacion) {
                        infoPermiso = PermisoDetalle(
                            "Permiso de Ubicación",
                            "Añade validez legal a tus grabaciones al certificar exactamente dónde se han realizado.",
                            "Vincular cada audio con coordenadas GPS precisas y dirección física.",
                            listOf("Entra en el apartado 'Permisos'.", "Selecciona 'Ubicación'.", "Elige 'Permitir solo si la aplicación está en uso'."),
                            { abrirAjustesAppLocal(contexto) }
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermisoRenglonAppBar("Notificaciones", notificaciones) {
                            infoPermiso = PermisoDetalle(
                                "Permiso de Notificaciones",
                                "Permite que el servicio de seguridad sea visible y no sea cerrado por Android.",
                                "Mantener el sistema de escucha activo permanentemente en la barra de estado.",
                                listOf("Entra en el apartado 'Notificaciones'.", "Activa el interruptor de 'Todas las notificaciones de Centinela'."),
                                { abrirAjustesAppLocal(contexto) }
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    PermisoRenglonAppBar("Accesibilidad", accesibilidad) {
                        infoPermiso = PermisoDetalle(
                            "Servicio de Accesibilidad",
                            "Permite a Centinela detectar las pulsaciones de volumen incluso bloqueado.",
                            "Garantizar la captura de audio en situaciones de emergencia sin manipular el dispositivo.",
                            listOf("Busca 'Servicios instalados' o 'Apps descargadas'.", "Selecciona 'Centinela' en la lista.", "Activa el interruptor principal."),
                            { contexto.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        )
                    }
                    PermisoRenglonAppBar("Aparecer encima", superposicion) {
                        infoPermiso = PermisoDetalle(
                            "Mostrar sobre otras apps",
                            "Permite que el proceso de grabación no sea interrumpido por el bloqueo de pantalla o el sistema.",
                            "Mantener la grabación activa en segundo plano de forma ininterrumpida.",
                            listOf("Busca 'Centinela' en la lista.", "Activa el interruptor de permitir superposición."),
                            { contexto.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${contexto.packageName}"))) }
                        )
                    }
                    PermisoRenglonAppBar("Gestión de Batería", bateria) {
                        infoPermiso = PermisoDetalle(
                            "Optimización de Energía",
                            "Evita que Android cierre la aplicación automáticamente para ahorrar batería.",
                            "Asegurar que el sistema de escucha esté siempre listo y no se apague solo.",
                            listOf("Se abrirá la ficha de la aplicación.", "Entra en el menú 'Batería'.", "Selecciona la opción 'SIN RESTRICCIONES'."),
                            { contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}"))) }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarPanelSeguridad = false }) { Text("CERRAR", color = Color(0xFF3D5AFE)) } },
            containerColor = Color(0xFF1A1D2E),
            shape = RoundedCornerShape(24.dp)
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

    infoPermiso?.let { info ->
        AlertDialog(
            onDismissRequest = { infoPermiso = null },
            title = { Text(info.titulo, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("FUNCIÓN", color = Color(0xFF3D5AFE), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Text(info.funcion, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    Column {
                        Text("NECESIDAD", color = Color(0xFF3D5AFE), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Text(info.porQue, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    
                    Column(modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text("PASOS A SEGUIR:", color = Color(0xFF3D5AFE), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        info.pasos.forEachIndexed { i, paso ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("${i + 1}. ", color = Color(0xFF3D5AFE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(paso, color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { info.accion(); infoPermiso = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE))) {
                    Text("IR A AJUSTES")
                }
            },
            containerColor = Color(0xFF1A1D2E),
            shape = RoundedCornerShape(28.dp)
        )
    }
}

private fun abrirAjustesAppLocal(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
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

data class PermisoDetalle(val titulo: String, val funcion: String, val porQue: String, val pasos: List<String>, val accion: () -> Unit)

fun isAccessibilityServiceEnabledLocal(context: Context, service: Class<out AccessibilityService>): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName && it.resolveInfo.serviceInfo.name == service.name }
}
