package dev.jmcerezo.centinela.ui.componentes.dialogos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialogoInfoTecnica(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D2E),
        titleContentColor = Color.White,
        title = { Text("Especificaciones de Seguridad") },
        text = {
            Column {
                ItemTecnico("Encriptación", "SHA-256 (Hash de Integridad)")
                ItemTecnico("Formato", "MPEG-4 (AAC)")
                ItemTecnico("Localización", "GPS en base de datos segura")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("ENTENDIDO", color = Color(0xFF3D5AFE)) }
        }
    )
}

@Composable
private fun ItemTecnico(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = Color(0xFF3D5AFE), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp)
    }
}
