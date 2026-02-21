package dev.jmcerezo.grabadoralegal.ui.componentes.dialogos

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DialogoEliminar(
    nombreArchivo: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D2E),
        titleContentColor = Color.White,
        textContentColor = Color.Gray,
        title = { Text("¿Eliminar evidencia?") },
        text = { Text("Vas a borrar permanentemente: $nombreArchivo. Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("ELIMINAR", color = Color(0xFFFF5252)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.White) }
        }
    )
}