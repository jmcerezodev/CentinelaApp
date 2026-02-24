package dev.jmcerezo.centinela.ui.componentes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.jmcerezo.centinela.core.engine.GrabadoraMotor
import dev.jmcerezo.centinela.core.service.CentinelaService
import dev.jmcerezo.centinela.data.local.prefs.Preferencias
import dev.jmcerezo.centinela.ui.componentes.dialogos.StructuredInfoDialog

/**
 * Componente principal de control de grabación con sincronización en tiempo real.
 */
@Composable
fun TarjetaGrabacion(gestorAudio: GrabadoraMotor, alVerArchivos: () -> Unit) {
    val contexto = LocalContext.current
    val prefs = remember { Preferencias(contexto) }
    
    var grabando by remember { mutableStateOf(gestorAudio.estaGrabando) }
    var servicioPermanente by remember { mutableStateOf(prefs.servicioPermanente) }
    var modoSilencioso by remember { mutableStateOf(prefs.modoSilencioso) }
    var botonesHabilitados by remember { mutableStateOf(prefs.botonesHabilitados) }

    var mostrarAjustes by remember { mutableStateOf(false) }
    var mostrarInfoPermanente by remember { mutableStateOf(false) }
    var mostrarInfoAntiSuspension by remember { mutableStateOf(false) }
    var mostrarInfoBotones by remember { mutableStateOf(false) }

    // RECEPTOR PARA SINCRONIZAR CON EL WIDGET
    DisposableEffect(contexto) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Cuando el widget cambia algo, actualizamos los estados de la UI de la app
                servicioPermanente = prefs.servicioPermanente
                modoSilencioso = prefs.modoSilencioso
                botonesHabilitados = prefs.botonesHabilitados
            }
        }
        val filter = IntentFilter("dev.jmcerezo.ACTUALIZAR_CONFIGURACION")
        ContextCompat.registerReceiver(contexto, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { contexto.unregisterReceiver(receiver) }
    }

    val rotacionIcono by animateFloatAsState(if (mostrarAjustes) 180f else 0f, label = "rotacion")

    val sincronizarServicios = {
        contexto.startService(Intent(contexto, CentinelaService::class.java))
        contexto.sendBroadcast(Intent("dev.jmcerezo.ACTUALIZAR_CONFIGURACION").setPackage(contexto.packageName))
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
                // FILA SUPERIOR: REC / STOP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = if (grabando) "GRABANDO AUDIO" else "SISTEMA EN ESPERA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(if (grabando) Color(0xFFFF5252) else Color(0xFF00C853), CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (grabando) "Micrófono activo" else "Escucha activa lista", color = Color.Gray, fontSize = 12.sp)
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
                        modifier = Modifier.size(56.dp).background(if (grabando) Color(0xFFFF5252) else Color(0xFF3D5AFE), CircleShape)
                    ) {
                        Text(text = if (grabando) "STOP" else "REC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { mostrarAjustes = !mostrarAjustes },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Settings, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONFIGURACIÓN AVANZADA", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.rotate(rotacionIcono))
                    }
                }

                AnimatedVisibility(visible = mostrarAjustes) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.1f))

                        AjusteInterruptorConInfo(
                            titulo = "Grabación con Botones",
                            subtitulo = "Usa volumen arriba (x3) para grabar",
                            activo = botonesHabilitados,
                            onInfo = { mostrarInfoBotones = true },
                            onToggle = { activado ->
                                botonesHabilitados = activado
                                prefs.botonesHabilitados = activado
                                sincronizarServicios()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AjusteInterruptorConInfo(
                            titulo = "Servicio Permanente",
                            subtitulo = "Evita el cierre automático",
                            activo = servicioPermanente,
                            onInfo = { mostrarInfoPermanente = true },
                            onToggle = { activado ->
                                servicioPermanente = activado
                                prefs.servicioPermanente = activado
                                sincronizarServicios()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AjusteInterruptorConInfo(
                            titulo = "Modo Anti-Suspensión",
                            subtitulo = "Escucha con pantalla apagada",
                            activo = modoSilencioso,
                            onInfo = { mostrarInfoAntiSuspension = true },
                            onToggle = { activado ->
                                modoSilencioso = activado
                                prefs.modoSilencioso = activado
                                sincronizarServicios()
                            }
                        )
                    }
                }
            }
        }
    }

    // DIÁLOGOS DE INFORMACIÓN
    if (mostrarInfoBotones) {
        StructuredInfoDialog(
            titulo = "Grabación con Botones",
            secciones = listOf(
                "Función" to "Permite iniciar o detener la grabación pulsando 3 veces el botón de volumen arriba.",
                "Recomendación" to "Desactívalo si vas a escuchar música.",
                "Seguridad" to "Aunque esté desactivado, el botón REC de la pantalla siempre funcionará."
            ),
            onDismiss = { mostrarInfoBotones = false }
        )
    }

    if (mostrarInfoPermanente) {
        StructuredInfoDialog(
            titulo = "Servicio Permanente",
            secciones = listOf(
                "Función" to "Mantiene la app en memoria para actuar siempre.",
                "Batería" to "Consumo insignificante (0%)."
            ),
            onDismiss = { mostrarInfoPermanente = false }
        )
    }

    if (mostrarInfoAntiSuspension) {
        StructuredInfoDialog(
            titulo = "Modo Anti-Suspensión",
            secciones = listOf(
                "Función" to "Fuerza la escucha con pantalla apagada.",
                "Caso de Uso" to "Actívalo solo para grabaciones discretas.",
                "Batería" to "Consumo moderado. Desactívalo al terminar."
            ),
            onDismiss = { mostrarInfoAntiSuspension = false }
        )
    }
}
