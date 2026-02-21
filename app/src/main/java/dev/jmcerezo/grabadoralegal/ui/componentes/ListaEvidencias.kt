package dev.jmcerezo.grabadoralegal.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jmcerezo.grabadoralegal.model.GrabacionDato
// Añadimos el import explícito por si el IDE no los asocia automáticamente
import dev.jmcerezo.grabadoralegal.ui.componentes.TarjetaEvidencia

@Composable
fun ListaEvidencias(
    lista: List<GrabacionDato>,
    onPlay: (GrabacionDato) -> Unit,
    onRename: (GrabacionDato) -> Unit,
    onShare: (GrabacionDato) -> Unit,
    onDelete: (GrabacionDato) -> Unit
) {
    if (lista.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No hay grabaciones disponibles",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color(0xFF141725),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, Color(0xFF1F2235))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(lista) { grabacion ->
                    TarjetaEvidencia(
                        grabacion = grabacion,
                        onPlay = { onPlay(grabacion) },
                        onRename = { onRename(grabacion) },
                        onShare = { onShare(grabacion) },
                        onDelete = { onDelete(grabacion) }
                    )
                }
            }
        }
    }
}