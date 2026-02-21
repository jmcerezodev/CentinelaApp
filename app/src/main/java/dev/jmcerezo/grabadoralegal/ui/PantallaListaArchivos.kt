package dev.jmcerezo.grabadoralegal.ui

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jmcerezo.grabadoralegal.core.GrabadoraMotor
import dev.jmcerezo.grabadoralegal.model.GrabacionDato

@Composable
fun PantallaListaArchivos(gestorAudio: GrabadoraMotor, alVolver: () -> Unit) {
    val contexto = LocalContext.current
    var listaGrabaciones by remember { mutableStateOf(gestorAudio.obtenerGrabaciones()) }
    var archivoParaEliminar by remember { mutableStateOf<GrabacionDato?>(null) }
    var archivoParaRenombrar by remember { mutableStateOf<GrabacionDato?>(null) }
    var nuevoNombre by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
            .padding(horizontal = 16.dp)
    ) {
        if (listaGrabaciones.isEmpty()) {
            Text(
                text = "No hay grabaciones disponibles",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 14.sp
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
                color = Color(0xFF141725),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2235))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(listaGrabaciones) { grabacion ->
                        // Llamamos al componente que vive en el otro archivo
                        TarjetaEvidencia(
                            grabacion = grabacion,
                            onPlay = {
                                gestorAudio.reproducirAudio(grabacion.archivo)
                                Toast.makeText(contexto, "Reproduciendo...", Toast.LENGTH_SHORT).show()
                            },
                            onRename = {
                                archivoParaRenombrar = grabacion
                                nuevoNombre = grabacion.archivo.nameWithoutExtension
                            },
                            onShare = { gestorAudio.compartirArchivo(grabacion) },
                            onDelete = { archivoParaEliminar = grabacion }
                        )
                    }
                }
            }
        }
    }

    // --- MANTENEMOS LOS DIÁLOGOS AQUÍ PARA CENTRALIZAR LA LÓGICA ---
    if (archivoParaRenombrar != null) {
        AlertDialog(
            onDismissRequest = { archivoParaRenombrar = null },
            containerColor = Color(0xFF1A1D2E),
            titleContentColor = Color.White,
            title = { Text("Renombrar evidencia", fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3D5AFE)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    archivoParaRenombrar?.let { grab ->
                        if (gestorAudio.renombrarGrabacion(grab.archivo, nuevoNombre)) {
                            listaGrabaciones = gestorAudio.obtenerGrabaciones()
                        }
                    }
                    archivoParaRenombrar = null
                }) { Text("GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = { archivoParaRenombrar = null }) { Text("CANCELAR", color = Color.White) }
            }
        )
    }

    if (archivoParaEliminar != null) {
        AlertDialog(
            onDismissRequest = { archivoParaEliminar = null },
            containerColor = Color(0xFF1A1D2E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray,
            title = { Text("¿Eliminar evidencia?") },
            text = { Text("Esta acción borrará el archivo físico de forma permanente.") },
            confirmButton = {
                TextButton(onClick = {
                    archivoParaEliminar?.let {
                        if (gestorAudio.eliminarGrabacion(it.archivo)) {
                            listaGrabaciones = gestorAudio.obtenerGrabaciones()
                        }
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