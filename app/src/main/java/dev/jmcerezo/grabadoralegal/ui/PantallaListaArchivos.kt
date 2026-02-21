package dev.jmcerezo.grabadoralegal.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jmcerezo.grabadoralegal.core.GrabadoraMotor
import dev.jmcerezo.grabadoralegal.model.GrabacionDato

@Composable
fun PantallaListaArchivos(gestorAudio: GrabadoraMotor, alVolver: () -> Unit) {
    val contexto = LocalContext.current

    // Cargamos la lista de grabaciones desde el motor
    val listaGrabaciones = remember {
        mutableStateListOf<GrabacionDato>().apply {
            addAll(gestorAudio.obtenerGrabaciones())
        }
    }

    var archivoParaEliminar by remember { mutableStateOf<GrabacionDato?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A)) // Azul Noche coherente
            .padding(24.dp)
    ) {
        Text(
            text = "EVIDENCIAS GUARDADAS",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Lista de archivos
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listaGrabaciones) { grabacion ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D2E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    gestorAudio.reproducirAudio(grabacion.archivo)
                                    Toast.makeText(contexto, "Reproduciendo...", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = grabacion.fecha,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SHA-256: ${grabacion.hash.take(16)}...",
                                color = Color(0xFF3D5AFE),
                                fontSize = 10.sp
                            )
                        }

                        // Botones de acción
                        IconButton(onClick = { gestorAudio.compartirArchivo(grabacion) }) {
                            Text("📤", fontSize = 20.sp)
                        }

                        IconButton(onClick = { archivoParaEliminar = grabacion }) {
                            Text("🗑️", fontSize = 20.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón Volver
        Button(
            onClick = {
                gestorAudio.detenerReproduccion()
                alVolver()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1D2E)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D5AFE))
        ) {
            Text("VOLVER AL PANEL", color = Color(0xFF3D5AFE), fontWeight = FontWeight.Bold)
        }
    }

    // Cuadro de diálogo de eliminación (se mantiene igual pero con estilo)
    if (archivoParaEliminar != null) {
        AlertDialog(
            onDismissRequest = { archivoParaEliminar = null },
            containerColor = Color(0xFF1A1D2E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray,
            title = { Text("¿Eliminar evidencia?") },
            text = { Text("Esta acción borrará el archivo físico y su firma digital de forma permanente.") },
            confirmButton = {
                TextButton(onClick = {
                    archivoParaEliminar?.let {
                        if (gestorAudio.eliminarGrabacion(it.archivo)) listaGrabaciones.remove(it)
                    }
                    archivoParaEliminar = null
                }) { Text("ELIMINAR", color = Color(0xFFFF5252)) }
            },
            dismissButton = {
                TextButton(onClick = { archivoParaEliminar = null }) { Text("CANCELAR", color = Color.White) }
            }
        )
    }
}