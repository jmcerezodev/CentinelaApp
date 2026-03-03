package dev.jmcerezo.centinela.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente de fila para un ajuste con interruptor o botón de activación.
 */
@Composable
fun AjusteInterruptorConInfo(
    titulo: String,
    subtitulo: String,
    activo: Boolean,
    habilitado: Boolean = true,
    onInfo: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (habilitado) 1f else 0.8f)
            ) {
                Text(
                    text = titulo, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitulo, 
                    color = if (habilitado) Color.Gray else Color(0xFFFF5252), 
                    fontSize = 10.sp, // Fuente un poco más pequeña para evitar saltos de línea
                    lineHeight = 12.sp
                )
            }
            // Botón de información siempre activo
            IconButton(
                onClick = onInfo,
                modifier = Modifier.size(30.dp) // Tamaño ligeramente reducido para ganar espacio de texto
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Información",
                    tint = Color(0xFF3D5AFE),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp)) // Margen mínimo de seguridad

        if (habilitado) {
            Switch(
                checked = activo,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.7f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3D5AFE),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF25293D)
                )
            )
        } else {
            Button(
                onClick = { onToggle(true) },
                modifier = Modifier.height(26.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252).copy(alpha = 0.15f),
                    contentColor = Color(0xFFFF5252)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "ACTIVAR",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
