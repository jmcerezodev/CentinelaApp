package dev.jmcerezo.centinela.ui.componentes

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jmcerezo.centinela.R
import kotlinx.coroutines.delay
import java.io.File

/**
 * REPRODUCTOR DE EVIDENCIAS PROFESIONAL (Ultra Compacto)
 * 
 * Interfaz minimalista con botones alineados exactamente con la línea de tiempo.
 */
@Composable
fun ReproductorEvidencia(archivo: File) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var estaReproduciendo by remember { mutableStateOf(false) }
    var posicionActual by remember { mutableFloatStateOf(0f) }
    var duracionTotal by remember { mutableFloatStateOf(0f) }
    var estaArrastrando by remember { mutableStateOf(false) }

    // Inicialización y liberación del hardware de audio
    DisposableEffect(archivo) {
        val player = MediaPlayer().apply {
            try {
                setDataSource(archivo.absolutePath)
                prepare()
                duracionTotal = duration.toFloat()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = player
        onDispose {
            player.release()
            mediaPlayer = null
        }
    }

    // Hilo de actualización de la barra de progreso
    LaunchedEffect(estaReproduciendo, estaArrastrando) {
        while (estaReproduciendo && !estaArrastrando) {
            posicionActual = mediaPlayer?.currentPosition?.toFloat() ?: 0f
            if (posicionActual >= duracionTotal - 200f) {
                estaReproduciendo = false
                mediaPlayer?.pause()
                mediaPlayer?.seekTo(0)
                posicionActual = 0f
            }
            delay(100)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color(0xFF141725), 
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // --- FILA 1: BOTONES Y SLIDER (Alineados al centro entre ellos) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bloque de botones
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // PLAY / PAUSE
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0xFF3D5AFE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (estaReproduciendo) {
                                    mediaPlayer?.pause()
                                } else {
                                    mediaPlayer?.start()
                                }
                                estaReproduciendo = mediaPlayer?.isPlaying ?: false
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (estaReproduciendo) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pause_pro),
                                    contentDescription = "Pausa",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Reproducir",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // STOP / RESET
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                mediaPlayer?.pause()
                                mediaPlayer?.seekTo(0)
                                estaReproduciendo = false
                                posicionActual = 0f
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_stop_pro),
                                contentDescription = "Detener",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Slider (Ocupa el resto de la fila)
                Slider(
                    value = posicionActual,
                    onValueChange = { nuevaPos ->
                        estaArrastrando = true
                        posicionActual = nuevaPos
                    },
                    onValueChangeFinished = {
                        mediaPlayer?.seekTo(posicionActual.toInt())
                        estaArrastrando = false
                    },
                    valueRange = 0f..(if (duracionTotal > 0) duracionTotal else 1f),
                    modifier = Modifier.weight(1f).height(18.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF3D5AFE),
                        activeTrackColor = Color(0xFF3D5AFE),
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            // --- FILA 2: TIEMPOS (Alineados bajo el slider) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 68.dp, end = 4.dp), // 26+26+6+10 = 68dp de offset para alinear con el slider
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(posicionActual.toInt()),
                    color = Color(0xFF3D5AFE),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTime(duracionTotal.toInt()),
                    color = Color.Gray,
                    fontSize = 8.sp
                )
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
