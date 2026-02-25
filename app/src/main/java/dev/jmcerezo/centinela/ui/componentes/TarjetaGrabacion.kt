package dev.jmcerezo.centinela.ui.componentes

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
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor

/**
 * Componente principal de control de grabación (Versión Minimalista).
 * Centrado exclusivamente en la acción de capturar evidencia.
 */
@Composable
fun TarjetaGrabacion(gestorAudio: GrabadoraMotor, alVerArchivos: () -> Unit) {
    var grabando by remember { mutableStateOf(gestorAudio.estaGrabando) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFF0F111A))
            .padding(horizontal = 24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D2E)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (grabando) "GRABANDO AUDIO" else "GRABADORA",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (grabando) Color(0xFF00C853) else Color(0xFFFF5252), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (grabando) "Micrófono activo" else "Micrófono inactivo",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

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
                            .background(if (grabando) Color(0xFFFF5252) else Color(0xFF3D5AFE), CircleShape)
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
}
