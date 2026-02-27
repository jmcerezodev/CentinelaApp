package dev.jmcerezo.centinela.ui.componentes.dialogos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@Composable
fun DialogoSugerenciaBiometria(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { 
            Text(
                text = "Reforzar Seguridad", 
                color = Color.White, 
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ) 
        },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Para proteger tus evidencias legales, Centinela puede solicitar tu huella dactilar o reconocimiento facial cada vez que abras la aplicación.",
                    color = Color.White,
                    fontSize = 14.sp
                )
                
                Box(
                    modifier = Modifier
                        .background(Color(0xFF3D5AFE).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Esto garantiza que solo tú puedas acceder a los archivos grabados, incluso si alguien más utiliza tu dispositivo.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Text(
                    text = "Nota: Puedes cambiar esta configuración en cualquier momento desde los Ajustes Avanzados de la aplicación.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ACTIVAR PROTECCIÓN", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("MANTENER DESACTIVADA", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1A1D2E),
        shape = RoundedCornerShape(28.dp)
    )
}
