package dev.jmcerezo.centinela.ui.componentes

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.jmcerezo.centinela.core.ServicioBotones

@Composable
fun TopBarApp(onInfoClick: () -> Unit) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mostrarPanelSeguridad by remember { mutableStateOf(false) }
    var infoPermiso by remember { mutableStateOf<PermisoDetalle?>(null) }

    var accesibilidad by remember { mutableStateOf(false) }
    var superposicion by remember { mutableStateOf(false) }
    var bateria by remember { mutableStateOf(false) }

    val todosLosPermisosOk = accesibilidad && superposicion && bateria

    val actualizarEstados = {
        accesibilidad = isAccessibilityServiceEnabled(contexto, ServicioBotones::class.java)
        superposicion = Settings.canDrawOverlays(contexto)
        val pm = contexto.getSystemService(Context.POWER_SERVICE) as PowerManager
        bateria = pm.isIgnoringBatteryOptimizations(contexto.packageName)
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
                    contentDescription = null,
                    tint = if (todosLosPermisosOk) Color(0xFF00C853) else Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onInfoClick) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF3D5AFE), modifier = Modifier.size(24.dp))
            }
        }
    }

    if (mostrarPanelSeguridad) {
        AlertDialog(
            onDismissRequest = { mostrarPanelSeguridad = false },
            title = { Text("Estado de Seguridad", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configura los permisos necesarios para el correcto funcionamiento del sistema.", color = Color.Gray, fontSize = 12.sp)
                    
                    PermisoFilaAppBar("Accesibilidad", accesibilidad) {
                        infoPermiso = PermisoDetalle(
                            "Servicio de Accesibilidad",
                            "Permite a Centinela detectar las pulsaciones de volumen incluso bloqueado.",
                            "Garantizar la captura de audio en situaciones de emergencia sin manipular el dispositivo.",
                            listOf("Busca 'Servicios instalados' o 'Apps descargadas'.", "Selecciona 'Centinela' en la lista.", "Activa el interruptor principal."),
                            { contexto.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        )
                    }
                    PermisoFilaAppBar("Aparecer encima", superposicion) {
                        infoPermiso = PermisoDetalle(
                            "Mostrar sobre otras apps",
                            "Permite que el proceso de grabación no sea interrumpido por el bloqueo de pantalla o el sistema.",
                            "Mantener la grabación activa en segundo plano de forma ininterrumpida.",
                            listOf("Busca 'Centinela' entre tus aplicaciones.", "Activa el interruptor de permitir superposición."),
                            { contexto.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${contexto.packageName}"))) }
                        )
                    }
                    PermisoFilaAppBar("Gestión de Batería", bateria) {
                        infoPermiso = PermisoDetalle(
                            "Gestión de Energía",
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

    infoPermiso?.let { info ->
        AlertDialog(
            onDismissRequest = { infoPermiso = null },
            title = { Text(info.titulo, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SeccionInfo("Función", info.funcion)
                    SeccionInfo("Por qué es necesario", info.porQue)
                    
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

@Composable
fun SeccionInfo(label: String, contenido: String) {
    Column {
        Text(label.uppercase(), color = Color(0xFF3D5AFE), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Text(contenido, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
fun PermisoFilaAppBar(nombre: String, activado: Boolean, onClick: () -> Unit) {
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
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
    }
}

data class PermisoDetalle(val titulo: String, val funcion: String, val porQue: String, val pasos: List<String>, val accion: () -> Unit)

fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName && it.resolveInfo.serviceInfo.name == service.name }
}
