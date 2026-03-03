package dev.jmcerezo.centinela.ui.componentes.dialogos

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    
    // Verificación de todos los permisos necesarios de forma dinámica
    val tieneMicro = ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val tieneAccesibilidad = SystemUtils.isAccessibilityServiceEnabled(contexto, ServicioBotones::class.java)
    val tieneNotif = if (android.os.Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración Avanzada", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. BIOMETRÍA (Independiente)
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
                    activo = botonesHabilitados,
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
                    activo = servicioPermanente,
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
                    activo = modoSilencioso,
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
