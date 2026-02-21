package dev.jmcerezo.grabadoralegal.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jmcerezo.grabadoralegal.core.GrabadoraMotor

@Composable
fun TarjetaGrabacion(gestorAudio: GrabadoraMotor, alVerArchivos: () -> Unit) {
    // Estado sincronizado con el motor
    var grabando by remember { mutableStateOf(gestorAudio.estaGrabando) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFF0F111A))
            .padding(horizontal = 24.dp)
    ) {
        // Tarjeta de Estado Principal
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D2E)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (grabando) "GRABANDO AUDIO" else "SISTEMA EN ESPERA",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (grabando) Color(0xFFFF5252) else Color(0xFF00C853),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (grabando) "Micrófono activo" else "Escucha activa lista",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Botón de acción REC/STOP
                IconButton(
                    onClick = {
                        if (grabando) {
                            gestorAudio.detenerGrabacion()
                            grabando = false
                            alVerArchivos()
                        } else {
                            gestorAudio.iniciarGrabacion()
                            grabando = true
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (grabando) Color(0xFFFF5252) else Color(0xFF3D5AFE),
                            CircleShape
                        )
                ) {
                    Text(
                        text = if (grabando) "STOP" else "REC",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}