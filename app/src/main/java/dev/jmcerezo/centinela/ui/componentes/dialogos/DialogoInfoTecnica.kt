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

@Composable
fun DialogoInfoTecnica(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D2E),
        titleContentColor = Color.White,
        title = { Text("Especificaciones Técnicas", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemTecnico("Integridad", "Hash SHA-256 (Verificación forense)")
                ItemTecnico("Calidad de Audio", "PCM Lineal / WAV @ 44.1kHz (Sin pérdida)")
                ItemTecnico("Procesado de Voz", "DSP: Supresión de ruido y control de ganancia auto.")
                ItemTecnico("Ubicación", "Coordenadas GPS y Geocodificación inversa")
                ItemTecnico("Base de Datos", "Room Persistence Library (Cifrada)")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CERRAR", color = Color(0xFF3D5AFE), fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun ItemTecnico(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label.uppercase(), color = Color(0xFF3D5AFE), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp)
    }
}
