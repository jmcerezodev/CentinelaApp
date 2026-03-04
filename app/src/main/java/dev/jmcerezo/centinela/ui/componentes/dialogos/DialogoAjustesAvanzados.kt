package dev.jmcerezo.centinela.ui.componentes.dialogos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.jmcerezo.centinela.core.service.ServicioBotones
import dev.jmcerezo.centinela.ui.componentes.AjusteInterruptorConInfo
import dev.jmcerezo.centinela.ui.componentes.PermisoConsentimiento
import dev.jmcerezo.centinela.util.SystemUtils

@Composable
fun DialogoAjustesAvanzados(
    seguridadBiometrica: Boolean,
    botonesHabilitados: Boolean,
    servicioPermanente: Boolean,
    modoSilencioso: Boolean,
    onToggleBiometria: (Boolean) -> Unit,
    onToggleBotones: (Boolean) -> Unit,
    onTogglePermanente: (Boolean) -> Unit,
    onToggleSilencioso: (Boolean) -> Unit,
    onInfoBiometria: () -> Unit,
    onInfoBotones: () -> Unit,
    onInfoPermanente: () -> Unit,
    onInfoAntiSuspension: () -> Unit,
    onRequerirConsentimiento: (PermisoConsentimiento) -> Unit,
    onDismiss: () -> Unit
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Estados reactivos para los permisos que se actualizarán al volver a la app
    var tieneMicro by remember { mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var tieneAccesibilidad by remember { mutableStateOf(SystemUtils.isAccessibilityServiceEnabled(contexto, ServicioBotones::class.java)) }
    var tieneNotif by remember { 
        mutableStateOf(if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true)
    }

    // Observador del ciclo de vida para refrescar estados automáticamente
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tieneMicro = ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                tieneAccesibilidad = SystemUtils.isAccessibilityServiceEnabled(contexto, ServicioBotones::class.java)
                if (Build.VERSION.SDK_INT >= 33) {
                    tieneNotif = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración Avanzada", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. BIOMETRÍA
                AjusteInterruptorConInfo(
                    titulo = "Protección Huella",
                    subtitulo = "Pedir huella al abrir la app",
                    activo = seguridadBiometrica,
                    onInfo = onInfoBiometria,
                    onToggle = onToggleBiometria
                )

                // 2. GRABACIÓN CON BOTONES (Requiere Micro y Accesibilidad)
                val okBotones = tieneMicro && tieneAccesibilidad
                AjusteInterruptorConInfo(
                    titulo = "Grabación con Botones",
                    subtitulo = if (okBotones) "Usa volumen arriba (x3) para grabar" else buildString {
                        if (!tieneMicro) append("Requiere permiso de Microfono")
                        if (!tieneAccesibilidad) { if (isNotEmpty()) append("\n"); append("Requiere permiso de Accesibilidad") }
                    },
                    activo = botonesHabilitados && okBotones, // Blindaje: solo activo si tiene permisos
                    habilitado = okBotones,
                    onInfo = onInfoBotones,
                    onToggle = { 
                        if (okBotones) onToggleBotones(it) 
                        else if (!tieneMicro) onRequerirConsentimiento(PermisoConsentimiento.Microfono)
                        else onRequerirConsentimiento(PermisoConsentimiento.Accesibilidad)
                    }
                )

                // 3. SERVICIO PERMANENTE (Requiere Micro y Notificaciones)
                val okPermanente = tieneMicro && tieneNotif
                AjusteInterruptorConInfo(
                    titulo = "Servicio Permanente",
                    subtitulo = if (okPermanente) "Evita el cierre automático" else buildString {
                        if (!tieneMicro) append("Requiere permiso de Microfono")
                        if (!tieneNotif) { if (isNotEmpty()) append("\n"); append("Requiere permiso de Notificaciones") }
                    },
                    activo = servicioPermanente && okPermanente, // Blindaje
                    habilitado = okPermanente,
                    onInfo = onInfoPermanente,
                    onToggle = { 
                        if (okPermanente) onTogglePermanente(it) 
                        else if (!tieneMicro) onRequerirConsentimiento(PermisoConsentimiento.Microfono)
                        else onRequerirConsentimiento(PermisoConsentimiento.Notificaciones)
                    }
                )

                // 4. MODO ANTI-SUSPENSIÓN (Requiere Micro y Notificaciones)
                val okAnti = tieneMicro && tieneNotif
                AjusteInterruptorConInfo(
                    titulo = "Modo Anti-Suspensión",
                    subtitulo = if (okAnti) "Escucha con pantalla apagada" else buildString {
                        if (!tieneMicro) append("Requiere permiso de Microfono")
                        if (!tieneNotif) { if (isNotEmpty()) append("\n"); append("Requiere permiso de Notificaciones") }
                    },
                    activo = modoSilencioso && okAnti, // Blindaje
                    habilitado = okAnti,
                    onInfo = onInfoAntiSuspension,
                    onToggle = { 
                        if (okAnti) onToggleSilencioso(it) 
                        else if (!tieneMicro) onRequerirConsentimiento(PermisoConsentimiento.Microfono)
                        else onRequerirConsentimiento(PermisoConsentimiento.Notificaciones)
                    }
                )
            }
        },
        confirmButton = { 
            TextButton(onClick = onDismiss) { 
                Text("CERRAR", color = Color(0xFF3D5AFE)) 
            } 
        },
        containerColor = Color(0xFF1A1D2E),
        shape = RoundedCornerShape(24.dp)
    )
}
