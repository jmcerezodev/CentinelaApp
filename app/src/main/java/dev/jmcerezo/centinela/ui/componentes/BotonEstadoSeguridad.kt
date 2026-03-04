package dev.jmcerezo.centinela.ui.componentes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.jmcerezo.centinela.core.service.ServicioBotones
import dev.jmcerezo.centinela.ui.componentes.dialogos.DialogoEstadoSeguridad
import dev.jmcerezo.centinela.util.SystemUtils

/**
 * Componente interactivo que muestra el estado de seguridad y permisos.
 * Centrado sobre la tarjeta de grabación para guiar al usuario.
 */
@Composable
fun BotonEstadoSeguridad(
    onSolicitarConsentimiento: (PermisoConsentimiento) -> Unit,
    onSolicitarDesactivacion: (PermisoConsentimiento) -> Unit
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mostrarPanelSeguridad by remember { mutableStateOf(false) }
    
    var accesibilidad by remember { mutableStateOf(false) }
    var superposicion by remember { mutableStateOf(false) }
    var bateria by remember { mutableStateOf(false) }
    var microfono by remember { mutableStateOf(false) }
    var ubicacion by remember { mutableStateOf(false) }
    var notificaciones by remember { mutableStateOf(false) }

    val todosLosPermisosOk = accesibilidad && superposicion && bateria && microfono && ubicacion && 
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificaciones else true)

    val actualizarEstados = {
        accesibilidad = SystemUtils.isAccessibilityServiceEnabled(contexto, ServicioBotones::class.java)
        superposicion = Settings.canDrawOverlays(contexto)
        val pm = contexto.getSystemService(Context.POWER_SERVICE) as PowerManager
        bateria = pm.isIgnoringBatteryOptimizations(contexto.packageName)
        microfono = ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ubicacion = ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificaciones = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            notificaciones = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) actualizarEstados()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        actualizarEstados()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clickable { mostrarPanelSeguridad = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (todosLosPermisosOk) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (todosLosPermisosOk) Color(0xFF00C853) else Color(0xFFFF5252),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = if (todosLosPermisosOk) "Estado de Permisos: Completos" else "Estado de Permisos: Incompletos",
            color = if (todosLosPermisosOk) Color(0xFF00C853) else Color(0xFFFF5252),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = (if (todosLosPermisosOk) Color(0xFF00C853) else Color(0xFFFF5252)).copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
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
}
