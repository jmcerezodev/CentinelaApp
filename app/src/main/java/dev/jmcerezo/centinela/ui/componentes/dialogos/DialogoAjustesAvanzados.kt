package dev.jmcerezo.centinela.ui.componentes.dialogos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jmcerezo.centinela.ui.componentes.AjusteInterruptorConInfo

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
    onDismiss: () -> Unit
) {
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
                    subtitulo = "Usa volumen arriba (x3) para grabar",
                    activo = botonesHabilitados,
                    onInfo = onInfoBotones,
                    onToggle = onToggleBotones
                )

                AjusteInterruptorConInfo(
                    titulo = "Servicio Permanente",
                    subtitulo = "Evita el cierre automático",
                    activo = servicioPermanente,
                    onInfo = onInfoPermanente,
                    onToggle = onTogglePermanente
                )

                AjusteInterruptorConInfo(
                    titulo = "Modo Anti-Suspensión",
                    subtitulo = "Escucha con pantalla apagada",
                    activo = modoSilencioso,
                    onInfo = onInfoAntiSuspension,
                    onToggle = onToggleSilencioso
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
