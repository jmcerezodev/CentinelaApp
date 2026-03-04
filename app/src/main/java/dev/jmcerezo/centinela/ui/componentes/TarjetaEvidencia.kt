package dev.jmcerezo.centinela.ui.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jmcerezo.centinela.data.local.db.GrabacionDato
import java.io.File

/**
 * Componente que muestra la información de una grabación individual.
 * Permite reproducir, renombrar, compartir, guardar y eliminar la evidencia.
 */
@Composable
fun TarjetaEvidencia(
    grabacion: GrabacionDato,
    estaMarcado: Boolean,
    onToggleFavorite: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDelete: () -> Unit,
    onGeneratePDF: () -> Unit
) {
    var menuExpandido by remember { mutableStateOf(false) }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { reproductorVisible = !reproductorVisible }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = grabacion.nombre,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (estaMarcado) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFF3D5AFE),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = grabacion.fecha,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { onToggleFavorite() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Destacar",
                            tint = if (estaMarcado) Color(0xFF3D5AFE) else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { infoVisible = !infoVisible },
                        modifier = Modifier.size(32.dp)
                    ) {
                        // Caja azul con la letra 'i' en blanco - Ajuste óptico de centrado (subido 1dp)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color(0xFF3D5AFE), CircleShape)
                        ) {
                            Text(
                                text = "i",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset(y = (-1).dp),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpandido = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
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
                                text = { Text("Guardar", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Download, null, tint = Color.White) },
                                onClick = { menuExpandido = false; onSaveToDevice() }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartir Audio", color = Color.White) },
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

            AnimatedVisibility(visible = reproductorVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ReproductorEvidencia(archivo = File(grabacion.rutaArchivo))
                }
            }

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
                    DatoFila(etiqueta = "LOCALIZACIÓN", valor = grabacion.ubicacion, esUbicacion = true)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // BOTÓN GENERAR INFORME PDF
                    Button(
                        onClick = onGeneratePDF,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3D5AFE).copy(alpha = 0.1f),
                            contentColor = Color(0xFF3D5AFE)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GENERAR INFORME PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
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
        Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.Top) {
            if (esUbicacion) {
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFF3D5AFE), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text = valor, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
