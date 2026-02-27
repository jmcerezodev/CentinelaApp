package dev.jmcerezo.centinela.ui.componentes.dialogos

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jmcerezo.centinela.ui.componentes.PermisoRenglonAppBar

@Composable
fun DialogoEstadoSeguridad(
    microfonoOk: Boolean,
    ubicacionOk: Boolean,
    notificacionesOk: Boolean,
    accesibilidadOk: Boolean,
    superposicionOk: Boolean,
    bateriaOk: Boolean,
    onClickMicrofono: () -> Unit,
    onClickUbicacion: () -> Unit,
    onClickNotificaciones: () -> Unit,
    onClickAccesibilidad: () -> Unit,
    onClickSuperposicion: () -> Unit,
    onClickBateria: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Estado de Seguridad", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configura los permisos necesarios para el correcto funcionamiento del sistema.", color = Color.Gray, fontSize = 12.sp)
                
                PermisoRenglonAppBar("Micrófono", microfonoOk, onClickMicrofono)
                PermisoRenglonAppBar("Ubicación (GPS)", ubicacionOk, onClickUbicacion)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermisoRenglonAppBar("Notificaciones", notificacionesOk, onClickNotificaciones)
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                PermisoRenglonAppBar("Accesibilidad", accesibilidadOk, onClickAccesibilidad)
                PermisoRenglonAppBar("Aparecer encima", superposicionOk, onClickSuperposicion)
                PermisoRenglonAppBar("Gestión de Batería", bateriaOk, onClickBateria)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CERRAR", color = Color(0xFF3D5AFE)) } },
        containerColor = Color(0xFF1A1D2E),
        shape = RoundedCornerShape(24.dp)
    )
}
