package dev.jmcerezo.grabadoralegal.ui.componentes

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jmcerezo.grabadoralegal.core.GrabadoraMotor
import dev.jmcerezo.grabadoralegal.core.Preferencias

@Composable
fun TarjetaGrabacion(gestorAudio: GrabadoraMotor, alVerArchivos: () -> Unit) {
    val contexto = LocalContext.current
    val prefs = remember { Preferencias(contexto) }
    
    var grabando by remember { mutableStateOf(gestorAudio.estaGrabando) }
    var servicioPermanente by remember { mutableStateOf(prefs.servicioPermanente) }
    var modoSilencioso by remember { mutableStateOf(prefs.modoSilencioso) }

    var mostrarAjustes by remember { mutableStateOf(false) }
    var mostrarInfoPermanente by remember { mutableStateOf(false) }
    var mostrarInfoAntiSuspension by remember { mutableStateOf(false) }

    val rotacionIcono by animateFloatAsState(if (mostrarAjustes) 180f else 0f, label = "")

    val actualizarServicio = {
        contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").apply {
            setPackage(contexto.packageName)
        })
    }

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
                // Fila Principal: REC / STOP
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Desplegable de Ajustes
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarAjustes = !mostrarAjustes },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CONFIGURACIÓN AVANZADA",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.rotate(rotacionIcono)
                        )
                    }
                }

                AnimatedVisibility(visible = mostrarAjustes) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.Gray.copy(alpha = 0.1f)
                        )

                        // OPCIÓN: SERVICIO PERMANENTE
                        AjusteFila(
                            titulo = "Servicio Permanente",
                            subtitulo = "Evita que el sistema cierre la app",
                            activo = servicioPermanente,
                            onInfo = { mostrarInfoPermanente = true },
                            onToggle = {
                                servicioPermanente = it
                                prefs.servicioPermanente = it
                                actualizarServicio()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // OPCIÓN: MODO ANTI-SUSPENSIÓN
                        AjusteFila(
                            titulo = "Modo Anti-Suspensión",
                            subtitulo = "Forzar escucha con pantalla apagada",
                            activo = modoSilencioso,
                            onInfo = { mostrarInfoAntiSuspension = true },
                            onToggle = {
                                modoSilencioso = it
                                prefs.modoSilencioso = it
                                actualizarServicio()
                            }
                        )
                    }
                }
            }
        }
    }

    // DIÁLOGOS DE INFORMACIÓN REESTRUCTURADOS
    if (mostrarInfoPermanente) {
        InfoDialog(
            titulo = "Servicio Permanente",
            secciones = listOf(
                "Función" to "Mantiene a Centinela en la memoria del móvil para que esté siempre lista para actuar.",
                "Uso Diario" to "Déjalo activado siempre. No interfiere con otras apps y garantiza que los botones respondan.",
                "Batería" to "Consumo casi inexistente (0%)."
            ),
            onDismiss = { mostrarInfoPermanente = false }
        )
    }

    if (mostrarInfoAntiSuspension) {
        InfoDialog(
            titulo = "Modo Anti-Suspensión",
            secciones = listOf(
                "Función" to "Activa un motor de audio silencioso para que el sistema no 'duerma' los botones de volumen.",
                "Caso de Uso" to "Actívalo solo cuando necesites grabar discretamente con el móvil bloqueado y en el bolsillo.",
                "Batería" to "Consumo moderado (similar a escuchar música). Desactívalo al terminar para ahorrar energía."
            ),
            onDismiss = { mostrarInfoAntiSuspension = false }
        )
    }
}

@Composable
fun AjusteFila(
    titulo: String,
    subtitulo: String,
    activo: Boolean,
    onInfo: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitulo, color = Color.Gray, fontSize = 11.sp)
            }
            IconButton(
                onClick = onInfo,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF3D5AFE).copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Switch(
            checked = activo,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.7f), // Interruptor más pequeño
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3D5AFE),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF25293D)
            )
        )
    }
}

@Composable
fun InfoDialog(titulo: String, secciones: List<Pair<String, String>>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = titulo, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                secciones.forEach { (subtitulo, contenido) ->
                    Column {
                        Text(
                            text = subtitulo.uppercase(),
                            color = Color(0xFF3D5AFE),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = contenido,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ENTENDIDO", color = Color(0xFF3D5AFE), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1A1D2E),
        shape = RoundedCornerShape(24.dp)
    )
}
