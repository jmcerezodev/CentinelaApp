package dev.jmcerezo.centinela.ui.componentes

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
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import dev.jmcerezo.centinela.ui.componentes.dialogos.StructuredInfoDialog

/**
 * Componente principal de control de grabación.
 * Gestiona el inicio/parada de la grabadora y los ajustes de servicio en segundo plano.
 *
 * @param gestorAudio Instancia del motor de grabación.
 * @param alVerArchivos Callback ejecutado al detener una grabación.
 */
@Composable
fun TarjetaGrabacion(gestorAudio: GrabadoraMotor, alVerArchivos: () -> Unit) {
    val contexto = LocalContext.current
    val prefs = remember { Preferencias(contexto) }
    
    // Estados locales sincronizados con el hardware y preferencias
    var grabando by remember { mutableStateOf(gestorAudio.estaGrabando) }
    var servicioPermanente by remember { mutableStateOf(prefs.servicioPermanente) }
    var modoSilencioso by remember { mutableStateOf(prefs.modoSilencioso) }

    // Estados para la gestión de la UI desplegable
    var mostrarAjustes by remember { mutableStateOf(false) }
    var mostrarInfoPermanente by remember { mutableStateOf(false) }
    var mostrarInfoAntiSuspension by remember { mutableStateOf(false) }

    val rotacionIcono by animateFloatAsState(if (mostrarAjustes) 180f else 0f, label = "rotacion_flecha")

    /**
     * Envía una señal al Servicio de Accesibilidad para que actualice su configuración
     * basándose en las nuevas preferencias guardadas.
     */
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
                // FILA SUPERIOR: Estado del sistema y botón de acción principal
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

                    // Botón circular REC / STOP
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

                // SECCIÓN DESPLEGABLE: Configuración Avanzada
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
                        Icon(Icons.Default.Settings, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
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

                        // Ajuste: Servicio Permanente
                        FilaAjusteConInfo(
                            titulo = "Servicio Permanente",
                            subtitulo = "Evita el cierre automático",
                            activo = servicioPermanente,
                            onInfo = { mostrarInfoPermanente = true },
                            onToggle = {
                                servicioPermanente = it
                                prefs.servicioPermanente = it
                                actualizarServicio()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ajuste: Modo Anti-Suspensión
                        FilaAjusteConInfo(
                            titulo = "Modo Anti-Suspensión",
                            subtitulo = "Escucha con pantalla apagada",
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

    // GESTIÓN DE POPUPS DE INFORMACIÓN
    if (mostrarInfoPermanente) {
        StructuredInfoDialog(
            titulo = "Servicio Permanente",
            secciones = listOf(
                "Función" to "Mantiene a Centinela en la memoria del móvil para que esté siempre lista para actuar.",
                "Uso Diario" to "Déjalo activado siempre. No interfiere con otras apps y garantiza que los botones respondan.",
                "Batería" to "Consumo insignificante (0%)."
            ),
            onDismiss = { mostrarInfoPermanente = false }
        )
    }

    if (mostrarInfoAntiSuspension) {
        StructuredInfoDialog(
            titulo = "Modo Anti-Suspensión",
            secciones = listOf(
                "Función" to "Activa un motor de audio silencioso para que el sistema no 'duerma' los botones de volumen.",
                "Caso de Uso" to "Actívalo solo cuando necesites grabar discretamente con el móvil bloqueado y en el bolsillo.",
                "Batería" to "Consumo moderado (similar a escuchar música). Desactívalo al terminar la jornada."
            ),
            onDismiss = { mostrarInfoAntiSuspension = false }
        )
    }
}

@Composable
fun FilaAjusteConInfo(
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
            modifier = Modifier.scale(0.7f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3D5AFE),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF25293D)
            )
        )
    }
}
