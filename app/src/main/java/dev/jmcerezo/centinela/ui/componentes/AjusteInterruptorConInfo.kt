package dev.jmcerezo.centinela.ui.componentes

import androidx.compose.foundation.layout.*
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
 * Componente de fila para un ajuste con interruptor y botón de información.
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
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (habilitado) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitulo, 
                    color = if (habilitado) Color.Gray else Color(0xFFFF5252), 
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = onInfo,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF3D5AFE).copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Switch(
            checked = activo,
            onCheckedChange = { if (habilitado) onToggle(it) else onToggle(true) },
            modifier = Modifier.scale(0.7f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3D5AFE),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF25293D)
            )
        )
    }
}
