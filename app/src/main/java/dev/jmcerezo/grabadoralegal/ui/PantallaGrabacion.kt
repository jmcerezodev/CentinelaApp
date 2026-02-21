package dev.jmcerezo.grabadoralegal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

// IMPORTANTE: Estos dos imports son los que suelen faltar para el "by"
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun PantallaGrabacion(gestorAudio: GrabadoraMotor, alVerArchivos: () -> Unit) {
    // Estado sincronizado con el motor
    var grabando by remember { mutableStateOf(gestorAudio.estaGrabando) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A)) // Azul Noche Profesional
            .padding(24.dp)
    ) {
        // Cabecera Minimalista
        Text(
            text = "CENTINELA",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = "Sistema de Protección",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Tarjeta de Estado Principal
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D2E)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
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

                // Botón de acción rápido
                IconButton(
                    onClick = {
                        if (grabando) {
                            gestorAudio.detenerGrabacion()
                        } else {
                            gestorAudio.iniciarGrabacion()
                        }
                        grabando = gestorAudio.estaGrabando
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

        Spacer(modifier = Modifier.height(16.dp))

        // Info Técnica
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoCard(modifier = Modifier.weight(1f), title = "Encriptación", value = "SHA-256")
            InfoCard(modifier = Modifier.weight(1f), title = "Formato", value = "M4A HQ")
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón Inferior: Historial
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { alVerArchivos() },
            color = Color(0xFF1A1D2E),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D5AFE))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "ACCEDER AL HISTORIAL",
                    color = Color(0xFF3D5AFE),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun InfoCard(modifier: Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = Color.Gray, fontSize = 10.sp)
            Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}