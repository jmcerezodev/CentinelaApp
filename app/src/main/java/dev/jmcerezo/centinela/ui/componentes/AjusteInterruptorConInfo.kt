package dev.jmcerezo.centinela.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente de fila para un ajuste con interruptor o botón de activación.
 * Garantiza que los botones de información estén siempre alineados verticalmente
 * y que el espaciado entre filas sea uniforme.
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
            .defaultMinSize(minHeight = 48.dp), // Altura mínima para uniformidad entre 1 y 2 líneas de texto
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
                fontSize = 10.sp, 
                lineHeight = 12.sp
            )
        }

        // Fila de controles con ancho controlado para alineación vertical del botón de info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Botón de información (Círculo azul, letra 'i' blanca perfectamente centrada)
            IconButton(
                onClick = onInfo,
                modifier = Modifier
                    .size(32.dp)
                    .semantics { contentDescription = "Información" } // Para accesibilidad y tests
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color(0xFF3D5AFE), CircleShape)
                ) {
                    Text(
                        text = "i",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Contenedor de ancho fijo (72dp) para el control derecho. 
            Box(
                modifier = Modifier.width(72.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5252).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFF5252)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ACTIVAR",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
