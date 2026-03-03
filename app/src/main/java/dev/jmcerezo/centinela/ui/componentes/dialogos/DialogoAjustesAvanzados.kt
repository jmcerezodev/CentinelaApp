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
import dev.jmcerezo.centinela.ui.componentes.AjusteInterruptorConInfo
import dev.jmcerezo.centinela.ui.componentes.PermisoConsentimiento

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
    
    // Verificamos el permiso de micro de forma reactiva
    val tienePermisoMicro = ContextCompat.checkSelfPermission(
        contexto, 
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración Avanzada", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AjusteInterruptorConInfo(
                    titulo = "Protección Huella",
                    subtitulo = "Pedir huella al abrir la app",
                    activo = seguridadBiometrica,
                    onInfo = onInfoBiometria,
                    onToggle = onToggleBiometria
                )

                AjusteInterruptorConInfo(
                    titulo = "Grabación con Botones",
                    subtitulo = if (tienePermisoMicro) "Usa volumen arriba (x3) para grabar" else "Requiere permiso de micrófono",
                    activo = botonesHabilitados,
                    habilitado = tienePermisoMicro,
                    onInfo = onInfoBotones,
                    onToggle = { 
                        if (tienePermisoMicro) onToggleBotones(it) 
                        else onRequerirConsentimiento(PermisoConsentimiento.Microfono) 
                    }
                )

                AjusteInterruptorConInfo(
                    titulo = "Servicio Permanente",
                    subtitulo = if (tienePermisoMicro) "Evita el cierre automático" else "Requiere permiso de micrófono",
                    activo = servicioPermanente,
                    habilitado = tienePermisoMicro,
                    onInfo = onInfoPermanente,
                    onToggle = { 
                        if (tienePermisoMicro) onTogglePermanente(it) 
                        else onRequerirConsentimiento(PermisoConsentimiento.Microfono) 
                    }
                )

                AjusteInterruptorConInfo(
                    titulo = "Modo Anti-Suspensión",
                    subtitulo = if (tienePermisoMicro) "Escucha con pantalla apagada" else "Requiere permiso de micrófono",
                    activo = modoSilencioso,
                    habilitado = tienePermisoMicro,
                    onInfo = onInfoAntiSuspension,
                    onToggle = { 
                        if (tienePermisoMicro) onToggleSilencioso(it) 
                        else onRequerirConsentimiento(PermisoConsentimiento.Microfono) 
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
