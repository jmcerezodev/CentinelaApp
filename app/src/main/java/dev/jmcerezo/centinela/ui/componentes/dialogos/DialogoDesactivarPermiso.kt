package dev.jmcerezo.centinela.ui.componentes.dialogos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import dev.jmcerezo.centinela.ui.componentes.PermisoConsentimiento

@Composable
fun DialogoDesactivarPermiso(
    consentimiento: PermisoConsentimiento,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { 
            Text(
                text = "Desactivar Protección", 
                color = Color.White, 
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Has decidido desactivar el acceso a: ${consentimiento.titulo}.",
                    color = Color.White,
                    fontSize = 14.sp
                )
                
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFF5252).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Al desactivar esta función, Centinela dejará de: ${consentimiento.proposito}",
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Text(
                    text = "Importante: Para revocar totalmente el permiso, Android requiere que lo hagas manualmente desde los Ajustes de la Aplicación.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("IR A AJUSTES", color = Color.White)
            }
        },
        containerColor = Color(0xFF1A1D2E),
        shape = RoundedCornerShape(28.dp)
    )
}
