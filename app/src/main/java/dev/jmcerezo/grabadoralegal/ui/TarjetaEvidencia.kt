package dev.jmcerezo.grabadoralegal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jmcerezo.grabadoralegal.model.GrabacionDato

@Composable
fun TarjetaEvidencia(
    grabacion: GrabacionDato,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay() }
                    .padding(16.dp)
            ) {
                Text(
                    text = grabacion.archivo.nameWithoutExtension,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 32.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                DatoFila(etiqueta = "FECHA", valor = grabacion.fecha)
                DatoFila(etiqueta = "ID", valor = grabacion.hash.take(16).uppercase())

                // UBICACIÓN: Ahora permite múltiples líneas para Calle + GPS
                DatoFila(
                    etiqueta = "UBICACIÓN",
                    valor = grabacion.ubicacion,
                    esUbicacion = true
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(onClick = { menuExpandido = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = menuExpandido,
                    onDismissRequest = { menuExpandido = false },
                    modifier = Modifier.background(Color(0xFF25293D))
                ) {
                    DropdownMenuItem(
                        text = { Text("Renombrar", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = Color.Gray) },
                        onClick = { menuExpandido = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Compartir", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.White) },
                        onClick = { menuExpandido = false; onShare() }
                    )
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = Color(0xFFFF5252)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252)) },
                        onClick = { menuExpandido = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun DatoFila(etiqueta: String, valor: String, esUbicacion: Boolean = false) {
    // Usamos Column para la ubicación si el texto es largo, o Row si es corto
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = if (esUbicacion) Alignment.Top else Alignment.CenterVertically
    ) {
        Text(
            text = "$etiqueta: ",
            color = Color(0xFF3D5AFE),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.width(75.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (esUbicacion) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = valor,
                    color = if (valor == "Ubicación no disponible") Color.DarkGray else Color.LightGray,
                    fontSize = if (esUbicacion) 11.sp else 12.sp, // Un poco más pequeña si es GPS
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp,
                    maxLines = 3, // Permitimos hasta 3 líneas
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}