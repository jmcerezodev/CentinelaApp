package dev.jmcerezo.grabadoralegal.ui.componentes.dialogos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DialogoRenombrar(
    nombreActual: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Estado local para lo que el usuario escribe
    var textoNuevoNombre by remember { mutableStateOf(nombreActual) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D2E),
        titleContentColor = Color.White,
        title = { Text("Renombrar evidencia") },
        text = {
            Column {
                Text(
                    text = "Introduce el nuevo nombre para el archivo:",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = textoNuevoNombre,
                    onValueChange = { textoNuevoNombre = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3D5AFE),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF3D5AFE)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (textoNuevoNombre.isNotBlank()) {
                        onConfirm(textoNuevoNombre)
                    }
                }
            ) {
                Text("GUARDAR", color = Color(0xFF3D5AFE))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.White)
            }
        }
    )
}