package dev.jmcerezo.centinela.ui.componentes.dialogos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Diálogo genérico estructurado para mostrar información detallada.
 * Se utiliza para explicar funciones de la app (Servicio Permanente, Anti-Suspensión, etc.)
 */
@Composable
fun StructuredInfoDialog(
    titulo: String,
    secciones: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = titulo, 
                color = Color.White, 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold 
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                secciones.forEach { (subtitulo, contenido) ->
                    Column {
                        Text(
                            text = subtitulo.uppercase(),
                            color = Color(0xFF3D5AFE),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = contenido,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ENTENDIDO", color = Color(0xFF3D5AFE), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1A1D2E),
        shape = RoundedCornerShape(24.dp)
    )
}
