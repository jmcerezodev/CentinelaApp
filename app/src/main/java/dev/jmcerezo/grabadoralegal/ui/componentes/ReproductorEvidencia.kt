package dev.jmcerezo.grabadoralegal.ui.componentes

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ReproductorEvidencia(archivo: File) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var estaReproduciendo by remember { mutableStateOf(false) }
    var posicionActual by remember { mutableFloatStateOf(0f) }
    var duracionTotal by remember { mutableFloatStateOf(0f) }

    // Estado para saber si el usuario está arrastrando el dedo
    var estaArrastrando by remember { mutableStateOf(false) }

    DisposableEffect(archivo) {
        val player = MediaPlayer().apply {
            try {
                setDataSource(archivo.absolutePath)
                prepare()
                duracionTotal = duration.toFloat()
            } catch (e: Exception) { e.printStackTrace() }
        }
        mediaPlayer = player
        onDispose {
            player.release()
            mediaPlayer = null
        }
    }

    // Actualización rápida para suavidad (100ms es el punto dulce)
    LaunchedEffect(estaReproduciendo, estaArrastrando) {
        while (estaReproduciendo && !estaArrastrando) {
            posicionActual = mediaPlayer?.currentPosition?.toFloat() ?: 0f
            if (posicionActual >= duracionTotal - 200f) {
                estaReproduciendo = false
                mediaPlayer?.pause()
            }
            delay(100)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (estaReproduciendo) {
                        mediaPlayer?.pause()
                    } else {
                        if (posicionActual >= duracionTotal - 1000f) {
                            mediaPlayer?.seekTo(0)
                            posicionActual = 0f
                        }
                        mediaPlayer?.start()
                    }
                    estaReproduciendo = mediaPlayer?.isPlaying ?: false
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (estaReproduciendo) Icons.Default.Refresh else Icons.Default.PlayArrow,
                    contentDescription = null, tint = Color.White
                )
            }

            Slider(
                value = posicionActual,
                onValueChange = { nuevaPos ->
                    estaArrastrando = true // Bloqueamos la actualización del hilo
                    posicionActual = nuevaPos
                },
                onValueChangeFinished = {
                    mediaPlayer?.seekTo(posicionActual.toInt())
                    estaArrastrando = false // Devolvemos el control al hilo
                },
                valueRange = 0f..(if (duracionTotal > 0) duracionTotal else 1f),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF3D5AFE),
                    activeTrackColor = Color(0xFF3D5AFE),
                    inactiveTrackColor = Color.DarkGray
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(posicionActual.toInt()), color = Color.Gray, fontSize = 10.sp)
            Text(formatTime(duracionTotal.toInt()), color = Color.Gray, fontSize = 10.sp)
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}