package dev.jmcerezo.centinela.ui.componentes.dialogos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import dev.jmcerezo.centinela.ui.componentes.PermisoConsentimiento

@Composable
fun DialogoConsentimientoDestacado(
    consentimiento: PermisoConsentimiento,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { 
            Text(
                text = consentimiento.titulo, 
                color = Color.White, 
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = consentimiento.introduccion,
                    color = Color.White,
                    fontSize = 14.sp
                )
                
                Box(
                    modifier = Modifier
                        .background(Color(0xFF3D5AFE).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Propósito de la función:",
                            color = Color(0xFF3D5AFE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = consentimiento.proposito,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Text(
                    text = "Privacidad: Centinela NO recopila ni comparte sus datos personales con terceros. Esta información se utiliza exclusivamente para la funcionalidad descrita.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "¿Deseas conceder este permiso ahora?",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("AHORA NO", color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SÍ, CONTINUAR", color = Color.White)
            }
        },
        containerColor = Color(0xFF1A1D2E),
        shape = RoundedCornerShape(28.dp)
    )
}
