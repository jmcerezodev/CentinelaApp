package dev.jmcerezo.grabadoralegal.ui.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    // ESTADOS INDEPENDIENTES
    var reproductorVisible by remember { mutableStateOf(false) }
    var infoVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // CABECERA SIEMPRE VISIBLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // El click en la tarjeta ahora controla el REPRODUCTOR
                    .clickable { reproductorVisible = !reproductorVisible }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // TÍTULO Y FECHA
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = grabacion.archivo.nameWithoutExtension,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = grabacion.fecha,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                // BOTONES DE ACCIÓN
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // BOTÓN INFO (Controla solo los DATOS TÉCNICOS)
                    IconButton(onClick = { infoVisible = !infoVisible }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Ver detalles",
                            tint = if (infoVisible) Color(0xFF3D5AFE) else Color.Gray
                        )
                    }

                    // BOTÓN MENÚ (3 PUNTOS)
                    Box {
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

            // SECCIÓN 1: REPRODUCTOR (Se activa al tocar la tarjeta)
            AnimatedVisibility(visible = reproductorVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ReproductorEvidencia(archivo = grabacion.archivo)
                }
            }

            // SECCIÓN 2: INFORMACIÓN TÉCNICA (Se activa con el botón Info)
            AnimatedVisibility(visible = infoVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = Color.Gray.copy(alpha = 0.1f)
                    )

                    DatoFila(etiqueta = "HASH SHA-256", valor = grabacion.hash.uppercase())
                    Spacer(modifier = Modifier.height(8.dp))
                    DatoFila(
                        etiqueta = "LOCALIZACIÓN",
                        valor = grabacion.ubicacion,
                        esUbicacion = true
                    )
                }
            }
        }
    }
}

@Composable
fun DatoFila(etiqueta: String, valor: String, esUbicacion: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = etiqueta,
            color = Color(0xFF3D5AFE),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (esUbicacion) {
                Icon(
                    Icons.Default.LocationOn,
                    null,
                    tint = Color(0xFF3D5AFE),
                    modifier = Modifier.size(14.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = valor,
                color = Color.LightGray,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}